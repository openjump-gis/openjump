package org.openjump.core.ui.plugin.raster;

import java.awt.Font;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.math.plot.render.AbstractDrawer;
import org.openjump.core.attributeoperations.AttributeOp;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.plot.Plot2DPanelOJ;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.FeatureCollectionPanel;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;

public class ProfileUtils {

    public final static String LAYER_NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.layer_name");
    public final static String MIN = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum");
    public final static String MEAN = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewTableModel.mean-mode");
    public final static String MAX = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum");
    public final static String SUM = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.sum");
    public final static String CELL_SIZE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_cell");
    public final static String PLOT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot");
    public final static String PROFILEPTS = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.profile-pts");
    public final static String PROFILE_INFO = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");
    public final static String PROFILE_LENGTH = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-length");
    public final static String STARTING_POINT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.starting-point");
    public final static String ENDING_POINT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.ending-point");
    public final static String MEAN_SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.mean-slope");
    public final static String HEIGHT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values");
    public final static String WIDTH = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.2d-distance");

    public static WorkbenchContext context = JUMPWorkbench.getInstance()
            .getFrame().getContext();

    public static RasterImageLayer getLayer() {
        return ProfileGraphPlugIn.dialog
                .getRasterLayer(ProfileGraphPlugIn.CLAYER);

    }

    public static GeometryFactory gf = new GeometryFactory();
    public static FeatureCollection resultFC = null;
    public static FeatureSchema resultFSchema = null;
    public static double dDist = 0, dHorzDist = 0;
    public static double m_dLastX, m_dLastY, m_dLastZ;
    public static int nPoints = 0;
    public static int n = 0;

    public static ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
    static double max, min, sum, mean, width, height, slope, cellsize,
            profLenght;
    protected static Font darkLabelFont = AbstractDrawer.DEFAULT_FONT;

    public static void calculateProfile(Coordinate[] coords)
            throws IOException, RasterImageLayer.RasterDataNotFoundException {

        final RasterImageLayer rLayer = getLayer();

        final LineString line = gf.createLineString(coords);
        if (line.within(rLayer.getWholeImageEnvelopeAsGeometry())) {
            final Random rand = new Random();
            n = rand.nextInt(100) + 1;
            processLine(line);
            if ((resultFC != null) && (resultFC.size() > 0)) {
                final FeatureDataset fd = new FeatureDataset(resultFSchema);
                fd.addAll(resultFC.getFeatures());
                context.getLayerManager().addLayer(
                        StandardCategoryNames.RESULT, PROFILEPTS, fd);
            }

            final FeatureCollectionPanel fPan = new FeatureCollectionPanel(
                    resultFC);
            fPan.setName("" + n);
            fPan.getSouthPanel().setVisible(false);
            AdditionalResults.addAdditionalResult(PLOT + "-" + n + " "
                    + PROFILE_INFO, getStatisticPanel(rLayer));
            AdditionalResults
                    .addAdditionalResult(
                            PLOT
                                    + "-"
                                    + n
                                    + " "
                                    + I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values"),
                            fPan);
            AdditionalResults.addAdditionalResultAndShow(PLOT + "-" + n,
                    getPlotPanel(resultFC));

        } else {
            context.getLayerViewPanel()
                    .getContext()
                    .warnUser(
                            "Query outside the extension of selected Raster layer");
        }
    }

    protected static Coordinate[] toArray(List<Coordinate> coordinates) {
        return coordinates.toArray(new Coordinate[] {});
    }

    public static void processSegment(double x, double y, double x2, double y2)
            throws RasterImageLayer.RasterDataNotFoundException, IOException {
        double dx, dy, d, n;
        dx = Math.abs(x2 - x);
        dy = Math.abs(y2 - y);
        if (dx > 0.0 || dy > 0.0) {
            if (dx > dy) {
                dx /= getLayer().getMetadata().getOriginalCellSize();// this.rstLayer.getWindowCellSize().x;
                n = dx;
                dy /= dx;
                dx = getLayer().getMetadata().getOriginalCellSize();// this.rstLayer.getWindowCellSize().x;
            } else {
                dy /= getLayer().getMetadata().getOriginalCellSize();// this.rstLayer.getWindowCellSize().y;
                n = dy;
                dx /= dy;
                dy = getLayer().getMetadata().getOriginalCellSize();// this.rstLayer.getWindowCellSize().y;
            }
            if (x2 < x) {
                dx = -dx;
            }
            if (y2 < y) {
                dy = -dy;
            }
            for (d = 0.0; d <= n; d++, x += dx, y += dy) {
                addPoint(x, y);
            }
        }
    }

    public static void processLine(Geometry line) throws IOException,
            RasterImageLayer.RasterDataNotFoundException {
        double x, y, x2, y2;
        final Coordinate[] coords = line.getCoordinates();
        for (int i = 0; i < coords.length - 1; i++) {
            x = coords[i].x;
            y = coords[i].y;
            x2 = coords[i + 1].x;
            y2 = coords[i + 1].y;
            processSegment(x, y, x2, y2);
        }
        // Giuseppe Aruta (2018-4-17) Missing last point of the profile
        addPoint(coords[coords.length - 1].x, coords[coords.length - 1].y);
    }

    public static void addPoint(double x, double y)
            throws RasterImageLayer.RasterDataNotFoundException, IOException {
        if (resultFSchema == null) {
            resultFSchema = new FeatureSchema();
            resultFSchema.addAttribute("geometry", AttributeType.GEOMETRY);
            resultFSchema.addAttribute("X", AttributeType.DOUBLE);
            resultFSchema.addAttribute("Y", AttributeType.DOUBLE);
            resultFSchema.addAttribute("Z", AttributeType.DOUBLE);
            resultFSchema.addAttribute("PlaneDist", AttributeType.DOUBLE);
            resultFSchema.addAttribute("TerrainDist", AttributeType.DOUBLE);
            // resultFSchema.addAttribute("Section", AttributeType.STRING);
            resultFC = new FeatureDataset(resultFSchema);
        }
        double z;
        double dDX, dDY, dDZ;
        z = getLayer().getCellValue(x, y, 0);
        if (nPoints == 0) {
            dDist = 0.0;
            dHorzDist = 0.0;
        } else {
            dDX = x - m_dLastX;
            dDY = y - m_dLastY;
            if (z == getLayer().getNoDataValue()
                    || m_dLastZ == getLayer().getNoDataValue()) {
                dDZ = 0.0;
            } else {
                dDZ = z - m_dLastZ;
            }
            dDist += Math.sqrt(dDX * dDX + dDY * dDY);
            dHorzDist += Math.sqrt(dDX * dDX + dDY * dDY + dDZ * dDZ);
        }
        m_dLastX = x;
        m_dLastY = y;
        m_dLastZ = z;
        nPoints++;
        final Point geometry = new GeometryFactory()
                .createPoint(new Coordinate(x, y));

        final Feature fpoint = new BasicFeature(resultFSchema);
        fpoint.setGeometry(geometry);
        fpoint.setAttribute("X", new Double(x));
        fpoint.setAttribute("Y", new Double(y));
        fpoint.setAttribute("Z", new Double(z));
        fpoint.setAttribute("PlaneDist", new Double(dDist));
        fpoint.setAttribute("TerrainDist", new Double(dHorzDist));
        final Coordinate coord = new Coordinate(new Double(z), new Double(
                dHorzDist));

        coordinates.add(coord);
        resultFC.add(fpoint);
    }

    // Giuseppe Aruta (2018-4-17) Terrain distance from FeatureCollection rather
    // then from Geometry
    public static HTMLPanel getStatisticPanel(RasterImageLayer rLayer) {
        final HTMLPanel outpanel = new HTMLPanel();
        outpanel.getRecordPanel().removeAll();
        final Feature first = resultFC.getFeatures().get(0);
        final Feature last = resultFC.getFeatures().get(resultFC.size() - 1);
        final double xmin = (double) first.getAttribute("X");
        final double ymin = (double) first.getAttribute("Y");
        final double xmax = (double) last.getAttribute("X");
        final double ymax = (double) last.getAttribute("Y");

        max = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                resultFC.getFeatures(), "Z");
        min = AttributeOp.evaluateAttributes(AttributeOp.MIN,
                resultFC.getFeatures(), "Z");
        width = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                resultFC.getFeatures(), "PlaneDist")
                - AttributeOp.evaluateAttributes(AttributeOp.MIN,
                        resultFC.getFeatures(), "PlaneDist");
        height = max - min;
        mean = AttributeOp.evaluateAttributes(AttributeOp.MEAN,
                resultFC.getFeatures(), "Z");
        sum = AttributeOp.evaluateAttributes(AttributeOp.SUM,
                resultFC.getFeatures(), "Z");
        /*
         * Overwrite Locale to UK Decimal format ####.##
         */
        final Locale locale = new Locale("en", "UK");
        final String pattern = "###.###";
        final DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);
        slope = Math.toDegrees(Math.atan((max - min) / width));
        cellsize = (rLayer.getWholeImageEnvelope().getMaxX() - rLayer
                .getWholeImageEnvelope().getMinX())
                / rLayer.getOrigImageWidth();
        profLenght = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                resultFC.getFeatures(), "TerrainDist");

        String htmlString = "<HTML><BODY>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + LAYER_NAME
                + ": </b>" + rLayer.getName() + "<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                + PROFILE_LENGTH + ": </b>" + df.format(profLenght) + "<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + WIDTH
                + ": </b>" + df.format(width) + "<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + HEIGHT
                + ": </b>" + df.format(height) + "<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MEAN_SLOPE
                + ": </b>" + df.format(slope) + "°<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                + STARTING_POINT + ": </b>" + df.format(xmin) + " - "
                + df.format(ymin) + "<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + ENDING_POINT
                + ": </b>" + df.format(xmax) + " - " + df.format(ymax) + "<br>";

        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MIN
                + ": </b>" + new Double(min) + "<br>";
        htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MAX
                + ": </b>" + new Double(max) + "</font><br>";
        htmlString += "</DIV></BODY></HTML>";
        outpanel.createNewDocument();
        outpanel.append(htmlString);
        outpanel.getSaveButton().setVisible(false);
        return outpanel;
    }

    public static Plot2DPanelOJ getPlotPanel(FeatureCollection fc) {
        final Plot2DPanelOJ plot2dA = new Plot2DPanelOJ();
        plot2dA.removeAllPlots();

        // Build a 2D data set
        final double[][] datas1 = new double[fc.size()][2];
        for (int j = 0; j < fc.size(); j++) {
            final Feature f = fc.getFeatures().get(j);
            datas1[j][0] = (Double) f.getAttribute("PlaneDist");
            datas1[j][1] = (Double) f.getAttribute("Z");
        }

        // To avoid that two or more consequiensal plots are drawn together

        // Build the 2D scatterplot of the datas in a Panel
        // LINE, SCATTER, BAR, QUANTILE, STAIRCASE, (HISTOGRAMM?)
        // Plot2DPanelOJ plot2dA = new Plot2DPanelOJ();
        plot2dA.addLinePlot("graph", datas1);

        // plot2dA.addScatterPlot("pts",datas1);
        // ====================
        plot2dA.setAxisLabel(0, WIDTH);
        plot2dA.setAxisLabel(1, HEIGHT);
        // change axis title position relatively to the base of the plot
        plot2dA.getAxis(0).setLabelPosition(0.5, -0.15);
        // change axis title position relatively to the base of the plot
        plot2dA.getAxis(1).setLabelPosition(-0.15, 0.5);
        // change axis title angle relatively to the base of the plot
        plot2dA.getAxis(1).setLabelAngle(-Math.PI / 2);
        plot2dA.setFixedBounds(0, 0, width);
        return plot2dA;

    }

}
