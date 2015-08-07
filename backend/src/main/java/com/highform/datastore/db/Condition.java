/**
 * Copyright at HighForm Inc. All rights reserved 2012.
 */

package com.highform.datastore.db;

import java.util.List;

/**
 * Condition interface
 * @author fei
 *
 */
public interface Condition {

  /**
   * Tokenizer Enum
   */
  public enum TokenizeType {
    TOKENIZE_NONE,
    TOKENIZE_COLUMN,
    TOKENIZE_COLUMN_AND_VALUE;
  }

  /**
   * Build the condition
   */
  public String build();

  /**
   * Generate a Condition  for a column that is less than a given value (inclusive or exclusive).
   */
  public <T> Condition lessThan(String column, T value, boolean inclusive);

  /**
   * Generate a Condition  for a column that is greater than a given value (inclusive or exclusive).
   */
  public <T> Condition greaterThan(String column, T value, boolean inclusive);

  /**
   * Generate a Condition for a column that is equal to a given value.
   */
  public <T> Condition equalTo(String column, T value);

  /**
   * Generate a Condition for a column that is not equal to a given value.
   */
  public <T> Condition notEqualTo(String column, T value);

  /**
   * Generate a Condition for a column that is greater than a value, low, and less than
   * a value, high (inclusive).
   */
  public <T> Condition range(String column, T low, T high);

  /**
   * Generate a Condition for a column that is "like" a given value.
   */
  public <T> Condition like(String column, T value);

  /**
   * Generate a Condition for a column that is not "like" a given value.
   */
  public <T> Condition notLike(String column, T value);

  /**
   * Generate a Condition for a column that is "in" a given list of values.
   */
  public <T> Condition in(String column, List<T> value);

  /**
   *
   * Tokenize a condition
   */
  public Condition tokenize(TokenizeType tokenizeType);
}
