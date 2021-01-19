package org.openjump.core.geomutils.algorithm;

import java.util.ArrayList;
import java.util.Iterator;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class GeometryConverter {

	/**
	 * 
	 * @param polygon a Polygon or MultiPolygon
	 * @return if the input Geometry is neither a Polygon nor a MultiPolygon it is returned
	 */
	public static ArrayList transformPolygonToLineStrings(Geometry polygon){
		ArrayList<Geometry> inGeoms = new ArrayList();
		ArrayList<Geometry> lines = new ArrayList();
		GeometryFactory gf = new GeometryFactory();
		if (polygon instanceof MultiPolygon){
			inGeoms.addAll(explodeGeomsIfMultiG(polygon));
		}
		else if(polygon instanceof Polygon) {
			inGeoms.add(polygon);
		}
		else{
			//-- all other geometry types are ignored and returned as is
			inGeoms.add(polygon);
			return inGeoms;
		}
		for (Iterator iterator = inGeoms.iterator(); iterator.hasNext();) {
			Polygon poly = (Polygon) iterator.next();
			lines.add(gf.createLineString(poly.getExteriorRing().getCoordinates()));
			if (poly.getNumInteriorRing() > 0){
				for (int i = 0; i < poly.getNumInteriorRing(); i++) {
					lines.add(gf.createLinearRing(poly.getInteriorRingN(i).getCoordinates()));
				}
			}
		}
		return lines;
	}
	
	
	/**
	 * The method explodes a geometry, if it is a multi-geometry (Geometry Collection), into their parts. 
	 * @param geom the geometry to explode
	 * @return a list of geometries
	 */
	public static ArrayList<Geometry> explodeGeomsIfMultiG(Geometry geom){
		ArrayList<Geometry> geoms = new ArrayList<Geometry>(); 
		if (geom instanceof GeometryCollection){
			//System.out.println("explode multigeoms");
			GeometryCollection multig = (GeometryCollection)geom;
			for (int i = 0; i < multig.getNumGeometries(); i++) {
				Geometry g = (Geometry) multig.getGeometryN(i);
				geoms.add(g);
			}
		}
		else{
			geoms.add(geom);
		}
		return geoms;
	}
}
