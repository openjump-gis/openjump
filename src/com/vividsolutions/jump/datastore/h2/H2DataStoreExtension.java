package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

import java.sql.Driver;
import java.sql.DriverManager;

import static com.vividsolutions.jump.datastore.h2.H2DataStoreDriver.JDBC_CLASS;

/**
 * Extension for H2GIS Support
 */
public class H2DataStoreExtension extends Extension {
    private static boolean disabled = false;

    public String getName() {
        return "H2GIS Datastore Extension";
    }

    public String getVersion() {
        return "0.1 (2015-12-20)";
    }

    public String getMessage() {
        return disabled ? "Disabled: Missing h2-<version>.jar in classpath"
                : "";
    }

    public void configure(PlugInContext context) throws Exception {
        WorkbenchContext wbc = context.getWorkbenchContext();

        // registers the H2DataStore driver to the system:
        try {
            ClassLoader pluginLoader = wbc.getWorkbench().getPlugInManager()
                    .getClassLoader();
            // check for h2-<version>.jar
            DriverManager.registerDriver(
                    (Driver) Class.forName(JDBC_CLASS, true, pluginLoader).newInstance());

            // register the datastore
            wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
                    new H2DataStoreDriver());

            wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
                    new H2ServerDataStoreDriver());
        } catch (Exception e) {
            disabled = true;
            wbc.getWorkbench()
                    .getFrame()
                    .log(
                            "H2GIS Data Store disabled:\n\t" + e.toString()
                                    + "\n\tH2 JDBC Driver (h2-<version>.jar) must exist in the classpath !", this.getClass());
        }
    }

}