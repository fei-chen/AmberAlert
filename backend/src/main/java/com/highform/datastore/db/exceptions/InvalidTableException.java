package com.highform.datastore.db.exceptions;

public class InvalidTableException extends DBException {
  /**
   * 
   */
  private static final long serialVersionUID = -5648121802697495889L;

  public InvalidTableException() {
    super();
  }

  public InvalidTableException(String errMsg) {
    super(errMsg);
  }
}
