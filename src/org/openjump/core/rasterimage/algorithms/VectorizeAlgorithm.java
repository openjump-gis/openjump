package org.openjump.core.rasterimage.algorithms;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.util.ArrayList;
import java.util.Collections;

import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
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

    public static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();

    private static int arrPos(int valIn, double[] arrayIn) {

        int valOut = -9999;
        for (int i = 0; i < arrayIn.length; i++) {
            if (arrayIn[i] == valIn) {
                valOut = i;
                break;
            }
        }
        return valOut;
    }

    /**
     * Create a FeatureCollection of polygons defining GridWrapperNotInterpolated and number of band
     * AdbToolbox algorithm
     * @param gwrapper. GridWrapperNotInterpolated
     * @param explodeMultipolygons. Explode MultiPolygons in Polygons
     * @param band. Number of band (0,1,2,etc)
     * @return
     */
    public static FeatureCollection toPolygonsAdbToolBox(
            GridWrapperNotInterpolated gwrapper, boolean explodeMultipolygons,
            String attributeName, int band) {
        int ID = 1;
        final double cellSize = gwrapper.getGridExtent().getCellSize().x;
        final double xllCorner = gwrapper.getGridExtent().getXMin() + cellSize;
        final double yllCorner = gwrapper.getGridExtent().getYMin() - cellSize;
        final double noData = gwrapper.getNoDataValue();
        // Find unique values
        final double[] uniqueVals = findUniqueVals(gwrapper, noData, band);
        final int uniqueValsCount = uniqueVals.length;
        // Scan lines
        @SuppressWarnings("unchecked")
        final ArrayList<Polygon>[] arrAll = new ArrayList[uniqueValsCount];
        for (int i = 0; i < arrAll.length; i++) {
            arrAll[i] = new ArrayList<Polygon>();
        }
        final Coordinate[] coords = new Coordinate[5];
        final PackedCoordinateSequenceFactory pcsf = new PackedCoordinateSequenceFactory();
        final GeometryFactory geomFactory = new GeometryFactory();
        LinearRing lr;
        Polygon polygon;
        final int nCols = gwrapper.getGridExtent().getNX();
        final int nRows = gwrapper.getGridExtent().getNY();
        final double yurCorner = yllCorner + (nRows * cellSize);
        for (int r = 0; r <= nRows + 1; r++) {
            double oldVal = noData;
            int cStart = 0;
            int cEnd;
            for (int c = 0; c <= nCols + 1; c++) {
                final double val = gwrapper.getCellValueAsDouble(c, r, band);
                if (val != oldVal) {
                    cEnd = c - 1;
                    // Get polygon vertices
                    if (oldVal != noData) {
                        coords[0] = new Coordinate(xllCorner
                                + (cStart * cellSize) - cellSize, yurCorner
                                - (r * cellSize));
                        coords[1] = new Coordinate(coords[0].x, coords[0].y
                                + cellSize);
                        coords[2] = new Coordinate(xllCorner
                                + (cEnd * cellSize), coords[1].y);
                        coords[3] = new Coordinate(coords[2].x, coords[0].y);
                        coords[4] = coords[0];
                        final CoordinateSequence cs = pcsf.create(coords);
                        lr = new LinearRing(cs, geomFactory);
                        polygon = new Polygon(lr, null, geomFactory);
                        arrAll[arrPos((int) oldVal, uniqueVals)].add(polygon);
                    }
                    oldVal = val;
                    cStart = c;
                }
            }
        }
        // Collapse polygons
        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featSchema.addAttribute("ID", AttributeType.INTEGER);
        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        Feature feature;
        // Create feature collection
        final FeatureCollection featColl = new FeatureDataset(featSchema);
        for (int i = 0; i < uniqueValsCount; i++) {
            Geometry geom = CascadedPolygonUnion.union(arrAll[i]);
            geom = DouglasPeuckerSimplifier.simplify(geom, 0);
            geom = TopologyPreservingSimplifier.simplify(geom, 00);
            if (explodeMultipolygons) {
                // From multipolygons to single polygons
                for (int g = 0; g < geom.getNumGeometries(); g++) {
                    feature = new BasicFeature(featSchema);
                    feature.setGeometry(geom.getGeometryN(g));
                    feature.setAttribute(1, new Integer(ID));
                    feature.setAttribute(2, uniqueVals[i]);
                    featColl.add(feature);
                    ID++;
                }
            } else {
                feature = new BasicFeature(featSchema);
                feature.setAttribute(1, new Integer(ID));
                feature.setGeometry(geom);
                feature.setAttribute(2, uniqueVals[i]);
                featColl.add(feature);
                ID++;
            }
        }
        System.gc();
        return featColl;
    }

    private static double[] findUniqueVals(GridWrapperNotInterpolated gwrapper,
            double nodata, int band) {
        // Pass values to 1D array
        final ArrayList<Double> vals = new ArrayList<Double>();
        final int nx = gwrapper.getNX();//rstLayer.getLayerGridExtent().getNX();
        final int ny = gwrapper.getNY();// rstLayer.getLayerGridExtent().getNY();
        for (int x = 0; x < nx; x++) {//cols
            for (int y = 0; y < ny; y++) {//rows
                final double value = gwrapper.getCellValueAsDouble(x, y, band);
                if (value != nodata) {
                    vals.add(gwrapper.getCellValueAsDouble(x, y, band));
                }
            }
        }
        // Find unique values
        Collections.sort(vals);
        final ArrayList<Double> uniqueValsArr = new ArrayList<Double>();
        uniqueValsArr.add(vals.get(0));

        for (int i = 1; i < vals.size(); i++) {
            if (!vals.get(i).equals(vals.get(i - 1))) {
                uniqueValsArr.add(vals.get(i));
            }
        }
        final double[] uniqueVals = new double[uniqueValsArr.size()];
        for (int i = 0; i < uniqueValsArr.size(); i++) {
            uniqueVals[i] = uniqueValsArr.get(i);
        }
        return uniqueVals;
    }

    private static int[][] m_Lock;
    private static char[][] m_Area;
    private static int m_iNX;
    private static int m_iNY;

    /** Create a FeatureCollection of polygons defining GridWrapperNotInterpolated and number of band
      * Sextante algorithm
      * @param gwrapper
      * @param explodeMultipolygons
      * @param attributeName
      * @param band
      * @return
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
        final int xTo[] = { 0, 1, 0, -1 }, yTo[] = { 1, 0, -1, 0 };
        final char goDir[] = { 1, 2, 4, 8 };
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
        final int xTo[] = { 0, 1, 0, -1 }, yTo[] = { 1, 0, -1, 0 };
        final int xLock[] = { 0, 0, -1, -1 }, yLock[] = { 0, -1, -1, 0 };
        boolean bContinue, bStart;
        int i, ix, iy, ix1, iy1, dir, iStart;
        final double xMin = gwrapper.getGridExtent().getXMin();
        final double yMax = gwrapper.getGridExtent().getYMax();
        final double dCellSizeX = gwrapper.getCellSize().x;
        final double dCellSizeY = gwrapper.getCellSize().y;
        double xFirst = 0, yFirst = 0;
        final ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
        Feature feature;
        feature = new BasicFeature(featSchema);
        feature.setAttribute(1, new Integer(ID));
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

    private static char[][] m_Row;
    private static char[][] m_Col;

    private static OpenJUMPSextanteRasterLayer m_Visited = new OpenJUMPSextanteRasterLayer();
    private static OpenJUMPSextanteRasterLayer m_Visited2 = new OpenJUMPSextanteRasterLayer();
    private final static GeometryFactory m_GF = new GeometryFactory();
    private static boolean removeZeroCells = false;

    public static FeatureCollection toContours(
            GridWrapperNotInterpolated gwrapper, final double zMin,
            final double zMax, double dDistance, String attributeName, int band) {
        final FeatureCollection featColl = new FeatureDataset(
                schema(attributeName));

        int x, y;
        int i;
        int ID;
        int iNX, iNY;
        double dZ;
        double dValue = 0;
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

    public static FeatureCollection toLines(
            GridWrapperNotInterpolated gwrapper, String attributeName) {
        final FeatureSchema featSchema = new FeatureSchema();
        featSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        featSchema.addAttribute(attributeName, AttributeType.DOUBLE);
        final FeatureCollection featColl = new FeatureDataset(featSchema);

        m_Visited
                .create("a", "a", gwrapper.getGridExtent(),
                        DataBuffer.TYPE_DOUBLE, 1, frame.getContext()
                                .getLayerManager());
        m_Visited2
                .create("b", "b", gwrapper.getGridExtent(),
                        DataBuffer.TYPE_DOUBLE, 1, frame.getContext()
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

    private static Feature createLine(int x, int y, Point2D pt2d2,
            GridWrapperNotInterpolated gwrapper, FeatureSchema featSchema) {
        final GeometryFactory m_GeometryFactory = new GeometryFactory();
        boolean bContinue = false;
        boolean bIsNotNull = false;
        Point pt;
        final Object values[] = new Object[1];

        final Feature feature = new BasicFeature(featSchema);
        final ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
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
                values[0] = new Integer(m_iLine++);
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
                    values[0] = new Integer(m_iLine++);
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

    private final static int m_iOffsetX[] = { 0, 1, 0, -1 };
    private final static int m_iOffsetY[] = { -1, 0, 1, 0 };
    private final static int m_iOffsetXDiag[] = { -1, 1, 1, -1 };
    private final static int m_iOffsetYDiag[] = { -1, -1, 1, 1 };

    private static ArrayList<Point> getSurroundingLineCells(final int x,
            final int y, GridWrapperNotInterpolated gwrapper) {

        int i;
        //   final int j;
        final ArrayList<Point> cells = new ArrayList<Point>();
        final boolean bBlocked[] = new boolean[4];

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

    private static Feature findContour(GridWrapperNotInterpolated gwrapper,
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
        final Object values[] = new Object[1];
        final NextContourInfo info = new NextContourInfo();
        final ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
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
        //  values[0] = new Integer(ID);
        values[0] = new Double(z);
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

    private static boolean findNextContour(final NextContourInfo info) {
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

    public static FeatureCollection toGridPoint(
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

    public static FeatureCollection toPoint(
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

    public static FeatureCollection toGridPolygon(
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
        final int numPoints = nx * ny;

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
                if (removeZeroCells == true) {
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

}
