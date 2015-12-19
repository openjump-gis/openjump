package com.vividsolutions.jump.datastore;

import com.vividsolutions.jump.io.FeatureInputStream;
import java.sql.Connection;

/**
 * A connection to a datastore which can execute {@link Query}s.
 */
public interface DataStoreConnection
{
  DataStoreMetadata getMetadata();
  FeatureInputStream execute(Query query) throws Exception;
  void close() throws DataStoreException;
  boolean isClosed() throws DataStoreException;
  // Nicolas Ribot: added now several Spatial Databases can implement this class
  Connection getJdbcConnection();

}