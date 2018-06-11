package org.openjump.core.ui.plugin.raster;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.math.plot.plots.Plot;
import org.math.plot.render.AbstractDrawer;
import org.openjump.core.attributeoperations.AttributeOp;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.plot.Plot2DPanelOJ;
import org.openjump.sextante.core.ObjectAndDescription;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;
import org.openjump.sextante.gui.additionalResults.AdditionalResultsFrame;

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
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.JTablePanel;
import com.vividsolutions.jump.workbench.ui.TableFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

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

    public final static String TIMES = I18N
            .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.Travel-time");
    public final static String SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Slope");

    public static WorkbenchContext context = JUMPWorkbench.getInstance()
            .getFrame().getContext();

    public static RasterImageLayer getLayer() {
        return ProfileGraphPlugIn.dialog
                .getRasterLayer(ProfileGraphPlugIn.CLAYER);

    }

    public static GeometryFactory gf = new GeometryFactory();
    public static FeatureCollection resultFC = null;
    public static FeatureSchema resultFSchema = null;
    public static double dDist = 0, dHorzDist = 0, dSlope = 0, dY = 0;
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
                max = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                        fd.getFeatures(), "Z");
                min = AttributeOp.evaluateAttributes(AttributeOp.MIN,
                        fd.getFeatures(), "Z");
                width = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                        fd.getFeatures(), "PlaneDist")
                        - AttributeOp.evaluateAttributes(AttributeOp.MIN,
                                fd.getFeatures(), "PlaneDist");
                height = max - min;
                mean = AttributeOp.evaluateAttributes(AttributeOp.MEAN,
                        fd.getFeatures(), "Z");
                sum = AttributeOp.evaluateAttributes(AttributeOp.SUM,
                        fd.getFeatures(), "Z");
                slope = Math.toDegrees(Math.atan((max - min) / width));

                profLenght = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                        fd.getFeatures(), "TerrainDist");

                // featColl().addAll(resultFC.getFeatures());
                context.getLayerManager().addLayer(
                        StandardCategoryNames.RESULT,
                        PLOT + "-" + n + " " + PROFILEPTS, fd);
            }
            AdditionalResults.addAdditionalResultAndShow(PLOT + "-" + n,
                    getPlotPanel(resultFC, "" + n));

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

    public static Plot2DPanelOJ getPlotPanel(FeatureCollection fc, String name) {
        final Plot2DPanelOJ plot2dA = new Plot2DPanelOJ();
        plot2dA.setName(name);
        plot2dA.removeAllPlots();
        plot2dA.plotToolBar.remove(5);
        plot2dA.plotToolBar.remove(4);

        // Build a 2D data set
        final double[][] datas1 = new double[fc.size()][2];
        for (int j = 0; j < fc.size(); j++) {
            final Feature f = fc.getFeatures().get(j);
            datas1[j][0] = (Double) f.getAttribute("PlaneDist");
            datas1[j][1] = (Double) f.getAttribute("Z");
        }
        plot2dA.addLinePlot("Profile", datas1);

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

        // Table part
        double[][] dataTableDouble = null;
        Object[][] dataTableObject = null;
        for (final Plot plot2 : plot2dA.getPlots()) {
            dataTableDouble = plot2.getData();
        }
        dataTableObject = plot2dA.plotCanvas.reverseMapedData(dataTableDouble);
        final double[][] slopeMatrixData = getRelativeSlope(dataTableObject);
        final Object[] slopeData = getXdoubleData(slopeMatrixData);
        final Object[] ZData = getYData(dataTableObject);
        final Object[] LData = getXData(dataTableObject);
        // final Object[] data_3d = get3Ddata(dataTableObject);

        // Buttons///////
        final JButton tableBtn = new JButton(IconLoader.icon("Row_16.gif"));
        tableBtn.setToolTipText(PROFILE_INFO);
        tableBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                final TableFrame freqFrame = new TableFrame();
                freqFrame.setLayout(new BorderLayout());
                freqFrame.setResizable(true);
                freqFrame.setClosable(true);
                freqFrame.setIconifiable(true);
                freqFrame.setMaximizable(true);
                freqFrame.setPreferredSize(new Dimension(900, 450));
                freqFrame.setSize(900, 450);
                freqFrame.setLayer(JLayeredPane.MODAL_LAYER);
                freqFrame.setTitle(PROFILE_INFO);

                final JTable jTable = new JTable();

                final DefaultTableModel dtm = (DefaultTableModel) jTable
                        .getModel();
                dtm.addColumn("Z", ZData);
                dtm.addColumn("PlaneDist", LData);
                dtm.addColumn(SLOPE + " (°)", slopeData);
                final JTablePanel jTablePanel = new JTablePanel(dtm);
                jTablePanel.getCommandPanel().setVisible(false);
                freqFrame.add(jTablePanel);
                freqFrame.setVisible(true);
                context.getWorkbench().getFrame()
                        .addInternalFrame(freqFrame, true, true);
            }
        });

        final JButton slopeBtn = new JButton(
                IconLoader.icon("profileSlope.png"));
        slopeBtn.setToolTipText(ProfileGraphPlugIn.SLOPE);
        slopeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                final Plot2DPanelOJ plotSlope = new Plot2DPanelOJ();
                plotSlope.plotToolBar.remove(5);
                plotSlope.plotToolBar.remove(4);
                plotSlope.setName(SLOPE);
                plotSlope.addLinePlot(SLOPE, Color.red, slopeMatrixData);
                plotSlope.setAxisLabel(0, WIDTH);
                plotSlope.setAxisLabel(1, SLOPE + " (°)");
                plotSlope.getAxis(0).setLabelPosition(0.5, -0.15);
                plotSlope.getAxis(1).setLabelPosition(-0.15, 0.5);
                plotSlope.getAxis(1).setLabelAngle(-Math.PI / 2);
                plotSlope.setFixedBounds(0, 0, width);
                final ArrayList<ObjectAndDescription> m_Components = new ArrayList<ObjectAndDescription>();
                final ObjectAndDescription ob = new ObjectAndDescription(
                        "Slope",
                        // If the string below is activated, slope profile can
                        // be exported also to dxf
                        // The main problem is to fix the Y axe scale on
                        // AdditionalResultsFrame.exportToDxf(..) method
                        // I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot"),
                        plotSlope);
                m_Components.add(ob);

                final AdditionalResultsFrame slopeFrame = new AdditionalResultsFrame(
                        m_Components);
                slopeFrame.setTitle("Slope");
                slopeFrame.setName(I18N.get("Slope"));
                slopeFrame.pack();
                final JInternalFrame[] frames = JUMPWorkbench.getInstance()
                        .getFrame().getInternalFrames();
                final ArrayList<JInternalFrame> list = new ArrayList<JInternalFrame>();
                for (final JInternalFrame iFrame : frames) {
                    if (iFrame instanceof AdditionalResultsFrame) {
                        list.add(iFrame);
                    }
                }

                if (list.size() == 1) {
                    slopeFrame.setSize(plot2dA.getWidth(), plot2dA.getHeight());
                    context.getWorkbench().getFrame()
                            .addInternalFrame(slopeFrame, true, true);

                } else {
                    // slopeFrame.repaint();
                    slopeFrame.toFront();

                }
                final int xpos = 0;
                int ypos = 0;
                final Dimension deskDim = context.getWorkbench().getFrame()
                        .getDesktopPane().getSize();
                int deskWidth = deskDim.width;
                final int deskHeight = deskDim.height;
                deskWidth = deskDim.width;
                final int frameWidth = deskWidth;
                final int frameHeight = deskHeight / 2;

                for (final JInternalFrame iFrame : JUMPWorkbench.getInstance()
                        .getFrame().getInternalFrames()) {
                    if (iFrame instanceof AdditionalResultsFrame) {
                        ((AdditionalResultsFrame) iFrame).reshape(xpos, ypos,
                                frameWidth, frameHeight);
                        ((AdditionalResultsFrame) iFrame).setLocation(xpos,
                                ypos);
                        ((AdditionalResultsFrame) iFrame).getSplitPanel()
                                .setDividerLocation(0);
                        ((AdditionalResultsFrame) iFrame).moveToFront();
                        ypos += frameHeight;
                    }
                }
            }
        });

        final JButton infoBtn = new JButton(
                IconLoader.icon("information_16x16.png"));
        infoBtn.setToolTipText(PROFILE_INFO);
        infoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DecimalFormat df = new DecimalFormat("##.###");
                final HTMLPanel out = new HTMLPanel();
                out.getRecordPanel().removeAll();
                out.createNewDocument();
                out.setBackground(Color.lightGray);
                String htmlString = "<HTML><BODY>";
                htmlString += "<h2 align=\"left\">" + PROFILE_INFO + "</h2>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MIN
                        + ": </b>" + df.format(min) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MAX
                        + ": </b>" + df.format(max) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + PROFILE_LENGTH + ": </b>" + df.format(profLenght)
                        + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + WIDTH + ": </b>" + df.format(width) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + HEIGHT + ": </b>" + df.format(height) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + MEAN_SLOPE + ": </b>" + df.format(slope) + "°<br>";
                htmlString += "</DIV></BODY></HTML>";
                out.append(htmlString);
                final HTMLFrame frame = new HTMLFrame();
                frame.setTitle(PROFILE_INFO);
                frame.add(out);
                frame.setClosable(true);
                frame.setResizable(true);
                frame.setMaximizable(true);
                frame.setSize(200, 280);
                frame.setVisible(true);
                context.getWorkbench().getFrame()
                        .addInternalFrame(frame, true, true);
            }
        });
        // To do: giving a downslope/upslope/flat speed, it calculates the
        // travel time along the profile
        final JButton timeBtn = new JButton(IconLoader.icon("fugue/clock.png"));
        timeBtn.setToolTipText(TIMES);
        timeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // final ProfileTimesDialog ptd = new ProfileTimesDialog(
                // slopeMatrixData);
                // ptd.setLocationRelativeTo(JUMPWorkbench.getInstance()
                // .getFrame());
                // ptd.setVisible(true);
            }
        });

        plot2dA.plotToolBar.addSeparator(new Dimension(16, 16));
        plot2dA.plotToolBar.add(infoBtn);
        plot2dA.plotToolBar.add(tableBtn);
        plot2dA.plotToolBar.add(slopeBtn);
        // plot2dA.plotToolBar.add(timeBtn);

        plot2dA.setFixedBounds(0, 0, width);
        return plot2dA;

    }

    private static Object[] getXdoubleData(double[][] slopeData) {
        final Object[] column = new Object[slopeData.length];
        double value;
        for (int i = 0; i < column.length; i++) {
            value = ((Double) slopeData[i][1]).doubleValue();
            column[i] = value;
        }
        return column;

    }

    public static double[][] getRelativeSlope(Object[][] dataTableObject) {
        final double[][] column = new double[dataTableObject.length][2];
        for (int i = 0; i < column.length; i++) {
            if (i == 0) {
                column[i][1] = 0;
                column[i][0] = ((Double) dataTableObject[i][0]).doubleValue();
            } else {
                final double dDist1 = ((Double) dataTableObject[i][1])
                        .doubleValue();
                final double dDist0 = ((Double) dataTableObject[i - 1][1])
                        .doubleValue();
                final double Z1 = ((Double) dataTableObject[i][0])
                        .doubleValue();
                final double Z0 = ((Double) dataTableObject[i - 1][0])
                        .doubleValue();
                double number = 0;
                final double slope = Math.atan(((Z1 - Z0) / (dDist1 - dDist0)));
                if (slope > 0 || slope == 0) {
                    number = 90 - Math.toDegrees(slope);
                } else if (slope < 0) {
                    number = -90 - Math.toDegrees(slope);
                }
                column[i][1] = number;
                column[i][0] = Z1;
            }
        }
        return column;
    }

    public static Object[] getXData(Object[][] dataTableObject) {
        final Object[] column = new Object[dataTableObject.length];
        double value;
        for (int i = 0; i < column.length; i++) {
            value = ((Double) dataTableObject[i][0]).doubleValue();
            column[i] = value;
        }
        return column;
    }

    public static Object[] getD3Data(Object[][] dataTableObject) {
        final Object[] column = new Object[dataTableObject.length];

        for (int i = 0; i < column.length; i++) {
            if (i == 0) {
                column[i] = 0;
            } else {
                final double dDist1 = ((Double) dataTableObject[i][1])
                        .doubleValue();
                final double dDist0 = ((Double) dataTableObject[i - 1][1])
                        .doubleValue();
                final double Z1 = ((Double) dataTableObject[i][0])
                        .doubleValue();
                final double Z0 = ((Double) dataTableObject[i - 1][0])
                        .doubleValue();
                column[i] = Math.sqrt(Z1 * Z0 + dDist1 * dDist0);
            }
        }
        return column;
    }

    public static Object[] getYData(Object[][] dataTableObject) {
        final Object[] column = new Object[dataTableObject.length];
        for (int i = 0; i < column.length; i++) {
            final double value = ((Double) dataTableObject[i][1]).doubleValue();
            column[i] = value;
        }
        return column;
    }

    public static double[][] getDataAsDouble(Object[][] dataTableObject) {
        final double[][] column = new double[dataTableObject.length][2];
        for (int i = 0; i < column.length; i++) {
            final double value1 = ((Double) dataTableObject[i][1])
                    .doubleValue();
            final double value0 = ((Double) dataTableObject[i][0])
                    .doubleValue();
            column[i][0] = value0;
            column[i][1] = value1;
        }
        return column;
    }
}
