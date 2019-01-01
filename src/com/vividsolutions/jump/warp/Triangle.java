
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.warp;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.util.Assert;


/**
 * A triangle, with special methods for use with BilinearInterpolatedTransform.
 * @see BilinearInterpolatedTransform
 */
public class Triangle {
    private static GeometryFactory factory = new GeometryFactory();
    private static Point2D hasher = new Point2D.Double();
    private SaalfeldCoefficients sc;
    private Coordinate p1;
    private Coordinate p2;
    private Coordinate p3;
    private int hashCode;
    private Envelope envelope = null;

    /**
     * Creates a Triangle.
     * @param p1 one vertex
     * @param p2 another vertex
     * @param p3 another vertex
     */
    public Triangle(Coordinate p1, Coordinate p2, Coordinate p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        Assert.isTrue(!p1.equals(p2), "p1 = " + p1 + "; p2 = " + p2);
        Assert.isTrue(!p2.equals(p3), "p1 = " + p1 + "; p2 = " + p2);
        Assert.isTrue(!p3.equals(p1), "p1 = " + p1 + "; p2 = " + p2);
        initHashCode();
        sc = saalfeldCoefficients();
    }

    /**
     * Returns the first vertex.
     * @return the first vertex
     */
    public Coordinate getP1() {
        return p1;
    }

    /**
     * Returns the second vertex.
     * @return the second vertex
     */
    public Coordinate getP2() {
        return p2;
    }

    /**
     * Returns the third vertex.
     * @return the third vertex
     */
    public Coordinate getP3() {
        return p3;
    }

    /**
     * Returns the smallest of this Triangle's three heights (as measured
     * perpendicularly from each side).
     * @return the smallest of this Triangle's three altitudes
     */
    public double getMinHeight() {
        return (2 * getArea()) / getMaxSideLength();
    }

    /**
     * Returns the area of the triangle.
     * See http://www.mathcs.emory.edu/~rudolf/math108/summ1-2-3/node7.html
     * @return the area of the triangle
     */
    public double getArea() {
        return 0.5 * Math.abs(((p2.x - p1.x) * (p3.y - p1.y)) -
            ((p2.y - p1.y) * (p3.x - p1.x)));
    }

    /**
     * Returns the length of this Triangle's longest side.
     * @return the length of this Triangle's longest side
     */
    public double getMaxSideLength() {
        return Math.max(Point2D.distance(p1.x, p1.y, p2.x, p2.y),
            Math.max(Point2D.distance(p2.x, p2.y, p3.x, p3.y),
                Point2D.distance(p3.x, p3.y, p1.x, p1.y)));
    }

    /**
     * Converts this Triangle to a JTS Geometry.
     * @return a LinearRing with the same vertices as this Triangle
     */
    public LinearRing toLinearRing() {
        //<<TODO:IMPROVE>> Why not return a LinearRing rather than a general
        //Geometry? [Jon Aquino]
        return factory.createLinearRing(new Coordinate[] { p1, p2, p3, p1 });
    }

    public String toString() {
        return toLinearRing().toString();
    }

    /**
     * Returns whether this Triangle contains the given coordinate
     * @param p the point to test for containment
     * @return whether this Triangle contains the given coordinate
     */
    public boolean contains(Coordinate p) {
        if (p.equals(p1) || p.equals(p2) || p.equals(p3)) {
            return true;
        }
        
        //Unfortunately we cannot use Saalfeld's point-in-triangle test because it
        //is not robust (see TriangulatorTestCase#testContains2) [Jon Aquino]
        
        //Can't simply use != because if one is 1 and the other is 0 that's OK. [Jon Aquino]
        if (CGAlgorithms.computeOrientation(p1, p2, p) == - CGAlgorithms.computeOrientation(p2, p3, p)) {
            return false;
        }
        
        if (CGAlgorithms.computeOrientation(p1, p2, p) == - CGAlgorithms.computeOrientation(p3, p1, p)) {
            return false;
        }        
        
        return true;        
    }

    /**
     * Returns whether this Triangle has the same vertices as the given Triangle
     * @param o another Triangle; otherwise, equals will return false
     * @return true if o is a Triangle and has the same vertices (though not
     * necessarily in the same order)
     */
    public boolean equals(Object o) {
        if (!(o instanceof Triangle)) {
            return false;
        }

        Triangle other = (Triangle) o;

        return other.hasVertex(p1) && other.hasVertex(p2) &&
        other.hasVertex(p3);
    }

    /**
     * Returns whether v is one of this Triangle's vertices.
     * @param v the candidate point
     * @return whether v is equal to one of the vertices of this Triangle
     */
    public boolean hasVertex(Coordinate v) {
        return p1.equals(v) || p2.equals(v) || p3.equals(v);
    }

    public int hashCode() {
        return hashCode;
    }

    /**
     * Returns the three triangles that result from splitting this triangle at
     * a given point.
     * @param  newVertex  the split point, which must be inside triangle
     * @return three Triangles resulting from splitting this triangle at the
     * given Coordinate
     */
    public List subTriangles(Coordinate newVertex) {
        ArrayList<Triangle> triangles = new ArrayList<>();
        triangles.add(new Triangle(p1, p2, newVertex));
        triangles.add(new Triangle(p2, p3, newVertex));
        triangles.add(new Triangle(p3, p1, newVertex));

        return triangles;
    }

    protected Coordinate min(Coordinate a, Coordinate b) {
        return (a.compareTo(b) < 0) ? a : b;
    }

    private void initHashCode() {
        Coordinate min = min(min(p1, p2), p3);
        hasher.setLocation(min.x, min.y);
        hashCode = hasher.hashCode();
    }

    private SaalfeldCoefficients saalfeldCoefficients() {
        double T = ((p1.x * p2.y) + (p2.x * p3.y) + (p3.x * p1.y)) -
            (p3.x * p2.y) - (p2.x * p1.y) - (p1.x * p3.y);
        SaalfeldCoefficients sc = new SaalfeldCoefficients();
        sc.A1 = (p3.x - p2.x) / T;
        sc.B1 = (p2.y - p3.y) / T;
        sc.C1 = ((p2.x * p3.y) - (p3.x * p2.y)) / T;
        sc.A2 = (p1.x - p3.x) / T;
        sc.B2 = (p3.y - p1.y) / T;
        sc.C2 = ((p3.x * p1.y) - (p1.x * p3.y)) / T;

        return sc;
    }

    /**
     * Converts from a Euclidean coordinate to a simplicial coordinate.
     * @param euclideanCoordinate the Euclidean coordinate
     * @return a new 3D Coordinate with the corresponding simplicial values
     */
    public Coordinate toSimplicialCoordinate(Coordinate euclideanCoordinate) {
        //<<TODO>> Preserve the z-coordinate [Jon Aquino]
        double s1 = s1(euclideanCoordinate);
        double s2 = s2(euclideanCoordinate);
        double s3 = 1 - s1 - s2;

        return new Coordinate(s1, s2, s3);
    }

    /**
     * Converts from a simplicial coordinate to a Euclidean coordinate.
     * @param simplicialCoordinate the simplicial coordinate, which uses x, y, and z
     * @return a new Coordinate with the corresponding Euclidean values
     */
    public Coordinate toEuclideanCoordinate(Coordinate simplicialCoordinate) {
        return toEuclideanCoordinate(simplicialCoordinate.x,
            simplicialCoordinate.y, simplicialCoordinate.z);
    }

    private Coordinate toEuclideanCoordinate(double s1, double s2, double s3) {
        return new Coordinate((s1 * p1.x) + (s2 * p2.x) + (s3 * p3.x),
            (s1 * p1.y) + (s2 * p2.y) + (s3 * p3.y));
    }

    /**
     * Computes the first simplicial coordinate.
     * @param c a Euclidean coordinate
     * @return the first simplicial coordinate for the given Euclidean coordinate
     */
    private double s1(Coordinate c) {
        return (sc.A1 * c.y) + (sc.B1 * c.x) + (sc.C1);
    }

    /**
     * Computes the second simplicial coordinate.
     * @param c a Euclidean coordinate
     * @return the second simplicial coordinate for the given Euclidean coordinate
     */
    private double s2(Coordinate c) {
        return (sc.A2 * c.y) + (sc.B2 * c.x) + (sc.C2);
    }

    /**
     * Returns the bounds of this Triangle.
     * @return the smallest Envelope enclosing this Triangle
     */
    public Envelope getEnvelope() {
        if (envelope == null) {
            envelope = new Envelope(p1, p2);
            envelope.expandToInclude(p3);
        }

        return envelope;
    }

    private class SaalfeldCoefficients {
        double A1;
        double B1;
        double C1;
        double A2;
        double B2;
        double C2;
    }
}
