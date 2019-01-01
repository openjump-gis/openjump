package org.openjump.core.ui.plugin.raster;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.math.plot.plotObjects.Line;
import org.math.plot.plots.Plot;
import org.math.plot.render.AbstractDrawer;
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
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.JTablePanel;
import com.vividsolutions.jump.workbench.ui.TableFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ProfileGraphGUI {

    private final static String NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphPlugIn.Profile-Graph");
    public final String LAYER_NAME = I18N
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
    public final static String TIMES = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Travel-time");
    public final static String SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Slope");
    public final static String DEGREES = I18N
            .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.degrees");
    public final static String TABLE_VIEW = I18N
            .get("org.openjump.core.ui.plugin.queries.SimpleQuery.display-the-table");

    public final static String RELATIVE_SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Relative-slope");
    public final static String ABSOLUTE_SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Absolute-slope");
    public final static String TYPE_SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Choose-slope-profile");
    public final static String HOURS = I18N.get("ui.GenericNames.hours");
    public final static String MINUTES = I18N.get("ui.GenericNames.minutes");
    public final static String FLAT = I18N.get("ui.GenericNames.flat");
    public final static String UPHILL = I18N.get("ui.GenericNames.uphill");
    public final static String DOWNHILL = I18N.get("ui.GenericNames.downhill");
    public final static String CALCULATE_TIMES = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.calculate-travel-time");

    public final static String UNIT = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.srs-unit");

    public final static String TIMES_TOOLTIP = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.travel-time-tooltip");

    public static WorkbenchFrame wFrame = JUMPWorkbench.getInstance()
            .getFrame();

    public static RasterImageLayer getLayer() {
        return ProfileGraphPlugIn.dialog
                .getRasterLayer(ProfileGraphPlugIn.CLAYER);

    }

    public static int getBand() {
        return ProfileGraphPlugIn.numband;

    }

    public static GeometryFactory gf = new GeometryFactory();
    public static FeatureCollection resultFC = null;
    public static FeatureSchema resultFSchema = null;
    public static String htmlString;
    public static double dDist = 0, dHorzDist = 0, dSlope = 0, dY = 0;
    public static double m_dLastX, m_dLastY, m_dLastZ;
    public static int nPoints = 0;
    public static int n = 0;
    public static ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
    public static String layerUnit = "";
    public static String unit = "";
    public static String speedUnit = "";
    public static double unitConvert;
    protected static Font darkLabelFont = AbstractDrawer.DEFAULT_FONT;
    public static String HEIGHT;
    public static String WIDTH;

    public static void calculateProfile(Coordinate[] coords)
            throws IOException, RasterImageLayer.RasterDataNotFoundException {

        final RasterImageLayer rLayer = getLayer();
        // Workaround to consider also maps unit as foot
        // In this case: kilometre per hour -> mile per hour
        // and 1000 m ->5280 ft
        layerUnit = ProfileGraphPlugIn.UNIT;

        if (layerUnit.equals("foot") || layerUnit.equals("US survey foot")) {
            speedUnit = " [mi/h]";
            unitConvert = 5280;
            unit = " [ft]";
        } else if (layerUnit.equals("metre")) {
            speedUnit = " [km/h]";
            unitConvert = 1000.00;
            unit = " [m]";

        } else if (layerUnit.equals("Unknown")) {
            speedUnit = " ?[km/h]";
            unitConvert = 1000.00;
            unit = " ?[m]";
        } else if (layerUnit.equals("degree")) {
            speedUnit = " [arc-second/h]";
            unitConvert = 0.00027777777;// =1°/3600
            unit = " [deg]";
        }

        else {
            speedUnit = " [" + "layerUnit" + "]/h]";
            unitConvert = 0;
            unit = " [" + "layerUnit" + "]";
        }
        HEIGHT = ProfileGraphPlugIn.HEIGHT;
        WIDTH = ProfileGraphPlugIn.WIDTH;
        final LineString line = gf.createLineString(coords);
        if (line.within(rLayer.getWholeImageEnvelopeAsGeometry())) {
            final Random rand = new Random();

            n = rand.nextInt(100) + 1;
            processLine(line);
            if ((resultFC != null) && (resultFC.size() > 0)) {
                final FeatureDataset fd = new FeatureDataset(resultFSchema);
                fd.addAll(resultFC.getFeatures());
                wFrame.getContext()
                        .getLayerManager()
                        .addLayer(StandardCategoryNames.RESULT,
                                PLOT + "-" + n + " " + PROFILEPTS, fd);

            }
            AdditionalResults.addAdditionalResultAndShow(PLOT + "-" + n,
                    getPlotPanel(resultFC, "" + n));

        } else {
            wFrame.getContext()
                    .getLayerViewPanel()
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

        z = getLayer().getCellValue(x, y, getBand());

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

    public static double[][] datas2;

    public static Plot2DPanelOJ getPlotPanel(FeatureCollection fc, String name) {
        final JPanel jPanel2 = new JPanel();
        final JPanel jPanel3 = new JPanel();
        final JLabel jLabel0 = new JLabel(CALCULATE_TIMES + speedUnit + ": ");

        jLabel0.setToolTipText("<HTML><BODY><DIV style=\"width: 400px; text-justification: justify;\">"
                + TIMES_TOOLTIP + "</DIV></BODY></HTML>");
        final JLabel jLabel1 = new JLabel();
        final JLabel jLabel2 = new JLabel();
        final JLabel jLabel3 = new JLabel();
        final JLabel jLabel4 = new JLabel();
        final JTextField jTextField_VelFlat = new JTextField();
        final JTextField jTextField_VelUp = new JTextField();
        final JTextField jTextField_VelDown = new JTextField();
        jTextField_VelDown.setPreferredSize(new Dimension(50, 20));
        jTextField_VelUp.setPreferredSize(new Dimension(50, 20));
        jTextField_VelFlat.setPreferredSize(new Dimension(50, 20));

        jTextField_VelDown.setMaximumSize(new Dimension(50, 20));
        jTextField_VelUp.setMaximumSize(new Dimension(50, 20));
        jTextField_VelFlat.setMaximumSize(new Dimension(50, 20));
        jTextField_VelFlat.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE) || (vChar == KeyEvent.VK_DELETE))) {
                    e.consume();
                }
            }
        });
        jTextField_VelUp.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE) || (vChar == KeyEvent.VK_DELETE))) {
                    e.consume();
                }
            }
        });
        jTextField_VelDown.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE) || (vChar == KeyEvent.VK_DELETE))) {
                    e.consume();
                }
            }
        });
        jLabel1.setText(FLAT);
        jLabel2.setText(UPHILL);
        jLabel3.setText(DOWNHILL);
        jLabel4.setText(UNIT + unit);
        jTextField_VelFlat.setText("6");
        jTextField_VelUp.setText("3");
        jTextField_VelDown.setText("8");

        final FlowLayout layout = new FlowLayout();
        jPanel2.setLayout(layout);
        layout.setAlignment(FlowLayout.LEADING);
        jPanel2.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        jPanel2.add(jLabel0);
        jPanel2.add(jLabel1);
        jPanel2.add(jTextField_VelFlat);
        jPanel2.add(jLabel2);
        jPanel2.add(jTextField_VelUp);

        jPanel2.add(jLabel3);
        jPanel2.add(jTextField_VelDown);
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
        plot2dA.setAxisLabel(0, WIDTH + unit);
        if (!layerUnit.equals("degree")) {
            plot2dA.setAxisLabel(1, HEIGHT + unit);
        } else {
            plot2dA.setAxisLabel(1, HEIGHT);
        }
        plot2dA.getAxis(0).setLabelPosition(0.5, -0.15);
        plot2dA.getAxis(1).setLabelPosition(-0.15, 0.5);
        plot2dA.getAxis(1).setLabelAngle(-Math.PI / 2);

        // Table part
        double[][] dataTableDouble = null;
        for (final Plot plot2 : plot2dA.getPlots()) {
            dataTableDouble = plot2.getData();
        }
        final ProfileGraphAlgorithms profile = new ProfileGraphAlgorithms();

        profile.calculateValues(dataTableDouble);
        // profile.calculateValues(dataTableDouble, velFlat, velUp, velDown);
        final double[] doubleZ = profile.getZData();
        final double[] doublePlanet = profile.getPlanetData();
        final double[] doubleTerrain = profile.getTerrainData();
        final double[] doubleRelativeSlope = profile.getRelativeSlopeData();
        final double[] doubleAbsoluteSlope = profile.getAbsoluteSlopeData();

        // Buttons///////

        // To generate a table with all partial values
        final JButton tableBtn = new JButton(IconLoader.icon("Row_16.gif"));
        tableBtn.setToolTipText(TABLE_VIEW);
        tableBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final TableFrame freqFrame = new TableFrame();
                freqFrame
                        .setDefaultCloseOperation(AdditionalResultsFrame.DISPOSE_ON_CLOSE);
                final JInternalFrame[] frames = wFrame.getInternalFrames();
                final ArrayList<JInternalFrame> list = new ArrayList<JInternalFrame>();
                for (final JInternalFrame iFrame : frames) {
                    if (iFrame instanceof TableFrame) {
                        list.add(iFrame);
                    }
                }
                if (list.size() == 1) {
                    freqFrame.toFront();
                }

                else {
                    final Object[] slopeData = doubleToObject(doubleRelativeSlope);
                    final Object[] absoluteSlopeData = doubleToObject(doubleAbsoluteSlope);
                    final Object[] zData = doubleToObject(doubleZ);
                    final Object[] planetDistData = doubleToObject(doublePlanet);
                    final Object[] terrainDistData = doubleToObject(doubleTerrain);
                    freqFrame.setLayout(new BorderLayout());
                    freqFrame.setResizable(true);
                    freqFrame.setClosable(true);
                    freqFrame.setIconifiable(true);
                    freqFrame.setMaximizable(true);
                    freqFrame.setPreferredSize(new Dimension(900, 450));
                    freqFrame.setSize(900, 450);
                    freqFrame.setLayer(JLayeredPane.MODAL_LAYER);
                    freqFrame.setTitle(PLOT + "-" + n);
                    final JTable jTable = new JTable();
                    final DefaultTableModel dtm = (DefaultTableModel) jTable
                            .getModel();
                    dtm.addColumn("Z", zData);
                    dtm.addColumn("PlaneDist", planetDistData);
                    dtm.addColumn("TerrainDist", terrainDistData);
                    dtm.addColumn(RELATIVE_SLOPE + " (°)", slopeData);
                    dtm.addColumn(ABSOLUTE_SLOPE + " (°)", absoluteSlopeData);
                    final JTablePanel jTablePanel = new JTablePanel(dtm);
                    jTablePanel.getCommandPanel().setVisible(false);
                    freqFrame.add(jTablePanel);
                    wFrame.getContext().getWorkbench().getFrame()
                            .addInternalFrame(freqFrame, true, true);
                }

            }
        });

        // To generate a generate a profile of the slope
        final JButton slopeBtn = new JButton(
                IconLoader.icon("profileSlope.png"));
        slopeBtn.setToolTipText(SLOPE);
        slopeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JInternalFrame[] frames = wFrame.getInternalFrames();
                final ArrayList<JInternalFrame> list = new ArrayList<JInternalFrame>();
                for (final JInternalFrame iFrame : frames) {
                    if (iFrame instanceof AdditionalResultsFrame) {
                        list.add(iFrame);
                    }
                }
                if (list.size() > 1) {
                    mosaicFrames();
                } else {
                    String type = "";
                    final double[][] slopedt = new double[doubleZ.length][2];
                    final String[] options = { RELATIVE_SLOPE, ABSOLUTE_SLOPE };
                    final int x = JOptionPane.showOptionDialog(null,
                            TYPE_SLOPE, NAME, JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null, options,
                            options[0]);
                    if (x == JOptionPane.CLOSED_OPTION) {
                        return;
                    }
                    final Plot2DPanelOJ plotSlope = new Plot2DPanelOJ();
                    switch (x) {
                    case 0:

                        for (int j = 0; j < doubleZ.length; j++) {
                            slopedt[j][0] = doublePlanet[j];
                            slopedt[j][1] = doubleRelativeSlope[j];
                        }

                        type = RELATIVE_SLOPE;

                        break;

                    case 1:
                        for (int j = 0; j < doubleZ.length; j++) {
                            slopedt[j][0] = doublePlanet[j];
                            slopedt[j][1] = doubleAbsoluteSlope[j];
                        }
                        type = ABSOLUTE_SLOPE;
                        break;
                    }

                    plotSlope.plotToolBar.remove(5);
                    plotSlope.plotToolBar.remove(4);
                    plotSlope.setName(SLOPE);
                    plotSlope.addLinePlot(SLOPE, Color.red, slopedt);
                    plotSlope.setAxisLabel(0, WIDTH + unit);
                    plotSlope.setAxisLabel(1, type + " [" + DEGREES + "]");
                    plotSlope.getAxis(0).setLabelPosition(0.5, -0.15);
                    plotSlope.getAxis(1).setLabelPosition(-0.15, 0.5);
                    plotSlope.getAxis(1).setLabelAngle(-Math.PI / 2);
                    plotSlope.setFixedBounds(0, 0, profile.getPlanetLength());
                    plotSlope.getAxis(0)
                            .setOrigin(
                                    new double[] {
                                            plotSlope.plotCanvas.base
                                                    .getMinBounds()[0], 0 });
                    plotSlope.addPlotable(new Line(
                            Color.gray,
                            new double[] {
                                    plotSlope.plotCanvas.base.getMinBounds()[0],
                                    0 },
                            new double[] {
                                    plotSlope.plotCanvas.base.getMaxBounds()[0],
                                    0 }));
                    final ArrayList<ObjectAndDescription> m_Components = new ArrayList<ObjectAndDescription>();
                    final ObjectAndDescription ob = new ObjectAndDescription(
                            SLOPE, plotSlope);
                    m_Components.add(ob);

                    final AdditionalResultsFrame slopeFrame = new AdditionalResultsFrame(
                            m_Components);
                    slopeFrame.setTitle(PLOT + "-" + n + " " + SLOPE);
                    slopeFrame.setName(SLOPE);
                    slopeFrame.pack();

                    if (list.size() == 1) {
                        slopeFrame.setSize(plot2dA.getWidth(),
                                plot2dA.getHeight());
                        wFrame.getContext().getWorkbench().getFrame()
                                .addInternalFrame(slopeFrame, true, true);

                    } else {
                        slopeFrame.toFront();

                    }
                    mosaicFrames();
                }
            }
        });

        // The info panel
        final JButton infoBtn = new JButton(
                IconLoader.icon("information_16x16.png"));
        infoBtn.setToolTipText(PROFILE_INFO);
        infoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HTMLFrame frame = new HTMLFrame();

                final JInternalFrame[] frames = JUMPWorkbench.getInstance()
                        .getFrame().getInternalFrames();
                final ArrayList<JInternalFrame> list = new ArrayList<JInternalFrame>();
                for (final JInternalFrame iFrame : frames) {
                    if (iFrame instanceof HTMLFrame) {
                        list.add(iFrame);
                    }
                }

                if (list.size() == 1) {
                    frame.dispose();
                }

                String velFlatString = jTextField_VelFlat.getText();
                if (velFlatString.isEmpty()) {
                    velFlatString = "0";
                }
                final double velFlat = Double.valueOf(velFlatString);

                String velUpString = jTextField_VelUp.getText();
                if (velUpString.isEmpty()) {
                    velUpString = "0";
                }
                final double velUp = Double.valueOf(velUpString);

                String velDownString = jTextField_VelDown.getText();
                if (velDownString.isEmpty()) {
                    velDownString = "0";
                }

                final double velDown = Double.valueOf(velDownString);
                double totTime = 0;
                for (int i = 0; i < doubleRelativeSlope.length; i++) {
                    if (doubleRelativeSlope[i] < 1.7184
                            && doubleRelativeSlope[i] > -1.7184
                            && doubleTerrain[i] != 0) {
                        // Flat
                        totTime += ((doubleTerrain[i] - doubleTerrain[i - 1]) / unitConvert)
                                * (1 / velFlat);
                    } else if (doubleRelativeSlope[i] > 1.7184
                            && doubleTerrain[i] != 0) {
                        // Up
                        totTime += ((doubleTerrain[i] - doubleTerrain[i - 1]) / unitConvert)
                                * (1 / velUp);
                    } else if (doubleRelativeSlope[i] < 1.7184
                            && doubleTerrain[i] != 0) {
                        // Down
                        totTime += ((doubleTerrain[i] - doubleTerrain[i - 1]) / unitConvert)
                                * (1 / velDown);

                    }
                }
                final HTMLPanel out = new HTMLPanel();

                final DecimalFormat df = new DecimalFormat("##.###");

                final String totTimeHM = (int) totTime + " " + HOURS + " "
                        + (int) ((totTime - (int) totTime) * 60) + " "
                        + MINUTES;

                final double min1 = profile.getZMin();
                final double max1 = profile.getZMax();
                final double lenght = profile.getTerrainLength();
                final double width1 = profile.getPlanetLength();
                final double height1 = Math.abs(max1 - min1);
                final double slope1 = profile.getSlope();

                htmlString = "<HTML><BODY>";
                htmlString += "<h2 align=\"left\">" + PROFILE_INFO + "-" + n
                        + "</h2>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MIN
                        + ": </b>" + df.format(min1) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">" + MAX
                        + ": </b>" + df.format(max1) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + PROFILE_LENGTH + ": </b>" + df.format(lenght)
                        + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + WIDTH + ": </b>" + df.format(width1) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + HEIGHT + ": </b>" + df.format(height1) + "<br>";
                htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                        + MEAN_SLOPE + ": </b>" + df.format(slope1) + "°<br>";
                if (!layerUnit.equals("degree") || !layerUnit.equals("UnKnown")) {
                    htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                            + UNIT + " </b>" + layerUnit + "<br>";

                    htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                            + FLAT + ": </b>" + velFlat + " " + speedUnit
                            + "<br>";
                    htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                            + UPHILL + ": </b>" + velUp + " " + speedUnit
                            + "<br>";
                    htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                            + DOWNHILL + ": </b>" + velDown + " " + speedUnit
                            + "<br>";
                    htmlString += "<b><font face=\"" + darkLabelFont + "\">"
                            + TIMES + ": </b>" + totTimeHM + "<br>";
                }
                htmlString += "</DIV></BODY></HTML>";
                out.getRecordPanel().removeAll();
                out.createNewDocument();
                out.setBackground(Color.lightGray);
                out.append(htmlString);
                frame.setTitle(PLOT + "-" + n + " " + PROFILE_INFO);
                frame.add(out);
                frame.setClosable(true);
                frame.setResizable(true);
                frame.setMaximizable(true);
                frame.setSize(230, 360);
                frame.setVisible(true);
                wFrame.getContext().getWorkbench().getFrame()
                        .addInternalFrame(frame, true, true);

            }
        });

        plot2dA.plotToolBar.addSeparator(new Dimension(16, 16));

        plot2dA.plotToolBar.add(tableBtn);
        plot2dA.plotToolBar.add(slopeBtn);
        plot2dA.plotToolBar.addSeparator(new Dimension(16, 16));
        plot2dA.plotToolBar.add(jLabel4);
        plot2dA.plotToolBar.addSeparator(new Dimension(16, 16));
        plot2dA.plotToolBar.add(infoBtn);
        if (!layerUnit.equals("degree") || !layerUnit.equals("UnKnown")) {
            plot2dA.plotToolBar.add(jPanel2);
            plot2dA.plotToolBar.addSeparator(new Dimension(16, 16));
            plot2dA.plotToolBar.add(jPanel3);
        }

        plot2dA.setFixedBounds(0, 0, profile.getPlanetLength());
        return plot2dA;

    }

    private static void mosaicFrames() {
        final int xpos = 0;
        int ypos = 0;
        final Dimension deskDim = wFrame.getContext().getWorkbench().getFrame()
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
                ((AdditionalResultsFrame) iFrame).setLocation(xpos, ypos);
                ((AdditionalResultsFrame) iFrame).getSplitPanel()
                        .setDividerLocation(0);
                ((AdditionalResultsFrame) iFrame).moveToFront();
                ypos += frameHeight;
            }
        }
    }

    public static Object[] doubleToObject(double[] data) {
        final Object[] column = new Object[data.length];
        for (int i = 0; i < column.length; i++) {
            column[i] = data[i];
        }
        return column;
    }

}
