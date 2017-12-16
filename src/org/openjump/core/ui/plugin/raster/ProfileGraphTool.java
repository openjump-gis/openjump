/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.raster;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.attributeoperations.AttributeOp;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridExtent;
import org.openjump.core.ui.plot.Plot2DPanelOJ;

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
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ProfileGraphTool extends MultiClickTool {

    /**
     * 2015_01_31. Giuseppe Aruta Add new panel which display profile info:
     * length, mean slope, coordinates of starting and ending points, cell
     * dimension, cell statistics.
     */

    private final static String CANCEL = I18N.get("ui.OKCancelPanel.cancel");
    private final static String sDistance = I18N
            .get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.Distance");
    private final static String sMeters = I18N
            .get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.meters");
    private final static String LAYER_NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.layer_name");
    private final static String MIN = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum");
    private final static String MEAN = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewTableModel.mean-mode");
    private final static String MAX = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum");
    private final static String SUM = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.sum");
    private final static String INFO = I18N.get("ui.AboutDialog.info");
    private final static String CELL_SIZE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_cell");
    private final static String PLOT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot");
    private final static String PROFILEPTS = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.profile-pts");

    private final static String CELL_STAT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.cell-statistics");
    private final static String PROFILE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile");
    private final static String PROFILE_INFO = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-info");
    private final static String PROFILE_LENGTH = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-length");
    private final static String STARTING_POINT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.starting-point");
    private final static String ENDING_POINT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.ending-point");
    private final static String MEAN_SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.mean-slope");

    private List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();

    private Coordinate currCoord;
    private OpenJUMPSextanteRasterLayer rstLayer = null;
    private RasterImageLayer rLayer = null;
    private GeometryFactory gf = new GeometryFactory();
    private FeatureCollection resultFC = null;
    private FeatureSchema resultFSchema = null;
    private double dDist = 0, dHorzDist = 0;
    private double m_dLastX, m_dLastY, m_dLastZ;
    private int nPoints = 0;
    private String sBand = "band";

    // private FeatureDatasetFactory fdf = new FeatureDatasetFactory();
    // private GridWrapperNotInterpolated gwrapper = null;

    public ProfileGraphTool() {
        this.allowSnapping();
        // -- do on init
        this.resultFSchema = new FeatureSchema();
        this.resultFSchema.addAttribute("geometry", AttributeType.GEOMETRY);
        this.resultFSchema.addAttribute("X", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("Y", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("Z", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("PlaneDist", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("TerrainDist", AttributeType.DOUBLE);
        this.resultFC = new FeatureDataset(this.resultFSchema);
    }

    @Override
    public Icon getIcon() {
        return IconLoader.icon("profile.png");
    }

    @Override
    public Cursor getCursor() {
        for (int i = 0; i < savedCoordinates.size(); i++) {
            add(savedCoordinates.get(i));
        }
        return createCursor(IconLoader.icon("profile_icon.gif").getImage());

    }

    @Override
    public void mouseLocationChanged(MouseEvent e) {
        try {
            if (isShapeOnScreen()) {
                ArrayList<Coordinate> currentCoordinates = new ArrayList<Coordinate>(
                        getCoordinates());
                currentCoordinates.add(getPanel().getViewport()
                        .toModelCoordinate(e.getPoint()));
                display(currentCoordinates, getPanel());
            }

            currCoord = snap(e.getPoint());
            super.mouseLocationChanged(e);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        savedCoordinates = new ArrayList<Coordinate>(getCoordinates());
    }

    @Override
    protected void gestureFinished() throws NoninvertibleTransformException,
            IOException, RasterImageLayer.RasterDataNotFoundException {
        reportNothingToUndoYet();
        savedCoordinates.clear();

        // Status bar is cleared before #gestureFinished is called. So redisplay
        // the length. [Jon Aquino]
        display(getCoordinates(), getPanel());

        // -- [sstein] now all the raster profile stuff
        this.rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(this
                .getWorkbench().getContext(), RasterImageLayer.class);
        if (rLayer == null) {
            getPanel()
                    .getContext()
                    .warnUser(
                            I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return;
        }
        this.rstLayer = new OpenJUMPSextanteRasterLayer();
        // [mmichaud 2013-05-25] false : this is a temporary image not a file
        // based image
        this.rstLayer.create(rLayer, false);
        this.rstLayer.setFullExtent(); // not sure why this needs to be done but
                                       // it seems to
                                       // be necessary (otherwise I get an NPE
                                       // when
                                       // doing
                                       // this.rstLayer.getWindowCellSize())
        GridExtent extent = this.rstLayer.getWindowGridExtent(); // not sure if
                                                                 // this needs
                                                                 // to be done -
                                                                 // but it was
                                                                 // in the
                                                                 // Sextante
                                                                 // class
        // -- clear the resultFC
        this.resultFC.clear();
        this.nPoints = 0;
        // -- create a gridwrapper to access the cells
        // this.gwrapper = new GridWrapperNotInterpolated(rstLayer,
        // rstLayer.getLayerGridExtent());
        this.calculateProfile(getCoordinates(), getWorkbench().getContext());
        /*
         * //-- this was used for testing double rvalue = 0.0; Coordinate
         * startCoord = (Coordinate)getCoordinates().get(0); GridCell cell =
         * rstLayer
         * .getLayerGridExtent().getGridCoordsFromWorldCoords(startCoord.x,
         * startCoord.y); // rvalue = cell.getValue(); //can't use this, since
         * the value will be zero, so I assume the cell //object is just a place
         * holder for the coordinates rvalue =
         * gwrapper.getCellValueAsDouble(cell.getX(), cell.getY(), 0); //get
         * value for first band //--output
         * getPanel().getContext().setStatusMessage("starting point value: " +
         * rvalue);
         */
    }

    private void display(List<Coordinate> coordinates, LayerViewPanel panel)
            throws NoninvertibleTransformException {
        display(distance(coordinates), panel);
    }

    private void display(double distance, LayerViewPanel panel) {
        DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
        String distString = df3.format(distance / 0.3048);
        panel.getContext().setStatusMessage(
                sDistance + ": " + panel.format(distance) + " " + sMeters + " "
                        + " = " + distString + " feet");
    }

    private double distance(List<Coordinate> coordinates) {
        double distance = 0;

        for (int i = 1; i < coordinates.size(); i++) {
            distance += coordinates.get(i - 1).distance(coordinates.get(i));
        }

        if ((currCoord != null) && (coordinates.size() > 1)) {
            distance -= coordinates.get(coordinates.size() - 2).distance(
                    coordinates.get(coordinates.size() - 1));
            distance += coordinates.get(coordinates.size() - 2).distance(
                    currCoord);
        }

        return distance;
    }

    // HTMLPanel outpanel;

    private void calculateProfile(List<Coordinate> coordinates,
            WorkbenchContext context) throws IOException,
            RasterImageLayer.RasterDataNotFoundException {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(this.getWorkbench().getContext(),
                        RasterImageLayer.class);
        final JInternalFrame frame = new JInternalFrame(PLOT);

        // -- create a linestring
        Coordinate[] coords = new Coordinate[coordinates.size()];
        int i = 0;
        for (Iterator iterator = coordinates.iterator(); iterator.hasNext();) {
            Coordinate c = (Coordinate) iterator.next();
            coords[i] = c;
            i++;
        }
        LineString line = gf.createLineString(coords);

        if (line.within(rLayer.getWholeImageEnvelopeAsGeometry())) {
            this.processLine(line);
            PlugInContext pc = context.createPlugInContext();
            if ((this.resultFC != null) && (this.resultFC.size() > 0)) {
                pc.addLayer(StandardCategoryNames.RESULT, PROFILEPTS,
                        this.resultFC);
            }

            // -- HTML Info panel
            final HTMLPanel outpanel = new HTMLPanel();
            outpanel.getRecordPanel().removeAll();
            Coordinate end = line.getEndPoint().getCoordinate();
            Coordinate start = line.getStartPoint().getCoordinate();
            double max, min, sum, mean;
            max = AttributeOp.evaluateAttributes(AttributeOp.MAX,
                    this.resultFC.getFeatures(), "Z");
            min = AttributeOp.evaluateAttributes(AttributeOp.MIN,
                    this.resultFC.getFeatures(), "Z");
            mean = AttributeOp.evaluateAttributes(AttributeOp.MEAN,
                    this.resultFC.getFeatures(), "Z");
            sum = AttributeOp.evaluateAttributes(AttributeOp.SUM,
                    this.resultFC.getFeatures(), "Z");
            /*
             * Overwrite Locale to UK Decimal format ####.##
             */
            Locale locale = new Locale("en", "UK");
            String pattern = "###.####";
            DecimalFormat df = (DecimalFormat) NumberFormat
                    .getNumberInstance(locale);
            df.applyPattern(pattern);

            String tooltip = "<HTML><BODY>";
            tooltip += "<DIV style=\"width: 500px; text-justification: justify;\">";
            tooltip += "<b><font size=+2>" + PROFILE_INFO + "</font></b><br>";
            tooltip += "<br>";
            tooltip += "<b><font size=+1>" + LAYER_NAME + ": </font></b>"
                    + rLayer.getName() + "<br>";
            tooltip += "<br>";
            tooltip += "<b><font size=+1>" + PROFILE + "</font></b><br>";
            tooltip += "<b>" + PROFILE_LENGTH + ": </b>"
                    + df.format(new Double(line.getLength())) + "<br>";// Lenght
                                                                       // of
                                                                       // profile
            tooltip += "<b>" + MEAN_SLOPE + ": </b>"
                    + df.format(Math.atan((max - min) / line.getLength()))
                    + "°<br>";// Slope
            tooltip += "<b>" + STARTING_POINT + ": </b>" + df.format(start.x)
                    + " - " + df.format(start.y) + "<br>";// Coordinate of
                                                          // starting point of
            // profile
            tooltip += "<b>" + ENDING_POINT + ": </b>" + df.format(end.x)
                    + " - " + df.format(end.y) + "<br>";// Coordinate of ending
                                                        // point of profile

            tooltip += "<b>"
                    + CELL_SIZE
                    + ": </b>"
                    + (rLayer.getWholeImageEnvelope().getMaxX() - rLayer
                            .getWholeImageEnvelope().getMinX())
                    / rLayer.getOrigImageWidth() + "<br>";// Cell size //
                                                          // profile
            tooltip += "<br>";

            tooltip += "<b><font size=+1>" + CELL_STAT + "</font></b><br>";
            tooltip += "<table border='1'>";
            tooltip += "</td><td bgcolor=#CCCCCC align='center'> " + MIN
                    + "</td><td bgcolor=#CCCCCC align='center'> " + MAX
                    + "</td><td bgcolor=#CCCCCC align='center'> " + MEAN
                    + "</td><td bgcolor=#CCCCCC align='center'> " + SUM

                    + "</td></tr>";
            tooltip += "</td><td align='right'>" + new Double(min)// min
                    + "</td><td align='right'>" + new Double(max) // max
                    + "</td><td align='right'>" + new Double(mean)// mean
                    + "</td><td align='right'>" + new Double(sum)// sum

                    + "</td></tr>";

            tooltip += "</table>";

            tooltip += "</DIV></BODY></HTML>";
            outpanel.createNewDocument();
            outpanel.append(tooltip);
            // -- End of HTML Info Panel

            // -- graph stuff
            ShowProfile myScorePlot = new ShowProfile(this.resultFC);
            Plot2DPanelOJ plot = myScorePlot.getPlot();

            // -- End graph stuff

            // -- OK button Panel
            JPanel okPanel = new JPanel();
            final JButton okButton = new JButton(CANCEL) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(100, 25);
                }
            };
            okButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    return;
                }
            });
            okPanel.add(okButton);
            // -- End of OK Buttom

            JTabbedPane tabbedPane = new JTabbedPane();
            Border mainComponentBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5));
            tabbedPane.setBorder(mainComponentBorder);
            tabbedPane.add(plot, PLOT);
            tabbedPane.add(outpanel, INFO);

            frame.add(tabbedPane, BorderLayout.CENTER);
            frame.add(okPanel, BorderLayout.SOUTH);

            frame.setClosable(true);
            frame.setResizable(true);
            frame.setMaximizable(true);
            frame.setSize(800, 450);
            frame.setVisible(true);

            context.getWorkbench().getFrame().addInternalFrame(frame);
        } else {

            getPanel().getContext().warnUser(
                    "Query outside the extension of selected Raster layer");
        }

    }

    private void processLine(Geometry line) throws IOException,
            RasterImageLayer.RasterDataNotFoundException {

        double x, y, x2, y2;
        Coordinate[] coords = line.getCoordinates();

        for (int i = 0; i < coords.length - 1; i++) {
            x = coords[i].x;
            y = coords[i].y;
            x2 = coords[i + 1].x;
            y2 = coords[i + 1].y;
            processSegment(x, y, x2, y2);
        }

    }

    private void processSegment(double x, double y, double x2, double y2)
            throws RasterImageLayer.RasterDataNotFoundException, IOException {

        double dx, dy, d, n;

        dx = Math.abs(x2 - x);
        dy = Math.abs(y2 - y);

        if (dx > 0.0 || dy > 0.0) {
            if (dx > dy) {
                dx /= this.rstLayer.getWindowCellSize().x;
                n = dx;
                dy /= dx;
                dx = this.rstLayer.getWindowCellSize().x;
            } else {
                dy /= this.rstLayer.getWindowCellSize().y;
                n = dy;
                dx /= dy;
                dy = this.rstLayer.getWindowCellSize().y;
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

    private void addPoint(double x, double y)
            throws RasterImageLayer.RasterDataNotFoundException, IOException {

        double z;
        double dDX, dDY, dDZ;

        // z = this.rstLayer.getValueAt(x, y);
        z = this.rLayer.getCellValue(x, y, 0);

        if (this.nPoints == 0) {
            dDist = 0.0;
            dHorzDist = 0.0;
        } else {
            dDX = x - m_dLastX;
            dDY = y - m_dLastY;
            if (this.rstLayer.isNoDataValue(z)
                    || this.rstLayer.isNoDataValue(m_dLastZ)) {
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

        this.nPoints++;

        Point geometry = new GeometryFactory()
                .createPoint(new Coordinate(x, y));
        Feature fpoint = new BasicFeature(this.resultFSchema);
        fpoint.setGeometry(geometry);
        fpoint.setAttribute("X", new Double(x));
        fpoint.setAttribute("Y", new Double(y));
        fpoint.setAttribute("Z", new Double(z));
        fpoint.setAttribute("PlaneDist", new Double(dDist));
        fpoint.setAttribute("TerrainDist", new Double(dHorzDist));

        /*
         * z //--graph stuff if (!this.rstLayer.isNoDataValue(z)){
         * serie.add(dDist, z); }
         */
        this.resultFC.add(fpoint);

    }

    protected Feature[] features = null;

}

final class ShowProfile extends JFrame {
    Plot2DPanelOJ plot = null;

    public ShowProfile(FeatureCollection fc) {

        // Build a 2D data set
        double[][] datas1 = new double[fc.size()][2];
        for (int j = 0; j < fc.size(); j++) {
            Feature f = fc.getFeatures().get(j);
            datas1[j][0] = (Double) f.getAttribute("PlaneDist");
            datas1[j][1] = (Double) f.getAttribute("Z");
        }
        // Build the 2D scatterplot of the datas in a Panel
        // LINE, SCATTER, BAR, QUANTILE, STAIRCASE, (HISTOGRAMM?)
        Plot2DPanelOJ plot2dA = new Plot2DPanelOJ();
        plot2dA.addLinePlot("graph", datas1);
        // plot2dA.addScatterPlot("pts",datas1);
        // ====================
        plot2dA.setAxisLabel(
                0,
                I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.2d-distance"));
        plot2dA.setAxisLabel(
                1,
                I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values"));
        // Display a Frame containing the plot panel
        // new FrameView(plot2dA);
        this.plot = plot2dA;

    }

    public Plot2DPanelOJ getPlot() {
        return this.plot;
    }

}
