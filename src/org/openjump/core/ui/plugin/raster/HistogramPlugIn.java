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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.math.plot.plotObjects.BaseLabel;
import org.math.plot.plots.Plot;
import org.math.plot.render.AbstractDrawer;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridRasterWrapper;
import org.openjump.core.ui.plot.Plot2DPanelOJ;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StatisticIndices;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.JTablePanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.TableFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 *
 * created on 06.06.2018
 * 
 * @author Giuseppe Aruta
 */
public class HistogramPlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    private final String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");
    private static final String NAME = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn");
    private static final String CLAYER = GenericNames.SELECT_LAYER;
    private static final String HISTOGRAM_PLOT = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Histogram-Plot");
    private static final String HISTOGRAM_OPTIONS = I18N
            .get("com.vividsolutions.jump.workbench.ui.plugin.OptionsPlugIn");
    private final String T2 = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Number-of-classes");
    private static final String LAYER_STATISTICS = I18N
            .get("com.vividsolutions.jump.workbench.ui.plugin.LayerStatisticsPlugIn");
    private final static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
    private final static String NODATACELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.nodatacell");
    private final static String VALIDCELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.validcells");

    private final static String STAT_MAX = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum");
    private final static String STAT_MIN = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum");
    private static final String STAT_MEAN = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.mean");
    private static final String STAT_STD = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.standard-dev");
    private static final String STAT_VAR = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.variance");
    private static final String STAT_MED = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.median");

    private static final String VALUES = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values");

    private static final String STAT_CRF = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.Coefficient-of-variation");
    private static final String STAT_RMS = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.Root-mean-squared");
    private static final String STAT_1QNT = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.25-percentile");
    private static final String STAT_3QNT = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.75-percentile");
    private static final String STAT_SKW = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.Skewness");
    private static final String STAT_KRT = I18N
            .get("com.vividsolutions.jump.util.StatisticIndices.Kurtosis");
    private static final String STAT_TOTSUM = I18N
            .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.sum");

    private static final String DESCRIPTION = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.description");
    private static final String MAXMINPINS = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.max-min-pins");
    private static final String MEDIANPIN = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.median-25-75-pins");
    private static final String SHOW_FREQUENCY = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.show-frequency");
    private static final String SHOW_STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.show-descriptive-stat");
    private static final String SELECT_BAND = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.select-one-band");

    private static final String FREQUENCY = I18N
            .get("com.vividsolutions.jump.util.Frequency.frequency");

    private final Font darkLabelFont = AbstractDrawer.DEFAULT_FONT;
    private final Font bold_font = new Font("BitStream Vera Sans", Font.BOLD,
            10);
    private final Font big_font = new Font("BitStream Vera Sans", Font.PLAIN,
            14);
    private RasterImageLayer selLayer = null;
    private int ranges = 100;

    private final Icon ICON = IconLoader.icon("histogramme.png");

    @Override
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller.getInstance().addMainMenuPlugin(this,
                new String[] { MenuNames.RASTER }, NAME + "...", false, // checkbox
                null, // icon
                check());
    }

    public Icon getIcon() {
        return ICON;
    }

    public static MultiEnableCheck check() {
        final EnableCheckFactory checkFactory = EnableCheckFactory
                .getInstance();
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class));
    }

    JCheckBox tableBox;
    JCheckBox rasterMaxMinLimitsBox;
    JCheckBox rasterCentralTendencyBox;
    JCheckBox rasterStatisticsBox;
    JCheckBox statisticsBox;
    JButton chooseElemetsBtn;

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), NAME, true);
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

        dialog.setSideBarImage(new javax.swing.ImageIcon(IconLoader.image(
                "histdisplay.png").getScaledInstance((int) (216.0 * 0.8),
                (int) (159.0 * 0.8), java.awt.Image.SCALE_SMOOTH)));

        dialog.setSideBarDescription(DESCRIPTION);
        dialog.addSubTitle(HISTOGRAM_PLOT);
        dialog.addLayerableComboBox(CLAYER, context.getLayerManager()
                .getRasterImageLayers().get(0), "", rlayers);

        dialog.addIntegerField(T2, ranges, 6, T2);
        dialog.addSubTitle(HISTOGRAM_OPTIONS);
        rasterStatisticsBox = dialog.addCheckBox(LAYER_STATISTICS, true,
                SHOW_STATISTICS);

        rasterMaxMinLimitsBox = dialog.addCheckBox(MAXMINPINS, false,
                MAXMINPINS);
        rasterCentralTendencyBox = dialog.addCheckBox(MEDIANPIN, false,
                MEDIANPIN);

        dialog.pack();
    }

    private void getDialogValues(MultiInputDialog dialog) {
        ranges = dialog.getInteger(T2);
        selLayer = dialog.getRasterLayer(CLAYER);

    }

    private boolean createHistogram(final PlugInContext context,
            RasterImageLayer selLayer) throws Exception {
        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(selLayer, true);
        Integer band = 0;
        if (selLayer.getNumBands() > 1) {
            final String[] bands = { "0", "1", "2" };
            final String stringInput = (String) JOptionPane.showInputDialog(
                    JUMPWorkbench.getInstance().getFrame(), SELECT_BAND, NAME,
                    JOptionPane.PLAIN_MESSAGE, null, bands, "0");

            try {
                band = Integer.parseInt(stringInput);
            } catch (final NumberFormatException e) {
                return false; // The typed text was not an integer
                // band = 0;
            }
        }
        final double[] data = GridRasterWrapper.rasterToArray(rstLayer, band);
        final StatisticIndices statUtils = new StatisticIndices();

        statUtils.calculateDescriptiveStatistics(data);

        final int nx = rstLayer.getLayerGridExtent().getNX();
        final int ny = rstLayer.getLayerGridExtent().getNY();
        final Plot2DPanelOJ plot = new Plot2DPanelOJ();

        if (rasterStatisticsBox.isSelected()) {
            final DecimalFormat numberFormat = new DecimalFormat("#.0000");
            final BaseLabel title0 = new BaseLabel(LAYER_STATISTICS,
                    Color.BLACK, 1.1, 1.15);
            title0.setFont(big_font);
            final BaseLabel title1 = new BaseLabel(
                    "max: " + statUtils.getMax(), Color.BLACK, 1.1, 1);
            final BaseLabel title2 = new BaseLabel(
                    "min: " + statUtils.getMin(), Color.BLACK, 1.1, 1.05);
            final BaseLabel title3 = new BaseLabel("mean: "
                    + numberFormat.format(statUtils.getMean()), Color.BLACK,
                    1.1, 0.95);
            final BaseLabel title4 = new BaseLabel("std. dev: "
                    + numberFormat.format(statUtils.getStdDev()), Color.BLACK,
                    1.1, 0.9);
            final BaseLabel title6 = new BaseLabel("25%: "
                    + statUtils.get25Percentile(), Color.BLACK, 1.1, 0.85);
            final BaseLabel title5 = new BaseLabel("median: "
                    + statUtils.getMedian(), Color.BLACK, 1.1, 0.8);
            final BaseLabel title7 = new BaseLabel("75%: "
                    + statUtils.get75Percentile(), Color.BLACK, 1.1, 0.75);
            title1.setFont(darkLabelFont);
            plot.addPlotable(title0);
            plot.addPlotable(title1);
            plot.addPlotable(title2);
            plot.addPlotable(title3);
            plot.addPlotable(title4);
            plot.addPlotable(title5);
            plot.addPlotable(title6);
            plot.addPlotable(title7);
        }

        if (rasterMaxMinLimitsBox.isSelected()) {
            final double[][] limits2show = new double[3][2];
            limits2show[0][0] = statUtils.getMin(); // x-axis
            limits2show[0][1] = 2 * Math.floor(selLayer.getOrigImageHeight()
                    * selLayer.getOrigImageWidth() / (ranges));
            limits2show[1][0] = statUtils.getMax(); // x-axis
            limits2show[1][1] = limits2show[0][1];
            plot.addBarPlot("limiti1", Color.red, limits2show);
            plot.addLabel("max", Color.RED, new double[] { statUtils.getMax(),
                    limits2show[0][1] * 110 / 100 });
            plot.addLabel("min", Color.RED, new double[] { statUtils.getMin(),
                    limits2show[0][1] * 110 / 100 });

        }
        if (rasterCentralTendencyBox.isSelected()) {
            final double[][] limits2show = new double[3][2];
            limits2show[0][0] = statUtils.getMedian(); // x-axis
            limits2show[0][1] = 2 * Math.floor(selLayer.getOrigImageHeight()
                    * selLayer.getOrigImageWidth() / (ranges));
            limits2show[1][0] = statUtils.get25Percentile(); // x-axis
            limits2show[1][1] = limits2show[0][1];
            limits2show[2][0] = statUtils.get75Percentile(); // x-axis
            limits2show[2][1] = limits2show[0][1];
            plot.addBarPlot("limiti", Color.GREEN.darker(), limits2show);

            plot.addLabel("median", Color.GREEN.darker(), new double[] {
                    statUtils.getMedian(), limits2show[0][1] * 110 / 100 });
            plot.addLabel("25%", Color.GREEN.darker(),
                    new double[] { statUtils.get25Percentile(),
                            limits2show[0][1] * 110 / 100 });
            plot.addLabel("75%", Color.GREEN.darker(),
                    new double[] { statUtils.get75Percentile(),
                            limits2show[0][1] * 110 / 100 });

        }

        plot.plotToolBar.remove(5);
        plot.plotToolBar.remove(4);
        plot.plotToolBar.remove(3);

        plot.addHistogramPlot(HISTOGRAM_PLOT, Color.blue.brighter(), data,
                rstLayer.getMinValue(), rstLayer.getMaxValue(), ranges);

        plot.setFixedBounds(0, rstLayer.getMinValue(), rstLayer.getMaxValue());
        plot.setFixedBounds(0, rstLayer.getMinValue(), rstLayer.getMaxValue());
        plot.setEditable(false);
        plot.setNotable(true);
        plot.setName(selLayer.getFilePath());
        plot.setToolTipText(selLayer.getFilePath());
        plot.setFixedBounds(0, selLayer.getMetadata().getStats().getMin(0),
                selLayer.getMetadata().getStats().getMax(0));
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, VALUES);
        plot.setAxisLabel(1, FREQUENCY);
        plot.getAxis(0).setLightLabelFont(bold_font);
        plot.getAxis(0).setLabelFont(darkLabelFont);
        plot.getAxis(1).setLightLabelFont(bold_font);
        plot.getAxis(1).setLabelFont(darkLabelFont);
        // change axis title position relatively to the base of the plot
        plot.getAxis(0).setLabelPosition(0.5, -0.15);
        // change axis title position relatively to the base of the plot
        plot.getAxis(1).setLabelPosition(-0.15, 0.5);
        // change axis title angle relatively to the base of the plot
        plot.getAxis(1).setLabelAngle(-Math.PI / 2);

        // Get frequancy classes
        double[][] dataTableDouble = null;
        Object[][] dataTableObject = null;
        for (final Plot plot2 : plot.getPlots()) {
            dataTableDouble = plot2.getData();
        }
        // Frequency section//
        dataTableObject = plot.plotCanvas.reverseMapedData(dataTableDouble);
        final int length = dataTableObject.length;
        final Object[] minObject = plot.getXData_limits(false);

        final Object[] meanObject = plot.getXData();

        final Object[] maxObject = plot.getXData_limits(true);

        final Object[] absoluteFrequencyObject = plot.getYData();

        final Object[] cumulativeFrequencyObject = plot
                .getYData_CumulativeFrequency();

        final Object[] relativeFrequencyObject = plot
                .getYData_RelativeFrequency();

        // Buttons///////
        final JButton freqBtn = new JButton(IconLoader.icon("Row_16.gif"));
        freqBtn.setToolTipText(SHOW_FREQUENCY);
        freqBtn.addActionListener(new ActionListener() {
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
                freqFrame.setTitle(HISTOGRAM_PLOT + " (" + selLayer.getName()
                        + ") - " + FREQUENCY);

                final JTable jTable = new JTable();
                // Adding class sequence number to the table
                final Integer[] numberIntervals = new Integer[length];
                Integer count = 1;
                for (int a = 0; a < numberIntervals.length; a++) {
                    numberIntervals[a] = count;
                    count++;
                }
                final Object[] objs = numberIntervals;
                final DefaultTableModel dtm = (DefaultTableModel) jTable
                        .getModel();
                dtm.addColumn(I18N
                        .get("com.vividsolutions.jump.util.Frequency.classes"),
                        objs);
                dtm.addColumn(
                        I18N.get("com.vividsolutions.jump.util.Frequency.lower-value"),
                        minObject);
                dtm.addColumn(
                        I18N.get("com.vividsolutions.jump.util.Frequency.mean-value"),
                        meanObject);
                dtm.addColumn(
                        I18N.get("com.vividsolutions.jump.util.Frequency.upper-value"),
                        maxObject);
                dtm.addColumn(
                        I18N.get("com.vividsolutions.jump.util.Frequency.absolute-frequency"),
                        absoluteFrequencyObject);
                dtm.addColumn(
                        I18N.get("com.vividsolutions.jump.util.Frequency.cumulative-frequency"),
                        cumulativeFrequencyObject);
                dtm.addColumn(
                        I18N.get("com.vividsolutions.jump.util.Frequency.relative-frequency"),
                        relativeFrequencyObject);
                final JTablePanel jTablePanel = new JTablePanel(dtm);

                freqFrame.add(jTablePanel);

                freqFrame.setVisible(true);

                context.getWorkbenchFrame().addInternalFrame(freqFrame, true,
                        true);

            }
        });

        final JButton statBtn = new JButton(IconLoader.icon("statistics16.png"));
        statBtn.setToolTipText(SHOW_STATISTICS);
        statBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DecimalFormat df = new DecimalFormat("##.###");
                final HTMLPanel out = new HTMLPanel();
                out.getRecordPanel().removeAll();
                out.createNewDocument();
                out.setBackground(Color.lightGray);
                out.append("<h");
                out.append("2");
                out.append(" align=\"left\">");
                out.append(HISTOGRAM_PLOT + " (" + selLayer.getName() + ") - "
                        + LAYER_STATISTICS);
                out.append("</h");
                out.append("2");
                out.append("<ul>\n");
                addListElement(out,
                        STAT_MIN + ": " + df.format(statUtils.getMin()));
                addListElement(out,
                        STAT_MAX + ": " + df.format(statUtils.getMax()));
                addListElement(out,
                        STAT_MEAN + ": " + df.format(statUtils.getMean()));
                addListElement(out,
                        STAT_STD + ": " + df.format(statUtils.getStdDev()));
                addListElement(
                        out,
                        STAT_1QNT + ": "
                                + df.format(statUtils.get25Percentile()));
                addListElement(out,
                        STAT_MED + ": " + df.format(statUtils.getMedian()));
                addListElement(
                        out,
                        STAT_3QNT + ": "
                                + df.format(statUtils.get75Percentile()));
                addListElement(out,
                        STAT_RMS + ": " + df.format(statUtils.getRMS()));
                addListElement(out,
                        STAT_VAR + ": " + df.format(statUtils.getVariance()));

                addListElement(out,
                        NODATA + ": " + df.format(selLayer.getNoDataValue()));
                addListElement(out,
                        STAT_TOTSUM + ": " + df.format(statUtils.getSum()));
                addListElement(out,
                        STAT_CRF + ": " + df.format(statUtils.getCoeffOfVar()));

                addListElement(out,
                        STAT_SKW + ": " + df.format(statUtils.getSkewness()));
                addListElement(out,
                        STAT_KRT + ": " + df.format(statUtils.getKurtosis()));
                addListElement(
                        out,
                        NODATACELLS
                                + ": "
                                + Integer.toString((nx * ny)
                                        - statUtils.getCount()));

                addListElement(
                        out,
                        VALIDCELLS + ": "
                                + Integer.toString(statUtils.getCount()));
                out.append("</ul>\n");

                final HTMLFrame frame = new HTMLFrame();
                frame.setTitle(HISTOGRAM_PLOT + " (" + selLayer.getName()
                        + ") - " + LAYER_STATISTICS);
                frame.add(out);
                frame.setClosable(true);
                frame.setResizable(true);
                frame.setMaximizable(true);
                frame.setSize(280, 520);
                frame.setVisible(true);

                context.getWorkbenchFrame().addInternalFrame(frame, true, true);

                return;
            }
        });
        plot.plotToolBar.addSeparator();
        plot.plotToolBar.add(statBtn);
        plot.plotToolBar.add(freqBtn);

        AdditionalResults.addAdditionalResultAndShow(HISTOGRAM_PLOT + " ("
                + selLayer.getName() + ")", plot);

        return true;
    }

    public void addListElement(HTMLPanel out, final String sText) {

        out.append("<li>");
        out.append("<font face=\"" + darkLabelFont + "\">" + sText);
        out.append("</li>\n");

    }

    protected void saved(File file) {
        JUMPWorkbench.getInstance().getFrame()
                .setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved(File file) {
        JOptionPane.showMessageDialog(null,
                SCouldNotSave + ": " + file.getName(), I18N.get(NAME),
                JOptionPane.WARNING_MESSAGE);
    }

}
