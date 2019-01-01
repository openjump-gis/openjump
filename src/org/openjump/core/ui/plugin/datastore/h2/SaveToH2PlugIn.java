package org.openjump.core.ui.plugin.datastore.h2;

import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

/**
 * This plugin is a write-only driver for a data source backed by a H2 database.
 * Note : it uses the same WritableDataStoreDataSource as the
 * {@link org.openjump.core.ui.plugin.datastore.AddWritableDataStoreLayerWizard}
 *
 * This plugin is not initialized until it is added in the default-plugin.xml
 * file along with H2GIS jar files or in an extension.
 */
public class SaveToH2PlugIn implements PlugIn {

    private H2SaveDataSourceQueryChooser saveChooser;

    /**
     * Initializes the plugin by creating the data source and data source query choosers.
     * @see com.vividsolutions.jump.workbench.plugin.PlugIn#initialize(com.vividsolutions.jump.workbench.plugin.PlugInContext)
     */
    public void initialize(PlugInContext context) {
        WritableDataStoreDataSource dataSource = new H2DataStoreDataSource();

        saveChooser = new H2SaveDataSourceQueryChooser(dataSource, context);

        DataSourceQueryChooserManager.get(
                context.getWorkbenchContext().getWorkbench().getBlackboard()
        ).addSaveDataSourceQueryChooser(saveChooser);
    }

    /**
     * This function always returns false.
     */
    public boolean execute(PlugInContext context) {
        return false;
    }

    /**
     * @see com.vividsolutions.jump.workbench.plugin.PlugIn#getName()
     */
    public String getName() {
        return "Writable H2 Driver";
    }
}
