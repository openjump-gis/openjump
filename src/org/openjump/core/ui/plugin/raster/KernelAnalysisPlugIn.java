package org.openjump.core.ui.plugin.raster;

import it.betastudio.adbtoolbox.libs.FileOperations;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.algorithms.KernelAlgorithm;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class KernelAnalysisPlugIn extends ThreadedBasePlugIn {

    public static String dimension = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension");

    private final String CLAYER = I18N.get("ui.GenericNames.Source-Layer");
    private final String OUTPUT_FILE = I18N
            .get("driver.DriverManager.file-to-save");
    private final String CHECK = I18N.get("ui.GenericNames.chech-field");
    private final String KernelAnalysis = I18N
            .get("ui.plugin.raster.KernelAnalysisPlugIn.Name");
    private final String defaultKernels = I18N
            .get("ui.plugin.raster.KernelAnalysisPlugIn.default-kernels");
    private final String PROCESSING = I18N
            .get("jump.plugin.edit.NoderPlugIn.processing");

    private JComboBox<RasterImageLayer> layerableComboBox = new JComboBox<RasterImageLayer>();
    private RasterImageLayer rLayer;
    private final ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");
    List<RasterImageLayer> fLayers = new ArrayList<RasterImageLayer>();
    private JComboBox<String> kernelComboBox = new JComboBox<String>();

    JTextField jTextField_RasterOut = new JTextField();

    private JScrollPane jScrollPane1;
    private JTable jTable;
    private DefaultTableModel dtm = null;
    FileNameExtensionFilter filter;
    private String path;
    private String nameKernel;

    public float[] array1d;

    public float[][] array2d;

    final KernelAlgorithm fil = new KernelAlgorithm();
    String hmKey = new String();

    LinkedHashMap<String, float[]> Map = new LinkedHashMap<String, float[]>();
    LinkedHashMap<String, String> Map2 = new LinkedHashMap<String, String>();

    @Override
    public String getName() {
        return KernelAnalysis;
    }

    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) throws IOException {
        dialog.setSideBarDescription(KernelAnalysis);
        if (!context.getLayerNamePanel().selectedNodes(RasterImageLayer.class)
                .isEmpty()) {
            rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(
                    context, RasterImageLayer.class);
        } else {
            rLayer = context.getTask().getLayerManager()
                    .getLayerables(RasterImageLayer.class).get(0);
        }
        fLayers = context.getTask().getLayerManager()
                .getLayerables(RasterImageLayer.class);
        layerableComboBox = dialog.addLayerableComboBox(CLAYER, rLayer, "",
                fLayers);
        layerableComboBox.setSize(200,
                layerableComboBox.getPreferredSize().height);
        final ArrayList<String> srsArray = new ArrayList<String>();
        Map = fil.createDataMap();
        Map2 = fil.createTextMap();
        final Iterator<Map.Entry<String, float[]>> itr = Map.entrySet()
                .iterator();
        while (itr.hasNext()) {
            srsArray.add(itr.next().getKey());
        }
        kernelComboBox = dialog.addComboBox(defaultKernels, srsArray.get(0),
                srsArray, null);
        kernelComboBox.setSize(200, kernelComboBox.getPreferredSize().height);
        kernelComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fixTable(dialog);
                jTable.setVisible(true);
                //  kernelComboBoxActionPerformed(evt);
            }
        });
        jTable = new JTable();
        jScrollPane1 = new JScrollPane();
        jTable.setModel(new DefaultTableModel(new Object[][] {
                { null, null, null, null }, { null, null, null, null },
                { null, null, null, null }, { null, null, null, null } },
                new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        GUIUtil.chooseGoodColumnWidths(jTable);
        jScrollPane1.setViewportView(jTable);
        dialog.addRow("Table", jScrollPane1, fillCheck, null);
        filter = new FileNameExtensionFilter("TIFF", "tif");
        dialog.addRow("Save", createOutputFilePanel(filter), saveCheck, null);
        fixComponents(dialog);
    }

    private void fixComponents(MultiInputDialog dialog) {
        final String test = fil.S_gradientEast;
        array1d = Map.get(test);
        final String description = Map2.get(test);
        final Double dim = Math.sqrt(array1d.length);
        final int val = dim.intValue();
        array2d = new float[val][val];

        for (int i = 0; i < val; i++) {
            System.arraycopy(array1d, (i * val), array2d[i], 0, val);
        }

        final int columns = array2d[0].length;
        dtm = new DefaultTableModel(0, columns) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Class getColumnClass(int column) {
                return Float.class;
            }
        };

        for (final float[] rowData : array2d) {
            final Vector<Object> row = new Vector<Object>(columns);
            for (final float element : rowData) {
                row.addElement(new Float(element));
            }

            dtm.addRow(row);
        }
        path = getOutputFilePath();
        final int it = path.lastIndexOf('.');
        if (it > 0) {
            path = path.substring(0, path.length() - path.length() + it);
        }
        dialog.setSideBarDescription(description);
        jTable.setModel(dtm);
    }

    private void fixTable(MultiInputDialog dialog) {
        final String test = dialog.getText(defaultKernels);//dialog.getText(defaultKernels);
        array1d = Map.get(test);
        final String description = Map2.get(test);
        final Double dim = Math.sqrt(array1d.length);
        final int val = dim.intValue();
        array2d = new float[val][val];

        for (int i = 0; i < val; i++) {
            System.arraycopy(array1d, (i * val), array2d[i], 0, val);
        }

        final int columns = array2d[0].length;
        dtm = new DefaultTableModel(0, columns) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Class getColumnClass(int column) {
                return Float.class; // number will be displayed right aligned
            }
        };

        for (final float[] rowData : array2d) {
            final Vector<Object> row = new Vector<Object>(columns);
            for (final float element : rowData) {
                row.addElement(new Float(element));
            }

            dtm.addRow(row);
        }
        path = getOutputFilePath();
        final int it = path.lastIndexOf('.');
        if (it > 0) {
            path = path.substring(0, path.length() - path.length() + it);
        }
        dialog.setSideBarDescription(description);

        jTable.setModel(dtm);
    }

    public float[][] getTableData(JTable table) {
        final TableModel dtm = table.getModel();
        final int nRow = dtm.getRowCount(), nCol = dtm.getColumnCount();
        final float[][] tableData = new float[nRow][nCol];
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                tableData[i][j] = (float) dtm.getValueAt(i, j);
            }
        }
        return tableData;
    }

    private void getDialogValues(MultiInputDialog dialog) {
        rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);

        nameKernel = dialog.getText(defaultKernels);//dialog.getText(defaultKernels);

        final float[][] fl2d = getTableData(jTable);

        final int rows = fl2d.length, cols = fl2d[0].length;
        final float[] mono = new float[(rows * cols)];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(fl2d[i], 0, mono, (i * cols), cols);
        }

        array1d = mono;//Map.get(nameKernel);
        path = getOutputFilePath();
        final int it = path.lastIndexOf('.');
        if (it > 0) {
            path = path.substring(0, path.length() - path.length() + it);
        }
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), KernelAnalysis, true);
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

        final File outFile = FileUtil.addExtensionIfNone(new File(path), "tif");
        fil.filterRaster(outFile, rLayer, array1d);
        String catName = KernelAnalysis;
        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (final RuntimeException e1) {
        }
        fil.load(outFile, nameKernel, catName);

        return;
    }

    private final EnableCheck[] saveCheck = new EnableCheck[] { new EnableCheck() {
        @Override
        public String check(JComponent component) {
            return jTextField_RasterOut.getText().isEmpty() ? CHECK
                    .concat(OUTPUT_FILE) : null;
        }
    } };

    private final EnableCheck[] fillCheck = new EnableCheck[] { new EnableCheck() {
        @Override
        public String check(JComponent component) {

            final int nTabRows = jTable.getRowCount();
            final int nTabCols = jTable.getColumnCount();

            for (int r = 0; r < nTabRows; r++) {
                for (int c = 0; c < nTabCols; c++) {

                    return jTable.getValueAt(r, c).equals("")
                            || jTable.getValueAt(r, c).equals(null) ||

                            !isNumeric(jTable.getValueAt(r, c).toString())

                    ? CHECK.concat("Table") : null;

                }
            }
            return CHECK;

        }
    } };

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?"); //match a number with optional '-' and decimal.
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
        jTextField_RasterOut.setEditable(true);
        jButton_Dir.setIcon(icon16);
        jTextField_RasterOut.setPreferredSize(new Dimension(250, 20));
        FormUtils.addRowInGBL(jPanel, 3, 0, OUTPUT_FILE, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel, 3, 2, jButton_Dir);
        return jPanel;
    }

    public String getOutputFilePath() {
        return jTextField_RasterOut.getText();
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

}
