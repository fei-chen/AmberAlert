/**
 * Copyright 2012 Bloomreach.com. All rights reserved.
 */
package com.bloomreach.db.mysql;

import java.beans.PropertyVetoException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import com.google.gdata.util.common.base.Preconditions;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import com.bloomreach.db.DataSourceFactory;
/**
 * Create a ComboPoolDataSource 
 * @author steve
 *
 */
public class ComboPooledDataSourceFactory implements DataSourceFactory {

  private final Map props;

  // We are assuming that the clients here are using mysql.  Therefore, we want to make
  // sure we are able to find the driver when this is instantiated and throw a clear error
  // if it is not found.
  static {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public ComboPooledDataSourceFactory() {
    this(getDefaultProperties());
  }

  public ComboPooledDataSourceFactory(Map props) {
    Preconditions.checkNotNull(props);
    this.props = props;
  }

  @Override
  public DataSource create(String jdbcurl) throws SQLException {
    DataSource dsUnpooled = DataSources.unpooledDataSource(jdbcurl);
    return DataSources.pooledDataSource(dsUnpooled, props);
  }

  @Override
  public void destroy(DataSource ds) {
    try {
      DataSources.destroy(ds);
    } catch (SQLException e) {
      // no worries if it fails
    }
  }

  private static Map getDefaultProperties() {
    Map props = new HashMap();
    props.put("idleConnectionTestPeriod", new Integer(1200));
    props.put("maxConnectionAge", new Integer(36000));
    props.put("maxIdleTime", new Integer(18000));
    props.put("maxIdleTimeExcessConnections", new Integer(300));
    props.put("initialPoolSize", new Integer(20));
    props.put("numHelperThreads", new Integer(30));
    props.put("minPoolSize", new Integer(20));
    props.put("acquireIncrement", new Integer(5));
    props.put("maxPoolSize", new Integer(100));
    props.put("maxStatements", new Integer(1000));
    props.put("maxStatementsPerConnection", new Integer(100));
    props.put("preferredTestQuery", "select 1");

    return props;
  }
}
