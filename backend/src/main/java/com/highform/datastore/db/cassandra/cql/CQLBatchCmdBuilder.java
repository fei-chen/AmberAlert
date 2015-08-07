package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.BatchCmdBuilder;
import com.highform.datastore.db.CmdBuilder;
import com.highform.datastore.db.DeleteCmdBuilder;
import com.highform.datastore.db.InsertCmdBuilder;
import com.highform.datastore.db.UpdateCmdBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class CQLBatchCmdBuilder implements BatchCmdBuilder {
  private List<String> cmdList;

  public static final String BEGIN = "BEGIN BATCH";
  public static final String END = "APPLY BATCH";

  public CQLBatchCmdBuilder() {
    this.cmdList = Lists.newArrayList();
  }

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String batchCmd = "";
    if (this.isValid()) {
      batchCmd += BEGIN;
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

}
