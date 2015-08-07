/**
 * Copyright at HighForm Inc. All rights reserved 2013.
 */

package com.highform.datastore.db;

import java.util.List;

/**
 * InsertCmdBuilder interface for generated Insert Command.
 * @author fei
 *
 */
public interface TableCmdBuilder extends CmdBuilder {
  /**
   * Configure a table in the database
   */
  public TableCmdBuilder table(String table);

  /**
   * Configure a column and its type
   */
  public <T> TableCmdBuilder columnDatatype(String column, T type);

  /**
   * Configure a primary key
   */
  public TableCmdBuilder primaryKey(String column);

  /**
   * Configure a composite primary key
   */
  public TableCmdBuilder compositePrimaryKey(List<String> columns);
}
