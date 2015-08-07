package com.highform.datastore.db.cassandra;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.exceptions.DBException;
import com.highform.datastore.db.exceptions.InvalidOperationException;
import com.highform.datastore.db.exceptions.InvalidTableException;
import com.highform.datastore.db.exceptions.InvalidTypeException;
import com.highform.datastore.db.exceptions.TimeoutException;
import com.google.common.base.Preconditions;


public class ExceptionHandler {
  public static final String TIMEOUT = "timeout";
  public static final String INVALID_TABLE = "unconfigured columnfamily";
  public static final String UNSUPPORTED = "Unsupported";

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
    }
    else {
      throw new InvalidOperationException(errMsg);
    }
  }
}
