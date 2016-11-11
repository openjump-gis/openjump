package com.vividsolutions.jump.geom;

import com.vividsolutions.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of GeometryProcessor that implements a method to
 * recursively process GeometryCollections, and that let the user redefine
 * one or all of the methods to process simple geometries.
 * By default, simple geometries are added to the result, and the source
 * geometry is returned unchanged except that it is returned with its
 * simplest possible representation (ex. if the source geometry has nested
 * GeometryCollections, it will return a GeometryCollection containing
 * only simple components.
 * You can use this class to simply filter geometries (ex. eliminating
 * dimension 0 or dimension 1 components by not adding them to the list,
 * or to process some types of geometries.
 */
public class AbstractGeometryProcessor {

    /**
     * Main method taking an input geometry and returning a new Geometry which
     * may have different properties.
     */
    public Geometry process(Geometry geometry) {
        List<Geometry> list= new ArrayList<>();
        process(geometry, list);
        return geometry.getFactory().buildGeometry(list);
    }

    /**
     * Implementation of a recursion procedure exploring and processing all
     * simple components of the geometry parameter.
     * @param geometry input Geometry
     * @param list the list accumulating all processed simple component.
     */
    private void process(Geometry geometry, List<Geometry> list) {
        for (int i = 0; i < geometry.getNumGeometries() ; i++) {
            Geometry g = geometry.getGeometryN(i);
            if (g instanceof Point) process((Point)geometry.getGeometryN(i), list);
            else if (g instanceof LineString) process((LineString)geometry.getGeometryN(i), list);
            else if (g instanceof Polygon) process((Polygon)geometry.getGeometryN(i), list);
            else process(geometry.getGeometryN(i), list);
        }
    }

    /** Method to process Points. Override this method to transform punctal elements).*/
    public void process(Point point, List<Geometry> list) {
        Point clone = (Point)point.clone();
        process(clone.getCoordinateSequence());
        list.add(clone);
    }

    /** Method to process Points. Override this method to transform linear elements).*/
    public void process(LineString lineString, List<Geometry> list) {
        LineString clone = (LineString)lineString.clone();
        process(clone.getCoordinateSequence());
        list.add(clone);
    }

    /** Method to process Points. Override this method to transform linear elements).*/
    public LinearRing process(LinearRing linearRing) {
        LinearRing clone = (LinearRing)linearRing.clone();
        process(clone.getCoordinateSequence());
        return clone;
    }

    /** Method to process Points. Override this method to transform areal elements).*/
    public void process(Polygon polygon, List<Geometry> list) {
        LinearRing ext = process((LinearRing)polygon.getExteriorRing());
        LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];
        for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
            holes[i] = process((LinearRing)polygon.getInteriorRingN(i));
        }
        list.add(polygon.getFactory().createPolygon(ext, holes));
    }

    /** Method to change CoordinateSequences of a geometry - ex. simplifiers. */
    public void process(CoordinateSequence sequence) {
        int d = sequence.getDimension();
        for (int i = 0 ; i < sequence.size() ; i++) {
            double[] dd = new double[d];
            for (int j = 0 ; j < d ; j++) {
                dd[j] = sequence.getOrdinate(i, j);
            }
            dd = process(dd);
            for (int j = 0 ; j < d ; j++) {
                sequence.setOrdinate(i, j, dd[j]);
            }
        }
    }

    /** Method to change each single coordinate of a Geometry - ex. coord transformation. */
    public double[] process(double[] dd) {
        return dd;
    }
}
