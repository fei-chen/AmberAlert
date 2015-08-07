/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 8, 2013.
 */

package com.bloomreach.db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.config.ConfigHelper;
import com.bloomreach.db.BatchCmdBuilder;
import com.bloomreach.db.Client;
import com.bloomreach.db.IndexCmdBuilder;
import com.bloomreach.db.InsertCmdBuilder;
import com.bloomreach.db.SelectQueryBuilder;
import com.bloomreach.db.TableCmdBuilder;
import com.bloomreach.db.UpdateCmdBuilder;
import com.bloomreach.db.exceptions.DBException;
import com.bloomreach.db.exceptions.InvalidTableException;
import com.bloomreach.db.mysql.proto.Mysql.ColumnSchema;
import com.bloomreach.db.mysql.proto.Mysql.Config;
import com.bloomreach.db.mysql.proto.Mysql.DBSchema;
import com.bloomreach.db.mysql.proto.Mysql.TabSchema;
import com.bloomreach.db.mysql.sql.SQLSelectQueryBuilder;
import com.bloomreach.db.mysql.sql.SQLTableCmdBuilder;
import com.bloomreach.index.RecordBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * MysqlClient reads data from Mysql and updates data to Mysql
 *
 * @author fei
 */
public class MysqlClient implements Client {
  private static final Logger log = LoggerFactory.getLogger(MysqlClient.class);

  private Connection connection;
  private Config config;
  private DBSchema dbSchema;
  private static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static String JDBC_URL_PREFIX = "jdbc:mysql://";

  public MysqlClient(Config config) {
    Preconditions.checkNotNull(config);
    this.config = config;
    this.dbSchema = config.getDbSchema();
  }

  public MysqlClient(String configFile) {
    this((Config) ConfigHelper.loadConfig(configFile, Config.newBuilder()));
  }

  /**
   * Convert the sql result set to RecordBuilder and the like.
   * 
   * @param result
   * @return
   */
  public static List<RecordBuilder<Object>> convertResultSetToRecordBuilders(ResultSet result) throws DBException {
    Preconditions.checkNotNull(result);

    List<RecordBuilder<Object>> records = Lists.newArrayList();

    try {
      List<String> columnNames = Lists.newArrayList();
      ResultSetMetaData resultMetadata = result.getMetaData();

      for (int i = 1; i < resultMetadata.getColumnCount() + 1; i++) {
        String columnName = resultMetadata.getColumnName(i);
        columnNames.add(columnName);
      }

      while (result.next()) {
        RecordBuilder<Object> record = RecordBuilder.newBuilder();
        for (String columnName : columnNames) {
          Object value = result.getObject(columnName);
          record.addField(columnName, value);
        }
        records.add(record);
      }
    } catch (SQLException e) {
      log.error("Failed to parse resultset: " + result.toString() + ". Error: " + e.getMessage());
      ExceptionHandler.throwException(e);
    }
    return records;
  }

  @Override
  public void close() {
    if (this.connection != null) {
      try {
        if (!this.connection.isClosed()) {
          this.connection.close();
        }
      } catch (SQLException e) {
        log.error("Failed to close mysql connection. Error: " + e.getMessage());
      }
    }
  }

  @Override
  public void connect() {
    String jdbcUrl = JDBC_URL_PREFIX + this.config.getHost() + ":" + this.config.getPort() + "/" + this.config.getDb();

    if (this.config.hasAutoReconnect() && this.config.getAutoReconnect() == true) {
      jdbcUrl += "?autoReconnect=true";
    }

    try {
      Class.forName(JDBC_DRIVER);
      this.connection = DriverManager.getConnection(jdbcUrl, this.config.getUser(), this.config.getPassword());
    } catch (ClassNotFoundException e) {
      log.error("Failed to find " + JDBC_DRIVER + ". Error: " + e.getMessage());
    } catch (SQLException e) {
      log.error("Failed to connect to Mysql DB. Error: " + e.getMessage());
    }
  }

  @Override
  public List<RecordBuilder<String>> getRecords(SelectQueryBuilder query, String table) throws DBException {
    Preconditions.checkNotNull(query);
    Preconditions.checkArgument(StringUtils.isNotBlank(table));

    List<RecordBuilder<Object>> records = getObjRecords(query, table);
    List<RecordBuilder<String>> strRecords = Lists.newArrayList();
    for (RecordBuilder<Object> record : records) {
      strRecords.add(record.getStringRecord());
    }
    return strRecords;
  }

  @Override
  public List<RecordBuilder<Object>> getObjRecords(SelectQueryBuilder query, String table) throws DBException {
    Preconditions.checkNotNull(query);
    Preconditions.checkArgument(StringUtils.isNotBlank(table));

    ResultSet result = (ResultSet) executeQuery(query.build());
    return convertResultSetToRecordBuilders(result);
  }

  @Override
  public long getCount(SelectQueryBuilder query) throws DBException {
    Preconditions.checkNotNull(query);

    long count = 0;

    query.count();
    ResultSet result = (ResultSet) executeQuery(query.build());
    try {
      result.next();
      count = result.getLong(1);
    } catch (SQLException e) {
      log.error("Failed to get count for the query: " + query + ". Error: " + e.getMessage());
      ExceptionHandler.throwException(e);
    }

    return count;
  }

  @Override
  public boolean insertRecords(InsertCmdBuilder insertCmd) throws DBException {
    Preconditions.checkNotNull(insertCmd);
    return executeCmd(insertCmd.build());
  }

  @Override
  public boolean updateRecords(UpdateCmdBuilder updateCmd) throws DBException {
    // TODO Auto-generated method stub
    Preconditions.checkArgument(false, "updateRecords is not supported");
    return false;
  }

  @Override
  public boolean batchRecords(BatchCmdBuilder batchCmd) throws DBException {
    Preconditions.checkNotNull(batchCmd);

    log.debug("----Launch SQL Query/Command----\n" + batchCmd.build());
    boolean isSuccessful = true;

    try {
      Statement statement = this.connection.createStatement();
      for (String cmd : batchCmd.getCmdList()) {
        if (StringUtils.isNotBlank(cmd)) {
          statement.addBatch(cmd);
        } else {
          log.warn("Empty command in batch");
        }
      }
      statement.executeBatch();
    } catch (SQLException e) {
      log.error("Failed to execute sql batch cmd: " + batchCmd.build() + ". Error: " + e.getMessage());
      ExceptionHandler.throwException(e);
    }

    return isSuccessful;
  }

  @Override
  public boolean updateIndex(IndexCmdBuilder indexCmd) throws DBException {
    // TODO Auto-generated method stub
    Preconditions.checkArgument(false, "updateIndex is not supported");
    return false;
  }

  @Override
  public boolean updateTable(TableCmdBuilder tableCmd) throws DBException {
    Preconditions.checkNotNull(tableCmd);
    return executeCmd(tableCmd.build());
  }

  @Override
  public boolean isTableExist(String table) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));

    SelectQueryBuilder query = new SQLSelectQueryBuilder();
    query.table(table).startFrom(0).limit(1);
    try {
      getCount(query);
      return true;
    } catch (DBException e) {
      if (e instanceof InvalidTableException) {
        log.info("Table " + table + " does not exist");
        return false;
      }
      throw new DBException(e);
    }
  }

  @Override
  public boolean createTableIfNotExist(String table, String schema) throws DBException {
    if (isTableExist(table))
      return false;
    TabSchema tabSchema = getTabSchema(this.dbSchema, schema);
    return createTable(table, tabSchema);
  }

  public boolean createTable(String table, TabSchema tabSchema) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkNotNull(tabSchema);

    TableCmdBuilder tableCmd = SQLTableCmdBuilder.create();
    tableCmd.table(table);

    for (ColumnSchema columnSchema : tabSchema.getColumnSchemaList()) {
      tableCmd.columnDatatype(columnSchema.getName(), columnSchema.getType(), columnSchema.getParams());
    }

    String primaryKey = "";
    // mysql table can only define one key
    for (int index : tabSchema.getPrimaryKey().getColumnSchemaIndexList()) {
      primaryKey = tabSchema.getColumnSchema(index).getName();
      break;
    }
    tableCmd.primaryKey(primaryKey);
    log.info("Create table " + table);
    return updateTable(tableCmd);
  }

  @Override
  public boolean dropTable(String table) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));

    boolean isSuccesful = true;
    if (isTableExist(table)) {
      TableCmdBuilder tableCmd = SQLTableCmdBuilder.drop();
      tableCmd.table(table);
      log.info("Drop table" + table);
      isSuccesful = updateTable(tableCmd);
    }
    return isSuccesful;
  }

  @Override
  public Object executeQuery(String query) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(query));

    log.debug("----Launch SQL Query/Command----\n" + query);
    ResultSet result = null;

    try {
      Statement statement = this.connection.createStatement();
      result = statement.executeQuery(query);
    } catch (Exception e) {
      log.error("Failed to execute sql query: " + query + ". Error: " + e.getMessage());
      ExceptionHandler.throwException(e);
    }

    return result;
  }

  @Override
  public boolean executeCmd(String cmd) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(cmd));

    log.debug("----Launch SQL Query/Command----\n" + cmd);
    boolean isSuccessful = true;

    try {
      Statement statement = this.connection.createStatement();
      statement.execute(cmd);
    } catch (Exception e) {
      log.error("Failed to execute sql cmd: " + cmd + ". Error: " + e.getMessage());
      ExceptionHandler.throwException(e);
    }

    return isSuccessful;
  }

  public static TabSchema getTabSchema(DBSchema dbSchema, String schemaName) {
    Preconditions.checkNotNull(dbSchema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
    TabSchema tabSchema = null;
    for (TabSchema t : dbSchema.getTableSchemaList()) {
      if (t.getName().equals(schemaName)) {
        tabSchema = t;
      }
    }
    return tabSchema;
  }
}
