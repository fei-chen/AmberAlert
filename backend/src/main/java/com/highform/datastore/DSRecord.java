/**
 * Copyright at HighForm Inc. All rights reserved Mar 6, 2013.
 */

package com.highform.datastore;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Record builder
 * 
 * @author fei
 */
public class DSRecord<T> {
  private final Map<String, T> map;
  private String key;

  public DSRecord() {
    this.map = Maps.newHashMap();
  }

  public DSRecord(String key, Map<String, T> map) {
    Preconditions.checkArgument(StringUtils.isNotBlank(key));
    Preconditions.checkNotNull(map);
    this.map = map;
    this.key = key;
  }

  public DSRecord(String key, Set<Entry<String, T>> set) {
    this();
    Preconditions.checkArgument(StringUtils.isNotBlank(key));
    Preconditions.checkNotNull(set);
    for (Entry<String, T> entry : set) {
      this.map.put(entry.getKey(), entry.getValue());
    }
    this.key = key;
  }

  public static <T> DSRecord<T> newBuilder() {
    return new DSRecord<T>();
  }

  public DSRecord<T> setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return this.key;
  }

  public DSRecord<T> addField(String field, T value) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    Preconditions.checkNotNull(value);
    this.map.put(field, value);
    return this;
  }

  public boolean removeField(String field) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    if (this.map.remove(field) != null) {
      return true;
    }
    return false;
  }

  public T getFieldValue(String field) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    return this.map.get(field);
  }

  public Set<Entry<String, T>> getFields() {
    return this.map.entrySet();
  }

  public boolean hasField(String field) {
    Preconditions.checkArgument(StringUtils.isNotBlank(field));
    return this.map.containsKey(key);
  }

  @Override
  public String toString() {
    return this.map.toString();
  }

}
