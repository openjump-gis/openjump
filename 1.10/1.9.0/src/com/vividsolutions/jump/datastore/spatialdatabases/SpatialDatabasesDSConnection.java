package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.Query;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.Connection;
import java.sql.SQLException;

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
  public DataStoreMetadata getMetadata() {
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
}
