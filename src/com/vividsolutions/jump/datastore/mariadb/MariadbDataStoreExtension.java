package com.vividsolutions.jump.datastore.mariadb;

import static com.vividsolutions.jump.datastore.mariadb.MariadbDataStoreDriver.JDBC_CLASS;

import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

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
    return disabled ? "Disabled: Missing mariadb-java-client-<version>.jar in classpath"
        : "";
  }

  public void configure(PlugInContext context) throws Exception {
    WorkbenchContext wbc = context.getWorkbenchContext();

    // registers the MariaDBDataStore driver to the system:
    try {
      ClassLoader pluginLoader = wbc.getWorkbench().getPlugInManager()
          .getClassLoader();
      // check for jar
      Class.forName(JDBC_CLASS, false, pluginLoader);

      // register the datastore
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new MariadbDataStoreDriver());
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new MysqlMariadbDataStoreDriver());
      
    } catch (Exception e) {
      disabled = true;
      wbc.getWorkbench()
          .getFrame()
          .log(
              getName() +" " + getMessage() +"\n\t" + e.toString(), this.getClass());
    }
  }

}
