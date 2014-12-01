/***********************************************
 * created on 		5.May.2008
 * last modified: 	19.May.2008
 * 
 * author:			sstein
 * license: 		LGPL
 * 
 * description:
 * 	provides functions to merge/intersects an ArrayList of geometries
 *  that may contain several (possibly different) geometries 
 ***********************************************/
package org.openjump.core.geomutils.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class IntersectGeometries {


/*	*//**
	 * TODO: this method is not properly tested for mixed geometries and needs to be revised.
	 * It can be used either for collections of LineString or Polygons
	 * @param geomList
	 * @param monitor can be null
	 * @param context can be null
	 * @return
	 *//*
	public static ArrayList<Geometry> intersectGeometries(ArrayList<Geometry> geomList, TaskMonitor monitor, PlugInContext context){
		ArrayList<Geometry> withoutIntersection = new ArrayList<Geometry>(); 
		//-- resolve all GeometryCollections/Multigeometries
		ArrayList<Geometry> tempList = new ArrayList();
		for (int i = 0; i < geomList.size(); i++) {
			Geometry geom = geomList.get(i);
			if (geom instanceof GeometryCollection){
				ArrayList<Geometry> parts = GeometryConverter.explodeGeomsIfMultiG(geom);
				tempList.addAll(parts);
			}
			else{
				tempList.add(geom);
			}
		}
		geomList = tempList;
		//-- note: no assumption that objects in one layer have contain overlaps
		int totalCount = 0;
		while(geomList.size() > 1){
			//-- take alsways the first item until there is not more left 
			Geometry g2Test = geomList.get(0);
			//--check against all others
			//   this is very time consuming, but necessary
			if (monitor != null){
				if (monitor.isCancelRequested()){
					return withoutIntersection;
				}
				else{
					monitor.report("n: " + geomList.size());
				}
			}
			//-- avoid that already GeomCollections are inside
			while (g2Test instanceof GeometryCollection){
				ArrayList<Geometry> parts = GeometryConverter.explodeGeomsIfMultiG(g2Test);
				geomList.addAll(parts);
				geomList.remove(0);
				g2Test = geomList.get(0);
			}
			//--start with the checks
			boolean checkNext = true; 
			int count = 0; 		  		 
			while(checkNext) {
				count++; totalCount++;
				Geometry gtemp = geomList.get(count);
				boolean checkRes = checkIntersectionByGeomTypeB(g2Test,gtemp);
				if (checkRes){
					//-- stop cycle, and take new one first
					checkNext = false;
					Geometry gnewI = null; Geometry gnewNi = null;
//					if (checkRes == 1){
						//-- get intersection and difference
						gnewI = g2Test.intersection(gtemp);
						//-- note: use difference as we
						//   are only interested in returning parts of the
						//   the this geometry and not the other
						gnewNi = g2Test.symDifference(gtemp);
//					}
//					else if (checkRes == 2){
//						gnewI = gtemp.intersection(g2Test);
//						gnewNi = gtemp.symDifference(g2Test);
//					}	
					//explode multi-geoms
					ArrayList<Geometry> intersection = GeometryConverter.explodeGeomsIfMultiG(gnewI);
					ArrayList<Geometry> nonIntersection = GeometryConverter.explodeGeomsIfMultiG(gnewNi);
					//-- remove the item (first the latter one, then the others)
					geomList.remove(count);
					geomList.remove(0);
//					System.out.println("size now:" + geomList.size());
					//-- add instead the parts
					geomList.addAll(intersection);
					geomList.addAll(nonIntersection);
					//-- testing stuff
//					System.out.println("size new:" + geomList.size());
//					System.out.println("tested g1: " + g2Test.getClass() + " g2: " + gtemp.getClass());
//					checkIntersectionByGeomTypeB(g2Test,gtemp); //for checking again
//					FeatureCollection fc = FeatureDatasetFactory.createFromGeometry(geomList);
//					ArrayList<Geometry> tointersect = new ArrayList<Geometry>();
//					tointersect.add(g2Test); tointersect.add(gtemp);
//					FeatureCollection fc2 = FeatureDatasetFactory.createFromGeometry(tointersect);
//					context.addLayer(StandardCategoryNames.WORKING, "tointersect", fc2);
//					context.addLayer(StandardCategoryNames.WORKING, "loop", fc);
				}
				if(((count+1) == geomList.size()) && (checkNext == true)){
					//-- nothing more to test
					//   so we can add this geometry to the finalList
					//System.out.println("added");
					withoutIntersection.add((Geometry)g2Test.clone());
					//-- we remove it from the list of items
					geomList.remove(0);
					checkNext = false;
				}
				// if(totalCount == 1000000){
				//	  checkNext = false;
				//	  //System.out.println("stopped");
				//  }
			}
		}
		//-- add the last (or only) one
		withoutIntersection.add(geomList.get(0));
		return withoutIntersection;
	}*/
	
	/**
	 * the method intersects all polygons in the geometry list with each other. An intersection is only
	 * proceeded if it returns another polygon (i.e. the intersection area > 0). Unfortunately the method
	 * returns results where some polygons may contain spikes. For this reason it may be better to create an 
	 * an intersection of Linestrings (derived from the Polygons) and then use the Polygonizer (see IntersectPolygonLayersPlugIn).  
	 * 
	 * @param geomList
	 * @param this parameter is currently not used and replaced by the use of a fixed precision model
	 * @param monitor can be null
	 * @param context can be null
	 * @return
	 */
	public static ArrayList<Geometry> intersectPolygons(ArrayList<Geometry> geomList, double accurracy, TaskMonitor monitor, PlugInContext context){
		ArrayList<Geometry> withoutIntersection = new ArrayList<Geometry>(); 
		//-- resolve all GeometryCollections/Multigeometries
		ArrayList<Geometry> tempList = new ArrayList();
		for (int i = 0; i < geomList.size(); i++) {
			Geometry geom = geomList.get(i);
			if (geom instanceof GeometryCollection){
				ArrayList<Geometry> parts = GeometryConverter.explodeGeomsIfMultiG(geom);
				tempList.addAll(parts);
			}
			else{
				tempList.add(geom);
			}
		}
		geomList = tempList;
		//-- use a fixed precision model 
		//double scaleFactor = (1.0/accurracy)/10.0; //devide additionally by 10 to remove one digit more
		//PrecisionModel pm = new PrecisionModel(scaleFactor);
		PrecisionModel pm = new PrecisionModel(PrecisionModel.FIXED);
		GeometryFactory gf = new GeometryFactory(pm);
		tempList = new ArrayList();
		for (Iterator iterator = geomList.iterator(); iterator.hasNext();) {
			Geometry geomOld = (Geometry) iterator.next();
			Geometry geomNew = createGeometryWithFixedPrecision(gf,geomOld);
			if (geomNew != null){
				tempList.add(geomNew);
			}
			else{
				//-- bad luck for geometry collections, we leave them out
			}
		}
		geomList = tempList;
		//-- note: no assumption that objects in one layer have contain overlaps
		int totalCount = 0;
		while(geomList.size() > 1){
			//-- take alsways the first item until there is not more left 
			Geometry g2Test = geomList.get(0);
			//--check against all others
			//   this is very time consuming, but necessary
			if (monitor != null){
				if (monitor.isCancelRequested()){
					return withoutIntersection;
				}
				else{
					monitor.report("n: " + geomList.size());
				}
			}
			//-- avoid that already GeomCollections are inside
			while (g2Test instanceof GeometryCollection){
				ArrayList<Geometry> parts = GeometryConverter.explodeGeomsIfMultiG(g2Test);
				geomList.addAll(parts);
				geomList.remove(0);
				g2Test = geomList.get(0);
			}
			//--start with the checks
			boolean checkNext = true; 
			int count = 0; 		  		 
			while(checkNext) {
				count++; totalCount++;
				Geometry gtemp = geomList.get(count);
				boolean checkRes = checkPolygonIntersection(g2Test,gtemp);
				if (checkRes){
					//-- stop cycle, and take new one first
					checkNext = false;
					Geometry gnewI = null; Geometry gnewNi = null;
					//-- get intersection and difference
					gnewI = g2Test.intersection(gtemp);
					//-- note: use difference as we
					//   are only interested in returning parts of the
					//   the this geometry and not the other
					gnewNi = g2Test.symDifference(gtemp);
					//explode multi-geoms
					ArrayList<Geometry> intersection = GeometryConverter.explodeGeomsIfMultiG(gnewI);
					ArrayList<Geometry> nonIntersection = GeometryConverter.explodeGeomsIfMultiG(gnewNi);
					//-- remove the item (first the latter one, then the others)
					geomList.remove(count);
					geomList.remove(0);
					geomList.addAll(intersection);
					geomList.addAll(nonIntersection);
				}
				if(((count+1) == geomList.size()) && (checkNext == true)){
					//-- nothing more to test
					//   so we can add this geometry to the finalList
					withoutIntersection.add((Geometry)g2Test.clone());
					//-- we remove it from the list of items
					geomList.remove(0);
					checkNext = false;
				}
			}
		}
		//-- add the last (or only) one
		withoutIntersection.add(geomList.get(0));
		return withoutIntersection;
	}
	
	/**
	 * 
	 * @param gf the GeometryFactory that has a specific (fixed) precision (create the PrecisionModel and afterwards the GeometryFactory before hand)
	 * @param geomOld the Geometry is not allowed to be a geometry collection
	 * @return null if input was a geometry collection
	 */
	private static Geometry createGeometryWithFixedPrecision(
			GeometryFactory gf, Geometry geomOld) {
		if (geomOld instanceof Polygon){
			Polygon po = (Polygon)geomOld;
			LinearRing shell = gf.createLinearRing(po.getExteriorRing().getCoordinates());
			LinearRing[] holes = null;
			if (po.getNumInteriorRing() > 0){
				holes = new LinearRing[po.getNumInteriorRing()];
				for (int i = 0; i < po.getNumInteriorRing(); i++) {
					holes[i] = gf.createLinearRing(po.getInteriorRingN(i).getCoordinates());
				}
			}
			return gf.createPolygon(shell, holes);
		}
		else if (geomOld instanceof Point){
			return gf.createPoint(((Point)geomOld).getCoordinate());
		}
		else if (geomOld instanceof LineString){
			return gf.createLineString(((LineString)geomOld).getCoordinates());
		}
		else{
			return null;
		}
	}
	
	/**
	 * evaluates if two geometries intersect. E.g. for two polygons it evaluates if 
	 * the intersection is an area and not just a line. \n
	 * This method is used since <i>area1.intersection(area2)</i> does return also a positive result
	 * if the intersection is on the boundary (i.e. no intersection of the interior).\n
	 * Note1: the order of how the geometries are provided matters! It tested such that 
	 * <i>g1.SpatialCritearia(g2)</i>. NOTE 2: the function has not been checked for mixed
	 * geometry types
	 * 
	 * @param g1
	 * @param g2
	 * @return true if more a geometry is split in at least 2 parts
	 * 
	 * @TODO: enable check of mixed geometries. 
	 */
	public static boolean checkIntersectionByGeomTypeB(Geometry g1, Geometry g2){
		//-- g1 is Polygon
		if (g1 instanceof Polygon){
			Geometry intersection = g1.intersection(g2);
			if(g2 instanceof Polygon){
				Geometry diff = g1.difference(g2);
				if (intersection.getArea() > 0){
						return true;
				}
				else{
					return false;
				}
			}
			else if(g2 instanceof LineString){
				Geometry diff = g1.difference(g2);
				if (diff instanceof GeometryCollection){
					if (((GeometryCollection)diff).getNumGeometries() > 1){
						return true;
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}
			else{//could be geometry collection or point
				if (g1.intersects(g2)){
					return true;
				}
				else{
					return false;
				}	
			}
		}
		//-- g1 is LineString
		else if (g1 instanceof LineString){
			if(g2 instanceof Polygon){
				Geometry diff = g2.difference(g1);
				if (diff instanceof GeometryCollection){
					if (((GeometryCollection)diff).getNumGeometries() > 1){
						return true;
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}
			else if(g2 instanceof LineString){
				Geometry diff = g1.difference(g2);
				if (diff instanceof GeometryCollection){
					if (((GeometryCollection)diff).getNumGeometries() > 1){
						return true;
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}
			else if(g2 instanceof Point){
				Geometry diff = g1.difference(g2);
				if (diff instanceof GeometryCollection){
					if (((GeometryCollection)diff).getNumGeometries() > 1){
						return true;
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}
			else{//could be geometry collection
				if (g1.intersects(g2)){
					return true;
				}
				else{
					return false;
				}	
			}
		}
		else if (g1 instanceof Point){
			if (g2 instanceof LineString){
				 //we don't want to test points with anything else
				  //as the intersection can only be a point
				  //but we may want to to test linestrings against points
				  //as this may result in two resulting linestrings
				Geometry diff = g2.difference(g2);
				if (((GeometryCollection)diff).getNumGeometries() > 1){
					return true;
				}
				else{
					return false;
				}
			}
			else{
				return false;
			}
		}
		//-- otherwise
		else{//could be geometry collection
			if (g1.intersects(g2)){
				return true;
			}
			else{
				return false;
			}	
		}
	}
	
	/**
	 * evaluates if two polygon geometries intersect. E.g. for two polygons it evaluates if 
	 * the intersection is an area and not just a line. \n
	 * This method is used since <i>area1.intersection(area2)</i> does return also a positive result
	 * if the intersection is on the boundary (i.e. no intersection of the interior).\n
	 * Note1: the order of how the geometries are provided matters! It tested such that 
	 * <i>g1.SpatialCritearia(g2)</i>. NOTE 2: the function has not been checked for mixed
	 * geometry types
	 * 
	 * @param g1
	 * @param g2
	 * @return true if area(intersection) > 0 
	 * 
	 * @TODO: enable check of mixed geometries. 
	 */
	public static boolean checkPolygonIntersection(Geometry g1, Geometry g2){
		//-- g1 is Polygon
		if (g1 instanceof Polygon){
			Geometry intersection = g1.intersection(g2);
			if(g2 instanceof Polygon){
				Geometry diff = g1.difference(g2);
				if (intersection.getArea() > 0){
						return true;
				}
				else{
					return false;
				}
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}	

	/**
	 * Nodes a collection of linestrings.
	 * Noding is done via JTS union, which is reasonably effective but
	 * may exhibit robustness failures. \n
	 * note: the method is taken from PolygonizerPlugIn
	 *
	 * @param lines the linear geometries to node
	 * @return a collection of linear geometries, noded together
	 */
	public static Collection nodeLines(Collection lines)
	{
		GeometryFactory fact = new GeometryFactory();
		Geometry linesGeom = fact.createMultiLineString(fact.toLineStringArray(lines));

		Geometry unionInput  = fact.createMultiLineString(null);
		// force the unionInput to be non-empty if possible, to ensure union is not optimized away
		Geometry point = extractPoint(lines);
		if (point != null)
			unionInput = point;

		Geometry noded = linesGeom.union(unionInput);
		List nodedList = new ArrayList();
		nodedList.add(noded);
		return nodedList;
	}

	private static Geometry extractPoint(Collection lines)
	{
		int minPts = Integer.MAX_VALUE;
		Geometry point = null;
		// extract first point from first non-empty geometry
		for (Iterator i = lines.iterator(); i.hasNext(); ) {
			Geometry g = (Geometry) i.next();
			if (! g.isEmpty()) {
				Coordinate p = g.getCoordinate();
				point = g.getFactory().createPoint(p);
			}
		}
		return point;
	}
}
