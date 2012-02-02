/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 */

package org.openjump.core.ui.plugin.layer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopySelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

public class CombineSelectedLayersPlugIn extends AbstractPlugIn {

	private static final String LAYER = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.LAYER");	
	private static final String COMBINE_SELECTED_LAYERS = 
		I18N.get("org.openjump.core.ui.plugin.layer.CombineSelectedLayers");	
	
    public void initialize(PlugInContext context) throws Exception
    {     
	    context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
		        new String[]
		        {MenuNames.EDIT},
				COMBINE_SELECTED_LAYERS, 
				false, 
				null, 
				createEnableCheck(context.getWorkbenchContext()));
   }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
        reportNothingToUndoYet(context);
        new CopySelectedItemsPlugIn().execute(context);
        
        Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();
        
    	FeatureSchema featureSchema = new FeatureSchema();
  	  	featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    	featureSchema.addAttribute(LAYER, AttributeType.STRING);
    	//add all attributes from selected layers
        for (int i=0; i<selectedLayers.length; i++) {
        	FeatureSchema schema = selectedLayers[i].getFeatureCollectionWrapper()
        		.getFeatureSchema();
        	for (int j=0; j<schema.getAttributeCount(); j++) {
        		String name = schema.getAttributeName(j);
        		if (AttributeType.GEOMETRY == schema.getAttributeType(name)) {
        		    continue;
        		}
        		if (!featureSchema.hasAttribute(name)) {
        			featureSchema.addAttribute(name, schema.getAttributeType(name));
        		} else if (schema.getAttributeType(name) 
        				!= featureSchema.getAttributeType(name)) {
        			featureSchema.addAttribute(name
        					+getAttributeTypeChar(schema.getAttributeType(name)), 
        					schema.getAttributeType(name));        			
        		}
        	}
        }

        FeatureDataset featureDataset = new FeatureDataset(featureSchema);
        
        Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
        Layer newLayer = context.addLayer(selectedCategories.isEmpty()
	        ? StandardCategoryNames.RESULT
	        : selectedCategories.iterator().next().toString(), "Combined",
	        featureDataset);
        
        newLayer.setFeatureCollectionModified(true).setEditable(true);
        Map attributeToStyleMap = new HashMap();
    	ColorScheme colorScheme = ColorScheme.create("Set 3 (ColorBrewer)");		  
        for (int i=0; i<selectedLayers.length; i++) {
        	Layer layer = selectedLayers[i];
        	Collection features = layer.getFeatureCollectionWrapper().getFeatures();
        	newLayer.getFeatureCollectionWrapper().addAll(
        			conform(features,featureSchema, layer.getName()));
        	attributeToStyleMap.put(layer.getName(), new BasicStyle(colorScheme.next()));
        }

        newLayer.getBasicStyle().setEnabled(false);
        ColorThemingStyle themeStyle = new ColorThemingStyle(LAYER, attributeToStyleMap, new BasicStyle(Color.gray));
        themeStyle.setEnabled(true);
        newLayer.addStyle(themeStyle);
        ColorThemingStyle.get(newLayer).setEnabled(true);
        newLayer.removeStyle(ColorThemingStyle.get(newLayer));
        ColorThemingStyle.get(newLayer).setEnabled(true);
        newLayer.getBasicStyle().setEnabled(false);
        
        return true;
    }
    
    private static String getAttributeTypeChar(AttributeType type) {
    	return type.toString().substring(0, 1);
    }
    
    public static Collection conform(Collection features,
    		FeatureSchema targetFeatureSchema, String layerName) {
    	final ArrayList featureCopies = new ArrayList();

    	for (Iterator i = features.iterator(); i.hasNext();) {
    		Feature feature = (Feature) i.next();
    		featureCopies.add(conform(feature, targetFeatureSchema, layerName));
    	}

    	return featureCopies;
    }

    private static Feature conform(Feature original,
    		FeatureSchema targetFeatureSchema, String layerName) {
    	//Transfer as many attributes as possible, matching on name. [Jon Aquino]
    	Feature copy = new BasicFeature(targetFeatureSchema);
    	copy.setGeometry((Geometry) original.getGeometry().clone());

    	for (int i = 0; i < original.getSchema().getAttributeCount(); i++) {
    		if (i == original.getSchema().getGeometryIndex()) {
    			continue;
    		}

    		String attributeName = original.getSchema().getAttributeName(i);
    		String newAttributeName = original.getSchema().getAttributeName(i);
    		if (!copy.getSchema().hasAttribute(attributeName)) {
    			continue;
    		}

    		if (copy.getSchema().getAttributeType(attributeName) != original.getSchema()
    				.getAttributeType(attributeName)) {
    			newAttributeName += getAttributeTypeChar(
    					original.getSchema().getAttributeType(attributeName));
         		if (copy.getSchema().getAttributeType(newAttributeName) != original.getSchema()
        				.getAttributeType(attributeName)) {
        			continue;
        		}
    		}

    		copy.setAttribute(newAttributeName, original.getAttribute(attributeName));
    	}
		copy.setAttribute(LAYER, layerName);

    	return copy;
    }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
        .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(2));
    }
}
