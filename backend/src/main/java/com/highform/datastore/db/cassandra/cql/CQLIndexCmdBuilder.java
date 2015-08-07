package com.highform.datastore.db.cassandra.cql;

import org.apache.commons.lang.StringUtils;

import com.highform.datastore.db.IndexCmdBuilder;
import com.google.common.base.Preconditions;

public class CQLIndexCmdBuilder implements IndexCmdBuilder {

  /**
   * Operator represents the operators
   */
  public enum Operator {
    DROP("DROP"),
    CREATE("CREATE");

    private final String opStr;

    private Operator(String opStr) {
      this.opStr = opStr;
    }

    @Override
    public String toString() { return this.opStr; }
  }

  // create index on <table> (<column>);
  // drop index on <table>_<column>_idx;
  private Operator op;
  private String table;
  private String column;

  private CQLIndexCmdBuilder(Operator op) {
    this.op = op;
  }

  /**
   * Generate a CQLIndexCmdBuilder with CREATE operator
   */
  public static CQLIndexCmdBuilder create() {
    return new CQLIndexCmdBuilder(Operator.CREATE);
  }

  /**
   * Generate a CQLIndexCmdBuilder with DROP operator
   */
  public static CQLIndexCmdBuilder drop() {
    return new CQLIndexCmdBuilder(Operator.DROP);
  }

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String cmd = "";
    if (this.isValid()) {
      String onClause = "";
      switch(this.op) {
      case CREATE:
        onClause = " INDEX ON " + this.table + " (" + this.column + ")";
        break;
      case DROP:
        onClause = " INDEX " + this.table + "_" + this.column + "_idx";
        break;
      }
      cmd = this.op.toString() + onClause + ";";
    }
    return cmd;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(this.table) && StringUtils.isNotBlank(this.column);
  }

  @Override
  public IndexCmdBuilder table(String table) {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    this.table = table;
    return this;
  }

  @Override
  public IndexCmdBuilder column(String column) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    this.column = column;
    return this;
  }

}
