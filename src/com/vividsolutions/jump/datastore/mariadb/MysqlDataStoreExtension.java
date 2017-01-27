package com.vividsolutions.jump.datastore.mariadb;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

/**
 * installs the MySQL database datastore driver if available
 */
public class MysqlDataStoreExtension extends
    AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public MysqlDataStoreExtension() {
    super(MysqlDataStoreDriver.class, Collections
        .unmodifiableMap(new HashMap<String, String>() {
          {
            put(MysqlDataStoreDriver.JDBC_CLASS,
                "mysql-connector-java-<version>.jar");
          }
        }));
  }

  public String getName() {
    return "MySQL Spatial Datastore Extension";
  }

}
