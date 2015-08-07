/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 9, 2013.
 */

package com.bloomreach.db.mysql.sql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.ComboCondition;
import com.bloomreach.db.Condition;
import com.google.common.collect.Lists;

/**
 * SQLComboCondition for joining multiple conditions with AND in SQL.
 * Right now, no support for NEITHER "OR" operator NOR nested conditions. For example,
 * 1. condition1 OR condition2
 * 2. (condition1 AND condition2) AND (...)
 * Thus, all the conditions and comboCombinations will be directly concatenated by "AND"
 * @author fei
 *
 */
public class SQLComboCondition implements ComboCondition {

  /**
   * Operator represents the operators supported in a Condition
   */
  public enum Operator {

    AND(" AND "),
    OR(" OR ");

    private final String opStr;

    private Operator(String opStr) {
      this.opStr = opStr;
    }

    @Override
    public String toString() { return this.opStr; }
  }

  private String opStr;
  private List<String> conditionsStr = Lists.newArrayList();


  private SQLComboCondition(Operator op) {
    this.opStr = op.toString();
  }


  /**
   * Generate a ComboCondition with AND operator
   */
  public static ComboCondition and() {
    return new SQLComboCondition(Operator.AND);
  }

  @Override
  public String build() {
    return StringUtils.join(this.conditionsStr, this.opStr);
  }

  @Override
  public ComboCondition addCondition(Condition condition) {
    this.conditionsStr.add(condition.build());
    return this;
  }

  @Override
  public ComboCondition addConditions(List<Condition> conditions) {
    for (Condition condition : conditions) {
      this.conditionsStr.add(condition.build());
    }
    return this;
  }

  @Override
  public ComboCondition addComboCondition(ComboCondition comboCondition) {
    this.conditionsStr.add(comboCondition.build());
    return this;
  }

  @Override
  public ComboCondition addComboConditions(List<ComboCondition> comboConditions) {
    for (ComboCondition comboCondition : comboConditions) {
      this.conditionsStr.add(comboCondition.build());
    }
    return this;
  }

}
