package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.*;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.FeatureInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all spatial databases DataStore connections. No need to
 * subclass for PostGIS, Oracle Spatial,
 *
 * @author nicolas Ribot
 */
public class SpatialDatabasesDSConnection implements DataStoreConnection {

  protected SpatialDatabasesDSMetadata dbMetadata;
  protected Connection connection;

  public SpatialDatabasesDSConnection(Connection conn) {
    connection = conn;
    dbMetadata = new SpatialDatabasesDSMetadata(this);
  }

  @Override
  public Connection getJdbcConnection() {
    return connection;
  }

  @Override
  public SpatialDatabasesDSMetadata getMetadata() {
    return dbMetadata;
  }

  public SpatialDatabasesSQLBuilder getSqlBuilder(
      SpatialReferenceSystemID srid, String[] colNames) {
    return new SpatialDatabasesSQLBuilder(this.dbMetadata, srid, colNames);
  }

  @Override
  public FeatureInputStream execute(Query query) throws Exception {
    if (query instanceof FilterQuery) {
      try {
        return executeFilterQuery((FilterQuery) query);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    if (query instanceof AdhocQuery) {
      return executeAdhocQuery((AdhocQuery) query);
    }
    throw new IllegalArgumentException(I18N.get(this.getClass().getName()
        + ".unsupported-query-type"));
  }

  /**
   * Executes a filter query.
   *
   * The SRID is optional for queries - it will be determined automatically from
   * the table metadata if not supplied.
   *
   * @param query the query to execute
   * @return the results of the query
   * @throws SQLException
   */
  public FeatureInputStream executeFilterQuery(FilterQuery query)
      throws SQLException {
    throw new UnsupportedOperationException();
  }

  /**
   * select gid, geom from departement where nom like 'A%' Executes an adhoc
   * query (direct SQL query)
   *
   * @param query the query to execute
   * @return a featureInputStream containing query's features
   * @throws Exception if no geometric column is found in the query
   */
  public FeatureInputStream executeAdhocQuery(AdhocQuery query)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws DataStoreException {
    try {
      connection.close();
    } catch (Exception ex) {
      throw new DataStoreException(ex);
    }
  }

  @Override
  public boolean isClosed() throws DataStoreException {
    try {
      return connection.isClosed();
    } catch (SQLException e) {
      throw new DataStoreException(e);
    }
  }

  public SpatialDatabasesValueConverterFactory getValueConverterFactory() {
    return new SpatialDatabasesValueConverterFactory(connection);
  }

  public String[] getCompatibleSchemaSubset(String schemaName, String tableName,
          FeatureSchema featureSchema, boolean normalizedColumnNames) throws Exception {
    SpatialDatabasesValueConverterFactory factory = getValueConverterFactory();
    ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " +
            SQLUtil.compose(schemaName, tableName) + " LIMIT 0");
    ResultSetMetaData rsMetaData = rs.getMetaData();
    List<String> commonAttributes = new ArrayList<String>();
    for (int i = 0 ; i < featureSchema.getAttributeCount() ; i++) {
      String attribut = featureSchema.getAttributeName(i);
      if (normalizedColumnNames) attribut = SQLUtil.normalize(attribut);
      try {
        AttributeType type = factory.getConverter(rsMetaData, rs.findColumn(attribut)).getType();
        if (type == featureSchema.getAttributeType(i));
        commonAttributes.add(featureSchema.getAttributeName(i));
      } catch(SQLException e) {}
    }
    return commonAttributes.toArray(new String[0]);
  }

}
