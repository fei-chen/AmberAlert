/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 14, 2013.
 */

package com.bloomreach.db.mysql.sql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.BatchCmdBuilder;
import com.bloomreach.db.CmdBuilder;
import com.bloomreach.db.ComboOption;
import com.bloomreach.db.DeleteCmdBuilder;
import com.bloomreach.db.InsertCmdBuilder;
import com.bloomreach.db.UpdateCmdBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * SQL batch command builder
 *
 * @author fei
 */
public class SQLBatchCmdBuilder implements BatchCmdBuilder {
  private List<String> cmdList;
  private String optionClause = "";

  public static final String BEGIN = "BEGIN BATCH";
  public static final String END = "APPLY BATCH";

  public SQLBatchCmdBuilder() {
    this.cmdList = Lists.newArrayList();
  }

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String batchCmd = "";
    if (this.isValid()) {
      batchCmd += BEGIN
          + (StringUtils.isNotBlank(this.optionClause) ? " USING " + this.optionClause : "");

      for (String cmd : this.cmdList) {
        batchCmd += "\n" + cmd;
      }
      batchCmd += "\n" + END + ";";
    }
    return batchCmd;
  }

  @Override
  public boolean isValid() {
    return !this.cmdList.isEmpty();
  }

  @Override
  public BatchCmdBuilder add(CmdBuilder cmd) {
    Preconditions.checkNotNull(cmd);
    String cql = cmd.build();
    Preconditions.checkArgument(StringUtils.isNotBlank(cql));

    // CQL Batch Cmd supports only insert, update, and delete commands
    if (cmd instanceof InsertCmdBuilder
        || cmd instanceof UpdateCmdBuilder
        || cmd instanceof DeleteCmdBuilder) {
      this.cmdList.add(cql);
    }
    return this;
  }

  @Override
  public List<String> getCmdList() {
    return this.cmdList;
  }

  @Override
  public BatchCmdBuilder option(ComboOption comboOption) {
    Preconditions.checkArgument(false, "option is not supported");
    return null;
  }

}
