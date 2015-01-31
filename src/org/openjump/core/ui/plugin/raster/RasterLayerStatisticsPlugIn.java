/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package org.openjump.core.ui.plugin.raster;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Giuseppe Aruta 2015_01_21 Class derived from LayerStatiticsPlugIn.class
 * Computes various statistics on cells for selected Raster Image Layers.
 * 
 * Giuseppe Aruta 2015_01_31.RasterLayerStatistics.class. Upgraded statistics to all bands 
 * Add plugin icon. Change name to "Raster statistics"
 */
public class RasterLayerStatisticsPlugIn extends AbstractPlugIn {
    private final static String CANCEL = I18N.get("ui.OKCancelPanel.cancel");
    private final static String LAYER_STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.raster-statistics");
    private final static String LAYER = I18N
            .get("ui.plugin.LayerStatisticsPlugIn.layer");
    private final static String ENV = I18N
            .get("ui.plugin.LayerStatisticsPlugIn.envelope");
    private final static String NUMBANDS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands_number");
    private final static String BAND = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.band");
    private final static String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.max");
    private final static String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.min");
    private final static String MEAN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.mean");
    private final static String SUM = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.sum");
    private final static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
    private final static String VARIANCE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.variance");
    private final static String STD = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.std");
    private final static String CVAR = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cvar");
    private final static String NODATACELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.nodatacell");
    private final static String VALIDCELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.validcells");

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(
                        1, RasterImageLayer.class));
    }

    /*
     * Count the number of cells (of a Sextante monoband raster layer) with no
     * data value
     */
    public int nodata(PlugInContext context,
            OpenJUMPSextanteRasterLayer rstLayer) {
        int counter = 0;
        GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        int nx = rstLayer.getLayerGridExtent().getNX();
        int ny = rstLayer.getLayerGridExtent().getNY();
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                double value = gwrapper.getCellValueAsDouble(x, y, 0);
                if (value == rstLayer.getNoDataValue())
                    counter++;
            }
        }
        return counter;
    }

    public String StatisticsText(PlugInContext context, RasterImageLayer rLayer)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {
        /*
         * Overwrite Locale to UK Decimal format ####.##
         */
        Locale locale = new Locale("en", "UK");
        String pattern = "###.########";
        DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);

        String infotext = null;
        OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(rLayer);
        Envelope layerEnv = rLayer.getWholeImageEnvelope();
        // Get the statistics
        int numBands = rLayer.getNumBands();

        String bandstring = ": " + String.valueOf(numBands);
        df.format(rstLayer.getMinValue());
        df.format(rstLayer.getMaxValue());
        df.format(rstLayer.getMeanValue());
        int X = rstLayer.getNX(); // Number of columns
        int Y = rstLayer.getNY(); // Number of rows
        df.format(rstLayer.getMeanValue() * (X * Y - nodata(context, rstLayer)));
        String nodata = df.format(rstLayer.getNoDataValue());// No data
        df.format(rstLayer.getVariance());
        double var = rstLayer.getVariance();// Variance as double
        df.format(Math.sqrt(var));
        df.format(var / rstLayer.getMeanValue());
        int validcells = X * Y - nodata(context, rstLayer);// Number of
                                                           // valid cells

        int nodatacells = nodata(context, rstLayer);// number of no data
                                                    // cells

        infotext = "<HTML><BODY>";
        infotext += "<table border='0.1'>";
        infotext += "<tr><td><b>" + LAYER + "</b> </td><td>" + rLayer.getName()
                + "</td></tr>";
        infotext += "</table><br>";

        infotext += "<table border='0.1'>";
        infotext += "<tr><td><b>" + ENV + "</b> </td><td>"
                + layerEnv.toString() + "</td></tr>";
        infotext += "<tr><td><b>" + NUMBANDS + "</b> </td><td>" + bandstring
                + "</td></tr>";
        infotext += "<tr><td><b>" + NODATA + "</b> </td><td>" + nodata
                + "</td></tr>";
        infotext += "<tr><td><b>" + VALIDCELLS + "</b> </td><td>"
                + Integer.toString(validcells) + "</td></tr>";
        infotext += "<tr><td><b>" + NODATACELLS + "</b> </td><td>"
                + Integer.toString(nodatacells) + "</td></tr>";
        infotext += "</table><br>";

        infotext += "<table border='1'>";
        infotext += "<tr><td bgcolor=#CCCCCC align='center'> " + BAND
                + "</td><td bgcolor=#CCCCCC align='center'> " + MIN
                + "</td><td bgcolor=#CCCCCC align='center'> " + MAX
                + "</td><td bgcolor=#CCCCCC align='center'> " + MEAN
                + "</td><td bgcolor=#CCCCCC align='center'> " + SUM
                + "</td><td bgcolor=#CCCCCC align='center'> " + VARIANCE
                + "</td><td bgcolor=#CCCCCC align='center'> " + STD
                + "</td><td bgcolor=#CCCCCC align='center'> " + CVAR

                + "</td></tr>";
        for (int b = 0; b < numBands; b++) {

            infotext += "</td><td align='right'>"
                    + b
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMinValue(b))// min
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMaxValue(b))// max
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMeanValue(b))// mean
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMeanValue(b)
                            * (X * Y - nodata(context, rstLayer)))// sum

                    + "</td><td align='right'>"
                    + df.format(rstLayer.getVariance(b))// variance
                    + "</td><td align='right'>"
                    + df.format(Math.sqrt(rstLayer.getVariance(b)))// std
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getVariance(b)
                            / rstLayer.getMeanValue(b))// cvar

                    + "</td></tr>";
        }
        infotext += "</table>";
        infotext += "</DIV></BODY></HTML>";

        return infotext;
    }

    public boolean execute(PlugInContext context) throws Exception {

        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        // final WorkbenchContext wbcontext = context.getWorkbenchContext();
        /*
         * Overwrite Locale to UK Decimal format ####.##
         */
        Locale locale = new Locale("en", "UK");
        String pattern = "###.########";
        DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);

        context.getWorkbenchContext();

        // HTMLFrame out = context.getOutputFrame();
        final JInternalFrame frame = new JInternalFrame(LAYER_STATISTICS);
        HTMLPanel out = new HTMLPanel();
        out.getRecordPanel().removeAll();
        out.createNewDocument();
        out.addHeader(1, LAYER_STATISTICS);
        // for (Iterator i = wbcontext.getLayerNamePanel()
        // .selectedNodes(RasterImageLayer.class).iterator(); i.hasNext();) {
        // RasterImageLayer rLayer = (RasterImageLayer) i.next();

        out.append(StatisticsText(context, rLayer));
        // }
        // -- OK button Panel
        JPanel okPanel = new JPanel();
        final JButton okButton = new JButton(CANCEL) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                return;
            }
        });
        okPanel.add(okButton);
        // -- End of OK Buttom

        /*
         * JTabbedPane tabbedPane = new JTabbedPane(); Border
         * mainComponentBorder = BorderFactory.createCompoundBorder(
         * BorderFactory.createEtchedBorder(),
         * BorderFactory.createEmptyBorder(5, 5, 5, 5));
         * tabbedPane.setBorder(mainComponentBorder); tabbedPane.add(out,
         * LAYER_STATISTICS); frame.add(tabbedPane, BorderLayout.CENTER);
         */

        frame.add(out, BorderLayout.CENTER);
        frame.add(okPanel, BorderLayout.SOUTH);

        frame.setClosable(true);
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setSize(800, 450);
        frame.setVisible(true);
        context.getWorkbenchFrame().addInternalFrame(frame);
        // }

        return true;
    }

    public String getName() {
        return LAYER_STATISTICS;
    }

    public static final ImageIcon ICON = IconLoader.icon("statistics16.png");

}
