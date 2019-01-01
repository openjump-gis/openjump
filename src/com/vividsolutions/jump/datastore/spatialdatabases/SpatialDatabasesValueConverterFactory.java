package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;
import java.sql.*;
import com.vividsolutions.jump.datastore.jdbc.*;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
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
//      try { 
//        JUMPWorkbench.getInstance().getFrame().log("creating a SpatialDatabasesValueConverterFactory (class:" + this.getClass() 
//            + " ) (driver: " + conn.getMetaData().getDriverName() + ") id"
//            + this.hashCode(), this.getClass());
//      } catch (SQLException ex) {
//        ex.printStackTrace();
//      }
    this.conn = conn;
  }

  /**
   * Base class to get converter from factory.
   * Should never be called !!
   * @param rsm
   * @param columnIndex
   * @return
   * @throws SQLException 
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
        return wktReader.read("GEOMETRYCOLLECTION EMPTY");
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
      Geometry geometry = null;
      if (bytes == null || bytes.length <= 0) {
        geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
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
      Geometry geometry = null;
      if (bytes == null || bytes.length <= 0) {
        geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
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
