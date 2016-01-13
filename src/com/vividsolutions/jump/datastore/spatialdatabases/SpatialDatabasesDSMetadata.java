package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.PrimaryKeyColumn;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base class for Spatial Databases DataStore Metadata: DB accessed through
 * JDBC driver, implementing if possible OGC SFSQL. Uses postgis default values
 * => already implements mechanism for postgis databases
 *
 * Ex: Postgis, Oracle Spatial, SpatiaLite, SQL Server.<br:>
 * For each spatial db support, a child class should be added based on this
 * class implementing methods as needed. PostGIS was the Spatial DB used for
 * first impl. TODO: sql injection
 *
 * @author Nicolas Ribot
 */
public class SpatialDatabasesDSMetadata implements DataStoreMetadata {

  /**
   * OGC WKB reader if needed: TODO: keep only in needed classes ?
   */
  protected final WKBReader reader = new WKBReader();
  /**
   * OGC WKB reader if needed: TODO: keep only in needed classes ?
   */
  protected final WKTReader txtReader = new WKTReader();
  /**
   * The dataStoreConnection to get MD from
   */
  protected DataStoreConnection conn;
  /**
   * The map of SRIDs found for these MD
   */
  protected Map sridMap = new HashMap();
  /**
   * query to get list of spatial tables from the connection. Must return
   * following columns: distinct table_schema, table_name (if several geo
   * columns exist, only one entry should be returned)
   *
   * @param conn
   */
  protected String datasetNameQuery = null;
  /**
   * the name of the default schema
   */
  protected String defaultSchemaName = null;
  /**
   * The name of this SpatialDatabase
   */
  protected String spatialDbName = null;
  /**
   * The SQL query to get spatial extent. Must return following columns: a
   * geometric column representing the extent
   */
  protected String spatialExtentQuery1 = null;
  /**
   * The alternate SQL query to get spatial extent (for instance for postgis)
   */
  protected String spatialExtentQuery2 = null;
  /**
   * The SQL query to get list of geo columns. Must return column name (String),
   * srid(int) and type (string) (if spatial database does not store type in
   * metadata, can force the base DB geo type in a string: ex: select ...
   * 'SDO_GEOMETRY' as type from all_sdo_geom_metadata
   */
  protected String geoColumnsQuery = null;
  /**
   * The SQL query to get a SRID for a given schema name, table name and geo
   * column
   *
   */
  protected String sridQuery = null;

  public SpatialDatabasesDSMetadata() {
  }

  public SpatialDatabasesDSMetadata(DataStoreConnection conn) {
    this.conn = conn;
    // TODO: use bind parameters to avoid SQL injection
    this.datasetNameQuery = "";
    this.defaultSchemaName = "";
    this.spatialDbName = "";
    // TODO
    this.spatialExtentQuery1 = "";
    // TODO
    this.spatialExtentQuery2 = "";
    // TODO
    this.geoColumnsQuery = "";
    // TODO
    this.sridQuery = "";
  }

  public String getDatasetNameQuery() {
    return this.datasetNameQuery;
  }

  public String getDefaultSchemaName() {
    return this.defaultSchemaName;
  }

  public String getSpatialDbName() {
    return this.spatialDbName;
  }

  public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
    return null;
  }

  public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
    return null;
  }

  public String getGeoColumnsQuery(String datasetName) {
    // TODO
    return String.format(this.geoColumnsQuery, datasetName);
  }

  public String getSridQuery(String schemaName, String tableName, String colName) {
    // TODO
    return String.format(this.sridQuery, schemaName, tableName, colName);
  }

  /**
   * Returns the schema name based on the given tableName: string before . if
   * exists, else returns schemaName
   *
   * @param schemaName
   * @return
   */
  protected String getSchemaName(String schemaName) {
    int dotPos = schemaName.indexOf(".");
    String schema = this.defaultSchemaName;
    if (dotPos != -1) {
      schema = schemaName.substring(0, dotPos);
    }
    return schema;
  }

  /**
   * Returns the table name based on the given tableName: string after "." if
   * exists, else returns tableName
   *
   * @param tableName
   * @return
   */
  protected String getTableName(String tableName) {
    int dotPos = tableName.indexOf(".");
    String ret = tableName;
    if (dotPos != -1) {
      ret = tableName.substring(dotPos + 1);
    }
    return ret;
  }

  /**
   * Returns true if the given Exception concerns a missing geometric metadata
   * table
   *
   * @return
   */
  protected boolean missingGeoException(Exception e) {
    return (e instanceof SQLException && e.getMessage().contains("geometry_columns"));
  }

  public String[] getDatasetNames() {
    final List datasetNames = new ArrayList();
    // Spatial tables only.
    try {
      JDBCUtil.execute(
          conn.getJdbcConnection(),
          this.getDatasetNameQuery(),
          new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                String schema = resultSet.getString(1);
                String table = resultSet.getString(2);
                if (!schema.equalsIgnoreCase(SpatialDatabasesDSMetadata.this.getDefaultSchemaName())) {
                  table = schema + "." + table;
                }
                datasetNames.add(table);
              }
            }
          });
    } catch (Exception e) {
      // Nico Ribot: TODO: returns a custom Ex ?
      if (this.missingGeoException(e)) {
        // TODO: logger + I18N
        JUMPWorkbench.getInstance().getFrame().log("not a " + this.getSpatialDbName() + "db or bad search_path", this.getClass());
      } else {
        e.printStackTrace();
      }
    }
    return (String[]) datasetNames.toArray(new String[datasetNames.size()]);
  }

  public synchronized Envelope getExtents(String datasetName, String attributeName) {

    final Envelope[] e = new Envelope[]{null};

    String schema;
    String table;
    if (datasetName.indexOf('.') != -1) {
      String[] parts = datasetName.split("\\.", 2);
      schema = parts[0];
      table = parts[1];
    } else {
      schema = this.defaultSchemaName;
      table = datasetName;
    }
    // There are two methods to compute the extent,
    // from DB metadata or from layer directly
    String sql1 = this.getSpatialExtentQuery1(schema, table, attributeName);
    String sql2 = this.getSpatialExtentQuery2(schema, table, attributeName);

    final ResultSetBlock resultSetBlock = new ResultSetBlock() {
      public void yield(ResultSet resultSet) throws Exception {
        if (resultSet.next()) {
          byte[] bytes = null;
          Geometry geom = null;
          try {
            bytes = (byte[]) resultSet.getObject(1);
            if (bytes != null) {
              geom = reader.read(bytes);
            }
          } catch (Exception e) {
            geom = txtReader.read(resultSet.getString(1));
          }
          if (geom != null) {
            e[0] = geom.getEnvelopeInternal();
          }
        }
      }
    };
    try {
      JDBCUtil.execute(conn.getJdbcConnection(), (sql1), resultSetBlock);
      if (e[0] == null || e[0].isNull()) {
        JDBCUtil.execute(conn.getJdbcConnection(), (sql2), resultSetBlock);
      }
    } catch (Exception ex1) {
      if (sql2 != null) {
        // some drivers do not support a second SQL query for extent:
        /// sqlite w/o spatialite for instance
        JDBCUtil.execute(conn.getJdbcConnection(), sql2, resultSetBlock);
      }
    }
    //System.out.println("getting extent for: " + datasetName + "." + attributeName + ": " + e[0].toString()  + " in th: " + Thread.currentThread().getName());
    return e[0];
  }

  /**
   * Retrieves list of geometric tables from a custom DB Query: Should use OGC
   * metadata geoemtry_columns or equivalent mechanism according to target DB.
   *
   * @param datasetName
   * @return
   */
  public List<GeometryColumn> getGeometryAttributes(String datasetName) {
    String sql = getGeoColumnsQuery(datasetName);
    return getGeometryAttributes(sql, datasetName);
  }

  protected List<GeometryColumn> getGeometryAttributes(String sql, String datasetName) {
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
                      resultSet.getString(3)));
            }
          }
        });
    return geometryAttributes;
  }

  /**
   * Returns PRIMARY KEY columns of dataset names. Uses JDBC database Metadata
   * to get this information, instead of custom SQL queries
   *
   * @param datasetName name of the table (optionally prefixed by the schema
   * name)
   * @return the list of columns involved in the Primary Key (generally, a
   * single column)
   */
  public List<PrimaryKeyColumn> getPrimaryKeyColumns(String datasetName) {
    final List<PrimaryKeyColumn> identifierColumns = new ArrayList<PrimaryKeyColumn>();
    ResultSet rs = null;

    //System.out.println("getting PK for dataset: " + datasetName);
    try {
      DatabaseMetaData dbMd = this.conn.getJdbcConnection().getMetaData();
      rs = dbMd.getPrimaryKeys(null, getSchemaName(datasetName), getTableName(datasetName));
      while (rs.next()) {
        String colName = rs.getString(4);
        // column type: 
        ResultSet rs2 = dbMd.getColumns(null, getSchemaName(datasetName), getTableName(datasetName), colName);
        // only one res expected
        rs2.next();
        String colType = rs2.getString(6);
        identifierColumns.add(new PrimaryKeyColumn(colName, colType));
        rs2.close();
      }
    } catch (SQLException sqle) {

    } finally {
      try {
        rs.close();
      } catch (SQLException e) {
      }
    }
    return identifierColumns;
  }

  /**
   * gets the list of columns for the given dataset. TODO: factorize MD
   * retrieval in an Util class ?
   *
   * @param datasetName
   * @return an array of column names
   */
  public synchronized String[] getColumnNames(String datasetName) {
    final List<String> cols = new ArrayList<String>();
    ResultSet rs = null;

    try {
      //System.out.println("getting cols for dataset: " + getSchemaName(datasetName) + " " + getTableName(datasetName) + " from: " + this.hashCode());
      DatabaseMetaData dbMd = this.conn.getJdbcConnection().getMetaData();
      rs = dbMd.getColumns(null, getSchemaName(datasetName), getTableName(datasetName), null);
      while (rs.next()) {
        // TODO: escape quotes in column names ?
        cols.add(rs.getString(4));
      }
    } catch (SQLException sqle) {

    } finally {
      try {
        rs.close();
      } catch (SQLException e) {
      }
    }
    return cols.toArray(new String[cols.size()]);
  }

  /**
   * Returns whether column is used by a spatial index (Gist) or not.
   * @param dsName
   * @param column
   * @return 
   * @throws java.sql.SQLException
   */
  public boolean isIndexed(final String dsName, final String column) throws SQLException {
    ResultSet rs = null;
    boolean ret = false;
    String schemaName = this.getSchemaName(dsName);
    String tableName = this.getTableName(dsName);
    try {
      if (schemaName.equals(dsName)) {
        // no schema defined in given dataset name: use default one
        schemaName = this.getDefaultSchemaName();
      }
      DatabaseMetaData dbMd = this.conn.getJdbcConnection().getMetaData();
      rs = dbMd.getIndexInfo(null, this.getSchemaName(dsName), this.getTableName(dsName), false, true);
      while (rs.next()) {
        if (column.equals(rs.getString(9)) && rs.getString(6) != null && rs.getString(9) != null) {
          ret = true;
          break;
        }
      }
    } catch (SQLException sqle) {
      //TODO: log ?
      sqle.printStackTrace();
    } finally {
      try {
        rs.close();
      } catch (SQLException e) {
      }
    }
    return ret;
  }

  private static class ColumnNameBlock implements ResultSetBlock {

    List colList = new ArrayList();
    String[] colName;

    public void yield(ResultSet resultSet) throws SQLException {
      while (resultSet.next()) {
        colList.add(resultSet.getString(1));
      }
      colName = (String[]) colList.toArray(new String[colList.size()]);
    }
  }

  public SpatialReferenceSystemID getSRID(String datasetName, String colName)
      throws SQLException {
    String key = datasetName + "#" + colName;
    if (!sridMap.containsKey(key)) {
      // not in cache, so query it
      String srid = querySRID(datasetName, colName);
      sridMap.put(key, new SpatialReferenceSystemID(srid));
    }
    return (SpatialReferenceSystemID) sridMap.get(key);
  }

  protected String querySRID(String datasetName, String colName) {
    final StringBuffer srid = new StringBuffer();
    String sql = this.getSridQuery(this.getSchemaName(datasetName), this.getTableName(datasetName), colName);
    JDBCUtil.execute(conn.getJdbcConnection(), sql, new ResultSetBlock() {
      public void yield(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
          // Nicolas Ribot: test if a null is returned
          String s = resultSet.getString(1);
          srid.append(s == null ? "0" : s);
        }
      }
    });

    return srid.toString();
  }
  
  @Override
  public DataStoreConnection getDataStoreConnection() {
    return this.conn;
  }
}
