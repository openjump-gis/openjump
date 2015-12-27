package com.vividsolutions.jump.geom;

import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import visad.data.netcdf.in.Merger;

import java.util.*;

import static com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory.*;

/**
 * Operator to make a geometry valid.
 * <br/>
 * Making a geometry valid will remove duplicate points although duplicate points
 * do not make a geometry invalid.
 */
public class MakeValidOp {

    private static final Coordinate[] EMPTY_COORD_ARRAY = new Coordinate[0];
    private static final LinearRing[] EMPTY_RING_ARRAY = new LinearRing[0];

    // If preserveGeomDim is true, geometry components with a dimension lesser than
    // input geometry dimension are ignored (except if input geometry is an heterogeneous
    // GeometryCollection)
    private boolean preserveGeomDim = false;

    // If preserveCoordDim is true, MakeValidOp preserves third and fourth ordinates.
    // If preserveCoordDim is false, third dimension is preserved but not fourth one.
    private boolean preserveCoordDim = false;

    public MakeValidOp() {}

    public MakeValidOp preserveGeomDim() {
        this.preserveGeomDim = true;
        return this;
    }

    public MakeValidOp preserveCoordDim() {
        this.preserveCoordDim = true;
        return this;
    }

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
     * TODO add an option to return a geometry preserving input dimension
     * @param geometry input geometry
     * @return
     */
    public Geometry makeValid(Geometry geometry) {
        List<Geometry> list = new ArrayList<Geometry>(geometry.getNumGeometries());
        decompose(geometry, list);
        List<Geometry> list2 = new ArrayList<Geometry>();
        for (Geometry component : list) {
            if (component instanceof Point) {
                Point p = makePointValid((Point)component);
                if (!p.isEmpty()) list2.add(p);
            }
            else if (component instanceof LineString) {
                Geometry geom = makeLineStringValid((LineString) component);
                for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
                    if (!geom.getGeometryN(i).isEmpty()) list2.add(geom.getGeometryN(i));
                }
            }
            else if (component instanceof Polygon) {
                Geometry geom = makePolygonValid((Polygon) component);
                for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
                    if (!geom.getGeometryN(i).isEmpty()) list2.add(geom.getGeometryN(i));
                }
            }
            else assert false : "Should never reach here";
        }
        if (preserveGeomDim && !geometry.getClass().getSimpleName().equals("GeometryCollection")) {
            list2 = removeLowerDimension(list2, geometry.getDimension());
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
            //TODO : The case of MultiPolygon with adjacent polygons is not processed !
            return geometry.getFactory().buildGeometry(list2);
        }
    }

    // Remove geometries with a dimension less than dimension parameter
    private List<Geometry> removeLowerDimension(List<Geometry> geometries, int dimension) {
        List<Geometry> list = new ArrayList<Geometry>();
        for (Geometry geom : geometries) {
            if (geom.getDimension() == dimension) {
                list.add(geom);
            }
        }
        return list;
    }

    // If X or Y is null, return an empty Point
    private Point makePointValid(Point point) {
        CoordinateSequence sequence = point.getCoordinateSequence();
        // The case where sequence contains more than one point is not
        // processed (it will return an empty point or the input point
        // unchanged)
        if (Double.isNaN(sequence.getOrdinate(0, 0)) || Double.isNaN(sequence.getOrdinate(0, 1))) {
            return point.getFactory().createPoint(DOUBLE_FACTORY.create(0, sequence.getDimension()));
        } else {
            return point;
        }
    }

    /**
     * Returns a coordinateSequence free of Coordinates with X or Y value, and if desired, free
     * of duplicated coordinates. makeSequenceValid keeps the original dimension of input sequence.
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
        // Close z, m dimension if needed
        if (close && count > 3 && dim > 2) {
            for (int d = 2 ; d < dim ; d++) {
                if (array[(count-1)*dim + d] != array[d]) modified = true;
                array[(count-1)*dim + d] = array[d];
            }
        }
        if (modified) {
            double[] shrinkedArray = new double[count*dim];
            System.arraycopy(array,0,shrinkedArray, 0, count*dim);
            return PackedCoordinateSequenceFactory.DOUBLE_FACTORY.create(shrinkedArray, dim);
        } else {
            return sequence;
        }
    }

    /**
     * Returns
     * <ul>
     *     <li>an empty LineString if input CoordinateSequence has no valid point</li>
     *     <li>a Point if input CoordinateSequence has a single valid Point</li>
     * </ul>
     * @param lineString
     * @return
     */
    private Geometry makeLineStringValid(LineString lineString) {
        CoordinateSequence sequence = lineString.getCoordinateSequence();
        CoordinateSequence sequenceWithoutDuplicates = makeSequenceValid(sequence, true, false);
        if (sequenceWithoutDuplicates.size() == 0) {
            // no valid point -> empty LineString
            return lineString.getFactory().createLineString(DOUBLE_FACTORY.create(0, sequence.getDimension()));
        } else if (sequenceWithoutDuplicates.size() == 1) {
            // a single valid point -> returns a Point
            return lineString.getFactory().createPoint(sequenceWithoutDuplicates);
        } else {
            // we use already calculated sequenceWithoutDuplicates
            return lineString.getFactory().createLineString(sequenceWithoutDuplicates);
        }
    }

    /**
     * Making a Polygon valid may creates
     * <ul>
     *     <li>an Empty Polygon if input has no valid coordinate</li>
     *     <li>a Point if input has only one valid coordinate</li>
     *     <li>a LineString if input has only a valid segment</li>
     *     <li>a Polygon in most cases</li>
     *     <li>a MultiPolygon if input has a self-intersection</li>
     *     <li>a GeometryCollection if input has degenerate parts (ex. degenerate holes)</li>
     * </ul>
     * @param polygon
     * @return
     */
    private Geometry makePolygonValid(Polygon polygon) {
        //This first step analyze linear components and create degenerate geometries
        //of dimension 0 or 1 if they do not form valid LinearRings
        //If degenerate geometries are found, it may produce a GeometryCollection with
        //heterogeneous dimension
        Geometry geom = makePolygonComponentsValid(polygon);
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

    /**
     * The method makes sure that outer and inner rings form valid LinearRings.
     * <p>
     * If outerRing is not a valid LinearRing, every linear component is considered as a
     * degenerated geometry of lower dimension (0 or 1)
     * </p>
     * <p>
     * If outerRing is a valid LinearRing but some innerRings are not, invalid innerRings
     * are transformed into LineString (or Point) and the returned geometry may be a
     * GeometryCollection of heterogeneous dimension.
     * </p>
     * @param polygon
     * @return
     */
    private Geometry makePolygonComponentsValid(Polygon polygon) {
        GeometryFactory factory = polygon.getFactory();
        CoordinateSequence outerRingSeq = makeSequenceValid(polygon.getExteriorRing().getCoordinateSequence(), true, true);
        // The validated sequence of the outerRing does not form a valid LinearRing
        // -> build valid 0-dim or 1-dim geometry from all the rings
        if (outerRingSeq.size() == 0 || outerRingSeq.size() < 4) {
            List<Geometry> list = new ArrayList<Geometry>();
            if (outerRingSeq.size() > 0) list.add(makeLineStringValid(polygon.getExteriorRing()));
            for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
                Geometry g = makeLineStringValid(polygon.getInteriorRingN(i));
                if (!g.isEmpty()) list.add(g);
            }
            if (list.isEmpty()) return factory.createPolygon(outerRingSeq);
            else return factory.buildGeometry(list);
        }
        // OuterRing forms a valid ring.
        // Inner rings may be degenerated
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
    protected void extractPolygons(Geometry geometry, List<Polygon> list) {
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
    protected Geometry nodePolygon(Polygon polygon) {
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
    protected Geometry getMultiPolygonFromLinearRing(LinearRing ring) {
        if (ring.isSimple()) {
            return ring.getFactory().createMultiPolygon(new Polygon[]{
                    ring.getFactory().createPolygon(ring, EMPTY_RING_ARRAY)
            });
        }
        else {
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(nodeLineString(ring.getCoordinates(), ring.getFactory()));
            Collection<Geometry> geoms = new ArrayList<Geometry>();
            geoms.addAll(polygonizer.getPolygons());
            geoms.addAll(polygonizer.getCutEdges());
            geoms.addAll(polygonizer.getDangles());
            geoms.addAll(polygonizer.getInvalidRingLines());
            return ring.getFactory().buildGeometry(geoms);
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
    protected Set<LineString> nodeLineString(Coordinate[] coords, GeometryFactory gf) {
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

        // WARNING : merger loose original linestrings
        // It is useful for LinearRings but should not be used for (Multi)LineStrings
        LineMerger merger = new LineMerger();
        merger.add(lineStringList);
        lineStringList = (List<LineString>)merger.getMergedLineStrings();

        // Remove duplicate linestrings preserving main orientation
        Set<LineString> lineStringSet = new HashSet<LineString>();
        for (LineString line : lineStringList) {
            if (lineStringSet.contains(line) || lineStringSet.contains(line.reverse())) {
                continue;
            } else {
                lineStringSet.add(line);
            }
        }
        return lineStringSet;
    }



    public static void main(String[] args) throws ParseException {
        GeometryFactory factory = new GeometryFactory();
        MakeValidOp op = new MakeValidOp();
        MakeValidOp opClean = new MakeValidOp().preserveGeomDim();
        Geometry input, result;

        // check makePointValid
        Point p1 = factory.createPoint(new Coordinate(0,0));
        Point p2 = op.makePointValid(p1);
        assert p1.equals(p2);

        p1 = factory.createPoint(new Coordinate(Double.NaN,0));
        p2 = op.makePointValid(p1);
        assert !p1.isEmpty();
        assert p2.isEmpty();

        p1 = factory.createPoint(new Coordinate(0, Double.NaN));
        p2 = op.makePointValid(p1);
        assert !p1.isEmpty();
        assert p2.isEmpty();

        p1 = factory.createPoint(DOUBLE_FACTORY.create(new double[]{0,1,2,3}, 4));
        p2 = op.makePointValid(p1);
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

        // test close z,m
        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3, 4,5,6,7, 8,9,10,11, 0,1,0,0}, 4);
        cs2 = makeSequenceValid(cs1, true, true);
        assert cs2.size() == 4;
        assert cs2.getOrdinate(3,2) == 2 : cs2.getOrdinate(3,2);
        assert cs2.getOrdinate(3,3) == 3 : cs2.getOrdinate(3,3);

        WKTReader reader = new WKTReader();
        // invalid polygon (single linearRing drawing 2 triangles joined by a line)
        input = reader.read("POLYGON (( 322 354, 322 348, 325 351, 328 351, 331 348, 331 354, 328 351, 325 351, 322 354 ))");
        result = op.makeValid(input);
        assert result.getNumGeometries() == 3;
        result = opClean.makeValid(input);
        assert result.getNumGeometries() == 2;

        reader = new WKTReader();
        // invalid polygon (single linearRing drawing 2 triangles joined by a line, first triangle has duplicated segments)
        input = reader.read("POLYGON (( 322 354, 322 348, 322 354, 322 348, 325 351, 328 351, 331 348, 331 354, 328 351, 325 351, 322 354 ))");
        result = op.makeValid(input);
        assert result.getNumGeometries() == 3;
        result = opClean.makeValid(input);
        assert result.getNumGeometries() == 2;


        reader = new WKTReader();
        input = reader.read("LINESTRING(0 0, 10 0, 20 0, 20 0, 30 0)");
        assert input.getNumPoints() == 5;
        Set<LineString> set = op.nodeLineString(input.getCoordinates(), input.getFactory());
        assert set.size() == 1;
        assert set.iterator().next().getCoordinates().length == 4; // removed duplicate coordinate

        input = reader.read("LINESTRING(0 0, 20 0, 20 20, 20 20, 10 -10)");
        assert input.getNumPoints() == 5;
        set = op.nodeLineString(input.getCoordinates(), input.getFactory());
        assert set.size() == 3; // node + merge -> 3 line strings

        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(set);
        Collection<Polygon> polys = polygonizer.getPolygons();
        System.out.println(polys);

    }

}
