package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

/**
 * This plugin is a write-only driver for a data source backed by a PostGIS
 * database.
 * Note : it uses the same WritableDataStoreDataSource as the
 * {@link org.openjump.core.ui.plugin.datastore.AddWritableDataStoreLayerWizard}
 */
public class SaveToPostGIS2PlugIn implements PlugIn {

    private PostGISSaveDataSourceQueryChooser saveChooser;

    /**
     * Initializes the plugin by creating the data source and data source query choosers.
     * @see com.vividsolutions.jump.workbench.plugin.PlugIn#initialize(com.vividsolutions.jump.workbench.plugin.PlugInContext)
     */
    public void initialize(PlugInContext context) {
        WritableDataStoreDataSource dataSource = new PostGISDataStoreDataSource();

        saveChooser = new PostGISSaveDataSourceQueryChooser(dataSource, context);

        DataSourceQueryChooserManager.get(
                context.getWorkbenchContext().getWorkbench().getBlackboard()
        ).addSaveDataSourceQueryChooser(saveChooser);

        //PostGISOpenWizard postGISOpenWizard = new PostGISOpenWizard(context.getWorkbenchContext());
        //OpenWizardPlugIn.addWizard(context.getWorkbenchContext(), postGISOpenWizard);
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
        return "Writable PostGIS Driver";
    }
}
