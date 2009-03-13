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
 * created:  		30.01.2006
 * last modified:   					
 * 					
 * 
 * @author sstein
 * 
 * description:
 * 	deletes items with same geometry
 *  
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComboBox;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
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
 * @description:
 *		eliminates features that have exactly the same Geometry
 *
 * @author sstein
 *
 * TODO: use STRtree for faster testing. Right now I just go through the complete list.
 **/
public class DeleteDuplicateGeometriesPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

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
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_QA}, 	//menu path
	                this.getName() + "...", //name methode .getName received by AbstractPlugIn 
	                false,			//checkbox
	                null,			//icon
	                createEnableCheck(context.getWorkbenchContext())); //enable check
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
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
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
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
	    System.gc(); //flush garbage collector
	    // --------------------------	    
	    //-- get selected items
	    final Collection features = this.itemlayer.getFeatureCollectionWrapper().getFeatures();
	    ArrayList notFoundItemsOld = new ArrayList();
	    ArrayList notFoundItemsNew = new ArrayList();	    
	    ArrayList doneItems = new ArrayList();	    
	    int size = features.size();
	    Iterator iter = features.iterator();
	    Feature f = (Feature)iter.next();
	    FeatureSchema fs = f.getSchema();
	    //-- new collection for output : assuming that all features have same schema and 
	    //   first feature has correct scheme
  	FeatureCollection myCollA = new FeatureDataset(fs);    	        	    
	    Feature fi = null;
	    int count=1; int dropped = 0;
	    notFoundItemsOld.addAll(features);
	    while(iter.hasNext()){
	    	monitor.report(count, size, sChecked);
	    	count++;
	    	fi = (Feature)iter.next();
 	    	Geometry geom = (Geometry)fi.getGeometry();
 	    	int copycounter = 0;
 	    	for (int j = 0; j < notFoundItemsOld.size(); j++){
 	    		boolean isequal = false;  	    		
 	    		Feature testF = (Feature)notFoundItemsOld.get(j);
 	    		try{
 	   	    		//-- test if other object has same geometry
 	    		    isequal = geom.equals(testF.getGeometry());
 	    		    //-- test if objects have same attribute values
 	    		    if((isequal == true) && (this.deleteOnlyForSameAttributes == true)){
 	    		    	boolean attributesAreDifferent = false;
 	    		    	for (int k=0; k < fs.getAttributeCount(); k++){
 	    		    		if(fs.getAttributeType(k) != AttributeType.GEOMETRY){
 	    		    			Object val1 = fi.getAttribute(k); 
 	    		    			Object val2 = testF.getAttribute(k);
 	    		    			if(!val1.equals(val2)){   	    		    				
 	    		    				attributesAreDifferent = true;
 	    		    				//System.out.println("Attribute vals are different: " + val1 + " and " + val2);
 	    		    			}
 	    		    		}
 	    		    	}
 	    		    	if (attributesAreDifferent == true){
 	    		    		isequal = false;
 	    		    	}
 	    		    }
 	    		}
   	    		catch(Exception e){
   	    		    System.out.println("items have problem (toplogy): " + testF.getID() + " and " + fi.getID());
   	    		    System.out.println(e.getStackTrace());
   	    		}
   	    		if (isequal){
   	    			copycounter = copycounter+1;
   	    			if (copycounter == 1){
   	    				//-- hold at least one 
   	    				// checking of Feature-Id's does not help - since every item is checked,
   	    				// even if it has been deleted already from the second notFoundList, since we are
						// walking through the original features list - still containing all items
   	    				notFoundItemsNew.add(testF);
   	    			}
   	    			else{
	   	    			//-- drop feature
	   	    			dropped= dropped+1;
   	    			}
   	    		}
   	    		else{ //has same id or was not found
   	    			notFoundItemsNew.add(testF);
   	    		}
   	    	} 
   	    	notFoundItemsOld.clear();
   	    	notFoundItemsOld.addAll(notFoundItemsNew);
   	    	notFoundItemsNew.clear();
	    }
	    for (Iterator iterator = notFoundItemsOld.iterator(); iterator.hasNext();) {
	    	Feature element = (Feature) iterator.next();
	    	//-- clone! not link
			myCollA.add((Feature)element.clone());
			
		}
		context.addLayer(StandardCategoryNames.WORKING,  itemlayer.getName()+ "-" + sCleaned, myCollA);	    	
		return true;        
	}	  	
}
