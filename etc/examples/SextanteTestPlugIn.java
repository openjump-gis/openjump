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
 * created:  		20.Oct.2009
 * last modified:   					
 * 					
 * 
 * @author sstein
 * 
 * description: 
 * OpenJUMP plugin to demonstrate the utilization and access to Sextante algorithms 
 * 	
 *  
 *****************************************************/

package ca.ucalgary.engg.moveantools.ojplugin;

import java.io.IOException;

import javax.swing.JComboBox;

import com.vividsolutions.jts.geom.GeometryFactory;
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
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.openjump.core.OpenJUMPOutputFactory;
import es.unex.sextante.openjump.core.OpenJUMPVectorLayer;
import es.unex.sextante.outputs.Output;
import es.unex.sextante.parameters.Parameter;
import es.unex.sextante.vectorTools.minimumEnclosingPolygon.MinimumEnclosingPolygonAlgorithm;

/**
 * @description: OpenJUMP plugin to demonstrate the utilization and access to Sextante algorithms
 *	
 * @author sstein
 *
 **/
public class SextanteTestPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private String sSidebar ="Testplugin that uses an algorithm that generates a convex hull (i.e. minimum enclosing polygon) from Sextante";   
    private final String sLAYERPTS = "Layer with Point Geometries";    
    private Layer input = null;
    private MultiInputDialog dialog;
    private PlugInContext context = null;
    
    GeometryFactory gfactory = new GeometryFactory();
        
    public void initialize(PlugInContext context) throws Exception {
    				
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {"OJ-Sextante"}, 	//menu path
	                "Sextante Test Plugin", 
	                //AbstractPlugIn.createName(CalculateMinimumConvexPolygonPlugIn.class),
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
        	this.getDialogValues(dialog); 
        }
        return true;	    
	}
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            		
	    	System.gc(); //flush garbage collector
	    	this.context = context;
	    	monitor.allowCancellationRequests();
	    	FeatureCollection resultC = this.calculateHull(this.input, context, monitor);
	        if(resultC != null){
	        	context.addLayer(StandardCategoryNames.RESULT, this.input.getName() + "-hull", resultC);
	        }
    	    System.gc();    		
    	}

	private void initDialog(PlugInContext context) {
    	
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), "Generate Hull", true);
        dialog.setSideBarDescription(sSidebar);
        try {
        	JComboBox addLayerComboBoxRegions = dialog.addLayerComboBox(this.sLAYERPTS, context.getCandidateLayer(0), null, context.getLayerManager());
        }
        catch (IndexOutOfBoundsException e) {}
        GUIUtil.centreOnWindow(dialog);
    }
    
    private void getDialogValues(MultiInputDialog dialog) {
    	this.input =  dialog.getLayer(this.sLAYERPTS); 
      }
    
	private FeatureCollection calculateHull(Layer stefanspoints,
			PlugInContext context, TaskMonitor monitor) 
			throws GeoAlgorithmExecutionException, IOException {
		
		OutputFactory outputFactory = new OpenJUMPOutputFactory(context.getWorkbenchContext());
		
		monitor.report("initialize sextante");
		Sextante.initialize();
		
		monitor.report("prepare computation");
		OpenJUMPVectorLayer layer = new OpenJUMPVectorLayer();
		layer.create(stefanspoints);
		MinimumEnclosingPolygonAlgorithm alg = new MinimumEnclosingPolygonAlgorithm();
		
		ParametersSet params = alg.getParameters();
		Parameter layerParam = (Parameter)params.getParameter(MinimumEnclosingPolygonAlgorithm.POINTS);
		boolean worked = layerParam.setParameterValue(layer);
		if(worked){
			//method values: 0 - for convex hull, 1 - for circle, 2 - envelope
			params.getParameter(MinimumEnclosingPolygonAlgorithm.METHOD).setParameterValue(new Integer(2));
			params.getParameter(MinimumEnclosingPolygonAlgorithm.USECLASSES).setParameterValue(false);
			//-- not sure why the following param needs to be set, if I set false before
			params.getParameter(MinimumEnclosingPolygonAlgorithm.FIELD).setParameterValue(new Integer(0));
			
			OutputObjectsSet outputs = alg.getOutputObjects();
			Output polygon = outputs.getOutput(MinimumEnclosingPolygonAlgorithm.RESULT);
			monitor.report("computation");
			alg.execute(null, outputFactory);
			
			monitor.report("retrieving results");
			IVectorLayer result = (IVectorLayer)polygon.getOutputObject();
			Layer resultOJLayer = (Layer)result.getBaseDataObject();
			return resultOJLayer.getFeatureCollectionWrapper().getWrappee();
		}
		else{
			context.getWorkbenchFrame().warnUser("layer not a point layer; has ShapeType: " + layer.getShapeType());
			return null;
		}
	}
  
}
