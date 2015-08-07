/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 14, 2013.
 */

package com.bloomreach.db.mysql.sql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.ComboOption;
import com.bloomreach.db.DataTypeConverter;
import com.bloomreach.db.InsertCmdBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * SQL insert command builder
 *
 * @author fei
 */
public class SQLInsertCmdBuilder implements InsertCmdBuilder {

  private String intoClause = "";
  private String insertClause = "";
  private String valueClause = "";

  private List<String> columns = Lists.newArrayList();
  private List<Object> values = Lists.newArrayList();

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String cmd = "";
    if (this.isValid()) {
      this.insertClause = "(" + StringUtils.join(this.columns, ",") + ")";
      this.valueClause = "(" + DataTypeConverter.toString(this.values) + ")";

      cmd = "INSERT INTO " + this.intoClause + " " + this.insertClause
          + " VALUES " + this.valueClause
          + ";";
    }
    return cmd;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(this.intoClause)
        && !this.columns.isEmpty()
        && this.columns.size() == this.values.size();
  }

  @Override
  public InsertCmdBuilder table(String table) {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    this.intoClause = table;
    return this;
  }

  @Override
  public <T> InsertCmdBuilder columnValue(String column, T value) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkNotNull(value);
    this.columns.add(column);
    this.values.add(value);
    return this;
  }

  @Override
  public InsertCmdBuilder option(ComboOption comboOption) {
    Preconditions.checkArgument(false, "option is not supported");
    return null;
  }

}
