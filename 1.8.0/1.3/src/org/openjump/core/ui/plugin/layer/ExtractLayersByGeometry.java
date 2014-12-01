/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2007 Integrated Systems Analysts, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */package org.openjump.core.ui.plugin.layer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class ExtractLayersByGeometry extends AbstractPlugIn {

	   private final static String EXTRACT_LAYERS_BY_GEOMETRY_TYPE = 
	    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-Layers-by-Geometry-Type");
	   private final static String ONLY_ONE_GEOMETRY_TYPE_FOUND = 
	    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Only-one-geometry-type-found");
	   private final static String POINT = 
	    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.point");
	   private final static String LINE = 
	    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.line");
	   private final static String AREA = 
	    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.area");
	 
	public ExtractLayersByGeometry() {

	}
	
	public void initialize(PlugInContext context) throws Exception {
		WorkbenchContext workbenchContext = context.getWorkbenchContext();
		FeatureInstaller featureInstaller = new FeatureInstaller(
				workbenchContext);
		/*
		JPopupMenu layerNamePopupMenu = workbenchContext.getWorkbench()
				.getFrame().getLayerNamePopupMenu();
		featureInstaller.addPopupMenuItem(layerNamePopupMenu, this, getName(),
				false, ICON, createEnableCheck(workbenchContext));
		*/
		//-- [sstein] this shouldn't be here, but as we try to use now the 
		//   default-plugins.xml for configuration, we need to add the submenu init
		//   in the first loaded submenu function
		featureInstaller.addMenuSeparator(MenuNames.EDIT);
        FeatureInstaller.addMainMenu(featureInstaller, new String[] {
                MenuNames.EDIT
              }, MenuNames.EXTRACT, 14);  
        //--
	    context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
		        new String[]
				{MenuNames.EDIT, MenuNames.EXTRACT},
				getName(), 
				false, 
				ICON, 
				createEnableCheck(context.getWorkbenchContext()));
	}

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext)
    {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);      
        return new MultiEnableCheck()
        .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    }  
    

	public boolean execute(PlugInContext context) throws Exception {
		Layer[] layers = context.getWorkbenchContext().getLayerNamePanel().getSelectedLayers();
		if (layers.length > 0){
			Layer layer = layers[0];
			if (!CompatibleFeatures(layer)) 
				splitLayer(context, layer);
			else
				context.getWorkbenchFrame().warnUser(ONLY_ONE_GEOMETRY_TYPE_FOUND);
			return true;
		} else
			return false;
	}
	
	public String getName() {
		return EXTRACT_LAYERS_BY_GEOMETRY_TYPE;
	}

    public static final ImageIcon ICON = IconLoader.icon("extract.gif");


    private List splitLayer(PlugInContext context, Layer layer)
    {
    	ArrayList newLayers = new ArrayList();
    	
    	if (!CompatibleFeatures(layer))
    	{
            ArrayList emptyFeatures = new ArrayList();
            ArrayList pointFeatures = new ArrayList();
            ArrayList lineFeatures = new ArrayList();
            ArrayList polyFeatures = new ArrayList();
            ArrayList groupFeatures = new ArrayList();
            
    		FeatureCollectionWrapper featureCollection = layer.getFeatureCollectionWrapper();
            List featureList = featureCollection.getFeatures();
            FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            
            //first find and handle all empty geometries and GeometryCollections
            for (Iterator i = featureList.iterator(); i.hasNext();)
            {
                Feature feature = (Feature) i.next();
                Geometry geometry = feature.getGeometry();
                
                if (geometry.isEmpty()) //going to delete it
                	emptyFeatures.add(feature);
                else if ((geometry instanceof GeometryCollection) &&
                		(!(geometry instanceof MultiPoint)) &&
                		(!(geometry instanceof MultiLineString)) &&
                		(!(geometry instanceof MultiPolygon))) //mixed geometry; going to explode it
                	groupFeatures.add(feature.clone(true));
            }   
            
//			for (int i = 0; i < emptyFeatures.size(); i++) //delete empty geometries
//            {
//            	featureCollection.remove((Feature) emptyFeatures.get(i));
//            }

			for (int i = 0; i < groupFeatures.size(); i++) //process GeometryCollections
            {
				Feature feature = (Feature) groupFeatures.get(i);
				GeometryCollection geometry = (GeometryCollection) feature.getGeometry();
				explodeGeometryCollection(featureSchema, pointFeatures, lineFeatures, polyFeatures, geometry, feature);
//            	featureCollection.remove(feature);
            }
			          
            featureCollection = layer.getFeatureCollectionWrapper();
            featureList = layer.getFeatureCollectionWrapper().getFeatures();

			boolean wasFiringEvents = context.getLayerManager().isFiringEvents();
            Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
            
            for (Iterator i = featureList.iterator(); i.hasNext();)
            {
            	Feature feature = (Feature) i.next();
            	Geometry geo = feature.getGeometry();
            	BitSet currFeatureBit = new BitSet();
            	currFeatureBit = setBit(currFeatureBit, geo);
            	
            	if  (currFeatureBit.get(pointBit))
            		pointFeatures.add(feature.clone(true));
            	
            	if  (currFeatureBit.get(lineBit))
            		lineFeatures.add(feature.clone(true));
            	
            	if (currFeatureBit.get(polyBit))
            		polyFeatures.add(feature.clone(true));
            }
            
            if (pointFeatures.size() > 0)
            {
		        Layer pointLayer = context.addLayer(selectedCategories.isEmpty()
		        ? StandardCategoryNames.WORKING
		        : selectedCategories.iterator().next().toString(), layer.getName() + "_" + POINT,
		        new FeatureDataset(featureSchema));
		        pointLayer.setStyles(layer.cloneStyles());
		        
		        FeatureCollectionWrapper pointFeatureCollection = pointLayer.getFeatureCollectionWrapper();
		        newLayers.add(pointLayer);
	           
				context.getLayerManager().setFiringEvents(false);
				for (int i = 0; i < pointFeatures.size(); i++)
	            {
	            	Feature feature = (Feature) pointFeatures.get(i);
	            	pointFeatureCollection.add(feature);
	            }
           }
            
            if (lineFeatures.size() > 0)
            {
    			context.getLayerManager().setFiringEvents(true);
		        Layer lineLayer = context.addLayer(selectedCategories.isEmpty()
				? StandardCategoryNames.WORKING
				: selectedCategories.iterator().next().toString(), layer.getName() + "_" + LINE,
				new FeatureDataset(featureSchema));
				lineLayer.setStyles(layer.cloneStyles());
				
		        FeatureCollectionWrapper lineFeatureCollection = lineLayer.getFeatureCollectionWrapper();
		        newLayers.add(lineLayer);
	           
				context.getLayerManager().setFiringEvents(false);
				for (int i = 0; i < lineFeatures.size(); i++)
	            {
	            	Feature feature = (Feature) lineFeatures.get(i);
	            	lineFeatureCollection.add(feature);
	            }
           }
            
            if (polyFeatures.size() > 0)
            {
    			context.getLayerManager().setFiringEvents(true);
		        Layer polyLayer = context.addLayer(selectedCategories.isEmpty()
				? StandardCategoryNames.WORKING
				: selectedCategories.iterator().next().toString(), layer.getName() + "_" + AREA,
				new FeatureDataset(featureSchema));
		        polyLayer.setStyles(layer.cloneStyles());
				        
		        FeatureCollectionWrapper polyFeatureCollection = polyLayer.getFeatureCollectionWrapper();
		        newLayers.add(polyLayer);
	           
				context.getLayerManager().setFiringEvents(false);
				for (int i = 0; i < polyFeatures.size(); i++)
	            {
	            	Feature feature = (Feature) polyFeatures.get(i);
	            	polyFeatureCollection.add(feature);
	            }
           }
    		context.getLayerManager().setFiringEvents(wasFiringEvents);
    		context.getLayerViewPanel().repaint();
    	}
    	return newLayers;
    }

	static final int emptyBit = 0;
	static final int pointBit = 1;
	static final int lineBit = 2;
	static final int polyBit = 3;
	
    private void explodeGeometryCollection(FeatureSchema fs, ArrayList pointFeatures, ArrayList lineFeatures, ArrayList polyFeatures, GeometryCollection geometryCollection, Feature feature)
    {
    	for (int i = 0; i < geometryCollection.getNumGeometries(); i++)
    	{
    		Geometry geometry = geometryCollection.getGeometryN(i);
    		
    		if (geometry instanceof GeometryCollection)
    		{
    			explodeGeometryCollection(fs, pointFeatures, lineFeatures, polyFeatures, (GeometryCollection) geometry, feature);
    		}
    		else
    		{
    			//Feature newFeature = new BasicFeature(fs);
    			Feature newFeature = feature.clone(true);
    			newFeature.setGeometry(geometry);
    			BitSet featureBit = new BitSet();
    			featureBit = setBit(featureBit, geometry);
    			if (featureBit.get(pointBit)) pointFeatures.add(newFeature);
    			if (featureBit.get(lineBit)) lineFeatures.add(newFeature);
    			if (featureBit.get(polyBit)) polyFeatures.add(newFeature);
    		}
    	}
    }

    private boolean CompatibleFeatures(Layer layer)
    {
        BitSet bitSet = new BitSet();        
        FeatureCollectionWrapper featureCollection = layer.getFeatureCollectionWrapper();
        List featureList = featureCollection.getFeatures();
        
        for (Iterator i = featureList.iterator(); i.hasNext();)
            bitSet = setBit(bitSet, ((Feature) i.next()).getGeometry());

        return (bitSet.cardinality() < 2);
    }
    

	private static BitSet setBit(BitSet bitSet, Geometry geometry)
    {
        BitSet newBitSet = (BitSet) bitSet.clone();
        if      (geometry.isEmpty())                  newBitSet.set(emptyBit);
        else if (geometry instanceof Point)           newBitSet.set(pointBit);
        else if (geometry instanceof MultiPoint)      newBitSet.set(pointBit);
        else if (geometry instanceof LineString)      newBitSet.set(lineBit);
        else if (geometry instanceof LinearRing)      newBitSet.set(lineBit);
        else if (geometry instanceof MultiLineString) newBitSet.set(lineBit);
        else if (geometry instanceof Polygon)         newBitSet.set(polyBit);
        else if (geometry instanceof MultiPolygon)    newBitSet.set(polyBit);
        else if (geometry instanceof GeometryCollection)
        {
            GeometryCollection geometryCollection = (GeometryCollection) geometry;
            for (int i = 0; i < geometryCollection.getNumGeometries(); i++)
                newBitSet = setBit(newBitSet, geometryCollection.getGeometryN(i));
        }
        return newBitSet;
    }
    


}
