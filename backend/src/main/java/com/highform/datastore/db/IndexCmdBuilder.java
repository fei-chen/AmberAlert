/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

/**
 * InsertCmdBuilder interface for generated Insert Command.
 * @author fei
 *
 */
public interface IndexCmdBuilder extends CmdBuilder {
  /**
   * Configure a table in the database to read.
   */
  public IndexCmdBuilder table(String table);

  /**
   * Configure a column and its value to insert.
   */
  public IndexCmdBuilder column(String column);
}
