
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionMap;

/**
 * A better name for this class would have been TriangleMapFactory. Given the 
 * coordinates of an initial and final triangulation, it will return a map of source 
 * Triangle to destination Triangle.
 * 
 *  Creates a FeatureCollection of triangles covering a given area. Thin
 *  triangles are avoided. <p>
 *
 *  Coordinates are not created, modified, or discarded. Thus, the triangles
 *  created will be composed of the Coordinates passed in to the Triangulator.
 *
 *  See White, Marvin S., Jr. and Griffin, Patricia. 1985. Piecewise linear
 *  rubber-sheet map transformation. "The American Cartographer" 12:2,
 *  123-31.
 */
public class Triangulator {
    private GeometryFactory factory = new GeometryFactory();
    private Collection ignoredVectors = new ArrayList();
    public Triangulator() {}

    /**
     * Splits two regions into Triangles. The two regions are called the
     * "source quadrilateral" and "destination quadrilateral", and are based on
     * the given dataset envelope. The "source quadrilateral" is the dataset envelope
     * expanded 5% along each margin. The "destination quadrilateral" is the
     * source quadrilateral with each vertex shifted according to the vector with
     * the nearest tail. The source quadrilateral is split using the vector tails;
     * the destination quadrilateral is split using the vector tips. In this way,
     * the vectors map the source Triangles to the destination Triangles.
     * @param datasetEnvelope the region to triangulate
     * @param vectorLineStrings vectors (2-point LineStrings) whose tails and tips split
     * the "source quadrilateral" and "destination quadrilateral" into triangles
     * @param monitor
     * @return    a map of source Triangles to destination Triangles
     */
    public Map triangleMap(
        Envelope datasetEnvelope,
        Collection vectorLineStrings,
        TaskMonitor monitor) {
        return triangleMap(
            datasetEnvelope,
            vectorLineStrings,
            new ArrayList(),
            new ArrayList(),
            monitor);
    }

    /**
     * @param sourceHints "far-away" Coordinates (even outside the dataset envelope) for which
     * we must ensure that source triangles include. 
     * @param destinationHints "far-away" Coordinates for which we must ensure that destination
     * triangles include
     */
    public Map triangleMap(
        Envelope datasetEnvelope,
        Collection vectorLineStrings,
        Collection sourceHints,
        Collection destinationHints,
        TaskMonitor monitor) {            
        ArrayList vectorListCopy = new ArrayList(vectorLineStrings);
        ignoredVectors = nonVectors(vectorListCopy);
        vectorListCopy.removeAll(ignoredVectors);

        //Refinement on White & Griffin's algorithm: Bring outlying vectors back inside
        //by gradually increasing the size of the source quad. This is a courtesy to
        //the caller because really there shouldn't be any outlying vectors. [Jon Aquino]
        Assert.isTrue(!datasetEnvelope.isNull());
        Envelope sourceEnvelope = new Envelope(datasetEnvelope);
        Quadrilateral sourceQuad;
        Quadrilateral destQuad;
//    	int count=0;
        while (true) {
            //#sourceQuad will grow the envelope by 5%. [Jon Aquino]
            sourceQuad = sourceQuad(sourceEnvelope);
            destQuad = destQuad(sourceQuad, vectorListCopy);
            //sstein[30.March.2008] -- note.. this loop will run endless 
            //     if we try to warp a single point. therefore we check if
            //     the envelope truly grows. It can't grow for dx=dy=0
            if ((sourceEnvelope.getWidth() == 0.0) && (sourceEnvelope.getHeight() == 0.0)){
            	break;
            }
            //-- 
            if (outlyingVectors(sourceQuad, destQuad, vectorListCopy).isEmpty()
                && sourceQuad.verticesOutside(sourceHints).isEmpty()
                && destQuad.verticesOutside(destinationHints).isEmpty()) {
                break;
            }
//            else{
//            	System.out.print("."); count=count+1;
//            	if ((count/50.0) == (Math.floor(count/50.0))){
//            		System.out.println(" " + count);
//            	}
//            }
            sourceEnvelope = sourceQuad.getEnvelope();
        }

        Quadrilateral taggedSourceQuad = tag(sourceQuad, destQuad);
        List taggedSourceTriangles =
            triangulate(taggedSourceQuad, taggedVectorVertices(false, vectorListCopy), monitor);

        return triangleMap(taggedSourceTriangles);
    }

    /**
     * Permits the caller to identify which vectors were ignored because they
     * were not 2-point LineStrings
     */
    public Collection getIgnoredVectors() {
        return Collections.unmodifiableCollection(ignoredVectors);
    }
   
    public static Collection nonVectors(Collection geometries) {
        TreeSet nonVectors = new TreeSet();
        for (Iterator i = geometries.iterator(); i.hasNext();) {
            Geometry g = (Geometry) i.next();
            if (vector(g)) {
                continue;
            }
            nonVectors.add(g);
        }
        return nonVectors;    
    }

    public static boolean vector(Geometry g) {
        return (g.getClass() == LineString.class) && (((LineString) g).getNumPoints() == 2);
    }

    /**
     * @return vectors with the tail outside sourceQuad or the
     * tip outside destQuad
     */
    private TreeSet outlyingVectors(
        Quadrilateral sourceQuad,
        Quadrilateral destQuad,
        Collection vectors) {
        TreeSet outliers = new TreeSet();
        outliers.addAll(
            toVectors(sourceQuad.verticesOutside(taggedVectorVertices(false, vectors)), false));
        outliers.addAll(
            toVectors(destQuad.verticesOutside(taggedVectorVertices(true, vectors)), true));
        return outliers;
    }

    /**
     *  The intent of this method is to avoid narrow triangles, which create near
     *  singularities.
     *
     *@param  PQS  a triangle sharing an edge with QRS; vertex order is irrelevant
     *@return      (PQS and QRS) or (PQR, PRS), whichever pair has the largest
     *      minimum height
     */
    protected List heightMaximizedTriangles(Triangle PQS, Triangle QRS) {
        List originalTriangles = Arrays.asList(new Triangle[] { PQS, QRS });
        List alternativeTriangles = alternativeTriangles(PQS, QRS);

        if (alternativeTriangles == null) {
            return originalTriangles;
        }

        Triangle t1 = (Triangle) alternativeTriangles.get(0);
        Triangle t2 = (Triangle) alternativeTriangles.get(1);

        if (Math.min(PQS.getMinHeight(), QRS.getMinHeight())
            > Math.min(t1.getMinHeight(), t2.getMinHeight())) {
            return originalTriangles;
        } else {
            return alternativeTriangles;
        }
    }

    /**
     *@return    the triangle containing p, or null if no triangle contains p
     */
    protected Triangle triangleContaining(Coordinate p, List triangles) {
        for (Iterator i = triangles.iterator(); i.hasNext();) {
            Triangle triangle = (Triangle) i.next();

            if (triangle.contains(p)) {
                return triangle;
            }
        }

        return null;
    }

    /**
     *@return    a + the displacement represented by vector
     */
    protected Coordinate add(Coordinate a, LineString vector) {
        return new Coordinate(
            (a.x + vector.getCoordinateN(1).x) - vector.getCoordinateN(0).x,
            (a.y + vector.getCoordinateN(1).y) - vector.getCoordinateN(0).y);
    }

    protected LineString vectorWithNearestTail(Coordinate x, List vectors) {
        Assert.isTrue(vectors.size() > 0);

        LineString vectorWithNearestTail = (LineString) vectors.get(0);

        for (Iterator i = vectors.iterator(); i.hasNext();) {
            LineString candidate = (LineString) i.next();

            if (candidate.getCoordinateN(0).distance(x)
                < vectorWithNearestTail.getCoordinateN(0).distance(x)) {
                vectorWithNearestTail = candidate;
            }
        }

        return vectorWithNearestTail;
    }

    /**
     *@return    sourceQuad wrapped in TaggedCoordinates pointing to the
     *      corresponding Coordinates in destQuad.
     */
    protected Quadrilateral tag(Quadrilateral sourceQuad, Quadrilateral destQuad) {
        return new Quadrilateral(
            new TaggedCoordinate(sourceQuad.getP1(), destQuad.getP1()),
            new TaggedCoordinate(sourceQuad.getP2(), destQuad.getP2()),
            new TaggedCoordinate(sourceQuad.getP3(), destQuad.getP3()),
            new TaggedCoordinate(sourceQuad.getP4(), destQuad.getP4()));
    }

    /**
     *@param  PQS  a triangle sharing an edge with QRS; vertex order is irrelevant
     *@return      triangles PQR and PRS, or null if PQRS is not convex
     */
    protected List alternativeTriangles(Triangle PQS, Triangle QRS) {
        Quadrilateral quad = dissolve(PQS, QRS);

        if (!quad.isConvex()) {
            return null;
        }

        return quad.triangles();
    }

    /**
     *@return    a rectangle 5% larger along each margin
     *@see       White and Griffin's paper
     */
    private Quadrilateral sourceQuad(Envelope datasetEnvelope) {
        double dx = datasetEnvelope.getWidth() * 0.05;
        double dy = datasetEnvelope.getHeight() * 0.05;

        return new Quadrilateral(
            new Coordinate(datasetEnvelope.getMinX() - dx, datasetEnvelope.getMinY() - dy),
            new Coordinate(datasetEnvelope.getMaxX() + dx, datasetEnvelope.getMinY() - dy),
            new Coordinate(datasetEnvelope.getMaxX() + dx, datasetEnvelope.getMaxY() + dy),
            new Coordinate(datasetEnvelope.getMinX() - dx, datasetEnvelope.getMaxY() + dy));
    }

    /**
     *  Modifies the triangle list to accomodate the new vertex.
     */
    private void triangulate(List triangles, Coordinate newVertex) {
        Triangle triangleContainingNewVertex = triangleContaining(newVertex, triangles);
        Assert.isTrue(triangleContainingNewVertex != null);
        triangles.remove(triangleContainingNewVertex);

        //Don't add triangles immediately, as we want #adjacentTriangle to return
        //a triangle that isn't one of the split triangles. [Jon Aquino]
        ArrayList trianglesToAdd = new ArrayList();

        for (Iterator i = triangleContainingNewVertex.subTriangles(newVertex).iterator();
            i.hasNext();
            ) {
            Triangle newTriangle = (Triangle) i.next();
            Triangle adjacentTriangle = adjacentTriangle(newTriangle, triangles);

            if (adjacentTriangle == null) {
                //that is, a boundary triangle [Jon Aquino]
                trianglesToAdd.add(newTriangle);
            } else {
                triangles.remove(adjacentTriangle);
                trianglesToAdd.addAll(heightMaximizedTriangles(newTriangle, adjacentTriangle));
            }
        }

        triangles.addAll(trianglesToAdd);
    }

    /**
     *@return    the triangle adjacent to the given triangle, or null if there is
     *      none
     */
    private Triangle adjacentTriangle(Triangle triangle, List triangles) {
        for (Iterator i = triangles.iterator(); i.hasNext();) {
            Triangle candidate = (Triangle) i.next();
            int vertexMatches = 0;

            if (candidate.hasVertex(triangle.getP1())) {
                vertexMatches++;
            }

            if (candidate.hasVertex(triangle.getP2())) {
                vertexMatches++;
            }

            if (candidate.hasVertex(triangle.getP3())) {
                vertexMatches++;
            }

            Assert.isTrue(vertexMatches != 3, candidate + "; " + triangle);

            if (vertexMatches == 2) {
                return candidate;
            }
        }

        return null;
    }

    /**
     *@return    sourceQuad, with each vertex shifted according to the vector with
     *      the nearest tail
     *@see       White and Griffin's paper
     */
    private Quadrilateral destQuad(Quadrilateral sourceQuad, List vectors) {
        if (vectors.isEmpty()) { return (Quadrilateral) sourceQuad.clone(); }
        return new Quadrilateral(
            addVectorWithNearestTail(sourceQuad.getP1(), vectors),
            addVectorWithNearestTail(sourceQuad.getP2(), vectors),
            addVectorWithNearestTail(sourceQuad.getP3(), vectors),
            addVectorWithNearestTail(sourceQuad.getP4(), vectors));
    }

    private Coordinate addVectorWithNearestTail(Coordinate x, List vectors) {
        return add(x, vectorWithNearestTail(x, vectors));
    }

    /**
     *@param  quad           quadrilateral region to triangulate
     *@param  vertices       triangle vertices; Coordinate objects, all within the
     *      quadrilateral region (use #containsAll to check)
     *@return                the triangles; Triangle objects
     *@throws  JUMPException  if one or more vertices are outside the quadrilateral
     *      region
     */
    private List triangulate(Quadrilateral quad, List vertices, TaskMonitor monitor) {
        monitor.allowCancellationRequests();
        monitor.report("Triangulating...");

        List triangles = quad.triangles();
        int count = 0;

        for (Iterator i = vertices.iterator(); i.hasNext() && !monitor.isCancelRequested();) {
            Coordinate vertex = (Coordinate) i.next();
            triangulate(triangles, vertex);
            count++;
            monitor.report(count, vertices.size(), "vectors");
        }

        return triangles;
    }

    /**
     *  The returned Coordinates will be tagged with the tails if the tips are
     *  requested (or the tips, if the tails are requested).
     *
     *@param  tips  true to return the vector tips; otherwise, the tails
     */
    public static List taggedVectorVertices(boolean tips, Collection vectors) {
        ArrayList taggedVectorVertices = new ArrayList();

        for (Iterator i = vectors.iterator(); i.hasNext();) {
            LineString vector = (LineString) i.next();
            taggedVectorVertices.add(
                new TaggedCoordinate(
                    tips ? vector.getCoordinateN(1) : vector.getCoordinateN(0),
                    tips ? vector.getCoordinateN(0) : vector.getCoordinateN(1)));
        }

        return taggedVectorVertices;
    }

    private Map triangleMap(List taggedSourceTriangles) {
        HashMap triangleMap = new HashMap();

        for (Iterator i = taggedSourceTriangles.iterator(); i.hasNext();) {
            Triangle sourceTriangle = (Triangle) i.next();
            triangleMap.put(
                sourceTriangle,
                new Triangle(
                    ((TaggedCoordinate) sourceTriangle.getP1()).getTag(),
                    ((TaggedCoordinate) sourceTriangle.getP2()).getTag(),
                    ((TaggedCoordinate) sourceTriangle.getP3()).getTag()));
        }

        return triangleMap;
    }

    /**
     * @param tips true if c is the tip and c's tag is the tail; false if
     * c is the tail and c's tag is the tip
     */
    private LineString toVector(TaggedCoordinate c, boolean tips) {
        //Constructor requires the tail followed by the tip.
        return factory.createLineString(
            new Coordinate[] { tips ? c.getTag() : c, tips ? c : c.getTag()});
    }

    /**
     *  The first coordinate of the returned quadrilateral will be an "unshared"
     *  vertex; that is, one that is present in only one of the triangles.
     *
     *@param  PQS  a triangle that shares an edge with QRS. The order of the
     *      Coordinates does not matter.
     *@return      a quadrilateral (four Coordinates) formed from the two
     *      triangles
     */
    private Quadrilateral dissolve(Triangle PQS, Triangle QRS) {
        CollectionMap vertexListMap = new CollectionMap(TreeMap.class);
        vertexListMap.addItem(PQS.getP1(), PQS.getP1());
        vertexListMap.addItem(PQS.getP2(), PQS.getP2());
        vertexListMap.addItem(PQS.getP3(), PQS.getP3());
        vertexListMap.addItem(QRS.getP1(), QRS.getP1());
        vertexListMap.addItem(QRS.getP2(), QRS.getP2());
        vertexListMap.addItem(QRS.getP3(), QRS.getP3());

        ArrayList sharedVertices = new ArrayList();
        ArrayList unsharedVertices = new ArrayList();

        for (Iterator i = vertexListMap.keySet().iterator(); i.hasNext();) {
            Coordinate vertex = (Coordinate) i.next();

            if (vertexListMap.getItems(vertex).size() == 1) {
                unsharedVertices.add(vertex);
            } else if (vertexListMap.getItems(vertex).size() == 2) {
                sharedVertices.add(vertex);
            } else {
                Assert.shouldNeverReachHere();
            }
        }

        Assert.isTrue(2 == sharedVertices.size(), PQS + "; " + QRS);
        Assert.isTrue(2 == unsharedVertices.size(), PQS + "; " + QRS);

        return new Quadrilateral(
            (Coordinate) unsharedVertices.get(0),
            (Coordinate) sharedVertices.get(0),
            (Coordinate) unsharedVertices.get(1),
            (Coordinate) sharedVertices.get(1));
    }

    private TreeSet toVectors(Collection taggedVectorVertices, boolean tips) {
        TreeSet badVectors = new TreeSet();

        for (Iterator i = taggedVectorVertices.iterator(); i.hasNext();) {
            TaggedCoordinate c = (TaggedCoordinate) i.next();
            badVectors.add(toVector(c, tips));
        }

        return badVectors;
    }
}
