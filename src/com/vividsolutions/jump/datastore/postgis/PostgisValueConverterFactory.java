package com.vividsolutions.jump.datastore.postgis;

import java.sql.*;
import java.io.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.*;
import com.vividsolutions.jump.datastore.jdbc.*;

/**
 *
 */
public class PostgisValueConverterFactory {

    // should lazily init these
    private final ValueConverter WKB_GEOMETRY_MAPPER = new WKBGeometryValueConverter();

    private final Connection conn;
    private static final WKBReader wkbReader = new WKBReader();
    private static final WKTReader wktReader = new WKTReader();

    public PostgisValueConverterFactory(Connection conn) {
    this.conn = conn;
  }

    public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex) throws SQLException {
        String classname = rsm.getColumnClassName(columnIndex);
        String dbTypeName = rsm.getColumnTypeName(columnIndex);
        ResultSet rs = conn.getMetaData().getColumns(
                rsm.getCatalogName(columnIndex),
                rsm.getSchemaName(columnIndex),
                rsm.getTableName(columnIndex),
                rsm.getColumnName(columnIndex));
        String udt = rs.next() ? rs.getString("TYPE_NAME") : "";
        //System.out.println(dbTypeName + " (" + classname + ") [" + udt + "]");

        // A geometry column created with addGeometryColumn has the ColumnTypeName = geometry
        // but if column is created from a statement like "create table2 as select * from table1"
        // its ColumnTypeName is bytea even if its UDT is geometry
        if ("geometry".equals(dbTypeName) || "geometry".equals(udt))
            return WKB_GEOMETRY_MAPPER;

        // handle the standard types
        ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
        if (stdConverter != null)
            return stdConverter;

        // default - can always show it as a string!
        return ValueConverterFactory.STRING_MAPPER;
    }


    static class WKBGeometryValueConverter implements ValueConverter {

        public AttributeType getType() {
            return AttributeType.GEOMETRY;
        }

        public Object getValue(ResultSet rs, int columnIndex) throws IOException, SQLException, ParseException {

            byte[] bytes = rs.getBytes(columnIndex);

            //so rs.getBytes will be one of two things:
            //1. The actual bytes of the WKB if someone did ST_AsBinary
            //2. The bytes of hex representation of the WKB.

            //in the case of #1, according to the WKB spec, the byte value can only be 0 or 1.
            //in the case of #2, it's a hex string, so values range from ascii 0-F
            //use this logic to determine how to process the bytes.

            Geometry geometry = null;
            if(bytes == null || bytes.length <= 0) {
                geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
            }
            else {
                //assume it's the actual bytes (from ST_AsBinary)
                byte[] realWkbBytes = bytes;
                if(bytes[0] >= '0') {
                    //ok, it's hex, convert hex string to actual bytes
                    String hexString = new String(bytes);
                    realWkbBytes = WKBReader.hexToBytes(hexString);
                }
                geometry = wkbReader.read(realWkbBytes);
            }

            return geometry;
        }
    }

}