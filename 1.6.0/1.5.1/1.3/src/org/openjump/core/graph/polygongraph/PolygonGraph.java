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
import java.util.List;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.core.geomutils.algorithm.GeometryConverter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;

public class PolygonGraph{

	public ArrayList<PolygonGraphEdge> edges = new ArrayList<PolygonGraphEdge>(); 
	public ArrayList<PolygonGraphNode> nodes = new ArrayList<PolygonGraphNode>();
	
	/**
	 * creates a new polygon graph objects and populates it with the feature delivered 
	 * @param polygonFeatures
	 */
	public PolygonGraph(Collection<Feature> polygonFeatures){
		this.createPolygonGraphFrom(polygonFeatures, null);
	}
	
	/**
	 * creates a new polygon graph objects and populates it with the feature delivered 
	 * @param polygonFeatures
	 * @param monitor can be null, used to cancel operation
	 */
	public PolygonGraph(Collection<Feature> polygonFeatures, TaskMonitor monitor){
		this.createPolygonGraphFrom(polygonFeatures, monitor);
	}
	
	public PolygonGraph(FeatureCollection polygonFeatures){
		this.createPolygonGraphFrom(polygonFeatures.getFeatures(), null);
	}
	
	/**
	 * 
	 * @param fc
	 * @param monitor can be null, used to cancel operation
	 */
	public void createPolygonGraphFrom(Collection<Feature> fc, TaskMonitor monitor){
		//----------------------------------------------
		// check if this are really polygons
		// if yes create a PolygonGraphNode
		// and add the node to a STRtree for faster search
		//----------------------------------------------
		SpatialIndex tree = new STRtree();
		for (Iterator iterator = fc.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			if(f.getGeometry() instanceof Polygon){
				//-- create a new node
				PolygonGraphNode node = new PolygonGraphNode(f); 
				this.nodes.add(node);
				tree.insert(f.getGeometry().getEnvelopeInternal(), node);
			}
			else if(f.getGeometry() instanceof MultiPolygon){ //must be else if !!!
				Geometry geom = f.getGeometry(); 
				//-- explode and add
				ArrayList<Geometry> parts = GeometryConverter.explodeGeomsIfMultiG(geom);
				for (Iterator iterator2 = parts.iterator(); iterator2.hasNext();) {
					Geometry gpart = (Geometry) iterator2.next();					
					Feature fnew = FeatureCollectionTools.copyFeature(f);					
					fnew.setGeometry(gpart);
					//-- create a new node
					PolygonGraphNode node = new PolygonGraphNode(fnew); 
					this.nodes.add(node);					
					tree.insert(gpart.getEnvelopeInternal(), node);
				}
			}
		}
    	if (monitor != null){
    		if (monitor.isCancelRequested()){
    			monitor.report("canceled 1");
    			return;
    		}
    	}
		//--------------------------------------------
		// analyze relations
		// note: updating the nodes with its relations should 
		//       work as we hopefully work only with pointers
		//--------------------------------------------
		for (Iterator iterator2 = nodes.iterator(); iterator2.hasNext();) {
			PolygonGraphNode node = (PolygonGraphNode) iterator2.next();
			Geometry geom = node.geometry; 
			//-- search all Polygons/nodes in the surrounding
			//-- get candidates
			List candidates = tree.query(geom.getEnvelopeInternal());
			//-- check those for touching or overlapping
			for (Iterator iterator = candidates.iterator(); iterator.hasNext();) {
				PolygonGraphNode ntemp = (PolygonGraphNode) iterator.next();
				//-- check if we did not retrieve the polygon itself
				if(ntemp.nodeId == node.nodeId){
					//-- don't do anything
					//System.out.println("found itself");
				}
				//-- check if connection already exists (from an earlier test)
				else if(node.hasConnection(ntemp)){
					//System.out.println("connection exists already");
				}
				else{				
					//System.out.println("calculate relation: node 1" + node.nodeId + " with node " + ntemp.nodeId);
					//IntersectionMatrix mat = geom.relate((ntemp.geometry);				
					if (geom.disjoint((ntemp.geometry)) == false){
						//System.out.println("found connected: node 1" + node.nodeId + " with node " + ntemp.nodeId);
						//-- make a new edge and add the edge to both nodes
						PolygonGraphEdge edge = new PolygonGraphEdge(node, ntemp);
						//-- add the edge to both and to the edge list
						node.edges.add(edge);
						ntemp.edges.add(edge);
						this.edges.add(edge);
					}
				}
		    	if (monitor != null){
		    		if (monitor.isCancelRequested()){
		    			monitor.report("canceled 2");
		    			return;
		    		}
		    	}
			}//-- end loop over candidates
		}//-- end loop over all nodes		
	}
	
	public FeatureCollection getEdgesBetweenInterriorPoints(){
		FeatureCollection fc = new FeatureDataset(PolygonGraphEdge.getEdgeFeatureSchema());
		for (Iterator iterator = this.edges.iterator(); iterator.hasNext();) {
			PolygonGraphEdge edge = (PolygonGraphEdge) iterator.next();
			fc.add(edge.getEdgeBetweenInterriorPointsAsFeature());
		}
		return fc;
	}
	
	public FeatureCollection getNodesAsInterriorPoint(){
		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("Geometry", AttributeType.GEOMETRY);
		fs.addAttribute("nodeId", AttributeType.INTEGER);
		fs.addAttribute("featureId", AttributeType.INTEGER);
		FeatureCollection fc = new FeatureDataset(fs);
		for (Iterator iterator = this.nodes.iterator(); iterator.hasNext();) {
			PolygonGraphNode node = (PolygonGraphNode) iterator.next();
			Feature f = new BasicFeature(fs);
			f.setAttribute("nodeId", node.nodeId);
			f.setAttribute("featureId", node.realWorldObject.getID());
			f.setGeometry(node.geometry.getInteriorPoint());
			fc.add(f);
		}
		return fc;
	}
	
	public FeatureCollection getSharedBoundaries(){
		//--loop over all edges to retrieve only unique boundaries
		FeatureCollection fc = new FeatureDataset(PolygonGraphNode.getBoundaryFeatureSchema());
		for (Iterator iterator = edges.iterator(); iterator.hasNext();) {
			PolygonGraphEdge tedge = (PolygonGraphEdge) iterator.next();
			ArrayList<Feature> edgeBoundaries = tedge.getBoundariesAsFeature();
			for (Iterator iterator2 = edgeBoundaries.iterator(); iterator2.hasNext();) {
				Feature f = (Feature) iterator2.next();
				f.setAttribute(PolygonGraphNode.edgeTypeAtributeName, "shared");
			}
			fc.addAll(edgeBoundaries);	
		}		
		return fc;
	}

	public FeatureCollection getNonSharedBoundaries(){
		//--loop over all edges to retrieve only unique boundaries
		FeatureCollection fc = new FeatureDataset(PolygonGraphNode.getBoundaryFeatureSchema());
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
			PolygonGraphNode tnode = (PolygonGraphNode) iterator.next();
			ArrayList<Feature> nonSharedBoundaries = tnode.getNonSharedBoundariesAsFeature();	
			for (Iterator iterator2 = nonSharedBoundaries.iterator(); iterator2.hasNext();) {
				Feature f = (Feature) iterator2.next();
				f.setAttribute(PolygonGraphNode.edgeTypeAtributeName, "nonshared");
			}
			fc.addAll(nonSharedBoundaries);	
		}		
		return fc;
	}

	public FeatureCollection getCommonBoundaries(PolygonGraphNode node){
		ArrayList<Feature> boundaries = node.retrieveSharedBoundaries();		
		FeatureCollection fc = new FeatureDataset(PolygonGraphNode.getBoundaryFeatureSchema());
		fc.addAll(boundaries);
		return fc;
	}
}
