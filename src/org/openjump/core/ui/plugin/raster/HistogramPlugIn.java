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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.math.plot.plotObjects.BaseLabel;
import org.math.plot.plots.Plot;
import org.math.plot.render.AbstractDrawer;
import org.openjump.core.apitools.LayerTools;
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
import com.vividsolutions.jump.workbench.ui.ColorChooserPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
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
    private final String NAME = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn");
    private final String CLAYER = I18N.get("ui.GenericNames.Source-Layer");
    private final String HISTOGRAM_PLOT = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Histogram-Plot");
    private final String HISTOGRAM_OPTIONS = I18N
            .get("com.vividsolutions.jump.workbench.ui.plugin.OptionsPlugIn");
    private final String NUM_CLASS = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Number-of-classes");
    private final String LAYER_STATISTICS = I18N
            .get("com.vividsolutions.jump.workbench.ui.plugin.LayerStatisticsPlugIn");
    private final String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
    private final static String VALUES = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values");
    private final String DESCRIPTION = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.description");
    private final String MAXMINPINS = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.max-min-pins");
    private final String MEDIANPIN = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.median-25-75-pins");
    private final String SHOW_FREQUENCY = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.show-frequency");
    private final String SHOW_STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.show-descriptive-stat");
    private final String SELECT_BAND = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.select-one-band");
    private final String FREQUENCY = I18N
            .get("com.vividsolutions.jump.util.Frequency.frequency");
    private final String CLASSIFICATION_METHOD = I18N
            .get("ui.renderer.style.ColorThemingStylePanel.Classification-Method");
    private final String COLOR = I18N
            .get("ui.renderer.style.ColorThemingTableModel.colour");
    private final String CHOOSE_COLOR = I18N
            .get("ui.ColorChooserPanel.choose-color");
    private final String EXECUTING = I18N
            .get("plugin.AbstractPlugIn.executing");

    private final String ANALISYS_INTERVAL = I18N
            .get("ui.GenericNames.interval-of-data-analysis");
    private final String HIST_AS_LINE = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.show-histogram-as-line");
    private final String UNIQUE_VALUES = I18N
            .get("ui.GenericNames.unique-values");

    private Color color = Color.blue.brighter();
    private static ColorChooserPanel colorChooserPanel = new ColorChooserPanel();
    private static JTextField field;

    private static JTextField minField;
    private static JTextField maxField;

    private final Font darkLabelFont = AbstractDrawer.DEFAULT_FONT;
    private final Font bold_font = new Font("BitStream Vera Sans", Font.BOLD,
            10);
    private final Font big_font = new Font("BitStream Vera Sans", Font.PLAIN,
            14);

    private String layerName;
    private int numIntervals = 100;
    //   private int dimension;
    private double min = 0.0D;
    private double max = 0.0D;
    private static RasterImageLayer rLayer;
    private final Icon ICON = IconLoader.icon("histogramme.png");
    private static String UNIT;
    private static JComboBox<String> comboBox = new JComboBox<String>();
    private JComboBox<RasterImageLayer> layerableComboBox = new JComboBox<RasterImageLayer>();

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

    JCheckBox typeHistogram, tableBox, rasterMaxMinLimitsBox,
            rasterCentralTendencyBox, rasterStatisticsBox, statisticsBox,
            analisysBox;
    JButton chooseElemetsBtn;
    private double[] data;

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
        monitor.report(EXECUTING);
        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(rLayer, true);
        Integer band = 0;
        if (rLayer.getNumBands() > 1) {
            final String[] bands = { "0", "1", "2" };
            final String stringInput = (String) JOptionPane.showInputDialog(
                    JUMPWorkbench.getInstance().getFrame(), SELECT_BAND, NAME,
                    JOptionPane.PLAIN_MESSAGE, null, bands, "0");
            try {
                band = Integer.parseInt(stringInput);
            } catch (final NumberFormatException e) {
                return;
            }
        }
        if (analisysBox.isSelected()) {
            data = GridRasterWrapper.rasterToArray(rstLayer, band, min, max);
        } else {
            data = GridRasterWrapper.rasterToArray(rstLayer, band);
        }
        if (UNIT.equals(UNIQUE_VALUES)) {
            drawHistContinuous(context);
        } else if (UNIT.equals(NUM_CLASS)) {
            drawHistIntervals(context, numIntervals);
        }
    }

    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) {
        if (!context.getLayerNamePanel().selectedNodes(RasterImageLayer.class)
                .isEmpty()) {
            rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(
                    context, RasterImageLayer.class);
        } else {
            rLayer = context.getTask().getLayerManager()
                    .getLayerables(RasterImageLayer.class).get(0);
        }
        final ArrayList<String> srsArray = new ArrayList<String>();

        srsArray.add(UNIQUE_VALUES);//All possible classes of data (StatisticIndices classes computes them
        srsArray.add(NUM_CLASS);//User can choose the number of classes
        final Collection<RasterImageLayer> rlayers = context.getTask()
                .getLayerManager().getLayerables(RasterImageLayer.class);
        dialog.setSideBarImage(new javax.swing.ImageIcon(IconLoader.image(
                "histdisplay.png").getScaledInstance((int) (216.0 * 0.8),
                (int) (159.0 * 0.8), java.awt.Image.SCALE_SMOOTH)));
        dialog.setSideBarDescription(DESCRIPTION);
        dialog.addSubTitle(HISTOGRAM_PLOT);
        layerableComboBox = dialog.addLayerableComboBox(CLAYER, rLayer, "",
                rlayers);
        layerableComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMinMaxValues(e, dialog);
                dialog.repaint();
            }
        });

        analisysBox = dialog.addCheckBox(ANALISYS_INTERVAL, false,
                ANALISYS_INTERVAL);
        analisysBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents1(e, dialog);
                dialog.repaint();
            }
        });
        min = rLayer.getMetadata().getStats().getMin(0);
        max = rLayer.getMetadata().getStats().getMax(0);
        minField = dialog.addDoubleField("min", min, 13, "");
        minField.setEnabled(false);
        minField.setEditable(false);
        maxField = dialog.addDoubleField("max", max, 13, "");
        maxField.setEnabled(false);
        maxField.setEditable(false);

        comboBox = dialog
                .addComboBox(CLASSIFICATION_METHOD, "", srsArray, null);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents(e);
                dialog.repaint();
            }
        });
        field = dialog.addIntegerField(NUM_CLASS, numIntervals, 13, NUM_CLASS);
        field.setEnabled(false);
        field.setEditable(false);

        typeHistogram = dialog.addCheckBox(HIST_AS_LINE, false, HIST_AS_LINE);
        typeHistogram.setEnabled(true);
        dialog.addSubTitle(HISTOGRAM_OPTIONS);
        rasterStatisticsBox = dialog.addCheckBox(LAYER_STATISTICS, false,
                SHOW_STATISTICS);
        rasterMaxMinLimitsBox = dialog.addCheckBox(MAXMINPINS, false,
                MAXMINPINS);
        rasterCentralTendencyBox = dialog.addCheckBox(MEDIANPIN, false,
                MEDIANPIN);
        colorChooserPanel
                .addActionListener(new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        color = colorChooserPanel.getColor();
                    }
                });
        colorChooserPanel.setColor(color);
        colorChooserPanel.setAlpha(255);
        dialog.addRow("CheckColor", new JLabel(COLOR + " (" + HISTOGRAM_PLOT
                + ")"), colorChooserPanel, null, CHOOSE_COLOR);
        dialog.pack();
    }

    private void updateMinMaxValues(ActionEvent evt, MultiInputDialog dialog) {
        final RasterImageLayer rasLayer = (RasterImageLayer) dialog
                .getLayerable(CLAYER);
        minField.setText(rasLayer.getMetadata().getStats().getMin(0) + "");
        maxField.setText(rasLayer.getMetadata().getStats().getMax(0) + "");

    }

    private void updateComponents1(ActionEvent evt, MultiInputDialog dialog) {
        if (analisysBox.isSelected()) {
            minField.setEnabled(true);
            minField.setEditable(true);
            maxField.setEnabled(true);
            maxField.setEditable(true);
        } else {
            minField.setEnabled(false);
            minField.setEditable(false);
            maxField.setEnabled(false);
            maxField.setEditable(false);
        }
    }

    private void updateComponents(ActionEvent evt) {
        switch (comboBox.getSelectedIndex()) {
        case 0:
            typeHistogram.setEnabled(true);
            field.setEnabled(false);
            field.setEditable(false);
            break;
        case 1:
            typeHistogram.setEnabled(false);
            field.setEnabled(true);
            field.setEditable(true);
            break;
        }
    }

    private void getDialogValues(MultiInputDialog dialog) {
        numIntervals = dialog.getInteger(NUM_CLASS);
        rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);
        layerName = rLayer.getName();
        UNIT = dialog.getText(CLASSIFICATION_METHOD);
        min = dialog.getDouble("min");
        max = dialog.getDouble("max");

    }

    private boolean drawHistContinuous(final PlugInContext context)
            throws Exception {

        final StatisticIndices statUtils = new StatisticIndices();
        statUtils.calculateDescriptiveStatistics(data);
        final Map<Double, Integer> datas = statUtils.getCounts();
        final Set<Entry<Double, Integer>> entries = datas.entrySet();
        final double[] X = new double[datas.size()];
        int count = 0;
        final double[][] plotdata = new double[datas.size()][2];

        for (final Map.Entry<?, ?> entry : entries) {
            plotdata[count][0] = (Double) entry.getKey();
            plotdata[count][1] = ((Number) entry.getValue()).doubleValue();
            X[count] = 1;
            count++;
        }
        final Plot2DPanelOJ plot = new Plot2DPanelOJ();

        if (typeHistogram.isSelected()) {
            plot.addLinePlot(HISTOGRAM_PLOT, color, plotdata);
        } else {
            plot.addHistogramPlot(HISTOGRAM_PLOT, color, plotdata, X);
        }
        plot.setFixedBounds(0, statUtils.getMin(), statUtils.getMax());
        plot.setFixedBounds(0, statUtils.getMin(), statUtils.getMax());
        plot.setEditable(false);
        plot.setNotable(true);
        plot.setName(rLayer.getFilePath());
        plot.setToolTipText(rLayer.getFilePath());
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
        addPlotAccessories(statUtils, plot, rLayer);
        final JButton statBtn = new JButton(IconLoader.icon("statistics16.png"));
        statBtn.setToolTipText(SHOW_STATISTICS);
        statBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcStatistics(context, statUtils);
                return;
            }
        });
        final JButton classesBtn = new JButton(IconLoader.icon("Row_16.gif"));
        classesBtn.setToolTipText(SHOW_FREQUENCY);
        classesBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final TableFrame classesFrame = new TableFrame();
                classesFrame.setLayout(new BorderLayout());
                classesFrame.setResizable(true);
                classesFrame.setClosable(true);
                classesFrame.setIconifiable(true);
                classesFrame.setMaximizable(true);
                classesFrame.setPreferredSize(new Dimension(900, 450));
                classesFrame.setSize(900, 450);
                classesFrame.setLayer(JLayeredPane.MODAL_LAYER);
                classesFrame.setTitle(HISTOGRAM_PLOT + " (" + layerName
                        + ") - " + "Classes");
                final JTable jTable = new JTable(toTableModel(statUtils
                        .getCounts()));
                final DefaultTableModel dtm = (DefaultTableModel) jTable
                        .getModel();
                final JTablePanel jTablePanel = new JTablePanel(dtm);
                jTablePanel.getCommandPanel().setVisible(false);
                classesFrame.add(jTablePanel);
                classesFrame.setVisible(true);
                context.getWorkbenchFrame().addInternalFrame(classesFrame,
                        true, true);
            }
        });
        plot.plotToolBar.addSeparator();
        plot.plotToolBar.add(statBtn);
        plot.plotToolBar.add(classesBtn);
        AdditionalResults.addAdditionalResultAndShow(HISTOGRAM_PLOT + " ("
                + layerName + ")", plot);
        return true;
    }

    private boolean drawHistIntervals(final PlugInContext context, int ranges)
            throws Exception {

        final StatisticIndices statUtils = new StatisticIndices();

        statUtils.calculateDescriptiveStatistics(data);

        final Plot2DPanelOJ plot = new Plot2DPanelOJ();

        //  addPlotAccessories(statUtils, plot, rLayer);

        if (typeHistogram.isSelected()) {

            plot.addHistogramPlot(HISTOGRAM_PLOT, color, data,
                    statUtils.getMin(), statUtils.getMax(), ranges);
        } else {
            plot.addHistogramPlot(HISTOGRAM_PLOT, color, data,
                    statUtils.getMin(), statUtils.getMax(), ranges);
        }

        plot.addHistogramPlot(HISTOGRAM_PLOT, color, data, statUtils.getMin(),
                statUtils.getMax(), ranges);

        plot.setFixedBounds(0, statUtils.getMin(), statUtils.getMax());
        plot.setFixedBounds(0, statUtils.getMin(), statUtils.getMax());
        plot.setEditable(false);
        plot.setNotable(true);
        plot.setName(rLayer.getFilePath());
        plot.setToolTipText(rLayer.getFilePath());
        //        plot.setFixedBounds(0, rLayer.getMetadata().getStats().getMin(band),
        //                rLayer.getMetadata().getStats().getMax(band));
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
        addPlotAccessories(statUtils, plot, rLayer);
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

                calcStatistics(context, statUtils);
                final TableFrame freqFrame = new TableFrame();
                freqFrame.setLayout(new BorderLayout());
                freqFrame.setResizable(true);
                freqFrame.setClosable(true);
                freqFrame.setIconifiable(true);
                freqFrame.setMaximizable(true);
                freqFrame.setPreferredSize(new Dimension(900, 450));
                freqFrame.setSize(900, 450);
                freqFrame.setLayer(JLayeredPane.MODAL_LAYER);
                freqFrame.setTitle(HISTOGRAM_PLOT + " (" + layerName + ") - "
                        + FREQUENCY);

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
                jTablePanel.getCommandPanel().setVisible(false);
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
                calcStatistics(context, statUtils);
                return;
            }
        });
        plot.plotToolBar.addSeparator();
        plot.plotToolBar.add(statBtn);

        plot.plotToolBar.add(freqBtn);

        AdditionalResults.addAdditionalResultAndShow(HISTOGRAM_PLOT + " ("
                + layerName + ")", plot);

        return true;
    }

    public static TableModel toTableModel(Map<?, ?> map) {
        final DefaultTableModel model = new DefaultTableModel(
                new Object[] {
                        VALUES,
                        I18N.get("com.vividsolutions.jump.util.Frequency.absolute-frequency"),
                        I18N.get("com.vividsolutions.jump.util.Frequency.cumulative-frequency"),
                        I18N.get("com.vividsolutions.jump.util.Frequency.relative-frequency") },
                0);
        double frqValue = 0;
        double sum = 0;
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final double value = ((Number) entry.getValue()).doubleValue();
            sum += value;
        }

        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final double value = ((Number) entry.getValue()).doubleValue();
            frqValue += value;
            model.addRow(new Object[] { entry.getKey(), entry.getValue(),
                    frqValue, (frqValue / sum) * 100 });
        }
        return model;
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

    public void calcStatistics(PlugInContext context, StatisticIndices statUtils) {

        final DecimalFormat df = new DecimalFormat("##.###");
        final HTMLPanel out = new HTMLPanel();
        out.getRecordPanel().removeAll();
        out.createNewDocument();
        out.setBackground(Color.lightGray);
        out.append("<h");
        out.append("2");
        out.append(" align=\"left\">");
        out.append(HISTOGRAM_PLOT + " (" + layerName + ") - "
                + LAYER_STATISTICS);
        out.append("</h");
        out.append("2");
        out.append("<ul>\n");
        addListElement(out,
                StatisticIndices.COUNT + ": " + df.format(statUtils.getCount()));
        addListElement(
                out,
                StatisticIndices.NUM_CLASSES + ": "
                        + df.format(statUtils.getClasses()));
        addListElement(out,
                StatisticIndices.MIN + ": " + df.format(statUtils.getMin()));
        addListElement(out,
                StatisticIndices.MAX + ": " + df.format(statUtils.getMax()));
        addListElement(out,
                StatisticIndices.MEAN + ": " + df.format(statUtils.getMean()));
        addListElement(
                out,
                StatisticIndices.STANDARD_DEV + ": "
                        + df.format(statUtils.getStdDev()));
        addListElement(
                out,
                StatisticIndices.PERCENTILE_25 + ": "
                        + df.format(statUtils.get25Percentile()));
        addListElement(
                out,
                StatisticIndices.MEDIAN + ": "
                        + df.format(statUtils.getMedian()));
        addListElement(
                out,
                StatisticIndices.PERCENTILE_75 + ": "
                        + df.format(statUtils.get75Percentile()));
        addListElement(out,
                StatisticIndices.RMS + ": " + df.format(statUtils.getRMS()));
        addListElement(
                out,
                StatisticIndices.VARIANCE + ": "
                        + df.format(statUtils.getVariance()));

        addListElement(out, NODATA + ": " + df.format(rLayer.getNoDataValue()));
        addListElement(out,
                StatisticIndices.SUM + ": " + df.format(statUtils.getSum()));
        addListElement(
                out,
                StatisticIndices.COEF_VARIATION + ": "
                        + df.format(statUtils.getCoeffOfVar()));

        addListElement(
                out,
                StatisticIndices.SKEWNESS + ": "
                        + df.format(statUtils.getSkewness()));
        addListElement(
                out,
                StatisticIndices.KURTOSIS + ": "
                        + df.format(statUtils.getKurtosis()));

        //Deactivated as int dimension depends on RasterLayer data (not saved into histogram),
        //not statistics data (saved on histogram component) so after first histogram the value was
        //cumulated from the sequence of analized layers     

        //       addListElement(
        //               out,
        //               NODATACELLS + ": "
        //                       + Integer.toString(dimension - statUtils.getCount()));

        //         addListElement(out,
        //              VALIDCELLS + ": " + Integer.toString(statUtils.getCount()));

        out.append("</ul>\n");

        final HTMLFrame frame = new HTMLFrame();
        frame.setTitle(HISTOGRAM_PLOT + LAYER_STATISTICS);
        frame.add(out);
        frame.setClosable(true);
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setSize(280, 520);
        frame.setVisible(true);

        context.getWorkbenchFrame().addInternalFrame(frame, true, true);

    }

    public void addPlotAccessories(StatisticIndices statUtils,
            Plot2DPanelOJ plot, RasterImageLayer selLayer) {
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
            final double[][] limits2show = new double[2][2];
            limits2show[0][0] = statUtils.getMin(); // x-axis
            //  limits2show[0][1] = 2 * Math.floor(selLayer.getOrigImageHeight()
            //          * selLayer.getOrigImageWidth() / (numIntervals));

            limits2show[0][1] = 2 * Math.floor(selLayer.getOrigImageHeight()
                    * selLayer.getOrigImageWidth() / (numIntervals));
            limits2show[1][0] = statUtils.getMax(); // x-axis
            limits2show[1][1] = limits2show[0][1];
            plot.addBarPlot("limiti1", Color.red, limits2show);
            //   plot.addLabel("max", Color.RED, new double[] { statUtils.getMax(),
            //           limits2show[0][1] * 110 / 100 });
            //   plot.addLabel("min", Color.RED, new double[] { statUtils.getMin(),
            //           limits2show[0][1] * 110 / 100 });
            plot.addLabel("max", Color.RED, new double[] { limits2show[1][0],
                    limits2show[0][1] });
            plot.addLabel("min", Color.RED, new double[] { limits2show[0][0],
                    limits2show[0][1] });

        }
        if (rasterCentralTendencyBox.isSelected()) {
            final double[][] limits2show = new double[3][2];
            limits2show[0][0] = statUtils.getMedian(); // x-axis
            limits2show[0][1] = 2 * Math.floor(selLayer.getOrigImageHeight()
                    * selLayer.getOrigImageWidth() / (numIntervals));
            limits2show[1][0] = statUtils.get25Percentile(); // x-axis
            limits2show[1][1] = limits2show[0][1];
            limits2show[2][0] = statUtils.get75Percentile(); // x-axis
            limits2show[2][1] = limits2show[0][1];
            plot.addBarPlot("limiti", Color.RED, limits2show);

            plot.addLabel("median", Color.RED,
                    new double[] { statUtils.getMedian(),
                            limits2show[0][1] * 110 / 100 });
            plot.addLabel("25%", Color.RED,
                    new double[] { statUtils.get25Percentile(),
                            limits2show[0][1] * 110 / 100 });
            plot.addLabel("75%", Color.RED,
                    new double[] { statUtils.get75Percentile(),
                            limits2show[0][1] * 110 / 100 });

        }

        plot.plotToolBar.remove(5);
        plot.plotToolBar.remove(4);
        plot.plotToolBar.remove(3);
    }

}
