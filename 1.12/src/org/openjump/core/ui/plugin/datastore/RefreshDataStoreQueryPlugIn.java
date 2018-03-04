package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.OpenProjectPlugIn;
import com.vividsolutions.jump.workbench.WorkbenchContext;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.openjump.core.ui.images.IconLoader;

/**
 * <code>RefreshDatastoreQueryPlugIn</code> runs the query associated
 * to this layer and replace the dataset.
 *
 * @author <a href="mailto:michael.michaud@free.fr">Michaël Michaud</a>
 */
public class RefreshDataStoreQueryPlugIn extends ThreadedBasePlugIn {

    public static final ImageIcon ICON = IconLoader.icon("arrow_refresh_sql.png");

    @Override
    public void initialize(PlugInContext context) throws Exception {
	    WorkbenchContext workbenchContext = context.getWorkbenchContext();
	    EnableCheck enableCheck = createEnableCheck(workbenchContext);
	    FeatureInstaller installer = new FeatureInstaller(workbenchContext);
	    JPopupMenu popupMenu = workbenchContext.getWorkbench().getFrame()
	        .getLayerNamePopupMenu();
		installer.addPopupMenuPlugin(popupMenu, this, new String[]{MenuNames.DATASTORE},
					getName(), false, ICON, enableCheck);
    }

    @Override
    public String getName() {
	    return I18N.get("org.openjump.core.ui.plugin.datastore.RefreshDataStoreQueryPlugIn.Refresh-datastore-query");
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
	    return true;
    }
    
    public void run(TaskMonitor monitor, final PlugInContext context) throws Exception {
        Layer[] selectedLayers = context.getSelectedLayers();
	    for (final Layer layer : selectedLayers) {
	        
	        DataSourceQuery dsq = layer.getDataSourceQuery();
	        if (dsq == null || !(dsq.getDataSource() instanceof DataStoreQueryDataSource)) {
	            continue;
	        }
	        
	        FeatureSchema oldSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
	        
	        OpenProjectPlugIn.load(layer,
                CoordinateSystemRegistry.instance(context.getWorkbenchContext().getBlackboard()),
                monitor);
            
            // Refreshing the layer change its schema. After a refresh,
            // the following code get the Operation and the readOnly properties
            // from the previous schema
            FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
		    if (oldSchema.equals(schema, false)) {
		        for (int i = 0 ; i < oldSchema.getAttributeCount() ; i++) {
		            String name = oldSchema.getAttributeName(i);
		            int index = schema.getAttributeIndex(name);
				    schema.setOperation(index, oldSchema.getOperation(i));
				    schema.setAttributeReadOnly(index, oldSchema.isAttributeReadOnly(i));
				}
	        }
            
            // setFeatureCollectionModified(false) must be set after fireFeaturesChanged
            // As in Layer.setFeatureCollection method, fireFeaturesChanged is
            // called in an invokeLater thread, setFeatureCollectionModified
            // must also be called in an invokeLater clause.
            SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    layer.setFeatureCollectionModified(false);
				}
            });
	    }
    }

    /**
     * Creates an EnableCheck object to enable the plugin if a project is active
     * and if only layers connected to a DataStoreQueryDataSource are selected.
     */
    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
	    final WorkbenchContext wc = workbenchContext;
	    EnableCheckFactory enableCheckFactory = new EnableCheckFactory(workbenchContext);
	    MultiEnableCheck enableCheck = new MultiEnableCheck();
	    enableCheck.add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck());
	    enableCheck.add(enableCheckFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, Layerable.class));
	    enableCheck.add(new EnableCheck(){
	        public String check(javax.swing.JComponent component) {
	            Layer[] selectedLayers = wc.getLayerNamePanel().getSelectedLayers();
	            for (Layer layer : selectedLayers) {
	                if (layer.getDataSourceQuery() == null ||
	                    !(layer.getDataSourceQuery().getDataSource() instanceof DataStoreQueryDataSource)) {
	                    return I18N.get("org.openjump.core.ui.plugin.datastore.RefreshDataStoreQueryPlugIn.Only-datastore-query-layers-must-be-selected");
	                }
	            }
	            return null;
	        }
	    });
	    return enableCheck;
    }

}
