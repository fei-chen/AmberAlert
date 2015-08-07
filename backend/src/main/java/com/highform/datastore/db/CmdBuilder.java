/**
 * Copyright at HighForm Inc. All rights reserved 2013.
 */

package com.highform.datastore.db;

/**
 * CmdBuilder interface for generated Command.
 * @author fei
 *
 */
public interface CmdBuilder {
  /**
   * Build a command from configured parameters.
   */
  public String build();

  /**
   * Check whether a command is valid.
   * @return true if the command is valid.
   */
  public boolean isValid();
}
