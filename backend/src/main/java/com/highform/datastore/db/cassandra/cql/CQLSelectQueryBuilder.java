/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.ComboCondition;
import com.highform.datastore.db.SelectQueryBuilder;
import com.highform.datastore.db.SortOrder;
import com.google.common.base.Preconditions;

/**
 * CQL query builder for building cassandra query
 * @author fei
 *
 */
public class CQLSelectQueryBuilder implements SelectQueryBuilder {

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
          + " ALLOW FILTERING;";
    }
    return query;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(this.selectClause) && StringUtils.isNotBlank(this.fromClause);
  }

  @Override
  public CQLSelectQueryBuilder columns(List<String> columns) {
    Preconditions.checkNotNull(columns);
    for (String column : columns) {
      Preconditions.checkArgument(StringUtils.isNotBlank(column));
    }
    this.selectClause = StringUtils.join(columns, ",");
    return this;
  }

  @Override
  public CQLSelectQueryBuilder allColumns() {
    this.selectClause = "*";
    return this;
  }

  @Override
  public CQLSelectQueryBuilder count() {
    this.selectClause = "COUNT(*)";
    return this;
  }

  @Override
  public CQLSelectQueryBuilder table(String table) {
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
  public CQLSelectQueryBuilder condition(ComboCondition comboCondition) {
    Preconditions.checkNotNull(comboCondition);
    if (StringUtils.isNotBlank(this.whereClause)) {
      this.whereClause += " AND ";
    }
    this.whereClause += comboCondition.build();
    return this;
  }

  @Override
  public CQLSelectQueryBuilder limit(int num) {
    Preconditions.checkArgument(num >= 0, "must be larger or equal to 0: %s", num);
    this.limitClause += Integer.toString(num);
    return this;
  }

  @Override
  public boolean hasLimit() {
    return StringUtils.isNotBlank(this.limitClause);
  }

  @Override
  public <T> CQLSelectQueryBuilder startFrom(String column, T index) {
    Preconditions.checkNotNull(index);
    if (StringUtils.isNotBlank(this.whereClause)) {
      this.whereClause += " AND ";
    }
    this.whereClause += "token(" + column + ") > token(" + ((String)index) + ")";
    return this;
  }

  @Override
  public CQLSelectQueryBuilder order(String column, SortOrder order) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkNotNull(order);
    if (order != SortOrder.UNSORTED) {
      this.orderClause = column + " " + order.toString();
    }
    return this;
  }
}
