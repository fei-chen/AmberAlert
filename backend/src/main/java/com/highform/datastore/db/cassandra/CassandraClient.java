/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.highform.config.ConfigHelper;
import com.highform.datastore.DSRecord;
import com.highform.datastore.TableSchemaMap;
import com.highform.datastore.db.BatchCmdBuilder;
import com.highform.datastore.db.Client;
import com.highform.datastore.db.CmdBuilder;
import com.highform.datastore.db.DataTypeConverter;
import com.highform.datastore.db.IndexCmdBuilder;
import com.highform.datastore.db.InsertCmdBuilder;
import com.highform.datastore.db.SelectQueryBuilder;
import com.highform.datastore.db.TableCmdBuilder;
import com.highform.datastore.db.UpdateCmdBuilder;
import com.highform.datastore.db.cassandra.cql.CQLSelectQueryBuilder;
import com.highform.datastore.db.cassandra.cql.CQLTableCmdBuilder;
import com.highform.datastore.db.exceptions.DBException;
import com.highform.datastore.db.exceptions.InvalidTableException;
import com.highform.datastore.proto.Datastore.DataType;
import com.highform.datastore.proto.Datastore.FieldSchema;
import com.highform.datastore.proto.Datastore.Schema;
import com.highform.datastore.proto.Datastore.TableSchema;
import com.highform.db.cassandra.proto.Cassandra.CSConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.retry.RetryPolicy;
import com.netflix.astyanax.serializers.StringSerializer;

/**
 * CassandraClient reads data from Cassandra and updates data to Cassandra
 *
 * @author fei
 *
 */
public class CassandraClient implements Client {
  private static final Logger log = Logger.getLogger(CassandraClient.class);

  private AstyanaxContext<Keyspace> context;
  private Schema keyspaceSchema;
  private Keyspace keyspace;
  private RetryPolicy retryPolicy;

  public static final ColumnFamily<String, String> cql3cf =
      ColumnFamily.newColumnFamily("Cql3CF", StringSerializer.get(), StringSerializer.get());

  public CassandraClient(CSConfig config) {
    this.context = CassandraConfig.getContext(config);
    this.keyspaceSchema = config.getKeyspaceSchema();
    this.retryPolicy = config.hasRetry() ? CassandraConfig.getRetryPolicy(config.getRetry()) : null;
  }

  public CassandraClient(String configFile) {
    this((CSConfig) ConfigHelper.loadConfig(configFile, CSConfig.newBuilder()));
  }

  @Override
  public void connect() {
    this.context.start();
    this.keyspace = this.context.getClient();
    try {
      KeyspaceDefinition ki = this.keyspace.describeKeyspace();
      log.debug("Connect to keyspace: " + ki.getName());
    } catch (Exception e) {
      log.error("Failed to get keyspace defintion of Cassandra DB. Error:" + e.getMessage());
    }
  }

  @Override
  public List<DSRecord<String>> getRecords(SelectQueryBuilder query, String table)
    throws DBException {
    Preconditions.checkNotNull(query);
    final String NULL = "null";

    List<DSRecord<String>> recordList = Lists.newArrayList();
    OperationResult<CqlResult<String, String>> result = executeCql(query.build());

    TableSchemaMap tabSchemaMap = getTabSchemaMap(this.keyspaceSchema, table);

    if (result != null) {
      for (Row<String, String> row : result.getResult().getRows()) {
        ColumnList<String> columns = row.getColumns();
        DSRecord<String> record = DSRecord.newBuilder();
        for (String columnName : row.getColumns().getColumnNames()) {
          Column<String> column = columns.getColumnByName(columnName);
          if (column.hasValue()) {
            String value = getColumnValue(column, columnName, tabSchemaMap);
            record.addField(columnName, value);
          } else {
            record.addField(columnName, NULL);
          }
        }
        recordList.add(record);
      }
    } else {
      System.out.println("No Records found.");
      log.warn("No records found for query.");
    }
    return recordList;
  }

  @Override
  public long getCount(SelectQueryBuilder query)
      throws DBException {
    Preconditions.checkNotNull(query);
    query.count();
    if (!query.hasLimit()) {
      query.limit(Integer.MAX_VALUE);
    }
    OperationResult<CqlResult<String, String>> result = executeCql(query.build());
    return result.getResult().getRows().getRowByIndex(0).getColumns().getColumnByName("count").getLongValue();
  }

  @Override
  public boolean insertRecords(InsertCmdBuilder insertCmd)
      throws DBException {
    Preconditions.checkNotNull(insertCmd);
    OperationResult<CqlResult<String, String>> result = executeCql(insertCmd.build());
    return result == null ? false : true;
  }

  @Override
  public boolean updateRecords(UpdateCmdBuilder updateCmd)
      throws DBException {
    Preconditions.checkNotNull(updateCmd);
    OperationResult<CqlResult<String, String>> result = executeCql(updateCmd.build());
    return result == null ? false : true;
  }

  @Override
  public boolean batchRecords(BatchCmdBuilder batchCmd)
    throws DBException {
    Preconditions.checkNotNull(batchCmd);
    OperationResult<CqlResult<String, String>> result = executeCql(batchCmd.build());
    return result == null ? false : true;
  }

  @Override
  public boolean updateIndex(IndexCmdBuilder indexCmd)
      throws DBException {
    Preconditions.checkNotNull(indexCmd);
    return executeCmd(indexCmd);
  }

  @Override
  public boolean updateTable(TableCmdBuilder tableCmd)
      throws DBException {
    Preconditions.checkNotNull(tableCmd);
    return executeCmd(tableCmd);
  }

  @Override
  public boolean createTableIfNotExist(String table, String schema)
      throws DBException {
    if (isTableExist(table))
      return false;
    TableSchema tabSchema = getTabSchema(this.keyspaceSchema, schema);
    return createTable(table, tabSchema);

  }

  @Override
  public boolean isTableExist(String table)
      throws DBException {
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

  public boolean createTable(String table, TableSchema tableSchema)
      throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkNotNull(tableSchema);

    TableCmdBuilder tableCmd = CQLTableCmdBuilder.create();
    tableCmd.table(table);

    for (FieldSchema fieldSchema : tableSchema.getFieldSchemaList()) {
      tableCmd.columnDatatype(fieldSchema.getName(), fieldSchema.getType());
    }

    List<String> compositeKeys = Lists.newArrayList();
    for (int index : tableSchema.getPrimaryKey().getFieldSchemaIndexList()) {
      compositeKeys.add(tableSchema.getFieldSchema(index).getName());
    }
    tableCmd.compositePrimaryKey(compositeKeys);
    log.info("Create table " + table);
    return updateTable(tableCmd);
  }

  @Override
  public void close() {
    if (this.context != null) {
      this.context.shutdown();
    }
  }

  private boolean executeCmd(CmdBuilder cmd)
      throws DBException {
    OperationResult<CqlResult<String, String>> result = executeCql(cmd.build());
    return result == null ? false : true;
  }

  private OperationResult<CqlResult<String, String>> executeCql(String cql)
      throws DBException {
    Preconditions.checkNotNull(cql);

    // if the cql command is an empty string, then return null
    if (StringUtils.isBlank(cql)) {
      return null;
    }

    log.debug("----Launch CQL 3 Query/Command----\n" + cql);
    OperationResult<CqlResult<String, String>> result = null;
    try {
      if (this.retryPolicy != null) {
        result = this.keyspace
            .prepareQuery(cql3cf)
            .withRetryPolicy(this.retryPolicy)
            .withCql(cql)
            .execute();
      } else {
        result = this.keyspace
            .prepareQuery(cql3cf)
            .withCql(cql)
            .execute();
      }
    } catch (ConnectionException e) {
      log.error("Failed to execute cql cmd : " + cql
          + ". Error " + e.getMessage());
      ExceptionHandler.throwException(e.getMessage());
    }
    return result;
  }

  public static TableSchemaMap getTabSchemaMap(Schema keyspaceSchema, String schemaName) {
    Preconditions.checkNotNull(keyspaceSchema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));

    // throw DBException.InvalidSchemaException
    TableSchemaMap tabSchemaMap = null;
    for (TableSchema t : keyspaceSchema.getTableSchemaList()) {
      if (t.getName().equals(schemaName)) {
        tabSchemaMap = new TableSchemaMap(t);
        break;
      }
    }
    return tabSchemaMap;
  }

  public static TableSchema getTabSchema(Schema keyspaceSchema, String schemaName) {
    Preconditions.checkNotNull(keyspaceSchema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
    TableSchema tabSchema = null;
    for (TableSchema t : keyspaceSchema.getTableSchemaList()) {
      if (t.getName().equals(schemaName)) {
        tabSchema = t;
      }
    }
    return tabSchema;
  }

  public static String getColumnValue(Column<String> column, String columnName, TableSchemaMap tabSchemaMap)
          throws DBException {
    Preconditions.checkNotNull(column);
    Preconditions.checkArgument(StringUtils.isNotBlank(columnName));
    Preconditions.checkNotNull(tabSchemaMap);

    DataType type = tabSchemaMap.getDataType(columnName);

    String value = "";
    switch (type) {
      case INT:
      case VARINT:
        value = Integer.toString(column.getIntegerValue());
        break;
      case VARCHAR:
      case TEXT:
      case ASCII:
        value = column.getStringValue();
        break;
      case UUID:
      case TIMEUUID:
        value = column.getUUIDValue().toString();
        break;
      case FLOAT:
      case DECIMAL:
        value = Float.toString(column.getFloatValue());
        break;
      case DOUBLE:
        value = Double.toString(column.getDoubleValue());
        break;
      case BINARY:
        byte[] bytes = column.getByteArrayValue();
        String blobClass = tabSchemaMap.getBinaryClass(columnName);
        if (StringUtils.isNotBlank(blobClass)) {
          try {
              DataTypeConverter.parseBlob(bytes, blobClass);
          } catch (Exception e) {
            log.error("Failed to parseBlob " + e.getMessage());
          }
        }
        // TODO remove the following line after change the output of getRecords() to List<Records>
        value = DataTypeConverter.bytesToHex(column.getByteArrayValue());
        break;
      case INET:
      case TIMESTAMP:
      case COUNTER:
      case LONG:
        value = Long.toString(column.getLongValue());
        break;
      case BOOLEAN:
        value = Boolean.toString(column.getBooleanValue());
        break;
      default:
        ExceptionHandler.throwException("Unsupported type found in Cassandra record.");
    }
    return value;
  }
}