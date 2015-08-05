#!/bin/bash
until java -cp /mnt/cassandra_latest/lib/*:/mnt/uber-elastic-cassandra-1.0-SNAPSHOT.jar:/mnt/cassandra_latest/conf -Dlog4j.configuration=file:////mnt/cassandra_latest/conf/log4j-server.properties -Dcassandra.config=file:////mnt/cassandra_latest/conf/cassandra.yaml com.bloomreach.service.ReplicationServer --port 9000 &>> /mnt/cassandra/log/rpc.log; do
    echo "RPC server crashed with exit code $?.  Respawning.." >&2
    sleep 1
done
