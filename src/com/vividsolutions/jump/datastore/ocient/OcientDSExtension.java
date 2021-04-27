package com.vividsolutions.jump.datastore.ocient;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

/**
 * installs ocient datastore into OJ
 */
public class OcientDSExtension extends AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public OcientDSExtension() {
    super(OcientDataStoreDriver.class, Collections
        .unmodifiableMap(new HashMap<String, String>() {
          {
            put(OcientDataStoreDriver.JDBC_CLASS, "ocient-jdbc4.jar");
          }
        }));
  }

  public String getName() {
    return "Ocient Datastore Extension";
  }

}
