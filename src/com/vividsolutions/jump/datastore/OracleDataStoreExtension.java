package com.vividsolutions.jump.datastore;

import static com.vividsolutions.jump.datastore.oracle.OracleDataStoreDriver.JDBC_CLASS;

import com.vividsolutions.jump.datastore.oracle.OracleDataStoreDriver;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class OracleDataStoreExtension extends Extension {
  private static boolean disabled = false;

  public String getName() {
    return "Oracle Spatial Datastore Extension (Nicolas Ribot)";
  }

  public String getVersion() {
    return "0.2 (3.12.2015)";
  }

  public String getMessage() {
    return disabled ? "Disabled: Missing either ojdbc6.jar or gt2-oracle-spatial-2.x.jar"
        : "";
  }

  public void configure(PlugInContext context) throws Exception {
    WorkbenchContext wbc = context.getWorkbenchContext();

    // registers the OracleDataStore driver to the system:
    try {
      ClassLoader pluginLoader = wbc.getWorkbench().getPlugInManager()
          .getClassLoader();
      // check for ojdbc6.jar
      Class.forName(JDBC_CLASS, true, pluginLoader).newInstance();
      // check for gt2-oracle-spatial-2.x.jar
      Class.forName("org.geotools.data.oracle.sdo.SDO", true, pluginLoader)
          .newInstance();
      // register the datastore
      wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
          new OracleDataStoreDriver());

    } catch (Exception e) {
      disabled = true;
      wbc.getWorkbench()
          .getFrame()
          .log(
              "Oracle Spatial Data Store disabled:\n" + e.toString() );
    }
  }

}