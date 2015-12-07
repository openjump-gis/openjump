package com.vividsolutions.jump.datastore;

import static com.vividsolutions.jump.datastore.oracle.OracleDataStoreDriver.JDBC_CLASS;
import static com.vividsolutions.jump.datastore.oracle.OracleDataStoreDriver.GT_SDO_CLASS_NAME;

import com.vividsolutions.jump.datastore.oracle.OracleDataStoreDriver;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import java.sql.Driver;
import java.sql.DriverManager;

/**
 * 
 * @author nicolas ribot
 */
public class OracleDataStoreExtension extends Extension {
  private static boolean disabled = false;

  public String getName() {
    return "Oracle Spatial Datastore Extension";
  }

  public String getVersion() {
    return "0.3 (2015-12-04)";
  }

  public String getMessage() {
    return disabled ? "Disabled: Missing either ojdbc6.jar or gt2-oracle-spatial-2.x.jar in classpath"
        : "";
  }

  public void configure(PlugInContext context) throws Exception {
    WorkbenchContext wbc = context.getWorkbenchContext();

    // registers the OracleDataStore driver to the system:
    try {
      ClassLoader pluginLoader = wbc.getWorkbench().getPlugInManager()
          .getClassLoader();
      // check for ojdbc6.jar
      DriverManager.registerDriver(
          (Driver)Class.forName(JDBC_CLASS, true, pluginLoader).newInstance());

      // check for gt2-oracle-spatial-2.x.jar
      Class.forName(GT_SDO_CLASS_NAME, true, pluginLoader)
          .newInstance();
      // register the datastore
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new OracleDataStoreDriver());
    } catch (Exception e) {
      disabled = true;
      wbc.getWorkbench()
          .getFrame()
          .log(
              "Oracle Spatial Data Store disabled:\n\t" + e.toString() 
            + "\n\tOracle JDBC Driver and gt2-oracle-spatial-2.x.jar must exist in the classpath !", this.getClass());
    }
  }

}
