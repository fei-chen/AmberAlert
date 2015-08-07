package com.highform.datastore.db.cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.highform.db.cassandra.proto.Cassandra.CSConfig;
import com.highform.db.cassandra.proto.Cassandra.Retry;
import com.google.common.base.Preconditions;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.impl.SimpleAuthenticationCredentials;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.retry.BoundedExponentialBackoff;
import com.netflix.astyanax.retry.ConstantBackoff;
import com.netflix.astyanax.retry.ExponentialBackoff;
import com.netflix.astyanax.retry.RetryNTimes;
import com.netflix.astyanax.retry.RetryPolicy;
import com.netflix.astyanax.retry.RunOnce;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

public class CassandraConfig {
  private static final Logger log = LoggerFactory.getLogger(CassandraConfig.class);

  public static AstyanaxContext<Keyspace> getContext (CSConfig config) {
    Preconditions.checkNotNull(config);

    AstyanaxConfigurationImpl astyanaxImpl = new AstyanaxConfigurationImpl()
        .setDiscoveryType(NodeDiscoveryType.NONE)
        .setCqlVersion(config.getCqlVersion())
        .setTargetCassandraVersion(config.getTargetCassandraVersion());

    ConnectionPoolConfigurationImpl connImpl = new ConnectionPoolConfigurationImpl(config.getConnectionPool())
        .setPort(config.getPort())
        .setMaxConnsPerHost(config.getMaxConnsPerHost())
        .setSeeds(config.getSeeds());

    connImpl.setConnectTimeout(config.getConnsTimeout());

    if (config.hasUser() && config.hasPassword()) {
      connImpl.setAuthenticationCredentials(new SimpleAuthenticationCredentials(
          config.getUser(), config.getPassword()));
    }

    AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
        .forCluster(config.getCluster())
        .forKeyspace(config.getKeyspace())
        .withAstyanaxConfiguration(astyanaxImpl)
        .withConnectionPoolConfiguration(connImpl)
        .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
        .buildKeyspace(ThriftFamilyFactory.getInstance());

    return context;
  }

  public static RetryPolicy getRetryPolicy(Retry retry) {
    Preconditions.checkNotNull(retry);
    switch(retry.getRetryType()) {
    case BOUNDEDEXPONENTIALBACKOFF:
      return new BoundedExponentialBackoff(retry.getBaseSleepTimeMs(), retry.getMaxSleepTimeMs(), retry.getMaxRetryTimes());
    case CONSTANTBACKOFF:
      return new ConstantBackoff(retry.getBaseSleepTimeMs(), retry.getMaxRetryTimes());
    case EXPONENTIALBACKOFF:
      return new ExponentialBackoff(retry.getBaseSleepTimeMs(), retry.getMaxRetryTimes());
    case RETRYNTIMES:
      return new RetryNTimes(retry.getMaxRetryTimes());
    case RUNONCE:
      return new RunOnce();
    default:
      log.error("Invalid type of retry policy: " + retry.getRetryType().toString());
      return new RunOnce();
    }
  }

}
