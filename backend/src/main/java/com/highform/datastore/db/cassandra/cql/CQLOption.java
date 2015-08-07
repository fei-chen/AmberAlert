package com.highform.datastore.db.cassandra.cql;

import com.highform.datastore.db.DataTypeConverter;
import com.highform.datastore.db.Option;
import com.highform.db.cassandra.proto.Cassandra.ConsistencyLevel;

public class CQLOption implements Option {


  public enum CQLOptionType {
    CONSISTENCY("CONSISTENCY"),
    TIMESTAMP("TIMESTAMP"),
    TTL("TTL");

    private final String opStr;

    private CQLOptionType(String opStr) {
      this.opStr = opStr;
    }

    @Override
    public String toString() { return this.opStr; }
  }

  private String opStr;
  private String valueStr;

  private <T> void setup(CQLOptionType op, T value) {
    this.opStr = op.toString();
    this.valueStr = DataTypeConverter.toString(value);
  }

  @Override
  public String build() {
    return this.opStr + " " + this.valueStr;
  }

  public CQLOption consistencyLevel(ConsistencyLevel level) {
    setup(CQLOptionType.CONSISTENCY, level);
    return this;
  }

  public CQLOption timestamp(Long time) {
    setup(CQLOptionType.TIMESTAMP, time);
    return this;
  }

  public CQLOption TTL(Long time) {
    setup(CQLOptionType.TTL, time);
    return this;
  }
}
