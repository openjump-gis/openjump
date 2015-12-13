package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Spatialite connexion metadata. Some extra processing occurs here: telling if
 * spatialite extension is loaded and what type of geo metatada are present:
 *
 * @author nicolas Ribot
 */
public class SpatialiteDSMetadata extends SpatialDatabasesDSMetadata {

  public static String GC_COLUMN_NAME = "geometry_columns";

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
   * 
   * @param con 
   */
  public SpatialiteDSMetadata(DataStoreConnection con) {
    conn = con;
    this.spatialiteLoaded = false;
    this.spatialiteVersion = "";
    this.geometryColumnsLayout = GeometryColumnsLayout.NO_LAYOUT;
    this.geoColTypesdMap = new HashMap<String, GeometricColumnType>();

    checkSpatialiteLoaded();
    setGeoColLayout();
    // done here as every connection needs it
    getGeoColumnType();

    // TODO: use bind parameters to avoid SQL injection
    datasetNameQuery = "SELECT DISTINCT '' as f_table_schema, f_table_name FROM geometry_columns";
    defaultSchemaName = "";
    spatialDbName = isSpatialiteLoaded() ? "Spatialite" : "SQLite";
    spatialExtentQuery1 = "SELECT %s from %s";
    spatialExtentQuery2 = "SELECT %s from %s";
    sridQuery = "SELECT srid FROM geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    // geo column query needs to be built occording to geometryColumnsLayout
    if (this.geometryColumnsLayout == GeometryColumnsLayout.FDO_LAYOUT) {
      geoColumnsQuery = "SELECT f_geometry_column, srid, geometry_type FROM geometry_columns where f_table_name = '%s'";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_SPATIALITE_LAYOUT) {
      geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_name = '%s'";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_OGR_LAYOUT) {
      geoColumnsQuery = "SELECT f_geometry_column, srid, geometry_type FROM geometry_columns where f_table_name = '%s'";
    } else {
      geoColumnsQuery = "SELECT '' ";
    }

  }

  @Override
  public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
    // No schema in SQLite, schema param not used
    // must cast the geometric field according to its type, to be able to use spatialite functions.
    //return String.format(spatialExtentQuery1, attributeName, table);
    String ret = "select 1";
    

    GeometricColumnType gcType = this.geoColTypesdMap.get(table.toLowerCase() + "." + attributeName.toLowerCase());

    if (gcType == null) {
      return "select 1";
    }
    // TODO: switch case
    if (this.isSpatialiteLoaded()) {
      if (gcType == GeometricColumnType.WKB) {
        ret = String.format("select st_asBinary(extent(st_geomFromWkb(%s))) from %s", attributeName, table);
      } else if (gcType == GeometricColumnType.WKT) {
        ret = String.format("select st_asBinary(extent(st_geomFromText(%s))) from %s", attributeName, table);
      } else if (gcType == GeometricColumnType.SPATIALITE) {
        ret = String.format("select st_asBinary(extent(%s)) from %s", attributeName, table);
      } else {
        // unknown geom type
        // TODO: log
        System.out.println("Unknown geo column type for: " + table + "." + attributeName + " : " + gcType);
        ret = "select 1";
      }
    } else {
      // spatialite functions not available: extent cannot be found 
      ret = "select 1";
    }
    return ret;
  }

  @Override
  public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
    // only one mechanism to get extent from spatialite
    return getSpatialExtentQuery1(schema, table, attributeName);
  }

  /**
   * No schema in SQLite
   *
   * @param datasetName
   * @return
   */
  @Override
  public String getGeoColumnsQuery(String datasetName) {
    // No schema in SQLite
    return String.format(this.geoColumnsQuery, getTableName(datasetName));
  }

  @Override
  public String getSridQuery(String schemaName, String tableName, String colName) {
    // no schema in sqlite
    return String.format(this.sridQuery, tableName, colName);
  }

  private void checkSpatialiteLoaded() {
    // tries to load spatialite, assuming it is available on the system's path
    Statement stmt = null;
    try {
      stmt = conn.getConnection().createStatement();
      stmt.executeUpdate("SELECT load_extension('mod_spatialite')");
      // ex is thrown if extension cannot be loaded
      this.spatialiteLoaded = true;
      ResultSet rs = stmt.executeQuery("select spatialite_version()");
      rs.next();
      this.setSpatialiteVersion(rs.getString(1));
      //TODO: log
      System.out.println("SpatialDatabasesPlugin: Spatialite extension loaded for this connexion, version: " + this.getSpatialiteVersion());
    } catch (Exception e) {
      System.out.println("SpatialDatabasesPlugin: Cannot load Spatialite Extention (mod_spatialite), reason:" + e.getMessage());
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
   * Sets the geometry_column layout in this sqlite database: either FDO or
   * OGC or no layout. Also tries to build the geo col type if geometry_columns
   * table contains such info TODO: generic mechanism to get geo col type for
   * Spatialite
   */
  private void setGeoColLayout() {
    DatabaseMetaData dbMd = null;
    try {
      dbMd = this.conn.getConnection().getMetaData();
      ResultSet rs = dbMd.getTables(null, null, SpatialiteDSMetadata.GC_COLUMN_NAME, null);
      if (rs.next()) {
        // tableName is third column in this metadata resultSet
        String col = rs.getString(3);
        boolean isGC = SpatialiteDSMetadata.GC_COLUMN_NAME.equalsIgnoreCase(col);

        // gc layout
        if (isGC) {
          rs = dbMd.getColumns(null, null, SpatialiteDSMetadata.GC_COLUMN_NAME, null);
          int i = 0;
          // geometry_columns table may have 2 layouts according to ogr2ogr
          // options used to create the table:
          // 1°) the "FDO provider for spatialite (https://trac.osgeo.org/fdo/wiki/FDORfc16)", as used in "regular sqlite database" (cf.ogr spatialite format doc):
          //                f_table_name	        TEXT	
          //                f_geometry_column	TEXT	
          //                geometry_type	        INTEGER	
          //                coord_dimension	INTEGER	
          //                srid	                INTEGER	
          //                geometry_format	TEXT
          // 2°) the "OGC" flavour, as understood by qgis for instance, as used in spatialite-enabled sqlite database:
          //                f_table_name          VARCHAR
          //                f_geometry_column     VARCHAR
          //                type                  VARCHAR
          //                coord_dimension       INTEGER 
          //                srid                  INTEGER
          //                spatial_index_enabled INTEGER 
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

    } catch (Exception e) {
      e.printStackTrace();
      //TODO: logging
      System.out.println("error getting geometry_column layout: " + e.getMessage());
    }
  }

  /**
   * builds the map of geometric columns database type: WKB, WKT, SPATIALITE to
   * be able to build custom queries for extent and geo type retrieval.
   * The geometry_format column of the metadata will be queries to find geometry type
   * (column only detected in the FDO_LAYOUT format).
   * For other layout, will default to SPATIALITE type
   */
  private void getGeoColumnType() {
    // Default query gets a hard-coded value for spatialite type
    String query = "select f_table_name, f_geometry_column, \"SPATIALITE\" from geometry_columns";
    if (this.getGeometryColumnsLayout() == GeometryColumnsLayout.FDO_LAYOUT) {
      // MD table contains a geometry_format column: query it
      query = "select f_table_name, f_geometry_column, geometry_format from geometry_columns";
    } 
    try {
      JDBCUtil.execute(
          conn.getConnection(),
          query,
          new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                // force lowercase as JDBC metadata and OGC spatialite metadata can return
                // different cases for the same geometric column
                String table = resultSet.getString(1).toLowerCase();
                String col = resultSet.getString(2).toLowerCase();
                GeometricColumnType gcType = GeometricColumnType.valueOf(resultSet.getString(3));
                geoColTypesdMap.put(table + "." + col, gcType);
              }
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
