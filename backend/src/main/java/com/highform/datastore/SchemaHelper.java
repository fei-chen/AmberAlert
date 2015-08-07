/**
 * Copyright at HighForm Inc. All rights reserved Mar 12, 2013.
 */

package com.highform.datastore;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.highform.datastore.proto.Datastore.Schema;
import com.highform.datastore.proto.Datastore.TableSchema;

/**
 * IndexSchema helper
 * 
 * @author fei
 */
public class SchemaHelper {
  public static TableSchemaMap getIndexSchemaMap(Schema schema, String schemaName) {
    Preconditions.checkNotNull(schema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
    TableSchemaMap tableSchemaMap = null;
    for (TableSchema tableSchema : schema.getTableSchemaList()) {
      if (tableSchema.getName().equals(schemaName)) {
        tableSchemaMap = new TableSchemaMap(tableSchema);
        break;
      }
    }
    return tableSchemaMap;
  }

  public static TableSchema getIndexSchema(Schema schema, String schemaName) {
    Preconditions.checkNotNull(schema);
    Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
    TableSchema resTableSchema = null;
    for (TableSchema tableSchema : schema.getTableSchemaList()) {
      if (tableSchema.getName().equals(schemaName)) {
        resTableSchema = tableSchema;
        break;
      }
    }
    return resTableSchema;
  }

  /**
   * Convert ...
   * 
   * @param schema
   * @param indices
   * @return
   */
  public static List<List<String>> recordBuilderToString(Schema schema, List<DSRecord<Object>> records) {
    Preconditions.checkNotNull(schema);
    Preconditions.checkNotNull(records);
    List<List<String>> newRecords = Lists.newArrayList();

    return newRecords;
  }

  /**
   * TODO
   * Convert ...
   * 
   * @param schema
   * @param records
   * @return
   */
  public static List<DSRecord<Object>> stringtoRecordBuilder(Schema schema, List<List<String>> records) {
    Preconditions.checkNotNull(schema);
    Preconditions.checkNotNull(records);

    List<DSRecord<Object>> newRecords = Lists.newArrayList();

    return newRecords;
  }
}
