//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/tags/2.2-rc5/src/org/deegree/model/spatialschema/JTSAdapter.java $
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/   
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de

 ---------------------------------------------------------------------------*/
package org.deegree.model.spatialschema;

import org.deegree.model.crs.CoordinateSystem;

import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jump.workbench.Logger;

/**
 * Adapter between deegree-<tt>Geometry</tt>s and JTS-<tt>Geometry<tt> objects.
 * <p>
 * Please note that the generated deegree-objects use null as
 * <tt>CS_CoordinateSystem</tt>!
 * <p>
 * 
 * @author <a href="mailto:mschneider@lat-lon.de">Markus Schneider</a>
 * @version $Revision: 9343 $ $Date: 2007-12-27 14:30:32 +0100 (Do, 27 Dez 2007) $
 * @author Sergio Baños Calvo (SAIG S.L.) Added suppport for Z coordinate (18/08/2008) Added support
 *         for SRS, currently the SRS is ignored (11/08/2009)
 */
public class JTSAdapter {

    // precision model that is used for all JTS-Geometries
    private static PrecisionModel pm = new PrecisionModel();

    // factory for creating JTS-Geometries
    private static com.vividsolutions.jts.geom.GeometryFactory jtsFactory =
        new com.vividsolutions.jts.geom.GeometryFactory(pm, 0);

    /**
     * Converts a <tt>Geometry</tt> to a corresponding JTS-<tt>Geometry</tt> object.
     * <p>
     * Currently, the following conversions are supported:
     * <ul>
     * <li>Point -> Point
     * <li>MultiPoint -> MultiPoint
     * <li>Curve -> LineString
     * <li>MultiCurve -> MultiLineString
     * <li>Surface -> Polygon
     * <li>MultiSurface -> MultiPolygon
     * <li>MultiPrimitive -> GeometryCollection
     * </ul>
     * <p>
     * 
     * @param gmObject the object to be converted
     * @return the corresponding JTS-<tt>Geometry</tt> object
     * @throws GeometryException if type unsupported or conversion failed
     */
    public static com.vividsolutions.jts.geom.Geometry export( Geometry gmObject )
            throws GeometryException {

        com.vividsolutions.jts.geom.Geometry geometry = null;
        if (gmObject instanceof Point) {
            geometry = export((Point) gmObject);
        } else if (gmObject instanceof MultiPoint) {
            geometry = export((MultiPoint) gmObject);
        } else if (gmObject instanceof Curve) {
            geometry = export((Curve) gmObject);
        } else if (gmObject instanceof MultiCurve) {
            geometry = export((MultiCurve) gmObject);
        } else if (gmObject instanceof Surface) {
            geometry = export((Surface) gmObject);
        } else if (gmObject instanceof MultiSurface) {
            geometry = export((MultiSurface) gmObject);
        } else if (gmObject instanceof MultiPrimitive) {
            geometry = export((MultiPrimitive) gmObject);
        } else {
            throw new GeometryException("JTSAdapter.export does not support type '" + gmObject.getClass().getName() //$NON-NLS-1$
                + "'!");
        }
        return geometry;
    }

    /**
     * Converts a JTS-<tt>Geometry</tt> object to a corresponding <tt>Geometry</tt>.
     * <p>
     * Currently, the following conversions are supported:
     * <ul>
     * <li>Point -> Point
     * <li>MultiPoint -> MultiPoint
     * <li>LineString -> Curve
     * <li>MultiLineString -> MultiCurve
     * <li>Polygon -> Surface
     * <li>MultiPolygon -> MultiSurface
     * <li>GeometryCollection -> MultiPrimitive
     * </ul>
     * <p>
     * 
     * @param geometry the JTS-<tt>Geometry</tt> to be converted
     * @return the corresponding <tt>Geometry</tt>
     * @throws GeometryException if type unsupported or conversion failed
     */
    public static Geometry wrap( com.vividsolutions.jts.geom.Geometry geometry, CoordinateSystem crs )
            throws GeometryException {

        Geometry gmObject = null;
        if (geometry instanceof com.vividsolutions.jts.geom.Point) {
            gmObject = wrap((com.vividsolutions.jts.geom.Point) geometry, crs);
        } else if (geometry instanceof com.vividsolutions.jts.geom.MultiPoint) {
            gmObject = wrap((com.vividsolutions.jts.geom.MultiPoint) geometry, crs);
        } else if (geometry instanceof com.vividsolutions.jts.geom.LineString) {
            gmObject = wrap((com.vividsolutions.jts.geom.LineString) geometry, crs);
        } else if (geometry instanceof com.vividsolutions.jts.geom.MultiLineString) {
            gmObject = wrap((com.vividsolutions.jts.geom.MultiLineString) geometry, crs);
        } else if (geometry instanceof com.vividsolutions.jts.geom.Polygon) {
            gmObject = wrap((com.vividsolutions.jts.geom.Polygon) geometry, crs);
        } else if (geometry instanceof com.vividsolutions.jts.geom.MultiPolygon) {
            gmObject = wrap((com.vividsolutions.jts.geom.MultiPolygon) geometry, crs);
        } else if (geometry instanceof com.vividsolutions.jts.geom.GeometryCollection) {
            gmObject = wrap((com.vividsolutions.jts.geom.GeometryCollection) geometry, crs);
        } else {
            throw new GeometryException("JTSAdapter.wrap does not support type '" + geometry.getClass().getName() //$NON-NLS-1$
                + "'!");
        }
        return gmObject;
    }

    /**
     * Converts a <tt>Point</tt> to a <tt>Point</tt>.
     * <p>
     * 
     * @param gmPoint point to be converted
     * @return the corresponding <tt>Point</tt> object
     */
    private static com.vividsolutions.jts.geom.Point export( Point gmPoint ) {
        double z = gmPoint.getZ();
        com.vividsolutions.jts.geom.Coordinate coord =
            Double.isNaN(z) ? new com.vividsolutions.jts.geom.Coordinate(gmPoint.getX(),
                gmPoint.getY()) : new com.vividsolutions.jts.geom.Coordinate(gmPoint.getX(),
                gmPoint.getY(),
                z);
        return jtsFactory.createPoint(coord);
    }

    /**
     * Converts a <tt>MultiPoint</tt> to a <tt>MultiPoint</tt>.
     * <p>
     * 
     * @param gmMultiPoint multipoint to be converted
     * @return the corresponding <tt>MultiPoint</tt> object
     */
    private static com.vividsolutions.jts.geom.MultiPoint export( MultiPoint gmMultiPoint ) {
        Point[] gmPoints = gmMultiPoint.getAllPoints();
        com.vividsolutions.jts.geom.Point[] points =
            new com.vividsolutions.jts.geom.Point[gmPoints.length];
        for( int i = 0; i < points.length; i++ ) {
            points[i] = export(gmPoints[i]);
        }
        return jtsFactory.createMultiPoint(points);
    }

    /**
     * Converts a <tt>Curve</tt> to a <tt>LineString</tt>.
     * <p>
     * 
     * @param curve <tt>Curve</tt> to be converted
     * @return the corresponding <tt>LineString</tt> object
     * @throws GeometryException
     */
    private static com.vividsolutions.jts.geom.LineString export( Curve curve )
            throws GeometryException {

        LineString lineString = curve.getAsLineString();
        com.vividsolutions.jts.geom.Coordinate[] coords =
            new com.vividsolutions.jts.geom.Coordinate[lineString.getNumberOfPoints()];
        for( int i = 0; i < coords.length; i++ ) {
            Position position = lineString.getPositionAt(i);
            coords[i] =
                new com.vividsolutions.jts.geom.Coordinate(position.getX(),
                    position.getY(),
                    position.getZ());
        }
        return jtsFactory.createLineString(coords);
    }

    /**
     * Converts a <tt>MultiCurve</tt> to a <tt>MultiLineString</tt>.
     * <p>
     * 
     * @param multi <tt>MultiCurve</tt> to be converted
     * @return the corresponding <tt>MultiLineString</tt> object
     * @throws GeometryException
     */
    private static com.vividsolutions.jts.geom.MultiLineString export( MultiCurve multi )
            throws GeometryException {

        Curve[] curves = multi.getAllCurves();
        com.vividsolutions.jts.geom.LineString[] lineStrings =
            new com.vividsolutions.jts.geom.LineString[curves.length];
        for( int i = 0; i < curves.length; i++ ) {
            lineStrings[i] = export(curves[i]);
        }
        return jtsFactory.createMultiLineString(lineStrings);
    }

    /**
     * Converts an array of <tt>Position</tt>s to a <tt>LinearRing</tt>.
     * <p>
     * 
     * @param positions an array of <tt>Position</tt>s
     * @return the corresponding <tt>LinearRing</tt> object
     */
    public static com.vividsolutions.jts.geom.LinearRing export( Position[] positions ) {
        com.vividsolutions.jts.geom.Coordinate[] coords =
            new com.vividsolutions.jts.geom.Coordinate[Math.max(positions.length, 4)];
        for( int i = 0; i < positions.length; i++ ) {
            coords[i] =
                new com.vividsolutions.jts.geom.Coordinate(positions[i].getX(),
                    positions[i].getY(),
                    positions[i].getZ());
        }
        return jtsFactory.createLinearRing(coords);
    }

    /**
     * Converts a <tt>Surface</tt> to a <tt>Polygon</tt>.
     * <p>
     * Currently, the <tt>Surface</tt> _must_ contain exactly one patch!
     * <p>
     * 
     * @param surface a <tt>Surface</tt>
     * @return the corresponding <tt>Polygon</tt> object
     */
    private static com.vividsolutions.jts.geom.Polygon export( Surface surface ) {
        SurfacePatch patch = null;
        try {
            patch = surface.getSurfacePatchAt(0);
        } catch (GeometryException e) {
            Logger.error(e);
        }
        Position[] exteriorRing = patch.getExteriorRing();
        Position[][] interiorRings = patch.getInteriorRings();

        com.vividsolutions.jts.geom.LinearRing shell = export(exteriorRing);
        com.vividsolutions.jts.geom.LinearRing[] holes =
            new com.vividsolutions.jts.geom.LinearRing[0];
        if (interiorRings != null)
            holes = new com.vividsolutions.jts.geom.LinearRing[interiorRings.length];
        for( int i = 0; i < holes.length; i++ ) {
            holes[i] = export(interiorRings[i]);
        }
        return jtsFactory.createPolygon(shell, holes);
    }

    /**
     * Converts a <tt>MultiSurface</tt> to a <tt>MultiPolygon</tt>.
     * <p>
     * Currently, the contained <tt>Surface</tt> _must_ have exactly one patch!
     * <p>
     * 
     * @param msurface a <tt>MultiSurface</tt>
     * @return the corresponding <tt>MultiPolygon</tt> object
     */
    private static com.vividsolutions.jts.geom.MultiPolygon export( MultiSurface msurface ) {

        Surface[] surfaces = msurface.getAllSurfaces();
        com.vividsolutions.jts.geom.Polygon[] polygons =
            new com.vividsolutions.jts.geom.Polygon[surfaces.length];

        for( int i = 0; i < surfaces.length; i++ ) {
            polygons[i] = export(surfaces[i]);
        }
        return jtsFactory.createMultiPolygon(polygons);
    }

    /**
     * Converts a <tt>MultiPrimitive</tt> to a <tt>GeometryCollection</tt>.
     * <p>
     * 
     * @param multi a <tt>MultiPrimtive</tt>
     * @return the corresponding <tt>GeometryCollection</tt> object
     * @throws GeometryException
     */
    private static com.vividsolutions.jts.geom.GeometryCollection export( MultiPrimitive multi )
            throws GeometryException {

        Geometry[] primitives = multi.getAllPrimitives();
        com.vividsolutions.jts.geom.Geometry[] geometries =
            new com.vividsolutions.jts.geom.Geometry[primitives.length];

        for( int i = 0; i < primitives.length; i++ ) {
            geometries[i] = export(primitives[i]);
        }
        return jtsFactory.createGeometryCollection(geometries);
    }

    /**
     * Converts a <tt>Point</tt> to a <tt>Point</tt>s.
     * <p>
     * 
     * @param point a <tt>Point</tt> object
     * @return the corresponding <tt>Point</tt>
     */
    private static Point wrap( com.vividsolutions.jts.geom.Point point, CoordinateSystem crs ) {
        com.vividsolutions.jts.geom.Coordinate coord = point.getCoordinate();
        return Double.isNaN(coord.z) ? new PointImpl(coord.x, coord.y, crs)
            : new PointImpl(coord.x, coord.y, coord.z, crs);
    }

    /**
     * Converts a <tt>MultiPoint</tt> to a <tt>MultiPoint</tt>.
     * <p>
     * 
     * @param multi a <tt>MultiPoint</tt> object
     * @return the corresponding <tt>MultiPoint</tt>
     */
    private static MultiPoint wrap( com.vividsolutions.jts.geom.MultiPoint multi,
            CoordinateSystem crs ) {
        Point[] gmPoints = new Point[multi.getNumGeometries()];
        for( int i = 0; i < gmPoints.length; i++ ) {
            gmPoints[i] = wrap((com.vividsolutions.jts.geom.Point) multi.getGeometryN(i), crs);
        }
        return new MultiPointImpl(gmPoints, null);
    }

    /**
     * Converts a <tt>LineString</tt> to a <tt>Curve</tt>.
     * <p>
     * 
     * @param line a <tt>LineString</tt> object
     * @return the corresponding <tt>Curve</tt>
     * @throws GeometryException
     */
    private static Curve wrap( com.vividsolutions.jts.geom.LineString line, CoordinateSystem crs )
            throws GeometryException {
        com.vividsolutions.jts.geom.Coordinate[] coords = line.getCoordinates();
        Position[] positions = new Position[coords.length];
        for( int i = 0; i < coords.length; i++ ) {
            positions[i] = new PositionImpl(coords[i].x, coords[i].y);
        }
        return GeometryFactory.createCurve(positions, crs);
    }

    /**
     * Converts a <tt>MultiLineString</tt> to a <tt>MultiCurve</tt>.
     * <p>
     * 
     * @param multi a <tt>MultiLineString</tt> object
     * @return the corresponding <tt>MultiCurve</tt>
     * @throws GeometryException
     */
    private static MultiCurve wrap( com.vividsolutions.jts.geom.MultiLineString multi,
            CoordinateSystem crs ) throws GeometryException {
        Curve[] curves = new Curve[multi.getNumGeometries()];
        for( int i = 0; i < curves.length; i++ ) {
            curves[i] = wrap((com.vividsolutions.jts.geom.LineString) multi.getGeometryN(i), crs);
        }
        return GeometryFactory.createMultiCurve(curves);
    }

    /**
     * Converts a <tt>Polygon</tt> to a <tt>Surface</tt>.
     * <p>
     * 
     * @param polygon a <tt>Polygon</tt>
     * @return the corresponding <tt>Surface</tt> object
     * @throws GeometryException
     */
    private static Surface wrap( com.vividsolutions.jts.geom.Polygon polygon, CoordinateSystem crs )
            throws GeometryException {

        Position[] exteriorRing = createGMPositions(polygon.getExteriorRing());
        Position[][] interiorRings = new Position[polygon.getNumInteriorRing()][];

        for( int i = 0; i < interiorRings.length; i++ ) {
            interiorRings[i] = createGMPositions(polygon.getInteriorRingN(i));
        }
        SurfacePatch patch =
            new PolygonImpl(new SurfaceInterpolationImpl(), exteriorRing, interiorRings, crs);

        return new SurfaceImpl(patch);
    }

    /**
     * Converts a <tt>MultiPolygon</tt> to a <tt>MultiSurface</tt>.
     * <p>
     * 
     * @param multiPolygon a <tt>MultiPolygon</tt>
     * @return the corresponding <tt>MultiSurface</tt> object
     * @throws GeometryException
     */
    private static MultiSurface wrap( com.vividsolutions.jts.geom.MultiPolygon multiPolygon,
            CoordinateSystem crs ) throws GeometryException {

        Surface[] surfaces = new Surface[multiPolygon.getNumGeometries()];
        for( int i = 0; i < surfaces.length; i++ ) {
            surfaces[i] =
                wrap((com.vividsolutions.jts.geom.Polygon) multiPolygon.getGeometryN(i), crs);
        }
        return new MultiSurfaceImpl(surfaces);
    }

    /**
     * Converts a <tt>GeometryCollection</tt> to a <tt>MultiPrimitve</tt>.
     * <p>
     * 
     * @param collection a <tt>GeometryCollection</tt>
     * @return the corresponding <tt>MultiPrimitive</tt> object
     * @throws GeometryException
     */
    private static MultiPrimitive wrap( com.vividsolutions.jts.geom.GeometryCollection collection,
            CoordinateSystem crs ) throws GeometryException {

        MultiPrimitive multi = new MultiPrimitiveImpl(null);
        for( int i = 0; i < collection.getNumGeometries(); i++ ) {
            multi.add(wrap(collection.getGeometryN(i), crs));
        }
        return multi;
    }

    /**
     * Converts a <tt>LineString</tt> to an array of <tt>Position</tt>s.
     * <p>
     * 
     * @param line a <tt>LineString</tt> object
     * @return the corresponding array of <tt>Position</tt>s
     */
    private static Position[] createGMPositions( com.vividsolutions.jts.geom.LineString line ) {
        com.vividsolutions.jts.geom.Coordinate[] coords = line.getCoordinates();
        Position[] positions = new Position[coords.length];
        for( int i = 0; i < coords.length; i++ ) {
            positions[i] = new PositionImpl(coords[i].x, coords[i].y);
        }
        return positions;
    }
}