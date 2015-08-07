/**
 * Copyright at HighForm Inc. All rights reserved Mar 14, 2013.
 */

package com.highform.util.concurrent.taskrunner;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Stats of ParrellelTaskRunner
 *
 * @author fei
 */
public class ParallelTaskRunnerStats implements Stats {

  private final Set<Stats> runnerStats = Sets.newHashSet();

  @Override
  public void addStats(Stats stats) {
    this.runnerStats.add(stats);
  }

  @Override
  public void removeStats(Stats stats) {
    this.runnerStats.remove(stats);
  }

  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    for (Stats stats : this.runnerStats) {
      strBuilder.append(stats.toString() + "\n");
    }
    return strBuilder.toString();
  }

}
