/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

/**
 * Batch command interface
 *
 * @author fei
 *
 */
public interface BatchCmdBuilder extends CmdBuilder {
  /**
   * Add command to batch.
   * @param cmd
   * @return
   */
  public BatchCmdBuilder add(CmdBuilder cmd);
}
