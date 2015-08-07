/**
 * Copyright at HighForm Inc. All rights reserved Jun 22, 2013.
 */

package com.highform.datastore.exceptions;

/**
 * <Class description>
 * 
 * @author fei
 */
public class InvalidDataTypeException extends DatastoreException {
  private static final String MESSAGE = "Invalid data type: ";
  /**
   * 
   */
  private static final long serialVersionUID = 789517070418204776L;

  public InvalidDataTypeException() {
    super();
  }

  public InvalidDataTypeException(String errMsg) {
    super(MESSAGE + errMsg);
  }

}
