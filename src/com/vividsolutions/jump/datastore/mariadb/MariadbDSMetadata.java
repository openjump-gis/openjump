package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.GeometryColumn;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;

public class MariadbDSMetadata extends SpatialDatabasesDSMetadata {

  public static String GC_COLUMN_NAME = "geometry_columns";

  /**
   * The second query to get geometric columns
   */
  private String geoColumnsQuery2;

  /**
   * The second query to get SRID
   */
  private String sridQuery2;

  /**
   * The geometry_columns table layout for this connection.
   * TODO: generalize gc layout mechanism for all SpatialDatabases
   */
  private GeometryColumnsLayout geometryColumnsLayout;

  public MariadbDSMetadata(DataStoreConnection con) {
    conn = con;
    // TODO: defaults to database name ?
    setGeoColLayout();
    
    defaultSchemaName = "";
    
    // query according to detected layout:
    // no schema in MySQL
    datasetNameQuery = "SELECT DISTINCT '' as f_table_schema, f_table_name FROM geometry_columns";
    if (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) {
      datasetNameQuery = "select distinct t.TABLE_SCHEMA, t.TABLE_NAME \n"
          + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
          + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
          + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
          + "and c.COLUMN_TYPE = 'geometry'";
    }
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
                "         SELECT st_ExteriorRing(st_envelope(%s)) AS geom\n" +
                "         FROM %s.%s\n" +
                "       ) AS t\n" +
                ") as t2";
    
    // NO st_extent function defined yet => same query is defined.
    spatialExtentQuery2 = spatialExtentQuery1;
    
    // query according to detected layout:
    geoColumnsQuery = "SELECT f_geometry_column, coord_dimension, srid, type FROM geometry_columns where f_table_name = '%s'";
    if (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) {
      geoColumnsQuery = "select c.COLUMN_NAME, 2, 0, 'geometry' \n"
        + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
        + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
        + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
        + "and t.TABLE_SCHEMA = '%s' and t.TABLE_NAME = '%s'\n"
        + "and c.COLUMN_TYPE = 'geometry'";
    }

    // query according to detected layout:
    sridQuery = "SELECT srid FROM geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    if (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) {
      // quote identifiers
      sridQuery2 = "select case when min(st_srid(%s)) <> max(st_srid(%s)) then 0 else min(st_srid(%s)) end as srid\n"
        + "from `%s`.`%s`";
    }

    coordDimQuery = "SELECT coord_dimension FROM geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    if (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) {
      // quote identifiers
      coordDimQuery = "select 2 as coord_dimension";
    }

    // query according to detected layout:
    datasetInfoQuery = "SELECT '' as f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type FROM geometry_columns";
    if (geometryColumnsLayout == GeometryColumnsLayout.NO_LAYOUT) {
      datasetInfoQuery = "select t.TABLE_SCHEMA, t.TABLE_NAME, c.COLUMN_NAME, 2, 0, 'geometry' \n"
              + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
              + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
              + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
              + "and c.COLUMN_TYPE = 'geometry'";
    }


  }

  @Override
  public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
    return String.format(this.spatialExtentQuery1, attributeName, schema, table);
  }

  @Override
  public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
    return String.format(this.spatialExtentQuery2, attributeName, schema, table);
  }

  @Override
  public String getGeoColumnsQuery(String datasetName) {
    // escape single quotes in identifier
    return String.format(this.geoColumnsQuery,
            SQLUtil.escapeSingleQuote(getTableName(datasetName)));
  }

  public String getGeoColumnsQuery2(String datasetName) {
    return String.format(this.geoColumnsQuery2, getSchemaName(datasetName), getTableName(datasetName));
  }

  @Override
  public String getSridQuery(String schemaName, String tableName, String colName) {
    // escape single quotes in identifier
    // TODO: geom ?
    return String.format(this.sridQuery,
            SQLUtil.escapeSingleQuote(tableName), colName);
  }

  public String getSridQuery2(String schemaName, String tableName, String colName) {
    return String.format(this.sridQuery2, colName, colName, colName,
            SQLUtil.escapeSingleQuote(schemaName),
            SQLUtil.escapeSingleQuote(tableName));
  }

  @Override
  public List<GeometryColumn> getGeometryAttributes(String datasetName) {
    String sql = this.getGeoColumnsQuery(datasetName);
    return getGeometryAttributes(sql, datasetName);
  }

  private void setGeoColLayout() {
    try {
      DatabaseMetaData dbMd = this.conn.getJdbcConnection().getMetaData();

      // GeoPackage test:
      ResultSet rs = dbMd.getTables(null, null, MariadbDSMetadata.GC_COLUMN_NAME, null);
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
