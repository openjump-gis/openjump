package com.vividsolutions.jump.datastore.ocient;

import com.vividsolutions.jump.datastore.jdbc.ValueConverter;
import com.vividsolutions.jump.datastore.jdbc.ValueConverterFactory;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesValueConverterFactory;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Factory to convert Ocient geometric data type.
 * 
 */
public class OcientValueConverterFactory extends SpatialDatabasesValueConverterFactory {

  public OcientValueConverterFactory(Connection conn) {
    super(conn);
  }

  @Override
  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException {
    String dbTypeName = rsm.getColumnTypeName(columnIndex);

    if ("st_point".equalsIgnoreCase(dbTypeName) ||
    	"st_linestring".equalsIgnoreCase(dbTypeName) ||
    	"st_polygon".equalsIgnoreCase(dbTypeName)) {
      return WKT_GEOMETRY_MAPPER;
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
