package com.highform.datastore.db.exceptions;

public class TimeoutException extends DBException {
  /**
   * 
   */
  private static final long serialVersionUID = -8934774279664137894L;

  public TimeoutException() {
    super();
  }

  public TimeoutException(String errMsg) {
    super(errMsg);
  }
}
