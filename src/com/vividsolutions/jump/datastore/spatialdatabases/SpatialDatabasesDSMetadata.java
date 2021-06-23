package com.vividsolutions.jump.datastore.spatialdatabases;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import com.vividsolutions.jump.datastore.*;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base class for Spatial Databases DataStore Metadata: DB accessed through
 * JDBC driver, implementing if possible OGC SFSQL. Uses postgis default values
 * =&lt; already implements mechanism for postgis databases
 *
 * <p>Ex: Postgis, Oracle Spatial, SpatiaLite, SQL Server.</p>
 * <p>For each spatial db support, a child class should be added based on this
 * class implementing methods as needed. PostGIS was the Spatial DB used for
 * first impl.</p>
 * TODO: sql injection
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
  protected Map<String,SpatialReferenceSystemID> sridMap = new HashMap();

  /**
   * query to get list of spatial tables from the connection. Must return
   * following columns: distinct table_schema, table_name (if several geo
   * columns exist, only one entry should be returned)
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
   * The SQL query to get a SRID for a given schema name, table name and
   * geometry column
   */
  protected String sridQuery = null;

  /**
   * The SQL query to get the coordinate dimension for a given schema name,
   * table name and geometry column
   */
  protected String coordDimQuery = null;

  // Nico Ribot, 2018-08-07: adds a new mechanism to load datasets info with geo columns and all OGC info
  // with one query, instead of launching one query per dataset !
  /**
   * The SQL query to get all dataset OGC information
   */
  protected String datasetInfoQuery = null;

  /**
   * The list of dataStoreLayer for this ds metadata. Built when list of dataset names
   * is requested: avoids to call getGeometryAttributes for each dataset, which takes too much time
   * on big DB. Instead, all dataStoreLayer are built once and filtered out to get dataset names
   */
  protected ArrayList<DataStoreLayer> dataStoreLayers = null;

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

    this.dataStoreLayers = new ArrayList<DataStoreLayer>();
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

  public String getCoordinateDimensionQuery(String schemaName, String tableName, String colName) {
    // TODO
    return String.format(this.coordDimQuery, schemaName, tableName, colName);
  }

  public String getDatasetInfoQuery() {
    return this.datasetInfoQuery;
  }

  public ArrayList<DataStoreLayer> getDataStoreLayers() {
    return this.dataStoreLayers;
  }

  /**
   * Returns the schema name based on the given tableName: string before . if
   * exists, else returns schemaName
   *
   * @param tableName the table name, eventually qualified
   * @return the name of the schema
   */
  protected String getSchemaName(String tableName) {
    int dotPos = tableName.indexOf(".");
    String schema = this.defaultSchemaName;
    if (dotPos != -1) {
      schema = tableName.substring(0, dotPos);
    }
    return schema;
  }

  /**
   * Returns the table name based on the given tableName: string after "." if
   * exists, else returns tableName
   *
   * @param tableName the table name, eventually qualified
   * @return the table part of the eventually qualified table name
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
   * @param e an exception
   * @return true if the exception is about missing geometry column
   */
  protected boolean missingGeoException(Exception e) {
    return (e instanceof SQLException && e.getMessage().contains("geometry_columns"));
  }

//  public String[] getDatasetNames() {
//    final List datasetNames = new ArrayList();
//    // Spatial tables only.
//    try {
//      JDBCUtil.execute(
//          conn.getJdbcConnection(),
//          this.getDatasetNameQuery(),
//          new ResultSetBlock() {
//            public void yield(ResultSet resultSet) throws SQLException {
//              while (resultSet.next()) {
//                String schema = resultSet.getString(1);
//                String table = resultSet.getString(2);
//                if (!schema.equalsIgnoreCase(SpatialDatabasesDSMetadata.this.getDefaultSchemaName())) {
//                  table = schema + "." + table;
//                }
//                datasetNames.add(table);
//              }
//            }
//          });
//    } catch (Exception e) {
//      // Nico Ribot: TODO: returns a custom Ex ?
//      if (this.missingGeoException(e)) {
//        // TODO: logger + I18N
//        JUMPWorkbench.getInstance().getFrame().log("not a " + this.getSpatialDbName()
//                + " db or bad search_path", this.getClass());
//      } else {
//        e.printStackTrace();
//      }
//    }
//    return (String[]) datasetNames.toArray(new String[datasetNames.size()]);
//  }

  /**
   * Nico Ribot: 2018-08-07: new method using a query to get all information for datasets
   * in a structure, to avoid querying too much the server
   * @return dataset (table) names from this spatial Database
   */
  public String[] getDatasetNames() {
    final List datasetNames = new ArrayList();
    this.dataStoreLayers = new ArrayList<DataStoreLayer>();

    // Spatial tables only.
    try {
      JDBCUtil.execute(
          conn.getJdbcConnection(),
          this.getDatasetInfoQuery(),
          new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                String schema = resultSet.getString(1);
                String table = resultSet.getString(2);
                if (!schema.equalsIgnoreCase(SpatialDatabasesDSMetadata.this.getDefaultSchemaName())) {
                  table = schema + "." + table;
                }
                // checks if dataset already exists
                if (! datasetNames.contains(table)) {
                  datasetNames.add(table);
                }
                // datastoreLayers:
                // 2020-02-12: caution here; geometryColumn is built without index info
                // this info is retrieved as necessary by specialized classes
                GeometryColumn geo = new GeometryColumn(
                        resultSet.getString(3),
                        resultSet.getInt(4),
                        resultSet.getInt(5),
                        resultSet.getString(6));
                dataStoreLayers.add(new DataStoreLayer(table, geo));
              }
            }
          });
    } catch (Exception e) {
      // Nico Ribot: TODO: returns a custom Ex ?
      if (this.missingGeoException(e)) {
        // TODO: logger + I18N
        Logger.error("not a " + this.getSpatialDbName()
                + " db or bad search_path", e);
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
   * @param datasetName name of a dataset
   * @return list of GeometryColumn's referenced in this dataset
   */
  public List<GeometryColumn> getGeometryAttributes(String datasetName) {
    String sql = getGeoColumnsQuery(datasetName);
    return getGeometryAttributes(sql, datasetName);
  }

  protected List<GeometryColumn> getGeometryAttributes(String sql, String datasetName) {
    final List<GeometryColumn> geometryAttributes = new ArrayList<>();
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
   * @throws SQLException if the server throws an Exception while trying to get
   *    PrimaryKey columns
   */
  public List<PrimaryKeyColumn> getPrimaryKeyColumns(String datasetName) throws SQLException {
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
      // For wiew, which have no PK but can reference a pk column,
      // give the opportunity to use int, bigint or varchar attribute
      rs = dbMd.getTables(null, getSchemaName(datasetName), getTableName(datasetName), null);
      while (rs.next()) {
        String tableType = rs.getString(4);
        if (tableType.equals("VIEW")) {
          ResultSet rs2 = dbMd.getColumns(null, getSchemaName(datasetName), getTableName(datasetName), null);
          while (rs2.next()) {
            PrimaryKeyColumn pk = new PrimaryKeyColumn(rs2.getString(4), rs2.getString(6));
            if (pk.getType()== Types.VARCHAR || pk.getType() == Types.INTEGER || pk.getType() == Types.BIGINT) {
              identifierColumns.add(pk);
            }
          }
        }
      }
    } catch (SQLException sqle) {
      throw sqle;
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
   * @param datasetName the dataset (table) name
   * @return an array of column names
   */
  public synchronized String[] getColumnNames(String datasetName) {
    final List<String> cols = new ArrayList<>();
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
   * @param dsName a dataset (table) name
   * @param column a column name
   * @return true if the column is indexed
   * @throws java.sql.SQLException if an exception occurs during metadata querying
   */
  public boolean isIndexed(final String dsName, final String column) throws SQLException {
    ResultSet rs = null;
    boolean ret = false;
    String schemaName = this.getSchemaName(dsName);
    String tableName = this.getTableName(dsName);
    try {
      DatabaseMetaData dbMd = this.conn.getJdbcConnection().getMetaData();
      rs = dbMd.getIndexInfo(null, schemaName, tableName, false, true);
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

  public int getCoordinateDimension(String datasetName, String colName) {
    final StringBuffer coordDim = new StringBuffer();
    String sql = this.getCoordinateDimensionQuery(this.getSchemaName(datasetName),
            this.getTableName(datasetName), colName);
    JDBCUtil.execute(conn.getJdbcConnection(), sql, new ResultSetBlock() {
      public void yield(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
          // Nicolas Ribot: test if a null is returned
          // Michael Michaud: choose 2 rather than 0 as the default coordDim in case of failure
          String s = resultSet.getString(1);
          coordDim.append(s == null ? "2" : s);
        }
      }
    });

    return Integer.parseInt(coordDim.toString());
  }
  
  @Override
  public DataStoreConnection getDataStoreConnection() {
    return this.conn;
  }

  /**
   * Returns the CREATE TABLE statement corresponding to this feature schema.
   * The statement includes column names and data types, but neither geometry
   * column nor primary key.
   * @param fSchema client feature schema
   * @param schemaName unquoted schema name or null to use default schema
   * @param tableName unquoted table name
   * @param normalizeColumnNames whether column names must be normalized (lowercased
   *                              and without special characters) or not
   * @return the sql string to create the table
   */
  public String getCreateTableStatement(FeatureSchema fSchema,
                                        String schemaName, String tableName, boolean normalizeColumnNames) {
    return "CREATE TABLE " + SQLUtil.compose(schemaName, tableName) +
            " (" + createColumnList(fSchema, true, false, false, true, normalizeColumnNames) + ");";
  }

  /**
   * Returns a comma-separated list of attributes included in schema.
   * @param schema the FeatureSchema
   * @param includeSQLDataType if true, each attribute name is immediately
   *        followed by its corresponding sql DataType
   * @param includeGeometry if true, the geometry attribute is included
   * @param includeExternalPK if true, the external primary key is included
   * @param includeReadOnly if true, readOnly attributes are included
   * @param normalizeColumnNames whether feature attribute names must be normalized
   *                             (lower case without spacial characters) to specify
   *                             table column names.
   * @return the sql string containing the list of columns for this schema
   */
  public String createColumnList(FeatureSchema schema,
                                    boolean includeSQLDataType,
                                    boolean includeGeometry,
                                    boolean includeExternalPK,
                                    boolean includeReadOnly,
                                    boolean normalizeColumnNames) {
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
      AttributeType type = schema.getAttributeType(i);
      if (type == AttributeType.GEOMETRY && !includeGeometry) continue;
      if (!includeExternalPK && schema.getExternalPrimaryKeyIndex() == i) continue;
      if (!includeReadOnly && schema.getExternalPrimaryKeyIndex()!=i && schema.isAttributeReadOnly(i)) continue;
      String name = normalizeColumnNames ?
              SQLUtil.normalize(schema.getAttributeName(i))
              :schema.getAttributeName(i);
      if (0 < count++) sb.append(", ");
      sb.append("\"").append(name).append("\"");
      if (includeSQLDataType) sb.append(" ").append(getDbTypeName(type));
    }
    return sb.toString();
  }

  /**
   * Create statement to add a spatial index on the specified geometry column.
   * The geometry column name must have its final form. Attribute name normalization
   * is the responsability of the calling method.
   * @param schemaName unquoted schema name or null if default schema is used
   * @param tableName unquoted table name
   * @param geometryColumn unquoted geometry column name
   * @return a sql string to add a spatial index
   */
  public String getAddSpatialIndexStatement(String schemaName, String tableName, String geometryColumn) {
    // Geometry index creation is different on different spatial databases
    // Do not add if it is not defined
    return ";";
  }

  /**
   * Creates the query String to add a GeometryColumn.
   * <p>Note 1 : In PostGIS 2.x, srid=-1 is automatically converted to srid=0 by
   * AddGeometryColumn function.</p>
   * <p>Note 2 : To stay compatible with PostGIS 1.x, last argument of
   * AddGeometryColumn is omitted. As a consequence, geometry type uses type modifier
   * rather than constraints (new default behaviour in 2.x)</p>
   * <p>The geometry column name must have its final form. Attribute name normalization
   * is the responsability of the calling method.</p>
   *
   * @param schemaName the name of the Schema
   * @param tableName the name of the Table
   * @param geometryColumn the name of the geometry column
   * @param srid the SRID for the geometry column
   * @param geometryType the (SQL) geometry type as a String
   * @param dim the coordinate dimension (2 or 3)
   * @return a SQL query string to add a geometry column to a table
   */
  public String getAddGeometryColumnStatement(String schemaName, String tableName,
                                                     String geometryColumn, int srid, String geometryType, int dim) {
    if (schemaName == null) {
      return "SELECT AddGeometryColumn('" + tableName + "','" +
              geometryColumn + "'," +
              srid + ",'" +
              geometryType.toUpperCase() + "'," +
              dim + ");";
    } else {
      return "SELECT AddGeometryColumn('" + schemaName + "','" +
              tableName + "','" +
              geometryColumn + "'," +
              srid + ",'" +
              geometryType.toUpperCase() + "'," +
              dim + ");";
    }
  }

  /**
   * Return standard SQL data type for OpenJUMP AttributeType.
   * This method must be overloaded by specific database oj2dbType
   * @param type OpenJUMP attribute type
   * @return the database datatype
   */
  protected String getDbTypeName(AttributeType type) {
    if (type == AttributeType.GEOMETRY)      return "varbinary";
    else if (type == AttributeType.STRING)   return "varchar";
    else if (type == AttributeType.INTEGER)  return "integer";
    else if (type == AttributeType.LONG)     return "bigint";
    else if (type == AttributeType.DOUBLE)   return "double precision";
    else if (type == AttributeType.NUMERIC)  return "numeric";
    else if (type == AttributeType.DATE)     return "timestamp";
    else if (type == AttributeType.BOOLEAN)  return "boolean";
    else if (type == AttributeType.OBJECT)   return "varbinary";
    else return "varchar";
  }

  ///**
  // * Return the JDBC datatype from the native datatype.
  // * This method is implemented for PostgreSQL datatypes. It must be overloaded
  // * by specific database mapping.
  // * @param sqlType
  // * @return
  // */
  //protected int getJdbcTypeFromSQL(String sqlType) {
  //  if (sqlType.equals("character"))                return Types.VARCHAR;
  //  else if (sqlType.equals("character varying"))   return Types.VARCHAR;
  //  else if (sqlType.equals("text"))                return Types.VARCHAR;
  //  else if (sqlType.equals("integer"))             return Types.INTEGER;
  //  else if (sqlType.equals("bigint"))              return Types.BIGINT;
  //  else if (sqlType.equals("bigserial"))           return Types.BIGINT;
  //  else if (sqlType.equals("bit"))                 return Types.BIT;
  //  else if (sqlType.equals("boolean"))             return Types.BOOLEAN;
  //  else if (sqlType.equals("date"))                return Types.DATE;
  //  else if (sqlType.equals("decimal"))             return Types.NUMERIC;
  //  else if (sqlType.equals("double"))              return Types.DOUBLE;
  //  else if (sqlType.equals("double precision"))    return Types.DOUBLE;
  //  else if (sqlType.equals("int4"))                return Types.INTEGER;
  //  else if (sqlType.equals("int8"))                return Types.BIGINT;
  //  else if (sqlType.equals("json"))                return Types.VARCHAR;
  //  else if (sqlType.equals("numeric"))             return Types.NUMERIC;
  //  else if (sqlType.equals("real"))                return Types.REAL;
  //  else if (sqlType.equals("smallint"))            return Types.SMALLINT;
  //  else if (sqlType.equals("serial"))              return Types.BIGINT;
  //  else if (sqlType.equals("serial4"))             return Types.INTEGER;
  //  else if (sqlType.equals("serial8"))             return Types.BIGINT;
  //  else if (sqlType.equals("timestamp"))           return Types.TIMESTAMP;
  //  else if (sqlType.equals("timestamp with time zone")) return Types.TIMESTAMP;
  //  else if (sqlType.equals("timestamp without time zone")) return Types.TIMESTAMP;
  //  else if (sqlType.equals("time"))                return Types.TIME;
  //  else if (sqlType.equals("varchar"))             return Types.VARCHAR;
  //  else                                            return Types.JAVA_OBJECT;
  //}
//

}
