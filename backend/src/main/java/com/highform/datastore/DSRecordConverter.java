// Copyright at HighForm Inc. 2013. All rights reserved.
package com.highform.datastore;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.highform.datastore.db.DataTypeConverter;
import com.highform.datastore.exceptions.DatastoreException;
import com.highform.datastore.exceptions.InvalidDataTypeException;
import com.highform.datastore.proto.Datastore.DataType;
import com.highform.datastore.proto.Datastore.Field;

/**
 *
 */
public class DSRecordConverter {
  private static final Logger log = LoggerFactory.getLogger(DSRecordConverter.class);
  private static final String KEY = "key";
  private static final String FIELD = "Field";
  private static final String DELIMITER = ",";

  public static <T extends Message> List<DSRecord<Object>> getDSRecords(List<T> messages, TableSchemaMap schemaMap) {
    Preconditions.checkNotNull(messages);
    Preconditions.checkNotNull(schemaMap);

    List<DSRecord<Object>> records = Lists.newArrayList();
    for (Message message : messages) {
      DSRecord<Object> record = getDSRecord(message, schemaMap);
      if (record != null) {
        records.add(record);
      }
    }
    return records;
  }

  /**
   * Builds an IndexBuilder with the keys being the protobuf field names and
   * values being the field values
   *
   * @param protoMessage
   * @return
   */
  public static DSRecord<Object> getDSRecord(Message message, TableSchemaMap schemaMap) {
    Preconditions.checkNotNull(message);
    Preconditions.checkNotNull(schemaMap);

    String key = getProtoKey(message);
    Map<String, Object> protoMap = getProtoMap(message, schemaMap);

    return new DSRecord<Object>(key, protoMap);
  }

  public static String getProtoKey(Message message) {
    Preconditions.checkNotNull(message);

    List<String> partialKeys = Lists.newArrayList();
    Map<FieldDescriptor, Object> fields = message.getAllFields();
    for (Entry<FieldDescriptor, Object> entry : fields.entrySet()) {
      FieldDescriptor fieldDescriptor = entry.getKey();
      Object value = entry.getValue();

      String partialKey = "";
      JavaType fieldJavaType = fieldDescriptor.getJavaType();
      if (fieldDescriptor.getName().equals(KEY)) {
        partialKey = (String) message.getField(fieldDescriptor);
      } else if (fieldJavaType.equals(JavaType.MESSAGE)) {
        partialKey = getProtoKey((Message) value);
      }
      if (StringUtils.isNotBlank(partialKey)) {
        partialKeys.add(partialKey);
      }
    }
    return StringUtils.join(partialKeys, DELIMITER);
  }

  public static String getProtoHexString(Message message) {
    Preconditions.checkNotNull(message);

    return DataTypeConverter.bytesToHex(message.toByteArray());
  }

  /**
   * Builds a map of the protobuf field name and values
   *
   * @param message
   * @return
   */
  public static Map<String, Object> getProtoMap(Message message, TableSchemaMap schemaMap) {
    Preconditions.checkNotNull(message);
    Preconditions.checkNotNull(schemaMap);

    Map<FieldDescriptor, Object> fields = message.getAllFields();
    Map<String, Object> protoMap = Maps.newHashMap();

    for (Entry<FieldDescriptor, Object> entry : fields.entrySet()) {
      // if the field name is Field
      FieldDescriptor fieldDescriptor = entry.getKey();
      Object value = entry.getValue();

      JavaType fieldJavaType = fieldDescriptor.getJavaType();
      if (fieldJavaType.equals(JavaType.MESSAGE)) {
        String fieldMessageType = fieldDescriptor.getMessageType().getName();
        if (fieldMessageType.equals(FIELD)) { // if it is a Field message
          Field field = (Field) value;
          try {
            protoMap.put(field.getName(), getValue(field, schemaMap));
          } catch (DatastoreException e) {
            log.error("Failed to get value for the field " + field.getName() + ". Error: " + e.getMessage());
          }
        } else {// if it is a non-field message
          Map<String, Object> subMap = getProtoMap((Message) value, schemaMap);
          if (!subMap.isEmpty()) {
            protoMap.putAll(subMap);
          }
        }
      }
    }
    return protoMap;
  }

  /**
   * Get value for a given field
   *
   * @param field
   * @param schemaMap
   * @return
   * @throws InvalidDataTypeException
   */
  private static Object getValue(Field field, TableSchemaMap schemaMap) throws InvalidDataTypeException {
    Preconditions.checkNotNull(field);
    Preconditions.checkNotNull(schemaMap);

    Object value = null;
    DataType type = schemaMap.getDataType(field.getName());
    String valueStr = field.getValue();
    switch (type) {
    case ASCII:
    case TEXT:
    case VARCHAR:
      value = valueStr;
      break;
    case INT:
    case VARINT:
      value = Integer.parseInt(valueStr);
      break;
    case LONG:
    case INET:
    case TIMESTAMP:
    case COUNTER:
      value = Long.parseLong(valueStr);
      break;
    case FLOAT:
    case DECIMAL:
      value = Float.parseFloat(valueStr);
      break;
    case DOUBLE:
      value = Double.parseDouble(valueStr);
      break;
    case BOOLEAN:
      value = Boolean.parseBoolean(valueStr);
      break;
    case UUID:
    case TIMEUUID:
      value = UUID.fromString(valueStr);
      break;
    case BINARY:
      value = valueStr;
      break;
    default:
      throw new InvalidDataTypeException(type.name());
    }
    return value;
  }
}
