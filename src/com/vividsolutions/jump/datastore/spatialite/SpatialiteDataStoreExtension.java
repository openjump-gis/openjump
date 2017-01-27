package com.vividsolutions.jump.datastore.spatialite;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

/**
 * installs spatialite datastore into OJ
 */
public class SpatialiteDataStoreExtension extends
    AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public SpatialiteDataStoreExtension() {
    super(SpatialiteDataStoreDriver.class, Collections
        .unmodifiableMap(new HashMap<String, String>() {
          {
            put(SpatialiteDataStoreDriver.JDBC_CLASS,
                "sqlite-jdbc-<version>.jar");
          }
        }));
  }

  public String getName() {
    return "Spatialite Datastore Extension";
  }

}
