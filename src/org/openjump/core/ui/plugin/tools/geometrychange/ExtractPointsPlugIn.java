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
 * created:  22.Oct.2008
 * last modified:
 * 
 * @author sstein
 *****************************************************/

package org.openjump.core.ui.plugin.tools.geometrychange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComboBox;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.sigle.utilities.geom.FeatureCollectionUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
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
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Extracts points from polygon or line features and writes them to a new layer
 *  
 * @author sstein
 *
 **/
public class ExtractPointsPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private String sName = "Extract Points";
    private String CLAYER = "select layer";
    private String DELETE_LAST_POINT_IF_CLOSED = "Account for closed Geometries";
    
    private String sideBarText = "Extracts points from polygon or line features and writes them to a new layer. " +
    		"Note, for closed geometries start point and end point are the same. If closed geometries are to be observed," +
    		"then the last point is not returned to avoid two overlaying points.";
    private String sPoints = "points";    
    
    private Layer itemlayer = null;
    private boolean deleteDoublePoints = false;
	private MultiInputDialog dialog;
	private JComboBox layerComboBoxBackground;
    
    public void initialize(PlugInContext context) throws Exception {
    		
    		this.CLAYER = GenericNames.LAYER;
       		this.sName = I18N.get("org.openjump.core.ui.plugin.tools.ExtractPointsPlugIn.Extract-Points");
    	    this.sideBarText = I18N.get("org.openjump.core.ui.plugin.tools.ExtractPointsPlugIn.description");
    		this.sPoints = I18N.get("org.openjump.core.ui.plugin.tools.ExtractPointsPlugIn.points");
    	    this.DELETE_LAST_POINT_IF_CLOSED = I18N.get("org.openjump.core.ui.plugin.tools.ExtractPointsPlugIn.Account-for-closed-Geometries");
    		
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, MenuNames.CONVERT}, 	//menu path
	                this.sName + "...", //name methode .getName recieved by AbstractPlugIn 
	                false,			//checkbox
	                null,			//icon
	                createEnableCheck(context.getWorkbenchContext())); //enable check
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
	public boolean execute(PlugInContext context) throws Exception{
	    this.reportNothingToUndoYet(context);
	        
 		dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), this.sName, true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);	    
	    return true;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
	    dialog.setSideBarDescription(this.sideBarText);	    
    	JComboBox addLayerComboBoxBuild = dialog.addLayerComboBox(this.CLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
    	dialog.addCheckBox(this.DELETE_LAST_POINT_IF_CLOSED, this.deleteDoublePoints);
	  }

	private void getDialogValues(MultiInputDialog dialog) {
    	this.itemlayer = dialog.getLayer(this.CLAYER);
    	this.deleteDoublePoints = dialog.getBoolean(this.DELETE_LAST_POINT_IF_CLOSED);
	  }
	
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            			    
    	 System.gc();
    	 final Collection features = this.itemlayer.getFeatureCollectionWrapper().getFeatures();    	 
    	 FeatureSchema fs = this.itemlayer.getFeatureCollectionWrapper().getFeatureSchema();
    	 //--
    	 final String ITEM_ID = "item_id";
    	 final String SEQ_ID = "sequence_id";    	 
    	 FeatureSchema fsNew = (FeatureSchema)fs.clone();
    	 fsNew.addAttribute(ITEM_ID, AttributeType.INTEGER);
    	 fsNew.addAttribute(SEQ_ID, AttributeType.INTEGER);
    	 //--
    	 FeatureDataset fd = new FeatureDataset(fsNew);
    	 for (Iterator iterator = features.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			ArrayList<Feature> points = FeatureCollectionTools.convertToPointFeature(f, this.deleteDoublePoints);
		    int seq_count=0;
			for (Iterator iterator2 = points.iterator(); iterator2.hasNext();) {
				Feature pt = (Feature) iterator2.next();
				pt = FeatureCollectionTools.copyFeatureAndSetFeatureSchema(pt, fsNew);
				pt.setAttribute(ITEM_ID, f.getID());
				pt.setAttribute(SEQ_ID, seq_count);
				fd.add(pt);
			    seq_count++;
			}			
		}
    	context.addLayer(StandardCategoryNames.RESULT, this.itemlayer.getName() + "-" + sPoints, fd);
    }
}
