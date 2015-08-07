/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

/**
 * UpdateCmdBuilder interface for generated update Command.
 * @author fei
 *
 */
public interface UpdateCmdBuilder extends CmdBuilder {
  /**
   * Configure a table to update
   */
  public UpdateCmdBuilder table(String table);

  /**
   * Configure the options of the update command
   */
  public UpdateCmdBuilder option(ComboOption comboOption);

  /**
   * Configure a column and its expression to insert.
   */
  public <T> UpdateCmdBuilder columnValue(String column, T value);

  /**
   * Configure a column and its expression to insert.
   */
  public UpdateCmdBuilder columnExpr(String column, String expr);
  /**
   * Configure a combo condition.
   */
  public UpdateCmdBuilder condition(ComboCondition comboCondition);
}
