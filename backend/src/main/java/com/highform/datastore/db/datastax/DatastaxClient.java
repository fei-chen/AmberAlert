/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 10, 2013.
 */

package com.bloomreach.db.datastax;

import java.nio.ByteBuffer;
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
import com.bloomreach.db.cassandra.ExceptionHandler;
import com.bloomreach.db.cassandra.TabSchemaMap;
import com.bloomreach.db.cassandra.cql.CQLSelectQueryBuilder;
import com.bloomreach.db.cassandra.cql.CQLTableCmdBuilder;
import com.bloomreach.db.cassandra.proto.CassandraSchema.ColumnSchema;
import com.bloomreach.db.cassandra.proto.CassandraSchema.DataType;
import com.bloomreach.db.cassandra.proto.CassandraSchema.KeyspaceSchema;
import com.bloomreach.db.cassandra.proto.CassandraSchema.TabSchema;
import com.bloomreach.db.datastax.proto.Datastax.Config;
import com.bloomreach.db.exceptions.DBException;
import com.bloomreach.db.exceptions.InvalidTableException;
import com.bloomreach.db.mysql.sql.SQLTableCmdBuilder;
import com.bloomreach.index.RecordBuilder;
import com.bloomreach.utils.ProtoBufUtils;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * DatastaxClient reads data from Cassandra and updates data to Cassandra
 *
 * @author fei
 */
public class DatastaxClient implements Client {
  private static final Logger log = LoggerFactory.getLogger(DatastaxClient.class);
  private static final String COUNT = "count";

  private Cluster cluster;
  private Session session;
  private String keyspace;
  private KeyspaceSchema keyspaceSchema;

  public DatastaxClient(Config config) {
    this.cluster = DatastaxClientConfig.getCluster(config);
    this.keyspace = config.getKeyspace();
    this.keyspaceSchema = config.getKeyspaceSchema();

  }

  public DatastaxClient(String configFile) {
    this((Config) ConfigHelper.loadConfig(configFile, Config.newBuilder()));
  }

  @Override
  public void close() {
    if (this.session != null) {
      this.session.shutdown();
    }

    if (this.cluster != null) {
      this.cluster.shutdown();
    }
  }

  @Override
  public void connect() {
    try {
      this.session = this.cluster.connect(this.keyspace);
    } catch (Exception e) {
      log.error("Failed to get keyspace defintion of Cassandra DB. Error:" + e.getMessage());
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

    List<RecordBuilder<Object>> records = Lists.newArrayList();

    ResultSet result = (ResultSet) executeQuery(query.build());

    if (result != null) {
      TabSchemaMap tabSchemaMap = getTabSchemaMap(this.keyspaceSchema, table);
      List<String> columnNames = Lists.newArrayList();
      for (Definition columnDefinition : result.getColumnDefinitions()) {
        String columnName = columnDefinition.getName();
        columnNames.add(columnName);
      }

      for (Row row : result) {
        RecordBuilder<Object> record = RecordBuilder.newBuilder();
        for (String columnName : columnNames) {
          if (!row.isNull(columnName)) {
            Object value = getColumnValue(row, columnName, tabSchemaMap);
            record.addField(columnName, value);
          } else {
            record.addField(columnName, null);
          }
        }
        records.add(record);
      }
    } else {
      log.warn("No records found for query.");
    }
    return records;
  }

  @Override
  public long getCount(SelectQueryBuilder query) throws DBException {
    Preconditions.checkNotNull(query);
    query.count();
    if (!query.hasLimit()) {
      query.limit(Integer.MAX_VALUE);
    }

    long count = 0;
    ResultSet result = (ResultSet) executeQuery(query.build());
    for (Row row : result) {
      count  = row.getLong(COUNT);
      break;
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
    Preconditions.checkNotNull(updateCmd);
    return executeCmd(updateCmd.build());
  }

  @Override
  public boolean batchRecords(BatchCmdBuilder batchCmd) throws DBException {
    Preconditions.checkNotNull(batchCmd);
    return executeCmd(batchCmd.build());
  }

  @Override
  public boolean updateIndex(IndexCmdBuilder indexCmd) throws DBException {
    Preconditions.checkNotNull(indexCmd);
    return executeCmd(indexCmd.build());
  }

  @Override
  public boolean updateTable(TableCmdBuilder tableCmd) throws DBException {
    Preconditions.checkNotNull(tableCmd);
    return executeCmd(tableCmd.build());
  }

  @Override
  public boolean isTableExist(String table) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));

    SelectQueryBuilder query = new CQLSelectQueryBuilder();
    query.table(table)
         .limit(1);
    try {
      getCount(query);
      return true;
    } catch (DBException e) {
      if (e instanceof InvalidTableException) {
        log.info("Table " + table + " does not exist");
        return false;
      }
      throw e;
    }
  }

  @Override
  public boolean createTableIfNotExist(String table, String schema) throws DBException {
    if (isTableExist(table))
      return false;
    TabSchema tabSchema = getTabSchema(this.keyspaceSchema, schema);
    return createTable(table, tabSchema);
  }

  public boolean createTable(String table, TabSchema tabSchema)
      throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkNotNull(tabSchema);

    TableCmdBuilder tableCmd = CQLTableCmdBuilder.create();
    tableCmd.table(table);

    for (ColumnSchema columnSchema : tabSchema.getColumnSchemaList()) {
      tableCmd.columnDatatype(columnSchema.getName(), columnSchema.getType());
    }

    List<String> compositeKeys = Lists.newArrayList();
    for (int index : tabSchema.getPrimaryKey().getColumnSchemaIndexList()) {
      compositeKeys.add(tabSchema.getColumnSchema(index).getName());
    }
    tableCmd.compositePrimaryKey(compositeKeys);
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

  public static TabSchemaMap getTabSchemaMap(KeyspaceSchema keyspaceSchema, String schemaName) {
    Preconditions.checkNotNull(keyspaceSchema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));

    // throw DBException.InvalidSchemaException
    TabSchemaMap tabSchemaMap = null;
    for (TabSchema t : keyspaceSchema.getTableSchemaList()) {
      if (t.getName().equals(schemaName)) {
        tabSchemaMap = new TabSchemaMap(t);
        break;
      }
    }
    return tabSchemaMap;
  }

  public static TabSchema getTabSchema(KeyspaceSchema keyspaceSchema, String schemaName) {
    Preconditions.checkNotNull(keyspaceSchema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
    TabSchema tabSchema = null;
    for (TabSchema t : keyspaceSchema.getTableSchemaList()) {
      if (t.getName().equals(schemaName)) {
        tabSchema = t;
      }
    }
    return tabSchema;
  }

  @Override
  public Object executeQuery(String query) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(query));
    return executeCql(query);
  }

  @Override
  public boolean executeCmd(String cmd) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(cmd));
    ResultSet result = executeCql(cmd);
    return result == null ? false : true;
  }

  private ResultSet executeCql(String cql) throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(cql));

    log.debug("----Launch CQL 3 Query/Command----\n" + cql);
    ResultSet result = null;
    try {     
      // Create a BoundStatement to set consistency level to ALL 
      BoundStatement boundStatemen = this.session.prepare(cql).setRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE).setConsistencyLevel(ConsistencyLevel.THREE).bind();
      log.debug("Using consistency level: " + ConsistencyLevel.THREE + " with downgrad policy.");
      result = this.session.execute(boundStatemen);
    } catch (Exception e) {
      log.error("Failed to execute cql cmd: " + cql
          + ". Error " + e.getMessage());
      throw new DBException(e);
    }
    return result;
  }

  public static Object getColumnValue(Row row, String columnName, TabSchemaMap tabSchemaMap)
      throws DBException {
    Preconditions.checkNotNull(row);
    Preconditions.checkArgument(StringUtils.isNotBlank(columnName));
    Preconditions.checkNotNull(tabSchemaMap);

    DataType type = tabSchemaMap.getDataType(columnName);
    Preconditions.checkNotNull(type, columnName + " must be defined in the table_schema in the datastax config file");

    Object value = null;
    switch (type) {
      case INT:
      case VARINT:
        value = Integer.valueOf(row.getInt(columnName));
        break;
      case VARCHAR:
      case TEXT:
      case ASCII:
        value = row.getString(columnName);
        break;
      case UUID:
      case TIMEUUID:
        value = row.getUUID(columnName);
        break;
      case FLOAT:
      case DECIMAL:
        value = Float.valueOf(row.getFloat(columnName));
        break;
      case DOUBLE:
        value = Double.valueOf(row.getDouble(columnName));
        break;
      case BLOB:
        ByteBuffer bytes = row.getBytes(columnName);
        String blobClass = tabSchemaMap.getBlobClass(columnName);
        if (StringUtils.isNotBlank(blobClass)) {
          try {
            value = ProtoBufUtils.fromCompressedByteBuffer(bytes, blobClass);
          } catch (Exception e) {
            log.error("Failed to parseBlob " + e.getMessage());
          }
        }
        break;
      case INET:
        value = row.getInet(columnName);
        break;
      case TIMESTAMP:
        value = Long.valueOf(row.getDate(columnName).getTime());
        break;
      case COUNTER:
      case BIGINT:
        value = Long.valueOf(row.getLong(columnName));
        break;
      case BOOLEAN:
        value = Boolean.valueOf(row.getBool(columnName));
        break;
      default:
        ExceptionHandler.throwException("Unsupported type found in Cassandra record.");
    }
    return value;
  }
}
