package com.vividsolutions.jump.datastore.postgis;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

/**
 * installs spatialite datastore into OJ
 */
public class PostgisDSExtension extends AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public PostgisDSExtension() {
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
