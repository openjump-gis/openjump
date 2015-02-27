package org.openjump.core.ui.plugin.raster.nodata;

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

import javax.swing.JOptionPane;
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
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class ChangeValueToNoDataPlugIn extends AbstractPlugIn {
    public static final String PLUGINNAME = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.name");
    private String SUBMENU = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.menu");
    private static final Logger LOGGER = Logger
            .getLogger(ChangeNoDataValuePlugIn.class);
    private Properties properties = null;
    private String byteOrder = "LSBFIRST";
    private static String propertiesFile = LoadSextanteRasterImagePlugIn
            .getPropertiesFile();
    NumberFormat cellFormat = null;

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

        FormUtils
                .addRowInGBL(
                        secondPanel,
                        2,
                        0,
                        I18N.get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.change"),
                        changing_data);
        FormUtils
                .addRowInGBL(
                        secondPanel,
                        3,
                        0,
                        I18N.get("org.openjump.core.ui.plugin.raster.nodata.ChangeValueToNoDataPlugIn.tonodata"),
                        text);
        int option = JOptionPane.showConfirmDialog(null, secondPanel,
                PLUGINNAME, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        switch (option) {

        case JOptionPane.CANCEL_OPTION:

            break;

        case JOptionPane.OK_OPTION:

            String redName = context.getLayerManager().uniqueLayerName(
                    rLayer.getName());
            int boxToInt = 0;

            File flt_outFile = new File(System.getProperty("java.io.tmpdir")
                    .concat(File.separator).concat(redName).concat(".flt"));
            File hdr_outFile = new File(System.getProperty("java.io.tmpdir")
                    .concat(File.separator).concat(redName).concat(".hdr"));
            float olddata = Float.parseFloat(changing_data.getText());
            float newdata = (float) rLayer.getNoDataValue();
            saveFLT(flt_outFile, context, rLayer, boxToInt, olddata, newdata);
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
        String catName = "Temp";// StandardCategoryNames.RESULT;
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

}