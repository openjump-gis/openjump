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


public class RunDatastoreQueryPlugIn extends
    AbstractAddDatastoreLayerPlugIn {

    protected ConnectionPanel createPanel( PlugInContext context ) {
        return new RunDatastoreQueryPanel( context.getWorkbenchContext() );
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

    private Layer createLayer( final RunDatastoreQueryPanel panel,
        TaskMonitor monitor,
        final PlugInContext context ) throws Exception {

        panel.saveQuery();

        monitor.allowCancellationRequests();
        monitor.report( I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPlugIn.Creating-layer") );

        int maxFeatures = ( ( Integer ) LangUtil.ifNull( panel.getMaxFeatures(),
            new Integer( Integer.MAX_VALUE ) ) ).intValue();
        // added by Michael Michaud on 2009-11-22 to use aliases representing
        // view rectangle or selection in a query
        String driver = panel.getConnectionDescriptor().getDataStoreDriverClassName();
        String query = panel.getQuery();
        if (driver.contains("Postgis") && query.matches("(?s).*\\$\\{[^\\{\\}]*\\}.*")) {
            query = expandQuery(query, context);
        }
        // end
        FeatureInputStream featureInputStream = ConnectionManager.instance(
            context.getWorkbenchContext() )
            .getOpenConnection( panel.getConnectionDescriptor() ).execute(
            new AdhocQuery( /*panel.getQuery()*/query ) );
        try {
            FeatureDataset featureDataset = new FeatureDataset(
                featureInputStream.getFeatureSchema() );
            int i = 0;
            while ( featureInputStream.hasNext()
                 && featureDataset.size() < maxFeatures
                 && !monitor.isCancelRequested() ) {
                featureDataset.add( featureInputStream.next() );
                monitor.report( ++i, -1, I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn.features"));
            }
            // Added by Michael Michaud on 2008-08-10 to avoid very long layer
            // names issued from long queries (keep about 64 characters)
            String name = panel.getQuery();
            if (name != null) {
                // remove the column definition part of the select
                name = name.replaceAll("(?s).*from\\s+","").trim();
                name = context.getLayerManager().uniqueLayerName(
                    name.substring(0, Math.min(name.length(), 64)).trim()
                );
            }
            return new Layer( name, context.getLayerManager()
                .generateLayerFillColor(), featureDataset, context.getLayerManager() );
        } finally {
            featureInputStream.close();
        }
    }
    
    private String expandQuery(String query, PlugInContext context) {
        GeometryFactory gf = new GeometryFactory();
        Geometry viewG = gf.toGeometry(context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates());
        Geometry fenceG = context.getLayerViewPanel().getFence();
        if (viewG != null) {
            query = query.replaceAll("\\$\\{view\\}", "\\${view:-1}");
            query = query.replaceAll("\\$\\{view(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + viewG.toText() + "',$1)");
        }
        if (fenceG != null) {
            query = query.replaceAll("\\$\\{fence\\}", "\\${fence:-1}");
            query = query.replaceAll("\\$\\{fence(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + fenceG.toText() + "',$1)");
        }
        return query;
    }
    
}
