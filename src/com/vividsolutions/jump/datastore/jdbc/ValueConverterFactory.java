package com.vividsolutions.jump.datastore.jdbc;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.util.FlexibleDateParser;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;


/**
 * Standard data converters for JDBC.
 * Clients can extend this class, or simply call it.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class ValueConverterFactory {

    public static final ValueConverter DOUBLE_MAPPER = new DoubleConverter();
    public static final ValueConverter INTEGER_MAPPER = new IntegerConverter();
    public static final ValueConverter DATE_MAPPER = new DateConverter();
    public static final ValueConverter STRING_MAPPER = new StringConverter();
    public static final ValueConverter LONG_MAPPER = new LongConverter();
    public static final ValueConverter BOOLEAN_MAPPER = new BooleanConverter();
    public static final ValueConverter OBJECT_MAPPER = new ObjectConverter();

    public ValueConverterFactory() {}

    /**
     * Handles finding a converter for standard JDBC types.
     * Clients should handle custom types themselves.
     *
     * @return null if no converter could be found
     * @throws SQLException
     */
    public static ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex) throws SQLException {

        int sqlType = rsm.getColumnType(columnIndex);

        if (sqlType == Types.INTEGER
                || sqlType == Types.SMALLINT
                || sqlType == Types.TINYINT)
            return INTEGER_MAPPER;

        if (sqlType == Types.DECIMAL
                || sqlType == Types.DOUBLE
                || sqlType == Types.FLOAT
                || sqlType == Types.NUMERIC
                || sqlType == Types.REAL)
            return DOUBLE_MAPPER;

        if (sqlType == Types.DATE
                || sqlType == Types.TIME
                || sqlType == Types.TIMESTAMP)
            return DATE_MAPPER;

        //[mmichaud 2013-08-07] used to store bigint database primary key
        // into a AttributeType.Object attribute
        if (sqlType == Types.BIGINT
                || sqlType == Types.ROWID)
            return LONG_MAPPER;

        if (sqlType == Types.VARCHAR
                || sqlType == Types.CHAR
                || sqlType == Types.CLOB
                || sqlType == Types.LONGNVARCHAR
                || sqlType == Types.LONGVARCHAR
                || sqlType == Types.NCHAR
                || sqlType == Types.NCLOB
                || sqlType == Types.NVARCHAR
                || sqlType == Types.SQLXML)
            return STRING_MAPPER;

        if (sqlType == Types.BIT
                || sqlType == Types.BOOLEAN) {
            return BOOLEAN_MAPPER;
        }

        if (sqlType == Types.BINARY
                || sqlType == Types.VARBINARY
                || sqlType == Types.BLOB
                || sqlType == Types.ARRAY
                || sqlType == Types.JAVA_OBJECT
                || sqlType == Types.LONGVARBINARY) {
            return OBJECT_MAPPER;
        }

        // default is null
        return null;
    }

    public static class IntegerConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.INTEGER; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            int value = rs.getInt(columnIndex);
            return rs.wasNull() ? null : value;
        }
    }

    public static class  DoubleConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.DOUBLE; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            double value = rs.getDouble(columnIndex);
            return rs.wasNull() ? null : value;
        }
    }

    public static class  StringConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.STRING; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getString(columnIndex);
        }
    }

    public static class  DateConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.DATE; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
              // always return string for dates and let FlexibleFeature convert later during runtime
              return rs.getString(columnIndex);
//            Object ret = null;
//            try {
//                ret = rs.getTimestamp(columnIndex);
//                if (rs.wasNull()) return null;
//            } catch (Exception e) {
//                // try to read date from string, as some SpatialDatabases like SQLite
//                // can store DATE type in string
//                FlexibleDateParser parser = new FlexibleDateParser();
//                try {
//                    ret = parser.parse(rs.getString(columnIndex), false);
//                } catch (Exception ee) {
//                    System.err.println("cannot parse date value: \"" + rs.getString(columnIndex)
//                    + "\" Defaulting to null.\n" + ee.getMessage());
//                }
//            }
//            return ret;
        }
    }

    //[mmichaud 2013-08-07]
    public static class LongConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.LONG; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            long value = rs.getLong(columnIndex);
            return rs.wasNull() ? null : value;
        }
    }

    // [mmichaud 2015-04-05]
    public static class  BooleanConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.BOOLEAN; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            boolean value = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : value;
        }
    }

    public static class  ObjectConverter implements ValueConverter {
        public AttributeType getType() { return AttributeType.OBJECT; }
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getBytes(columnIndex);
        }
    }
}