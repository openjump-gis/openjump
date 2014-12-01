/***********************************************
 * created on 		14.09.2005
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 	merges two polygon and returns the result via
 *  getOutPolygon() if the result is not of another
 *  geometry type. The merge is done with the JTS union()
 *  method.
 ***********************************************/
package org.openjump.core.geomutils.algorithm;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.Polygon;

/** 
 *  Merges two polygon and returns the result via
 *  getOutPolygon() if the result is not of another
 *  geometry type. The merge is done with the JTS union()
 *  method.
 *  
 * @author sstein
 *
 */
public class PolygonMerge {

	private boolean polys = false;
	private int mergeSuccesfull = 0;
	private Geometry outPolygon = null;
	
	public PolygonMerge(Geometry poly1, Geometry poly2){
		if ((poly1 instanceof Polygon) && (poly2 instanceof Polygon)){
			this.polys = true;
			//check if polygons do touch
	        IntersectionMatrix myIM = poly1.relate(poly2);
	        /*	        
	        System.out.println("IntersectionMat: top right-bottom left : " 
	                + myIM.get(0,0) +  " " +  myIM.get(0,1) +  " " + myIM.get(0,2) +  "," 
	                + myIM.get(1,0) +  " " +  myIM.get(1,1) +  " " + myIM.get(1,2) +  ","
	                + myIM.get(2,0) +  " " +  myIM.get(2,1) +  " " + myIM.get(2,2) );
            */
	        if (myIM.matches("2********") || (myIM.matches("****1****"))){
	        	//System.out.println("PolygonMerge.constructor: calc merge");
	            Geometry geom = poly1.union(poly2);
            	this.outPolygon = geom;
	            if (geom instanceof Polygon){
	            	this.mergeSuccesfull = 1;
	            }
	            else{
	            	//System.out.println("PolygonMerge.constructor: merged object would be multi-polygon");	            	
	            	this.mergeSuccesfull = 2;
	            }
			}
	        else{
	        	//System.out.println("PolygonMerge.constructor: polygons don't touch");
	        }
		}		
	}
	
	/**
	 * @return Returns the mergeSuccesfull.<p>
	 * 		0: polys dont touch, 1: sucessfull, 2: polys touch but only with one point on line. thus 
	 * 		a multipolygon is created.
	 */
	public int isMergeSuccesfull() {
		return mergeSuccesfull;
	}
	/**
	 * @return Returns the outPolygon.
	 */
	public Geometry getOutPolygon() {
		return outPolygon;
	}
	/**
	 * @return Returns if both are polys.
	 */
	public boolean isPolys() {
		return polys;
	}
}
