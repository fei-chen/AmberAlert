/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

import java.util.List;

/**
 * ComboCondition for joining multiple conditions with AND and OR.
 * @author fei
 *
 */
public interface ComboCondition {

  /**
   * Build the ComboCondition
   */
  public String build();

  /**
   * Add a condition
   */
  public ComboCondition addCondition(Condition condition);

  /**
   * Add a list of conditions
   */
  public ComboCondition addConditions(List<Condition> conditions);

  /**
   * Add a comboCondition
   */
  public ComboCondition addComboCondition(ComboCondition comboCondition);

  /**
   * Add a list of comboConditions
   */
  public ComboCondition addComboConditions(List<ComboCondition> comboConditions);

}
