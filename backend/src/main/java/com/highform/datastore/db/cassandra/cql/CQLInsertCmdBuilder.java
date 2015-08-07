/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.ComboOption;
import com.highform.datastore.db.DataTypeConverter;
import com.highform.datastore.db.InsertCmdBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * CQL insert command builder
 * @author fei
 *
 */
public class CQLInsertCmdBuilder implements InsertCmdBuilder {

  private String intoClause = "";
  private String insertClause = "";
  private String valueClause = "";
  private String optionClause = "";

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
          + (StringUtils.isNotBlank(this.optionClause) ? " USING " + this.optionClause : "")
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
    Preconditions.checkNotNull(comboOption);
    this.optionClause = comboOption.build();
    return this;
  }

}
