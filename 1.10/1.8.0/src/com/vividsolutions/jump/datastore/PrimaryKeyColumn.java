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
        if (sqlTypeName.startsWith("character")) this.sqlType = Types.VARCHAR;
        else if (sqlTypeName.startsWith("text")) this.sqlType = Types.VARCHAR;
        else if (sqlTypeName.startsWith("integer")) this.sqlType = Types.INTEGER;
        else if (sqlTypeName.startsWith("big")) this.sqlType = Types.BIGINT;
        else if (sqlTypeName.startsWith("bit")) this.sqlType = Types.BIT;
        else if (sqlTypeName.startsWith("boolean")) this.sqlType = Types.BOOLEAN;
        else if (sqlTypeName.startsWith("date")) this.sqlType = Types.DATE;
        else if (sqlTypeName.startsWith("boolean")) this.sqlType = Types.BOOLEAN;
        else if (sqlTypeName.startsWith("double")) this.sqlType = Types.DOUBLE;
        else if (sqlTypeName.startsWith("boolean")) this.sqlType = Types.BOOLEAN;
        else if (sqlTypeName.startsWith("numeric")) this.sqlType = Types.NUMERIC;
        else if (sqlTypeName.startsWith("real")) this.sqlType = Types.REAL;
        else if (sqlTypeName.startsWith("smallint")) this.sqlType = Types.SMALLINT;
        else if (sqlTypeName.startsWith("serial")) this.sqlType = Types.INTEGER;
        else if (sqlTypeName.startsWith("time")) this.sqlType = Types.TIME;
        else if (sqlTypeName.startsWith("timestamp")) this.sqlType = Types.TIMESTAMP;
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
