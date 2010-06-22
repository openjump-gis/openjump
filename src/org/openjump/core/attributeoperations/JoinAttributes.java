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
/***********************************************
 * created on 		21.06.2006
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 	joins attribute values according to some spatial and statistical criterion
 * 
 ***********************************************/
package org.openjump.core.attributeoperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * joins attribute values according to some spatial and statistical criterion
 * 
 * @author sstein
 *
 */
public class JoinAttributes {

    /**
     * 
     * @param sourceFeatures
     * @param targetFeatures
     * @param attributeName
     * @param attributeOp
     * @param spatialRelation
     * @param bufferRadius
     * @return a feature dataset
     */
    public static FeatureDataset joinAttributes(Collection sourceFeatures, Collection targetFeatures, String attributeName, int attributeOp, int spatialRelation, double bufferRadius, TaskMonitor monitor){
        /*
        System.out.println("Join Attributes --- attribute op:" + attributeOp + " - " + 
                AttributeOp.getName(attributeOp) + " --- spatial op: " + spatialRelation + " - " +
                SpatialRelationOp.getName(spatialRelation));
        */
        FeatureDataset fd = null;
	    AttributeType newAttributeType = AttributeType.DOUBLE;
	    String newAttributeName = attributeName + "_" + AttributeOp.getName(attributeOp);
	    if (attributeOp == AttributeOp.COUNT){
	    	newAttributeName = AttributeOp.getName(attributeOp);
	    }
	    
	    //-- put all in a tree
        Quadtree fqTree = new Quadtree();
        FeatureSchema sourceFS = null;
        int count =0; 
       	for (Iterator iter = sourceFeatures.iterator(); iter.hasNext();) {
            Feature pt = (Feature) iter.next();                        
            fqTree.insert(pt.getGeometry().getEnvelopeInternal(), pt);
            if(count == 0){
                sourceFS = pt.getSchema();
            }
            count++;
       	}
       	//-- get AttributeType
       	AttributeType at = null;
       	try{ 
       	    at = sourceFS.getAttributeType(attributeName);
       	    }
       	catch(Exception e){
       	    at = AttributeType.GEOMETRY;
       	    attributeName = sourceFS.getAttributeName(0);
       	    System.out.println("JoinAttributes.joinAttributes: replace unknown attribute name by geometry");
       	}
       	//
       	ArrayList outPolys = new ArrayList();
	    int size = targetFeatures.size();  	    	        		    
	    FeatureSchema targetFSnew = null;
	    count=0;	    
	    Iterator iterp = targetFeatures.iterator();	    
	    while(iterp.hasNext()){	    	
	    	count=count+1;
	    	if(monitor != null){
	    	    monitor.report("item: " + count + " of " + size);
	    	}
	    	Feature p = (Feature)iterp.next();
	    	if (count == 1){
	    	    FeatureSchema targetFs = p.getSchema();
	    	    targetFSnew = copyFeatureSchema(targetFs);
	    	    if (targetFSnew.hasAttribute(newAttributeName)){
	    	        //attribute will be overwriten
	    	    }
	    	    else{
	    	        //add attribute
	    	        targetFSnew.addAttribute(newAttributeName, newAttributeType);
	    	    }
	    	}
	    	//-- evaluate value for every polygon
	    	double value = evaluateSinglePolygon(p.getGeometry(), fqTree, attributeName, attributeOp, spatialRelation, bufferRadius);
	    	Feature fcopy = copyFeature(p, targetFSnew);
	    	fcopy.setAttribute(newAttributeName, new Double(value));
	    	outPolys.add(fcopy);
	    }
    	fd = new FeatureDataset(targetFSnew);  
    	fd.addAll(outPolys);	    	
		return fd;        
    }
    
	/**
	 * 
	 * @param poly
	 * @param 
	 * @return value 
	 */
	private static double evaluateSinglePolygon(Geometry poly, Quadtree fqTree, String attributeName, int attributeOp, int spatialRelation, double bufferRadius){	    
	    List items = SpatialRelationOp.evaluateSpatial(spatialRelation, fqTree, poly, bufferRadius);
	    double val = AttributeOp.evaluateAttributes(attributeOp, items, attributeName);	        
	    return val;	    
	}
	  
    
	/**
	 * Copy/clone the input featureSchema since it is not proper implemented in Jump 
	 * @param oldSchema
	 * @return a clone of oldSchema
	 */
	public static FeatureSchema copyFeatureSchema(FeatureSchema oldSchema){
		FeatureSchema fs = new FeatureSchema();
		for (int i = 0; i < oldSchema.getAttributeCount(); i++) {
			AttributeType at = oldSchema.getAttributeType(i);
			String aname = oldSchema.getAttributeName(i);
			fs.addAttribute(aname,at);
			fs.setCoordinateSystem(oldSchema.getCoordinateSystem());			
		}		
		return fs;
	}
	 
	/**
	 * Copy the input feature to a new Schema whereby the new 
	 * Feature Schema must be an extended or shortened one 
	 * @param feature
	 * @param newSchema
	 * @return a new Feature with newSchema as Schema and feature values
	 */
	public static Feature copyFeature(Feature feature, FeatureSchema newSchema){
		FeatureSchema oldSchema = feature.getSchema();
		Feature newF = new BasicFeature(newSchema);
		int n = 0;
		if (oldSchema.getAttributeCount() > newSchema.getAttributeCount()){
			//for schema shortening
			n = newSchema.getAttributeCount();
		}
		else{
			//for schema extension
			n = oldSchema.getAttributeCount();
		}
		for (int i = 0; i < n; i++) {			
			String aname = oldSchema.getAttributeName(i);
			Object value = feature.getAttribute(aname);			
			newF.setAttribute(aname,value);						
		}		
		return newF;
	}
	
}
