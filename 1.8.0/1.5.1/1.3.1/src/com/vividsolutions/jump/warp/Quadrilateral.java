
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

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


/**
 * A simple four-sided polygon.
 */
public class Quadrilateral implements Cloneable {
    private Coordinate p1;
    private Coordinate p2;
    private Coordinate p3;
    private Coordinate p4;
    private GeometryFactory factory = new GeometryFactory();
    public Envelope getEnvelope() {
        Envelope envelope = new Envelope(p1);
        envelope.expandToInclude(p2);
        envelope.expandToInclude(p3);
        envelope.expandToInclude(p4);
        return envelope;
    }
    
    protected Object clone() {
        return new Quadrilateral(new Coordinate(p1), new Coordinate(p2), new Coordinate(p3), new Coordinate(p4));
    }

    /**
     * Creates a Quadrilateral.
     * @param p1 one vertex
     * @param p2 another vertex
     * @param p3 another vertex
     * @param p4 another vertex
     */
    public Quadrilateral(Coordinate p1, Coordinate p2, Coordinate p3,
        Coordinate p4) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }

    private boolean diagonalsIntersect() {
        return Line2D.linesIntersect(p1.x, p1.y, p3.x, p3.y, p2.x, p2.y, p4.x,
            p4.y);
    }

    /**
     * Returns whether this Quadrilateral is a convex polygon.
     * @return whether the diagonals intersect
     */
    public boolean isConvex() {
        return diagonalsIntersect();
    }

    /**
     * Creates two Triangles from this Quadrilateral.
     * @return two Triangles: P1-P2-P3 and P1-P4-P3
     */
    public List triangles() {
        //Some callers depend on the fact that the two triangles are P1-P2-P3
        //and P1-P4-P3. [Jon Aquino]
        ArrayList triangles = new ArrayList();
        triangles.add(new Triangle(p1, p2, p3));
        triangles.add(new Triangle(p1, p4, p3));

        return triangles;
    }

    private String toString(Coordinate c) {
        return c.x + " " + c.y;
    }

    public String toString() {
        return "LINESTRING (" + toString(p1) + ", " + toString(p2) + ", " +
        toString(p3) + ", " + toString(p4) + ", " + toString(p1) + ")";
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
     * Returns the fourth vertex.
     * @return the fourth vertex
     */
    public Coordinate getP4() {
        return p4;
    }

    /**
     * Converts this Quadrilateral to a JTS Polygon.
     * @return a new JTS Polygon with one shell and no holes
     */
    public Polygon toPolygon() {
        return factory.createPolygon(factory.createLinearRing(
                new Coordinate[] { p1, p2, p3, p4, p1 }), null);
    }

    /**
     * Filters out points that lie inside this Quadrilateral
     * @param vertices points to check
     * @return those points which lie outside this Quadrilateral
     */
    public Collection verticesOutside(Collection vertices) {
        ArrayList outsideVertices = new ArrayList();
        Polygon quadrilateralPolygon = toPolygon();

        for (Iterator i = vertices.iterator(); i.hasNext();) {
            Coordinate vertex = (Coordinate) i.next();
            Point p = factory.createPoint(vertex);

            if (!quadrilateralPolygon.contains(p)) {
                outsideVertices.add(vertex);
            }
        }

        return outsideVertices;
    }
}
