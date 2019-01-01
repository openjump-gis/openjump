package org.openjump.core.ui.plugin.raster.nodata;

import it.betastudio.adbtoolbox.libs.ExtensionFilter;
import it.betastudio.adbtoolbox.libs.FileOperations;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageIOUtils;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.plugin.layer.pirolraster.LoadSextanteRasterImagePlugIn;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ChangeRangeValuesToNoDataPlugIn extends ThreadedBasePlugIn {
    /**
     * 
     * @author Giuseppe Aruta
     * @date 2015_3_25 (Giuseppe Aruta) This class allows to change a range
     *       allows values to nodata Reverse operation extracts the input range
     *       of value and set the others to nodata The output is a ESRI float
     *       file
     * @date 2015_19_5 (Giuseppe Aruta) Correct bug introduced with new
     *       RasterImageLayer.cellvalue Substitute export to .flt file to .asc
     *       file
     * @date 2015_15_11 (Giuseppe Aruta) Improved GUI
     */

    private Properties properties = null;
    private String byteOrder = "LSBFIRST";
    private static String propertiesFile = LoadSextanteRasterImagePlugIn
            .getPropertiesFile();
    NumberFormat cellFormat = null;

    private boolean reverse = false;

    JTextField jTextField_RasterOut = new JTextField();

    // Language codes: 12

    private static String INVERSE = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.Inverse");
    private static String REVERSE_TOOLTIP = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeRangeValuesToNoDataPlugIn.tooltips");
    private static String CHANGE = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeRangeValuesToNoDataPlugIn.change");
    private static String PLUGINNAME = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeRangeValuesToNoDataPlugIn.name");
    private static String FROM = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.from");
    private static String TO = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.to");
    private String SUBMENU = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.menu");
    private static String OUTPUT_FILE = I18N
            .get("driver.DriverManager.file-to-save") + ": ";

    private static String STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.CellStatistics");
    private static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.nodata");
    private static String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.min");
    private static String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.max");

    private static ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");

    public ChangeRangeValuesToNoDataPlugIn() {

    }

    @Override
    public String getName() {
        return PLUGINNAME;
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add(
                checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class)).add(
                checkFactory
                        .createRasterImageLayerExactlyNBandsMustExistCheck(1));
        return multiEnableCheck;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.processing"));
        reportNothingToUndoYet(context);
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        String nome = getName() + " (" + rLayer.getName() + ")";
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), nome, true);

        // Top panel. Visualize nodata/max/min cell values
        JPanel jPanel1 = new JPanel(new GridBagLayout());
        jPanel1.setBorder(BorderFactory.createTitledBorder(STATISTICS));
        OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(rLayer);
        JTextField nd = new JTextField(String.valueOf(rLayer.getNoDataValue()));
        nd.setEditable(false);
        JTextField max = new JTextField(String.valueOf(rstLayer.getMaxValue()));
        max.setEditable(false);
        JTextField min = new JTextField(String.valueOf(rstLayer.getMinValue()));
        min.setEditable(false);
        JLabel nd_label = new JLabel(NODATA);
        JLabel min_label = new JLabel(MIN);
        JLabel max_label = new JLabel(MAX);
        FormUtils.addRowInGBL(jPanel1, 1, 0, nd_label, nd);
        FormUtils.addRowInGBL(jPanel1, 1, 2, min_label, min);
        FormUtils.addRowInGBL(jPanel1, 1, 4, max_label, max);

        // middle panel: input fields for upper/lower values of the range

        JPanel jPanel2 = new JPanel(new GridBagLayout());
        JTextField min_nodata = new JTextField();
        min_nodata.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE)
                        || (vChar == KeyEvent.VK_DELETE) || (vChar == KeyEvent.VK_MINUS))) {
                    e.consume();
                }
            }
        });
        JTextField max_nodata = new JTextField();
        max_nodata.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE)
                        || (vChar == KeyEvent.VK_DELETE) || (vChar == KeyEvent.VK_MINUS))) {
                    e.consume();
                }
            }
        });
        JLabel max_NoData_label = new JLabel(TO);
        JLabel min_NoData_label = new JLabel(FROM);
        // JCheckBox checkBox = new JCheckBox(REVERSE, false);
        // checkBox.setToolTipText(REVERSE_TOOLTIP);
        jPanel2.setBorder(BorderFactory.createTitledBorder(CHANGE));
        FormUtils.addRowInGBL(jPanel2, 2, 0, min_NoData_label, min_nodata);
        FormUtils.addRowInGBL(jPanel2, 2, 2, max_NoData_label, max_nodata);
        // FormUtils.addRowInGBL(jPanel2, 3, 0, checkBox);

        // Lower panel: choose output raster file
        JPanel jPanel3 = new JPanel(new GridBagLayout());
        jPanel3 = new javax.swing.JPanel();
        JLabel jLabel3 = new javax.swing.JLabel();
        jTextField_RasterOut = new javax.swing.JTextField();
        JButton jButton_Dir = new javax.swing.JButton();
        jTextField_RasterOut.setText("");
        jButton_Dir.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_RasterOutActionPerformed(evt);
            }
        });
        jLabel3.setText(OUTPUT_FILE);
        jTextField_RasterOut.setEditable(true);
        jButton_Dir.setIcon(icon16);
        jTextField_RasterOut.setPreferredSize(new Dimension(250, 20));
        FormUtils.addRowInGBL(jPanel3, 3, 0, OUTPUT_FILE, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel3, 3, 2, jButton_Dir);

        // Build the dialog. Add the panels
        dialog.addRow(jPanel1);
        dialog.addRow(jPanel2);
        dialog.addCheckBox(INVERSE, reverse, REVERSE_TOOLTIP);
        dialog.addRow(jPanel3);
        // Build the dialog. Add a checkbox for a reverse operation (extraxt the
        // range)

        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return;
        } else {
            // Get the path of file
            String path = jTextField_RasterOut.getText();
            // Set the band
            int band = 0;
            // Set the extension of the files FLT/HDR
            File flt_outFile = new File(path.concat(".asc"));
            // Set range (min/max data) and nodata
            float mindata = Float.parseFloat(min_nodata.getText());
            float maxdata = Float.parseFloat(max_nodata.getText());
            float nodata1 = (float) rLayer.getNoDataValue();

            if (dialog.getCheckBox(INVERSE).isSelected())
            // A)Reverse operation: extract the range of cell values
            {
                saveASC_extract(flt_outFile, context, rLayer, band, mindata,
                        maxdata, nodata1);

            }
            // B)Mask the range of cell values
            else {
                saveASC_extract(flt_outFile, context, rLayer, band, mindata,
                        maxdata, nodata1);
            }
            String catName = StandardCategoryNames.WORKING;
            try {
                catName = ((Category) context.getLayerNamePanel()
                        .getSelectedCategories().toArray()[0]).getName();
            } catch (RuntimeException e1) {
            }
            RasterImageIOUtils.loadASC(flt_outFile, context, catName);

        }
        return;
    }

    // This code derives from AdBToolbox 1.7 - Set the output file name
    private void jButton_RasterOutActionPerformed(java.awt.event.ActionEvent evt) {
        File outputPathFile = null;
        JFileChooser chooser = new GUIUtil.FileChooserWithOverwritePrompting();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(FileOperations.lastVisitedFolder);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        ExtensionFilter filter = new ExtensionFilter();
        filter.setDescription("ASCII  grid");
        filter.addExtension("asc");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            outputPathFile = chooser.getSelectedFile();
            jTextField_RasterOut.setText(outputPathFile.getPath());
            FileOperations.lastVisitedFolder = outputPathFile;
        }

    }

    public void saveASC_mask(File file, PlugInContext context,
            RasterImageLayer rLayer, int band, float mindata, float maxdata,
            float nodata) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(file);
            this.cellFormat = NumberFormat.getNumberInstance();
            this.cellFormat.setMaximumFractionDigits(3);
            this.cellFormat.setMinimumFractionDigits(0);
            this.properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                this.properties.load(fis);
                this.properties
                        .getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
                fis.close();
            } catch (FileNotFoundException localFileNotFoundException) {
            } catch (IOException e) {
                context.getWorkbenchFrame().warnUser(GenericNames.ERROR);
            }
            PrintStream o = new PrintStream(out);
            o.println("ncols " + rLayer.getOrigImageWidth());

            o.println("nrows " + rLayer.getOrigImageHeight());

            o.println("xllcorner " + rLayer.getActualImageEnvelope().getMinX());

            o.println("yllcorner " + rLayer.getActualImageEnvelope().getMinY());

            o.println("cellsize "
                    + Double.toString(rstLayer.getLayerCellSize().x));
            o.println("NODATA_value " + nodata);
            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            for (int y = 0; y < ny; y++) {
                StringBuffer b = new StringBuffer();
                for (int x = 0; x < nx; x++) {
                    double value = gwrapper.getCellValueAsDouble(x, y, band);

                    if (Double.isNaN(value)) {
                        value = nodata;
                    }
                    if (Math.floor(value) == value) {

                        if (value >= mindata && value <= maxdata) {
                            b.append((int) nodata + " ");

                        } else {
                            b.append((int) value + " ");

                        }
                    } else {
                        b.append(value + " ");
                    }
                }
                o.println(b);
            }
            o.close();
        } catch (Exception e) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame()
                    .getOutputFrame()
                    .addText(
                            "SaveImageToRasterPlugIn Exception:Export Part of FLT/ASC or modify raster to ASC not yet implemented. Please Use Sextante Plugin");
        } finally {
            if (out != null)
                out.close();
        }
    }

    public void saveASC_extract(File file, PlugInContext context,
            RasterImageLayer rLayer, int band, float mindata, float maxdata,
            float nodata) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(file);
            this.cellFormat = NumberFormat.getNumberInstance();
            this.cellFormat.setMaximumFractionDigits(3);
            this.cellFormat.setMinimumFractionDigits(0);
            this.properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                this.properties.load(fis);
                this.properties
                        .getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
                fis.close();
            } catch (FileNotFoundException localFileNotFoundException) {
            } catch (IOException e) {
                context.getWorkbenchFrame().warnUser(GenericNames.ERROR);
            }
            PrintStream o = new PrintStream(out);
            o.println("ncols " + rLayer.getOrigImageWidth());

            o.println("nrows " + rLayer.getOrigImageHeight());

            o.println("xllcorner " + rLayer.getActualImageEnvelope().getMinX());

            o.println("yllcorner " + rLayer.getActualImageEnvelope().getMinY());

            o.println("cellsize "
                    + Double.toString(rstLayer.getLayerCellSize().x));

            o.println("NODATA_value " + nodata);
            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            for (int y = 0; y < ny; y++) {
                StringBuffer b = new StringBuffer();
                for (int x = 0; x < nx; x++) {
                    double value = gwrapper.getCellValueAsDouble(x, y, band);

                    if (Double.isNaN(value)) {
                        value = nodata;
                    }
                    if (Math.floor(value) == value) {

                        if (value >= mindata && value <= maxdata) {
                            b.append((int) value + " ");

                        } else {
                            b.append((int) nodata + " ");

                        }
                    } else {
                        b.append(value + " ");
                    }
                }
                o.println(b);
            }
            o.close();
        } catch (Exception e) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame()
                    .getOutputFrame()
                    .addText(
                            "SaveImageToRasterPlugIn Exception:Export Part of FLT/ASC or modify raster to ASC not yet implemented. Please Use Sextante Plugin");
        } finally {
            if (out != null)
                out.close();
        }
    }

}