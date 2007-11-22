/*
 * Created on 08.02.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package org.openjump.core.apitools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

import de.fho.jump.pirol.utilities.FeatureCollection.PirolFeatureCollection;
import de.fho.jump.pirol.utilities.FeatureCollection.PirolFeatureCollectionRole;
import de.fho.jump.pirol.utilities.FeatureCollection.RoleTriangularIrregularNet;
import de.fho.jump.pirol.utilities.i18n.PirolPlugInMessages;
import de.fho.jump.pirol.utilities.settings.PirolPlugInSettings;

/**
 * Class for more convenient use of Layer objects. Offers methods e.g. to get features that reside within
 * a given geometry or to easily add a result layer to the map.
 * 
 * @author Ole Rahn
 * 
 * FH Osnabrück - University of Applied Sciences Osnabrück
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 * 
 */
public class LayerTools extends ToolToMakeYourLifeEasier {
    protected PlugInContext context = null;

    
    public LayerTools(PlugInContext context) {
        super();
        this.context = context;
    }
    
    public Layer[] getSelectedLayers(){
        return this.context.getSelectedLayers();
    }
    
    public List getSelectedFeatures(){
        return SelectionTools.getSelectedFeatures(this.context);
    }
    
    public Geometry getFenceGeometry(){
        return SelectionTools.getFenceGeometry(this.context);
    }
    
    public List getFeaturesInFence(){
        SelectionTools st = new SelectionTools(this.context);
        return st.getFeaturesInFence();
    }
    
    public Feature[] getFeaturesInFenceInLayer( Layer layer, Geometry fenceGeometry ){
		return SelectionTools.getFeaturesInFenceInLayer(layer, fenceGeometry);
    }
    
    public Feature[] getFeaturesInFenceInLayer( Feature[] featArray, Geometry fenceGeometry ){
		return SelectionTools.getFeaturesInFenceInLayer(featArray, fenceGeometry);
    }
    
    public int getNumSelectedLayers(){
        return LayerTools.getNumSelectedLayers(this.context);
    }
    
    public static int getNumSelectedLayers( PlugInContext context ){
        return context.getSelectedLayers().length;
    }
    
    public static Layer addStandardResultLayer( String title, FeatureCollection featCollection, PlugInContext context, PirolFeatureCollectionRole role  ){
        return LayerTools.addStandardResultLayer(title, featCollection, Color.yellow, context, role);
    }
    
    public static Layer addAndSelectStandardResultLayer( String title, FeatureCollection featCollection, Color color,  PlugInContext context, PirolFeatureCollectionRole role  ){
        return LayerTools.addStandardResultLayer(title, featCollection, color, context, true, role);
    }
    
    public static Layer addAndSelectStandardResultLayer( String title, FeatureCollection featCollection, PlugInContext context, PirolFeatureCollectionRole role  ){
        return LayerTools.addStandardResultLayer(title, featCollection, Color.YELLOW, context, true, role);
    }
    
    public static Layer addStandardResultLayer( String title, FeatureCollection featCollection, Color color,  PlugInContext context, PirolFeatureCollectionRole role  ){
        return LayerTools.addStandardResultLayer(title, featCollection, color, context, false, role);
    }
    
    public static Layer addStandardResultLayer( String title, FeatureCollection featCollection, Color color,  PlugInContext context, boolean select, PirolFeatureCollectionRole role ){
        if (featCollection==null || context==null || LayerTools.getResultCategory(context)==null) return null;
        
        Layer newLayer = null;
        
        if (!PirolFeatureCollection.class.isInstance(featCollection)) {
            newLayer = new Layer( title, color, new PirolFeatureCollection(featCollection, role), context.getLayerManager());
        } else {
        	if (role != null)
        		((PirolFeatureCollection)featCollection).addRole(role);
            newLayer = new Layer( title, color, featCollection, context.getLayerManager());
        }
        
		context.getLayerManager().addLayer(PirolPlugInSettings.resultLayerCategory(), newLayer);
		
		if (select){
		    SelectionTools.selectLayer(context, newLayer);
		}
		
		return newLayer;
    }
    
    public Layer addStandardResultLayer( String title, FeatureCollection featCollection, PirolFeatureCollectionRole role ){
        return LayerTools.addStandardResultLayer(title,featCollection,this.context, role);
    }
    
    public Map getLayer2FeatureMap(List features){
        return LayerTools.getLayer2FeatureMap(features, this.context);
    }
    
    public final static Layer putGeometryArrayIntoMap(Geometry[] geometryArray, PlugInContext context){
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute(PirolPlugInMessages.getString("geometry"), AttributeType.GEOMETRY);
        FeatureCollection fc = new FeatureDataset(fs);
        
        Feature f;
        for (int i=0; i<geometryArray.length; i++){
            f = new BasicFeature(fs);
            f.setGeometry(geometryArray[i]);
            fc.add(f);
        }
        
        return LayerTools.addStandardResultLayer(PirolPlugInMessages.getString("triangles"), fc, context, new RoleTriangularIrregularNet());
    }
    
    public static Map getLayer2FeatureMap(List features, PlugInContext context){
        Layer layer = null;
        List<Layer> layers = context.getLayerManager().getVisibleLayers(false);
        
        Iterator iter = layers.iterator();
        Iterator featIter;
        Map layer2FeatList = new HashMap();
        List layerFeats;
        Feature feat;

        while ( iter.hasNext() ){
            layer = (Layer) iter.next();
            layerFeats = layer.getFeatureCollectionWrapper().getUltimateWrappee().getFeatures();
            featIter = features.iterator();
            while ( featIter.hasNext() ){
                feat = (Feature)featIter.next();
                
                if (layerFeats.contains(feat)){
                    if (!layer2FeatList.containsKey(layer)){
                        layer2FeatList.put(layer, new ArrayList());
                    }
                    ((ArrayList)layer2FeatList.get(layer)).add(feat);
                }
            }
        }
        return layer2FeatList;
    }
    
    public Category getResultCategory(){
        return LayerTools.getResultCategory(this.context);
    }
    
    public static Category getResultCategory( PlugInContext context ){
        Category category = context.getLayerManager().getCategory(PirolPlugInSettings.resultLayerCategory());
		
		if ( category == null ){
			context.getLayerManager().addCategory(PirolPlugInSettings.resultLayerCategory(), 0);
			category = context.getLayerManager().getCategory(PirolPlugInSettings.resultLayerCategory());
		}
		
		return category;
    } 
    
    /**
	 * Get a given number of selected Layers.
	 * @param context the current PlugInContext
	 * @param num max. number of layers to return, -1 returns all selected layers
	 * @return a given number of selected Layers, null if no Layers are selected
	 */
	public static Layer[] getSelectedLayers(PlugInContext context, int num){
	    Layer[] selLayers = context.getSelectedLayers();

	    if (selLayers.length==0){
		    return null;
		} else if (num <= 0){
		    return selLayers;
		} else {
		    Layer[] result = new Layer[num];
		    
		    for ( int i=0; i<selLayers.length && i<result.length; i++ ){
		        result[i] = selLayers[i];
		    }
		    
		    return result;
		}
	}
	
	/**
	 * get one Layer that is selected
	 * @param context the current PlugInContext
	 * @return one selected Layer, null if no Layers are selected
	 */
	public static Layer getSelectedLayer(PlugInContext context){
	    Layer[] selLayers = LayerTools.getSelectedLayers(context, 1);
	    
	    if (selLayers==null || selLayers.length==0){
	        return null;
	    }
	    return selLayers[0];
	}
    
    /**
     * get one Layer that is selected
     * @param context the current PlugInContext
     * @return one selected Layer, null if no Layers are selected
     */
    public static Layerable getSelectedLayerable(PlugInContext context, Class layerableClass){
        
        Collection selLayers = context.getLayerNamePanel().selectedNodes(layerableClass);
        
        if (selLayers==null || selLayers.size() == 0){
            return null;
        }
        
        return ((Layerable[])selLayers.toArray(new Layerable[0]))[0];
    }
    
    /**
     * returns a layername, that is not yet used in the layername panel
     */
    public static String getUniqueLayerName(PlugInContext context, String name){
        
        return context.getLayerManager().uniqueLayerName(name);
    }
    
}
