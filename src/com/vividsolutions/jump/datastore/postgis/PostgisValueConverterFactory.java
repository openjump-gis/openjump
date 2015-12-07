/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.jdbc.ValueConverter;
import com.vividsolutions.jump.datastore.jdbc.ValueConverterFactory;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesValueConverterFactory;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 *
 */
public class PostgisValueConverterFactory extends SpatialDatabasesValueConverterFactory {

    public PostgisValueConverterFactory(Connection conn) {
        super(conn);
    }

    @Override
    public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
        throws SQLException {
        String classname = rsm.getColumnClassName(columnIndex);
        String dbTypeName = rsm.getColumnTypeName(columnIndex);

        // MD - this is slow - is there a better way?
        if (dbTypeName.equalsIgnoreCase("geometry")) // WKB is now the normal way to store geometry in PostGIS [mmichaud 2007-05-13]
        {
            return WKB_GEOMETRY_MAPPER;
        }

        if (dbTypeName.equalsIgnoreCase("bytea")) {
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
