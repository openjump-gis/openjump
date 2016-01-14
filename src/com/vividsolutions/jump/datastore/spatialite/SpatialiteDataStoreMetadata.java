package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.jdbc.BoundQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Spatialite connexion metadata. Some extra processing occurs here: telling if
 * spatialite extension is loaded and what type of geo metatada are present:
 *
 * @author nicolas Ribot
 */
public class SpatialiteDataStoreMetadata extends SpatialDataStoreMetadata {

  public static String GC_COLUMN_NAME = "geometry_columns";
  public static String GPKG_GC_COLUMN_NAME = "gpkg_geometry_columns";

  //TODO= variables for all SQL code + String.format.
  /**
   * True if spatialite mod extension loaded
   */
  private boolean spatialiteLoaded;
  /**
   * spatialite version
   */
  private String spatialiteVersion;
  /**
   * The geometry_columns table layout for this connection
   */
  private GeometryColumnsLayout geometryColumnsLayout;

  /**
   * The map of geometric columns types (WKB, WKT, SPATIALITE)
   */
  private Map<String, GeometricColumnType> geoColTypesdMap = null;

  /**
   * The query to get the list of geometric columns data types, used to build a
   * suitable SQL query OJ can read
   */
  private String geoColumnTypesQuery = null;
  
  // To manage several Spatial MD layouts
  private String datasetNameQuery2 = null;
  private String sridQuery2 = null;
  private String geoColumnsQuery2 = null;
  private String geoColumnsQuery3 = null;
  private String spatialExtentQuery1_2 = null;
  private String spatialExtentQuery1_3 = null;
  
  /**
   *
   * @param con
   */
  public SpatialiteDataStoreMetadata(DataStoreConnection con) {
    conn = con;
    this.spatialiteLoaded = false;
    this.spatialiteVersion = "";
    this.geometryColumnsLayout = GeometryColumnsLayout.NO_LAYOUT;
    this.geoColTypesdMap = new HashMap<String, GeometricColumnType>();

    checkSpatialiteLoaded();
    setGeoColLayout();

    // formats queries to use for this connection according to the detected layout
    geoColumnTypesQuery = "select f_table_name, f_geometry_column, \"SPATIALITE\" as geometry_format from geometry_columns";
    if (this.getGeometryColumnsLayout() == GeometryColumnsLayout.FDO_LAYOUT) {
      // MD table contains a geometry_format column: query it
      geoColumnTypesQuery = "select f_table_name, f_geometry_column, geometry_format from geometry_columns";
    } else if (this.getGeometryColumnsLayout() == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      // MD table contains a geometry_format column: query it
      geoColumnTypesQuery = "select table_name as f_table_name, column_name as "
          + "f_geometry_columns, \"SPATIALITE\" as geometry_format  from gpkg_geometry_columns";
    }

    // done here as every connection needs it
    getGeoColumnType();

    datasetNameQuery = "SELECT DISTINCT '' as f_table_schema, f_table_name FROM geometry_columns";
    datasetNameQuery2 = "SELECT DISTINCT '' as f_table_schema, table_name as f_table_name FROM gpkg_geometry_columns";

    defaultSchemaName = "";
    spatialDbName = isSpatialiteLoaded() ? "Spatialite" : "SQLite";
    spatialExtentQuery1 = "select st_asBinary(extent(st_geomFromWkb(\"%s\"))) from \"%s\"";
    spatialExtentQuery1_2 = "select st_asBinary(extent(st_geomFromText(\"%s\"))) from \"%s\"";
    spatialExtentQuery1_3 = "select st_asBinary(extent(CastAutomagic(\"%s\"))) from \"%s\"";
    // no second query for spatialite
    spatialExtentQuery2 = null;
    
    sridQuery = "SELECT srs_id FROM gpkg_geometry_columns where table_name = ? and column_name = ?";
    sridQuery2 = "SELECT srid FROM geometry_columns where f_table_name = ? and f_geometry_column = ?";
    
    // geo column query needs to be built occording to geometryColumnsLayout
    geoColumnsQuery = "SELECT f_geometry_column, srid,\n"
        + "  case\n"
        + "    when geometry_type = 1 then 'POINT'\n"
        + "    when geometry_type = 2 then 'LINESTRING'\n"
        + "    when geometry_type = 3 then 'POLYGON'\n"
        + "    when geometry_type = 4 then 'MULTIPOINT'\n"
        + "    when geometry_type = 5 then 'MULTILINESTRING'\n"
        + "    when geometry_type = 6 then 'MULTIPOLYGON'\n"
        + "    when geometry_type = 7 then 'GEOMETRY COLLECTION'\n"
        + "    else geometry_type end as geometry_type\n"
        + "FROM geometry_columns where f_table_name = ?";
      geoColumnsQuery2 = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_name = ?";
      geoColumnsQuery3 = "SELECT column_name, srs_id, geometry_type_name FROM gpkg_geometry_columns where table_name = ?";
  }

  @Override
  public BoundQuery getDatasetNameQuery() {
    String s = (this.getGeometryColumnsLayout() == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) ?
        datasetNameQuery2 : datasetNameQuery;
    return new BoundQuery(s);
  }
  
  @Override
  public BoundQuery getSpatialExtentQuery1(String schema, String table, String attributeName) {
    GeometricColumnType gcType = this.geoColTypesdMap.get(table.toLowerCase() + "." + attributeName.toLowerCase());

    String s = null;
    if (gcType == null) {
      // dummy query if no geo type detected
      s = "select 1";
    }
    // TODO: switch case
    if (this.isSpatialiteLoaded()) {
      if (gcType == GeometricColumnType.WKB) {
        // quotes identifier.
        s = String.format(spatialExtentQuery1, attributeName, table);
      } else if (gcType == GeometricColumnType.WKT) {
        s = String.format(spatialExtentQuery1_2, attributeName, table);
      } else if (gcType == GeometricColumnType.SPATIALITE) {
        s = String.format(spatialExtentQuery1_3, attributeName, table);
      } else {
        // unknown geom type
        // TODO: log
        System.out.println("Unknown geo column type for: " + table + "." + attributeName + " : " + gcType);
        s = "select 1";
      }
    } 
    return new BoundQuery(s);
  }

  /**
   * no second query for spatialite: defaults to query1
   * @param schema
   * @param table
   * @param attributeName
   * @return 
   */
  @Override
  public BoundQuery getSpatialExtentQuery2(String schema, String table, String attributeName) {
    return getSpatialExtentQuery1(schema, table, attributeName);
  }

  private void checkSpatialiteLoaded() {
    // tries to load spatialite, assuming it is available on the system's path
    Statement stmt = null;
    try {
      stmt = conn.getJdbcConnection().createStatement();
      stmt.executeUpdate("SELECT load_extension('mod_spatialite')");
      // ex is thrown if extension cannot be loaded
      this.spatialiteLoaded = true;
      ResultSet rs = stmt.executeQuery("select spatialite_version()");
      rs.next();
      this.setSpatialiteVersion(rs.getString(1));

      JUMPWorkbench.getInstance().getFrame().log(
          "SpatialDatabasesPlugin: Spatialite extension loaded for this connexion, version: "
          + this.getSpatialiteVersion(), this.getClass());
    } catch (Exception e) {
      JUMPWorkbench.getInstance().getFrame().log(
          "SpatialDatabasesPlugin: CANNOT load Spatialite Extention (mod_spatialite), reason:"
          + e.getMessage(), this.getClass());
    } finally {
      try {
        stmt.close();
      } catch (Throwable th) {
        // TODO: log
        th.printStackTrace();
      }
    }
  }

  /**
   * schemaName not use for Spatialite. query bound with tableName and colName.
   * @param schemaName
   * @param tableName
   * @param colName
   * @return 
   */
  @Override
  public BoundQuery getSridQuery(String schemaName, String tableName, String colName) {
    String s = (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) ?
        sridQuery : sridQuery2;

    return new BoundQuery(s).addParameter(tableName).addParameter(colName);
  }

  @Override
  public BoundQuery getGeoColumnsQuery(String datasetName) {
    String s = null;
    
    if (this.geometryColumnsLayout == GeometryColumnsLayout.FDO_LAYOUT
        || this.geometryColumnsLayout == GeometryColumnsLayout.OGC_OGR_LAYOUT) {
      s = geoColumnsQuery;
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_SPATIALITE_LAYOUT) {
      s = geoColumnsQuery2;
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      s = geoColumnsQuery3;
    } else {
      // default to dummy query
      s = "SELECT '' ";
    }
    return new BoundQuery(s).addParameter(getTableName(datasetName));
  }
  
  
  

  /**
   * Sets the geometry_column layout in this sqlite database: either FDO or OGC
   * or GeoPkg or no layout. Also tries to build the geo col type if
   * geometry_columns table contains such info TODO: generic mechanism to get
   * geo col type for Spatialite.
   *
   * Geometry_columns metadata table may have 4 layouts: options used to create
   * the table or using a geo package (http://www.geopackage.org/) layout 1°)
   * the "FDO provider for spatialite
   * (https://trac.osgeo.org/fdo/wiki/FDORfc16)", as used in "regular sqlite
   * database" (cf.ogr spatialite format doc): f_table_name	TEXT
   * f_geometry_column	TEXT geometry_type	INTEGER coord_dimension	INTEGER srid
   * INTEGER geometry_format	TEXT 2°) the "OGC Spatialite" flavour, as
   * understood by qgis for instance, as used in spatialite-enabled sqlite
   * database: f_table_name VARCHAR f_geometry_column VARCHAR type VARCHAR
   * coord_dimension INTEGER srid INTEGER spatial_index_enabled INTEGER 3°) the
   * "OGC OGR" layout: f_table_name VARCHAR f_geometry_column VARCHAR
   * geometry_type VARCHAR coord_dimension INTEGER srid INTEGER
   * spatial_index_enabled INTEGER 3°) the "OGC GeoPackage" layout, as
   * specificed by standard: table_name TEXT NOT NULL, column_name TEXT NOT
   * NULL, geometry_type_name TEXT NOT NULL, srs_id INTEGER NOT NULL, z INTEGER
   * NOT NULL, m INTEGER NOT NULL,
   *
   */
  private void setGeoColLayout() {
    DatabaseMetaData dbMd = null;
    try {
      dbMd = this.conn.getJdbcConnection().getMetaData();

      // GeoPackage test:
      ResultSet rs = dbMd.getTables(null, null, SpatialiteDataStoreMetadata.GPKG_GC_COLUMN_NAME, null);
      if (rs.next()) {
        // no need to look at table layout: table name found is enough to say its geoPackage layout
        geometryColumnsLayout = GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT;
        rs.close();
      } else {

        // OGC/FDO layout
        rs = dbMd.getTables(null, null, SpatialiteDataStoreMetadata.GC_COLUMN_NAME, null);
        if (rs.next()) {
          // tableName is third column in this metadata resultSet
          String col = rs.getString(3);

          // TODO: clean-up the JDBC metadata use...
          boolean isGC = (SpatialiteDataStoreMetadata.GC_COLUMN_NAME.equalsIgnoreCase(col)
              || SpatialiteDataStoreMetadata.GPKG_GC_COLUMN_NAME.equalsIgnoreCase(col));

          // gc layout
          if (isGC) {
            rs = dbMd.getColumns(null, null, SpatialiteDataStoreMetadata.GC_COLUMN_NAME, null);
            int i = 0;

            i = 0;
            String geoTypeCol = "";
            String extraInfoCol = "";
            while (rs.next()) {
              // assume columns order is respected when gc table is created.
              // TODO: enhance this
              if (i == 2) {
                geoTypeCol = rs.getString(4);
              }
              if (i == 5) {
                extraInfoCol = rs.getString(4);
              }
              i++;
            }
            if (geoTypeCol.equalsIgnoreCase("geometry_type") && extraInfoCol.equalsIgnoreCase("geometry_format")) {
              geometryColumnsLayout = GeometryColumnsLayout.FDO_LAYOUT;
            } else if (geoTypeCol.equalsIgnoreCase("type") && extraInfoCol.equalsIgnoreCase("spatial_index_enabled")) {
              geometryColumnsLayout = GeometryColumnsLayout.OGC_SPATIALITE_LAYOUT;
            } else if (geoTypeCol.equalsIgnoreCase("geometry_type") && extraInfoCol.equalsIgnoreCase("spatial_index_enabled")) {
              geometryColumnsLayout = GeometryColumnsLayout.OGC_OGR_LAYOUT;
            } else {
              geometryColumnsLayout = GeometryColumnsLayout.NO_LAYOUT;
            };
            rs.close();
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      //TODO: logging
      System.out.println("error getting geometry_column layout: " + e.getMessage());
    }
  }

  /**
   * builds the map of geometric columns database type: WKB, WKT, SPATIALITE to
   * be able to build custom queries for extent and geo type retrieval. The
   * geometry_format column of the metadata will be queries to find geometry
   * type (column only detected in the FDO_LAYOUT format). For other layout,
   * will default to SPATIALITE type
   */
  private void getGeoColumnType() {
    try {
//      JDBCUtil.execute(
//          conn.getJdbcConnection(),
//          this.geoColumnTypesQuery,
//          new ResultSetBlock() {
//            public void yield(ResultSet resultSet) throws SQLException {
//              while (resultSet.next()) {
//                // force lowercase as JDBC metadata and OGC spatialite metadata can return
//                // different cases for the same geometric column
//                String table = resultSet.getString(1).toLowerCase();
//                String col = resultSet.getString(2).toLowerCase();
//                GeometricColumnType gcType = GeometricColumnType.valueOf(resultSet.getString(3));
//                geoColTypesdMap.put(table + "." + col, gcType);
//              }
//            }
//          });

      // preparedStatement now
      JDBCUtil.query(
          conn.getJdbcConnection(),
          new BoundQuery(this.geoColumnTypesQuery),
          new ResultSetHandler() {
            @Override
            public Object handle(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                // force lowercase as JDBC metadata and OGC spatialite metadata can return
                // different cases for the same geometric column
                String table = resultSet.getString(1).toLowerCase();
                String col = resultSet.getString(2).toLowerCase();
                GeometricColumnType gcType = GeometricColumnType.valueOf(resultSet.getString(3));
                geoColTypesdMap.put(table + "." + col, gcType);
              }
              return null;
            }
          });

    } catch (Exception e) {
      //TODO...
    }
  }

  public boolean isSpatialiteLoaded() {
    return spatialiteLoaded;
  }

  public String getSpatialiteVersion() {
    return spatialiteVersion;
  }

  public void setSpatialiteVersion(String spatialiteVersion) {
    this.spatialiteVersion = spatialiteVersion;
  }

  public GeometryColumnsLayout getGeometryColumnsLayout() {
    return geometryColumnsLayout;
  }

  public Map<String, GeometricColumnType> getGeoColTypesdMap() {
    return geoColTypesdMap;
  }

}
