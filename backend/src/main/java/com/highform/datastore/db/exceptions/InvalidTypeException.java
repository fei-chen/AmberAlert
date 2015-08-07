/**
 * Copyright at HighForm Inc. All rights reserved Apr 24, 2013.
 */

package com.highform.datastore.db.exceptions;

/**
 * Invalid type exception
 *
 * @author fei
 */
public class InvalidTypeException extends DBException {

  /**
   *
   */
  private static final long serialVersionUID = 3374823467171659751L;

  public InvalidTypeException() {
    super();
  }

  public InvalidTypeException(String errMsg) {
    super(errMsg);
  }

}