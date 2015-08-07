/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.ComboCondition;
import com.highform.datastore.db.Condition;
import com.google.common.collect.Lists;

/**
 * CQLComboCondition for joining multiple conditions with AND in CQL 3.0.
 * CQL 3.0 supports NEITHER "OR" operator NOR nested conditions. For example,
 * 1. condition1 OR condition2
 * 2. (condition1 AND condition2) AND (...)
 * Thus, all the conditions and comboCombinations will be directly concatenated by "AND"
 * @author fei
 *
 */
public class CQLComboCondition implements ComboCondition {

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

  private CQLComboCondition(Operator op) {
    this.opStr = op.toString();
  }

  /**
   * Generate a ComboCondition with AND operator
   */
  public static ComboCondition and() {
    return new CQLComboCondition(Operator.AND);
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
