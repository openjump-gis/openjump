/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2008 Integrated Systems Analysts, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;

public class ExtractLayersByAttribute extends AbstractPlugIn {

	private final static String EXTRACT_LAYERS_BY_ATTRIBUTE = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Extract-Layer-by-Attribute");
    private static final String LAYER_ATTRIBUTE = 
    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Attribute");
	private static final String DIALOGMSG = 
    	I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Extracts-layers-using-a-common-attribute");
	private static final String LAYER = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.LAYER");	
	private static final String TEXT = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.TEXT");
	private static final String EXTRACT = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Extract");
	// NULL has not to be translated
	private static final String NULL = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute._NULL_");
    private static final String EMPTY = 
		I18N.get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute._EMPTY_");
	
	private Layer sourceLayer = null;
	private JComboBox layerAttributeComboBox = null;
	private boolean textAttributeFound = false;
	private String textAttribute = null;
	 	 
	public void initialize(PlugInContext context) throws Exception {
	    context.getFeatureInstaller().addMainMenuItem(this,
		        new String[]
				{MenuNames.EDIT, MenuNames.EXTRACT},
				getName(), 
				false, 
				ICON, 
				createEnableCheck(context.getWorkbenchContext()));
	}

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext)
    {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);      
        return new MultiEnableCheck()
        .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1))
        .add(new EnableCheck() {
                // At one point, this EnableCheck should be added to
                // EnableCheckFactory with it's own I18N key string
                public String check(JComponent component) {
                    Layer[] lyrs = workbenchContext.getLayerNamePanel().getSelectedLayers();
                    if (lyrs.length == 0) {
                        return I18N.get("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-be-selected");
                    }
                    else if (lyrs[0].getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount() < 2) {
                        return I18N.get("ui.renderer.style.ColorThemingPanel.layer-must-have-at-least-1-attribute");
                    }
                    return null;
                }
             });
    }  
    

	public boolean execute(PlugInContext context) throws Exception {
		sourceLayer = context.getSelectedLayer(0);
		MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(),
				getName(), true);
		setDialogFields(dialog);

		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (dialog.wasOKPressed()) {
			String attrName = dialog.getText(LAYER_ATTRIBUTE);
			extractLayers(context, sourceLayer, attrName);
			return true;
		}
		return false;
	}
	
    private void setDialogFields( final MultiInputDialog dialog) {
		dialog.setSideBarDescription(DIALOGMSG);
		layerAttributeComboBox = dialog.addComboBox(LAYER_ATTRIBUTE, null, new ArrayList(), null);
		List names = attributeNames();
		layerAttributeComboBox.setModel(new DefaultComboBoxModel(new Vector(names)));
		String layerName = null;
		textAttributeFound = false;
        for (Iterator i = names.iterator(); i.hasNext();) {
            String attribute = (String) i.next();
            if (attribute.equalsIgnoreCase(LAYER)) {
            	layerName = attribute;
            } else if (attribute.equalsIgnoreCase(TEXT)) {
            	textAttributeFound = true;
            	textAttribute = attribute;
            }
        }
		layerAttributeComboBox.setSelectedItem(layerName);
		if (layerName == null && names.size() > 0) {
		    layerAttributeComboBox.setSelectedIndex(0);
		}
    }

    private List attributeNames() {
        ArrayList candidateAttributeNames = new ArrayList();
        FeatureSchema schema = sourceLayer.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0; i < schema.getAttributeCount(); i++) {
        	String name = schema.getAttributeName(i);
        	if (!name.equalsIgnoreCase("GEOMETRY"))
                candidateAttributeNames.add(name);
        }
        return candidateAttributeNames;
    }

    public String getName() {
		return EXTRACT_LAYERS_BY_ATTRIBUTE;
	}

    public static final ImageIcon ICON = IconLoader.icon("extract.gif");

    private void extractLayers(PlugInContext context, Layer layer, String attributeName) {
 		FeatureCollectionWrapper featureCollection = layer.getFeatureCollectionWrapper();
        List featureList = featureCollection.getFeatures();
        FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        int attributeIndex = featureSchema.getAttributeIndex(attributeName);        
       			          
		boolean wasFiringEvents = context.getLayerManager().isFiringEvents();
        
       	Set newLayerNameList = new HashSet();
        for (Iterator i = featureList.iterator(); i.hasNext();) {
        	Feature feature = (Feature) i.next();
			// modified by michaelm on 2009-02-20 to handle null and empty strings
        	Object attributeValue = feature.getAttribute(attributeIndex);
			if (attributeValue == null) {
				    newLayerNameList.add(NULL);
			}
			else {
				String attributeString = attributeValue.toString().trim();
				if (attributeString.length() == 0) {
				    newLayerNameList.add(EMPTY);
				}
        	    else {
        		    newLayerNameList.add(attributeString);
				}
        	}
        }
 
        for (Iterator i = newLayerNameList.iterator(); i.hasNext();) {
        	String layerName = (String) i.next();
        	
    		context.getLayerManager().setFiringEvents(true);
	        Layer newLayer = context.addLayer(EXTRACT, layerName,
	        	new FeatureDataset(featureSchema));
	        if (textAttributeFound) {
	            boolean textAttributePopulated = false;
		        for (Iterator j = featureList.iterator(); j.hasNext();) {
		        	Feature feature = (Feature) j.next();
		        	String attributeValue = feature.getAttribute(textAttribute).toString();
		        	if ( feature.getAttribute(attributeIndex).toString().equals(layerName) && attributeValue != null && !attributeValue.isEmpty()) {
		        		textAttributePopulated = true;
			        	break;
		        	}
	            }
	            if (textAttributePopulated) {
	        	    LabelStyle labelStyle = new LabelStyle();
	        	    labelStyle.setAttribute(textAttribute);
	        	    labelStyle.setScaling(true);
	        	    labelStyle.setEnabled(true);	        	
		            newLayer.addStyle(labelStyle);
		        }
	        }
			context.getLayerManager().setFiringEvents(false);
	        
	        FeatureCollectionWrapper newFeatureCollection = newLayer.getFeatureCollectionWrapper();
           
	        for (Iterator j = featureList.iterator(); j.hasNext();) {
	        	Feature feature = (Feature) j.next();
				// modified by michaelm on 2009-02-20 to handle null and empty strings
	        	Object attributeValue = feature.getAttribute(attributeIndex);
				if (attributeValue == null) {
					if (layerName.equals(NULL)) {
						newFeatureCollection.add((Feature) feature.clone());
					}
				}
				else {
					String attributeString = attributeValue.toString();
				    if (attributeString.trim().length() == 0) {
						if (layerName.equals(EMPTY)) {
							newFeatureCollection.add((Feature) feature.clone());
						}
					}
					else if (attributeString.trim().equals(layerName)) {
		        	    newFeatureCollection.add((Feature) feature.clone());
					}
	        	}
            }
        }
        
		context.getLayerManager().setFiringEvents(wasFiringEvents);
		context.getLayerViewPanel().repaint();
    }
	    
}
