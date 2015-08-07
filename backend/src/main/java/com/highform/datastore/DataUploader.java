/**
 * Copyright at HighForm Inc. All rights reserved Jul 21, 2013.
 */

package com.highform.datastore;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.highform.datastore.db.BatchCmdBuilder;
import com.highform.datastore.db.Client;
import com.highform.datastore.db.exceptions.DBException;
import com.highform.datastore.index.IClient;
import com.highform.datastore.index.exceptions.IndexException;

/**
 * Upload data to datastore
 * 
 * @author fei
 */
public class DataUploader {
  private static final Logger log = LoggerFactory.getLogger(DataUploader.class);
  private static int BATCH_NUM = 20;

  // constant variables for course data
  private static String COURSE_TABLE = "course";
  private static String COURSE_COLUMN = "course_data";
  private static String COURSE_COLLECTION = "course";

  // constant variables for course data
  private static String BOOK_TABLE = "book";
  private static String BOOK_COLUMN = "book_data";
  private static String BOOK_COLLECTION = "book";

  private final Client dbClient;
  private final IClient indexClient;
  private final TableSchemaMap schemaMap;

  public DataUploader(Client dbClient, IClient indexClient, TableSchemaMap schemaMap) {
    this.dbClient = Preconditions.checkNotNull(dbClient);
    this.indexClient = Preconditions.checkNotNull(indexClient);
    this.schemaMap = schemaMap;
  }

  /**
   * upload course data to datastore
   *
   * @param data
   */
  public <T extends Message> void uploadCourseData(List<T> data) {
    Preconditions.checkNotNull(data);

    List<T> partialData = Lists.newArrayList();
    for (int from = 0, to = BATCH_NUM; from < data.size(); from += BATCH_NUM, to += BATCH_NUM) {
      partialData = data.subList(from, Math.min(to, data.size()));
      uploadData(partialData, COURSE_TABLE, COURSE_COLUMN, COURSE_COLLECTION);
    }
  }

  /**
   * upload book data to datastore
   *
   * @param data
   */
  public <T extends Message> void uploadBookData(List<T> data) {
    Preconditions.checkNotNull(data);

    List<T> partialData = Lists.newArrayList();
    for (int from = 0, to = BATCH_NUM; from < data.size(); from += BATCH_NUM, to += BATCH_NUM) {
      partialData = data.subList(from, Math.min(to, data.size()));
      uploadData(partialData, BOOK_TABLE, BOOK_COLUMN, BOOK_COLLECTION);
    }
  }

  /**
   * Upload data to cassandra and elastic search
   *
   * @param data
   * @return
   */
  public <T extends Message> boolean uploadData(List<T> data, String table, String column, String collection) {
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkArgument(StringUtils.isNotBlank(collection));

    return uploadDataToDb(data, table, column) && uploadDataToIndex(data, collection);
  }

  /**
   * Upload data to db
   *
   * @param records
   * @return
   */
  public <T extends Message> boolean uploadDataToDb(List<T> data, String table, String column) {
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    Preconditions.checkArgument(StringUtils.isNotBlank(column));

    boolean isSuccess = true;
    if (!data.isEmpty()) {
      try {
        BatchCmdBuilder batchCmd = DBCommandConverter.getInsertBatchCmd(data, table, column);
        this.dbClient.batchRecords(batchCmd);
      } catch (DBException e) {
        isSuccess = false;
        log.error("Failed to upload data to db. Table: " + table + ", Column: " + column + ". Error: " + e.getMessage());
        log.error(e.getStackTrace().toString());
      }
    }
    return isSuccess;
  }

  /**
   * Upload data to index
   *
   * @param records
   * @return
   */
  public <T extends Message> boolean uploadDataToIndex(List<T> data, String collection) {
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(StringUtils.isNotBlank(collection));

    boolean isSuccess = true;
    if (!data.isEmpty()) {
      try {
        List<DSRecord<Object>> records = DSRecordConverter.getDSRecords(data, this.schemaMap);
        this.indexClient.setDefaultCollection(collection);
        this.indexClient.updateRecords(records, null);
      } catch (IndexException e) {
        isSuccess = false;
        log.error("Failed to upload data to index. Collection: " + collection + ". Error: " + e.getMessage());
        log.error(e.getStackTrace().toString());
      }
    }
    return isSuccess;
  }

}
