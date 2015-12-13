package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.DataStoreDriver;
import static com.vividsolutions.jump.datastore.mariadb.MariadbDataStoreDriver.JDBC_CLASS;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import java.sql.Driver;
import java.sql.DriverManager;

/**
 * 
 * @author nicolas ribot
 */
public class MariadbDataStoreExtension extends Extension {
  private static boolean disabled = false;

  public String getName() {
    return "MariaDB/MySQL Spatial Datastore Extension";
  }

  public String getVersion() {
    return "0.3 (2015-12-04)";
  }

  public String getMessage() {
    return disabled ? "Disabled: Missing mysql-connector-java-<version>.jar in classpath"
        : "";
  }

  public void configure(PlugInContext context) throws Exception {
    WorkbenchContext wbc = context.getWorkbenchContext();

    // registers the MariaDBDataStore driver to the system:
    try {
      ClassLoader pluginLoader = wbc.getWorkbench().getPlugInManager()
          .getClassLoader();
      // check for ojdbc6.jar
      DriverManager.registerDriver(
          (Driver)Class.forName(JDBC_CLASS, true, pluginLoader).newInstance());

      // register the datastore
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new MariadbDataStoreDriver());
    } catch (Exception e) {
      disabled = true;
      wbc.getWorkbench()
          .getFrame()
          .log(
              "MariaDB Spatial Data Store disabled:\n\t" + e.toString() 
            + "\n\tMariaDB JDBC Driver (mysql-connector-java-<version>.jar) must exist in the classpath !", this.getClass());
    }
  }

}
