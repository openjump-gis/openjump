package com.vividsolutions.jump.datastore.mariadb;

import static com.vividsolutions.jump.datastore.mariadb.MysqlDataStoreDriver.JDBC_CLASS;

import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * 
 * @author nicolas ribot
 */
public class MysqlDataStoreExtension extends MariadbDataStoreExtension {
  private static boolean disabled = false;

  public String getName() {
    return "MySQL Spatial Datastore Extension";
  }

  public String getVersion() {
    return super.getVersion();
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
      // check for jar
      Class.forName(JDBC_CLASS, false, pluginLoader);

      // register the datastore
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new MysqlDataStoreDriver());

    } catch (Exception e) {
      disabled = true;
      wbc.getWorkbench()
          .getFrame()
          .log(
              getName() +" " + getMessage() +"\n\t" + e.toString(), this.getClass());
    }
  }

}
