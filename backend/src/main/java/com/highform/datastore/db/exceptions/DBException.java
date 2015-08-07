package com.highform.datastore.db.exceptions;

public class DBException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 1610827044470643885L;

  public DBException() {

  }

  public DBException(String errMsg) {
    super(errMsg);
  }
}
