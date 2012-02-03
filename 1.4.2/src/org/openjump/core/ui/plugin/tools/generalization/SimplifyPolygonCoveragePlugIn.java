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
 *****************************************************/

package org.openjump.core.ui.plugin.tools.generalization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;

import org.openjump.core.geomutils.algorithm.IntersectGeometries;
import org.openjump.core.graph.polygongraph.PolygonGraph;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
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
 * Extracts the boundaries of a polygon layer, simplifies them, and then 
 * uses the polygonizer to create polygons again.
 *	
 * @author sstein
 *
 **/
public class SimplifyPolygonCoveragePlugIn extends AbstractPlugIn implements ThreadedPlugIn{

	private String sName = "Simplify Polygon Coverage";
    private String sSidebar ="Simplifies the outlines of polygons that have adjacent polygons."; 
	private String note = "Note, if the simplification destroys the topology, then try to simplify iteratively.";
    private String sCreateGraph = "create graph";
    private String sSimplify = "simplify";  
    private String LAYERREGIONS = "select layer with polygons";
    private static String T3="Maximum point displacement in model units";
    private String sSimplificationFinalized="simplification finalized";
    private String sPolygonize="Polygonization";
    
    private FeatureCollection regions = null;        
    private Layer input = null;
    private MultiInputDialog dialog;
    private double tolerance = 0;
        
    public void initialize(PlugInContext context) throws Exception {
    	
    		this.sName = I18N.get("org.openjump.core.ui.plugin.tools.SimplifyPolygonCoveragePlugIn.Simplify-Polygon-Coverage");
    		this.note = I18N.get("org.openjump.core.ui.plugin.tools.SimplifyPolygonCoveragePlugIn.note");
    		this.sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.SimplifyPolygonCoveragePlugIn.Simplifies-the-outlines-of-polygons-that-have-adjacent-polygons");
	        this.sCreateGraph = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.create-graph");	        
	        this.LAYERREGIONS = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.select-layer-with-polygons");
    		this.sSimplify = I18N.get("ui.plugin.analysis.GeometryFunction.Simplify-(D-P)");
    	    this.T3=I18N.get("org.openjump.core.ui.plugin.tools.LineSimplifyJTS15AlgorithmPlugIn.Maximum-point-displacement-in-model-units");    
    	    this.sSimplificationFinalized=I18N.get("org.openjump.core.ui.plugin.tools.LineSimplifyJTS15AlgorithmPlugIn.simplification-finalized");
    	    this.sPolygonize=I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonization");
    	    
    	    this.sSidebar = this.sSidebar + " " + this.note;
    	    	
    		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERALIZATION}, 	//menu path
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
        	this.tolerance = this.dialog.getDouble(T3);
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
	    		//-- extract the unique boundaries
		    	monitor.report(sCreateGraph);
		    	PolygonGraph pg = new PolygonGraph(features, monitor);
		    	FeatureCollection boundaries = pg.getSharedBoundaries();
		    	boundaries.addAll(pg.getNonSharedBoundaries().getFeatures());
		    	if (monitor.isCancelRequested()){
		    		return;
		    	}
	    		//-- simplify the unique boundaries
		    	monitor.report(sSimplify);
		    	int count = 0; int noItems = boundaries.size();
		    	for (Iterator iterator = boundaries.iterator(); iterator.hasNext();) {
		    		count++;
					Feature edge = (Feature) iterator.next();
					Geometry resultgeom = TopologyPreservingSimplifier.simplify(edge.getGeometry(), Math.abs(tolerance));
					edge.setGeometry(resultgeom);
				    String mytext =  count + " / " + noItems + " : " + sSimplificationFinalized;
				    monitor.report(mytext);
			    	if (monitor.isCancelRequested()){
			    		return;
			    	}
				}
		    	//-- create polygons	  
		    	monitor.report(sPolygonize);
				//-- calculate the intersections and use the Polygonizer
		    	Collection<Geometry> lines = new ArrayList<Geometry>();
		    	for (Iterator iterator = boundaries.iterator(); iterator.hasNext();) {
					Feature edge = (Feature) iterator.next();
		    		lines.add(edge.getGeometry());
		    	}
				Collection<Geometry>  nodedLines = IntersectGeometries.nodeLines(lines);
			    Polygonizer polygonizer = new Polygonizer();
			    for (Iterator i = nodedLines.iterator(); i.hasNext(); ) {
			        Geometry g = (Geometry) i.next();
			        polygonizer.add(g);
			    	if (monitor.isCancelRequested()){
			    		return;
			    	}
			      }
			    //-- get the Polygons
				Collection<Geometry> withoutIntersection = polygonizer.getPolygons();
		    	//-- transfer Attributes
				FeatureCollection resultD = this.transferAttributesFromPolysToPolys(this.regions, withoutIntersection, context, monitor);
		    	context.addLayer(StandardCategoryNames.RESULT, this.input + "-" + sSimplify, resultD);
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
	    dialog.addDoubleField(T3,1.0,5);
        GUIUtil.centreOnWindow(dialog);
    }	

	public FeatureCollection transferAttributesFromPolysToPolys(FeatureCollection fcA, Collection<Geometry> geometries, PlugInContext context, TaskMonitor monitor){
		//-- check if the polygon has a correspondent 
		//	 if yes, transfer the attributes - if no: remove the polygon
		
		//-- build a tree for the existing layers first.
		SpatialIndex treeA = new STRtree();
		for (Iterator iterator = fcA.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			treeA.insert(f.getGeometry().getEnvelopeInternal(), f);
		}
		// -- get all intersecting features (usually there should be only one
		// corresponding feature per layer)
		// to avoid problems with spatial predicates we do the query for an
		// internal point of the result polygons
		// and apply an point in polygon test
		AttributeMapping mapping = new AttributeMapping(fcA.getFeatureSchema(),
				new FeatureSchema());
		// -- create the empty dataset with the final FeatureSchema
		FeatureDataset fd = new FeatureDataset(mapping.createSchema("Geometry"));
		// -- add the features and do the attribute mapping
		for (Iterator iterator = geometries.iterator(); iterator
				.hasNext();) {
			Geometry geom = (Geometry) iterator.next();
			Point pt = geom.getInteriorPoint();
			Feature f = new BasicFeature(fd.getFeatureSchema());
			Feature featureA = null;
			Feature featureB = null;
			// -- query Layer A ---
			List candidatesA = treeA.query(pt.getEnvelopeInternal());
			int foundCountA = 0;
			for (Iterator iterator2 = candidatesA.iterator(); iterator2.hasNext();){
				Feature ftemp = (Feature) iterator2.next();
				if (ftemp.getGeometry().contains(pt)) {
					foundCountA++;
					featureA = ftemp;
				}
			}
			if (foundCountA > 1) {
				if (context != null) {
					context.getWorkbenchFrame().warnUser(
							I18N.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Found-more-than-one-source-feature-in-Layer")
							+ " " + GenericNames.LAYER_A);
				}
			} else if (foundCountA == 0) {
				if (context != null) {
					// context.getWorkbenchFrame().warnUser("no corresponding
					// feature in Layer A");
				}
			}
			if (foundCountA > 0){ 
				// -- do mapping
				mapping.transferAttributes(featureA, featureB, f);
				// -- set Geometry
				f.setGeometry((Geometry) geom.clone());
				fd.add(f);
			}
//			else{
//				System.out.println("polygon without correspondent"); 
//			}
	    	if (monitor != null){
	    		if (monitor.isCancelRequested()){
	    			return fd;
	    		}
	    	}
		}
		return fd;
	}
}
