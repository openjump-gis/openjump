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
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.Icon;

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
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Giuseppe Aruta [2015_01_27] Computes various statistics for selected layers.
 * Giuseppe Aruta [2015_01_27] added header with the number of selected raster layers Giuseppe
 * Giuseppe Aruta [2015_04_09] Reduce display of large nodata values (es QGIS) to
 * readable number
 * Giuseppe Aruta [2015_05_16] Added X*Y Cell size
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
    private final static String UNSPECIFIED = I18N
            .get("coordsys.CoordinateSystem.unspecified");

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
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

        Locale locale = new Locale("en", "UK");
        String pattern = "###.########";
        DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);

        final WorkbenchContext wbcontext = context.getWorkbenchContext();

        int ras = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).size();
        HTMLFrame out = context.getOutputFrame();
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
        for (Iterator i = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).iterator(); i.hasNext();) {
            RasterImageLayer slayer = (RasterImageLayer) i.next();

            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(slayer);

            slayer.getNumBands();

            String min = df.format(rstLayer.getMinValue());// Min value of
                                                           // cells
            String max = df.format(rstLayer.getMaxValue());// Max value of
            df.format(rstLayer.getMeanValue());

            Envelope extent = slayer.getWholeImageEnvelope(); // Envelope of
                                                              // layer

            Locale locale1 = new Locale("en", "UK");
            String pattern1 = "###.## ";
            DecimalFormat df1 = (DecimalFormat) NumberFormat
                    .getNumberInstance(locale1);
            df.applyPattern(pattern1);
            String cellSizex = df1.format(rstLayer.getLayerCellSize().x);// Cell
                                                                         // size
            String cellSizey = df1.format(rstLayer.getLayerCellSize().y);
            String cellSize = cellSizex + "x" + cellSizey;
            String minx = df.format(extent.getMinX());
            String miny = df.format(extent.getMinY());
            int X = rstLayer.getNX(); // Number of columns
            int Y = rstLayer.getNY(); // Number of rows
            
            
            /*
             * Giuseppe Aruta Nov. 2015
             *  workaround for OpenJUMP bug 410 (Sextante), If nodata value is -3.40282346639E38
     		 *  and min value -9999, -99999 or 1.70141E38. Those two values are displayed in red
  			 *  on DEMStatistic table
             */
            String nodata = null;
            String texmin = df.format(rstLayer.getMinValue());
            double nda = slayer.getNoDataValue();
            String begin ="<b><font color='red'>";
            String end = "</font></b>";
            if (nda == -3.40282346639E38 || rstLayer.getNoDataValue() == -1.79769313486E308) {
            	 if (rstLayer.getMinValue() == -9999
                         || rstLayer.getMinValue() == -99999
                         || rstLayer.getMinValue() == 1.70141E38){
            		 nodata = begin+nodata+end;
            		 min = begin+texmin+end;
            	 } else{
            		 nodata = Double.toString(nda); 
            		 min = texmin;
            	 }
            	
            } else{
            	nodata = Double.toString(nda); 
       		 	min = texmin;
            	
            }
            
            
            
            /*
            
            
            
            if (nda == -3.4028234e+038) {
                nodata = "<b><font color='red'>-3.4028234e+038</font></b>";
            } else {
                nodata = Double.toString(nda);
            }*/

            int validcells = X * Y - nodata(context, rstLayer);// Number of
                                                               // valid
                                                               // cells
            int nodatacells = nodata(context, rstLayer);// number of no data
                                                        // cells
            out.append("</td><td align='right'>" + slayer.getName()
                    + "</td><td align='right'>" + min
                    + "</td><td align='right'>" + max
                    + "</td><td align='right'>" + nodata
                    + "</td><td align='right'>" + validcells
                    + "</td><td align='right'>" + nodatacells
                    + "</td><td align='right'>" + X + "</td><td align='right'>"
                    + Y + "</td><td align='right'>" + minx
                    + "</td><td align='right'>" + miny
                    + "</td><td align='right'>" + cellSize + "</td></tr>");

        }

        out.append("</table>");
        out.setPreferredSize(new Dimension(800, 100));
        out.setMinimumSize(new Dimension(800, 100));
        out.surface();
        return true;
    }

    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.name");
    }

    public Icon getIcon() {
        return IconLoader.icon("grid_statistics.png");
    }

}
