/**
 * Copyright at HighForm Inc. All rights reserved 2013.
 */

package com.highform.datastore.db.cassandra.cql;

/**
 * CQL table command builder for building cassandra table command
 * @author fei
 *
 */
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;

import com.highform.datastore.db.TableCmdBuilder;
import com.highform.datastore.proto.Datastore.DataType;
import com.google.common.base.Preconditions;

public class CQLTableCmdBuilder implements TableCmdBuilder {

  /**
   * Operator represents the operators
   */
  public enum Operator {
    CREATE("CREATE"),
    ALTER("ALTER"),
    DROP("DROP");

    private final String opStr;

    private Operator(String opStr) {
      this.opStr = opStr;
    }

    @Override
    public String toString() { return this.opStr; }
  }

  private Operator op;
  private String table = "";
  private List<String> columnDatatypePairs = Lists.newArrayList();
  private String primaryKey = "";

  private CQLTableCmdBuilder(Operator op) {
    this.op = op;
  }

  /**
   * Generate a CQLTableCmdBuilder with CREATE operator
   */
  public static CQLTableCmdBuilder create() {
    return new CQLTableCmdBuilder(Operator.CREATE);
  }

  /**
   * Generate a CQLTableCmdBuilder with DROP operator
   */
  public static CQLTableCmdBuilder drop() {
    return new CQLTableCmdBuilder(Operator.DROP);
  }

  @Override
  public String build() {
    // if the command is not valid, return empty string
    String cmd = "";
    if (this.isValid()) {
      switch(this.op) {
      case CREATE:
        cmd = buildCreate();
        break;
      case DROP:
        cmd = buildDrop();
        break;
      case ALTER:
        cmd = buildAlter();
        break;
      }
    }
    return cmd;
  }

  @Override
  public boolean isValid() {
    boolean isValid = false;
    switch(this.op) {
    case CREATE:
      isValid = StringUtils.isNotBlank(this.table) && !this.columnDatatypePairs.isEmpty();
      break;
    case DROP:
      isValid = StringUtils.isNotBlank(this.table);
      break;
    case ALTER:// TODO change the validation for ALTER when add alter table command
      isValid = StringUtils.isNotBlank(this.table);
      break;
    }
    return isValid;
  }

  /**
   * build a command for creating a table
   * @return
   */
  private String buildCreate() {
    Preconditions.checkArgument(StringUtils.isNotBlank(this.table));
    Preconditions.checkArgument(!this.columnDatatypePairs.isEmpty());

    String cmd = "CREATE TABLE " + this.table
        + " ( " + StringUtils.join(this.columnDatatypePairs, ",")
        + (StringUtils.isNotBlank(this.primaryKey) ? ",PRIMARY KEY (" + this.primaryKey + ")" : "")
        + " );";
    return cmd;
  }

  /**
   * build a command for dropping a table
   * @return
   */
  private String buildDrop() {
    Preconditions.checkArgument(StringUtils.isNotBlank(this.table));

    String cmd = "DROP TABLE " + this.table + ";";
    return cmd;
  }

  /**
   * build a command for altering a table
   * @return
   */
  private String buildAlter() {
    // TODO
    return null;
  }

  @Override
  public TableCmdBuilder table(String table) {
    this.table = table;
    return this;
  }

  @Override
  public <T> TableCmdBuilder columnDatatype(String column, T type) {
    Preconditions.checkNotNull(type);
    Preconditions.checkArgument(type instanceof DataType);
    return fieldDatatype(column, (DataType) type);
  }

  private TableCmdBuilder fieldDatatype(String column, DataType type) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkNotNull(type);
    this.columnDatatypePairs.add(column + " " + type.toString());
    return this;
  }

  @Override
  public TableCmdBuilder primaryKey(String column) {
    this.primaryKey = column;
    return this;
  }

  @Override
  public TableCmdBuilder compositePrimaryKey(List<String> columns) {
    this.primaryKey = StringUtils.join(columns, ",");
    return this;
  }

}
