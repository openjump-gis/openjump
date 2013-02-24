package com.vividsolutions.jump.datastore.jdbc;

import com.vividsolutions.jump.datastore.*;
import java.sql.*;
import java.io.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.*;


/**
 * Standard data converters for JDBC.
 * Clients can extend this class, or simply call it.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class ValueConverterFactory
{
  public static final ValueConverter DOUBLE_MAPPER = new DoubleConverter();
  public static final ValueConverter INTEGER_MAPPER = new IntegerConverter();
  public static final ValueConverter DATE_MAPPER = new DateConverter();
  public static final ValueConverter STRING_MAPPER = new StringConverter();

  public ValueConverterFactory()
  {

  }

  /**
   * Handles finding a converter for standard JDBC types.
   * Clients should handle custom types themselves.
   *
   * @param rsm
   * @param columnIndex
   * @return null if no converter could be found
   * @throws SQLException
   */
  public static ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException
  {
    String classname = rsm.getColumnClassName(columnIndex);
    String dbTypeName = rsm.getColumnTypeName(columnIndex);
    int precision = rsm.getPrecision(columnIndex);
    int scale = rsm.getScale(columnIndex);
    int sqlType = rsm.getColumnType(columnIndex);

    if (sqlType == Types.INTEGER
        || classname.equalsIgnoreCase("java.lang.Integer")
        || (classname.equalsIgnoreCase("java.math.BigDecimal")
        && precision == 10 && scale == 0))
        return INTEGER_MAPPER;

    if (classname.equalsIgnoreCase("java.math.BigDecimal")
        || sqlType == Types.FLOAT
        || sqlType == Types.REAL
        || sqlType == Types.DOUBLE)
        return DOUBLE_MAPPER;

    if (classname.equalsIgnoreCase("java.sql.Timestamp")
        || classname.equalsIgnoreCase("java.sql.Date"))
        return DATE_MAPPER;

    if (classname.equalsIgnoreCase("java.String"))
      return STRING_MAPPER;

    // default is null
    return null;
  }

  public static class IntegerConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.INTEGER; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws SQLException
    {
      Object value = rs.getObject(columnIndex);
      if (value == null) return null;
      else return new Integer(rs.getInt(columnIndex));
    }
  }

  public static class  DoubleConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.DOUBLE; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws SQLException
    {
      Object value = rs.getObject(columnIndex);
      if (value == null) return null;
      return new Double(rs.getDouble(columnIndex));
    }
  }

  public static class  StringConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.STRING; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws SQLException
    {
      return rs.getString(columnIndex);
    }
  }

  public static class  DateConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.DATE; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws SQLException
    {
      //return rs.getDate(columnIndex);
      return rs.getTimestamp(columnIndex);
    }
  }
}