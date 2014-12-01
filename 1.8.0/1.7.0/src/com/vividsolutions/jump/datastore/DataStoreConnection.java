package com.vividsolutions.jump.datastore;

import com.vividsolutions.jump.io.FeatureInputStream;

/**
 * A connection to a datastore which can execute {@link Query}s.
 */
public interface DataStoreConnection
{
  DataStoreMetadata getMetadata();
  FeatureInputStream execute(Query query) throws Exception;
  void close() throws DataStoreException;
  boolean isClosed() throws DataStoreException;

}