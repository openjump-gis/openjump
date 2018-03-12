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

package org.openjump.core.ui.plugin.raster.statistics;

import java.awt.Color;
import java.awt.image.Raster;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.Icon;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayer.RasterDataNotFoundException;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

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
 * @author Giuseppe Aruta [2015_01_27] Computes various statistics for selected
 *         layers.
 * @author Giuseppe Aruta [2015_01_27] added header with the number of selected
 *         raster layers
 * @author Giuseppe Aruta [2015_04_09] Reduce display of large nodata values (es
 *         QGIS) to readable number
 * @author Giuseppe Aruta [2015_05_16] Added X*Y Cell size
 * @author Giuseppe Aruta [2018_01_19] Removed depency to
 *         OpenJUMPSextanteRasterLayer class. Clean the code
 */
public class DEMStatisticsPlugIn extends AbstractPlugIn {

    private final static String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.max");
    private final static String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.min");
    private final static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
    private final static String NUM_LAYER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Layers");
    private final static String NODATACELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.nodatacell");
    private final static String VALIDCELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.validcells");
    private final static String XMIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.xmin");
    private final static String YMIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.ymin");
    private final static String CELL_SIZE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_cell");
    private final static String COLUMNS = I18N
            .get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.columns");
    private final static String ROWS = I18N
            .get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.rows");
    private final static String LAYERS = I18N
            .get("org.openjump.core.ui.plugin.queries.SimpleQuery.selected-layers")
            + " :";

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(
                        1, RasterImageLayer.class))
                .add(checkFactory
                        .createRasterImageLayerExactlyNBandsMustExistCheck(1));
    }

    /*
     * Count the number of cells (of a Sextante monoband raster layer) with no
     * data value
     */

    public int nodata(Raster ras, double nodata) throws IOException,
            RasterDataNotFoundException {
        int counter = 0;

        final int nx = ras.getWidth();
        final int ny = ras.getHeight();
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                final double value = ras.getSampleDouble(x, y, 0);
                if (value == nodata) {
                    counter++;
                }
            }
        }
        return counter;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        final Locale locale = new Locale("en", "UK");
        final String pattern = "###.########";
        final DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);
        final WorkbenchContext wbcontext = context.getWorkbenchContext();
        final int ras = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).size();
        final HTMLPanel out = new HTMLPanel();
        out.setRecordNavigationControlVisible(false);
        out.createNewDocument();
        out.setBackground(Color.lightGray);
        out.addHeader(1, NUM_LAYER + ": " + ras);
        out.append("<table border='1'>");
        out.append("<tr><td bgcolor=#CCCCCC align='center'> "
                + I18N.get("ui.plugin.LayerStatisticsPlugIn.layer")
                + "</td><td bgcolor=#CCCCCC align='center'> " + MIN
                + "</td><td bgcolor=#CCCCCC align='center'> " + MAX
                + "</td><td bgcolor=#CCCCCC align='center'> " + NODATA
                + "</td><td bgcolor=#CCCCCC align='center'> " + VALIDCELLS
                + "</td><td bgcolor=#CCCCCC align='center'> " + NODATACELLS
                + "</td><td bgcolor=#CCCCCC align='center'> " + COLUMNS
                + "</td><td bgcolor=#CCCCCC align='center'> " + ROWS
                + "</td><td bgcolor=#CCCCCC align='center'> " + XMIN
                + "</td><td bgcolor=#CCCCCC align='center'> " + YMIN
                + "</td><td bgcolor=#CCCCCC align='center'> " + CELL_SIZE
                + "</td></tr>");
        for (final Iterator i = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).iterator(); i.hasNext();) {
            final RasterImageLayer slayer = (RasterImageLayer) i.next();
            final Raster raster = slayer.getRasterData(null);
            final Double nodata = slayer.getNoDataValue();
            final double minRas = slayer.getMetadata().getStats().getMin(0);
            final double maxRas = slayer.getMetadata().getStats().getMax(0);
            final double meanRas = slayer.getMetadata().getStats().getMean(0);
            final Envelope extent = slayer.getWholeImageEnvelope(); // Envelope
                                                                    // of
            // layer
            String min = df.format(minRas);// Min value of
                                           // cells
            final String max = df.format(maxRas);// Max value of
            df.format(meanRas);

            final Locale locale1 = new Locale("en", "UK");
            final String pattern1 = "###.## ";
            final DecimalFormat df1 = (DecimalFormat) NumberFormat
                    .getNumberInstance(locale1);
            df.applyPattern(pattern1);
            final String cellSizex = df1.format(cellSizeX(raster, extent));// Cell
            // size
            final String cellSizey = df1.format(cellSizeY(raster, extent));
            final String cellSize = cellSizex + "x" + cellSizey;
            final String minx = df.format(extent.getMinX());
            final String miny = df.format(extent.getMinY());
            final int X = raster.getWidth(); // Number of columns
            final int Y = raster.getHeight(); // Number of rows

            /*
             * Giuseppe Aruta Nov. 2015 workaround for OpenJUMP bug 410
             * (Sextante), If nodata value is -3.40282346639E38 and min value
             * -9999, -99999 or 1.70141E38. Those two values are displayed in
             * red on DEMStatistic table
             */
            String nodataText = null;
            final String texmin = df.format(minRas);
            final double nda = slayer.getNoDataValue();
            final String begin = "<b><font color='red'>";
            final String end = "</font></b>";
            if (nda == -3.40282346639E38 || nodata == -1.79769313486E308) {
                if (minRas == -9999 || minRas == -99999 || minRas == 1.70141E38) {
                    nodataText = begin + nodata + end;
                    min = begin + texmin + end;
                } else {
                    nodataText = Double.toString(nda);
                    min = texmin;
                }

            } else {
                nodataText = Double.toString(nda);
                min = texmin;

            }
            final int nodatacells = nodata(raster, nodata);// number of no data
            // cells
            final int validcells = X * Y - nodatacells;// Number of
            // valid
            // cells

            out.append("</td><td align='right'>" + slayer.getName()
                    + "</td><td align='right'>" + min
                    + "</td><td align='right'>" + max
                    + "</td><td align='right'>" + nodataText
                    + "</td><td align='right'>" + validcells
                    + "</td><td align='right'>" + nodatacells
                    + "</td><td align='right'>" + X + "</td><td align='right'>"
                    + Y + "</td><td align='right'>" + minx
                    + "</td><td align='right'>" + miny
                    + "</td><td align='right'>" + cellSize + "</td></tr>");

        }
        out.append("</table>");
        AdditionalResults.addAdditionalResultAndShow(getName() + "[" + LAYERS
                + +ras + "]", out);

        // out.setPreferredSize(new Dimension(800, 100));
        // out.setMinimumSize(new Dimension(800, 100));
        // out.surface();
        return true;
    }

    @Override
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.name");
    }

    public Icon getIcon() {
        return IconLoader.icon("grid_statistics.png");
    }

    /*
     * Gets cell size
     */
    public double cellSizeX(Raster r, Envelope env) throws IOException {
        return env.getWidth() / r.getWidth();
    }

    public double cellSizeY(Raster r, Envelope env) throws IOException {
        return env.getHeight() / r.getHeight();
    }

}
