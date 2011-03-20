package org.openjump.core.ui.plugin.datastore;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;

import org.openjump.core.ui.images.IconLoader;

/**
 * <code>RefreshDatastoreQueryPlugIn</code> runs the query associated
 * to this layer and replace the dataset.
 *
 * @author <a href="mailto:michael.michaud@free.fr">Michaël Michaud</a>
 */
public class RefreshDataStoreQueryPlugIn extends AbstractPlugIn {

    public static final ImageIcon ICON = IconLoader.icon("arrow_refresh_sql.png");

    @Override
    public void initialize(PlugInContext context) throws Exception {
	    WorkbenchContext workbenchContext = context.getWorkbenchContext();
	    EnableCheck enableCheck = createEnableCheck(workbenchContext);
	    FeatureInstaller installer = new FeatureInstaller(workbenchContext);
	    JPopupMenu popupMenu = workbenchContext.getWorkbench().getFrame()
	        .getLayerNamePopupMenu();
	        installer.addPopupMenuItem(popupMenu, this, new String[]{MenuNames.DATASTORE},
		     getName(), false, null, enableCheck);		
    }

    @Override
    public String getName() {
	return I18N
		.get("org.openjump.core.ui.plugin.datastore.RefreshDataStoreQueryPlugIn.Refresh-datastore-query");
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
	    reportNothingToUndoYet(context);
	    
	    
	    Layer[] selectedLayers = context.getSelectedLayers();
	    for (Layer layer : selectedLayers) {
	        DataSourceQuery dsq = layer.getDataSourceQuery();
	        layer.setFeatureCollection(dsq.getDataSource().getConnection()
	            .executeQuery(dsq.getQuery(), new DummyTaskMonitor()));
	        context.getLayerManager().fireLayerChanged(layer, LayerEventType.APPEARANCE_CHANGED);
	    }
	    return true;
    }

    /**
     * @param workbenchContext
     * @return an enable check
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
