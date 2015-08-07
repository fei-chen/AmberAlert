/**
 * Copyright at Bloomreach Inc. All rights reserved Oct 9, 2013.
 */

package com.bloomreach.db.mysql.sql;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.db.TableCmdBuilder;
import com.bloomreach.db.mysql.proto.Mysql.DataType;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * SQL table command builder for building mysql table command
 *
 * @author fei
 */
public class SQLTableCmdBuilder implements TableCmdBuilder {

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

  private SQLTableCmdBuilder(Operator op) {
    this.op = op;
  }

  /**
   * Generate a SQLTableCmdBuilder with CREATE operator
   */
  public static SQLTableCmdBuilder create() {
    return new SQLTableCmdBuilder(Operator.CREATE);
  }

  /**
   * Generate a SQLTableCmdBuilder with DROP operator
   */
  public static SQLTableCmdBuilder drop() {
    return new SQLTableCmdBuilder(Operator.DROP);
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
      isValid = StringUtils.isNotBlank(this.table) && !this.columnDatatypePairs.isEmpty() && StringUtils.isNotBlank(this.primaryKey);
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
    Preconditions.checkArgument(false, "buildAlter is not supported");
    return null;
  }

  @Override
  public TableCmdBuilder table(String table) {
    Preconditions.checkArgument(StringUtils.isNotBlank(table));
    this.table = table;
    return this;
  }

  @Override
  public <T> TableCmdBuilder columnDatatype(String column, T type) {
    Preconditions.checkNotNull(type);
    Preconditions.checkArgument(type instanceof DataType);
    return columnDatatype(column, (DataType) type, "");
  }

  @Override
  public <T> TableCmdBuilder columnDatatype(String column, T type, String params) {
    Preconditions.checkNotNull(type);
    Preconditions.checkArgument(type instanceof DataType);
    return columnDatatype(column, (DataType) type, params);
  }

  private TableCmdBuilder columnDatatype(String column, DataType type, String params) {
    Preconditions.checkArgument(StringUtils.isNotBlank(column));
    Preconditions.checkNotNull(type);
    String columnDatatypePair = column + " " + type.toString()
        + ( StringUtils.isNotBlank(params) ? " " + params : "" );
    this.columnDatatypePairs.add(columnDatatypePair);
    return this;
  }

  @Override
  public TableCmdBuilder primaryKey(String column) {
    this.primaryKey = column;
    return this;
  }

  @Override
  public TableCmdBuilder compositePrimaryKey(List<String> columns) {
    Preconditions.checkArgument(false, "compositePrimaryKey is not supported");
    return null;
  }

}
