package com.vividsolutions.jump.datastore.spatialdatabases;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import java.sql.*;
import com.vividsolutions.jump.datastore.jdbc.*;
import com.vividsolutions.jump.feature.AttributeType;
import java.io.IOException;

/**
 *
 */
public class SpatialDatabasesValueConverterFactory {

  protected final ValueConverter WKT_GEOMETRY_MAPPER = new SpatialDatabasesValueConverterFactory.WKTGeometryValueConverter();
  protected final ValueConverter WKB_GEOMETRY_MAPPER = new SpatialDatabasesValueConverterFactory.WKBGeometryValueConverter();
  public final ValueConverter WKB_OBJECT_MAPPER = new SpatialDatabasesValueConverterFactory.WKBObjectValueConverter();

  protected final WKBReader wkbReader = new WKBReader();
  protected final WKTReader wktReader = new WKTReader();

  protected final Connection conn;

  public SpatialDatabasesValueConverterFactory(Connection conn) {
    this.conn = conn;
  }

  /**
   * Base class to get converter from factory.
   * Should never be called !!
   * @param rsm a ResultSetMetaData
   * @param columnIndex column index
   * @return the value converter to use for this column
   * @throws SQLException if the server throws an exception during ResultSetMetaData reading
   * @throws UnsupportedOperationException if the method is not implemented
   */
  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException {
      throw new UnsupportedOperationException();
  }

  class WKTGeometryValueConverter implements ValueConverter {

    public AttributeType getType() {
      return AttributeType.GEOMETRY;
    }

    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException {
      Object valObj = rs.getObject(columnIndex);
      if (valObj == null) {
        // 2024-06-29 now return null if geometry is null
        // This behaviour is more predictable and useful for geometries embeded in a Object type attribute
        // For the main GEOMETRY, null are changed into GeometryCollection by the SQL query
        //return wktReader.read("GEOMETRYCOLLECTION EMPTY");
        return null;
      } else {
        return wktReader.read(valObj.toString());
      }
    }
  }

  class WKBGeometryValueConverter implements ValueConverter {

    public AttributeType getType() {
      return AttributeType.GEOMETRY;
    }

    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException {
      byte[] bytes = rs.getBytes(columnIndex);

      //so rs.getBytes will be one of two things:
      //1. The actual bytes of the WKB if someone did ST_AsBinary
      //2. The bytes of hex representation of the WKB.
      //in the case of #1, according to the WKB spec, the byte value
      //can only be 0 or 1.
      //in the case of #2, it's a hex string, so values range from ascii 0-F
      //use this logic to determine how to process the bytes.
      Geometry geometry;
      if (bytes == null || bytes.length == 0) {
        // 2024-06-29 now return null if geometry is null
        // This behaviour is more predictable and useful for geometries embeded in a Object type attribute
        // For the main GEOMETRY, null are changed into GeometryCollection by the SQL query
        //geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
        geometry = null;
      } else if (new String(new byte[]{bytes[0]}).matches("[GLMP]")) {
        geometry = wktReader.read(new String(bytes));
      } else {
        //assume it's the actual bytes (from ST_AsBinary)
        byte[] realWkbBytes = bytes;
        if (bytes[0] >= '0') {
          //ok, it's hex, convert hex string to actual bytes
          String hexString = new String(bytes);
          realWkbBytes = WKBReader.hexToBytes(hexString);
        }

        geometry = wkbReader.read(realWkbBytes);
      }

      return geometry;
    }
  }

  class WKBObjectValueConverter implements ValueConverter {

    public AttributeType getType() {
      return AttributeType.OBJECT;
    }

    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException {
      byte[] bytes = rs.getBytes(columnIndex);

            //so rs.getBytes will be one of two things:
      //1. The actual bytes of the WKB if someone did ST_AsBinary
      //2. The bytes of hex representation of the WKB.
            //in the case of #1, according to the WKB spec, the byte value
      //can only be 0 or 1.
      //in the case of #2, it's a hex string, so values range from ascii 0-F
      //use this logic to determine how to process the bytes.
      Geometry geometry;
      if (bytes == null || bytes.length == 0) {
        // 2024-06-29 now return null if geometry is null
        // This behaviour is more predictable and useful for geometries embeded in a Object type attribute
        // For the main GEOMETRY, null are changed into GeometryCollection by the SQL query
        // geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
        geometry = null;
      } else {
        //assume it's the actual bytes (from ST_AsBinary)
        byte[] realWkbBytes = bytes;
        if (bytes[0] >= '0') {
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
