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
 * created on 		22.06.2006
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 		contains some method to extract sets of features
 * 		which fullfill some spatial criterion
 * 
 ***********************************************/
package org.openjump.core.attributeoperations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jump.feature.Feature;

/**
 *
 * contains some method to extract sets of features
 * which fullfill some spatial criterion<p>
 * notes:<p>
 * - use "intersects" only for polygon geometries (condition intersection area > 0).<p>
 * - "contains" can be used for polygons and points (centroid from polygon is calculated)<p>
 * 
 * @author sstein
 * 
 */
public class SpatialRelationOp {
    public final static int CONTAINS = 0;
    public final static int INTERSECTS = 1;
    public final static int COVEREDBY = 2;    
    //public final static int TOUCHES = 3;

    public static String getName(int spatialRel){
        String retval = "";
        if(spatialRel == 0){
            retval ="contains";
        }
        else if(spatialRel == 1){
            retval ="intersects";
        }
        return retval; 
    }
 
    /**
     * note: if input feature is point and spatial attribute is "intersect" or "covered by" the 
     * candidate features are selected from a 10.0m radius   
     * @param spatialRelation
     * @param featureTree
     * @param g
     * @param radius
     * @return ArrayList of Feature fullfilling the spatial criterion
     */
    public static List evaluateSpatial(int spatialRelation, Quadtree featureTree, Geometry g, double radius){

        List foundItems = new ArrayList();
        
        if(spatialRelation == SpatialRelationOp.CONTAINS){
            Geometry buffer = g.buffer(radius);            
	        List candidates = featureTree.query(buffer.getEnvelopeInternal());
            if (g instanceof Point){
                radius = 10.0;
                Geometry buffer2 = g.buffer(radius);                
                candidates = featureTree.query(buffer2.getEnvelopeInternal());
            }   	        
	       	for (Iterator iter = candidates.iterator(); iter.hasNext();) {
	            Feature candidate = (Feature) iter.next();
	            boolean retval = buffer.contains(candidate.getGeometry().getCentroid());
	            if(retval){
	                foundItems.add(candidate);
	            }
	        } 
        }
        else if(spatialRelation == SpatialRelationOp.INTERSECTS){
            Geometry buffer = g.buffer(radius);            
	        List candidates = featureTree.query(buffer.getEnvelopeInternal());
	        if (g instanceof Point){
	                radius = 10.0;
	                Geometry buffer2 = g.buffer(radius);                
	                candidates = featureTree.query(buffer2.getEnvelopeInternal());
	                //-- reset to point geom
	                buffer = g;
	        }   	        
	       	for (Iterator iter = candidates.iterator(); iter.hasNext();) {
	            Feature candidate = (Feature) iter.next();
	            Geometry geom = buffer.intersection(candidate.getGeometry());
	            if(geom.getLength() > 0){
	                foundItems.add(candidate);
	            }
	        } 
        }
        else if(spatialRelation == SpatialRelationOp.COVEREDBY){
            Geometry buffer = g.buffer(radius);
            List candidates = featureTree.query(buffer.getEnvelopeInternal());
            if (g instanceof Point){
                radius = 10.0;
                Geometry buffer2 = g.buffer(radius);                
                candidates = featureTree.query(buffer2.getEnvelopeInternal());
                //-- reset to point geom
                buffer = g;
            }            	              
	       	for (Iterator iter = candidates.iterator(); iter.hasNext();) {
	            Feature candidate = (Feature) iter.next();	            
	            if(buffer.coveredBy(candidate.getGeometry())){
	                foundItems.add(candidate);
	            }
	        } 
        }                
        else{
            System.out.println("SpatialRelationOp: spatial relation does not exit");
        }
        return foundItems; 
    }
}
