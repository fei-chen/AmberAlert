/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 9, 2013.
 */

package com.bloomreach.db.mysql.sql;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.ComboCondition;
import com.bloomreach.db.SelectQueryBuilder;
import com.bloomreach.db.SortOrder;
import com.google.common.base.Preconditions;

/**
 * SQL query builder for building mysql query
 *
 * @author fei
 */
public class SQLSelectQueryBuilder implements SelectQueryBuilder {

  private String selectClause = "";
  private String fromClause = "";
  private String whereClause = "";
  private String orderClause = "";
  private String limitClause = "";

  @Override
  public String build() {
    // if the query is not valid, return empty string
    String query = "";
    if (this.isValid()) {
      query = "SELECT " + this.selectClause + " FROM " + this.fromClause
          + (StringUtils.isNotBlank(this.whereClause) ? " WHERE " + this.whereClause : "")
          + (StringUtils.isNotBlank(this.orderClause) ? " ORDER BY " + this.orderClause : "")
          + (StringUtils.isNotBlank(this.limitClause) ? " LIMIT " + this.limitClause : "")
          + ";";
    }
    return query;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(this.selectClause) && StringUtils.isNotBlank(this.fromClause);
  }

  @Override
  public SelectQueryBuilder columns(List<String> columns) {
    Preconditions.checkNotNull(columns);
    for (String column : columns) {
      Preconditions.checkArgument(StringUtils.isNotBlank(column));
    }
    this.selectClause = StringUtils.join(columns, ",");
    return this;
  }

  @Override
  public SelectQueryBuilder columns(String... columns) {
    Preconditions.checkNotNull(columns);
    columns(Arrays.asList(columns));
    return this;
  }

  @Override
  public SelectQueryBuilder allColumns() {
    this.selectClause = "*";
    return this;
  }

  @Override
  public SelectQueryBuilder count() {
    this.selectClause = "COUNT(*)";
    return this;
  }

  @Override
  public SelectQueryBuilder table(String table) {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    this.fromClause = table;
    return this;
  }

  @Override
  public String getTable() {
    Preconditions.checkArgument(StringUtils.isNotBlank(this.fromClause), "table has not been configured");
    return this.fromClause;
  }

  @Override
  public SelectQueryBuilder condition(ComboCondition comboCondition) {
    Preconditions.checkNotNull(comboCondition);
    if (StringUtils.isNotBlank(this.whereClause)) {
      this.whereClause += " AND ";
    }
    this.whereClause += comboCondition.build();
    return this;
  }

  @Override
  public SelectQueryBuilder limit(int num) {
    Preconditions.checkArgument(num >= 0, "must be larger or equal to 0: %s", num);
    this.limitClause += Integer.toString(num);
    return this;
  }

  @Override
  public boolean hasLimit() {
    return StringUtils.isNotBlank(this.limitClause);
  }

  @Override
  public SelectQueryBuilder startFrom(int index) {
    Preconditions.checkArgument(index >= 0, "must be larger or equal to 0: %s", index);
    this.limitClause = Integer.toString(index) + ", " + this.limitClause;
    return this;
  }

  @Override
  public <T> SelectQueryBuilder startFrom(String column, T index) {
    Preconditions.checkArgument(false, "startFrom is not supported");
    return null;
  }

  @Override
  public SelectQueryBuilder order(String column, SortOrder order) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkNotNull(order);
    if (order != SortOrder.UNSORTED) {
      this.orderClause = column + " " + order.toString();
    }
    return this;
  }

}
