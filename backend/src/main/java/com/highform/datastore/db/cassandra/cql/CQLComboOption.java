/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db.cassandra.cql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.ComboOption;
import com.highform.datastore.db.Option;
import com.google.common.collect.Lists;

/**
 * CQLComboOption for joining multiple options with AND in CQL 3.0.
 * Thus, all the conditions and comboCombinations will be directly concatenated by "AND"
 * @author fei
 *
 */
public class CQLComboOption implements ComboOption {

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
  private List<String> optionsStr = Lists.newArrayList();

  private CQLComboOption(Operator op) {
    this.opStr = op.toString();
  }

  /**
   * Generate a ComboOption with AND operator
   */
  public static ComboOption and() {
    return new CQLComboOption(Operator.AND);
  }

  @Override
  public String build() {
    return StringUtils.join(this.optionsStr, this.opStr);
  }

  @Override
  public ComboOption addOption(Option option) {
    this.optionsStr.add(option.build());
    return this;
  }

  @Override
  public ComboOption addOptions(List<Option> options) {
    for (Option option : options) {
      this.optionsStr.add(option.build());
    }
    return this;
  }

  @Override
  public ComboOption addComboOption(ComboOption comboOption) {
    this.optionsStr.add(comboOption.build());
    return this;
  }

  @Override
  public ComboOption addComboOptions(List<ComboOption> comboOptions) {
    for (ComboOption comboOption : comboOptions) {
      this.optionsStr.add(comboOption.build());
    }
    return this;
  }

}
