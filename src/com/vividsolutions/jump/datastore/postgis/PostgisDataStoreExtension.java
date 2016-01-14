package com.vividsolutions.jump.datastore.postgis;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDataStoreExtension;

/**
 * installs spatialite datastore into OJ
 */
public class PostgisDataStoreExtension extends AbstractSpatialDataStoreExtension {

  /**
   * customize the abstract implementation
   */
  public PostgisDataStoreExtension() {
    super(PostgisDataStoreDriver.class, Collections
        .unmodifiableMap(new HashMap<String, String>() {
          {
            put(PostgisDataStoreDriver.JDBC_CLASS, "postgresql-<version>.jdbc4.jar");
          }
        }));
  }

  public String getName() {
    return "Postgis Datastore Extension";
  }

}
