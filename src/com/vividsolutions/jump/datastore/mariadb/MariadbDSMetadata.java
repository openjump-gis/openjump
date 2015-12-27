package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MariadbDSMetadata extends SpatialDatabasesDSMetadata {

  /**
   * The second query to get geometric columns
   */
  private String geoColumnsQuery2;
  /**
   * The second query to get SRID
   */
  private String sridQuery2;

  public MariadbDSMetadata(DataStoreConnection con) {
    conn = con;
    // TODO: defaults to database name ?
    defaultSchemaName = "";
    // TODO: use bind parameters to avoid SQL injection
    datasetNameQuery = "select distinct t.TABLE_SCHEMA, t.TABLE_NAME \n"
        + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
        + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
        + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
        + "and c.COLUMN_TYPE = 'geometry';";
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
    
    // NO metadata => same query is defined.
    spatialExtentQuery2 = spatialExtentQuery1;
    geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_name = '%s'";

    // Double mechanism is used for Maria/MySQL: ogr, for instance, creates 
    // and populates a geometry_columns table with geo MD
    // Original mechanism was based on pure MySQL MD to guess geo column
    // Keep both mechanisms to handle all cases.
    geoColumnsQuery2 = "select c.COLUMN_NAME, 0, 'geometry' \n"
        + "from information_schema.TABLES t join information_schema.COLUMNS C \n"
        + "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n"
        + "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n"
        + "and t.TABLE_SCHEMA = '%s' and t.TABLE_NAME = '%s'\n"
        + "and c.COLUMN_TYPE = 'geometry'";
    // TODO: test for big datasets...
    sridQuery = "SELECT srid FROM geometry_columns where f_table_name = '%s' and f_geometry_column = '%s'";
    sridQuery2 = "select case when min(st_srid(%s)) <> max(st_srid(%s)) then 0 else min(st_srid(%s)) end as srid\n"
        + "from %s.%s";
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
    return String.format(this.geoColumnsQuery, getTableName(datasetName));
  }

  public String getGeoColumnsQuery2(String datasetName) {
    return String.format(this.geoColumnsQuery2, getSchemaName(datasetName), getTableName(datasetName));
  }

  @Override
  public String getSridQuery(String schemaName, String tableName, String colName) {
    return String.format(this.sridQuery, tableName, colName);
  }

  public String getSridQuery2(String schemaName, String tableName, String colName) {
    return String.format(this.sridQuery2, colName, colName, colName, schemaName, tableName);
  }

  @Override
  public List<GeometryColumn> getGeometryAttributes(String datasetName) {
    String sql = this.getGeoColumnsQuery(datasetName);
    return getGeometryAttributes(sql, datasetName);
  }

  /**
   * Retrieves geometric attributes for given dataset using 2 methods: classical
   * method from OGC metadata (geometryColumns) and DB metadata method, querying
   * system catalog.
   *
   * @param sql
   * @param datasetName
   * @return
   */
  @Override
  protected List<GeometryColumn> getGeometryAttributes(String sql, String datasetName) {
    final List<GeometryColumn> geometryAttributes2 = new ArrayList<GeometryColumn>();
    try {
      final List<GeometryColumn> geometryAttributes = super.getGeometryAttributes(sql, datasetName);
      return geometryAttributes;
    } catch (Exception e) {
      // second method
      String sql2 = getGeoColumnsQuery2(datasetName);
      JDBCUtil.execute(
          conn.getJdbcConnection(), sql2,
          new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                geometryAttributes2.add(new GeometryColumn(
                        resultSet.getString(1),
                        resultSet.getInt(2),
                        resultSet.getString(3)));
              }
            }
          });
    }
    return geometryAttributes2;
  }
  
  /**
   * Gets the SRID of the dataset using a second method if OGC metadata does not work.
   * @param datasetName
   * @param colName
   * @return 
   */
  @Override
  protected String querySRID(String datasetName, String colName) {
    final StringBuffer srid = new StringBuffer();
    try {
      srid.append(super.querySRID(datasetName, colName));
    } catch (Exception e) {
      // TODO: log ex message ?
      String sql = this.getSridQuery2(this.getSchemaName(datasetName), this.getTableName(datasetName), colName);
      JDBCUtil.execute(conn.getJdbcConnection(), sql, new ResultSetBlock() {
        public void yield(ResultSet resultSet) throws SQLException {
          if (resultSet.next()) {
            srid.append(resultSet.getString(1));
          }
        }
      });
    }

    return srid.toString();
  }

}
