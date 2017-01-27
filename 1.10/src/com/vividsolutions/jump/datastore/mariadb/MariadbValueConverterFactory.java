package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jump.datastore.jdbc.ValueConverter;
import com.vividsolutions.jump.datastore.jdbc.ValueConverterFactory;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesValueConverterFactory;
import com.vividsolutions.jump.feature.AttributeType;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 *
 */
public class MariadbValueConverterFactory extends SpatialDatabasesValueConverterFactory {

  protected final ValueConverter MYSQLWKB_GEOMETRY_MAPPER = new MariadbValueConverterFactory.MySQLWKBGeometryValueConverter();

  public MariadbValueConverterFactory(Connection conn) {
    super(conn);
  }

  @Override
  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException {
    String dbTypeName = rsm.getColumnTypeName(columnIndex);

    // manages 2 cases: type retrieved from Database metadata (DataStore Panel)
    // and from direct Adhoc query (type of the column resultset).
    if ("LONGBLOB".equalsIgnoreCase(dbTypeName)) {
      return WKB_GEOMETRY_MAPPER;
    } else if ("GEOMETRY".equalsIgnoreCase(dbTypeName)) {
      return MYSQLWKB_GEOMETRY_MAPPER;
    }

    // handle the standard types
    ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
    if (stdConverter != null) {
      return stdConverter;
    }

    // default - can always show it as a string!
    return ValueConverterFactory.STRING_MAPPER;
  }

  /**
   * Custom WKB reader for MySQL/MariaDB (cf Jump DB Query plugin source code).
   * Provides support for both Binary format is blob with 4 empty bytes as the
   * beginning From Larry Reader code
   */
  class MySQLWKBGeometryValueConverter implements ValueConverter {

    public AttributeType getType() {
      return AttributeType.GEOMETRY;
    }

    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException {
      byte[] bytes = rs.getBytes(columnIndex);

      Geometry geometry = null;
      if (bytes == null || bytes.length < 5) {
        geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
      } else {
        boolean nativeFormat = appearsToBeNativeFormat(bytes);
        WKBReader wr = new WKBReader();

        if (nativeFormat) {
          //copy the byte array, removing the first four
          //zero bytes added by mysql to store SRID in binary
          byte[] wkb = new byte[bytes.length - 4];
          System.arraycopy(bytes, 4, wkb, 0, wkb.length);
          geometry = wr.read(wkb);
        } else {
          // true WKB format as from st_asbinary
          geometry = wr.read(bytes);
          }
      } 

      return geometry;
    }
  }

  /**
   * From Larry Reeder, code to detect MySQL spatial type.
   * Added detection code for older/strange mysql geometry format beginning with 6A 08 00 00 
   * TODO: make method public static in its package ?
   * Newest MySQL/MariaDB version stores srid at the beginning of the blob, as int
   * The JUMP DB Query Plugin is Copyright (C) 2007  Larry Reeder
   *  JUMP is Copyright (C) 2003 Vivid Solutions
   * 
   */
  private boolean appearsToBeNativeFormat(final byte[] geometryAsBytes) {
      //use a heuristic here.  MySQL seems to store
    //geometries as WKB with four leading zero bytes
    //so, the first four should be zero, with the fifth
    //byte being the byte-order byte, which always seems
    //to be 0x01 in MySQL
    int firstFive = geometryAsBytes[0]
        | geometryAsBytes[1]
        | geometryAsBytes[2]
        | geometryAsBytes[3]
        | geometryAsBytes[4];
    byte[] ctrl = javax.xml.bind.DatatypeConverter.parseHexBinary("6A080000");
    byte[] firstFour = new byte[4];
    System.arraycopy(geometryAsBytes, 0, firstFour, 0, firstFour.length);
    boolean nativeFormat = false;

    if ((firstFive & 0xFF) == 0x01) {
          //the next section in WKB is the geometry type.
      //MySql supports types 1-7
      if (geometryAsBytes[5] >= 1 && geometryAsBytes[5] <= 7) {
        nativeFormat = true;
      }

    } else if (true) {
      // Nicolas Ribot: mysql binary stores srid at the beginning of the geom
      nativeFormat = true;
    }
    return nativeFormat;
  }

}
