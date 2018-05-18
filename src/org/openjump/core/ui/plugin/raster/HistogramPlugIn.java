/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */
package org.openjump.core.ui.plugin.raster;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.math.plot.plots.Plot;
import org.math.plot.render.AbstractDrawer;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridRasterWrapper;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.plot.Plot2DPanelOJ;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 *
 * created on 19.10.2018
 * 
 * @author Giuseppe Aruta
 */
public class HistogramPlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    private final Font darkLabelFont = AbstractDrawer.DEFAULT_FONT;
    private final Color LIGHT_GRAY = new Color(230, 230, 230);
    public final static Font default_font = new Font("BitStream Vera Sans",
            Font.BOLD, 10);
    private String sHistogram = "Histogram";
    private String T2 = "number of ranges";
    private RasterImageLayer selLayer = null;
    private int ranges = 100;
    private String sName = "Create Histogram Plot";
    private String CLAYER = "select layer";
    public static final Icon ICON = IconLoader.icon("histogramme.png");

    /**
     * this method is called on the startup by JUMP/OpenJUMP. We set here the
     * menu entry for calling the function.
     */
    @Override
    public void initialize(PlugInContext context) throws Exception {
        CLAYER = GenericNames.SELECT_LAYER;
        T2 = I18N
                .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Number-of-ranges");
        sHistogram = I18N
                .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Histogram-Plot");
        I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.count");
        sName = I18N
                .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn");
        I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");

        final FeatureInstaller featureInstaller = new FeatureInstaller(
                context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(this,
                new String[] { MenuNames.RASTER }, sName + "...", false, // checkbox
                null, // icon
                createEnableCheck(context.getWorkbenchContext()));
    }

    public Icon getIcon() {
        return ICON;
    }

    /**
     * This method is used to define when the menu entry is activated or
     * disabled. In this example we allow the menu entry to be usable only if
     * one layer exists.
     */
    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class)).add(
                checkFactory
                        .createRasterImageLayerExactlyNBandsMustExistCheck(1));
    }

    /**
     * this function is called by JUMP/OpenJUMP if one clicks on the menu entry.
     * It is called before the "run" method and useful to do all the GUI
     * /user-input things In this example we call two additional methods
     * {@link #setDialogValues(MultiInputDialog, PlugInContext)} and
     * {@link #getDialogValues(MultiInputDialog)} to obtain the Layer and the
     * buffer radius by the user.
     */

    JCheckBox box;

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        reportNothingToUndoYet(context);

        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), sName, true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);

        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        createHistogram(context, selLayer);

    }

    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) {

        final Collection<RasterImageLayer> rlayers = context.getTask()
                .getLayerManager().getLayerables(RasterImageLayer.class);
        // dialog.setSideBarImage(IconLoader.icon("histdisplay.png"));
        dialog.setSideBarDescription("Create a frequency histogram of raster data from selected raster image layer, choosing the number of intervals. Optionally the table of frequency can be created");
        dialog.addComboBox(CLAYER, context.getLayerManager()
                .getRasterImageLayers().get(0), rlayers, "");

        // dialog.addSeparator();
        // dialog.addRow();
        dialog.addIntegerField(T2, ranges, 6, T2);
        box = dialog
                .addCheckBox(
                        "Calculate table",
                        true,
                        "Calculate a table with intervals, min. value of class, max. value of class, absolute frequency of class");

        dialog.pack();
    }

    private void getDialogValues(MultiInputDialog dialog) {
        ranges = dialog.getInteger(T2);
        selLayer = dialog.getRasterLayer(CLAYER);

    }

    private static int CLASS_COUNT;;

    private boolean createHistogram2(final PlugInContext context,
            RasterImageLayer selLayer) throws Exception {

        CLASS_COUNT = ranges;
        final double z = 0;
        int i;
        int A;

        double dMin = 0, dMax = 0;
        double Count[];

        Count = new double[CLASS_COUNT + 1];

        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(selLayer, false);
        final int iNX = rstLayer.getLayerGridExtent().getNX();
        final int iNY = rstLayer.getLayerGridExtent().getNY();
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());

        A = 0;

        for (int x = 0; x < iNX; x++) {// cols
            for (int y = 0; y < iNY; y++) {// rows
                final double value = gwrapper.getCellValueAsDouble(x, y, 0);
                if (value != selLayer.getNoDataValue()) {
                    if (A <= 0) {
                        dMin = dMax = z;
                    } else {
                        if (dMin > z) {
                            dMin = z;
                        } else if (dMax < z) {
                            dMax = z;
                        }
                    }
                    A++;
                }
            }
        }
        final double dInterval = (dMax - dMin) / CLASS_COUNT;
        for (int x = 0; x < iNX; x++) {// cols
            for (int y = 0; y < iNY; y++) {// rows
                final double value = gwrapper.getCellValueAsDouble(x, y, 0);
                if (value != selLayer.getNoDataValue()) {
                    i = (int) ((z - dMin) / dInterval);
                    Count[Math.min(i, CLASS_COUNT)]++;
                }
            }
        }

        final ArrayList list = new ArrayList();
        for (i = 0; i < Count.length; i++) {
            final int iCount = (int) (10000 * Count[i] / A);
            for (int j = 0; j < iCount; j++) {
                list.add(new Double(dMin + dInterval * i));
            }
        }

        final double countForHistogram[] = new double[list.size()];
        for (int j = 0; j < list.size(); j++) {
            countForHistogram[j] = ((Double) list.get(j)).doubleValue();
        }
        final int values = ranges;

        double[][] limits2show = new double[2][countForHistogram.length];
        if (countForHistogram.length == 2) {
            limits2show = new double[2][countForHistogram.length * 2];
        }
        for (int j = 0; j < countForHistogram.length; j++) {
            limits2show[0][j] = countForHistogram[j]; // x-axis
            limits2show[1][j] = Math.floor(i / (4.0 * values)); // y-axis,
                                                                // estimate
                                                                // height of
                                                                // "bar"
                                                                // from
                                                                // number of
                                                                // items
            // limits2show[1][j]= 1;
            // -- due to bug in jmathplot add limits twice if only three
            // classes
            // are sought
            if (countForHistogram.length == 2) {
                limits2show[0][countForHistogram.length + j] = countForHistogram[j];
                limits2show[1][countForHistogram.length + j] = Math.floor(i
                        / (4.0 * values));
            }
        }

        final Plot2DPanelOJ plot = new Plot2DPanelOJ();
        plot.addHistogramPlot("", countForHistogram, values);
        plot.addBarPlot("", limits2show);
        // plot.addScatterPlotOJ(sDatapoints, plotdata, fID, context,
        // selLayer);
        // plot.addBarPlot(sClassbreaks, limits2show);
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, "1");
        plot.setAxisLabel(1, "2");
        plot.addLegend("SOUTH");
        plot.setBorder(javax.swing.BorderFactory.createLineBorder(
                java.awt.Color.gray, 1));

        plot.setFixedBounds(0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        AdditionalResults.addAdditionalResult("Histogram", plot);
        return true;

    }

    private double round(double value, int numberOfDigitsAfterDecimalPoint) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(numberOfDigitsAfterDecimalPoint,
                BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
    }

    private Double[] getColumnAsInteger(Object[][] array, int index) {
        final Double[] column = new Double[array.length];

        for (int i = 0; i < column.length; i++) {
            column[i] = round(((Double) array[i][index]).doubleValue(), 0);
        }
        return column;
    }

    public static Double[] getColumnAsInteger1(Object[][] array, int index) {
        final Double[] column = new Double[array.length];
        double sum = 0;
        int io = 0; // Create a separate integer to serve as your array indexer.
        while (io < column.length) {
            sum += ((Double) array[io][index]).doubleValue();
            io++;
        }

        for (int i = 0; i < column.length; i++) {
            column[i] = (((Double) array[i][index]).doubleValue() / sum) * 100;
        }
        return column;
    }

    private Double[] getMinMaxValues(RasterImageLayer selLayer,
            Object[][] array, int index, boolean maxvalue) {
        final Double[] column = new Double[array.length];
        final double interval = round(((Double) array[0][0]).doubleValue(), 2)
                - selLayer.getMetadata().getStats().getMin(0);
        if (!maxvalue) {
            for (int i = 0; i < column.length; i++) {
                column[i] = round(((Double) array[i][index]).doubleValue(), 2)
                        - interval;
            }
        } else {
            for (int i = 0; i < column.length; i++) {
                column[i] = round(((Double) array[i][index]).doubleValue(), 2)
                        + interval;
            }
        }
        return column;
    }

    public static String[] getAbsoluteFrqAsIntArray(Object[][] array, int index) {
        final String[] column = new String[array.length];

        final Locale specialLocale = new Locale("en", "EN");
        final String formatPattern = "###";
        final DecimalFormat nf = (DecimalFormat) NumberFormat
                .getNumberInstance(specialLocale);
        nf.applyPattern(formatPattern);
        final DecimalFormat df = nf;
        for (int i = 0; i < column.length; i++) {
            final double value = ((Double) array[i][index]).doubleValue();
            final String stringValue = df.format(value);
            column[i] = stringValue;
        }
        return column;
    }

    public static String[] getMinMaxValuesArray(RasterImageLayer selLayer,
            Object[][] array, int index, boolean maxvalue) {
        final String[] column = new String[array.length];
        final Locale specialLocale = new Locale("en", "EN");
        final String formatPattern = "###.##";
        final DecimalFormat nf = (DecimalFormat) NumberFormat
                .getNumberInstance(specialLocale);
        nf.applyPattern(formatPattern);

        final double interval = Math
                .round(((Double) array[0][0]).doubleValue() * 100.0)
                / 100.0
                - selLayer.getMetadata().getStats().getMin(0);

        if (!maxvalue) {
            for (int i = 0; i < column.length; i++) {
                final double value = Math.round(((Double) array[i][index])
                        .doubleValue() * 100.0) / 100.0 - interval;
                final String stringValue = nf.format(value);
                column[i] = stringValue;

            }
        } else {
            for (int i = 0; i < column.length; i++) {
                final double value = Math.round(((Double) array[i][index])
                        .doubleValue() * 100.0) / 100.0 + interval;
                final String stringValue = nf.format(value);
                column[i] = stringValue;

            }
        }
        return column;
    }

    private boolean createHistogram(final PlugInContext context,
            RasterImageLayer selLayer) throws Exception {

        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        // [mmichaud 2013-05-25] false : this is a temporary image not a file
        // based image
        rstLayer.create(selLayer, false);
        final double[] data2 = GridRasterWrapper.rasterToArray(rstLayer);
        final Plot2DPanelOJ plot = new Plot2DPanelOJ();
        plot.plotToolBar.remove(5);
        plot.plotToolBar.remove(4);
        plot.plotToolBar.remove(3);
        plot.addHistogramPlot(sHistogram, Color.blue.brighter(), data2,
                selLayer.getMetadata().getStats().getMin(0), selLayer
                        .getMetadata().getStats().getMax(0), ranges);

        // .addHistogramPlot(sHistogram, data2, ranges);
        plot.setEditable(false);
        plot.setNotable(true);
        plot.setName(selLayer.getFilePath());
        plot.setToolTipText(selLayer.getFilePath());
        plot.setFixedBounds(0, selLayer.getMetadata().getStats().getMin(0),
                selLayer.getMetadata().getStats().getMax(0));
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, "Values");
        plot.setAxisLabel(1, "Fequency");
        plot.getAxis(0).setLightLabelFont(default_font);
        plot.getAxis(0).setLabelFont(darkLabelFont);
        plot.getAxis(1).setLightLabelFont(default_font);
        plot.getAxis(1).setLabelFont(darkLabelFont);

        // change axis title position relatively to the base of the plot
        plot.getAxis(0).setLabelPosition(0.5, -0.15);
        // change axis title position relatively to the base of the plot
        plot.getAxis(1).setLabelPosition(-0.15, 0.5);
        // change axis title angle relatively to the base of the plot
        plot.getAxis(1).setLabelAngle(-Math.PI / 2);

        if (box.isSelected()) {
            final JTable table = new JTable();
            table.setName(selLayer.getFilePath());
            table.setFont(table.getFont().deriveFont(Font.PLAIN));
            double[][] dataTableDouble = null;
            Object[][] dataTableObject = null;
            for (final Plot plot2 : plot.getPlots()) {
                dataTableDouble = plot2.getData();
            }
            dataTableObject = plot.plotCanvas.reverseMapedData(dataTableDouble);

            final JTable jTable = new JTable() {

                /**
             * 
             */
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) // Override
                                                                   // the
                                                                   // isCellEditable
                                                                   // method

                {

                    return false;// Table not allowed to edit

                }
            };

            final Object[] minObject = getMinMaxValuesArray(selLayer,
                    dataTableObject, 0, false);

            final Object[] maxObject = getMinMaxValuesArray(selLayer,
                    dataTableObject, 0, true);

            final Object[] absoluteFrequencyObject = getAbsoluteFrqAsIntArray(
                    dataTableObject, 1);

            // Adding class sequence number to the table
            final Integer[] evenNumbers = new Integer[dataTableObject.length];
            Integer count = 1;
            for (int a = 0; a < evenNumbers.length; a++) {
                evenNumbers[a] = count;
                count++;
            }
            final Object[] objs = evenNumbers;
            final DefaultTableModel dtm = (DefaultTableModel) jTable.getModel();

            dtm.addColumn("classes", objs);
            dtm.addColumn("min. value", minObject);
            dtm.addColumn("max. value", maxObject);
            dtm.addColumn("absolute frequency", absoluteFrequencyObject);

            final int lastRow = jTable.getRowCount() - 1;
            final int column = jTable.getColumnCount() - 2;
            dtm.setValueAt(rstLayer.getMaxValue(), lastRow, column);

            // jTable.moveColumn(2, 0);

            // table = new JTable(dataTableObject, columnNames);
            final JScrollPane jScrollPane = new JScrollPane(jTable);

            AdditionalResults.addAdditionalResult(
                    sHistogram + " (" + selLayer.getName() + ") - table",
                    jScrollPane);
        }
        AdditionalResults.addAdditionalResultAndShow(sHistogram + " ("
                + selLayer.getName() + ")", plot);

        return true;
    }

}
