#!/bin/bash
nodetool=/mnt/cassandra_latest/bin/nodetool

function Timeout {
 command="$1"
 t=$2
 echo command=$command
 echo timeout=$t
 timeout $t $command
 error=$?
 if [ $error == 124 ]; then
   echo $command TIMEDOUT AFTER $t seconds;
 else
   echo $COMMAND returned $error;
 fi
 return $error
}

while true; do
  echo SAFE Shutdown cassandra
  # stop cassandra nicely first
  Timeout "$nodetool disablebinary" 15
  Timeout "$nodetool disablethrift" 15
  Timeout "$nodetool disablebackup" 15
  Timeout "$nodetool disablegossip" 15
  Timeout "$nodetool disablehandoff" 15
  Timeout "$nodetool stop compaction" 30
  Timeout "$nodetool stop cleanup" 30
  Timeout "$nodetool stop scrub" 30
  Timeout "$nodetool stop index_build" 30
  Timeout "$nodetool drain" 120

  # Try to kill the cassandra process through multiple attempts 
  count=0
  while [ $count -le 10 ]; do
    sleep 2;
    if [[ -z $(pgrep -f CassandraDaemon) ]]; then
      # process quit break the loop
      break;
    fi
    echo PROCESS running, trying to kill again $count attempt
    if [ $count -le 5 ]; then
      # soft kill
      pkill -f CassandraDaemon;
    else
      # Hard kill
      kill -9 $(pgrep -f CassandraDaemon)
    fi
    ((count++))
  done

  # check if we managed to finally kill the cassandra process
  if [[ -n $(pgrep -f cassandraDaemon) ]]; then
    # Nope still up, fail now.
    echo failed to terminate cassandra process.
    exit 1;
  fi

  # Start the cassandra process now
  echo STARTING cassandra
  # & means redirecting exception and error message to the log 
  /mnt/cassandra_latest/bin/cassandra &> /mnt/cassandra/log/start.log
  echo cassandra started 
 
  # wait for cassandra to come up
  failed=true;
  for i in {0..20}; do
    sleep 10;
    echo checking connection
    # check if cassandra is running
    if [ exec 6<>/dev/tcp/localhost/9160 ]; then
      if [ exec 6<>/dev/tcp/localhost/9041 ]; then
          echo "Node UP"
        exit 0;
      fi
    fi

    # check for known issues in cassandra process
    if [ -n "$(grep -i "Unfinished compactions reference missing sstables" /mnt/cassandra/log/start.log)" ]; then
      # This is a known issue and workaround is to clear the compactions in progress directory
      echo Unfinished Compaction bug recovery
      rm -rf /mnt/cassandra/data/system/compactions_in_progress/*
      failed=false;
      break;
    elif [ -n "$(grep -i "java.net.BindException: Address already in use" /mnt/cassandra/log/start.log)" ]; then
      # This can happen sometimes when the cassandra process shutsdown and we start again the port fails to bind
      echo Cassandra process still alive , retrying shutdown
      failed=false;
      break;
    fi
  done

  if $failed; then
      # at this point we expected cassandra to start, if it still has not we should fail and get some human involved
      echo "Cassandra failed to start , unknown reason";
      exit 1;
  fi

done;
