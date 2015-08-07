/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.util.concurrent.taskrunner;

/**
 * Exception of ParallelTaskRunner
 *
 * @author fei
 */
@SuppressWarnings("serial")
public class ProcessException extends Exception {
  public ProcessException(Exception e) {
    super(e);
  }

  public ProcessException(String errMsg) {
    super(errMsg);
  }
}