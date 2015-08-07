/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.util.concurrent.taskrunner;

/**
 * Task Stats
 *
 * @author fei
 */
public interface Stats {
  public void addStats(Stats stats);
  public void removeStats(Stats stats);
}
