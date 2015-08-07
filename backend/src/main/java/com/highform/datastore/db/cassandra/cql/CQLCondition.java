/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.Condition;
import com.highform.datastore.db.DataTypeConverter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * CQLCondition for generating cql condition
 * @author fei
 *
 */
public class CQLCondition implements Condition {

  /**
   * Operator represents the operators supported in a Condition
   */
  public enum Operator {
    LESS_THAN(" < "),
    LESS_THAN_OR_EQUAL_TO(" <= "),
    GREATER_THAN(" > "),
    GREATER_THAN_OR_EQUAL_TO(" >= "),
    EQUAL_TO(" = "),
    IN(" IN ");

    private final String opStr;

    private Operator(String opStr) {
      this.opStr = opStr;
    }

    @Override
    public String toString() { return this.opStr; }
  }

  private static final String TOKEN = "TOKEN";

  private String columnStr;
  private List<String> opStrs;
  private List<String> valueStrs;
  private TokenizeType tokenizeType = TokenizeType.TOKENIZE_NONE;

  public CQLCondition () {
    this.opStrs = Lists.newArrayList();
    this.valueStrs = Lists.newArrayList();
  }

  private <T> void setup(Operator op, String column, T value) {
    this.columnStr = column;
    this.opStrs.add(op.toString());
    this.valueStrs.add(DataTypeConverter.toString(value));
  }

  private <T> void setup(Operator op, String column, List<T> values) {
    this.columnStr = column;
    this.opStrs.add(op.toString());
    this.valueStrs.add("(" + DataTypeConverter.toString(values) + ")");
  }

  @Override
  public String build(){
    Preconditions.checkArgument(this.opStrs.size()==this.valueStrs.size(), "opStrs is inconsistent with valueStrs");

    String conditionStr = "";
    for (int i=0; i<this.opStrs.size(); i++) {
      if (StringUtils.isNotBlank(conditionStr)) {
        conditionStr += " AND ";
      }

      boolean isTokenizeColumn = (this.tokenizeType == TokenizeType.TOKENIZE_COLUMN || this.tokenizeType == TokenizeType.TOKENIZE_COLUMN_AND_VALUE);
      boolean isTokenizeValue = (this.tokenizeType == TokenizeType.TOKENIZE_COLUMN_AND_VALUE);
      String column = isTokenizeColumn ? TOKEN + "(" + this.columnStr + ")" : this.columnStr;
      String value = isTokenizeValue ? TOKEN + "(" + this.valueStrs.get(i) + ")" : this.valueStrs.get(i);
      conditionStr += column + this.opStrs.get(i) + value;
    }
    return conditionStr;
  }

  @Override
  public <T> Condition lessThan(String column, T value, boolean inclusive) {
    Operator op = inclusive ? Operator.LESS_THAN_OR_EQUAL_TO : Operator.LESS_THAN;
    setup(op, column, value);
    return this;
  }

  @Override
  public <T> Condition greaterThan(String column, T value, boolean inclusive) {
    Operator op = inclusive ? Operator.GREATER_THAN_OR_EQUAL_TO : Operator.GREATER_THAN;
    setup(op, column, value);
    return this;
  }

  @Override
  public <T> Condition equalTo(String column, T value) {
    setup(Operator.EQUAL_TO, column, value);
    return this;
  }

  @Override
  public <T> Condition in(String column, List<T> values) {
    setup(Operator.IN, column, values);
    return this;
  }

  @Override
  public <T> Condition range(String column, T low, T high) {
    setup(Operator.GREATER_THAN_OR_EQUAL_TO, column, low);
    setup(Operator.LESS_THAN_OR_EQUAL_TO, column, high);
    return this;
  }

  @Override
  public <T> Condition notEqualTo(String column, T value) {
    // Not Support
    return null;
  }

  @Override
  public <T> Condition like(String column, T value) {
    // Not Support
    return null;
  }

  @Override
  public <T> Condition notLike(String column, T value) {
    // Not Support
    return null;
  }

  @Override
  public Condition tokenize(TokenizeType tokenizeType) {
    Preconditions.checkNotNull(tokenizeType);
    this.tokenizeType = tokenizeType;
    return this;
  }

}
