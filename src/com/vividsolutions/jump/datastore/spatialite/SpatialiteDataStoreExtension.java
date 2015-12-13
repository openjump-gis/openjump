package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.DataStoreDriver;
import static com.vividsolutions.jump.datastore.spatialite.SpatialiteDataStoreDriver.JDBC_CLASS;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import java.sql.Driver;
import java.sql.DriverManager;

/**
 * 
 * @author nicolas ribot
 */
public class SpatialiteDataStoreExtension extends Extension {
  private static boolean disabled = false;

  public String getName() {
    return "Spatialite Spatial Datastore Extension";
  }

  public String getVersion() {
    return "0.3 (2015-12-04)";
  }

  public String getMessage() {
    return disabled ? "Disabled: Missing sqlite-jdbc-<version>.jar in classpath"
        : "";
  }
  // TODO: refactor in a base class.
  public void configure(PlugInContext context) throws Exception {
    WorkbenchContext wbc = context.getWorkbenchContext();

    // registers the MariaDBDataStore driver to the system:
    try {
      ClassLoader pluginLoader = wbc.getWorkbench().getPlugInManager()
          .getClassLoader();
      // check for sqlite jar
      DriverManager.registerDriver(
          (Driver)Class.forName(JDBC_CLASS, true, pluginLoader).newInstance());

      // register the datastore
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new SpatialiteDataStoreDriver());
    } catch (Exception e) {
      disabled = true;
      wbc.getWorkbench()
          .getFrame()
          .log(
              "Sqlite/Spatialite Spatial Data Store disabled:\n\t" + e.toString() 
            + "\n\tSqlite JDBC Driver (sqlite-jdbc-<version>.jar) must exist in the classpath !", this.getClass());
    }
  }

}
