package com.highform.datastore;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.highform.datastore.proto.Datastore.DataType;
import com.highform.datastore.proto.Datastore.FieldSchema;
import com.highform.datastore.proto.Datastore.TableSchema;

public class TableSchemaMap {
  private final static Logger log = LoggerFactory.getLogger(TableSchemaMap.class);
  private Map<String, DataType> fieldToDataTypeMap;
  private Map<String, String> binaryToClassMap;
  private String uniqueKey;

  public TableSchemaMap() {
    this(null);
  }

  public TableSchemaMap(TableSchema tableSchema) {
    this.fieldToDataTypeMap = Maps.newHashMap();
    this.binaryToClassMap = Maps.newHashMap();
    this.uniqueKey = null;
    if (tableSchema != null) {
      setTableSchemaMap(tableSchema);
      this.uniqueKey = tableSchema.getFieldSchema(tableSchema.getPrimaryKey().getFieldSchemaIndex(0)).getName();
    }
  }

  public void setTableSchemaMap(TableSchema tableSchema) {
    Preconditions.checkNotNull(tableSchema);

    for (FieldSchema fieldSchema : tableSchema.getFieldSchemaList()) {
      this.fieldToDataTypeMap.put(fieldSchema.getName(), fieldSchema.getType());
      if (fieldSchema.getType().equals(DataType.BINARY) && fieldSchema.hasBinaryClass()) {
        this.binaryToClassMap.put(fieldSchema.getName(), fieldSchema.getBinaryClass());
      }
    }
  }

  public DataType getDataType(String field) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    return this.fieldToDataTypeMap.get(field);
  }

  public String getBinaryClass(String field) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    return this.binaryToClassMap.get(field);
  }

  public boolean containsField(String field) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    return this.fieldToDataTypeMap.containsKey(field);
  }

  public String getUniqueKey() {
    return this.uniqueKey;
  }

  public Set<String> getFieldNames() {
    Preconditions.checkNotNull(this.fieldToDataTypeMap);
    return this.fieldToDataTypeMap.keySet();
  }
}
