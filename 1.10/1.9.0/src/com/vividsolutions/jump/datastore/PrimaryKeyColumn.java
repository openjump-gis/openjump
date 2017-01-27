package com.vividsolutions.jump.datastore;

import java.sql.Types;

/**
 * Metadata about a Primary Key column
 */
public class PrimaryKeyColumn {

    private String name;
    int sqlType;
    String sqlTypeName;

    public PrimaryKeyColumn(String name) {
        this.name = name;
    }

    public PrimaryKeyColumn(String name, String sqlTypeName) {
        this(name);
        this.sqlTypeName = sqlTypeName;
        if (sqlTypeName.equals("character")) this.sqlType = Types.VARCHAR;
        else if (sqlTypeName.equals("character varying")) this.sqlType = Types.VARCHAR;
        else if (sqlTypeName.equals("text")) this.sqlType = Types.VARCHAR;
        else if (sqlTypeName.equals("integer")) this.sqlType = Types.INTEGER;
        else if (sqlTypeName.equals("bigint")) this.sqlType = Types.BIGINT;
        else if (sqlTypeName.equals("bigserial")) this.sqlType = Types.BIGINT;
        else if (sqlTypeName.equals("bit")) this.sqlType = Types.BIT;
        else if (sqlTypeName.equals("boolean")) this.sqlType = Types.BOOLEAN;
        else if (sqlTypeName.equals("date")) this.sqlType = Types.DATE;
        else if (sqlTypeName.equals("double")) this.sqlType = Types.DOUBLE;
        else if (sqlTypeName.equals("double precision")) this.sqlType = Types.DOUBLE;
        else if (sqlTypeName.equals("json")) this.sqlType = Types.VARCHAR;
        else if (sqlTypeName.equals("numeric")) this.sqlType = Types.NUMERIC;
        else if (sqlTypeName.equals("real")) this.sqlType = Types.REAL;
        else if (sqlTypeName.equals("smallint")) this.sqlType = Types.SMALLINT;
        else if (sqlTypeName.equals("serial")) this.sqlType = Types.BIGINT;
        else if (sqlTypeName.equals("timestamp")) this.sqlType = Types.TIMESTAMP;
        else if (sqlTypeName.equals("timestamp with time zone")) this.sqlType = Types.TIMESTAMP;
        else if (sqlTypeName.equals("timestamp without time zone")) this.sqlType = Types.TIMESTAMP;
        else if (sqlTypeName.equals("time")) this.sqlType = Types.TIME;
        else this.sqlType = Types.JAVA_OBJECT;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return sqlType;
    }

    public String getSqlTypeName() {
        return sqlTypeName;
    }

    public String toString() {
        return name + " (" + sqlTypeName + ")";
    }

}
