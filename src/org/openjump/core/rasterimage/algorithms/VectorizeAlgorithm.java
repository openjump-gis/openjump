package org.openjump.core.rasterimage.algorithms;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.util.*;
import java.util.stream.Collectors;

import com.vividsolutions.jump.workbench.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
 * This class provides a complete set to transform a grid (GridWrapperNotInterpolated.class) derived 
 * from a RasterImageLayer.class into vector objetcs.
 * All methods derived from AdbToolbox project, from Sextante and from OpenJUMP inner methods.
 * To build a grid from RasterImageLayer: 
 * OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
 * rstLayer.create(rLayer, false);
 * GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(rstLayer, rstLayer.getLayerGridExtent());
 * @author Beppe
 *
 */
public class VectorizeAlgorithm {

    public static WorkbenchFrame frame;// = JUMPWorkbench.getInstance().getFrame();

    /**
     * Create a FeatureCollection of polygons defining a GridWrapperNotInterpolated and number of band
     * AdbToolbox algorithm
     * @param gwrapper GridWrapperNotInterpolated
     * @param explodeMultipolygons Explode MultiPolygons in Polygons
     * @param band Number of band (0,1,2,etc)
     * @return a FeatureCollection containing vectorized polygons
     */
    public static FeatureCollection toPolygonsAdbToolBox(
            GridWrapperNotInterpolated gwrapper, boolean explodeMultipolygons,
            String attributeName, int band) {

        final double xCellSize = gwrapper.getGridExtent().getCellSize().x;
        final double yCellSize = gwrapper.getGridExtent().getCellSize().y;
        final double xllCorner = gwrapper.getGridExtent().getXMin();
        final double yllCorner = gwrapper.getGridExtent().getYMin();
        final double noData = gwrapper.getNoDataValue();

        // Find unique values and associate an empty list of polygons to each
        final Map<Double,List<Polygon>> uniqueVals = findUniqueVals(gwrapper, noData, band);

        final GeometryFactory geomFactory = new GeometryFactory();
        Polygon polygon;

        final int nCols = gwrapper.getGridExtent().getNX();
        final int nRows = gwrapper.getGridExtent().getNY();
        for (int r = 0; r <= nRows + 1; r++) {
            double oldVal = noData;
            int cStart = 0;
            for (int c = 0; c <= nCols + 1; c++) {
                final double val = gwrapper.getCellValueAsDouble(c, r, band);
                if (!sameAs(val, oldVal, noData)) {
                    // Get the polygon made of all contiguous pixels with the same value
                    // != does not work well with NaN : add a specific test
                    if (!isNoData(oldVal, noData)) {
                        polygon = (Polygon)geomFactory.toGeometry(new Envelope(
                            xllCorner + cStart * xCellSize,
                            xllCorner + c * xCellSize,
                            yllCorner + (nRows-r-1) * yCellSize,
                            yllCorner + (nRows-r) * yCellSize
                            ));
                        uniqueVals.get(oldVal).add(polygon);
                    }
                    oldVal = val;
                    cStart = c;
                }
            }
        }

        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featSchema.addAttribute("ID", AttributeType.INTEGER);
        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        Feature feature;
        int ID = 1;
        // Create feature collection
        final FeatureCollection featColl = new FeatureDataset(featSchema);
        for (Map.Entry<Double,List<Polygon>> entry : uniqueVals.entrySet()) {
            // Collapse polygons
            Geometry geom = CascadedPolygonUnion.union(entry.getValue());
            geom = DouglasPeuckerSimplifier.simplify(geom, 0);
            if (explodeMultipolygons) {
                // From multipolygons to single polygons
                for (int g = 0; g < geom.getNumGeometries(); g++) {
                    feature = new BasicFeature(featSchema);
                    feature.setGeometry(geom.getGeometryN(g));
                    feature.setAttribute(1, ID);
                    feature.setAttribute(2, entry.getKey());
                    featColl.add(feature);
                    ID++;
                }
            } else {
                feature = new BasicFeature(featSchema);
                feature.setAttribute(1, ID);
                feature.setGeometry(geom);
                feature.setAttribute(2, entry.getKey());
                featColl.add(feature);
                ID++;
            }
        }
        return featColl;
    }

    /**
     * Return true if a == b or if both a and b are nodata or NaN
     * @param a first value
     * @param b second value
     * @param noData double representing noData (maybe NaN or any double value)
     * @return true if both a and b are noData value or both a and b represent
     * same data value
     */
    private static boolean sameAs(double a, double b, double noData) {
        boolean aIsNaN = isNoData(a, noData);
        boolean bIsNaN = isNoData(b, noData);
        return (aIsNaN && bIsNaN) || (!aIsNaN && !bIsNaN && a == b);
    }

    private static boolean isNoData(double a, double noData) {
        return Double.isNaN(a) || (!Double.isNaN(noData) && a == noData);
    }

    private static Map<Double,List<Polygon>> findUniqueVals(GridWrapperNotInterpolated gwrapper,
            double noData, int band) {
        // Creates a map associating an empty list of polygons to each unique value
        Map<Double,List<Polygon>> map = new TreeMap<>();
        final int nx = gwrapper.getNX();
        final int ny = gwrapper.getNY();
        for (int x = 0; x < nx; x++) {
            for (int y = 0; y < ny; y++) {
                final double value = gwrapper.getCellValueAsDouble(x, y, band);
                // != does not work well with NaN : add an explicit test
                if (!isNoData(value, noData)) {
                    map.computeIfAbsent(value, a -> new ArrayList<>());
                }
            }
        }
        return map;
    }

    // [mmichaud - 2021-05-09] : add a polygonizer as fast as Sextante and giving
    // the same precise result as the one of Adb
    public static FeatureCollection toPolygonsMikeToolBox(
            GridWrapperNotInterpolated gwrapper,
            boolean simplify,
            String attributeName, int band) {

        final int width = gwrapper.getGridExtent().getNX();
        final int height = gwrapper.getGridExtent().getNY();
        final double noData = gwrapper.getNoDataValue();
        final List<Face> faces = new ArrayList<>();

        final double xCellSize = gwrapper.getGridExtent().getCellSize().x;
        final double yCellSize = gwrapper.getGridExtent().getCellSize().y;
        final double xllCorner = gwrapper.getGridExtent().getXMin();
        final double yllCorner = gwrapper.getGridExtent().getYMin();
        // Transformation from image to model coordinates
        AffineTransformation transform = new AffineTransformation(
            xCellSize, 0.0, xllCorner, 0.0, -yCellSize, yllCorner+height*yCellSize
        );
        // Compact structure to flag visited pixels on a single bit
        final BooleanMatrix m = new BooleanMatrix(width, height);
        // Initialization : flag pixels with no value
        for (int r = 0 ; r < height ; r++) {
            for (int c = 0 ; c < width ; c++) {
                double val = gwrapper.getCellValueAsDouble(c, r, band);
                if (isNoData(val, noData)) m.set(r, c);
            }
        }
        // Main loop on the image
        for (int r = 0 ; r < height ; r++) {
            for (int c = 0 ; c < width ; c++) {
                if (!m.isSet(r, c)) {
                    // if the pixel has not yet been visited, it is used
                    // as the root of a new face
                    double val = gwrapper.getCellValueAsDouble(c, r, band);
                    Face face = new Face(val);
                    faces.add(face);
                    face.cells.add(new Cell(c,r));
                    // iterativeExpansion will explore pixel neighbours having
                    // the same value until no more are left
                    iterativeExpansion(face, band, gwrapper, m);
                }
            }
        }
        // Last part get all segments making a face boundary and build
        // the polygon by merging segments and polygonizing the result
        // Create feature collection
        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featSchema.addAttribute("ID", AttributeType.INTEGER);
        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        final FeatureCollection featColl = new FeatureDataset(featSchema);
        int ID = 1;
        for (Face face : faces) {
            LineMerger merger = new LineMerger();
            // Build boundaries from all segments of a face
            merger.add(face.limits.stream().map(it -> it.toGeometry(gf)).collect(Collectors.toList()));
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(merger.getMergedLineStrings());
            Collection<Polygon> polygons = polygonizer.getPolygons();
            Geometry geom = null;
            // If several polygons are found, it means that the face has holes
            // In this case, only the polygon with holes is of interest
            if (polygons.size() > 1) {
                for (Polygon poly : polygons) {
                    if (poly.getNumInteriorRing() > 0) geom = poly;
                    break;
                }
            } else geom = polygons.iterator().next();
            if (geom == null) {
                Logger.warn("Merge/Polygonization gave an unexpected result " + polygons);
            }
            geom = transform.transform(geom);
            if (simplify)
                geom = DouglasPeuckerSimplifier.simplify(geom, 0);
            Feature feature = new BasicFeature(featSchema);
            feature.setAttribute(1, ID);
            feature.setGeometry(geom);
            feature.setAttribute(2, face.value);
            featColl.add(feature);
            ID++;
        }
        System.out.println("end " + new Date());
        return featColl;
    }

    static GeometryFactory gf = new GeometryFactory();

    static void iterativeExpansion(Face face, int band,
                                  GridWrapperNotInterpolated grid,
                                  BooleanMatrix m) {

        Set<Cell> exp = new HashSet<>();

        while (!face.cells.isEmpty()) {

            exp.clear();

            for (Iterator<Cell> it = face.cells.iterator() ; it.hasNext() ; ) {
                Cell cell = it.next();
                int col = cell.col;
                int row = cell.row;
                it.remove(); // only way to remove element from the collection being iterated
                             // other modifications (add) are done outside the loop
                m.set(row, col);

                // right side : col+1, row
                if (col + 1 < m.width && !m.isSet(row, col + 1) &&
                    grid.getCellValueAsDouble(col + 1, row, band) == face.value)
                    exp.add(new Cell(col + 1, row));
                else if (col + 1 >= m.width || grid.getCellValueAsDouble(col + 1, row, band) != face.value) {
                    face.limits.add(new Segment(col + 1, row, col + 1, row + 1));
                }

                // bottom side : col, row+1
                if (row + 1 < m.height && !m.isSet(row + 1, col) &&
                    grid.getCellValueAsDouble(col, row + 1, band) == face.value)
                    exp.add(new Cell(col, row + 1));
                else if (row + 1 >= m.height || grid.getCellValueAsDouble(col, row + 1, band) != face.value) {
                    face.limits.add(new Segment(col, row + 1, col + 1, row + 1));
                }

                // left side : col-1, row
                if (col - 1 > -1 && !m.isSet(row, col - 1) &&
                    grid.getCellValueAsDouble(col - 1, row, band) == face.value)
                    exp.add(new Cell(col - 1, row));
                else if (col - 1 < 0 || grid.getCellValueAsDouble(col - 1, row, band) != face.value) {
                    face.limits.add(new Segment(col, row, col, row + 1));
                }

                // top side : col, row-1
                if (row - 1 > -1 && !m.isSet(row - 1, col) &&
                    grid.getCellValueAsDouble(col, row - 1, band) == face.value)
                    exp.add(new Cell(col, row - 1));
                else if (row - 1 < 0 || grid.getCellValueAsDouble(col, row - 1, band) != face.value) {
                    face.limits.add(new Segment(col, row, col + 1, row));
                }
            }
            face.cells.addAll(exp);
        }
    }

    /**
     * Structure to visit all contiguous cells with the same value
     * and holding limits with cells having different values
     */
    static class Face {
        double value;
        Set<Segment> limits;
        Set<Cell> cells;
        Face(double value) {
            this.value = value;
            this.cells = new HashSet<>();
            this.limits = new HashSet<>();
        }
    }

    // A cell of the image made of a column and a row number
    static class Cell {
        public int col, row;
        Cell(int col, int row) {
            this.col = col;
            this.row = row;
        }
        @Override public boolean equals(Object o) {
            if (o instanceof Cell) {
                Cell cell = (Cell)o;
                return col == cell.col && row == cell.row;
            } else return false;
        }
        @Override public int hashCode() {
            int result = 17;
            result = 37 * result + col;
            result = 37 * result + row;
            return result;
        }
        @Override public String toString() {
            return "("+col+","+row+")";
        }
    }

    /** Segment is a border between two cells with different values.*/
    static class Segment {
        double x0, y0,x1, y1;
        Segment(double x0, double y0, double x1, double y1) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
        Geometry toGeometry(GeometryFactory gf) {
            return gf.createLineString(new Coordinate[]{
                new Coordinate(x0,y0),
                new Coordinate(x1,y1)
            });
        }
        @Override public boolean equals(Object o) {
            if (o instanceof Segment) {
                Segment s = (Segment)o;
                return x0 == s.x0 && y0 == s.y0 && x1 == s.x1 && y1 == s.y1;
            } else return false;
        }
        @Override public int hashCode() {
            //Algorithm from Effective Java by Joshua Bloch [Jon Aquino]
            int result = 17;
            result = 37 * result + Coordinate.hashCode(x0);
            result = 37 * result + Coordinate.hashCode(y0);
            result = 37 * result + Coordinate.hashCode(x1);
            result = 37 * result + Coordinate.hashCode(y1);
            return result;
        }
    }

    /** A compact structure consuming 1 bit per pixel to keep track of visited cells.*/
    static class BooleanMatrix {
        int width, height;
        long[] array;
        BooleanMatrix(int width, int height) {
            this.width = width;
            this.height = height;
            double length = Math.ceil((double)width*height/64);
            array = new long[(int)length];
        }
        void set(int row, int col) {
            long index = (long)row*width+col;
            array[(int)(index/64)] |= (1L << index%64);
        }
        void unset(int row, int col) {
            long index = (long)row*width+col;
            array[(int)(index/64)] &= ~(1L << index%64);
        }
        boolean isSet(int row, int col) {
            long index = (long)row*width+col;
            return (array[(int)(index/64)] & (1L << index%64)) == (1L << index%64);
        }
    }


    private static int[][] m_Lock;
    private static char[][] m_Area;
    private static int m_iNX;
    private static int m_iNY;

    /** Create a FeatureCollection of polygons defining a GridWrapperNotInterpolated and number of band
      * Sextante algorithm
      * @param gwrapper a GridWrapperNotInterpolated
      * @param attributeName an attribute name
      * @param band the image band to use
      * @return a FeatureCollection containing vectorized polygons
      */
    public static FeatureCollection toPolygonsSextante(
            GridWrapperNotInterpolated gwrapper, String attributeName, int band) {
        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featSchema.addAttribute("ID", AttributeType.INTEGER);
        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        // Create feature collection
        final FeatureCollection featColl = new FeatureDataset(featSchema);
        int x, y, ID;
        double dValue;
        m_iNX = gwrapper.getNX();
        m_iNY = gwrapper.getNY();
        m_Lock = new int[m_iNY][m_iNX];
        m_Area = new char[m_iNY + 1][m_iNX + 1];
        for (y = 0, ID = 1; y < m_iNY; y++) {
            for (x = 0; x < m_iNX; x++) {
                dValue = gwrapper.getCellValueAsDouble(x, y, band);
                if (!gwrapper.isNoDataValue(dValue) && (m_Lock[y][x] == 0)) {
                    Discrete_Lock(gwrapper, x, y, ID, band);
                    featColl.add(Discrete_Area(gwrapper, featSchema,
                            attributeName, x, y, ID, band));
                    ID++;
                }
            }
        }
        System.gc();
        return featColl;
    }

    private static void Discrete_Lock(GridWrapperNotInterpolated gwrapper,
            int x, int y, final int ID, int band) {
        final int[] xTo = { 0, 1, 0, -1 };
        final int[] yTo = { 1, 0, -1, 0 };
        final char[] goDir = { 1, 2, 4, 8 };
        boolean isBorder, doRecurse;
        char goTemp = 0;
        char[] goStack = new char[50];
        int[] xStack = new int[50];
        int[] yStack = new int[50];
        int i, ix, iy, iStack = 0;
        double dValue, dValue2;
        dValue = gwrapper.getCellValueAsDouble(x, y, band);
        for (iy = 0; iy <= m_iNY; iy++) {
            for (ix = 0; ix <= m_iNX; ix++) {
                m_Area[iy][ix] = 0;
            }
        }
        do {
            if (m_Lock[y][x] == 0) {
                if (goStack.length <= iStack) {
                    final char[] cAux = new char[goStack.length + 50];
                    System.arraycopy(goStack, 0, cAux, 0, goStack.length);
                    goStack = cAux;
                    int[] iAux = new int[xStack.length + 50];
                    System.arraycopy(xStack, 0, iAux, 0, xStack.length);
                    xStack = iAux;
                    iAux = new int[yStack.length + 50];
                    System.arraycopy(yStack, 0, iAux, 0, yStack.length);
                    yStack = iAux;
                }
                goStack[iStack] = 0;
                m_Lock[y][x] = ID;
                for (i = 0; i < 4; i++) {
                    ix = x + xTo[i];
                    iy = y + yTo[i];
                    isBorder = true;
                    dValue2 = gwrapper.getCellValueAsDouble(ix, iy, band);
                    if ((ix >= 0) && (ix < m_iNX) && (iy >= 0) && (iy < m_iNY)
                            && (dValue == dValue2)) {
                        isBorder = false;
                        if (m_Lock[iy][ix] == 0) {
                            goStack[iStack] |= goDir[i];
                        }
                    }
                    if (isBorder) {
                        switch (i) {
                        case 0:
                            m_Area[y + 1][x]++;
                            m_Area[y + 1][x + 1]++;
                            break;
                        case 1:
                            m_Area[y][x + 1]++;
                            m_Area[y + 1][x + 1]++;
                            break;
                        case 2:
                            m_Area[y][x]++;
                            m_Area[y][x + 1]++;
                            break;
                        case 3:
                            m_Area[y][x]++;
                            m_Area[y + 1][x]++;
                            break;
                        }
                    }
                }
            }
            doRecurse = false;
            for (i = 0; i < 4; i++) {
                if ((goStack[iStack] & goDir[i]) != 0) {
                    if (doRecurse) {
                        goTemp |= goDir[i];
                    } else {
                        goTemp = 0;
                        doRecurse = true;
                        xStack[iStack] = x;
                        yStack[iStack] = y;
                        x = x + xTo[i];
                        y = y + yTo[i];
                    }
                }
            }
            if (doRecurse) {
                goStack[iStack++] = goTemp;
            } else if (iStack > 0) {
                iStack--;
                x = xStack[iStack];
                y = yStack[iStack];
            }
        } while (iStack > 0);

    }

    private static Feature Discrete_Area(GridWrapperNotInterpolated gwrapper,
            FeatureSchema featSchema, String attributeName, int x, int y,
            final int ID, int band) {
        final int[] xTo = { 0, 1, 0, -1 };
        final int[] yTo = { 1, 0, -1, 0 };
        final int[] xLock = { 0, 0, -1, -1 }, yLock = { 0, -1, -1, 0 };
        boolean bContinue, bStart;
        int i, ix, iy, ix1, iy1, dir, iStart;
        final double xMin = gwrapper.getGridExtent().getXMin();
        final double yMax = gwrapper.getGridExtent().getYMax();
        final double dCellSizeX = gwrapper.getCellSize().x;
        final double dCellSizeY = gwrapper.getCellSize().y;
        double xFirst = 0, yFirst = 0;
        final ArrayList<Coordinate> coordinates = new ArrayList<>();
        Feature feature;
        feature = new BasicFeature(featSchema);
        feature.setAttribute(1, ID);
        feature.setAttribute(2, gwrapper.getCellValueAsDouble(x, y, band));
        xFirst = xMin + (x) * dCellSizeX;
        yFirst = yMax - (y) * dCellSizeY;
        coordinates.add(new Coordinate(xFirst, yFirst));
        iStart = 0;
        bStart = true;
        do {
            coordinates.add(new Coordinate(xMin + (x) * dCellSizeX, yMax - (y)
                    * dCellSizeY));
            m_Area[y][x] = 0;
            bContinue = false;
            while (true) {
                // assure clockwise direction at starting point
                if (bStart) {
                    for (i = 0; i < 4; i++) {
                        ix = x + xTo[i];
                        iy = y + yTo[i];
                        if ((ix >= 0) && (ix <= m_iNX) && (iy >= 0)
                                && (iy <= m_iNY) && (m_Area[iy][ix] > 0)) {
                            // check, if inside situated cell (according to
                            // current direction) is locked
                            ix1 = x + xLock[i];
                            iy1 = y + yLock[i];
                            if ((ix1 >= 0) && (ix1 <= m_iNX) && (iy1 >= 0)
                                    && (iy1 <= m_iNY)
                                    && (m_Lock[iy1][ix1] == ID)) {
                                x = ix;
                                y = iy;
                                iStart = (i + 3) % 4;
                                bContinue = true;
                                bStart = false;
                                break;
                            }
                        }
                    }
                } else {
                    for (i = iStart; i < iStart + 4; i++) {
                        dir = i % 4;
                        ix = x + xTo[dir];
                        iy = y + yTo[dir];

                        if ((ix >= 0) && (ix <= m_iNX) && (iy >= 0)
                                && (iy <= m_iNY) && (m_Area[iy][ix] > 0)) {
                            if (i < iStart + 3) {
                                // check, if inside situated cell (according to
                                // current direction) is locked
                                ix1 = x + xLock[dir];
                                iy1 = y + yLock[dir];

                                if ((ix1 >= 0) && (ix1 <= m_iNX) && (iy1 >= 0)
                                        && (iy1 <= m_iNY)
                                        && (m_Lock[iy1][ix1] == ID)) {
                                    x = ix;
                                    y = iy;
                                    iStart = (i + 3) % 4;
                                    bContinue = true;
                                    break;
                                }
                            } else {
                                x = ix;
                                y = iy;
                                bContinue = true;
                                iStart = (i + 3) % 4;
                                break;
                            }
                        }
                    }
                }
                break;
            }
            ;
        } while (bContinue);
        coordinates.add(new Coordinate(xFirst, yFirst));
        final Coordinate[] coords = new Coordinate[coordinates.size()];
        for (i = 0; i < coords.length; i++) {
            coords[i] = coordinates.get(i);
        }
        final GeometryFactory gf = new GeometryFactory();
        if (coords.length > 1) {
            final LinearRing ring = gf.createLinearRing(coords);
            final Polygon polyg = gf.createPolygon(ring, null);
            feature.setGeometry(polyg);
        }
        return feature;

    }

    private char[][] m_Row;
    private char[][] m_Col;

    private final OpenJUMPSextanteRasterLayer m_Visited = new OpenJUMPSextanteRasterLayer();
    private final OpenJUMPSextanteRasterLayer m_Visited2 = new OpenJUMPSextanteRasterLayer();
    private final GeometryFactory m_GF = new GeometryFactory();
    private boolean removeZeroCells = false;

    /**
     * Convert a DTM raster to a feature collection of contours (linestrings) defining
     * a GridWrapperNotInterpolated, a minimum and maximun elevations, a vertical distance
     * between each contour, an attribute name and the band of the raster
     * @param gwrapper a GridWrapperNotInterpolated
     * @param zMin minimum elevation
     * @param zMax maximum elevation
     * @param dDistance vertical distance between contour lines
     * @param attributeName attribute name
     * @param band the band containing elevation data
     * @return a FeatureCollection containing vectorized contour lines
     */
    public FeatureCollection toContours(
            GridWrapperNotInterpolated gwrapper, final double zMin,
            final double zMax, double dDistance, String attributeName, int band) {
        final FeatureCollection featColl = new FeatureDataset(
                schema(attributeName));

        int x, y;
        int i;
        int ID;
        int iNX, iNY;
        double dZ;
        double dValue;
        iNX = gwrapper.getGridExtent().getNX();
        iNY = gwrapper.getGridExtent().getNY();
        m_Row = new char[iNY][iNX];
        m_Col = new char[iNY][iNX];
        if (dDistance <= 0) {
            dDistance = 1;
        }
        for (dZ = zMin, ID = 0; (dZ <= zMax); dZ += dDistance) {
            for (y = 0; y < iNY - 1; y++) {
                for (x = 0; x < iNX - 1; x++) {
                    dValue = gwrapper.getCellValueAsDouble(x, y, band);
                    if (dValue >= dZ) {
                        m_Row[y][x] = (char) (gwrapper.getCellValueAsDouble(
                                x + 1, y, band) < dZ ? 1 : 0);
                        m_Col[y][x] = (char) (gwrapper.getCellValueAsDouble(x,
                                y + 1, band) < dZ ? 1 : 0);
                    } else {
                        m_Row[y][x] = (char) (gwrapper.getCellValueAsDouble(
                                x + 1, y, band) >= dZ ? 1 : 0);
                        m_Col[y][x] = (char) (gwrapper.getCellValueAsDouble(x,
                                y + 1, band) >= dZ ? 1 : 0);
                    }
                }
            }
            for (y = 0; y < iNY - 1; y++) {
                for (x = 0; x < iNX - 1; x++) {
                    if (m_Row[y][x] != 0) {
                        for (i = 0; i < 2; i++) {
                            final Feature feat = findContour(gwrapper, x, y,
                                    dZ, true, ID++, attributeName, band);
                            if (feat.getGeometry().getGeometryType()
                                    .equals("LineString")
                                    || feat.getGeometry().getGeometryType()
                                            .equals("LineString")) {
                                featColl.add(feat);
                            }
                        }
                        m_Row[y][x] = 0;
                    }

                    if (m_Col[y][x] != 0) {
                        for (i = 0; i < 2; i++) {
                            final Feature feat = findContour(gwrapper, x, y,
                                    dZ, false, ID++, attributeName, band);
                            if (feat.getGeometry().getGeometryType()
                                    .equals("LineString")
                                    || feat.getGeometry().getGeometryType()
                                            .equals("LineString")) {
                                featColl.add(feat);
                            }
                        }
                        m_Col[y][x] = 0;
                    }
                }
            }

        }
        System.gc();
        return featColl;
    }

    /**
     * Convert a line raster a feature collection of linestrings defining
     * a GridWrapperNotInterpolated and an attribute.
     * [Currently not working possibly due to a bug of OpenJUMPSextanteRasterLayer.create method]
     * 
     * @param gwrapper a GridWrapperNotInterpolated
     * @param attributeName attribute name
     * @return
     */
    public FeatureCollection toLines(
            GridWrapperNotInterpolated gwrapper, String attributeName) {
        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        final FeatureCollection featColl = new FeatureDataset(featSchema);

        m_Visited.create("a", "a", gwrapper.getGridExtent(),
                DataBuffer.TYPE_DOUBLE, 1, null, frame.getContext()
                        .getLayerManager());
        m_Visited2.create("b", "b", gwrapper.getGridExtent(),
                DataBuffer.TYPE_DOUBLE, 1, null, frame.getContext()
                        .getLayerManager());
        //     m_Visited.setWindowExtent(gwrapper.getGridExtent());
        //     m_Visited2.setWindowExtent(gwrapper.getGridExtent());
        int x, y;
        double byValue;
        int iNX, iNY;
        iNX = gwrapper.getGridExtent().getNX();
        iNY = gwrapper.getGridExtent().getNY();

        for (y = 0; y < m_iNY; y++) {
            for (x = 0; x < m_iNX; x++) {

                final double dValue = gwrapper.getCellValueAsDouble(x, y);
                if (gwrapper.isNoDataValue(dValue) || (dValue == 0)) {
                    m_Visited.setCellValue(x, y, 0.0);
                } else {
                    m_Visited.setCellValue(x, y, 1.0);
                }
            }
        }

        for (y = 0; (y < iNY); y++) {
            for (x = 0; x < iNX; x++) {
                byValue = m_Visited.getCellValueAsDouble(x, y);
                if (byValue == 1) {
                    final Feature feat = createLine(x, y, m_Visited
                            .getWindowGridExtent()
                            .getWorldCoordsFromGridCoords(x, y), gwrapper,
                            featSchema);
                    featColl.add(feat);
                }
            }
        }
        return featColl;

    }

    static int iNX;
    static int iNY;
    private static int m_iLine = 1;

    private Feature createLine(int x, int y, Point2D pt2d2,
            GridWrapperNotInterpolated gwrapper, FeatureSchema featSchema) {
        final GeometryFactory m_GeometryFactory = new GeometryFactory();
        boolean bContinue = false;
        boolean bIsNotNull = false;
        Point pt;
        final Object[] values = new Object[1];

        final Feature feature = new BasicFeature(featSchema);
        final ArrayList<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(pt2d2.getX(), pt2d2.getY()));

        pt2d2 = m_Visited.getWindowGridExtent().getWorldCoordsFromGridCoords(x,
                y);
        coordinates.add(new Coordinate(pt2d2.getX(), pt2d2.getY()));

        do {
            m_Visited.setCellValue(x, y, 0, 0);

            //  m_Visited.setCellValue(x, y, 0);
            final ArrayList<Point> cells = getSurroundingLineCells(x, y,
                    gwrapper);
            m_Visited2.setCellValue(x, y, 0, cells.size());

            //  m_Visited2.setCellValue(x, y, cells.size());
            if (cells.size() == 0) {
                final Coordinate[] coords = new Coordinate[coordinates.size()];
                for (int i = 0; i < coords.length; i++) {
                    coords[i] = coordinates.get(i);
                }
                final Geometry line = m_GeometryFactory
                        .createLineString(coords);
                values[0] = m_iLine++;
                feature.setGeometry(line);
                feature.setAttribute(1, values[0]);
                // m_Lines.addFeature(line, values);
                bContinue = false;
            } else if (cells.size() == 1) {
                pt = cells.get(0);
                pt2d2 = m_Visited.getWindowGridExtent()
                        .getWorldCoordsFromGridCoords(pt.x, pt.y);
                coordinates.add(new Coordinate(pt2d2.getX(), pt2d2.getY()));
                x = pt.x;
                y = pt.y;
                bContinue = true;
                bIsNotNull = true;
            } else {
                if (bIsNotNull) {
                    final Coordinate[] coords = new Coordinate[coordinates
                            .size()];
                    for (int i = 0; i < coords.length; i++) {
                        coords[i] = coordinates.get(i);
                    }

                    final Geometry line = m_GeometryFactory
                            .createLineString(coords);
                    values[0] = m_iLine++;
                    feature.setGeometry(line);
                    feature.setAttribute(1, values[0]);
                }
                for (int i = 0; i < cells.size(); i++) {
                    pt = cells.get(i);
                    m_Visited.setCellValue(pt.x, pt.y, 0, 0);

                }
                for (int i = 0; i < cells.size(); i++) {
                    pt = cells.get(i);
                    pt2d2 = m_Visited.getWindowGridExtent()
                            .getWorldCoordsFromGridCoords(x, y);
                    createLine(pt.x, pt.y, pt2d2, gwrapper, featSchema);
                }

            }
        } while (bContinue);
        return feature;

    }

    private final int[] m_iOffsetX = { 0, 1, 0, -1 };
    private final int[] m_iOffsetY = { -1, 0, 1, 0 };
    private final int[] m_iOffsetXDiag = { -1, 1, 1, -1 };
    private final int[] m_iOffsetYDiag = { -1, -1, 1, 1 };

    private ArrayList<Point> getSurroundingLineCells(final int x,
            final int y, GridWrapperNotInterpolated gwrapper) {

        int i;
        //   final int j;
        final ArrayList<Point> cells = new ArrayList<>();
        final boolean[] bBlocked = new boolean[4];

        for (i = 0; i < 4; i++) {

            if (m_Visited.getCellValueAsByte(x + m_iOffsetX[i], y
                    + m_iOffsetY[i]) == 1

            ) {
                cells.add(new Point(x + m_iOffsetX[i], y + m_iOffsetY[i]));
                bBlocked[i] = true;
                bBlocked[(i + 1) % 4] = true;
            }
        }

        for (i = 0; i < 4; i++) {
            if ((m_Visited.getCellValueAsByte(x + m_iOffsetXDiag[i], y
                    + m_iOffsetYDiag[i]) == 1)
                    && !bBlocked[i]) {
                cells.add(new Point(x + m_iOffsetXDiag[i], y
                        + m_iOffsetYDiag[i]));
            }
        }

        return cells;

    }

    private Feature findContour(GridWrapperNotInterpolated gwrapper,
            final int x, final int y, final double z, final boolean doRow,
            final int ID, String attribueName, int band) {
        final Feature feature = new BasicFeature(schema(attribueName));
        boolean doContinue = true;
        int zx = doRow ? x + 1 : x;
        int zy = doRow ? y : y + 1;
        double d;
        double xPos, yPos;
        final double xMin = gwrapper.getGridExtent().getXMin();
        final double yMax = gwrapper.getGridExtent().getYMax();
        Geometry line;
        final Object[] values = new Object[1];
        final NextContourInfo info = new NextContourInfo();
        final ArrayList<Coordinate> coords = new ArrayList<>();
        info.x = x;
        info.y = y;
        info.iDir = 0;
        info.doRow = doRow;
        do {
            d = gwrapper.getCellValueAsDouble(info.x, info.y, band);
            d = (d - z) / (d - gwrapper.getCellValueAsDouble(zx, zy, band));

            xPos = xMin + gwrapper.getGridExtent().getCellSize().x
                    * (info.x + d * (zx - info.x) + 0.5);
            yPos = yMax - gwrapper.getGridExtent().getCellSize().y
                    * (info.y + d * (zy - info.y) + 0.5);
            coords.add(new Coordinate(xPos, yPos));
            if (!findNextContour(info)) {
                doContinue = findNextContour(info);
            }
            info.iDir = (info.iDir + 5) % 8;
            if (info.doRow) {
                m_Row[info.y][info.x] = 0;
                zx = info.x + 1;
                zy = info.y;
            } else {
                m_Col[info.y][info.x] = 0;
                zx = info.x;
                zy = info.y + 1;
            }
        } while (doContinue);
        values[0] = z;
        final Coordinate[] coordinates = new Coordinate[coords.size()];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = coords.get(i);
        }
        if (coordinates.length > 1) {
            line = m_GF.createLineString(coordinates);
            feature.setGeometry(line);
            feature.setAttribute(1, values[0]);
            //   feature.setAttribute(2, values[1]);
        } else if (coordinates.length == 1) {
            final Geometry point = m_GF.createPoint(coordinates[0]);
            feature.setGeometry(point);
            feature.setAttribute(1, values[0]);
        } else if (coordinates.length == 0) {

            final Geometry gc = m_GF.createGeometryCollection(new Geometry[0]);
            feature.setGeometry(gc);
            feature.setAttribute(1, values[0]);

        }
        return feature;
    }

    private static FeatureSchema schema(String attributeName) {
        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        //  featSchema.addAttribute("ID", AttributeType.INTEGER);
        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        return featSchema;
    }

    private boolean findNextContour(final NextContourInfo info) {
        boolean doContinue;
        if (info.doRow) {
            switch (info.iDir) {
            case 0:
                if (m_Row[info.y + 1][info.x] != 0) {
                    info.y++;
                    info.iDir = 0;
                    doContinue = true;
                    break;
                }
            case 1:
                if (m_Col[info.y][info.x + 1] != 0) {
                    info.x++;
                    info.iDir = 1;
                    info.doRow = false;
                    doContinue = true;
                    break;
                }
            case 2:
            case 3:
                if (info.y - 1 >= 0) {
                    if (m_Col[info.y - 1][info.x + 1] != 0) {
                        info.x++;
                        info.y--;
                        info.doRow = false;
                        info.iDir = 3;
                        doContinue = true;
                        break;
                    }
                }
            case 4:
                if (info.y - 1 >= 0) {
                    if (m_Row[info.y - 1][info.x] != 0) {
                        info.y--;
                        info.iDir = 4;
                        doContinue = true;
                        break;
                    }
                }
            case 5:
                if (info.y - 1 >= 0) {
                    if (m_Col[info.y - 1][info.x] != 0) {
                        info.y--;
                        info.doRow = false;
                        info.iDir = 5;
                        doContinue = true;
                        break;
                    }
                }
            case 6:
            case 7:
                if (m_Col[info.y][info.x] != 0) {
                    info.doRow = false;
                    info.iDir = 7;
                    doContinue = true;
                    break;
                }
            default:
                info.iDir = 0;
                doContinue = false;
            }
        } else {
            switch (info.iDir) {
            case 0:
            case 1:
                if (m_Row[info.y + 1][info.x] != 0) {
                    info.y++;
                    info.doRow = true;
                    info.iDir = 1;
                    doContinue = true;
                    break;
                }
            case 2:
                if (m_Col[info.y][info.x + 1] != 0) {
                    info.x++;
                    info.iDir = 2;
                    doContinue = true;
                    break;
                }
            case 3:
                if (m_Row[info.y][info.x] != 0) {
                    info.doRow = true;
                    info.iDir = 3;
                    doContinue = true;
                    break;
                }
            case 4:
            case 5:
                if (info.x - 1 >= 0) {
                    if (m_Row[info.y][info.x - 1] != 0) {
                        info.x--;
                        info.doRow = true;
                        info.iDir = 5;
                        doContinue = true;
                        break;
                    }
                }
            case 6:
                if (info.x - 1 >= 0) {
                    if (m_Col[info.y][info.x - 1] != 0) {
                        info.x--;
                        info.iDir = 6;
                        doContinue = true;
                        break;
                    }
                }
            case 7:
                if (info.x - 1 >= 0) {
                    if (m_Row[info.y + 1][info.x - 1] != 0) {
                        info.x--;
                        info.y++;
                        info.doRow = true;
                        info.iDir = 7;
                        doContinue = true;
                        break;
                    }
                }
            default:
                info.iDir = 0;
                doContinue = false;
            }
        }
        return (doContinue);
    }

    private static class NextContourInfo {
        public int iDir;
        public int x;
        public int y;
        public boolean doRow;
    }

    public FeatureCollection toGridPoint(
            GridWrapperNotInterpolated gwrapper, int numBands) {
        final FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("geometry", AttributeType.GEOMETRY);
        fs.addAttribute("cellid_x", AttributeType.INTEGER);
        fs.addAttribute("cellid_y", AttributeType.INTEGER);

        for (int i = 0; i < numBands; i++) {
            fs.addAttribute("band" + "_" + i, AttributeType.DOUBLE);
        }
        //-- create a new empty dataset
        final FeatureCollection fd = new FeatureDataset(fs);

        final int nx = gwrapper.getGridExtent().getNX();
        final int ny = gwrapper.getGridExtent().getNY();
        //int numPoints = nx * ny;
        for (int x = 0; x < nx; x++) {//cols
            for (int y = 0; y < ny; y++) {//rows
                final Feature ftemp = new BasicFeature(fs);
                final Point2D pt = gwrapper.getGridExtent()
                        .getWorldCoordsFromGridCoords(x, y);
                final Geometry centerPoint = m_GF.createPoint(new Coordinate(pt
                        .getX(), pt.getY()));
                ftemp.setGeometry(centerPoint);
                for (int i = 0; i < numBands; i++) {
                    final double value = gwrapper.getCellValueAsDouble(x, y, i);
                    ftemp.setAttribute("band" + "_" + i, value);
                }
                ftemp.setAttribute("cellid_x", x);
                ftemp.setAttribute("cellid_y", y);
                //-- add the feature
                fd.add(ftemp);

            }
        }
        return fd;

    }

    public FeatureCollection toPoint(
            GridWrapperNotInterpolated gwrapper, int band) {
        final FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("geometry", AttributeType.GEOMETRY);

        fs.addAttribute("value", AttributeType.DOUBLE);

        //-- create a new empty dataset
        final FeatureCollection fd = new FeatureDataset(fs);

        final int nx = gwrapper.getGridExtent().getNX();
        final int ny = gwrapper.getGridExtent().getNY();
        final double noData = gwrapper.getNoDataValue();

        for (int x = 0; x < nx; x++) {//cols
            for (int y = 0; y < ny; y++) {//rows

                final double value = gwrapper.getCellValueAsDouble(x, y, band);
                if (value != noData) {
                    final Feature ftemp = new BasicFeature(fs);
                    final Point2D pt = gwrapper.getGridExtent()
                            .getWorldCoordsFromGridCoords(x, y);
                    final Geometry centerPoint = m_GF
                            .createPoint(new Coordinate(pt.getX(), pt.getY()));
                    ftemp.setGeometry(centerPoint);

                    //-- add the feature
                    fd.add(ftemp);
                }
            }
        }
        return fd;

    }

    public FeatureCollection toGridPolygon(
            GridWrapperNotInterpolated gwrapper, int maxCells, int numBands) {
        final FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("geometry", AttributeType.GEOMETRY);

        for (int i = 0; i < numBands; i++) {
            fs.addAttribute("band" + "_" + i, AttributeType.DOUBLE);
        }
        //-- create a new empty dataset
        final FeatureCollection fd = new FeatureDataset(fs);
        //-- create points

        final int nx = gwrapper.getGridExtent().getNX();
        final int ny = gwrapper.getGridExtent().getNY();
        final double halfCellDimX = 0.5 * gwrapper.getGridExtent()
                .getCellSize().x;
        final double halfCellDimY = 0.5 * gwrapper.getGridExtent()
                .getCellSize().y;
        //final int numPoints = nx * ny;

        for (int x = 0; x < nx; x++) {//cols
            for (int y = 0; y < ny; y++) {//rows
                final Feature ftemp = new BasicFeature(fs);
                final Point2D pt = gwrapper.getGridExtent()
                        .getWorldCoordsFromGridCoords(x, y);
                final Coordinate[] coords = new Coordinate[5];
                coords[0] = new Coordinate(pt.getX() - halfCellDimX, pt.getY()
                        + halfCellDimY); //topleft
                coords[1] = new Coordinate(pt.getX() + halfCellDimX, pt.getY()
                        + halfCellDimY); //topright
                coords[2] = new Coordinate(pt.getX() + halfCellDimX, pt.getY()
                        - halfCellDimY); //lowerright
                coords[3] = new Coordinate(pt.getX() - halfCellDimX, pt.getY()
                        - halfCellDimY); //lowerleft
                //-- to close poly
                coords[4] = (Coordinate) coords[0].clone(); //topleft
                //-- create the cell poly
                final LinearRing lr = m_GF.createLinearRing(coords);
                final Geometry poly = m_GF.createPolygon(lr, null);
                ftemp.setGeometry(poly);
                //-- set attributes
                double sumvalue = 0;
                for (int i = 0; i < numBands; i++) {
                    final double value = gwrapper.getCellValueAsDouble(x, y, i);
                    ftemp.setAttribute("band" + "_" + i, value);
                    sumvalue = sumvalue + value;
                }
                //-- add the feature
                if (removeZeroCells) {
                    if (sumvalue > 0) {
                        fd.add(ftemp);
                    }
                } else {
                    fd.add(ftemp);
                }
            }
        }
        return fd;

    }

    /** Create a FeatureCollection of polygons defining a GridWrapperNotInterpolated and number of band
     * Sextante algorithm - compatible with OpenKLEM methods
     * @param gwrapper a GridWrapperNotInterpolated
     * @param explodeMultipolygons true to separate disjointed polygons
     * @param attributeName an attribute name
     * @param band the image band to use
     * @return a FeatureCollection containing vectorized polygons
     */
   public static FeatureCollection toPolygons(
           GridWrapperNotInterpolated gwrapper,  boolean explodeMultipolygons,String attributeName,int band) {
       final FeatureSchema featSchema = new FeatureSchema();
       featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
       featSchema.addAttribute("ID", AttributeType.INTEGER);
       featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
       // Create feature collection

       return toPolygonsAdbToolBox(
               gwrapper, explodeMultipolygons,attributeName, band);
   }

   public static void main(String[] args) {
       BooleanMatrix m = new BooleanMatrix(640, 480);
       for (int c = 0 ; c < 640 ; c++) {
           for (int r = 0 ; r < 480 ; r++) {
               if (m.isSet(r, c)) System.out.println("ERROR");
           }
       }
       m.set(37, 19);
       m.unset(37,19);
       for (int c = 0 ; c < 640 ; c++) {
           for (int r = 0 ; r < 480 ; r++) {
               if (m.isSet(r, c)) System.out.println("" + r + " - " + c);
           }
       }

   }
    
}
