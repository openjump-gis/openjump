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
 * created:  		19.05.2005
 * last modified:  	21.05.2005 (copy only one item in new layer)
 * 					18.11.2005 do deep copy (clone)					
 * 
 * @author sstein
 * 
 * description:
 * 	replicates/ copys selected items in a new or existing layer
 *  
 *****************************************************/

package org.openjump.core.ui.plugin.edit;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

import java.util.Iterator;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;

import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import org.openjump.core.ui.images.IconLoader;


/**
 * Replicates/ copys selected items in a new or existing layer
 *
 * @author sstein
 *
 **/
public class ReplicateSelectedItemsPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

	public static ImageIcon ICON = IconLoader.icon("shape_replicate.png");	
    private String T1 ="Replicate to new layer?";
    private String CLAYER = "otherwise select layer";    
    boolean newLayer = true;
    private Layer itemlayer = null;
    private boolean copyAsGeometry = false;


    public void initialize(PlugInContext context) throws Exception {
    	
		    context.getFeatureInstaller().addPopupMenuItem(
		    		LayerViewPanel.popupMenu(),
					this,
					I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.replicate-selected-items"),
					false, 
					this.getIcon(),
                    createEnableCheck(context.getWorkbenchContext()));

		    context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
		        new String[]
				{MenuNames.EDIT},
				I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.replicate-selected-items")+"{pos:16}", 
				false, 
				this.getIcon(), 
				createEnableCheck(context.getWorkbenchContext()));
		    
		this.T1 = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.replicate-to-new-layer");		
		this.CLAYER = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.otherwise-select-target-layer");
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
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
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
    	String sidebarString = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.replicates-selected-items-if-all-have-same-attribute-schema");
	    dialog.setSideBarDescription(sidebarString);
	        //"Replicates selected items if all have same feature schema"
	    dialog.addCheckBox(T1,true);
    	JComboBox addLayerComboBoxBuild = dialog.addLayerComboBox(this.CLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
	  }

	private void getDialogValues(MultiInputDialog dialog) {
	    this.newLayer = dialog.getBoolean(T1);
    	this.itemlayer = dialog.getLayer(this.CLAYER);
	  }
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            		
    	    this.replicate(context, monitor, this.newLayer);
    	    System.gc();    		
    	}
	
	private boolean replicate(PlugInContext context, TaskMonitor monitor, boolean newLayer) throws Exception{
		String statusMessage1 = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.item");
		String statusMessage2 = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.no-replication-because-different-attribute-schema");
		
	    boolean hasBeenCalled = false;
	    System.gc(); //flush garbage collector
	    // --------------------------	    
	    //-- get selected items
	    final Collection features = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();

	    if (newLayer == false){	    	
	    	EditTransaction transaction = new EditTransaction(features, this.getName(), this.itemlayer,
						this.isRollingBackInvalidEdits(context), true, context.getWorkbenchFrame());
	    	//Collection actualLayerFeatures = this.itemlayer.getFeatureCollectionWrapper().getFeatures();
	    	FeatureCollection actualLayerFeatures = this.itemlayer.getFeatureCollectionWrapper().getWrappee();
	    	FeatureSchema fschema = this.itemlayer.getFeatureCollectionWrapper().getFeatureSchema();
	    	//-- check if schema is the same if yes add the feature (or change Schema)
	    	Iterator iter = features.iterator();
	    	int i=0; Feature fi = null;
	    	while (iter.hasNext()){
	    		i++;
	    		fi = (Feature)iter.next();
	    	    if(fschema.equals(fi.getSchema())){ 
					actualLayerFeatures.add((Feature)fi.clone());
	    		}
	    	    else{
	    	    	context.getWorkbenchFrame().setStatusMessage(
	    	    			statusMessage1 + ": " + i + " "+ statusMessage2);
	    	    	if(hasBeenCalled == false){
	    	    		this.askWhatToDo(context);
	    	    		hasBeenCalled = true;
	    	    	}
	    	    	if (this.copyAsGeometry == true){
	    	    		Geometry geom = (Geometry)fi.getGeometry().clone();
	    	    		Feature newFeature = FeatureUtil.toFeature(geom, fschema);
	    	    		actualLayerFeatures.add(newFeature);
	    	    	}
	    	    }
	    	}	    	
	    	transaction.commit();
	    }
	    else{
	    	Iterator iter = features.iterator();
	    	Feature f = (Feature)iter.next();
    	    FeatureCollection myCollA = new FeatureDataset(f.getSchema());
    	    myCollA.add((Feature)f.clone()); //copy first Item
	    	Feature fi = null;
	    	int i=1;
	    	while (iter.hasNext()){
	    		i++;
	    		fi = (Feature)iter.next();
	    	    if(f.getSchema().equals(fi.getSchema())){ 
					myCollA.add((Feature)fi.clone());
	    		}
	    	    else{
	    	    	context.getWorkbenchFrame().setStatusMessage(
	    	    			statusMessage1 + ": " + i + statusMessage2);
	    	    	if(hasBeenCalled == false){
	    	    		this.askWhatToDo(context);
	    	    		hasBeenCalled = true;
	    	    	}
	    	    	if (this.copyAsGeometry == true){
	    	    		Geometry geom = (Geometry)fi.getGeometry().clone();
	    	    		Feature newFeature = FeatureUtil.toFeature(geom, f.getSchema());
	    	    		myCollA.add(newFeature);
	    	    	}	    	    	
	    	    }
	    	}
		    context.addLayer(StandardCategoryNames.WORKING, 
		    		I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.new"),
					myCollA);	    	
	    }
        return true;        
	}
	  
	private void askWhatToDo(PlugInContext context){
		String dialog1 = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.attribute-schemas-are-different");
		String dialog2 = I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.copy-only-geometry");
		String dialog3 = "(" + I18N.get("org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn.on-ok-attributes-will-be-lost-on-cancel-items-are-not-copied") + ")";

		MultiInputDialog dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), getName(), true);
		dialog.addLabel(dialog1);
		dialog.addLabel(dialog2);
		dialog.addLabel(dialog3);
	    GUIUtil.centreOnWindow(dialog);
	    dialog.setVisible(true);
	    if (! dialog.wasOKPressed()) { 
	        this.copyAsGeometry=false; 
	    }
	    else{
	    	this.copyAsGeometry=true;
	    }

	}
	
    public ImageIcon getIcon() {
        return ICON;
    }
}
