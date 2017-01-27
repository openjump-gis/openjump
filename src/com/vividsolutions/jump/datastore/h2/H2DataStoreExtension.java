package com.vividsolutions.jump.datastore.h2;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

/**
 * Extension for H2GIS Support
 */
public class H2DataStoreExtension extends AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public H2DataStoreExtension() {
    super(
        new Class[] { H2DataStoreDriver.class, H2ServerDataStoreDriver.class },
        Collections.unmodifiableMap(new HashMap<String, String>() {
          {
            put(H2DataStoreDriver.JDBC_CLASS, "h2-<version>.jar");
          }
        }));
  }

  public String getName() {
    return "H2GIS Datastore Extension";
  }

}