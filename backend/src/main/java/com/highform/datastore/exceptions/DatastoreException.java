/**
 * Copyright at HighForm Inc. All rights reserved Jun 22, 2013.
 */

package com.highform.datastore.exceptions;

/**
 * Datastore exceptions
 *
 * @author fei
 */
public class DatastoreException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = -8726906150181772930L;

  public DatastoreException() {

  }

  public DatastoreException(String errMsg) {
    super(errMsg);
  }
}
