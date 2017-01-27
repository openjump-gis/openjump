package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.OpenProjectPlugIn;

public class AddDatastoreLayerPlugIn extends AbstractAddDatastoreLayerPlugIn {

    public boolean execute(final PlugInContext context) throws Exception {
        ((AddDatastoreLayerPanel) panel(context)).setCaching(true);
        return super.execute(context);
    }

    public String getName(){
    	return I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPlugIn.Add-Datastore-Layer");
    }
    
    private Layer createLayer(
            final AddDatastoreLayerPanel panel,
            final PlugInContext context) throws Exception {

        Layer layer = new Layer(
                panel.getDatasetName(),
                context.getLayerManager().generateLayerFillColor(),
                AddNewLayerPlugIn.createBlankFeatureCollection(),
                context.getLayerManager());

        DataStoreDataSource ds = new DataStoreDataSource(
                panel.getDatasetName(),
                panel.getGeometryAttributeName(),
                panel.getWhereClause(),
                panel.getConnectionDescriptor(),
                panel.isCaching(),
                context.getWorkbenchContext());

        DataSourceQuery dsq = new DataSourceQuery(ds, null, panel.getDatasetName());

        layer.setDataSourceQuery(dsq);
        
        context.getLayerManager().setFiringEvents(false); // added by michaudm on 2009-04-23
        OpenProjectPlugIn.load( layer,
                                CoordinateSystemRegistry.instance(context.getWorkbenchContext().getBlackboard()),
                                new DummyTaskMonitor());
        context.getLayerManager().setFiringEvents(true); // added by michaudm on 2009-04-23
        return layer;
    }


    protected ConnectionPanel createPanel(PlugInContext context) {
        return new AddDatastoreLayerPanel(context.getWorkbenchContext());
    }

    protected Layerable createLayerable(ConnectionPanel panel,
            TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.report(I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPlugIn.Creating-layer"));
        return createLayer((AddDatastoreLayerPanel) panel, context);
    }

}