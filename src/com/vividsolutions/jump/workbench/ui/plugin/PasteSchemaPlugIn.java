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
 * www.ashs.isa.com
 */

package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Apply a Feature Schema to a FeatureCollection.
 * Attributes already in the FeatureCollection schema are ignored.
 */ 
public class PasteSchemaPlugIn extends AbstractPlugIn {
	
	public static ImageIcon ICON = IconLoader.icon("schema_paste.png");    
    
    public void initialize(PlugInContext context) throws Exception {
        
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        
        JPopupMenu layerNamePopupMenu = 
            context
            .getWorkbenchContext()
            .getWorkbench()
            .getFrame()
            .getLayerNamePopupMenu();
                
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, this, 
        		new String[] { I18N.get("ui.MenuNames.SCHEMA") }, 
        		getName(),
                false, 
                getIcon(),
                CopySchemaPlugIn.createEnableCheck(workbenchContext));
    }

    public boolean execute(PlugInContext context) throws Exception {
        
        Transferable transferable = GUIUtil.getContents(Toolkit.getDefaultToolkit().getSystemClipboard());

        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        	String schemaString = (String) transferable.getTransferData(DataFlavor.stringFlavor);
        	if (!schemaString.endsWith("\n"))
        		schemaString = schemaString + "\n";
        	FeatureSchema cbFeatureSchema = new FeatureSchema();
        	boolean isSchema = (schemaString.length() > 0);
        	
        	if (isSchema) {
        		int tabIndex = schemaString.indexOf("\t");
        		int crIndex = schemaString.indexOf("\n");
        		boolean endOfString = ((tabIndex < 0) || (crIndex < 0));
        		
	        	while (!endOfString) {
	        		String name = schemaString.substring(0, tabIndex);
	        		String typeStr = schemaString.substring(tabIndex + 1, crIndex);
	        		AttributeType type = AttributeType.STRING;
	        		
	        		if (typeStr.compareToIgnoreCase("STRING") == 0) 
	        			type = AttributeType.STRING;
	        		else if (typeStr.compareToIgnoreCase("DOUBLE") == 0)
	        			type = AttributeType.DOUBLE;
	        		else if (typeStr.compareToIgnoreCase("INTEGER") == 0)
	        			type = AttributeType.INTEGER;
	        		else if (typeStr.compareToIgnoreCase("DATE") == 0)
	        			type = AttributeType.DATE;
	        		else if (typeStr.compareToIgnoreCase("GEOMETRY") == 0)
	        			type = AttributeType.GEOMETRY;
	        		else if (typeStr.compareToIgnoreCase("OBJECT") == 0)
	        			type = AttributeType.OBJECT;
	        		else {
	        			isSchema = false;
	        			break;
	        		}
	        		
	        		cbFeatureSchema.addAttribute(name, type);
	        		schemaString = schemaString.substring(crIndex + 1);
	        		tabIndex = schemaString.indexOf("\t");
	        		crIndex = schemaString.indexOf("\n");
	        		endOfString = ((tabIndex < 0) || (crIndex < 0));
	        	}
        	    
	        	isSchema = (cbFeatureSchema.getAttributeCount() > 0);
        	}
        	
        	if (isSchema) {
	        	Collection layerCollection = (Collection) context.getWorkbenchContext().getLayerNamePanel().selectedNodes(Layer.class);
	        	
	            for (Iterator i = layerCollection.iterator(); i.hasNext();) {
	            	Layer layer = (Layer) i.next();
	            	FeatureSchema layerSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
	            	int numAttributes = cbFeatureSchema.getAttributeCount();
	            	boolean changedSchema = false;
	            	
	            	for (int index = 0; index < numAttributes; index++) {
	            		String name = cbFeatureSchema.getAttributeName(index);
	            		AttributeType type = cbFeatureSchema.getAttributeType(index);
	            		
		            	if (!layerSchema.hasAttribute(name)) {
		            		if ((type == AttributeType.STRING) ||
		            			(type == AttributeType.DOUBLE) ||
		            			(type == AttributeType.INTEGER) ||
		            			(type == AttributeType.DATE) ||
		            			(type == AttributeType.OBJECT)) {
		            		    layerSchema.addAttribute(name, type);
		            		    changedSchema = true;
		            		}
		            	}
	            	}
	            	
	            	if (changedSchema) {
		                List layerFeatures = layer.getFeatureCollectionWrapper().getFeatures();

	            		for (int j = 0; j < layerFeatures.size(); j++) {
	            			Feature newFeature = new BasicFeature(layerSchema);
		                    Feature origFeature = (Feature) layerFeatures.get(j);
		                    int numAttribs = origFeature.getAttributes().length;
		                    
		                    for (int k = 0; k < numAttribs; k++)
		                    	newFeature.setAttribute(k, origFeature.getAttribute(k));
		                    
		                    origFeature.setSchema(newFeature.getSchema());
		                    origFeature.setAttributes(newFeature.getAttributes());
		                }
	            		
		            	layer.setFeatureCollectionModified(true);
		            	layer.fireLayerChanged(LayerEventType.METADATA_CHANGED);
	            	}
	            }
        	}
        }
        return true;
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1))
            .add(checkFactory.createSelectedLayersMustBeEditableCheck());
    }  
  
    public ImageIcon getIcon() {
        return ICON;
    }
    
}
