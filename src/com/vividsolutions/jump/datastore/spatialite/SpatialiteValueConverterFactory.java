package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jump.datastore.jdbc.ValueConverter;
import com.vividsolutions.jump.datastore.jdbc.ValueConverterFactory;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesValueConverterFactory;
import com.vividsolutions.jump.feature.AttributeType;
import java.io.IOException;
import java.sql.ResultSet;

/**
 *
 */
public class SpatialiteValueConverterFactory extends SpatialDatabasesValueConverterFactory {

  protected final ValueConverter SPATIALITE_GEOMETRY_MAPPER = new SpatialiteValueConverterFactory.SpatialiteGeometryValueConverter();

  /**
   * propagate the metadata object through Spatialite classes to get access to
   * specific information
   */
  private SpatialiteDSMetadata metadata;

  public SpatialiteValueConverterFactory(Connection conn) {
    super(conn);
  }

  public void setMetadata(SpatialiteDSMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException {
    // Reads type from column and let converter be smart with it:
    // TODO: enum for geo types.
    String dbTypeName = rsm.getColumnTypeName(columnIndex);
    // gets concerned tableName and column name to be able to detect geom column as text
    String tableName = rsm.getTableName(columnIndex).toLowerCase();
    String columnName = rsm.getColumnName(columnIndex).toLowerCase();

    GeometricColumnType gcType = metadata.getGeoColTypesdMap().get(tableName + "." + columnName);
    if (gcType == null) {
      ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
      if (stdConverter != null) {
        return stdConverter;
      }
      // default - can always show it as a string!
      return ValueConverterFactory.STRING_MAPPER;
    } else if (gcType == GeometricColumnType.WKB) {
      return WKB_GEOMETRY_MAPPER;
    } else if (gcType == GeometricColumnType.WKT) {
      return WKT_GEOMETRY_MAPPER;
    } else if (gcType == GeometricColumnType.SPATIALITE
        || gcType == GeometricColumnType.NATIVE) {
      return SPATIALITE_GEOMETRY_MAPPER;
    } else {
      return ValueConverterFactory.STRING_MAPPER;
    }
  }

  /**
   * Custom WKB reader for Spatialite (cf Jump DB Query plugin source code).
   * TODO: refactor, merge two codes.
   */
  class SpatialiteGeometryValueConverter implements ValueConverter {

    public AttributeType getType() {
      return AttributeType.GEOMETRY;
    }

    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException {

      Geometry returnGeometry = null;

      //no FDO info for this table, try native spatialite blob encoding
      byte[] geometryBytes = rs.getBytes(columnIndex);
      if (geometryBytes != null) {
        if (appearsToBeGeopackageGeometry(geometryBytes)) {
          returnGeometry = getGeopackageGeometryFromBlob(geometryBytes);
        } else {
          returnGeometry = getNativeGeometryFromBlob(geometryBytes);
        }
      } else {
        returnGeometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
      }

      return returnGeometry;
    }

    private Geometry getNativeGeometryFromBlob(byte[] blobAsBytes) throws IOException, ParseException {
      Geometry returnGeometry;

      //copy the byte array, removing the MBR at the front,
      //and the ending OxFE byte at the end
      byte[] wkb = new byte[blobAsBytes.length - 39];
      System.arraycopy(blobAsBytes, 39, wkb, 1, blobAsBytes.length - 1 - 39);

      //prepend byte-order byte
      wkb[0] = blobAsBytes[1];

      WKBReader wkbReader = new WKBReader();
      setEwkbGeometryType(wkb);
      returnGeometry = wkbReader.read(wkb);

      if (returnGeometry == null) {
        throw new IOException("Unable to parse WKB");
      }

      return returnGeometry;
    }



    /**
     * From DB Query plugin: TODO: factorize code
     *
     * @param blobAsBytes
     * @return
     * @throws Exception
     */
    private Geometry getGeopackageGeometryFromBlob(byte[] blobAsBytes) throws IOException, ParseException {
      Geometry returnGeometry;

      //first two bytes are GP..
      //Third byte is version
      //Fourth byte is flags
      byte flags = blobAsBytes[3];
      //Bytes 5-8 are SRS ID

      int evelopeSize = 0;

      //FIXME do something with this like Create empty geometry collection??
      // 0b00100000 ==  0X20
      boolean emptyGeometry = (flags & 0X20) != 0;

      int envelopSize = getEnvelopeSize(flags);

      int headerSize = 8 + envelopSize;

      byte[] wkb = new byte[blobAsBytes.length - headerSize];
      System.arraycopy(blobAsBytes, headerSize, wkb, 0, blobAsBytes.length - headerSize);
      WKBReader wkbReader = new WKBReader();
      setEwkbGeometryType(wkb);
      returnGeometry = wkbReader.read(wkb);

      if (returnGeometry == null) {
        throw new IOException("Unable to parse WKB");
      }

      return returnGeometry;
    }

    // JTS only supports postgis ewkb for geometry with Z values
    // following method rewrites a wkb geometry written according to OGC (SQL/MM)
    // to a valid postgis ewkb.
    private void setEwkbGeometryType(byte[] wkb) {
      int byteOrder = wkb[0];
      int geometryType;
      if (byteOrder == 0) {
        geometryType = (wkb[4] & 0xFF) | (wkb[3] & 0xFF) << 8 | (wkb[2] & 0xFF) << 16 | (wkb[1] & 0xFF) << 24;
      } else {
        geometryType = (wkb[1] & 0xFF) | (wkb[2] & 0xFF) << 8 | (wkb[3] & 0xFF) << 16 | (wkb[4] & 0xFF) << 24;
      }
      boolean hasZ = ((geometryType & 0x80000000) != 0) || (geometryType >= 1000 && geometryType < 3000);
      geometryType = (geometryType & 0x0000FFFF)%1000;
      if (byteOrder == 0) {
        wkb[1] = hasZ ? (byte)(wkb[1] | 0x80) : wkb[1];
        wkb[2] = 0x00;
        wkb[3] = 0x00;
        wkb[4] = (byte)(geometryType & 0x000000FF);
      } else {
        wkb[4] = hasZ ? (byte)(wkb[4] | 0x80) : wkb[4];
        wkb[3] = 0x00;
        wkb[2] = 0x00;
        wkb[1] = (byte)(geometryType & 0x000000FF);
      }
    }

    /**
     * From DB Query plugin: TODO: factorize code
     *
     * @param flags
     * @return
     * @throws Exception
     */
    private int getEnvelopeSize(byte flags) throws IOException {
      //0b0000001 == 0x01
      boolean littleEndian = (flags & 0x01) != 0;

      //0b00001110 == 0x0E
      int envelopeCode = (flags & 0x0E) >>> 1;

      //spec says the endian bit sets byte order for "header" values
      //Not sure what 'header" is in this context, but using it with
      //geonames sample provided by Jukka results in bad parsing of BLOB
//      if(littleEndian)
//      {
//         envelopeCode = 0;
//         if( (flags & 0b00001000) != 0)
//         {
//            envelopeCode += 1;
//         }
//
//         if( (flags & 0b00000100) != 0)
//         {
//            envelopeCode += 2;
//         }
//
//         if( (flags & 0b00000010) != 0)
//         {
//            envelopeCode += 4;
//         }
//      }
      int envelopeSize;
      switch (envelopeCode) {
        case 0:
          envelopeSize = 0;
          break;
        case 1:
          envelopeSize = 32;
          break;
        case 2:
        case 3:
          envelopeSize = 48;
          break;
        case 4:
          envelopeSize = 64;
          break;
        default:
          //Envelope codes 5-7 are invalid
          throw new IOException("Invalid envelope code " + envelopeCode);
      }

      return envelopeSize;
    }

  }

  private boolean appearsToBeGeopackageGeometry(byte[] geometryAsBytes) {

    //From http://opengis.github.io/geopackage/#gpb_format
    //Geopackage blobs start with "gp", contain some other header
    //info, and are followed by a WKB
    return (geometryAsBytes.length > 2
        && geometryAsBytes[0] == (byte) 0x47 //G
        && geometryAsBytes[1] == (byte) 0x50 //P
        );
  }

  private boolean appearsToBeNativeGeometry(byte[] geometryAsBytes) {
    boolean blobIsGeometry = false;

    //From http://www.gaia-gis.it/spatialite-2.1/SpatiaLite-manual.html
    //Spatialite geometry blobs are WKB-like, with some specifics to
    //spatialite:  For our purposes, this should be good enough:
    //the 39th byte must be 0x7C (marks MBR end)
    //and the blob must end with 0xFE
    int numBytes = geometryAsBytes.length;

    if (numBytes > 39
        && geometryAsBytes[38] == (byte) 0x7C
        && geometryAsBytes[numBytes - 1] == (byte) 0xFE) {
      blobIsGeometry = true;
    }

    return blobIsGeometry;
  }

}
