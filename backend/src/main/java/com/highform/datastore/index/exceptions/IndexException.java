/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.datastore.index.exceptions;

/**
 * Basic exception for the index system.
 * 
 * @author fei
 */
public class IndexException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = -4100442654431443893L;

  public IndexException() {

  }

  public IndexException(String errMsg) {
    super(errMsg);
  }

  public IndexException(Exception e) {
    super(e);
  }
}
