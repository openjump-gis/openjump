package com.vividsolutions.jump.datastore.mariadb;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

/**
 * a database datastore extension for accessing MariaDB and MySQL w/ the MariaDB
 * JDBC driver
 */
public class MariadbDataStoreExtension extends
    AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public MariadbDataStoreExtension() {
    super(new Class[] { MariadbDataStoreDriver.class,
        MysqlMariadbDataStoreDriver.class }, Collections
        .unmodifiableMap(new HashMap<String, String>() {
          {
            put(MariadbDataStoreDriver.JDBC_CLASS,
                "mariadb-java-client-<version>.jar");
          }
        }));
  }

  public String getName() {
    return "MariaDB/MySQL Spatial Datastore Extension";
  }

}
