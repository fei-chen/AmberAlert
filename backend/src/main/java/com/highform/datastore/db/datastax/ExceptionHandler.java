/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 11, 2013.
 */

package com.bloomreach.db.datastax;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.exceptions.DBException;
import com.bloomreach.db.exceptions.InvalidOperationException;
import com.bloomreach.db.exceptions.InvalidTableException;
import com.bloomreach.db.exceptions.InvalidTypeException;
import com.bloomreach.db.exceptions.TimeoutException;
import com.google.common.base.Preconditions;

public class ExceptionHandler {
  private static final String TIMEOUT = "timeout";
  private static final String INVALID_TABLE = "unconfigured columnfamily";
  private static final String UNSUPPORTED = "Unsupported";

  public static void throwException(String errMsg)
      throws DBException {
    Preconditions.checkArgument(StringUtils.isNotBlank(errMsg));
    String lowcaseErrMsg = errMsg.toLowerCase();
    if (lowcaseErrMsg.contains(TIMEOUT)) {
      throw new TimeoutException(errMsg);
    } else if (lowcaseErrMsg.contains(INVALID_TABLE)) {
      throw new InvalidTableException(errMsg);
    } else if (lowcaseErrMsg.contains(UNSUPPORTED)) {
        throw new InvalidTypeException(errMsg);
    } else {
      throw new InvalidOperationException(errMsg);
    }
  }
}