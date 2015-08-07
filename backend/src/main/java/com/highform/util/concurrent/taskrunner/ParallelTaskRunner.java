/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.util.concurrent.taskrunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This class is used to run tasks in parallel.
 *
 * @author fei
 * @param <T> The return type of the task
 */
public class ParallelTaskRunner<T> {
  private final static Logger log = LoggerFactory.getLogger(ParallelTaskRunner.class);

  private final List<Task<T>> tasks;
  private final long timeoutMins;
  private final List<Future<T>> submitedTasks;
  private final ExecutorService service;
  private final ParallelTaskRunnerStats stats;

  public ParallelTaskRunner(List<Task<T>> tasks, long timeoutMins, ParallelTaskRunnerStats stats) {
    this.tasks = tasks;
    this.timeoutMins = timeoutMins;
    this.submitedTasks = Lists.newArrayList();
    this.service = Executors.newFixedThreadPool(tasks.size());
    this.stats = stats;
  }

  public void execute() throws ProcessException {
    final Map<Task<T>, ProcessException> failedTasks = Maps.newHashMap();
    for (Task<T> task : this.tasks) {
      Future<T> future = this.service.submit(task);
      this.submitedTasks.add(future);
    }
    // All tasks have been submitted, we can begin the shutdown of our executor
    // No new tasks will be accepted.
    this.service.shutdown();

    try {
      while(!this.service.awaitTermination(this.timeoutMins, TimeUnit.MINUTES)){}
    } catch (InterruptedException e) {
      throw new ProcessException(e);
    }

    for (int i = 0; i < this.submitedTasks.size(); i++) {
      Future<T> furtureTask = this.submitedTasks.get(i);
      Task<T> task = this.tasks.get(i);

      if (this.stats != null && task.getStats() != null) {
        this.stats.addStats(task.getStats());
      }

      if (failedTasks.containsKey(task)) {
        continue;
      }

      try {
        furtureTask.get(this.timeoutMins, TimeUnit.MINUTES);
        task.handleSuccess();
      } catch (Exception e) {
        ProcessException pe = new ProcessException(e);
        // Add a policy to cancel pending tasks and abort right away or let current
        // set of tasks finish before reporting exception.
        failedTasks.put(task, pe);
      }
    }

    if (!failedTasks.isEmpty()) {
      StringBuilder msg = new StringBuilder();
      boolean reportFailures = false;
      for (Task<T> task : failedTasks.keySet()) {
        if (task.handleError(failedTasks.get(task))) {
          msg.append(failedTasks.get(task).getMessage());
          reportFailures = true;
        }
      }
      if (reportFailures) {
        log.error(failedTasks.size() + " tasks execution failed. " + msg.toString());
        throw new ProcessException(new Exception(msg.toString()));
      }
    }
  }

  public String printStats() {
    return this.stats.toString();
  }

  public void cancel() {
    for (Future<T> future : this.submitedTasks) {
       if (!future.isDone()) {
         future.cancel(true);
       }
    }
  }
}
