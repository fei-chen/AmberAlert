/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 8, 2013.
 */

package com.bloomreach.db.mysql;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.exceptions.DBException;
import com.bloomreach.db.exceptions.InvalidOperationException;
import com.bloomreach.db.exceptions.InvalidTableException;
import com.bloomreach.db.exceptions.InvalidTypeException;
import com.bloomreach.db.exceptions.TimeoutException;

/**
 * Mysql exception handler
 *
 * @author fei
 */

public class ExceptionHandler {
  private static final String TIMEOUT = "timeout expired";
  private static final String INVALID_TABLE = "doesn't exist";
  private static final String UNSUPPORTED = "unsupported feature";

  public static void throwException(Exception e)
      throws DBException {
    if (StringUtils.isBlank(e.getMessage())) {
      throw new DBException(e);
    }

    String errMsg = e.getMessage();
    String lowcaseErrMsg = e.getMessage().toLowerCase();
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