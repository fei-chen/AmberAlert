package com.highform.datastore.db;

public enum SortOrder {
  UNSORTED("UNSORTED"), ASC("ASC"), DESC("DESC");

  private final String orderStr;

  private SortOrder(String orderStr) {
    this.orderStr = orderStr;
  }

  @Override
  public String toString() { return this.orderStr; }
}