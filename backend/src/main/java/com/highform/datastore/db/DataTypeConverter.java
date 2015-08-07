/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.twitter.elephantbird.mapreduce.io.ProtobufConverter;

/**
 * Converter for converting values to strings based on their types.
 * @author fei
 *
 */
public class DataTypeConverter {

  private static final Logger log = LoggerFactory.getLogger(DataTypeConverter.class);
  public static final String HEXES = "0123456789ABCDEF";
  public static final String DELIMITER = ",";

  public static <T> String toString(T value) {
    Preconditions.checkNotNull(value);
    if (value instanceof UUID) {
      return ((UUID)value).toString();
    } else if (value instanceof Integer) {
      return ((Integer)value).toString();
    } else if (value instanceof Float) {
      return ((Float)value).toString();
    } else if (value instanceof Double) {
      return ((Double)value).toString();
    } else if (value instanceof Long) {
      return ((Long)value).toString();
    } else if (value instanceof String) {
      // Single quote ' needs to be treated special because CQL use it to quote a string, e.g. '<string>'
      // CQL will treat '' as a ' when it implements a CQL query/command
      String newValue = StringUtils.replace((String)value, "'", "''");
      return "'"+ newValue + "'";
    } else if (value instanceof ByteString) {
      return bytesToHex(((ByteString)value).toByteArray());
    }
    return value.toString();
  }

  public static <T> String toString(List<T> values) {
    Preconditions.checkNotNull(values);
    List<String> strValues = Lists.newArrayList();
    for (T value : values) {
      strValues.add(toString(value));
    }
    return StringUtils.join(strValues, DELIMITER);
  }

  public static String bytesToHex(byte[] value) {
    if ( value == null ) {
      return null;
    }
    StringBuilder hex = new StringBuilder(2 * value.length);
    for ( byte b : value ) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4))
         .append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static Object parseBlob(byte[] bytes, String className)
      throws Exception {
    Preconditions.checkNotNull(bytes);
    Preconditions.checkArgument(bytes.length > 0);
    Preconditions.checkArgument(StringUtils.isNotBlank(className));

    Object result = null;
    Class c = Class.forName(className);

    if (Message.class.isAssignableFrom(c)) {
      result = ProtobufConverter.newInstance(c).fromBytes(bytes);
    } else {
      log.error("Invalid type of binary data " + c.getClass().getName());
    }
    return result;
  }

}
