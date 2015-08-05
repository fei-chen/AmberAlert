#/bin/bash
nodetool=/mnt/cassandra_latest/bin/nodetool
echo SAFE Shutdown cassandra
  # stop cassandra nicely first
$nodetool disablebinary
$nodetool disablethrift
$nodetool disablebackup
$nodetool disablegossip
$nodetool disablehandoff
$nodetool stop compaction
$nodetool stop cleanup
$nodetool stop scrub
$nodetool stop index_build
$nodetool drain

# Try to kill the cassandra process through multiple attempts 
count=0
while [ $count -le 10 ]; do
sleep 2;
if [[ -z $(pgrep -f cassandra) ]]; then
# process quit break the loop
break;
fi
echo PROCESS running, trying to kill again $count attempt
if [ $count -le 5 ]; then
# soft kill
pkill -f cassandra;
else
# Hard kill
kill -9 $(pgrep -f cassandra)
fi
((count++))
done

# check if we managed to finally kill the cassandra process
if [[ -n $(pgrep -f cassandra) ]]; then
# Nope still up, fail now.
echo failed to terminate cassandra process.
exit 1;
fi

