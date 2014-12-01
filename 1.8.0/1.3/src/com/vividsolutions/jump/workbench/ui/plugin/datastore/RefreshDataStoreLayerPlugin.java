package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.cache.CachingFeatureCollection;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class RefreshDataStoreLayerPlugin extends AbstractPlugIn {

    public static final ImageIcon ICON = IconLoader.icon("arrow_refresh.png");

    public RefreshDataStoreLayerPlugin() {
        super(I18N.get("ui.plugin.datastore.RefreshDataStoreLayerPlugin.Refresh-Layer"));
    }
    

    public static EnableCheck createEnableCheck(final WorkbenchContext context) {
        MultiEnableCheck mec = new MultiEnableCheck();

        mec.add(new EnableCheckFactory(context).createExactlyNLayersMustBeSelectedCheck(1));
        mec.add(
            new EnableCheck() {
                public String check(JComponent component) {
                    DataSourceQuery dsq =  context.getLayerNamePanel().getSelectedLayers()[0].getDataSourceQuery();
                    if(dsq!=null){
                    	return dsq.getDataSource() == null?I18N.get("ui.plugin.datastore.RefreshDataStoreLayerPlugin.Layer-must-have-a-Data-Source"):
                    		dsq.getDataSource() instanceof DataStoreDataSource?null:I18N.get("ui.plugin.datastore.RefreshDataStoreLayerPlugin.Layer-must-be-a-DataStore");
                    }else{
                    	return I18N.get("ui.plugin.datastore.RefreshDataStoreLayerPlugin.Layer-must-have-a-Data-Source");
                    }
                }
            }
            );
        return mec;
    }
    
    public boolean execute(PlugInContext context) throws Exception {
    	Layer layer = context.getLayerNamePanel().getSelectedLayers()[0];
    	FeatureCollectionWrapper fcw =  layer.getFeatureCollectionWrapper();
    	
    	while (fcw != null && !(fcw instanceof CachingFeatureCollection)){
    		FeatureCollection fc = fcw.getWrappee();
    		fcw = null;
    		if(fc instanceof FeatureCollectionWrapper)
    			fcw = (FeatureCollectionWrapper)fc;
    	}
    	
    	
    	if(fcw != null){
    		// must be a cache
    		CachingFeatureCollection cfc = (CachingFeatureCollection)fcw;
    		cfc.emptyCache();
    		
    		context.getLayerManager().fireLayerChanged(layer,LayerEventType.APPEARANCE_CHANGED);
    	}
    	
        return false;
    }
}
