/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

/**
 * InsertCmdBuilder interface for generated Insert Command.
 * @author fei
 *
 */
public interface InsertCmdBuilder extends CmdBuilder {
  /**
   * Configure a table in the database to read.
   */
  public InsertCmdBuilder table(String table);

  /**
   * Configure a column and its value to insert.
   */
  public <T> InsertCmdBuilder columnValue(String column, T value);

  /**
   * Configure the options of the insert command
   */
  public InsertCmdBuilder option(ComboOption comboOption);
}
