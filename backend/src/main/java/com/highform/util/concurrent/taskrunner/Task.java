/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.util.concurrent.taskrunner;

import java.util.concurrent.Callable;

/**
 * Task
 *
 * @author fei
 * @param <T>
 */
public interface Task<T> extends Callable<T>{
  @Override
  public T call() throws ProcessException;

  public boolean handleError(ProcessException e);

  public void handleSuccess();

  public Stats getStats();
}
