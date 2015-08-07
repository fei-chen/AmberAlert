/**
 * Copyright at HighForm Inc. All rights reserved Jun 22, 2013.
 */

package com.highform.datastore.exceptions;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

/**
 * Datastore exception handler
 * 
 * @author fei
 */
public class ExceptionHandler {
  public static final String UNSUPPORTED = "Unsupported";

  public static void throwException(String errMsg) throws DatastoreException {
    Preconditions.checkArgument(StringUtils.isNotBlank(errMsg));
    String lowcaseErrMsg = errMsg.toLowerCase();
    if (lowcaseErrMsg.contains(UNSUPPORTED)) {
      throw new InvalidDataTypeException(errMsg);
    }
  }
}
