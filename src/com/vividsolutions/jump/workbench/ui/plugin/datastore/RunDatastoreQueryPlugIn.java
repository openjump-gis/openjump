package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.MenuNames;

import javax.swing.Icon;

/**
 * This PlugIn runs a SQL query against a datastore and creates a Layer
 * from the result.
 */
public class RunDatastoreQueryPlugIn extends AbstractAddDatastoreLayerPlugIn {

    protected ConnectionPanel createPanel( PlugInContext context ) {
        return new RunDatastoreQueryPanel( context.getWorkbenchContext() );
    }
    
    public void initialize(final PlugInContext context) throws Exception {
        super.initialize(context);
        context.getFeatureInstaller().addMainMenuPlugin(this, 
            new String[]{MenuNames.FILE});
    }

    protected Layerable createLayerable(
        ConnectionPanel panel,
        TaskMonitor monitor,
        PlugInContext context ) throws Exception {
        return createLayer( ( RunDatastoreQueryPanel ) panel, monitor, context );
    }

    public String getName(){
    	return I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn.Run-Datastore-Query");
    }
    
    public Icon getIcon(){
    	return IconLoader.icon("sql.png");
    }

    private Layer createLayer( final RunDatastoreQueryPanel panel,
        TaskMonitor monitor,
        final PlugInContext context ) throws Exception {

        panel.saveQuery();

        monitor.allowCancellationRequests();
        monitor.report( I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPlugIn.Creating-layer") );

        //int maxFeatures = ((Integer)LangUtil.ifNull( panel.getMaxFeatures(),
        //    new Integer(Integer.MAX_VALUE))).intValue();
        
        // added by Michael Michaud on 2009-11-22 to use aliases representing
        // view rectangle or selection in a query
        String driver = panel.getConnectionDescriptor().getDataStoreDriverClassName();
        String query = panel.getQuery();
        if (driver.contains("Postgis") && query.matches("(?s).*\\$\\{[^\\{\\}]*\\}.*")) {
            query = DataStoreQueryDataSource.expandQuery(query, context);
        }
        // end
        FeatureInputStream featureInputStream =
            ConnectionManager.instance(context.getWorkbenchContext())
                .getOpenConnection(panel.getConnectionDescriptor())
                .execute(new AdhocQuery(query));
        try {
            FeatureDataset featureDataset = new FeatureDataset(
                featureInputStream.getFeatureSchema());
            int i = 0;
            while (featureInputStream.hasNext() && !monitor.isCancelRequested()) {
                featureDataset.add(featureInputStream.next());
                monitor.report( ++i, -1, I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn.features"));
            }
            String name = panel.getLayerName();
            Layer layer = new Layer(name,
                context.getLayerManager().generateLayerFillColor(),
                featureDataset, context.getLayerManager());
            layer.setDataSourceQuery(new com.vividsolutions.jump.io.datasource.DataSourceQuery(
                new DataStoreQueryDataSource(name,
                    panel.getQuery(),
                    panel.getConnectionDescriptor(),
                    context.getWorkbenchContext()),
                panel.getQuery(), name));
            return layer;
        }
        finally {
            // This code had been added as an attempt to cancel a long running query
            // but it has a side effect on the connection which is closed
            // This peace of code is removed until a better solution is found 
            //if (featureInputStream instanceof com.vividsolutions.jump.datastore.postgis.PostgisFeatureInputStream) {
            //    java.sql.Statement stmt = 
            //    ((com.vividsolutions.jump.datastore.postgis.PostgisFeatureInputStream)featureInputStream).getStatement();
            //    if (stmt != null) stmt.cancel();
            //}
            featureInputStream.close();
        }
    }
    
}
