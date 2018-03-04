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
import javax.swing.JTextPane;

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

public class ChangeValueToNoDataPlugIn extends ThreadedBasePlugIn {

    /**
     * 
     * @author Giuseppe Aruta
     * @description This class allows to change an input value to nodata,
     *              inverse operation set nodata cells to the input value
     * @version 01 (Giuseppe Aruta) [2015_02_27] first version
     * @version 02 (Giuseppe Aruta) [2015_03_22] Add output file selection
     * @version 03 (Giuseppe Aruta) [2015_03_25] Add Inverse operation to set
     *          nodata cells to the input value
     * @date 2015_19_5 (Giuseppe Aruta) Correct bug introduced with new
     *       RasterImageLayer.cellvalue Substitute export to .flt file to .asc
     *       file
     * @date 2015_15_11 (Giuseppe Aruta) Improved GUI
     */

    // Language codes: 11
    public static final String PLUGINNAME = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.name");
    private String CHANGE = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.change");
    private String TONODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.tonodata");
    private static String INVERSE = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.Inverse");
    private static String REVERSE_TOOLTIP = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.tooltips");
    private String OUTPUT_FILE = I18N.get("driver.DriverManager.file-to-save")
            + ": ";
    private static String STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.CellStatistics");
    private static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.nodata");
    private static String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.min");
    private static String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.max");

    private Properties properties = null;
    private static String propertiesFile = LoadSextanteRasterImagePlugIn
            .getPropertiesFile();
    NumberFormat cellFormat = null;

    private static ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");

    private boolean reverse = false;

    public ChangeValueToNoDataPlugIn() {

    }

    @Override
    public String getName() {
        return PLUGINNAME;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.processing"));
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

        // Main Panel. Set range source-target no data value
        JPanel secondPanel = new JPanel(new GridBagLayout());
        JTextPane text = new JTextPane();
        text.setOpaque(false);
        text.setText(String.valueOf(rLayer.getNoDataValue()));
        JTextField changing_data = new JTextField();
        changing_data.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE) || (vChar == KeyEvent.VK_DELETE))) {
                    e.consume();
                }
            }
        });
        FormUtils.addRowInGBL(secondPanel, 2, 0, CHANGE, changing_data);
        FormUtils.addRowInGBL(secondPanel, 3, 0, TONODATA, text);

        // Lower panel
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        jPanel2 = new javax.swing.JPanel();
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
        FormUtils.addRowInGBL(jPanel2, 3, 0, OUTPUT_FILE, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel2, 3, 2, jButton_Dir);

        dialog.addRow(jPanel1);
        dialog.addRow(secondPanel);
        dialog.addCheckBox(INVERSE, reverse, REVERSE_TOOLTIP);
        dialog.addRow(jPanel2);

        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return;
        } else {

            String path = jTextField_RasterOut.getText();

            int band = 0;

            File flt_outFile = new File(path.concat(".asc"));
            float olddata = Float.parseFloat(changing_data.getText());
            float newdata = (float) rLayer.getNoDataValue();
            if (dialog.getCheckBox(INVERSE).isSelected())
            // A)Reverse operation: set nodata cells to input value
            {
                saveASC(flt_outFile, context, rLayer, band, newdata, olddata);
            } else {// B) Main operation: change cells with input value to
                    // nodata value
                saveASC(flt_outFile, context, rLayer, band, olddata, newdata);
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

    public static double defaultNoData = -99999.0D;

    public void saveASC(File file, PlugInContext context,
            RasterImageLayer rLayer, int band, float oldnodata, float newnodata)
            throws IOException {
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

            o.println("NODATA_value " + oldnodata);
            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            for (int y = 0; y < ny; y++) {
                StringBuffer b = new StringBuffer();
                for (int x = 0; x < nx; x++) {
                    double value = gwrapper.getCellValueAsDouble(x, y, band);

                    if (Double.isNaN(value)) {
                        value = oldnodata;
                    }
                    if (Math.floor(value) == value) {

                        if (value == newnodata) {
                            b.append((int) oldnodata + " ");

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

    JTextField jTextField_RasterOut = new JTextField();

    private void jButton_RasterOutActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton_RasterOutActionPerformed

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
            // outputpathString = outputpathFile.getPath();
            jTextField_RasterOut.setText(outputPathFile.getPath());
            FileOperations.lastVisitedFolder = outputPathFile;
        }

    }

}