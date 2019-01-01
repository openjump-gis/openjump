package com.vividsolutions.jump.datastore.oracle;

import java.util.Collections;
import java.util.HashMap;

import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSExtension;

public class OracleDataStoreExtension extends AbstractSpatialDatabasesDSExtension {

  /**
   * customize the abstract implementation
   */
  public OracleDataStoreExtension() {
    super(OracleDataStoreDriver.class, Collections.unmodifiableMap(
        new HashMap<String, String>() {{
          put(OracleDataStoreDriver.JDBC_CLASS,"ojdbc6.jar");
          put(OracleDataStoreDriver.GT_SDO_CLASS_NAME, "gt2-oracle-spatial-2.x.jar");
      }}) );
  }

  public String getName() {
    return "Oracle Spatial Datastore Extension";
  }

}
