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
//import org.openjump.core.ui.plugin.layer.pirolraster.LoadSextanteRasterImagePlugIn;
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

/**
 *
 * @author Giuseppe Aruta
 * date 2015_3_25 (Giuseppe Aruta) This class allows to change nodata value
 *       of a single band file The output is a ESRI float file
 * date 2015_19_5 (Giuseppe Aruta) Correct bug introduced with new
 *       RasterImageLayer.cellvalue Substitute export to .flt file to .asc
 *       file
 * date 2015_15_11 (Giuseppe Aruta) Improved GUI
 */
public class ChangeNoDataValuePlugIn extends ThreadedBasePlugIn {

    public static final String PLUGINNAME = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.ChangeNoDataValuePlugIn.name");
    private String OUTPUT_FILE = I18N.getInstance().get("driver.DriverManager.file-to-save")
            + ": ";
    private static String FROM = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.from");
    private static String TO = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.to");
    private static String STATISTICS = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.CellStatistics");
    private static String NODATA = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.nodata");
    private static String MIN = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.min");
    private static String MAX = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.max");

    private Properties properties = null;
 //   private static String propertiesFile = LoadSextanteRasterImagePlugIn
 //           .getPropertiesFile();
    private static String propertiesFile = "path";
    NumberFormat cellFormat = null;
    private static ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");

    public ChangeNoDataValuePlugIn() {

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
        monitor.report(I18N.getInstance().get("jump.plugin.edit.NoderPlugIn.processing"));
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

        // Main Panel. Set range source-target no data value
        JPanel mainPanel = new JPanel(new GridBagLayout());
        JLabel source_NoData_label = new JLabel(FROM);
        JLabel target_NoData_label = new JLabel(TO);
        JTextField source_nodata = new JTextField(String.valueOf(rLayer
                .getNoDataValue()));
        source_nodata.setEditable(false);
        JTextField target_nodata = new JTextField("-99999");
        source_nodata.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char vChar = e.getKeyChar();
                if (!(Character.isDigit(vChar) || (vChar == KeyEvent.VK_PERIOD)
                        || (vChar == KeyEvent.VK_BACK_SPACE) || (vChar == KeyEvent.VK_DELETE))) {
                    e.consume();
                }
            }
        });
        target_nodata.addKeyListener(new java.awt.event.KeyAdapter() {
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
        FormUtils.addRowInGBL(mainPanel, 2, 0, source_NoData_label,
                source_nodata);
        FormUtils.addRowInGBL(mainPanel, 2, 3, target_NoData_label,
                target_nodata);

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
        // FormUtils.addRowInGBL(jPanel2, 3, 0, jLabel3);
        FormUtils.addRowInGBL(jPanel2, 3, 0, OUTPUT_FILE, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel2, 3, 2, jButton_Dir);

        dialog.addRow(jPanel1);
        dialog.addRow(mainPanel);
        dialog.addRow(jPanel2);

        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return;
        }

        else {

            String path = jTextField_RasterOut.getText();

            int band = 0;

            File flt_outFile = new File(path.concat(".asc"));

            float newdata = Float.parseFloat(target_nodata.getText());
            float olddata = (float) rLayer.getNoDataValue();
            saveASC(flt_outFile, context, rLayer, band, olddata, newdata);
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
                        .getProperty("path");
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

            o.println("cellsize " + rstLayer.getLayerCellSize().x);

            o.println("NODATA_value " + newnodata);
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

                        if (value == oldnodata) {
                            b.append((int) newnodata + " ");

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
                            I18N.getInstance().get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
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