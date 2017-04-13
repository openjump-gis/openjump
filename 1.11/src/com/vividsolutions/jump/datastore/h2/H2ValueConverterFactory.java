package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.jdbc.ValueConverter;
import com.vividsolutions.jump.datastore.jdbc.ValueConverterFactory;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesValueConverterFactory;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Factory to convert H2 geometric data type.
 */
public class H2ValueConverterFactory extends SpatialDatabasesValueConverterFactory {

    public H2ValueConverterFactory(Connection conn) {
        super(conn);
    }

    @Override
    public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
            throws SQLException {
        String dbTypeName = rsm.getColumnTypeName(columnIndex);
        //System.out.println(dbTypeName + " " + rsm.getColumnType(columnIndex) + " " + rsm.getColumnClassName(columnIndex));

        // manages 2 cases: type retrieved from Database metadata (DataStore Panel)
        // and from direct Adhoc query (type of the column resultset).
        if ("GEOMETRY".equalsIgnoreCase(dbTypeName)) {
            return WKB_GEOMETRY_MAPPER;
        }

        // handle the standard types
        ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
        if (stdConverter != null) {
            return stdConverter;
        }
        // default - can always show it as a string!
        return ValueConverterFactory.STRING_MAPPER;
    }
}
