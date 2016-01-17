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
   */
  DataStoreMetadata getMetadata();

  /**
   * expose underlying JDBC connection
   */
  Connection getJdbcConnection();

  /**
   * expose sqlbuilder
   */
  SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid,
      String[] colNames);

  /**
   * run a query
   * 
   * @see {@link Query}
   */
  FeatureInputStream execute(Query query) throws Exception;

  void close() throws DataStoreException;

  boolean isClosed() throws DataStoreException;

}