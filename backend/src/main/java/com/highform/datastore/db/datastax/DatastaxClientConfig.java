/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 10, 2013.
 */

package com.bloomreach.db.datastax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.db.datastax.proto.Datastax.Config;
import com.bloomreach.db.datastax.proto.Datastax.Reconnection;
import com.bloomreach.db.datastax.proto.Datastax.Retry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.FallthroughRetryPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

import com.google.common.base.Preconditions;

/**
 * datastax client config
 *
 * @author fei
 */
public class DatastaxClientConfig {
  private static final Logger log = LoggerFactory.getLogger(DatastaxClientConfig.class);

  public static Cluster getCluster(Config config) {
    Preconditions.checkNotNull(config);

    Cluster.Builder cluster = Cluster.builder();

    String seeds = config.getSeeds();
    for (String seed : seeds.split("[\\s,;]+")) {
      cluster.addContactPoint(seed);
    }

    if (config.hasUser() && config.hasPassword()) {
      cluster.withCredentials(config.getUser(), config.getPassword());
    }

    cluster.withPort(config.getPort());

    if (config.hasRetry()) {
      cluster.withRetryPolicy(getRetryPolicy(config.getRetry()));
    }

    if (config.hasReconnection()) {
        cluster.withReconnectionPolicy(getReconnectionPolicy(config.getReconnection()));
    }

    if (config.hasDatacenter()) {
      LoadBalancingPolicy policy = new DCAwareRoundRobinPolicy(config.getDatacenter());
      TokenAwarePolicy tokenAware = new TokenAwarePolicy(policy);
      cluster.withLoadBalancingPolicy(tokenAware);
    }

    cluster.poolingOptions().setMaxConnectionsPerHost(HostDistance.LOCAL, config.getMaxConnsPerHost());
    cluster.socketOptions().setConnectTimeoutMillis(config.getConnsTimeout());
    return cluster.build();
  }

  public static RetryPolicy getRetryPolicy(Retry retry) {
    Preconditions.checkNotNull(retry);
    RetryPolicy retryPolicy = null;
    switch(retry.getRetryType()) {
    case DEFAULTRETRYPOLICY:
      retryPolicy = new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE);
      break;
    case DOWNGRADINGCONSISTENCYRETRYPOLICY:
      retryPolicy = DowngradingConsistencyRetryPolicy.INSTANCE;
      break;
    case FALLTHROUGHRETRYPOLICY:
      retryPolicy = FallthroughRetryPolicy.INSTANCE;
      break;
    case LOGGINGRETRYPOLICY:
      retryPolicy = new LoggingRetryPolicy(FallthroughRetryPolicy.INSTANCE);
      break;
    default:
      log.error("Invalid type of retry policy: " + retry.getRetryType().toString()
          + ". DefaultRetryPolicy is used.");
      retryPolicy = new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE);
      break;
    }
    return retryPolicy;
  }

  public static ReconnectionPolicy getReconnectionPolicy(Reconnection reconnection) {
    Preconditions.checkNotNull(reconnection);

    ReconnectionPolicy reconnectionPolicy = null;
    switch(reconnection.getReconnectionType()) {
    case CONSTANTRECONNECTIONPOLICY:
      reconnectionPolicy = new ConstantReconnectionPolicy(reconnection.getBaseDelayMs());
      break;
    case EXPONENTIALRECONNECTIONPOLICY:
      reconnectionPolicy = new ExponentialReconnectionPolicy(reconnection.getBaseDelayMs(), reconnection.getMaxDelayMs());
      break;
    default:
      log.error("Invalid type of reconnection policy: " + reconnection.getReconnectionType().toString()
          + ". ConstantReconnectionPolicy is used.");
      reconnectionPolicy = new ConstantReconnectionPolicy(reconnection.getBaseDelayMs());
      break;
    }
    return reconnectionPolicy;
  }
}
