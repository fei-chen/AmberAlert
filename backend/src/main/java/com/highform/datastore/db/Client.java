/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

import java.io.Closeable;
import java.util.List;

import com.highform.datastore.DSRecord;
import com.highform.datastore.db.exceptions.DBException;

/**
 * Client interface
 *
 * @author fei
 *
 */
public interface Client extends Closeable {
  /**
   * Connect to database
   */
  public void connect();

  /**
   * Get the query result
   * @param SelectQueryBuilder
   * @return list of records
   * @throws DBException
   */
  public List<DSRecord<String>> getRecords(SelectQueryBuilder query, String table)
     throws DBException;

  /**
   * Get the number of rows in the query result
   * @param query
   * @return count
   * @throws DBException
   */
  public long getCount(SelectQueryBuilder query)
      throws DBException;

  /**
   * Update records to database
   * @param insertCmd
   * @return true if success
   * @throws DBException
   */
  public boolean insertRecords(InsertCmdBuilder insertCmd)
      throws DBException;

  /**
   * Update a record to database
   * @param updateCmd
   * @return true if success
   * @throws DBException
   */
  public boolean updateRecords(UpdateCmdBuilder updateCmd)
      throws DBException;

  /**
   * Excuete a set of insert/update/delete cmds
   * @param batchCmd
   * @return true if success
   * @throws DBException
   */
  public boolean batchRecords(BatchCmdBuilder batchCmd)
      throws DBException;

  /**
   * Update index of a column in a table
   * @param indexCmd
   * @return true if success
   * @throws DBException
   */
  public boolean updateIndex(IndexCmdBuilder indexCmd)
      throws DBException;

  /**
   * Create/drop/alter table
   * @param tableCmd
   * @return true if success
   * @throws DBException
   */
  public boolean updateTable(TableCmdBuilder tableCmd)
      throws DBException;

  /**
   * Check whether the table is exist
   * @param merchant
   * @return true if the table is exist
   * @throws DBException
   */
  public boolean isTableExist(String table) throws DBException;

  /**
   * Create a table if it does not exist
   * @param table
   * @param schema
   * @return true if sucess
   * @throws DBException
   */
  public boolean createTableIfNotExist(String table, String schema)
      throws DBException;
}
