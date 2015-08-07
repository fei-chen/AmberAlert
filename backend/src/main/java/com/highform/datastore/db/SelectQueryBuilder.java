/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

import java.util.List;

/**
 * SelectQueryBuilder interface for generated Select Query.
 * @author fei
 *
 */
public interface SelectQueryBuilder extends CmdBuilder {
  /**
   * Configure columns in a table to read.
   */
  public SelectQueryBuilder columns(List<String> columns);

  /**
   * Read all columns in a table.
   */
  public SelectQueryBuilder allColumns();

  /**
   * Get the number of records that satisfy the query.
   */
  public SelectQueryBuilder count();

  /**
   * Configure a table in the database to read.
   */
  public SelectQueryBuilder table(String table);

  /**
   * Get a table in the database to read.
   */
  public String getTable();

  /**
   * Configure a combo condition.
   */
  public SelectQueryBuilder condition(ComboCondition comboCondition);

  /**
   * Configure the number of rows to read in the query result.
   */
  public SelectQueryBuilder limit(int num);

  /**
   * Limit has been configured.
   */
  public boolean hasLimit();

  /**
   * Configure reading from the index-th row in the query result.
   */
  public <T> SelectQueryBuilder startFrom(String column, T index);

  /**
   * Configure the sorting order of rows in the query result.
   */
  public SelectQueryBuilder order(String column, SortOrder order);
}