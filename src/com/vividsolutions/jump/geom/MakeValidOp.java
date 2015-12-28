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
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

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

    // If preserveGeomDim is true, the geometry dimension returned by MakeValidOp
    // must be the same as the inputGeometryType (degenerate components of lower
    // dimension are removed).
    // If preserveGeomDim is false MakeValidOp will preserve as much coordinates
    // as possible and may return a geometry of lower dimension or a
    // GeometryCollection if input geometry or geometry components have not the
    // required number of points.
    private boolean preserveGeomDim = true;

    // If preserveCoordDim is true, MakeValidOp preserves third and fourth ordinates.
    // If preserveCoordDim is false, third dimension is preserved but not fourth one.
    private boolean preserveCoordDim = true;

    // If preserveDuplicateCoord is true, MakeValidOp will preserve duplicate
    // coordinates as much as possible. Generally, duplicate coordinates can be
    // preserved for linear geometries but not for areal geometries (overlay
    // operations used to repair polygons remove duplicate points).
    // If preserveDuplicateCoord is false, all duplicated coordinates are removed.
    private boolean preserveDuplicateCoord = true;

    public MakeValidOp() {}


    public MakeValidOp setPreserveGeomDim(boolean preserveGeomDim) {
        this.preserveGeomDim = preserveGeomDim;
        return this;
    }

    public MakeValidOp setPreserveCoordDim(boolean preserveCoordDim) {
        this.preserveCoordDim = preserveCoordDim;
        return this;
    }

    public MakeValidOp setPreserveDuplicateCoord(boolean preserveDuplicateCoord) {
        this.preserveDuplicateCoord = preserveDuplicateCoord;
        return this;
    }


    /**
     * Decompose a geometry recursively into simple components.
     * @param geometry input geometry
     * @param list a list of simple components (Point, LineString or Polygon)
     */
    private static void decompose(Geometry geometry, List<Geometry> list) {
        for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
            Geometry component = geometry.getGeometryN(i);
            if (component instanceof GeometryCollection) decompose(component, list);
            else list.add(component);
        }
    }



    /**
     * Repair an invalid geometry.
     * <br/>
     * If preserveGeomDim is true, makeValid will remove degenerated geometries from
     * the result, i.e geometries which dimension is lower than the input geometry
     * dimension (except for mixed GeometryCollection).
     * <br/>
     * A multi-geometry will always produce a multi-geometry (eventually empty or made
     * of a single component).
     * A simple geometry may produce a multi-geometry (ex. polygon with self-intersection
     * will generally produce a multi-polygon). In this case, it is up to the client to
     * explode multi-geometries if he needs to.
     * <br/>
     * If preserveGeomDim is off, it is up to the client to filter degenerate geometries.
     * <br/>
     * WARNING : for geometries of dimension 1 (linear), duplicate coordinates are
     * preserved as much as possible. For geometries of dimension 2 (areal), duplicate
     * coordinates are generally removed due to the use of overlay operations.
     * @param geometry input geometry
     * @return a valid Geometry
     */
    public Geometry makeValid(Geometry geometry) {
        // Input geometry is exploded into a list of simple components
        List<Geometry> list = new ArrayList<Geometry>(geometry.getNumGeometries());
        decompose(geometry, list);
        // Each single component is made valid
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
        // If preserveGeomDim is true and input geometry is not a GeometryCollection
        // components with a lower dimension than input geometry are removed
        if (preserveGeomDim && !geometry.getClass().getSimpleName().equals("GeometryCollection")) {
            list2 = removeLowerDimension(list2, geometry.getDimension());
        }
        // In a MultiPolygon, polygons cannot touch or overlap each other
        // (adjacent polygons are not merged in the context of a mixed GeometryCollection)
        if (list2.size() > 1) {
            boolean multiPolygon = true;
            for (Geometry geom : list2) {
                if (geom.getDimension() < 2) multiPolygon = false;
            }
            if (multiPolygon) {
                list2 = unionAdjacentPolygons(list2);
            }
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
            Geometry result = geometry.getFactory().buildGeometry(list2);
            // If input geometry was a GeometryCollection and result is a simple geometry
            // create a multi-geometry made of a single component
            if (geometry instanceof GeometryCollection && !(result instanceof GeometryCollection)) {
                if (geometry instanceof MultiPoint && result instanceof Point) {
                    result = geometry.getFactory().createMultiPoint(new Point[]{(Point)result});
                }
                else if (geometry instanceof MultiLineString && result instanceof LineString) {
                    result = geometry.getFactory().createMultiLineString(new LineString[]{(LineString) result});
                }
                else if (geometry instanceof MultiPolygon && result instanceof Polygon) {
                    result = geometry.getFactory().createMultiPolygon(new Polygon[]{(Polygon) result});
                }
            }
            return result;
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

    // Union adjacent polygons to make an invalid MultiPolygon valid
    private List<Geometry> unionAdjacentPolygons(List<Geometry> list) {
        UnaryUnionOp op = new UnaryUnionOp(list);
        Geometry result = op.union();
        if (result.getNumGeometries() < list.size()) {
            list.clear();
            for (int i = 0 ; i < result.getNumGeometries() ; i++) {
                list.add(result.getGeometryN(i));
            }
        }
        return list;
    }


    // If X or Y is null, return an empty Point
    private Point makePointValid(Point point) {
        CoordinateSequence sequence = point.getCoordinateSequence();
        GeometryFactory factory = point.getFactory();
        CoordinateSequenceFactory csFactory = factory.getCoordinateSequenceFactory();
        if (sequence.size() == 0) {
            return point;
        } else if (Double.isNaN(sequence.getOrdinate(0, 0)) || Double.isNaN(sequence.getOrdinate(0, 1))) {
            return factory.createPoint(csFactory.create(0, sequence.getDimension()));
        } else if (sequence.size() == 1) {
            return point;
        } else {
            throw new RuntimeException("JTS cannot create a point from a CoordinateSequence containing several points");
        }
    }

    /**
     * Returns a coordinateSequence free of Coordinates with X or Y NaN value, and if desired, free
     * of duplicated coordinates. makeSequenceValid keeps the original dimension of input sequence.
     * @param sequence input sequence of coordinates
     * @param preserveDuplicateCoord if duplicate coordinates must be preserved
     * @param close if the sequence must be closed
     * @return
     */
    private static CoordinateSequence makeSequenceValid(CoordinateSequence sequence,
                                                        boolean preserveDuplicateCoord, boolean close) {
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
            if (!preserveDuplicateCoord && count > 0 && sequence.getCoordinate(i).equals(sequence.getCoordinate(i-1))) {
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
        CoordinateSequence sequenceWithoutDuplicates = makeSequenceValid(sequence, false, false);
        GeometryFactory factory = lineString.getFactory();
        if (sequenceWithoutDuplicates.size() == 0) {
            // no valid point -> empty LineString
            return factory.createLineString(factory.getCoordinateSequenceFactory().create(0, sequence.getDimension()));
        } else if (sequenceWithoutDuplicates.size() == 1) {
            // a single valid point -> returns a Point
            if (preserveGeomDim) {
                return factory.createLineString(factory.getCoordinateSequenceFactory().create(0, sequence.getDimension()));
            } else {
                return factory.createPoint(sequenceWithoutDuplicates);
            }
        } else if (preserveDuplicateCoord){
            return factory.createLineString(makeSequenceValid(sequence, true, false));
        } else {
            return factory.createLineString(sequenceWithoutDuplicates);
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
        CoordinateSequence outerRingSeq = makeSequenceValid(polygon.getExteriorRing().getCoordinateSequence(), false, true);
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
            List<Geometry> degeneratedRings = new ArrayList<Geometry>();
            for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
                CoordinateSequence seq = makeSequenceValid(polygon.getInteriorRingN(i).getCoordinateSequence(), false, true);
                if (seq.size() == 0) continue;
                else if (seq.size() == 1) degeneratedRings.add(factory.createPoint(seq));
                else if (seq.size() < 4) degeneratedRings.add(factory.createLineString(seq));
                else innerRings.add(factory.createLinearRing(seq));
            }
            Polygon poly = factory.createPolygon(factory.createLinearRing(outerRingSeq),
                    innerRings.toArray(new LinearRing[innerRings.size()]));
            if (degeneratedRings.isEmpty()) {
                return poly;
            }
            else {
                degeneratedRings.add(0, poly);
                return factory.buildGeometry(degeneratedRings);
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
        Geometry geom = getArealGeometryFromLinearRing(exteriorRing);
        for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
            LinearRing interiorRing = (LinearRing)polygon.getInteriorRingN(i);
            // TODO avoid the use of difference operator
            geom = geom.difference(getArealGeometryFromLinearRing(interiorRing));
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
    protected Geometry getArealGeometryFromLinearRing(LinearRing ring) {
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
            if (ring.getCoordinateSequence().getDimension() == 4 && preserveCoordDim) {
                geoms = restoreFourthDimension(ring, geoms);
            }
            return ring.getFactory().buildGeometry(geoms);
        }
    }

    private Collection<Geometry> restoreFourthDimension(LinearRing ring, Collection<Geometry> geoms) {
        CoordinateSequence sequence = ring.getCoordinateSequence();
        GeometryFactory factory = ring.getFactory();
        CoordinateSequenceFactory csFactory = factory.getCoordinateSequenceFactory();
        if (sequence.getDimension() < 4) {
            return geoms;
        }
        Collection<Geometry> result = new ArrayList<Geometry>();
        Map<Coordinate,Double> map = new HashMap<Coordinate,Double>();
        for (int i = 0 ; i < sequence.size() ; i++) {
            map.put(sequence.getCoordinate(i), sequence.getOrdinate(i, 3));
        }
        for (Geometry geom : geoms) {
            if (geom instanceof Point) {
                result.add(factory.createPoint(restoreFourthDimension(
                        ((Point) geom).getCoordinateSequence().toCoordinateArray(), map)));
            }
            else if (geom instanceof LineString) {
                result.add(factory.createLineString(restoreFourthDimension(
                        ((LineString) geom).getCoordinateSequence().toCoordinateArray(), map)));
            }
            if (geom instanceof Polygon) {
                result.add(factory.createPolygon(restoreFourthDimension(
                        ((Polygon) geom).getExteriorRing().getCoordinateSequence().toCoordinateArray(), map)));
            }
        }
        return result;
    }

    CoordinateSequence restoreFourthDimension(Coordinate[] array, Map<Coordinate,Double> map) {
        CoordinateSequence seq = new PackedCoordinateSequenceFactory(DOUBLE, 4).create(array.length, 4);
        for (int i = 0 ; i < array.length ; i++) {
            seq.setOrdinate(i,0,array[i].x);
            seq.setOrdinate(i,1,array[i].y);
            seq.setOrdinate(i,2,array[i].z);
            Double d = map.get(array[i]);
            seq.setOrdinate(i,3,d==null?Double.NaN:d);
        }
        return seq;
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
        WKTReader reader = new WKTReader();
        MakeValidOp op = new MakeValidOp();
        MakeValidOp opGeomDimNotPreserved = new MakeValidOp().setPreserveGeomDim(false);
        MakeValidOp opDupCoordNotPreserved = new MakeValidOp().setPreserveDuplicateCoord(false);
        MakeValidOp opCoordDimNotPreserved = new MakeValidOp().setPreserveCoordDim(false);
        Geometry input, result;

        // -----------------------------------------------------
        // Test Point
        // -----------------------------------------------------
        Point p1 = factory.createPoint(new Coordinate(0,0));
        Point p2 = op.makePointValid(p1);
        assert p1.equals(p2);

        // return an empty point if X or Y is NaN
        p1 = factory.createPoint(new Coordinate(Double.NaN,0));
        p2 = op.makePointValid(p1);
        assert !p1.isEmpty();
        assert p2.isEmpty();

        // return an empty point if X or Y is NaN
        p1 = factory.createPoint(new Coordinate(0, Double.NaN));
        p2 = op.makePointValid(p1);
        assert !p1.isEmpty();
        assert p2.isEmpty();

        // Preserve fourth coord dimension
        p1 = factory.createPoint(DOUBLE_FACTORY.create(new double[]{0,1,2,3}, 4));
        p2 = op.makePointValid(p1);
        assert p1.getCoordinateSequence().getOrdinate(0,3) == p2.getCoordinateSequence().getOrdinate(0,3);



        // -----------------------------------------------------
        // Test CoordinateSequence
        // -----------------------------------------------------
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
        assert cs2.size() == 2;
        assert cs2.getOrdinate(0,3) == 3;

        cs1 = DOUBLE_FACTORY.create(new double[]{0,1,2,3,0,1,6,7}, 4);
        cs2 = makeSequenceValid(cs1, false, false);
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


        // -----------------------------------------------------
        // Test LineString
        // -----------------------------------------------------
        reader = new WKTReader();
        input = reader.read("LINESTRING(0 0, 10 0, 20 0, 20 0, 30 0)");
        assert input.getNumPoints() == 5;
        // preserve duplicate point
        result = op.makeValid(input);
        assert result.getNumPoints() == 5;
        // do not preserve duplicate point
        result = opDupCoordNotPreserved.makeValid(input);
        assert result.getNumPoints() == 4;

        cs1 = DOUBLE_FACTORY.create(new double[]{0,0,2,3, 10,0,4,5, 20,0,6,7}, 4);
        input = new GeometryFactory(new PackedCoordinateSequenceFactory(DOUBLE, 4)).createLineString(cs1);
        // preserve 4th coordinate dimension
        result = op.makeValid(input);
        assert ((LineString)result).getCoordinateSequence().getOrdinate(1,3) == 5;
        // do not preserve 4th coordinate dimension (actually, coord dim is preserved in the case of LineString)
        result = opCoordDimNotPreserved.makeValid(input);
        assert ((LineString)result).getCoordinateSequence().getDimension() == 4;


        input = reader.read("LINESTRING(0 0, 20 0, 20 20, 20 20, 10 -10)");
        assert input.getNumPoints() == 5;
        Set<LineString> set = op.nodeLineString(input.getCoordinates(), input.getFactory());
        assert set.size() == 3; // node + merge -> 3 line strings

        // -----------------------------------------------------
        // Test Polygon
        // -----------------------------------------------------

        // invalid polygon (single linearRing drawing 2 triangles joined by a line)
        input = reader.read("POLYGON (( 322 354, 322 348, 325 351, 328 351, 331 348, 331 354, 328 351, 325 351, 322 354 ))");
        result = op.makeValid(input);
        assert result.getNumGeometries() == 2;
        result = opGeomDimNotPreserved.makeValid(input);
        assert result.getNumGeometries() == 3;

        // invalid polygon (single linearRing drawing 2 triangles joined by a line, first triangle has duplicated segments)
        input = reader.read("POLYGON (( 322 354, 322 348, 322 354, 322 348, 325 351, 328 351, 331 348, 331 354, 328 351, 325 351, 322 354 ))");
        result = op.makeValid(input);
        assert result.getNumGeometries() == 2;
        result = opGeomDimNotPreserved.makeValid(input);
        assert result.getNumGeometries() == 3;

        // restore fourth dimension (self-intersection)
        cs1 = DOUBLE_FACTORY.create(new double[]{0,0,0,5, 10,0,0,1, 0,10,0,2, 10,10,0,3, 0,0,0,5}, 4);
        input = factory.createPolygon(cs1);
        assert input.getNumGeometries() == 1;
        result = op.makeValid(input);
        assert result.getNumGeometries() == 2;
        assert ((Polygon)result.getGeometryN(0)).getExteriorRing().getCoordinateSequence().getDimension() == 4;
        assert ((Polygon)result.getGeometryN(0)).getExteriorRing().getCoordinateSequence().getOrdinate(1,3) > 0;
        result = opCoordDimNotPreserved.makeValid(input);
        assert result.getNumGeometries() == 2;
        assert ((Polygon)result.getGeometryN(0)).getExteriorRing().getCoordinateSequence().getDimension() == 3;
        // -----------------------------------------------------
        // Test MultiPolygon
        // -----------------------------------------------------
        input = reader.read("MULTIPOLYGON (((0 0, 10 0, 10 10, 0 10, 0 0)), ((10 0, 20 0, 20 10, 10 10, 10 0)))");
        assert input.getNumGeometries() == 2;
        result = op.makeValid(input);
        assert result.getNumGeometries() == 1;

    }

}
