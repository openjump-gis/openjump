package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Spatialite connexion metadata. Some extra processing occurs here: telling if
 * spatialite extension is loaded and what type of geo metatada are present:
 *
 * @author nicolas Ribot
 */
public class SpatialiteDSMetadata extends SpatialDatabasesDSMetadata {

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
  
  /**
   * the query to know if a geometry is indexed or not.
   */
  private String spatialIndexQuery = null;
  
  /**
   * The map of geometryColumn list for each dataset: stores it locally as it is accessed
   * several times for spatialite: at UI init, and when loading a layer
   */
  private Map<String, List<GeometryColumn>> geometryColumnListMap = null;
  
  
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
    this.geometryColumnListMap = new HashMap<String, List<GeometryColumn>>();

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
    if (this.getGeometryColumnsLayout() == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      // MD table contains a geometry_format column: query it
      datasetNameQuery = "SELECT DISTINCT '' as f_table_schema, table_name as f_table_name FROM gpkg_geometry_columns";
    } 
    
    defaultSchemaName = "";
    spatialDbName = isSpatialiteLoaded() ? "Spatialite" : "SQLite";
    spatialExtentQuery1 = "SELECT %s from \"%s\"";
    // no second query for spatialite
    spatialExtentQuery2 = null;

    if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      sridQuery = "SELECT srs_id FROM gpkg_geometry_columns where table_name = '%s' and column_name = '%s'";
    } else {
      sridQuery = "SELECT srid FROM geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    }
    // geo column query needs to be built occording to geometryColumnsLayout
    if (this.geometryColumnsLayout == GeometryColumnsLayout.FDO_LAYOUT
        || this.geometryColumnsLayout == GeometryColumnsLayout.OGC_OGR_LAYOUT) {
      geoColumnsQuery = "SELECT f_geometry_column, coord_dimension, srid,\n"
          + "  case\n"
          + "    when geometry_type = 1 then 'POINT'\n"
          + "    when geometry_type = 2 then 'LINESTRING'\n"
          + "    when geometry_type = 3 then 'POLYGON'\n"
          + "    when geometry_type = 4 then 'MULTIPOINT'\n"
          + "    when geometry_type = 5 then 'MULTILINESTRING'\n"
          + "    when geometry_type = 6 then 'MULTIPOLYGON'\n"
          + "    when geometry_type = 7 then 'GEOMETRY COLLECTION'\n"
          + "    else geometry_type end as geometry_type\n"
          + "FROM geometry_columns where f_table_name = '%s'";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_SPATIALITE_LAYOUT) {
      geoColumnsQuery = "SELECT f_geometry_column, coord_dimension, srid, type FROM geometry_columns where f_table_name = '%s'";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      geoColumnsQuery = "SELECT column_name, " +
              "case when z+m = 0 then 2 when z = 1 and m = 1 then 4 else 3 end as coord_dimension, " +
              "srs_id, geometry_type_name FROM gpkg_geometry_columns where table_name = '%s'";
    } else {
      geoColumnsQuery = "SELECT '' ";
    }

    if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      coordDimQuery = "SELECT case when z+m = 0 then 2 when z = 1 and m = 1 then 4 else 3 end as coord_dimension " +
              "FROM gpkg_geometry_columns where table_name = '%s' and column_name = '%s'";
    } else {
      coordDimQuery = "SELECT coord_dimension FROM geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    }
    
    // spatial index query
    
    
    if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      spatialIndexQuery = "select exists(SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'rtree_%s_%s')";
    } else if (this.spatialiteLoaded) {
      // for all cases where spatialite is detected: normal function should be used
      spatialIndexQuery = "SELECT CASE WHEN CheckSpatialIndex('%s', '%s') = 1 then 1 else 0 end as isindexed";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_SPATIALITE_LAYOUT) {
      spatialIndexQuery = "select spatial_index_enabled from geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    } else {
      // TODO: 
      spatialIndexQuery = "";
    }

    // geo column query needs to be built occording to geometryColumnsLayout
    if (this.geometryColumnsLayout == GeometryColumnsLayout.FDO_LAYOUT
            || this.geometryColumnsLayout == GeometryColumnsLayout.OGC_OGR_LAYOUT) {
      datasetInfoQuery = "SELECT '' as f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid,\n"
              + "  case\n"
              + "    when geometry_type = 1 then 'POINT'\n"
              + "    when geometry_type = 2 then 'LINESTRING'\n"
              + "    when geometry_type = 3 then 'POLYGON'\n"
              + "    when geometry_type = 4 then 'MULTIPOINT'\n"
              + "    when geometry_type = 5 then 'MULTILINESTRING'\n"
              + "    when geometry_type = 6 then 'MULTIPOLYGON'\n"
              + "    when geometry_type = 7 then 'GEOMETRY COLLECTION'\n"
              + "    else geometry_type end as geometry_type\n"
              + "FROM geometry_columns";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_SPATIALITE_LAYOUT) {
      datasetInfoQuery = "SELECT '' as f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type FROM geometry_columns";
    } else if (this.geometryColumnsLayout == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
      datasetInfoQuery = "SELECT '' as table_schema, table_name, column_name, " +
              "case when z+m = 0 then 2 when z = 1 and m = 1 then 4 else 3 end as coord_dimension, " +
              "srs_id, geometry_type_name FROM gpkg_geometry_columns";
    } else {
      datasetInfoQuery = "SELECT '' ";
    }

  }
  
  /**
   * Overriden to deal with indexed geo columns, as queries to get features are different
   * if spatial index is detected on the column.
   * Buids a GeometryColumn object with 5 params ctor.
   * @param sql
   * @param datasetName
   * @return 
   */
  @Override
  protected List<GeometryColumn> getGeometryAttributes(String sql, String datasetName) {
    if (this.geometryColumnListMap.get(datasetName) == null) {
      final List<GeometryColumn> geometryAttributes = new ArrayList<GeometryColumn>();
      //System.out.println("getting geom Attribute for dataset: " + datasetName + " with query: " + sql);

      JDBCUtil.execute(
          conn.getJdbcConnection(), sql,
          new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                // TODO: escape single quotes in geo column name ?
                geometryAttributes.add(new GeometryColumn(
                        resultSet.getString(1),
                        resultSet.getInt(2),
                        resultSet.getInt(3),
                        resultSet.getString(4)));
              }
            }
          });

      // sets geo columns index information by querying spatialite function or direct
      // index table
      for (Iterator<GeometryColumn> iterator = geometryAttributes.iterator(); iterator.hasNext();) {
        GeometryColumn gc = iterator.next();
        setIndexInfo(datasetName, gc);

      }
      this.geometryColumnListMap.put(datasetName, geometryAttributes);
      
    }
    
    return this.geometryColumnListMap.get(datasetName);
  }

  @Override
  public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
    // No schema in SQLite, schema param not used
    // must cast the geometric field according to its type, to be able to use spatialite functions.
    String ret = "select 1";

    GeometricColumnType gcType = this.geoColTypesdMap.get(table.toLowerCase() + "." + attributeName.toLowerCase());

    if (gcType == null) {
      return "select 1";
    }
    // TODO: switch case
    if (this.isSpatialiteLoaded()) {
      if (gcType == GeometricColumnType.WKB) {
        // quotes identifier.
        ret = String.format("select st_asBinary(extent(st_geomFromWkb(%s))) from \"%s\"", attributeName, table);
      } else if (gcType == GeometricColumnType.WKT) {
        ret = String.format("select st_asBinary(extent(st_geomFromText(%s))) from \"%s\"", attributeName, table);
      } else if (gcType == GeometricColumnType.SPATIALITE) {
        ret = String.format("select st_asBinary(extent(CastAutomagic(%s))) from \"%s\"", attributeName, table);
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
    return spatialExtentQuery2;
  }

  /**
   * No schema in SQLite
   *
   * @param datasetName
   * @return
   */
  @Override
  public String getGeoColumnsQuery(String datasetName) {
    // No schema in SQLite, escape single quotes in name:
      return String.format(this.geoColumnsQuery, SQLUtil.escapeSingleQuote(getTableName(datasetName)));
  }

  @Override
  public String getSridQuery(String schemaName, String tableName, String colName) {
    // no schema in sqlite
    return String.format(this.sridQuery, 
        SQLUtil.escapeSingleQuote(tableName), 
        SQLUtil.escapeSingleQuote(colName));
  }

  private void checkSpatialiteLoaded() {
    // tries to load spatialite, assuming it is available on the system's path
    Statement stmt = null;
    try {
//      stmt = conn.getJdbcConnection().createStatement();
//      stmt.executeUpdate("SELECT load_extension('mod_spatialite')");
//      // ex is thrown if extension cannot be loaded
//      this.spatialiteLoaded = true;
//      ResultSet rs = stmt.executeQuery("select spatialite_version()");
//      rs.next();
//      this.setSpatialiteVersion(rs.getString(1));
//
//      JUMPWorkbench.getInstance().getFrame().log(
//          "SpatialDatabasesPlugin: Spatialite extension loaded for this connexion, version: "
//          + this.getSpatialiteVersion(), this.getClass());
    } catch (Exception e) {
      JUMPWorkbench.getInstance().getFrame().log(
          "SpatialDatabasesPlugin: CANNOT load Spatialite Extention (mod_spatialite), reason:"
          + e.getMessage(), this.getClass());
    } finally {
      try {
//        stmt.close();
      } catch (Throwable th) {
        // TODO: log
        th.printStackTrace();
      }
    }
  }

  /**
   * Sets the geometry_column layout in this sqlite database: either FDO or OGC
   * or GeoPkg or no layout. Also tries to build the geo col type if
   * geometry_columns table contains such info TODO: generic mechanism to get
   * geo col type for Spatialite.
   * 
   * Geometry_columns metadata table may have 4 layouts:
   * options used to create the table or using a geo package (http://www.geopackage.org/) layout
   * 1?) the "FDO provider for spatialite (https://trac.osgeo.org/fdo/wiki/FDORfc16)", as used in "regular sqlite database" (cf.ogr spatialite format doc):
   *                f_table_name	        TEXT	
   *                f_geometry_column	TEXT	
   *                geometry_type	        INTEGER	
   *                coord_dimension	INTEGER	
   *                srid	                INTEGER	
   *                geometry_format	TEXT
   * 2?) the "OGC Spatialite" flavour, as understood by qgis for instance, as used in spatialite-enabled sqlite database:
   *                f_table_name          VARCHAR
   *                f_geometry_column     VARCHAR
   *                type                  VARCHAR
   *                coord_dimension       INTEGER 
   *                srid                  INTEGER
   *                spatial_index_enabled INTEGER 
   * 3?) the "OGC OGR" layout: 
   *                f_table_name          VARCHAR
   *                f_geometry_column     VARCHAR
   *                geometry_type         VARCHAR
   *                coord_dimension       INTEGER 
   *                srid                  INTEGER
   *                spatial_index_enabled INTEGER 
   * 3?) the "OGC GeoPackage" layout, as specificed by standard:
   *                table_name         TEXT NOT NULL,
   *                column_name        TEXT NOT NULL,
   *                geometry_type_name TEXT NOT NULL,
   *                srs_id             INTEGER NOT NULL,
   *                z                  INTEGER NOT NULL,
   *                m                  INTEGER NOT NULL,
   * 
   */
  private void setGeoColLayout() {
    DatabaseMetaData dbMd = null;
    try {
      dbMd = this.conn.getJdbcConnection().getMetaData();

      // GeoPackage test:
      ResultSet rs = dbMd.getTables(null, null, SpatialiteDSMetadata.GPKG_GC_COLUMN_NAME, null);
      if (rs.next()) {
        // no need to look at table layout: table name found is enough to say its geoPackage layout
        geometryColumnsLayout = GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT;
        rs.close();
      } else {

        // OGC/FDO layout
        rs = dbMd.getTables(null, null, SpatialiteDSMetadata.GC_COLUMN_NAME, null);
        if (rs.next()) {
          // tableName is third column in this metadata resultSet
          String col = rs.getString(3);

          // TODO: clean-up the JDBC metadata use...
          boolean isGC = (SpatialiteDSMetadata.GC_COLUMN_NAME.equalsIgnoreCase(col)
              || SpatialiteDSMetadata.GPKG_GC_COLUMN_NAME.equalsIgnoreCase(col));

          // gc layout
          if (isGC) {
            rs = dbMd.getColumns(null, null, SpatialiteDSMetadata.GC_COLUMN_NAME, null);
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
            }
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
   * geometry_format column of the metadata will be queried to find geometry
   * type (column only detected in the FDO_LAYOUT format). For other layout,
   * will default to SPATIALITE type
   */
  private void getGeoColumnType() {
    try {
      JDBCUtil.execute(
          conn.getJdbcConnection(),
          this.geoColumnTypesQuery,
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
  
  /**
   * set if given gc column is spatially indexed (true/false) according to spatialite
   * DB type
   * @param datasetName the name of the dataset this column belongs to
   * @param gc the geometry column to set
   */
  private void setIndexInfo(String datasetName, final GeometryColumn gc) {
    String q = String.format(Locale.US, spatialIndexQuery, datasetName, gc.getName());
    try {
      JDBCUtil.execute(
          conn.getJdbcConnection(),
          q,
          new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                gc.setIndexed(resultSet.getBoolean(1));
              }
            }
          });
    } catch (Exception e) {
      //TODO...
      e.printStackTrace();
    }
  }
  
  /**
   * Convenience method to get a geometryColumn object from this metadata
   * @param datasetName the name of the dataset
   * @param geoCol the name of the geo column
   * @return 
   */
  public GeometryColumn getGeometryColumn(String datasetName, String geoCol) {
    List<GeometryColumn> l = this.geometryColumnListMap.get(datasetName);
    if (l == null) {
      // 2018-02-28: also look into dataStoreLayers to find geo column, now this container is used to
      // speedup DB tables discovery
      if (this.dataStoreLayers != null) {
        for (DataStoreLayer dsl : this.dataStoreLayers) {
          if (datasetName.equals(dsl.getFullName()) && geoCol.equals(dsl.getGeoCol().getName())) {
            return dsl.getGeoCol();
          }
        }
      }
      return null;
    }
    
    for (Iterator<GeometryColumn> iterator = l.iterator(); iterator.hasNext();) {
      GeometryColumn gc = iterator.next();
      if (gc.getName().equals(geoCol)) {
        return gc;
      }
    }
    return null;
  }

}
