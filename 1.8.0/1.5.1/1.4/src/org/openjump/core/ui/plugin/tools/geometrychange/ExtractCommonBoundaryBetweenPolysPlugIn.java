/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
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
 * created:  		10.July.2008
 * last modified:   					
 * 					
 * 
 * @author sstein
 * 
 * description:
 * 	TODO
 *  
 *****************************************************/

package org.openjump.core.ui.plugin.tools.geometrychange;

import java.util.Collection;

import javax.swing.JComboBox;

import org.openjump.core.graph.polygongraph.PolygonGraph;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
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
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * @description: Extracts the boundaries of a polygon layer distinguishing 
 * between shared and non-shared boundaries.
 *	
 * @author sstein
 *
 **/
public class ExtractCommonBoundaryBetweenPolysPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

	private String sName = "Extract Common Boundary Between Polygons";
    private String sSidebar ="Classifies the boundaries of a polygon by using a neighbourhood graph.";  
    private String sCreateGraph = "create graph";
    private String sBoundaries = "boundaries";   
    private String LAYERREGIONS = "select layer with polygons";
    private FeatureCollection regions = null;        
    private Layer input = null;
    private MultiInputDialog dialog;
        
    public void initialize(PlugInContext context) throws Exception {
    	
    		this.sName = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.Extract-Common-Boundary-Between-Polygons");
	        this.sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.Classifies-the-boundaries-of-a-polygon-by-using-a-neighbourhood-graph");
	        this.sCreateGraph = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.create-graph");
    		this.sBoundaries = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.boundaries");	        
	        this.LAYERREGIONS = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.select-layer-with-polygons");
    		
    		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, MenuNames.CONVERT}, 	//menu path
	                this.sName + "...",
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
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes.
        initDialog(context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        else{
        	this.input =  dialog.getLayer(this.LAYERREGIONS);
        	this.regions = this.input.getFeatureCollectionWrapper();        	
        }
        return true;	    
	}
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            		
	    	System.gc(); //flush garbage collector
	    	monitor.allowCancellationRequests();
		    //final Collection features = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
	    	Collection<Feature> features = this.regions.getFeatures();
	    	Feature firstFeature = (Feature)features.iterator().next();
	    	if (firstFeature.getGeometry() instanceof Polygon){
		    	monitor.report(sCreateGraph);
		    	PolygonGraph pg = new PolygonGraph(features, monitor);
		    	//pg.getCommonBoundaries(pg.nodes.get(0));
		    	//FeatureCollection resultD = pg.getBoundaries(pg.nodes.get(0));
		    	//-- old sorted return of boundaries
		    	//FeatureCollection resultD = pg.getSharedBoundaries();
		        //context.addLayer(StandardCategoryNames.RESULT, "sharedBoundaries", resultD);
		    	//FeatureCollection resultE = pg.getNonSharedBoundaries();
		        //context.addLayer(StandardCategoryNames.RESULT, "nonSharedBoundaries", resultE);	
		    	//-- new return of boundaries, distinction by attribute value for PolygonGraphNode.edgeTypeAtributeName
		    	FeatureCollection resultD = pg.getSharedBoundaries();
		    	resultD.addAll(pg.getNonSharedBoundaries().getFeatures());
		    	context.addLayer(StandardCategoryNames.RESULT, this.input + "-" + sBoundaries, resultD);
	    	}
	    	else{
	    		context.getWorkbenchFrame().warnUser("no (simple) polygon geometries found");
	    	}
    	}

	private void initDialog(PlugInContext context) {
    	
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.sName, true);
        dialog.setSideBarDescription(sSidebar);
        try {
        	JComboBox addLayerComboBoxRegions = dialog.addLayerComboBox(this.LAYERREGIONS, context.getCandidateLayer(0), null, context.getLayerManager());
        }
        catch (IndexOutOfBoundsException e) {
        	//eat it
        }
        GUIUtil.centreOnWindow(dialog);
    }	

}
