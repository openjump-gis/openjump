package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.jdbc.BoundQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDataStoreMetadata;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class MariadbDataStoreMetadata extends SpatialDataStoreMetadata {

  public static String GC_COLUMN_NAME = "geometry_columns";
  //To handle several md layouts
  private String datasetNameQuery2 = null;
  private String geoColumnsQuery2 = null;
  private String sridQuery2 = null;
  /**
   * The geometry_columns table layout for this connection.
   * TODO: generalize gc layout mechanism for all SpatialDatabases
   */
  private GeometryColumnsLayout geometryColumnsLayout;

  public MariadbDataStoreMetadata(DataStoreConnection con) {
    conn = con;
    // TODO: defaults to database name ?
    setGeoColLayout();
    
    defaultSchemaName = "";
    
    // query according to detected layout:
    // no schema in MySQL
    datasetNameQuery = "SELECT DISTINCT '' as f_table_schema, f_table_name FROM geometry_columns";
    datasetNameQuery2 = "select distinct t.TABLE_SCHEMA, t.TABLE_NAME \n"
        + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
        + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
        + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
        + "and c.COLUMN_TYPE = 'geometry'";
    spatialDbName = "MariaDB/MySQL";
    // No safe way found to get layer's extent from database.
    // text aggregation trick (select st_asBinary(st_envelope(Geomfromtext(concat(concat(\"geometrycollection(\",group_concat(astext(%s))),\")\")))) from %s.%s)
    // does not work well with JDBC: group_concat_max_len param should maybe be set in the driver prefs ?
    // Use extent of first record only as an hint to layer location
    spatialExtentQuery1 = "select st_asbinary(st_geomfromtext(concat('POLYGON((', minx, ' ', miny, ', ',\n" +
                "              maxx, ' ', miny, ', ',\n" +
                "              maxx, ' ', maxy, ', ',\n" +
                "              minx, ' ', maxy, ', ',\n" +
                "              minx, ' ', miny, '))'))) as geom\n" +
                "from (\n" +
                "  SELECT\n" +
                "    min(st_x(st_pointN(geom, 1))) AS minx,\n" +
                "    min(st_y(st_pointN(geom, 1))) AS miny,\n" +
                "    max(st_x(st_pointN(geom, 3))) AS maxx,\n" +
                "    max(st_y(st_pointN(geom, 3))) AS maxy\n" +
                "  FROM (\n" +
                "         SELECT st_ExteriorRing(st_envelope(`%s`)) AS geom\n" +
                "         FROM %s\n" +
                "       ) AS t\n" +
                ") as t2";
    
    // NO st_extent function defined yet => same query is defined.
    spatialExtentQuery2 = spatialExtentQuery1;
    
    // query according to detected layout:
    // bind parameters to usable in second case: todo/.
    geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns "
        + "where (f_table_schema is null or f_table_schema = ?) and f_table_name = ?";
    // TODO: not the same number of param to replace...
    geoColumnsQuery2 = "select c.COLUMN_NAME, 0, 'geometry' \n"
      + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
      + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
      + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
      + "and (t.TABLE_SCHEMA is null or t.TABLE_SCHEMA = ?) and t.TABLE_NAME = ?\n"
      + "and c.COLUMN_TYPE = 'geometry'";

    // TODO: test for big datasets...
    // queries according to detected layout:
    sridQuery = "SELECT srid FROM geometry_columns "
        + "where (f_table_schema is null or f_table_schema = ?) and f_table_name = ? and f_geometry_column = ?";
    sridQuery2 = "select case when min(st_srid(`%s`)) <> max(st_srid(`%s`)) then 0 else min(st_srid(`%s`)) end as srid\n"
        + "from %s";
  }

  /**
   * Manages MD layouts
   * @return 
   */
  @Override
  public BoundQuery getDatasetNameQuery() {
    String s = (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) ?
        datasetNameQuery2 : datasetNameQuery;
    
    return new BoundQuery(s);
  }

  /**
   * Manages schema name here (can be empty).
   * @param schema
   * @param table
   * @param attributeName
   * @return 
   */
  @Override
  public BoundQuery getSpatialExtentQuery1(String schema, String table, String attributeName) {
    String s = (schema == null || schema.isEmpty()) ? "`" + table + "`" 
        : "`" + schema + "`.`" + table + "`";
    return new BoundQuery(String.format(this.spatialExtentQuery1, attributeName, s));
  }

  /**
   * No second extent query yet for this DB.
   * Default to query1
   * @param schema
   * @param table
   * @param attributeName
   * @return 
   */
  @Override
  public BoundQuery getSpatialExtentQuery2(String schema, String table, String attributeName) {
    return getSpatialExtentQuery1(schema, table, attributeName);
  }
  
  /**
   * Manages the query according to detected layout
   * @param datasetName
   * @return 
   */
  @Override
  public BoundQuery getGeoColumnsQuery(String datasetName) {
    String s = (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) ?
        geoColumnsQuery2 : geoColumnsQuery;
    return new BoundQuery(s).addParameter(getSchemaName(datasetName))
          .addParameter(getSchemaName(datasetName));
  }
/**
   * Manages MD layout and schema name for MySQL/MariaDB.
   * query2: replace geom col and qualified name
   * query: bind 3 params schema, table, col
   * @param schemaName
   * @param tableName
   * @param colName
   * @return 
   */
  @Override
  public BoundQuery getSridQuery(String schemaName, String tableName, String colName) {
    if (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) {
      String s = (schemaName == null || schemaName.isEmpty()) ? "`" + tableName + "`" 
          : "`" + schemaName + "`.`" + tableName + "`";
      return new BoundQuery(String.format(sridQuery2, colName, colName, colName, s));
    } else {
      return new BoundQuery(sridQuery).addParameter(schemaName)
          .addParameter(tableName).addParameter(colName);
    }
  }

  private void setGeoColLayout() {
    try {
      DatabaseMetaData dbMd = this.conn.getJdbcConnection().getMetaData();

      // GeoPackage test:
      ResultSet rs = dbMd.getTables(null, null, MariadbDataStoreMetadata.GC_COLUMN_NAME, null);
      if (rs.next()) {
        // no need to look at table layout: table name found is enough to say its OGC layout
        geometryColumnsLayout = GeometryColumnsLayout.OGC_LAYOUT;
        rs.close();
      } else {
        geometryColumnsLayout = GeometryColumnsLayout.NO_LAYOUT;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
