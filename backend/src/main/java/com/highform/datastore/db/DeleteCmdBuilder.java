/**
 * Copyright at HighForm Inc. All rights reserved 2013.
 */

package com.highform.datastore.db;

import java.util.List;

/**
 * DeleteCmdBuilder interface for generated Insert Command.
 * @author fei
 *
 */
public interface DeleteCmdBuilder extends CmdBuilder {
  /**
   * Configure a table
   */
  public DeleteCmdBuilder table(String table);

  /**
   * Configure the options
   */
  public DeleteCmdBuilder option(ComboOption comboOption);

  /**
   * Configure the columns from a row to remove
   */
  public DeleteCmdBuilder columns(List<String> columns);

  /**
   * Configure all columns to remove
   */
  public DeleteCmdBuilder allColumns();

  /**
   * Configure a combo condition.
   */
  public DeleteCmdBuilder condition(ComboCondition comboCondition);
}
