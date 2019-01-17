package org.openjump.core.rasterimage.algorithms;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;

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
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * This class provides a complete set to transform a grid (GridWrapperNotInterpolated.class) derived 
 * from a RasterImageLayer.class into vector objetcs.
 * All methods derived from AdbToolbox project, from Sextante and from OpenJUMP inner methods.
 * To build a grid from RasterImageLayer: 
 * OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
 * rstLayer.create(rLayer, false);
 * GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(rstLayer, rstLayer.getLayerGridExtent());
 * @author Giuseppe Aruta
 *
 */
public class VectorizeAlgorithm {

    private static int arrPos(int valIn, int[] arrayIn) {

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
     * 
     * @param gwrapper. GridWrapperNotInterpolated
     * @param explodeMultipolygons. Explode MultiPolygons in Polygons
     * @param band. Number of band (0,1,2,etc)
     * @return
     */
    public static FeatureCollection toPolygons(
            GridWrapperNotInterpolated gwrapper, boolean explodeMultipolygons,
            String attributeName, int band) {

        final double cellSize = gwrapper.getGridExtent().getCellSize().x;
        final double xllCorner = gwrapper.getGridExtent().getXMin() + cellSize;
        final double yllCorner = gwrapper.getGridExtent().getYMin() - cellSize;
        final double noData = gwrapper.getNoDataValue();

        // Find unique values
        final int[] uniqueVals = findUniqueVals(gwrapper, noData, band);
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
        featSchema.addAttribute(attributeName, AttributeType.INTEGER);
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
                    feature.setAttribute(1, uniqueVals[i]);
                    featColl.add(feature);
                }
            } else {
                feature = new BasicFeature(featSchema);
                feature.setGeometry(geom);
                feature.setAttribute(1, uniqueVals[i]);
                featColl.add(feature);
            }
        }
        System.gc();
        return featColl;

    }

    private static int[] findUniqueVals(GridWrapperNotInterpolated gwrapper,
            double nodata, int band) {

        // Pass values to 1D array
        final ArrayList<Integer> vals = new ArrayList<Integer>();

        final int nx = gwrapper.getNX();//rstLayer.getLayerGridExtent().getNX();
        final int ny = gwrapper.getNY();// rstLayer.getLayerGridExtent().getNY();
        for (int x = 0; x < nx; x++) {//cols
            for (int y = 0; y < ny; y++) {//rows
                final double value = gwrapper.getCellValueAsDouble(x, y, band);
                if (value != nodata) {
                    vals.add((int) gwrapper.getCellValueAsDouble(x, y, band));
                }
            }
        }

        // Find unique values
        Collections.sort(vals);

        final ArrayList<Integer> uniqueValsArr = new ArrayList<Integer>();
        uniqueValsArr.add(vals.get(0));

        for (int i = 1; i < vals.size(); i++) {
            if (!vals.get(i).equals(vals.get(i - 1))) {
                uniqueValsArr.add(vals.get(i));
            }
        }

        final int[] uniqueVals = new int[uniqueValsArr.size()];
        for (int i = 0; i < uniqueValsArr.size(); i++) {
            uniqueVals[i] = uniqueValsArr.get(i);
        }

        return uniqueVals;

    }

    private static final String ATTRIBUTE_NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.value");
    private static char[][] m_Row;
    private static char[][] m_Col;
    private final static GeometryFactory m_GF = new GeometryFactory();
    private static boolean removeZeroCells = false;

    public static FeatureCollection toContours(
            GridWrapperNotInterpolated gwrapper, final double dMin,
            final double dMax, double dDistance, String attributeName, int band) {
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

        for (dZ = dMin, ID = 0; (dZ <= dMax); dZ += dDistance) {
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
