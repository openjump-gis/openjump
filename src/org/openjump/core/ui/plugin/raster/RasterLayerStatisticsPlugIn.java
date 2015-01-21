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

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Giuseppe Aruta 2015_01_21 Class derived from LayerStatiticsPlugIn.class
 * Computes various statistics on cells for selected Raster Image Layers.
 */
public class RasterLayerStatisticsPlugIn extends AbstractPlugIn {

    private final static String LAYER_STATISTICS = I18N
            .get("ui.plugin.LayerStatisticsPlugIn.layer-statistics");
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

    public boolean execute(PlugInContext context) throws Exception {
        /*
         * Overwrite Locale to UK Decimal format ####.##
         */
        Locale locale = new Locale("en", "UK");
        String pattern = "###.########";
        DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);

        final WorkbenchContext wbcontext = context.getWorkbenchContext();

        HTMLFrame out = context.getOutputFrame();
        out.createNewDocument();
        out.setBackground(Color.lightGray);
        out.addHeader(1, LAYER_STATISTICS);
        for (Iterator i = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).iterator(); i.hasNext();) {
            RasterImageLayer slayer = (RasterImageLayer) i.next();
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(slayer);

            // Get the statistics
            int bands = slayer.getNumBands();

            String bandstring = ": " + String.valueOf(bands);
            String min = df.format(rstLayer.getMinValue());// Min value of
                                                           // cells
            String max = df.format(rstLayer.getMaxValue());// Max value of
                                                           // cells
            String mean = df.format(rstLayer.getMeanValue());// Mean value
            int X = rstLayer.getNX(); // Number of columns
            int Y = rstLayer.getNY(); // Number of rows
            String sum = df.format(rstLayer.getMeanValue()
                    * (X * Y - nodata(context, rstLayer)));// Sum
            String nodata = df.format(rstLayer.getNoDataValue());// No data
                                                                 // value
            String variance = df.format(rstLayer.getVariance());// variance
            double var = rstLayer.getVariance();// Variance as double
            String std = df.format(Math.sqrt(var));// Standard deviation
            String cvar = df.format(var / rstLayer.getMeanValue());// Covariance
            int validcells = X * Y - nodata(context, rstLayer);// Number of
                                                               // valid cells
            int nodatacells = nodata(context, rstLayer);// number of no data
                                                        // cells

            out.addHeader(2, I18N.get("ui.plugin.LayerStatisticsPlugIn.layer"
                    + ": ")
                    + " " + slayer.getName());

            Envelope layerEnv = slayer.getWholeImageEnvelope();
            out.addField(I18N.get("ui.plugin.LayerStatisticsPlugIn.envelope"),
                    layerEnv.toString());
            out.addField(
                    I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands_number"),
                    bandstring);
            out.append("<table border='1'>");
            out.append("<tr><td bgcolor=#CCCCCC align='center'> " + MIN
                    + "</td><td bgcolor=#CCCCCC align='center'> " + MAX
                    + "</td><td bgcolor=#CCCCCC align='center'> " + MEAN
                    + "</td><td bgcolor=#CCCCCC align='center'> " + SUM
                    + "</td><td bgcolor=#CCCCCC align='center'> " + NODATA
                    + "</td><td bgcolor=#CCCCCC align='center'> " + VARIANCE
                    + "</td><td bgcolor=#CCCCCC align='center'> " + STD
                    + "</td><td bgcolor=#CCCCCC align='center'> " + CVAR
                    + "</td><td bgcolor=#CCCCCC align='center'> " + VALIDCELLS
                    + "</td><td bgcolor=#CCCCCC align='center'> " + NODATACELLS
                    + "</td></tr>");
            out.append("</td><td align='right'>" + min
                    + "</td><td align='right'>" + max
                    + "</td><td align='right'>" + mean
                    + "</td><td align='right'>" + sum
                    + "</td><td align='right'>" + nodata
                    + "</td><td align='right'>" + variance
                    + "</td><td align='right'>" + std
                    + "</td><td align='right'>" + cvar
                    + "</td><td align='right'>" + validcells
                    + "</td><td align='right'>" + nodatacells + "</td></tr>");
            out.append("</table>");
            out.surface();
        }
        return true;
    }

    public String getName() {
        return I18N
                .get("com.vividsolutions.jump.workbench.ui.plugin.LayerStatisticsPlugIn");
    }

}
