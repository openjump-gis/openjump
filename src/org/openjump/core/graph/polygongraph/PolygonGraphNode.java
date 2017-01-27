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
import java.util.Iterator;

import org.openjump.core.geomutils.algorithm.GeometryConverter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

public class PolygonGraphNode {

	public Geometry geometry = null;
	public Feature realWorldObject = null;
	private static int nodeIds = 0;
	public int nodeId = -1;
	public ArrayList<PolygonGraphEdge> edges = new ArrayList<PolygonGraphEdge>();
	public static final String edgeTypeAtributeName = "edgeType"; 
	
	public PolygonGraphNode(Feature f){
		this.realWorldObject = f;
		this.geometry = f.getGeometry();
		nodeIds++;
		this.nodeId = nodeIds;
	}	
	
	public ArrayList<Feature> retrieveSharedBoundaries(){
		ArrayList<Feature> boundaries = new ArrayList();
		for (Iterator iterator = edges.iterator(); iterator.hasNext();) {
			PolygonGraphEdge tedge = (PolygonGraphEdge) iterator.next();
			ArrayList<Geometry> lines = tedge.getBoundaries();
			int bdcount = 0;
			for (Iterator iterator2 = lines.iterator(); iterator2.hasNext();) {
				bdcount++;
				Geometry geom = (Geometry) iterator2.next();
				Feature fnew = new BasicFeature(PolygonGraphNode.getBoundaryFeatureSchema());
				fnew.setGeometry(geom);
				fnew.setAttribute("edgeId", tedge.edgeId);
				fnew.setAttribute("boundaryId", bdcount);
				fnew.setAttribute("startNode", tedge.node1.nodeId);
				fnew.setAttribute("endNode", tedge.node2.nodeId);
				boundaries.add(fnew);
			}
		}
		return boundaries;
	}
	
	public ArrayList<Feature> getNonSharedBoundariesAsFeature(){
		ArrayList<Feature> nonShared = new ArrayList<Feature>();
		ArrayList<Geometry> boundaries = new ArrayList();
		for (Iterator iterator = edges.iterator(); iterator.hasNext();) {
			PolygonGraphEdge tedge = (PolygonGraphEdge) iterator.next();
			ArrayList<Geometry> lines = tedge.getBoundaries();
			boundaries.addAll(lines);
		}  
		//-- merge the shared boundaries to a MultiLineString
		//   I assume there will be no crossings/intersections of the boundaries
		Geometry diff = null;
		if (boundaries.size() > 0){
			Geometry union = boundaries.get(0);
			for(int i=1; i < boundaries.size() ;i++){
				union = union.union(boundaries.get(i));
			}
			//-- calculate difference with original (boundary) geometry
			diff = this.geometry.getBoundary().difference(union);
		}
		else{// if there are no other polygons we return the boundary
			diff = this.geometry.getBoundary();
		}
		ArrayList<Geometry> explodedGeoms = GeometryConverter.explodeGeomsIfMultiG(diff);
		//-- create the features		
		int count = 0; GeometryFactory gf = new GeometryFactory();
		for (Iterator iterator = explodedGeoms.iterator(); iterator.hasNext();) {
			Geometry geom = (Geometry) iterator.next();
			count++;
			Feature fnew = new BasicFeature(PolygonGraphNode.getBoundaryFeatureSchema());
			if (geom instanceof LinearRing){
				geom = gf.createLineString(((LinearRing)geom).getCoordinateSequence());
			}			
			fnew.setGeometry(geom);
			fnew.setAttribute("startNode", nodeId);
			fnew.setAttribute("endNode", nodeId);
			fnew.setAttribute("boundaryId", count);
			fnew.setAttribute("edgeId", -1);			
			nonShared.add(fnew);
		}
		return nonShared;		
	}
	
	public boolean hasConnection(PolygonGraphNode node){
		boolean found = false; 
		for (Iterator iterator = edges.iterator(); iterator.hasNext();) {
			PolygonGraphEdge tedge = (PolygonGraphEdge) iterator.next();
			if(tedge.hasNodes(this.nodeId, node.nodeId)){
				found = true;
				return true;
			}
		}
		return found;
	}
	
	public static FeatureSchema getBoundaryFeatureSchema(){
		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("Geometry", AttributeType.GEOMETRY);
		fs.addAttribute("edgeId", AttributeType.INTEGER);
		fs.addAttribute("boundaryId", AttributeType.INTEGER);
		fs.addAttribute("startNode", AttributeType.INTEGER);
		fs.addAttribute("endNode", AttributeType.INTEGER);
		fs.addAttribute(PolygonGraphNode.edgeTypeAtributeName, AttributeType.STRING);
		return fs;
	}
}
