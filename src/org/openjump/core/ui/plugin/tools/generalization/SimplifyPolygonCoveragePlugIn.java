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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.IntersectionMatrix;
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
public class SimplifyPolygonCoveragePlugIn extends AbstractPlugIn implements ThreadedPlugIn {

	//private String sName = "Simplify Polygon Coverage";
    private String sSidebar ="Simplifies the outlines of polygons that have adjacent polygons."; 
	private String note = "Note, if the simplification destroys the topology, then try to simplify iteratively.";
    private String sCreateGraph = "create graph";
    private String sSimplify = "simplify";  
    private String LAYERREGIONS = "select layer with polygons";
    private static String T3="Maximum point displacement in model units";
    private String sSimplificationFinalized="simplification finalized";
    private String sPolygonize="Polygonization";
    private String sLayerMustBePolygonal="Layer must be a polygonal coverage";
    private String sAttributeTransferNotExhaustive="Attribute transfer is not exhaustive";
    
    private FeatureCollection regions = null;        
    private Layer input = null;
    private MultiInputDialog dialog;
    private double tolerance = 1.0;
        
    public void initialize(PlugInContext context) throws Exception {
    
    		//this.sName = I18N.get("org.openjump.core.ui.plugin.tools.SimplifyPolygonCoveragePlugIn.Simplify-Polygon-Coverage");
    		this.note = I18N.get("org.openjump.core.ui.plugin.tools.generalization.SimplifyPolygonCoveragePlugIn.note");
    		this.sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.generalization.SimplifyPolygonCoveragePlugIn.Simplifies-the-outlines-of-polygons-that-have-adjacent-polygons");
	        this.sCreateGraph = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.create-graph");	        
	        this.LAYERREGIONS = I18N.get("org.openjump.core.ui.plugin.tools.ExtractCommonBoundaryBetweenPolysPlugIn.select-layer-with-polygons");
    		this.sSimplify = I18N.get("ui.plugin.analysis.GeometryFunction.Simplify-(D-P)");
    	    this.T3=I18N.get("org.openjump.core.ui.plugin.tools.LineSimplifyJTS15AlgorithmPlugIn.Maximum-point-displacement-in-model-units");    
    	    this.sSimplificationFinalized=I18N.get("org.openjump.core.ui.plugin.tools.LineSimplifyJTS15AlgorithmPlugIn.simplification-finalized");
    	    this.sPolygonize=I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonization");
    	    this.sLayerMustBePolygonal = I18N.get("org.openjump.core.ui.plugin.tools.generalization.SimplifyPolygonCoveragePlugIn.Layer-Must-Be-Polygonal");
    	    this.sAttributeTransferNotExhaustive = I18N.get("org.openjump.core.ui.plugin.tools.generalization.SimplifyPolygonCoveragePlugIn.Attribute-Transfer-Not-Exhaustive");
    	    
    	    this.sSidebar = this.sSidebar + "\n" + this.note;
    	    	
    		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuPlugin(
	    	        this,
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERALIZATION},
	                this.getName() + "...",
	                false,			//checkbox
	                null,			//icon
	                createEnableCheck(context.getWorkbenchContext()));
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
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
	    	System.gc(); //flush garbage collector
	    	monitor.allowCancellationRequests();
	    	
	    	Collection<Feature> features = this.regions.getFeatures();
	    	Feature firstFeature = (Feature)features.iterator().next();
	    	if (firstFeature.getGeometry().getDimension() == 2){
	    		//-- extract the unique boundaries
		    	monitor.report(sCreateGraph);
		    	PolygonGraph pg = new PolygonGraph(features, monitor);
		    	FeatureCollection boundaries = pg.getSharedBoundaries();
		    	boundaries.addAll(pg.getNonSharedBoundaries().getFeatures());
		    	
		    	SpatialIndex index = new STRtree();
		    	for (Iterator iterator = boundaries.iterator(); iterator.hasNext();) {
		    	    Geometry geom = ((Feature)iterator.next()).getGeometry();
		    	    index.insert(geom.getEnvelopeInternal(), geom);
		    	}
		    	
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
					
					// [mmichaud 2013-03-16] add a test to check if the edge can 
					// be safely simplified.
					// If not we roll back to original edge geometry
					Envelope env = edge.getGeometry().getEnvelopeInternal();
					env.expandBy(Math.abs(tolerance));
					List neighbours = index.query(env);
					boolean simplify = true;
					for (Object object : neighbours) {
					    if (object == edge.getGeometry()) continue;
					    Geometry neighbour = TopologyPreservingSimplifier.simplify((Geometry)object, Math.abs(tolerance));
					    IntersectionMatrix im = resultgeom.relate(neighbour);
					    if (im.matches("0********") || im.matches("1********")) {
					        simplify = false;
					        break;
					    }
					}
					if (simplify == false) {
					    resultgeom = edge.getGeometry();
					}
					// end
					
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
	    		context.getWorkbenchFrame().warnUser(sLayerMustBePolygonal);
	    	}
    	}

	private void initDialog(PlugInContext context) {
    	
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.getName(), true);
        dialog.setSideBarDescription(sSidebar);
        try {
        	JComboBox addLayerComboBoxRegions = dialog.addLayerComboBox(this.LAYERREGIONS, context.getCandidateLayer(0), null, context.getLayerManager());
        }
        catch (IndexOutOfBoundsException e) {
        	//eat it
        }
	    dialog.addDoubleField(T3, tolerance, 5);
        GUIUtil.centreOnWindow(dialog);
    }	

	public FeatureCollection transferAttributesFromPolysToPolys(FeatureCollection fcA, 
	            Collection<Geometry> geometries, PlugInContext context, TaskMonitor monitor){
		
		// [2013-03-16 mmichaud and tmichaud] algorithm is changed to match
		// source geometries to simplified geometries based on the number of
		// common coordinates 
		// Old algorithm tried to match simplified geometries to source
		// geometries based on an intersection between polygon and interior 
		// point, but it was not reliable as interior points are often out of
		// the source polygon 
		SpatialIndex indexB = new STRtree();
		for (Iterator iterator = geometries.iterator(); iterator.hasNext();) {
		    Geometry geom = (Geometry) iterator.next();
		    indexB.insert(geom.getEnvelopeInternal(), geom);
		}
		AttributeMapping mapping = new AttributeMapping(fcA.getFeatureSchema(), new FeatureSchema());
		// -- create the empty dataset with the final FeatureSchema
		FeatureDataset fd = new FeatureDataset(mapping.createSchema("Geometry"));
		for (Iterator iteratorA = fcA.iterator(); iteratorA.hasNext();) {
			Feature featureA = (Feature) iteratorA.next();
			Geometry geometryA = featureA.getGeometry();
			int numGeometries = geometryA.getNumGeometries();
			// Process each component of multipolygons individually
			for (int i = 0 ; i < numGeometries ; i++) {
			    Geometry componentA = geometryA.getGeometryN(i);
			    if (!(componentA instanceof Polygon)) continue;
			    List candidates = indexB.query(componentA.getEnvelopeInternal());
			    Geometry geomB = null;
			    boolean match = false;
			    int minCountCommonCoordinates = -1;
			    double bestSimilarity = 0;
			    for (Iterator iteratorB = candidates.iterator() ; iteratorB.hasNext();) {
			        Geometry gB = (Geometry)iteratorB.next();
			        // Find simplified candidate having the maximum number of 
			        // coordinates in common with componentA
			        // This makes sense only because the simplification is based 
			        // on Douglas-Peucker algorithm)
			        // Don't use "=" while testing number of common coordinates  
			        // because sometimes, new coordinates are introduced in the 
			        // simplification algorithm
			        // To avoid some pitfalls when simplified edges cross each 
			        // others, commonCoordinates also check orientation of
			        // polygons and return a negative value if they have an
			        // opposite orientation
			        int countCommonCoordinates = commonCoordinates((Polygon)componentA, (Polygon)gB);
			        if (countCommonCoordinates >= minCountCommonCoordinates) {
			            // if the number of common coordinates is strictly equal
			            // check that B can really be a simplified version of A
			            if (countCommonCoordinates == minCountCommonCoordinates) {
			                double areaRatio = componentA.getEnvelope().getArea()/gB.getEnvelope().getArea();
			                double similarity = Math.min(areaRatio, 1.0/areaRatio);
			                if (similarity > bestSimilarity &&
			                    gB.buffer(Math.abs(tolerance)*1.1).contains(componentA) &&
			                    componentA.buffer(Math.abs(tolerance)*1.1).contains(gB)) {
			                    geomB = gB;
			                    bestSimilarity = similarity;
			                }
			            }
			            else {
			                geomB = gB;
			            }
			            minCountCommonCoordinates = countCommonCoordinates;
			            match = true;
			        }
			    }
			    if (match) {
			        Feature f = new BasicFeature(fd.getFeatureSchema());
			        mapping.transferAttributes(featureA, null, f);
			        f.setGeometry(geomB);
			        fd.add(f);
			    }
			    else {
			        context.getWorkbenchFrame().warnUser(sAttributeTransferNotExhaustive);
			    }
			    if (monitor != null && monitor.isCancelRequested()){
	    		    return fd;
	    	    }
			}
		}
		return fd;
	}
	
	
	/**
	 * Count the number of coordinates belonging to a and b
	 * End points which are common to start points are skipped
	 * Input geometries must not have identical consecutive points 
	 */
	private int commonCoordinates(Polygon a, Polygon b) {
	    Coordinate[] cca = ((Polygon)a.norm()).getExteriorRing().getCoordinates();
	    Coordinate[] ccb = ((Polygon)b.norm()).getExteriorRing().getCoordinates();
	    int count = 0;
	    // orientation = 1 (same orientation or -1 (opposite orientation)
	    int orientation = 1;
	    for (int i = 0 ; i < cca.length-1 ; i++) {
	        for (int j = 0 ; j < ccb.length-1 ; j++) {
	            if (cca[i].equals(ccb[j])) {
	                count++;
	                // we can use i+1 and j+1 without test because the loops
	                // stops at i (and j) < length-1
	                if (i > 0 && cca[i-1].equals(ccb[j+1])) orientation = -1;
	                if (j > 0 && ccb[j-1].equals(cca[i+1])) orientation = -1;
	                break;
	            }
	        }
	    } 
	    return count * orientation;
	}
}
