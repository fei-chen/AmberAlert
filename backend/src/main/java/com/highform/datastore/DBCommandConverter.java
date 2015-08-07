/**
 * Copyright at HighForm Inc. All rights reserved Aug 25, 2013.
 */

package com.highform.datastore;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import com.highform.datastore.db.BatchCmdBuilder;
import com.highform.datastore.db.InsertCmdBuilder;
import com.highform.datastore.db.cassandra.cql.CQLBatchCmdBuilder;
import com.highform.datastore.db.cassandra.cql.CQLInsertCmdBuilder;

/**
 * Convert DSRecord to DB command converter
 *
 * @author fei
 */
public class DBCommandConverter {
  private static final String KEY = "key";

  /**
   * get an insert batch command from data
   * 
   * @param data
   * @param table
   * @param column
   * @return
   */
  public static <T extends Message> BatchCmdBuilder getInsertBatchCmd(List<T> data, String table, String column) {
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkArgument(StringUtils.isNotBlank(column));

    BatchCmdBuilder batchCmd = new CQLBatchCmdBuilder();
    for (T message : data) {
      InsertCmdBuilder cmd = getInsertCmd(message, table, column);
      batchCmd.add(cmd);
    }
    return batchCmd;
  }

  /**
   * get an insert command from data
   * 
   * @param message
   * @param table
   * @param column
   * @return
   */
  public static <T extends Message> InsertCmdBuilder getInsertCmd(T message, String table, String column) {
    Preconditions.checkNotNull(message);
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkArgument(StringUtils.isNotBlank(column));

    String key = DSRecordConverter.getProtoKey(message);
    String value = DSRecordConverter.getProtoHexString(message);

    InsertCmdBuilder cmd = new CQLInsertCmdBuilder()
        .table(table)
        .columnValue(KEY, key)
        .columnValue(column, value);

    return cmd;
  }
}
