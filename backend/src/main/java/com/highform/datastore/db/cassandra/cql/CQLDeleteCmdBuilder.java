package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.ComboCondition;
import com.highform.datastore.db.ComboOption;
import com.highform.datastore.db.DeleteCmdBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class CQLDeleteCmdBuilder implements DeleteCmdBuilder {

  private String deleteClause = "";
  private String fromClause = "";
  private String whereClause = "";
  private String optionClause = "";

  private List<String> columns = null;

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String cmd = "";
    if (this.isValid()) {
      this.deleteClause = StringUtils.join(this.columns, ",");
      cmd = "DELETE"
          + (StringUtils.isNotBlank(this.deleteClause) ? " " + this.deleteClause : "")
          + " FROM " + this.fromClause
          + (StringUtils.isNotBlank(this.optionClause) ? " USING " + this.optionClause : "")
          + " WHERE " + this.whereClause
          + ";";
    }
    return cmd;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(this.fromClause) && StringUtils.isNotBlank(this.whereClause);
  }

  @Override
  public DeleteCmdBuilder table(String table) {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    this.fromClause = table;
    return this;
  }

  @Override
  public DeleteCmdBuilder option(ComboOption comboOption) {
    Preconditions.checkNotNull(comboOption);
    this.optionClause = comboOption.build();
    return this;
  }

  @Override
  public DeleteCmdBuilder columns(List<String> columns) {
    this.columns = Preconditions.checkNotNull(columns);
    return this;
  }

  @Override
  public DeleteCmdBuilder allColumns() {
    this.columns = Lists.newArrayList();
    return this;
  }

  @Override
  public DeleteCmdBuilder condition(ComboCondition comboCondition) {
    Preconditions.checkNotNull(comboCondition);
    this.whereClause = comboCondition.build();
    return this;
  }

}
