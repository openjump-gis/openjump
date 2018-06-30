package com.vividsolutions.jump.workbench.ui.plugin.datastore;


import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
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
        final AdhocQuery adhocQuery = new AdhocQuery(query);
        // end
        // Nicolas Ribot, 08 dec 2015:
        // manages several datasources now
        ConnectionDescriptor desc = panel.getConnectionDescriptor();
        final DataStoreConnection dscon = ConnectionManager.instance(context.getWorkbenchContext()).getOpenConnection(desc);
        RunnableQuery rQuery = new RunnableQuery(dscon, adhocQuery);
        FeatureInputStream featureInputStream;

        try {
            // SQL query is execute in a separate thread to give the user a chance
            // to interrupt it
            rQuery.start();
            while (rQuery.getState() == Thread.State.RUNNABLE) {
                Thread.sleep(1000);
                if (monitor.isCancelRequested()) {
                    throw new InterruptedException("The following query has been interrupted :\n" +
                            adhocQuery.getQuery());
                }
            }
            rQuery.join();
            featureInputStream = rQuery.getFeatureInputStream();
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
            dscon.close();
        }
    }

    class RunnableQuery extends Thread {

        DataStoreConnection connection;
        AdhocQuery query;
        FeatureInputStream featureInputStream;

        RunnableQuery(DataStoreConnection connection, AdhocQuery query) {
            this.connection = connection;
            this.query = query;
        }

        public void run() {
            try {
                featureInputStream = connection.execute(query);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        FeatureInputStream getFeatureInputStream() {
            return featureInputStream;
        }
    }
    
}
