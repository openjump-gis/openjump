/*
* Copyright (C) 2019  Giuseppe Aruta
* 
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
* 
* ******************************************************************
*/
package org.openjump.core.ui.plugin.raster;

import static com.vividsolutions.jump.I18N.get;
import it.betastudio.adbtoolbox.libs.FileOperations;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.algorithms.GenericRasterAlgorithm;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.util.LayerableUtil;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ManageDataPlugIn extends ThreadedBasePlugIn {
    /**
     * 
     * @author Giuseppe Aruta 2019_25_03
     * A comprensive class/plugin to manage data of a single raster layer
     */
    public static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();

    private final String CHANGE_NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeNoDataValuePlugIn.name");
    private final String CHANGE_INTERVAL_TO_NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeRangeValuesToNoDataPlugIn.name");
    private final String FROM = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.from");
    private final String TO = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.to");
    private final String LOWER_VALUE = I18N
            .get("com.vividsolutions.jump.util.Frequency.lower-value");
    private final String UPPER_VALUE = I18N
            .get("com.vividsolutions.jump.util.Frequency.upper-value");
    private final String STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.CellStatistics");
    private final String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.nodata");
    private final String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.min");
    private final String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.max");
    private final String ERROR = I18N.get("ui.GenericNames.Error");
    private final String SELECT_BAND = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.select-one-band");
    private final static String CHECK_FILE = I18N
            .get("plugin.EnableCheckFactory.at-least-one-single-banded-layer-should-exist");

    private final ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");
    private JTextField target_nodata, source_nodata, lv_field, uv_field, nd,
            max, min;
    private JLabel source_NoData_label, target_NoData_label, lv_label,
            uv_label;
    JTextField jTextField_RasterOut = new JTextField();
    private JPanel mainPanel, rangePanel, resetPanel, decimalPanel;
    private RasterImageLayer rLayer;
    private JComboBox<String> comboBox = new JComboBox<String>();
    private JComboBox<RasterImageLayer> layerableComboBox = new JComboBox<RasterImageLayer>();
    private String UNIT;
    private JRadioButton radioMin = new JRadioButton();
    private JRadioButton radioMax = new JRadioButton();
    boolean minval = true;
    boolean maxval = false;
    double min_value, max_value;
    private String path;
    private final SpinnerModel dimensionModel = new SpinnerNumberModel(2, // initial
            // value
            0, // min
            25, // max
            1);
    private JSpinner dimensionSpinner = new JSpinner();
    private Integer dimension;
    Envelope envWanted = new Envelope();
    Envelope fix = new Envelope();

    private final String CLAYER = I18N.get("ui.GenericNames.Source-Layer");
    private final String OUTPUT_FILE = I18N
            .get("driver.DriverManager.file-to-save");
    private final String PROCESSING = I18N
            .get("jump.plugin.edit.NoderPlugIn.processing");

    private final String CHECK = RasterMenuNames.Check_field;
    private final String ACTION = RasterMenuNames.Choose_an_action;
    private final String CHANGE_NODATA_TIP = RasterMenuNames.CHANGE_NODATA_TIP;
    private final String CHANGE_INTERVAL_TO_NODATA_TIP = RasterMenuNames.CHANGE_INTERVAL_TO_NODATA_TIP;
    private final String EXTRACT_INTERVAL_TIP = RasterMenuNames.EXTRACT_INTERVAL_TIP;
    private final String EXTRACT_INTERVAL = RasterMenuNames.EXTRACT_INTERVAL;
    private final String RESET_NODATA_TAG_TIP = RasterMenuNames.RESET_NODATA_TAG_TIP;
    private final String RESET_NODATA_TAG = RasterMenuNames.RESET_NODATA_TAG;
    private final String SET_DECIMAL = RasterMenuNames.SET_DECIMAL;
    private final String SET_DECIMAL_TIP = RasterMenuNames.SET_DECIMAL_TIP;
    private final String RESET_TO_MIN = RasterMenuNames.RESET_TO_MIN;
    private final String RESET_TO_MAX = RasterMenuNames.RESET_TO_MAX;
    private final String NAME = RasterMenuNames.DATA_NAME;

    List<RasterImageLayer> fLayers = new ArrayList<RasterImageLayer>();

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class)).add(new EnableCheck() {
                    @Override
                    public String check(JComponent component) {
                        final List<RasterImageLayer> mLayer = new ArrayList<RasterImageLayer>();
                        final Collection<RasterImageLayer> rlayers = workbenchContext
                                .getLayerManager().getLayerables(
                                        RasterImageLayer.class);
                        for (final RasterImageLayer currentLayer : rlayers) {
                            if (LayerableUtil.isMonoband(currentLayer)) {
                                mLayer.add(currentLayer);
                            }
                        }
                        if (!mLayer.isEmpty()) {
                            return null;
                        }
                        String msg = null;
                        if (mLayer.isEmpty()) {
                            msg = get(CHECK_FILE);
                        }
                        return msg;
                    }
                });
    }

    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) throws IOException {
        dialog.setSideBarDescription(CHANGE_NODATA_TIP);

        if (!context.getLayerNamePanel().selectedNodes(RasterImageLayer.class)
                .isEmpty()) {
            rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(
                    context, RasterImageLayer.class);
        } else {
            rLayer = context.getTask().getLayerManager()
                    .getLayerables(RasterImageLayer.class).get(0);
        }
        min_value = rLayer.getMetadata().getStats().getMin(0);
        max_value = rLayer.getMetadata().getStats().getMax(0);

        fLayers = context.getTask().getLayerManager()
                .getLayerables(RasterImageLayer.class);
        layerableComboBox = dialog.addLayerableComboBox(CLAYER, rLayer, "",
                fLayers);
        layerableComboBox.setSize(200,
                layerableComboBox.getPreferredSize().height);
        layerableComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateValues(e, dialog);
                dialog.pack();
                dialog.repaint();
            }
        });
        final ArrayList<String> srsArray = new ArrayList<String>();
        srsArray.add(CHANGE_NODATA);
        srsArray.add(CHANGE_INTERVAL_TO_NODATA);
        srsArray.add(EXTRACT_INTERVAL);
        srsArray.add(RESET_NODATA_TAG);
        srsArray.add(SET_DECIMAL);
        comboBox = dialog.addComboBox(ACTION, "", srsArray, null);
        comboBox.setSize(200, comboBox.getPreferredSize().height);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGUI(e, dialog);
                updateValues(e, dialog);
                dialog.pack();
                dialog.repaint();
            }
        });
        dialog.addRow(statisticPanel());
        dialog.addRow("ChangeNoData", changeNoDataValuePanel(),
                chengeNoDataCheck, null);
        dialog.addRow("ChangeData", changeRangeToNoDataPanel(),
                chengeDataCheck, null);
        dialog.addRow(resetNoDataPanel());
        dialog.addRow(decimalPanel());
        rangePanel.setEnabled(false);
        rangePanel.setVisible(false);
        resetPanel.setVisible(false);
        decimalPanel.setEnabled(false);
        decimalPanel.setVisible(false);
        final FileNameExtensionFilter filter;
        filter = new FileNameExtensionFilter("TIF", "tif");
        dialog.addRow("Save", createOutputFilePanel(filter), saveCheck, null);

    }

    private final EnableCheck[] chengeDataCheck = new EnableCheck[] {
            new EnableCheck() {
                @Override
                public String check(JComponent component) {
                    return uv_field.getText().isEmpty() ? CHECK.concat(": ")
                            .concat(UPPER_VALUE) : null;
                }
            }, new EnableCheck() {
                @Override
                public String check(JComponent component) {
                    return lv_field.getText().isEmpty() ? CHECK.concat(": ")
                            .concat(LOWER_VALUE) : null;
                }
            }, new EnableCheck() {
                @Override
                public String check(JComponent component) {
                    return Double.parseDouble(lv_field.getText()) > Double
                            .parseDouble(uv_field.getText()) ? ERROR + " :"
                            + LOWER_VALUE + ">" + UPPER_VALUE + "!" : null;
                }
            }

    };

    private final EnableCheck[] chengeNoDataCheck = new EnableCheck[] { new EnableCheck() {
        @Override
        public String check(JComponent component) {
            return target_nodata.getText().isEmpty() ? CHECK.concat(": ")
                    .concat(TO) : null;
        }
    } };

    private final EnableCheck[] saveCheck = new EnableCheck[] { new EnableCheck() {
        @Override
        public String check(JComponent component) {
            return jTextField_RasterOut.getText().isEmpty() ? CHECK
                    .concat(": ").concat(OUTPUT_FILE) : null;
        }
    } };

    private void getDialogValues(MultiInputDialog dialog) {
        rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);
        UNIT = dialog.getText(ACTION);
        if (UNIT.equals(RESET_NODATA_TAG)) {
            minval = dialog.getBoolean(RESET_TO_MIN);
            maxval = dialog.getBoolean(RESET_TO_MAX);
        }
        dimension = (Integer) dimensionSpinner.getValue();
        path = getOutputFilePath();
        final int i = path.lastIndexOf('.');
        if (i > 0) {
            path = path.substring(0, path.length() - path.length() + i);
        }
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), NAME, true);
        setDialogValues(dialog, context);
        if (fLayers.isEmpty()) {
            return false;
        }
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        } else if (dialog.wasOKPressed()) {
            getDialogValues(dialog);
            return true;
        }
        return false;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.report(PROCESSING);
        reportNothingToUndoYet(context);
        final GenericRasterAlgorithm IO = new GenericRasterAlgorithm();
        int band = 0;
        if (rLayer.getNumBands() > 1) {
            final String[] bands = { "0", "1", "2" };
            final String stringInput = (String) JOptionPane.showInputDialog(
                    JUMPWorkbench.getInstance().getFrame(), SELECT_BAND,
                    getName(), JOptionPane.PLAIN_MESSAGE, null, bands, "0");
            try {
                band = Integer.parseInt(stringInput);
            } catch (final NumberFormatException e) {
                return; // The typed text was not an integer
                // band = 0;
            }
        }

        final File outFile = FileUtil.addExtensionIfNone(new File(path), "tif");
        if (UNIT.equals(CHANGE_NODATA)) {
            final double newdata = Double.parseDouble(target_nodata.getText());
            final double olddata = rLayer.getNoDataValue();
            IO.save_ChangeNoData(outFile, rLayer, band, olddata, newdata);
        } else if (UNIT.equals(CHANGE_INTERVAL_TO_NODATA)) {
            final double mindata = Double.parseDouble(lv_field.getText());
            final double maxdata = Double.parseDouble(uv_field.getText());
            IO.save_LimitValidData(outFile, rLayer, band, mindata, maxdata);
        } else if (UNIT.equals(EXTRACT_INTERVAL)) {
            final double mindata = Double.parseDouble(lv_field.getText());
            final double maxdata = Double.parseDouble(uv_field.getText());
            IO.save_ExtractValidData(outFile, rLayer, band, mindata, maxdata);
        } else if (UNIT.equals(RESET_NODATA_TAG)) {
            if (minval) {
                IO.save_ResetNoDataTag(outFile, rLayer, band, rLayer
                        .getMetadata().getStats().getMin(0));
            } else if (maxval) {
                IO.save_ResetNoDataTag(outFile, rLayer, band, rLayer
                        .getMetadata().getStats().getMax(0));
            }
        } else if (UNIT.equals(SET_DECIMAL)) {
            IO.save_ChangeDecimalValues(outFile, rLayer, band, dimension);
        }
        String catName = StandardCategoryNames.WORKING;
        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (final RuntimeException e1) {
        }
        IO.load(outFile, catName);
        return;
    }

    private void updateValues(ActionEvent evt, MultiInputDialog dialog) {
        final RasterImageLayer rasLayer = (RasterImageLayer) dialog
                .getLayerable(CLAYER);
        min.setText(rasLayer.getMetadata().getStats().getMin(0) + "");
        max.setText(rasLayer.getMetadata().getStats().getMax(0) + "");
        nd.setText(rasLayer.getNoDataValue() + "");
        source_nodata.setText(rasLayer.getNoDataValue() + "");
        lv_field.setText(min_value + "");
        uv_field.setText(max_value + "");
    }

    private void updateGUI(ActionEvent evt, MultiInputDialog dialog) {
        switch (comboBox.getSelectedIndex()) {
        case 0:
            dialog.setSideBarDescription(CHANGE_NODATA_TIP);
            mainPanel.setEnabled(true);
            mainPanel.setVisible(true);
            rangePanel.setEnabled(false);
            rangePanel.setVisible(false);
            resetPanel.setEnabled(false);
            resetPanel.setVisible(false);
            decimalPanel.setVisible(false);
            decimalPanel.setEnabled(false);
            break;
        case 1:
            dialog.setSideBarDescription(CHANGE_INTERVAL_TO_NODATA_TIP);
            rangePanel.setEnabled(true);
            rangePanel.setVisible(true);
            rangePanel.setBorder(BorderFactory
                    .createTitledBorder(CHANGE_INTERVAL_TO_NODATA));
            mainPanel.setEnabled(false);
            mainPanel.setVisible(false);
            resetPanel.setEnabled(false);
            resetPanel.setVisible(false);
            decimalPanel.setVisible(false);
            decimalPanel.setEnabled(false);
            break;
        case 2:
            dialog.setSideBarDescription(EXTRACT_INTERVAL_TIP);
            rangePanel.setEnabled(true);
            rangePanel.setVisible(true);
            rangePanel.setBorder(BorderFactory
                    .createTitledBorder(EXTRACT_INTERVAL));
            mainPanel.setEnabled(false);
            mainPanel.setVisible(false);
            resetPanel.setEnabled(false);
            resetPanel.setVisible(false);
            decimalPanel.setEnabled(false);
            decimalPanel.setVisible(false);
            break;
        case 3:
            dialog.setSideBarDescription(RESET_NODATA_TAG_TIP);
            resetPanel.setEnabled(true);
            resetPanel.setVisible(true);
            mainPanel.setEnabled(false);
            mainPanel.setVisible(false);
            rangePanel.setEnabled(false);
            rangePanel.setVisible(false);
            decimalPanel.setEnabled(false);
            decimalPanel.setVisible(false);
            break;
        case 4:
            dialog.setSideBarDescription(SET_DECIMAL_TIP);
            decimalPanel.setEnabled(true);
            decimalPanel.setVisible(true);
            resetPanel.setEnabled(false);
            resetPanel.setVisible(false);
            mainPanel.setEnabled(false);
            mainPanel.setVisible(false);
            rangePanel.setEnabled(false);
            rangePanel.setVisible(false);
            break;

        }
    }

    private JPanel statisticPanel() {
        final JPanel jPanel1 = new JPanel(new GridBagLayout());
        jPanel1.setBorder(BorderFactory.createTitledBorder(STATISTICS));
        nd = new JTextField(String.valueOf(rLayer.getNoDataValue()));
        nd.setEditable(false);
        max = new JTextField(min_value + "");
        max.setEditable(false);
        min = new JTextField(max_value + "");
        min.setEditable(false);
        final JLabel nd_label = new JLabel(NODATA);
        final JLabel min_label = new JLabel(MIN);
        final JLabel max_label = new JLabel(MAX);
        FormUtils.addRowInGBL(jPanel1, 1, 0, nd_label, nd);
        FormUtils.addRowInGBL(jPanel1, 1, 2, min_label, min);
        FormUtils.addRowInGBL(jPanel1, 1, 4, max_label, max);
        return jPanel1;
    }

    private JPanel resetNoDataPanel() {
        resetPanel = new JPanel(new GridBagLayout());
        resetPanel
                .setBorder(BorderFactory.createTitledBorder(RESET_NODATA_TAG));
        radioMin = new JRadioButton(RESET_TO_MIN);
        radioMin.setSelected(true);
        radioMax = new JRadioButton(RESET_TO_MAX);
        final ButtonGroup group = new ButtonGroup();
        group.add(radioMin);
        group.add(radioMax);
        FormUtils.addRowInGBL(resetPanel, 1, 0, radioMin);
        FormUtils.addRowInGBL(resetPanel, 2, 0, radioMax);
        return resetPanel;
    }

    private JPanel decimalPanel() {
        decimalPanel = new JPanel(new GridBagLayout());
        decimalPanel.setBorder(BorderFactory.createTitledBorder(SET_DECIMAL));
        dimensionSpinner = new JSpinner(dimensionModel);
        dimensionSpinner.setSize(20, 10);
        FormUtils.addRowInGBL(decimalPanel, 1, 0, dimensionSpinner);
        return decimalPanel;
    }

    public JPanel changeNoDataValuePanel() {
        mainPanel = new JPanel(new GridBagLayout());
        source_NoData_label = new JLabel(FROM);
        target_NoData_label = new JLabel(TO);
        source_nodata = new JTextField(String.valueOf(rLayer.getNoDataValue()));
        source_nodata.setEditable(false);
        target_nodata = new JTextField(String.valueOf("-99999"));
        source_nodata.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE) || (vChar == KeyEvent.VK_DELETE))) {
                    e.consume();
                }
            }
        });
        target_nodata.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE)
                        || (vChar == KeyEvent.VK_DELETE) || (vChar == KeyEvent.VK_MINUS))) {
                    e.consume();
                }
            }
        });
        mainPanel.setBorder(BorderFactory.createTitledBorder(CHANGE_NODATA));
        FormUtils.addRowInGBL(mainPanel, 2, 0, source_NoData_label,
                source_nodata);
        FormUtils.addRowInGBL(mainPanel, 2, 3, target_NoData_label,
                target_nodata);
        return mainPanel;
    }

    public JPanel changeRangeToNoDataPanel() {
        rangePanel = new JPanel(new GridBagLayout());
        lv_field = new JTextField(String.valueOf(rLayer.getMetadata()
                .getStats().getMin(0)));
        lv_field.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE)
                        || (vChar == KeyEvent.VK_DELETE) || (vChar == KeyEvent.VK_MINUS))) {
                    e.consume();
                }
            }
        });
        uv_field = new JTextField(String.valueOf(rLayer.getMetadata()
                .getStats().getMax(0)));
        uv_field.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                final char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE)
                        || (vChar == KeyEvent.VK_DELETE) || (vChar == KeyEvent.VK_MINUS))) {
                    e.consume();
                }
            }
        });
        lv_label = new JLabel(LOWER_VALUE);
        uv_label = new JLabel(UPPER_VALUE);
        rangePanel.setBorder(BorderFactory
                .createTitledBorder(CHANGE_INTERVAL_TO_NODATA));
        FormUtils.addRowInGBL(rangePanel, 2, 0, lv_label, lv_field);
        FormUtils.addRowInGBL(rangePanel, 2, 2, uv_label, uv_field);
        return rangePanel;
    }

    public JPanel createOutputFilePanel(FileNameExtensionFilter filter) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel = new javax.swing.JPanel();
        final JLabel jLabel3 = new javax.swing.JLabel();
        jTextField_RasterOut = new JTextField();
        final JButton jButton_Dir = new JButton();
        jTextField_RasterOut.setText("");
        jButton_Dir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                File outputPathFile = null;
                final JFileChooser chooser = new GUIUtil.FileChooserWithOverwritePrompting();
                chooser.setDialogTitle(getName());
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                chooser.setSelectedFile(FileOperations.lastVisitedFolder);
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                GUIUtil.removeChoosableFileFilters(chooser);
                chooser.setFileFilter(filter);
                final int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    outputPathFile = FileUtil.removeExtensionIfAny(chooser
                            .getSelectedFile());
                    jTextField_RasterOut.setText(outputPathFile.getPath()
                            .concat(".tif"));
                    FileOperations.lastVisitedFolder = outputPathFile;
                }
            }
        });
        jLabel3.setText(OUTPUT_FILE);
        jTextField_RasterOut.setEditable(false);
        jButton_Dir.setIcon(icon16);
        jTextField_RasterOut.setPreferredSize(new Dimension(250, 20));
        FormUtils.addRowInGBL(jPanel, 3, 0, OUTPUT_FILE, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel, 3, 2, jButton_Dir);
        return jPanel;
    }

    public String getOutputFilePath() {
        return jTextField_RasterOut.getText();
    }

    @Override
    public String getName() {
        return NAME;
    }

}