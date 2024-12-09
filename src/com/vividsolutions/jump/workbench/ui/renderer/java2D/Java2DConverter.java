
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

package com.vividsolutions.jump.workbench.ui.renderer.java2D;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.*;

/**
 * Converts JTS Geometry objects into Java 2D Shape objects
 */
 // Optimizations from larry becker's SkyJUMP code to OpenJUMP [mmichaud]
 // 1 - Added point decimation to toViewCoordinates.
 // Results in much  improved render speeds when displaying full extent of large datasets.
 // 2 - Optimized the toShape conversion for linestrings. 
 // Reduced draw times by 60%.
 // 3 - Made toViewCoordinates(Coordinate[]) public to make use
 // of its decimation optimization in AbstractSelectionRenderer.
 // 4 - New optimizations from mmichaud on 2024-11 for components (ex. holes) outside viewport
 // or having a subpixel size.
public class Java2DConverter {

  private static double POINT_MARKER_SIZE = 3.0;

  private final PointConverter pointConverter;
  private final static GeneralPath emptyPath = new GeneralPath();

  // Add the resolution of the decimator as an option to be able to choose more easily
  // between speed and quality (ex. speed is preferred for light-gray display while
  // dragging the zoombar or while using the mousewheel) [mmichaud 2007-05-27]
  // Default resolution for the decimator is half a pixel as discussed on the list
  private double decimatorResolution = 0.5;


  /**
   * Create a Java2DConverter based on pointConverter and using a default resolution
   * of 0.5 pixel for decimation.
   * @param pointConverter the PointConverter to convert coordinates from Model to View
   */
  public Java2DConverter(PointConverter pointConverter) {
    this.pointConverter = pointConverter;
  }

  /**
   * Create a coordinate converter using a custom resolution for decimation.
   * @param pointConverter the PointConverter to convert coordinates from Model to View
   * @param resolution resolution in pixels for decimation
   */
  public Java2DConverter(PointConverter pointConverter, double resolution) {
    this.pointConverter = pointConverter;
    this.decimatorResolution = resolution;
  }

  private Shape toShape(Polygon p) throws NoninvertibleTransformException {
    // [mmichaud 2024-11-16] skip holes not intersecting the view and simplify those entirely
    // included in a pixel
    double modelResolution = decimatorResolution / pointConverter.getScale();
    Envelope viewEnvelopeInModelCoordinate = pointConverter.getEnvelopeInModelCoordinates();

    // short circuit to draw a very short line if the Polygon is entirely inside a pixel
    Envelope geomEnvelope = p.getEnvelopeInternal();
    if (geomEnvelope.getWidth() < modelResolution && geomEnvelope.getHeight() < modelResolution) {
      Coordinate c = p.getCoordinate();
      return toShape(p.getFactory().createLineString(
          new Coordinate[] {c, new Coordinate(c.x + modelResolution, c.y)}
      ));
    }

    // Return a GeometryCollectionShape to handle case where small holes are represented as micro-segments
    GeometryCollectionShape gcs = new GeometryCollectionShape();
    GeneralPath microholes = new GeneralPath();
    List<Coordinate[]> holeVertexCollection = new ArrayList<>();

    for (int j = 0; j < p.getNumInteriorRing(); j++) {
      LinearRing ring = p.getInteriorRingN(j);
      geomEnvelope = ring.getEnvelopeInternal();
      // skip holes outside view
      if (!geomEnvelope.intersects(viewEnvelopeInModelCoordinate)) continue;
      // skip holes < 1/10 pixel to keep small scale display fast with hundreds of thousands of holes
      // (simplifying geometries is fine, but it unexpectedly gets longer at smaller scales)
      if (geomEnvelope.getWidth() < modelResolution/10 && geomEnvelope.getHeight() < modelResolution/10) continue;
      // for larger holes (still < 1 pixel), display a simple 1 pixel size segment
      if (geomEnvelope.getWidth() < modelResolution && geomEnvelope.getHeight() < modelResolution) {
        Coordinate c = ring.getCoordinate();
        microholes.append(new LineStringPath(ring.getFactory().createLineString(
            new Coordinate[]{c, new Coordinate(c.x + modelResolution, c.y)}), this), false);
      }
      // else, display the linear ring (eventually decimated by toViewCoordinates)
      else {
        holeVertexCollection.add(toViewCoordinates(ring.getCoordinates()));
      }
    }
    // polygonal shape
    gcs.add(new PolygonShape(toViewCoordinates(p.getExteriorRing().getCoordinates()), holeVertexCollection));
    // add simplified version of micro holes
    gcs.add(microholes);
    return gcs;
  }

  public Coordinate[] toViewCoordinates(Coordinate[] modelCoordinates)
    throws NoninvertibleTransformException {
    double modelResolution = decimatorResolution / pointConverter.getScale();
    CoordinateList viewCoordinates = new CoordinateList();
    Coordinate p0 = modelCoordinates[0];
    int mpts = modelCoordinates.length;
    for (int i = 0; i < mpts; i++) {
      Coordinate pi = modelCoordinates[i];
      //inline Decimator
      double xd = Math.abs(p0.x-pi.x);
      double yd = Math.abs(p0.y-pi.y);
      if (xd>=modelResolution || yd>=modelResolution || viewCoordinates.size()<4 || i==mpts-1) {
        //LDB: have replaced the following with inline code but
        //     it was no faster.  AffineTransform must be highly optimized!
        Point2D point2D = pointConverter.toViewPoint(pi);
        viewCoordinates.add(new Coordinate(point2D.getX(), point2D.getY()));
        p0 = pi;
      }
    }
    return viewCoordinates.toCoordinateArray();
  }

  private Shape toShape(GeometryCollection gc)
        throws NoninvertibleTransformException {

    double modelResolution = decimatorResolution / pointConverter.getScale();
    Envelope viewEnvelopeInModelCoordinate = pointConverter.getEnvelopeInModelCoordinates();

    GeometryCollectionShape shape = new GeometryCollectionShape();
    for (int i = 0; i < gc.getNumGeometries(); i++) {
      Geometry g = gc.getGeometryN(i);
      Envelope geomEnvelope = g.getEnvelopeInternal();
      // short-circuit for component outside the view
      if (!geomEnvelope.intersects(viewEnvelopeInModelCoordinate)) continue;
      // simplify sub-pixel size geometry to a simple micro-segment
      if (g.getDimension() > 0 && geomEnvelope.getWidth() < modelResolution && geomEnvelope.getHeight() < modelResolution) {
        Coordinate c = g.getCoordinate();
        shape.add(toShape(g.getFactory().createLineString(
            new Coordinate[] {c, new Coordinate(c.x + modelResolution, c.y)}
        )));
      }
      // normal component
      shape.add(toShape(g));
    }

    return shape;
  }

//  private GeneralPath toShape(MultiLineString mls)
//    throws NoninvertibleTransformException {
//    Envelope viewInModelCoordinates = pointConverter.getEnvelopeInModelCoordinates();
//    double modelResolution = decimatorResolution / pointConverter.getScale();
//    GeneralPath path = new GeneralPath();
//
//    for (int i = 0; i < mls.getNumGeometries(); i++) {
//      LineString lineString = (LineString) mls.getGeometryN(i);
//      Envelope env = lineString.getEnvelopeInternal();
//      if (!env.intersects(viewInModelCoordinates)) continue;
//      if (env.getWidth() < modelResolution && env.getHeight() < modelResolution) {
//        Coordinate c = lineString.getCoordinate();
//        path.append(toShape(lineString.getFactory().createLineString(new Coordinate[]{
//            c, new Coordinate(c.x + modelResolution, c.y)
//        })), false);
//      }
//      else {
//        path.append(toShape(lineString), false);
//      }
//    }
//
//    //BasicFeatureRenderer expects LineStrings and MultiLineStrings to be
//    //converted to GeneralPaths. [Jon Aquino]
//    return path;
//  }

  static class LineStringPath implements PathIterator {

    private int iterate;
    private int numPoints;
    private Coordinate[] points;
    private boolean closed;

    public LineStringPath(LineString linestring, Java2DConverter j2D){
      try {
        points = j2D.toViewCoordinates(linestring.getCoordinates());
      }
      catch (NoninvertibleTransformException ignored){ }
      this.numPoints = points.length;
      iterate = 0;
      closed = (numPoints>1) && (points[0].equals2D(points[numPoints-1]));
    }

    private int getSegType(){
     // tip from Larry Becker for a better rendering 2007-07-13 [mmichaud]
     if (closed && (iterate == numPoints-1))
       return PathIterator.SEG_CLOSE;
     return (iterate==0) ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO;
    }

    public int currentSegment(double[] coords) {
      coords[0] = points[iterate].x;
      coords[1] = points[iterate].y;
      return getSegType();
    }
    public int currentSegment(float[] coords) {
      coords[0] = (float) points[iterate].x;
      coords[1] = (float) points[iterate].y;
      return getSegType();
    }
    public int getWindingRule() {
      return GeneralPath.WIND_NON_ZERO;
    }
    public boolean isDone() {
      return !(iterate < numPoints);
    }
    public void next() {
      iterate++;
    }

  }

  // New toShape method for LineString [mmichaud 2011-03-05]
  // This new method exclude all segments entirely out of the viewPort from
  // the general path
  private GeneralPath toShape(LineString lineString) {
      GeometryFactory gf = lineString.getFactory();
      Coordinate[] cc = lineString.getCoordinates();
      GeneralPath shape = new GeneralPath(GeneralPath.WIND_NON_ZERO, cc.length);
      CoordinateList list = new CoordinateList();
      Envelope viewEnvelopeInModelCoordinate = pointConverter.getEnvelopeInModelCoordinates();
      Coordinate previous = cc[0];
      boolean start = true; // start of a subpath intersecting the view
      for (int i = 1,  max = cc.length ; i < max ; i++) {
          Coordinate current = cc[i];
          // if current segment intersects view, add current coordinate
          if (viewEnvelopeInModelCoordinate.intersects(new Envelope(previous, current))) {
              if (start) list.add(previous); // add start point first if not yet there
              list.add(current);
              start = false;
          }
          // if current segment does not intersect view, add current list to path and clear the list
          else if (!list.isEmpty()) {
              PathIterator pi = new LineStringPath(gf.createLineString(list.toCoordinateArray()), this);
            shape.append(pi,false);
            list.clear();
            start = true;
          }
          else {
            // list is empty and current segment does not intersect the view : do nothing
          }
          // it is the last point and previous segment was intersecting the view : add current segment
          if (i == max-1 && !list.isEmpty()) {
              PathIterator pi = new LineStringPath(gf.createLineString(list.toCoordinateArray()), this);
            shape.append(pi,false);
          }
          previous = current;
      }
      return shape;
  }

  private Shape toShape(Point point) throws NoninvertibleTransformException {
    Rectangle2D.Double pointMarker =
        new Rectangle2D.Double(0.0, 0.0, POINT_MARKER_SIZE, POINT_MARKER_SIZE);
    Point2D viewPoint = toViewPoint(point.getCoordinate());
    pointMarker.x = viewPoint.getX() - (POINT_MARKER_SIZE / 2);
    pointMarker.y = viewPoint.getY() - (POINT_MARKER_SIZE / 2);

    return pointMarker;
  }

  private Point2D toViewPoint(Coordinate modelCoordinate)
    throws NoninvertibleTransformException {
    // Optimization recommended by Todd Warnes [Jon Aquino 2004-02-06]
    // viewPoint.setLocation(
    //        Math.round(viewPoint.getX()),
    //        Math.round(viewPoint.getY()));
    // Do the rounding now; don't rely on Java 2D rounding, because it
    // seems to do it differently for drawing and filling, resulting in the draw
    // being a pixel off from the fill sometimes. [Jon Aquino]
    // LDB 04/25/2007: this assumption doesn't seem to be true any longer
    return pointConverter.toViewPoint(modelCoordinate);

  }


  public interface PointConverter {
    Point2D toViewPoint(Coordinate modelCoordinate) throws NoninvertibleTransformException;
    double getScale() throws NoninvertibleTransformException;
    Envelope getEnvelopeInModelCoordinates();
  }


  /**
   * If you pass in a general GeometryCollection, note that a Shape cannot
   * preserve information about which elements are 1D and which are 2D.
   * For example, if you pass in a GeometryCollection containing a ring and a
   * disk, you cannot render them as such: if you use Graphics.fill, you'll get
   * two disks, and if you use Graphics.draw, you'll get two rings. Solution:
   * create Shapes for each element.
   */
  public Shape toShape(Geometry geometry)
    throws NoninvertibleTransformException {

    // short-circuit for empty geometry
    if (geometry.isEmpty()) {
      return emptyPath;
    }

    Envelope viewInModelCoordinates = pointConverter.getEnvelopeInModelCoordinates();
    Envelope env = geometry.getEnvelopeInternal();

    // short-circuit for geometry component not intersecting the view
    if (!env.intersects(viewInModelCoordinates)) return emptyPath;

    double modelResolution = decimatorResolution / pointConverter.getScale();

    // short-circuit for a sub-pixel size geometry of any kind : display a micro-segment (1 pixel long)
    if (geometry.getDimension() > 0 && env.getWidth() < modelResolution && env.getHeight() < modelResolution) {
      // [mmichaud - 2007-05-23] try a short-circuit, but finally remove it because of ugly rendering
      // [mmichaud - 2024-10-12] finally get an efficient short circuit without side effect
      Coordinate c = geometry.getCoordinate();
      return toShape(geometry.getFactory().createLineString(
          new Coordinate[] {c, new Coordinate(c.x + modelResolution, c.y)}
      ));
    }

    if (geometry instanceof Polygon) {
      return toShape((Polygon) geometry);
    }

    if (geometry instanceof LineString) {
      return toShape((LineString) geometry);
    }

    if (geometry instanceof Point) {
      return toShape((Point) geometry);
    }

    if (geometry instanceof GeometryCollection) {
      return toShape((GeometryCollection) geometry);
    }

    throw new IllegalArgumentException(
      "Unrecognized Geometry class: " + geometry.getClass());
  }
}
