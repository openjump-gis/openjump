package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.geom.AbstractGeometryProcessor;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Simplifier removing segments based on their size, and taking care to remove
 * the point with angles close to 0%PI
 */
public class RemoveSmallSegments extends AbstractGeometryProcessor {

    private double tolerance;
    private double minTolerance;
    private double squareTolerance;

    public RemoveSmallSegments(double tolerance) {
        this.tolerance = tolerance;
        this.squareTolerance = tolerance * tolerance;
        this.minTolerance = tolerance/16;
    }

    private RemoveSmallSegments(double tolerance, double minTolerance) {
        this.tolerance = tolerance;
        this.minTolerance = minTolerance;
        this.squareTolerance = tolerance * tolerance;
    }

    @Override
    public Geometry process(Geometry geometry) {
        Geometry geom = super.process(geometry);
        // Recursive with a tolerance always smaller until toleranceIni/32
        if (!geom.isValid() && tolerance > minTolerance) {
            return new RemoveSmallSegments(tolerance/2.0, minTolerance).process(geometry);
        } else return geom;
    }

    @Override
    public void process(LineString line, List<Geometry> list) {
        CoordinateList cl = new CoordinateList(line.getCoordinates(), false);
        cl = simplifies(cl, tolerance, false);
        if (cl.size() < 2) System.out.println("" + line + " -> " + cl.getCoordinate(0));
        list.add(line.getFactory().createLineString(cl.toCoordinateArray()));
    }

    @Override
    public LinearRing process(LinearRing ring) {
        CoordinateList cl = new CoordinateList(ring.getCoordinates(), false);
        cl.remove(cl.size()-1);
        cl = simplifies(cl, tolerance, true);
        cl.closeRing();
        return ring.getFactory().createLinearRing(cl.toCoordinateArray());
    }

    // Try an iterative process
    private CoordinateList simplifies(CoordinateList cl, double tolerance, boolean closed) {
        int size, index;
        int min = closed ? 4 : 2;
        while ((size = cl.size()) > min && (index = getShortestSegment(cl, tolerance, closed)) > -1) {
            double a0, a1;
            if (closed) {
                Coordinate prev = cl.getCoordinate((size+index-1)%size);
                Coordinate cur0 = cl.getCoordinate((size+index)%size);
                Coordinate cur1 = cl.getCoordinate((size+index+1)%size);
                Coordinate next = cl.getCoordinate((size+index+2)%size);
                a0 = sin2AOB(prev, cur0, cur1);
                a1 = sin2AOB(cur0, cur1, next);
            } else {
                a0 = index == 0 ? 1.0 : sin2AOB(cl.getCoordinate(index-1),
                        cl.getCoordinate(index), cl.getCoordinate(index+1));
                a1 = index == size-2 ? 1.0 : sin2AOB(cl.getCoordinate(index),
                        cl.getCoordinate(index+1), cl.getCoordinate(index+2));
            }
            if (a0 < a1) cl.remove(index);
            else cl.remove((index+1)%size);
        }
        return cl;
    }

    private int getShortestSegment(CoordinateList cl, double tolerance, boolean closed) {
        int index = -1;
        double shortestLength = tolerance;
        int size = cl.size();
        int max = closed ? size : size-1;
        for (int i = 0 ; i < max ; i++) {
            double length = cl.getCoordinate(i).distance(cl.getCoordinate((i+1)%size));
            if (length < shortestLength) {
                index = i;
                shortestLength = length;
            }
        }
        return index;
    }

    // Compare square length to check if tis is a micro segment
    // Comparing squares avoid a square root calculation which is longer
    private boolean isMicro(Coordinate c0, Coordinate c1) {
        double dx = c1.x - c0.x;
        double dy = c1.y - c0.y;
        return dx*dx + dy*dy < squareTolerance;
    }

    // Returns the square sinus of angle A-O-B
    // This method is used to remove the point of the micro-segment with the
    // most flat angle
    private double sin2AOB(Coordinate A, Coordinate O, Coordinate B) {
        double dxa = A.x - O.x;
        double dya = A.y - O.y;
        double dxb = B.x - O.x;
        double dyb = B.y - O.y;
        return (dxa*dyb-dya*dxb)*(dxa*dyb-dya*dxb)/(dxa*dxa+dya*dya)/(dxb*dxb+dyb*dyb);
    }

    private double cos2OAOB(Coordinate A, Coordinate O, Coordinate B) {
        double oax = A.x - O.x;
        double oay = A.y - O.y;
        double obx = B.x - O.x;
        double oby = B.y - O.y;
        return (oax*obx + oay*oby) * (oax*obx + oay*oby) / (oax*oax+oay*oay)/(obx*obx+oby*oby);
    }
}
