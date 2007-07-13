
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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Converts JTS Geometry objects into Java 2D Shape objects
 */
 // Optimizations from larry becker's SkyJUMP code to OpenJUMP [mmichaud]
 // 1 - Added point decimation to toViewCoordinates.
 // Results in much  improved render speeds when displaying full extent of large datasets.
 // 2 - Optimized the toShape conversion for linestrings. 
 // Reduced darw times by 60%.
 // 3 - Made toViewCoordinates(Coordinate[]) public to make use
 // of its decimation optimization in AbstractSelectionRenderer.
public class Java2DConverter {
	private static double POINT_MARKER_SIZE = 3.0;
	private PointConverter pointConverter;
    // Add the resolution of the decimator as an option to be able to choose more easily
    // between speed and quality (ex. speed is preferred for light-gray display while
    // dragging the zoombar or while using the mousewheel) [mmichaud 2007-05-27]
    // Default resolution for the decimator is half a pixel as discussed on the list
    private double decimatorResolution = 0.5; 

	public Java2DConverter(PointConverter pointConverter) {
		this.pointConverter = pointConverter;
	}
    
    // Add a constructor to choose another  decimatorResolution
    public Java2DConverter(PointConverter pointConverter, double resolution) {
		this.pointConverter = pointConverter;
        this.decimatorResolution = resolution;
	}

	private Shape toShape(Polygon p) throws NoninvertibleTransformException {
		ArrayList holeVertexCollection = new ArrayList();

		for (int j = 0; j < p.getNumInteriorRing(); j++) {
			holeVertexCollection.add(
				toViewCoordinates(p.getInteriorRingN(j).getCoordinates()));
		}

		return new PolygonShape(
			toViewCoordinates(p.getExteriorRing().getCoordinates()),
			holeVertexCollection);
	}

	public Coordinate[] toViewCoordinates(Coordinate[] modelCoordinates)
		throws NoninvertibleTransformException {
		Coordinate[] viewCoordinates = new Coordinate[modelCoordinates.length];
        double ps = decimatorResolution / pointConverter.getScale();  // convert in model units
		Coordinate p0 = modelCoordinates[0];
		int npts = 0;
		int mpts = modelCoordinates.length;
		for (int i = 0; i < mpts; i++) {
			Coordinate pi = modelCoordinates[i];
			//inline Decimator
			double xd = Math.abs(p0.x-pi.x);
			double yd = Math.abs(p0.y-pi.y);
			if ((xd>=ps) || (yd>=ps) || (npts<4) || (i == mpts-1)) { 
				//LDB: have replaced the following with inline code but
				//     it was no faster.  AffineTransform must be highly optimized!
				Point2D point2D = pointConverter.toViewPoint(pi);
				viewCoordinates[npts++] = new Coordinate(point2D.getX(), point2D.getY());
				p0 = pi;
			}
		} 
		if (npts != mpts) {
			Coordinate[] viewCoordinates2 = new Coordinate[npts];
			for (int i = 0; i < npts; i++) {
					viewCoordinates2[i] = viewCoordinates[i];
			}
			return viewCoordinates2;
			// LDB: benchmarkes verify that the following line is slower than
			//      the above loop probably because of copying vs. referencing
			//return Arrays.copyOfRange(viewCoordinates,0,npts);
		}
		else
			return viewCoordinates;
	}

	private Shape toShape(GeometryCollection gc)
		throws NoninvertibleTransformException {
		GeometryCollectionShape shape = new GeometryCollectionShape();

		for (int i = 0; i < gc.getNumGeometries(); i++) {
			Geometry g = (Geometry) gc.getGeometryN(i);
			shape.add(toShape(g));
		}

		return shape;
	}

	private GeneralPath toShape(MultiLineString mls)
		throws NoninvertibleTransformException {
		GeneralPath path = new GeneralPath();

		for (int i = 0; i < mls.getNumGeometries(); i++) {
			LineString lineString = (LineString) mls.getGeometryN(i);
			path.append(toShape(lineString), false);
		}

		//BasicFeatureRenderer expects LineStrings and MultiLineStrings to be
		//converted to GeneralPaths. [Jon Aquino]
		return path;
	}

    class LineStringPath extends LineString implements PathIterator {
		
		private int iterate;
		private int numPoints;
		private Coordinate[] points;
		private Java2DConverter j2D;
        private boolean closed;
		
		public LineStringPath(LineString linestring, Java2DConverter j2D){
			super(null, new GeometryFactory());
			//this.linestring = linestring;
			this.j2D = j2D;
			try {
			  points = j2D.toViewCoordinates(linestring.getCoordinates());
			}
			catch (NoninvertibleTransformException ex){	}
			this.numPoints = points.length; //linestring.getNumPoints();
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

	private GeneralPath toShape(LineString lineString)
		throws NoninvertibleTransformException {
        int numPoints = lineString.getNumPoints();
		GeneralPath shape = new GeneralPath(GeneralPath.WIND_NON_ZERO, numPoints);
        PathIterator pi = new LineStringPath(lineString, this);
		shape.append(pi,false);
		//Point2D viewPoint = toViewPoint(lineString.getCoordinateN(0));
		//shape.moveTo((float) viewPoint.getX(), (float) viewPoint.getY());
        //
		//for (int i = 1; i < lineString.getNumPoints(); i++) {
		//	viewPoint = toViewPoint(lineString.getCoordinateN(i));
		//	shape.lineTo((float) viewPoint.getX(), (float) viewPoint.getY());
		//}

		//BasicFeatureRenderer expects LineStrings and MultiLineStrings to be
		//converted to GeneralPaths. [Jon Aquino]
		return shape;
	}

	private Shape toShape(Point point) throws NoninvertibleTransformException {
		Rectangle2D.Double pointMarker =
			new Rectangle2D.Double(
				0.0,
				0.0,
				POINT_MARKER_SIZE,
				POINT_MARKER_SIZE);
		Point2D viewPoint = toViewPoint(point.getCoordinate());
		pointMarker.x = (double) (viewPoint.getX() - (POINT_MARKER_SIZE / 2));
		pointMarker.y = (double) (viewPoint.getY() - (POINT_MARKER_SIZE / 2));

		return pointMarker;
	}

    private Point2D toViewPoint(Coordinate modelCoordinate)
    throws NoninvertibleTransformException {
        //Do the rounding now; don't rely on Java 2D rounding, because it
        //seems to do it differently for drawing and filling, resulting in the draw
        //being a pixel off from the fill sometimes. [Jon Aquino]
    	//LDB 04/25/2007: this assumption doesn't seem to be true any longer
        Point2D viewPoint = pointConverter.toViewPoint(modelCoordinate);
        //Optimization recommended by Todd Warnes [Jon Aquino 2004-02-06]
        //viewPoint.setLocation(
        //        Math.round(viewPoint.getX()),
        //        Math.round(viewPoint.getY()));
        return viewPoint;
    }

	public static interface PointConverter {
		public Point2D toViewPoint(Coordinate modelCoordinate)
			throws NoninvertibleTransformException;
        public double getScale() throws NoninvertibleTransformException;
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
        
        // [NOTE] I tested a short-circuit here to return a 1 pixel representation of
        // geometries having an envelope of less than 1/2 pixel, but I get only an ugly
        // render for a small performance improvement [mmichaud - 2007-05-23]
        
		if (geometry.isEmpty()) {
			return new GeneralPath();
		}

		if (geometry instanceof Polygon) {
			return toShape((Polygon) geometry);
		}

		if (geometry instanceof MultiPolygon) {
			return toShape((MultiPolygon) geometry);
		}

		if (geometry instanceof LineString) {
			return toShape((LineString) geometry);
		}

		if (geometry instanceof MultiLineString) {
			return toShape((MultiLineString) geometry);
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
