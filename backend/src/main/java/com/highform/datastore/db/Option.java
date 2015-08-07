/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

/**
 * Option interface for generated options for InsertCmdBuilder and UpdateCmdBuilder.
 * @author fei
 *
 */
public interface Option {
  /**
   * Build a insert command from configured parameters.
   */
  public String build();
}
