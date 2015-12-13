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
    
    GeometricColumnType gcType = metadata.getGeoColTypesdMap().get(tableName+"."+columnName);
    if (gcType == null) {
      // not a geo column
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
    } else if (gcType == GeometricColumnType.SPATIALITE) {
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
      returnGeometry = getNativeGeometryFromBlob(geometryBytes);

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
      returnGeometry = wkbReader.read(wkb);

      if (returnGeometry == null) {
        throw new IOException("Unable to parse WKB");
      }

      return returnGeometry;
    }

  }

}
