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
package org.openjump.core.graph.polygongraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

public class PolygonGraphEdge {
	
	private static int edgeIds = 0;
	public int edgeId = -1;
	public PolygonGraphNode node1 = null;
	public PolygonGraphNode node2 = null;
	private ArrayList<Geometry> boundaries = new ArrayList();
	public boolean boundaryCalculated = false;
		
	public PolygonGraphEdge(PolygonGraphNode node1, PolygonGraphNode node2){
		this.node1= node1;
		this.node2= node2;
		edgeIds++;
		this.edgeId = edgeIds;
	}

	private void calculateBoundary(){
		this.boundaryCalculated = true;
		Geometry intersection = node1.geometry.intersection(node2.geometry);
	    // Create the edge layer by merging lines between 3+ order nodes
	    // (Merged lines are multilines)
	    LineMerger lineMerger = new LineMerger();
	    for (int i = 0 ; i < intersection.getNumGeometries() ; i++) {
	        lineMerger.add(intersection.getGeometryN(i));
	    }
	    Collection edges = lineMerger.getMergedLineStrings();		
		this.boundaries.addAll(edges);
	}
	
	public boolean hasNodes(int nodeId1, int nodeId2) {
		boolean found = false;
		if((node1.nodeId == nodeId1) || (node1.nodeId == nodeId2)){
			//-- node1 is there
			//   now check if node2 is also there
			if((node2.nodeId == nodeId1) || (node2.nodeId == nodeId2)){
				found = true;
				return true;
			}
		}
		return found;
	}
	
	public LineString getEdgeAsInterriorPointLineString(){
		LineString ls = null;
		Coordinate[] coords = new Coordinate[2];
		Point c1 = node1.geometry.getInteriorPoint();
		Point c2 = node2.geometry.getInteriorPoint();
		coords[0] = new Coordinate(c1.getX(), c1.getY());
		coords[1] = new Coordinate(c2.getX(), c2.getY());
		ls = new GeometryFactory().createLineString(coords);
		return ls;
	}
	
	public Feature getEdgeBetweenInterriorPointsAsFeature(){
		Feature f = new BasicFeature(PolygonGraphEdge.getEdgeFeatureSchema());
		f.setGeometry(this.getEdgeAsInterriorPointLineString());
		f.setAttribute("edgeId", this.edgeId);
		f.setAttribute("startNode", node1.nodeId);
		f.setAttribute("endNode", node2.nodeId);
		return f;
	}
	
	public static FeatureSchema getEdgeFeatureSchema(){
		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("Geometry", AttributeType.GEOMETRY);
		fs.addAttribute("edgeId", AttributeType.INTEGER);
		fs.addAttribute("startNode", AttributeType.INTEGER);
		fs.addAttribute("endNode", AttributeType.INTEGER);
		return fs;
	}	

	public ArrayList<Geometry> getBoundaries() {
		if(this.boundaryCalculated == false){
			this.calculateBoundary();
			this.boundaryCalculated = true;
		}
		return boundaries;
	}
	
	public ArrayList<Feature> getBoundariesAsFeature(){
		GeometryFactory gf = new GeometryFactory();
		ArrayList<Feature> fbds = new ArrayList();
		ArrayList<Geometry> geoms = this.getBoundaries();
		int bdcount=0;
		for (Iterator iterator = geoms.iterator(); iterator.hasNext();) {
			Geometry geometry = (Geometry) iterator.next();
			bdcount++;		
			Feature fnew = new BasicFeature(PolygonGraphNode.getBoundaryFeatureSchema());
			if (geometry instanceof LinearRing){
				geometry = gf.createLineString(((LinearRing)geometry).getCoordinateSequence());
			}
			fnew.setGeometry(geometry);
			fnew.setAttribute("edgeId", this.edgeId);
			fnew.setAttribute("boundaryId", bdcount);
			fnew.setAttribute("startNode", this.node1.nodeId);
			fnew.setAttribute("endNode", this.node2.nodeId);
			fbds.add(fnew);
		}
		return fbds;
	}
}
