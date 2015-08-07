package com.highform.datastore.db.exceptions;

public class InvalidOperationException extends DBException {
  /**
   * 
   */
  private static final long serialVersionUID = -6512737875209098262L;

  public InvalidOperationException() {
    super();
  }

  public InvalidOperationException(String errMsg) {
    super(errMsg);
  }
}