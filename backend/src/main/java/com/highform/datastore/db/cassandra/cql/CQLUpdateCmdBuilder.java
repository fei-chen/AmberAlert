/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.ComboCondition;
import com.highform.datastore.db.ComboOption;
import com.highform.datastore.db.DataTypeConverter;
import com.highform.datastore.db.UpdateCmdBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * CQL update command builder
 * @author fei
 *
 */
public class CQLUpdateCmdBuilder implements UpdateCmdBuilder {

  private String updateClause = "";
  private String optionClause = "";
  private String setClause = "";
  private String whereClause = "";

  private List<String> columnValueOrExprPairs = Lists.newArrayList();

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String cmd = "";
    if (this.isValid()) {
      this.setClause = StringUtils.join(this.columnValueOrExprPairs, ",");

      cmd = "UPDATE " + this.updateClause
          + (StringUtils.isNotBlank(this.optionClause) ? " USING " + this.optionClause : "")
          + " SET " + this.setClause
          + " WHERE " + this.whereClause
          + ";";
    }
    return cmd;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(this.updateClause)
           && !this.columnValueOrExprPairs.isEmpty()
           && StringUtils.isNotBlank(this.whereClause);
  }

  @Override
  public UpdateCmdBuilder table(String table) {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    this.updateClause = table;
    return this;
  }

  @Override
  public UpdateCmdBuilder option(ComboOption comboOption) {
    Preconditions.checkNotNull(comboOption);
    this.optionClause = comboOption.build();
    return this;
  }

  @Override
  public <T> UpdateCmdBuilder columnValue(String column, T value) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkNotNull(value);
    String columnValuePair = column + " = " + DataTypeConverter.toString(value);
    this.columnValueOrExprPairs.add(columnValuePair);
    return this;
  }

  @Override
  public UpdateCmdBuilder columnExpr(String column, String expr) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkArgument(StringUtils.isNotBlank(expr));
    String columnExprPair = column + " = " + expr;
    this.columnValueOrExprPairs.add(columnExprPair);
    return this;
  }

  @Override
  public UpdateCmdBuilder condition(ComboCondition comboCondition) {
    Preconditions.checkNotNull(comboCondition);
    this.whereClause = comboCondition.build();
    return this;
  }

}
