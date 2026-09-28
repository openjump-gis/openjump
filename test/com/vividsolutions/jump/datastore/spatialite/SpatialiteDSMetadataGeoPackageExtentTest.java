package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.WKTReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Covers https://github.com/openjump-gis/openjump/issues/47 : a GeoPackage table whose
 * features don't intersect the current map view couldn't be zoomed to, because
 * {@link SpatialiteDSMetadata#getSpatialExtentQuery1} only ever tried Spatialite's
 * {@code extent()} SQL function, which isn't loaded/available for a plain GeoPackage file.
 * GeoPackage tables already record their own extent in {@code gpkg_contents}
 * (min_x/min_y/max_x/max_y) -- these tests build a minimal, real GeoPackage-shaped SQLite
 * database (no native Spatialite extension involved) and confirm that extent is read
 * correctly.
 */
public class SpatialiteDSMetadataGeoPackageExtentTest {

  private Connection conn;

  @Before
  public void setUp() throws Exception {
    Class.forName("org.sqlite.JDBC");
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(
          "CREATE TABLE gpkg_geometry_columns ("
              + " table_name TEXT NOT NULL,"
              + " column_name TEXT NOT NULL,"
              + " geometry_type_name TEXT NOT NULL,"
              + " srs_id INTEGER NOT NULL,"
              + " z TINYINT NOT NULL,"
              + " m TINYINT NOT NULL)");
      stmt.execute(
          "CREATE TABLE gpkg_contents ("
              + " table_name TEXT NOT NULL PRIMARY KEY,"
              + " data_type TEXT NOT NULL,"
              + " min_x DOUBLE,"
              + " min_y DOUBLE,"
              + " max_x DOUBLE,"
              + " max_y DOUBLE)");
    }
  }

  @After
  public void tearDown() throws Exception {
    if (conn != null) conn.close();
  }

  private void insertGeometryColumn(String table) throws Exception {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(
          "INSERT INTO gpkg_geometry_columns VALUES ('" + table + "', 'geom', 'POINT', 4326, 0, 0)");
    }
  }

  private void insertContents(String table, Double minX, Double minY, Double maxX, Double maxY)
      throws Exception {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(
          "INSERT INTO gpkg_contents VALUES ('"
              + table
              + "', 'features', "
              + (minX == null ? "NULL" : minX)
              + ", "
              + (minY == null ? "NULL" : minY)
              + ", "
              + (maxX == null ? "NULL" : maxX)
              + ", "
              + (maxY == null ? "NULL" : maxY)
              + ")");
    }
  }

  private Envelope executeExtentQuery(String sql) throws Exception {
    final Envelope[] result = {null};
    JDBCUtil.execute(
        conn,
        sql,
        new ResultSetBlock() {
          public void yield(ResultSet resultSet) throws Exception {
            if (resultSet.next()) {
              String wkt = resultSet.getString(1);
              if (wkt != null) {
                result[0] = new WKTReader().read(wkt).getEnvelopeInternal();
              }
            }
          }
        });
    return result[0];
  }

  @Test
  public void geoPackageLayoutIsDetected() throws Exception {
    insertGeometryColumn("test_layer");
    insertContents("test_layer", 1.0, 2.0, 3.0, 4.0);

    SpatialiteDSConnection dsConn = new SpatialiteDSConnection(conn);
    SpatialiteDSMetadata metadata = (SpatialiteDSMetadata) dsConn.getMetadata();

    assertEquals(GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT, metadata.getGeometryColumnsLayout());
  }

  @Test
  public void extentQueryReadsGpkgContentsBoundingBox() throws Exception {
    insertGeometryColumn("test_layer");
    insertContents("test_layer", 10.0, 20.0, 30.0, 40.0);

    SpatialiteDSConnection dsConn = new SpatialiteDSConnection(conn);
    SpatialiteDSMetadata metadata = (SpatialiteDSMetadata) dsConn.getMetadata();

    String sql = metadata.getSpatialExtentQuery1("", "test_layer", "geom");
    Envelope envelope = executeExtentQuery(sql);

    assertEquals(new Envelope(10.0, 30.0, 20.0, 40.0), envelope);
  }

  @Test
  public void extentQueryReturnsNoRowsWhenTableHasNoContentsEntry() throws Exception {
    insertGeometryColumn("test_layer");
    // no gpkg_contents row at all for this table

    SpatialiteDSConnection dsConn = new SpatialiteDSConnection(conn);
    SpatialiteDSMetadata metadata = (SpatialiteDSMetadata) dsConn.getMetadata();

    String sql = metadata.getSpatialExtentQuery1("", "test_layer", "geom");
    Envelope envelope = executeExtentQuery(sql);

    assertNull(envelope);
  }

  @Test
  public void extentQueryReturnsNullWhenGpkgContentsBoundsAreNull() throws Exception {
    insertGeometryColumn("test_layer");
    // An empty GeoPackage table is a real, spec-legal state: min_x/min_y/max_x/max_y are null.
    insertContents("test_layer", null, null, null, null);

    SpatialiteDSConnection dsConn = new SpatialiteDSConnection(conn);
    SpatialiteDSMetadata metadata = (SpatialiteDSMetadata) dsConn.getMetadata();

    String sql = metadata.getSpatialExtentQuery1("", "test_layer", "geom");
    Envelope envelope = executeExtentQuery(sql);

    assertNull(envelope);
  }

  /**
   * A single-feature (or single-point-geometry) table is a real, legal state: min_x == max_x
   * and/or min_y == max_y, so the WKT built for the extent is a degenerate polygon (all vertices
   * identical, or collinear). {@link WKTReader} parses this without throwing -- the resulting
   * geometry is invalid as a polygon, but {@link org.locationtech.jts.geom.Geometry#getEnvelopeInternal()}
   * still returns the correct bounding box regardless, which is all this code needs.
   */
  @Test
  public void extentQueryHandlesDegenerateBoundingBoxes() throws Exception {
    insertGeometryColumn("point_layer");
    insertContents("point_layer", 5.0, 5.0, 5.0, 5.0); // single point: min == max on both axes

    insertGeometryColumn("vertical_line_layer");
    insertContents("vertical_line_layer", 5.0, 1.0, 5.0, 4.0); // min_x == max_x only

    insertGeometryColumn("horizontal_line_layer");
    insertContents("horizontal_line_layer", 1.0, 5.0, 4.0, 5.0); // min_y == max_y only

    SpatialiteDSConnection dsConn = new SpatialiteDSConnection(conn);
    SpatialiteDSMetadata metadata = (SpatialiteDSMetadata) dsConn.getMetadata();

    Envelope point = executeExtentQuery(metadata.getSpatialExtentQuery1("", "point_layer", "geom"));
    assertEquals(new Envelope(5.0, 5.0, 5.0, 5.0), point);

    Envelope verticalLine =
        executeExtentQuery(metadata.getSpatialExtentQuery1("", "vertical_line_layer", "geom"));
    assertEquals(new Envelope(5.0, 5.0, 1.0, 4.0), verticalLine);

    Envelope horizontalLine =
        executeExtentQuery(metadata.getSpatialExtentQuery1("", "horizontal_line_layer", "geom"));
    assertEquals(new Envelope(1.0, 4.0, 5.0, 5.0), horizontalLine);
  }
}
