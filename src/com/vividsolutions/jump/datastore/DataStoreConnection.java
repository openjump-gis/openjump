package com.vividsolutions.jump.datastore;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.io.FeatureInputStream;

import java.sql.Connection;

/**
 * A connection to a database datastore which can execute {@link Query}s.
 */
public interface DataStoreConnection {
  /**
   * retrieve metadata describing the database's datasets (column names etc.)
   * @return the DataStoreMetadata
   */
  DataStoreMetadata getMetadata();

  /**
   * expose underlying JDBC connection
   * @return the Connection
   */
  Connection getJdbcConnection();

  /**
   * expose sqlbuilder
   * @param srid the SpatialReferenceSystemID
   * @param colNames array of columns to query
   * @return the SpatialDatabasesSQLBuilder
   */
  SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid,
      String[] colNames);

  /**
   * run a query
   *
   * @param query the query to execute
   * @return a FeatureInputStream
   * @see Query
   * @throws Exception if an Exception occurs during query execution
   */
  FeatureInputStream execute(Query query) throws Exception;

  void close() throws DataStoreException;

  boolean isClosed() throws DataStoreException;

}