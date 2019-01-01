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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.Raster;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayer.RasterDataNotFoundException;
import org.openjump.core.rasterimage.Stats;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
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
 * @author Giuseppe Aruta [2018_06_26] Substitute output HTMLDoc to JTable.
 *         Allow selection of multiple layers on plugin dialog.
 */
public class DEMStatisticsPlugIn extends ThreadedBasePlugIn {
    private static final String R_MAX = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum");
    private static final String R_MIN = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum");
    private static final String R_MEAN = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.mean");
    private static final String R_STD = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.standard-dev");
    private final static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
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
    private final static String RASTER_BANDS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands_number");
    private final static String AREA = I18N
            .get("ui.plugin.LayerStatisticsPlugIn.area");

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class));
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

    public static MultiInputDialog dialog;
    public static JPanel panel;
    private JList<RasterImageLayer> list;
    private final DefaultListModel<RasterImageLayer> listModel = new DefaultListModel<RasterImageLayer>();
    private JScrollPane scroller;
    private static final Dimension scroller_size = new Dimension(400, 200);

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(),
                true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        return true;

    }

    @SuppressWarnings("unchecked")
    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) {
        final Collection<RasterImageLayer> rlayers = context.getTask()
                .getLayerManager().getLayerables(RasterImageLayer.class);
        dialog.addSubTitle(I18N.get("ui.GenericNames.select-layers"));
        dialog.addLabel(I18N
                .get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.select-multiple-layers"));
        listModel.removeAllElements();
        for (final RasterImageLayer currentLayer : rlayers) {
            listModel.addElement(currentLayer);

        }

        list = new JList<RasterImageLayer>(listModel);
        // list.setBorder(new MatteBorder(1, 1, 1, 1, dialog.getForeground()));
        list.setFont(new Font(dialog.getFont().getName(), Font.BOLD, 13));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();
        layerListCellRenderer.setCheckBoxVisible(false);
        layerListCellRenderer.setProgressIconLabelVisible(false);
        list.setCellRenderer(layerListCellRenderer);

        panel = new JPanel(new BorderLayout());
        panel.setMinimumSize(scroller_size);
        panel.setPreferredSize(scroller_size);
        panel.add(list);
        scroller = new JScrollPane(panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        dialog.addRow(scroller);

        dialog.setResizable(false);
        dialog.pack();

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

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {

        monitor.allowCancellationRequests();
        monitor.report(getName()
                + ": "
                + I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.computing"));
        final Locale locale = new Locale("en", "UK");
        final String pattern = "###.########";
        final DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);
        final JTable jTable = new JTable();
        if (!dialog.wasOKPressed()) {
            return;
        } else {
            final Collection<RasterImageLayer> rLayers = list
                    .getSelectedValuesList();
            String min;
            String max;
            String mean;
            String stddev;
            String bands;
            final DefaultTableModel dtm = (DefaultTableModel) jTable.getModel();

            final Object[] header = new Object[] { XMIN, YMIN, COLUMNS, ROWS,
                    CELL_SIZE, AREA, RASTER_BANDS, R_MIN, R_MAX, R_MEAN, R_STD,
                    NODATA, VALIDCELLS, NODATACELLS };
            dtm.addColumn(I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Layer")
                    .toUpperCase(), header);
            // .getSelectedObjects();

            for (final Object name2 : rLayers) {
                final RasterImageLayer slayer = (RasterImageLayer) name2;
                final Stats stats = slayer.getMetadata().getStats();
                final Raster raster = slayer.getRasterData(null);
                final Double nodata = slayer.getNoDataValue();
                bands = "" + slayer.getNumBands();
                if (slayer.getNumBands() > 1) {
                    min = df.format(stats.getMin(0)) + "-"
                            + df.format(stats.getMin(1)) + "-"
                            + df.format(stats.getMin(2));
                    max = df.format(stats.getMax(0)) + "-"
                            + df.format(stats.getMax(1)) + "-"
                            + df.format(stats.getMax(2));
                    mean = df.format(stats.getMean(0)) + "-"
                            + df.format(stats.getMean(1)) + "-"
                            + df.format(stats.getMean(2));
                    stddev = df.format(stats.getStdDev(0)) + "-"
                            + df.format(stats.getStdDev(1)) + "-"
                            + df.format(stats.getStdDev(2));
                } else {
                    min = df.format(stats.getMin(0));
                    max = df.format(stats.getMax(0));
                    mean = df.format(stats.getMean(0));
                    stddev = df.format(stats.getStdDev(0));
                }
                final Envelope extent = slayer.getWholeImageEnvelope(); // Envelope
                                                                        // of

                // final Locale locale1 = new Locale("en", "UK");
                final String pattern1 = "###.## ";
                // final DecimalFormat df1 = (DecimalFormat) NumberFormat
                // .getNumberInstance(locale1);
                df.applyPattern(pattern1);
                final double cellSizeX = cellSizeX(raster, extent);
                final double cellSizeY = cellSizeY(raster, extent);

                final String CELLSIZEX = df.format(cellSizeX);// Cell

                final String CELLSIZEY = df.format(cellSizeY);
                final String cellSize = CELLSIZEX + "x" + CELLSIZEY;
                final String minx = df.format(extent.getMinX());
                final String miny = df.format(extent.getMinY());
                final int X = raster.getWidth(); // Number of columns
                final int Y = raster.getHeight(); // Number of rows
                /*
                 * Giuseppe Aruta Nov. 2015 workaround for OpenJUMP bug 410
                 * (Sextante), If nodata value is -3.40282346639E38 and min
                 * value -9999, -99999 or 1.70141E38. Those two values are
                 * displayed in red on DEMStatistic table
                 */
                String nodataText = null;
                final String texmin = df.format(stats.getMin(0));
                final double nda = slayer.getNoDataValue();
                final String begin = "<b><font color='red'>";
                final String end = "</font></b>";
                if (nda == -3.40282346639E38 || nodata == -1.79769313486E308) {
                    if (stats.getMin(0) == -9999 || stats.getMin(0) == -99999
                            || stats.getMin(0) == 1.70141E38) {
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
                final int nodatacells = nodata(raster, nodata);// number of no
                                                               // data
                // cells
                final int validcells = X * Y - nodatacells;// Number of
                // valid
                // cells
                final double area = raster.getWidth() * raster.getHeight()
                        * cellSizeX * cellSizeY;
                final String TOT_AREA = df.format(area);
                final Object[] layers = new Object[] { minx, miny, X, Y,
                        cellSize, TOT_AREA, bands, min, max, mean, stddev,
                        nodataText, validcells, nodatacells };

                dtm.addColumn(slayer.getName().toUpperCase(), layers);

            }

            final JTable table = new JTable(dtm) {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

            };

            // final JTable table = new JTable(dtm);

            table.setRowHeight(22);
            final JScrollPane scrollPane = new JScrollPane(table);
            table.setPreferredScrollableViewportSize(new Dimension(500, 300));

            AdditionalResults.addAdditionalResultAndShow(getName(), scrollPane);

            return;

        }
    }

}
