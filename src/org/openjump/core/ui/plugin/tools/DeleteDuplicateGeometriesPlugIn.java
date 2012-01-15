/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */
/*****************************************************
 * created:         30.01.2006
 * last modified:   15.01.2012                    
 *                  
 * 
 * @author sstein, mmichaud
 * 
 * description: deletes items with same geometry
 *  
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Eliminates features that have exactly the same Geometry
 *
 * @author sstein
 *
 * TODO : use STRtree for faster testing. Right now I just go through the complete list.
 **/
public class DeleteDuplicateGeometriesPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

    private String CLAYER = "select layer";    
    private String sDescription = "deletes features with similar geometry";
    private String deleteByAttribute = "delete only if attributes are the same";
    private String sName = "Delete Duplicate Geometries";
    private String sChecked = "checked";
    private String sCleaned = "cleaned";
    private Layer itemlayer = null;
    private boolean deleteOnlyForSameAttributes = false;
      
    public void initialize(PlugInContext context) throws Exception {
        
        this.CLAYER = GenericNames.SELECT_LAYER;    
        this.sDescription = I18N.get("org.openjump.core.ui.plugin.tools.DeleteDuplicateGeometriesPlugIn.deletes-features-with-similar-geometry");
        this.deleteByAttribute = I18N.get("org.openjump.core.ui.plugin.tools.DeleteDuplicateGeometriesPlugIn.delete-only-if-attributes-are-the-same");
        this.sName = I18N.get("org.openjump.core.ui.plugin.tools.DeleteDuplicateGeometriesPlugIn.Delete-Duplicate-Geometries");
        this.sChecked = I18N.get("org.openjump.core.ui.plugin.tools.DeleteDuplicateGeometriesPlugIn.checked");
        this.sCleaned = I18N.get("org.openjump.core.ui.plugin.tools.DeleteDuplicateGeometriesPlugIn.cleaned");
        /*//-- no need to be in mouse menu
        context.getFeatureInstaller().addPopupMenuItem(
                LayerViewPanel.popupMenu(),
                this,
                this.getName(), 
                false, 
                null,
                createEnableCheck(context.getWorkbenchContext()));
        */
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuItem(this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_QA},
                new JMenuItem(this.getName() + "..."), 
                //false,
                //null,
                createEnableCheck(context.getWorkbenchContext()));
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(
            checkFactory.createAtLeastNLayersMustExistCheck(1)
        );
    }
    
    public String getName(){
        return sName;
    }
    
    public boolean execute(PlugInContext context) throws Exception{
        this.reportNothingToUndoYet(context);
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), getName(), true);
            setDialogValues(dialog, context);
            GUIUtil.centreOnWindow(dialog);
            dialog.setVisible(true);
            if (! dialog.wasOKPressed()) { return false; }
            getDialogValues(dialog);
            return true;
    }
    
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        dialog.setSideBarDescription(sDescription);
        JComboBox addLayerComboBoxBuild = dialog.addLayerComboBox(this.CLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
        dialog.addCheckBox(deleteByAttribute, this.deleteOnlyForSameAttributes);
    }

    private void getDialogValues(MultiInputDialog dialog) {
    this.itemlayer = dialog.getLayer(this.CLAYER);
    this.deleteOnlyForSameAttributes = dialog.getBoolean(deleteByAttribute);
      }
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{                 
        this.delete(context, monitor);
        System.gc();            
    }
    
    private boolean delete(PlugInContext context, TaskMonitor monitor) throws Exception{
        // Method is completely reworked to take advantage of indexes [mmichaud 2012-01-15]
        FeatureCollection fc = itemlayer.getFeatureCollectionWrapper();
    	FeatureSchema schema = fc.getFeatureSchema();
    	int geomIndex = schema.getGeometryIndex();
    	// Input data is indexed
    	IndexedFeatureCollection index = new IndexedFeatureCollection(fc);
    	Set<Integer> duplicates = new HashSet<Integer>();
    	Iterator it = fc.iterator();
    	while (it.hasNext()) {
    		Feature f = (Feature)it.next();
    		// For each feature, only candidate features are compared
    		List candidates = index.query(f.getGeometry().getEnvelopeInternal());
    		for (Object o : candidates) {
    			Feature c = (Feature)o;
    			// For equal features, the one with the greater ID is removed
    			if (c.getID() > f.getID() && f.getGeometry().equalsNorm(c.getGeometry())) {
    				if (deleteOnlyForSameAttributes) {
    					boolean attributesEqual = true;
    					for (int k=0; k < schema.getAttributeCount(); k++){
                            if(k!=geomIndex){
                                Object att1 = f.getAttribute(k);
                                Object att2 = c.getAttribute(k);
                                if (att1 == null && att2 == null) {
                                    continue;
                                }
                                else if (att1 == null || att2 == null ||
                                    !f.getAttribute(k).equals(c.getAttribute(k))) {
                                    attributesEqual = false;
                            	    break;
                                }
                            }
                        }
                        // if geometry and attributes are equals, add ID to duplicates
    					if (attributesEqual) duplicates.add(c.getID());
    				}
    				// if geometry are equals, add ID to duplicates
    				else duplicates.add(c.getID());
    			}
    		}
    	}
    	// Create a layer with features which ID is not in duplicates
    	FeatureCollection noDuplicates = new FeatureDataset(schema);
    	it = fc.iterator();
    	while (it.hasNext()) {
    		Feature f = (Feature)it.next();
    		if (!duplicates.contains(f.getID())) {
    	        noDuplicates.add(f.clone(true));
    		}
    	}
    	context.addLayer(StandardCategoryNames.RESULT,  itemlayer.getName()+ "-" + sCleaned, noDuplicates);         
        return true;
    }
    
}
