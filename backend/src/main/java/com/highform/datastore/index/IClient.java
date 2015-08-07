/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.datastore.index;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import com.highform.datastore.DSRecord;
import com.highform.datastore.TableSchemaMap;
import com.highform.datastore.index.exceptions.IndexException;

/**
 * <Class description>
 *
 * @author fei
 */
public interface IClient extends Closeable{

  public enum ClientType {
    SOLR, ELASTICSEARCH
  }

  /**
   * Connect to database
   */
  public void connect();

  /**
   * Close the connection
   */
  public void close();

  /**
   * update records to the index system
   * @param records
   * @return true if success
   */
  public boolean updateRecords(List<DSRecord<Object>> records, Map<String, String> params)
      throws IndexException;

  /**
   * delete records from the index system by their ids
   * @param ids
   * @param params
   * @return
   * @throws IndexException
   */
  public boolean deleteRecordsByIds(List<String> ids, Map<String, String> params)
      throws IndexException;

  /**
   * Get list of records that satisfy the query
   * @param query
   * @param params
   * @return list of records
   */
  public <T> List<DSRecord<Object>> getRecords(T query, Map<String, Object> params)
      throws IndexException;

  /**
   * Get list of records that satisfy the query with sorting
   * @param query
   * @param params
   * @return list of records
   */
  public <T> List<DSRecord<Object>> getRecords(T query, Map<String, Object> params,
      Map<String, String> sortFields)
      throws IndexException;

  /**
   * Get the number of records that satisfy the query
   * @param query
   * @param params
   * @return count
   */
  public <T> long getCount(T query, Map<String, Object> params)
      throws IndexException;

  /**
   * Returns an index schema map for this client
   *
   * @param indexName
   * @return
   */
  public TableSchemaMap getIndexSchemaMap(String indexName);

  /**
   * Set the default collection
   *
   * @param collectionName
   */
  public void setDefaultCollection(String collectionName);

}
