package com.vividsolutions.jump.geom;

import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory.*;

/**
 * Operator to make a geometry valid.
 */
public class MakeValidOp {

    private static final Coordinate[] EMPTY_COORD_ARRAY = new Coordinate[0];
    private static final LinearRing[] EMPTY_RING_ARRAY = new LinearRing[0];

    public MakeValidOp() {}

    /**
     * Decompose a geometry recursively into simple components.
     * @param geometry input geometry
     * @param list a list of simple components (Point, LineString or Polygon)
     */
    public static void decompose(Geometry geometry, List<Geometry> list) {
        for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
            Geometry component = geometry.getGeometryN(i);
            if (component instanceof GeometryCollection) decompose(component, list);
            else list.add(component);
        }
    }


    public static Geometry makeValid(Geometry geometry) {
        return makeValid(geometry, true);
    }

    /**
     * Repair an invalid geometry.
     * <br/>
     * WARNING : a simple geometry may result in a multi-geometry and/or includes
     * degenerate geometries (geometries with a lower dimension than the input geometry).
     * <br/>
     * It is up to the client to filter degenerate geometries or to explode geometry
     * collections if he needs to.
     * <br/>
     * WARNING : removeDuplicate is not always respected : polygonization will
     * automatically remove duplicate coordniates. Also 4d coordinate may loose
     * their M value after the noding or the polygonization phase.
     * @param geometry input geometry
     * @param removeDuplicate
     * @return
     */
    public static Geometry makeValid(Geometry geometry, boolean removeDuplicate) {
        List<Geometry> list = new ArrayList<Geometry>(geometry.getNumGeometries());
        decompose(geometry, list);
        List<Geometry> list2 = new ArrayList<Geometry>();
        for (Geometry component : list) {
            if (component instanceof Point) {
                Point p = makePointValid((Point)component);
                if (!p.isEmpty()) list2.add(p);
            }
            else if (component instanceof LineString) {
                Geometry geom = makeLineStringValid((LineString) component, removeDuplicate);
                for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
                    if (!geom.getGeometryN(i).isEmpty()) list2.add(geom.getGeometryN(i));
                }
            }
            else if (component instanceof Polygon) {
                Geometry geom = makePolygonValid((Polygon) component, removeDuplicate);
                for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
                    if (!geom.getGeometryN(i).isEmpty()) list2.add(geom.getGeometryN(i));
                }
            }
            else assert false : "Should never reach here";
        }
        if (list2.isEmpty()) {
            GeometryFactory factory = geometry.getFactory();
            if (geometry instanceof Point) return factory.createPoint((Coordinate)null);
            else if (geometry instanceof LinearRing) return factory.createLinearRing(EMPTY_COORD_ARRAY);
            else if (geometry instanceof LineString) return factory.createLineString(EMPTY_COORD_ARRAY);
            else if (geometry instanceof Polygon) return factory.createPolygon(factory.createLinearRing(EMPTY_COORD_ARRAY), EMPTY_RING_ARRAY);
            else if (geometry instanceof MultiPoint) return factory.createMultiPoint(new Point[0]);
            else if (geometry instanceof MultiLineString) return factory.createMultiLineString(new LineString[0]);
            else if (geometry instanceof MultiPolygon) return factory.createMultiPolygon(new Polygon[0]);
            else return factory.createGeometryCollection(new Geometry[0]);
        } else {
            return geometry.getFactory().buildGeometry(list2);
        }
    }

    private static Point makePointValid(Point point) {
        CoordinateSequence sequence = point.getCoordinateSequence();
        if (Double.isNaN(sequence.getOrdinate(0, 0)) || Double.isNaN(sequence.getOrdinate(0, 1))) {
            return point.getFactory().createPoint(DOUBLE_FACTORY.create(0, sequence.getDimension()));
        } else {
            return point;
        }
    }

    /**
     * Returns a coordinateSequence where coordinates with NaN x, NaN y or duplicate points
     * have been removed. Keep all the dimensions of input coordinates.
     * @param sequence input sequence of coordinates
     * @param removeDuplicate
     * @param close
     * @return
     */
    private static CoordinateSequence makeSequenceValid(CoordinateSequence sequence, boolean removeDuplicate, boolean close) {
        int dim = sequence.getDimension();
        // we add 1 to the sequence size for the case where we have to close the linear ring
        double[] array = new double[(sequence.size()+1) * sequence.getDimension()];
        boolean modified = false;
        int count = 0;
        // Iterate through coordinates, skip points with x=NaN, y=NaN or duplicate
        for (int i = 0 ; i < sequence.size() ; i++) {
            System.out.println("coordinate " + i + " " + sequence.getCoordinate(i));
            if (Double.isNaN(sequence.getOrdinate(i, 0)) || Double.isNaN(sequence.getOrdinate(i, 1))) {
                modified = true;
                continue;
            }
            if (removeDuplicate && count > 0 && sequence.getCoordinate(i).equals(sequence.getCoordinate(i-1))) {
                modified = true;
                continue;
            }
            for (int j = 0 ; j < dim ; j++) {
                array[count*dim + j] = sequence.getOrdinate(i, j);
                if (j == dim-1) count++;
            }
        }
        // Close the sequence if it is not closed and there is already 3 distinct coordinates
        if (close && count > 2 && (array[0] != array[(count-1)*dim] || array[1] != array[(count-1)*dim + 1])) {
            for (int j = 0 ; j < dim ; j++) {
                array[count*dim + j] = array[j];
            }
            modified = true;
            count++;
        }
        if (modified) {
            double[] shrinkedArray = new double[count*dim];
            System.arraycopy(array,0,shrinkedArray, 0, count*dim);
            return PackedCoordinateSequenceFactory.DOUBLE_FACTORY.create(shrinkedArray, dim);
        } else {
            return sequence;
        }
    }

    private static Geometry makeLineStringValid(LineString lineString, boolean removeDuplicate) {
        CoordinateSequence sequence = lineString.getCoordinateSequence();
        CoordinateSequence sequence2 = makeSequenceValid(sequence, true, false);
        if (sequence2.size() == 0) {
            // no valid point -> empty LineString
            return lineString.getFactory().createLineString(DOUBLE_FACTORY.create(0, sequence.getDimension()));
        } else if (sequence2.size() == 1) {
            // a single valid point -> returns a Point
            return lineString.getFactory().createPoint(sequence2);
        } else if (removeDuplicate) {
            // we use already calculated sequence2
            return lineString.getFactory().createLineString(sequence2);
        } else {
            // we need to recompute a cleaned sequence without removinf duplicates
            return lineString.getFactory().createLineString(makeSequenceValid(sequence, false, false));
        }
    }

    private static Geometry makePolygonValid(Polygon polygon, boolean removeDuplicate) {
        Geometry geom = makePolygonComponentsValid(polygon, removeDuplicate);
        List<Geometry> list = new ArrayList<Geometry>();
        for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
            Geometry component = geom.getGeometryN(i);
            if (component instanceof Polygon) {
                Geometry nodedPolygon = nodePolygon((Polygon)component);
                for (int j = 0 ; j < nodedPolygon.getNumGeometries() ; j++) {
                    list.add(nodedPolygon.getGeometryN(j));
                }
            } else {
                list.add(component);
            }
        }
        return polygon.getFactory().buildGeometry(list);
    }

    // For degenerate geometries, duplicate are removed anyway
    private static Geometry makePolygonComponentsValid(Polygon polygon, boolean removeDuplicate) {
        GeometryFactory factory = polygon.getFactory();
        CoordinateSequence outerRingSeq = makeSequenceValid(polygon.getExteriorRing().getCoordinateSequence(), true, true);
        // Created a valid sequence, but it does not form a LinearRing
        // -> build valid 0-dim or 1-dim geometry from all the rings
        if (outerRingSeq.size() == 0 || outerRingSeq.size() < 4) {
            List<Geometry> list = new ArrayList<Geometry>();
            if (outerRingSeq.size() > 0) list.add(makeLineStringValid(polygon.getExteriorRing(), removeDuplicate));
            for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
                Geometry g = makeLineStringValid(polygon.getInteriorRingN(i), removeDuplicate);
                if (!g.isEmpty()) list.add(g);
            }
            if (list.isEmpty()) return factory.createPolygon(outerRingSeq);
            else return factory.buildGeometry(list);
        }
        else {
            List<LinearRing> innerRings = new ArrayList<LinearRing>();
            List<Geometry> degeneracies = new ArrayList<Geometry>();
            for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
                CoordinateSequence seq = makeSequenceValid(polygon.getInteriorRingN(i).getCoordinateSequence(), true, true);
                if (seq.size() == 0) continue;
                else if (seq.size() == 1) degeneracies.add(factory.createPoint(seq));
                else if (seq.size() < 4) degeneracies.add(factory.createLineString(seq));
                else innerRings.add(factory.createLinearRing(seq));
            }
            Polygon poly = factory.createPolygon(factory.createLinearRing(outerRingSeq),
                    innerRings.toArray(new LinearRing[innerRings.size()]));
            if (degeneracies.isEmpty()) {
                return poly;
            }
            else {
                degeneracies.add(0, poly);
                return factory.buildGeometry(degeneracies);
            }
        }
    }

    /**
     * Extract polygonal components from a geometry.
     * <p>The algorithm is recursive and can handle nested GeometryCollection.</p>
     * <p>null and empty geometries are discarded.</p>
     * @param geometry the geometry from which polygonal components wil be extracted
     * @param list the list into which polygonal components will be added.
     */
    protected static void extractPolygons(Geometry geometry, List<Polygon> list) {
        for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
            Geometry g = geometry.getGeometryN(i);
            if (g == null) continue;            // null components are discarded
            else if (g.isEmpty()) continue;     // empty components are discarded
            else if (g instanceof Polygon) {
                if (!g.isValid()) {
                    Geometry repaired = nodePolygon((Polygon)g); // repair invalid invalid polygons
                    if (repaired.isValid()) extractPolygons(repaired, list);
                    else {
                        repaired = g.buffer(0); // second tentative to repair the polygon
                        extractPolygons(repaired, list);
                    }
                }
                else if (g.getArea() > 0) list.add((Polygon)g); // discard flat polygons
            }
            else if (g instanceof GeometryCollection) {
                extractPolygons(g, list);       // recursivity
            }
            else {
                assert false : "should never reach here";
            }
        }
    }

    /**
     * Computes a valid Geometry from a Polygon which may not be valid
     * (auto-intersecting ring or overlapping holes).
     * <ul>
     * <li>creates a Geometry from the <em>noded</em> exterior boundary</li>
     * <li>remove Geometries computed from noded interior boundaries</li>
     * </ul>
     */
    protected static Geometry nodePolygon(Polygon polygon) {
        GeometryFactory factory = polygon.getFactory();
        LinearRing exteriorRing = (LinearRing)polygon.getExteriorRing();
        Geometry geom = getMultiPolygonFromLinearRing(exteriorRing);
        for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
            LinearRing interiorRing = (LinearRing)polygon.getInteriorRingN(i);
            geom = geom.difference(getMultiPolygonFromLinearRing(interiorRing));
        }
        return geom;
    }

    /**
     * Node a LinearRing and return a MultiPolygon containing
     * <ul>
     * <li>a single Polygon if the LinearRing is simple</li>
     * <li>several Polygons if the LinearRing auto-intersects</li>
     * </ul>
     * This is used to repair auto-intersecting Polygons
     */
    protected static MultiPolygon getMultiPolygonFromLinearRing(LinearRing ring) {
        if (ring.isSimple()) {
            return ring.getFactory().createMultiPolygon(new Polygon[]{
                    ring.getFactory().createPolygon(ring, EMPTY_RING_ARRAY)
            });
        }
        else {
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(nodeLineString(ring.getCoordinates(), ring.getFactory()));
            Collection<Polygon> polys = polygonizer.getPolygons();
            return ring.getFactory().createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
        }
    }

    /**
     * Nodes a LineString and returns a List of Noded LineString's.
     * Used to repare auto-intersecting LineString and Polygons.
     * This method cannot process CoordinateSequence. The noding process is limited
     * to 3d geometries.<br/>
     * Preserves duplicate coordinates.
     * @param coords coordinate array to be noded
     * @param gf geometryFactory to use
     * @return a list of noded LineStrings
     */
    protected static List<LineString> nodeLineString(Coordinate[] coords, GeometryFactory gf) {
        MCIndexNoder noder = new MCIndexNoder();
        noder.setSegmentIntersector(new IntersectionAdder(new RobustLineIntersector()));
        List<NodedSegmentString> list = new ArrayList<NodedSegmentString>();
        list.add(new NodedSegmentString(coords, null));
        noder.computeNodes(list);
        List<LineString> lineStringList = new ArrayList<LineString>();
        for (Object segmentString : noder.getNodedSubstrings()) {
            lineStringList.add(gf.createLineString(
                    ((NodedSegmentString)segmentString).getCoordinates()
            ));
        }
        return lineStringList;
    }



    public static void main(String[] args) throws ParseException {
        GeometryFactory factory = new GeometryFactory();

        // check makePointValid
        Point p1 = factory.createPoint(new Coordinate(0,0));
        Point p2 = makePointValid(p1);
        assert p1.equals(p2);

        p1 = factory.createPoint(new Coordinate(Double.NaN,0));
        p2 = makePointValid(p1);
        assert !p1.isEmpty();
        assert p2.isEmpty();

        p1 = factory.createPoint(new Coordinate(0, Double.NaN));
        p2 = makePointValid(p1);
        assert !p1.isEmpty();
        assert p2.isEmpty();

        p1 = factory.createPoint(DOUBLE_FACTORY.create(new double[]{0,1,2,3}, 4));
        p2 = makePointValid(p1);
        assert p1.getCoordinateSequence().getOrdinate(0,3) == p2.getCoordinateSequence().getOrdinate(0,3);

        // check makeSequenceValid
        CoordinateSequence cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,4,5,6,7}, 4);
        CoordinateSequence cs2 = makeSequenceValid(cs1, false, false);
        assert cs1.getOrdinate(1,3) == cs2.getOrdinate(1,3);

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,4,5,6,7}, 4);
        cs2 = makeSequenceValid(cs1, true, false);
        assert cs1.getOrdinate(1,3) == cs2.getOrdinate(1,3);

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,4,5,Double.NaN,7}, 4);
        cs2 = makeSequenceValid(cs1, true, false);
        assert cs1.getOrdinate(1,3) == cs2.getOrdinate(1,3);

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,4,Double.NaN,6,7}, 4);
        cs2 = makeSequenceValid(cs1, true, false);
        assert cs2.size() == 1;
        assert cs2.getOrdinate(0,3) == 3;

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,0,1,6,7}, 4);
        cs2 = makeSequenceValid(cs1, true, false);
        assert cs2.size() == 1;
        assert cs2.getOrdinate(0,3) == 3;

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,4,5,6,7,8,9,10,11}, 4);
        cs2 = makeSequenceValid(cs1, true, false);
        assert cs2.size() == 3;
        assert cs2.getOrdinate(2,3) == 11;

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,4,5,6,7,8,9,10,11}, 4);
        cs2 = makeSequenceValid(cs1, true, true);
        assert cs2.size() == 4;
        assert cs2.getOrdinate(3,3) == 3;

        WKTReader reader = new WKTReader();
        Geometry geometry = reader.read("LINESTRING(0 0, 10 0, 20 0, 20 0, 30 0)");
        assert geometry.getNumPoints() == 5;
        List<LineString> list = nodeLineString(geometry.getCoordinates(), geometry.getFactory());
        assert list.size() == 1;
        assert list.get(0).getCoordinates().length == 5;

        geometry = reader.read("LINESTRING(0 0, 20 0, 20 20, 20 20, 10 -10)");
        assert geometry.getNumPoints() == 5;
        list = nodeLineString(geometry.getCoordinates(), geometry.getFactory());
        assert list.size() == 5; // creates a degenerate segment from 20 20 to 20 20

        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(list);
        Collection<Polygon> polys = polygonizer.getPolygons();
        System.out.println(polys);

    }

}
