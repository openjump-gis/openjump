package org.openjump.core.ui.plugin.raster.nodata;

import it.betastudio.adbtoolbox.libs.ExtensionFilter;
import it.betastudio.adbtoolbox.libs.FileOperations;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
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

import org.apache.log4j.Logger;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.GridFloat;
import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;
import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.plugin.layer.pirolraster.LoadSextanteRasterImagePlugIn;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class ChangeValueToNoDataPlugIn extends AbstractPlugIn {

    /**
     * 
     * @author Giuseppe Aruta
     * @description This class allows to change an input value to nodata,
     *              inverse operation set nodata cells to the input value
     * @version 01 [2015_02_27] first version
     * @version 02 [2015_03_22] Add output file selection
     * @version 03 [2015_03_25] Add Inverse operation to set nodata cells to the
     *          input value
     */

    // Language codes: 11
    public static final String PLUGINNAME = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.name");
    private String SUBMENU = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.menu");
    private String CHANGE = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.change");
    private String TONODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.tonodata");
    private static String INVERSE = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.Inverse");
    private static String REVERSE_TOOLTIP = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.Tooltips");
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

    private static final Logger LOGGER = Logger
            .getLogger(ChangeNoDataValuePlugIn.class);
    private Properties properties = null;
    private String byteOrder = "LSBFIRST";
    private static String propertiesFile = LoadSextanteRasterImagePlugIn
            .getPropertiesFile();
    NumberFormat cellFormat = null;

    private static ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");

    private boolean reverse = false;

    public ChangeValueToNoDataPlugIn() {

    }

    public String getName() {
        return PLUGINNAME;
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        new FeatureInstaller(workbenchContext);

        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] { MenuNames.RASTER, SUBMENU }, getName(), false,
                null, createEnableCheck(context.getWorkbenchContext()));
    }

    public boolean execute(PlugInContext context) throws Exception {
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
            return false;
        } else {

            String path = jTextField_RasterOut.getText();

            int band = 0;

            File flt_outFile = new File(path.concat(".flt"));
            File hdr_outFile = new File(path.concat(".hdr"));
            float olddata = Float.parseFloat(changing_data.getText());
            float newdata = (float) rLayer.getNoDataValue();
            if (dialog.getCheckBox(INVERSE).isSelected())
            // A)Reverse operation: set nodata cells to input value
            {
                saveFLT(flt_outFile, context, rLayer, band, newdata, olddata);
            } else {// B) Main operation: change cells with input value to
                    // nodata value
                saveFLT(flt_outFile, context, rLayer, band, olddata, newdata);
            }
            saveHDR(hdr_outFile, context, rLayer);
            loadFLT(flt_outFile, context);

        }
        return true;

    }

    private void saveHDR(File outFile, PlugInContext context,
            RasterImageLayer rLayer) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);
            LOGGER.debug(getClass());
            out = new FileOutputStream(outFile);
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

            o.println("xllcorner " + rLayer.getWholeImageEnvelope().getMinX());

            o.println("yllcorner " + rLayer.getWholeImageEnvelope().getMinY());

            o.println("cellsize " + rstLayer.getLayerCellSize());

            String sNoDataVal = Double.toString(rstLayer.getNoDataValue());

            o.println("NODATA_value " + sNoDataVal);
            o.println("byteorder " + byteOrder);
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

    private void saveFLT(File outFile, PlugInContext context,
            RasterImageLayer rLayer, int band, float oldnodata, float newnodata)
            throws IOException {
        FileOutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);
            LOGGER.debug(getClass());
            out = new FileOutputStream(outFile);
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
            FileChannel fileChannelOut = out.getChannel();
            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            ByteBuffer bb = ByteBuffer.allocateDirect(nx * 4);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    float value = gwrapper.getCellValueAsFloat(x, y, band);
                    if (bb.hasRemaining()) {
                        if (value == oldnodata) {
                            bb.putFloat(newnodata);
                        } else {
                            bb.putFloat(value);
                        }
                    } else {
                        x--;
                        // c--;
                        bb.compact();
                        fileChannelOut.write(bb);
                        bb.clear();
                    }
                }
            }
            bb.compact();
            fileChannelOut.write(bb);
            bb.clear();
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

    private void loadFLT(File flt_outFile, PlugInContext context)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        RasterImageIO rasterImageIO = new RasterImageIO();
        Viewport viewport = context.getWorkbenchContext().getLayerViewPanel()
                .getViewport();
        Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                context.getWorkbenchContext(), flt_outFile.getAbsolutePath(),
                null, viewport.getEnvelopeInModelCoordinates(), requestedRes);

        GridFloat gf = new GridFloat(flt_outFile.getAbsolutePath());

        Envelope imageEnvelope = new Envelope(gf.getXllCorner(),
                gf.getXllCorner() + gf.getnCols() * gf.getCellSize(),
                gf.getYllCorner(), gf.getYllCorner() + gf.getnRows()
                        * gf.getCellSize());

        RasterImageLayer ril = new RasterImageLayer(flt_outFile.getName(),
                context.getWorkbenchContext().getLayerManager(),
                flt_outFile.getAbsolutePath(), imageAndMetadata.getImage(),
                imageEnvelope);
        String catName = StandardCategoryNames.WORKING;
        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e1) {
        }
        context.getLayerManager().addLayerable(catName, ril);
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
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(FileOperations.lastVisitedFolder);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        ExtensionFilter filter = new ExtensionFilter();
        filter.addExtension("flt");
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