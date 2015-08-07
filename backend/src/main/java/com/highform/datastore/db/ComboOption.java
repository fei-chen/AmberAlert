/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

import java.util.List;

/**
 * ComboOption for joining multiple Options.
 * @author fei
 *
 */
public interface ComboOption {

  /**
   * Build the ComboOption
   */
  public String build();

  /**
   * Add a Option
   */
  public ComboOption addOption(Option Option);

  /**
   * Add a list of Options
   */
  public ComboOption addOptions(List<Option> Options);

  /**
   * Add a ComboOption
   */
  public ComboOption addComboOption(ComboOption ComboOption);

  /**
   * Add a list of ComboOptions
   */
  public ComboOption addComboOptions(List<ComboOption> ComboOptions);

}

