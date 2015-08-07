/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 8, 2013.
 */

package com.bloomreach.db.mysql;

import com.bloomreach.db.Client;
import com.bloomreach.db.ClientFactory;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * MysqlClient reads data from Mysql and updates data to Mysql
 *
 * @author shaochuan.wang
 */

public class MysqlClientFactory implements ClientFactory {
  private static final Logger log = LoggerFactory.getLogger(MysqlClientFactory.class);

  private String mysqlConfig;
  public MysqlClientFactory(String mysqlConfig) {
    Preconditions.checkArgument(StringUtils.isNotBlank(mysqlConfig));
    this.mysqlConfig = mysqlConfig;
  }

  @Override
  public Client newClient() {
    return new MysqlClient(this.mysqlConfig);
  }
}