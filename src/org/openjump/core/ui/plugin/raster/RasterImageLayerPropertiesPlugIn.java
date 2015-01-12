package org.openjump.core.ui.plugin.raster;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.media.jai.PlanarImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Giuseppe Aruta (giuseppe_aruta[AT]yahoo.it)
 * @version 0.1 - 2013_05_27 Simple plugin that allows to view some properties
 *          of Sextante Raster Layer: name, file location, raster dimension (in
 *          cell), raster extension, X cell size, numbers of bands, min-max-mean
 *          of 1st band value (if the raster is monoband)
 * @version 0.2 - 2015_02_01 Advanced plugin. Displays File, Raster and cells
 *          data properties of Sextante Raster Layer and allows to save those
 *          information as TXT file
 */

public class RasterImageLayerPropertiesPlugIn extends AbstractPlugIn implements
        ThreadedPlugIn {

    protected static final String TXTENDING = ".txt";
    private String minVal;
    private String maxVal;
    private String noVal;
    private String varVal;
    private int datatype;
    private String meanVal;
    private String directory;
    private String cellSize;
    private String name_raster;
    private String name;
    private int X;
    private int Y;
    private String area;
    private String width;
    private String height;
    private long size;
    private String sizeMB;
    private String type;
    private String filetype;
    private int colordepth;
    private int numbands; // number bands
    private double variance; // Variance of band0 values
    private String stdvar; // Standard deviation
    private String covar; // Covariance
    private String sum; // Sum of cell values

    // /Language code deriving from previous RasterImageLayerPropertiesPlugIn
    private final static String INFO = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn");
    private final static String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.max");
    private final static String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.min");
    private final static String MEAN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.mean");
    private final static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");

    // New language codes
    private final static String RASTER = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.raster");
    private final static String NAMEFILE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.namefile");
    private final static String STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.statistics");
    private final static String DIRECTORY = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.directory");
    private final static String EXTENT = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.extent");
    private final static String BANDS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands_number");
    private final static String RASTER_SIZE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_raster");
    private final static String CELL_SIZE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_cell");
    private final static String DATATYPE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.datatype");
    private final static String LAYER_NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.layer_name");
    private final static String NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.name");
    private final static String AREA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.area");
    private final static String STD = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.std");
    private final static String CVAR = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cvar");
    private final static String TYPE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.type");
    private final static String DIMENSION = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension");
    private final static String COLORDEPTH = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.colordepth");
    private final static String CELL_NUM = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cellnum");
    private final static String VARIANCE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.variance");
    private final static String EXPORT_TO_TXT = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.export");
    private final static String NODATACELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.nodatacell");
    private final static String VALIDCELLS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.validcells");
    private final static String SUM = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.sum");
    private final static String FILE_SAVED = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final static String LAYER_IN_MEMORY = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.layer_memory");
    private final static String ERROR = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.error");
    private final static String XMIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.xmin");
    private final static String YMIN = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.ymin");
    private final static String XMAX = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.xmax");
    private final static String YMAX = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.ymax");

    private Envelope extent;

    /*
     * @Override public void initialize(PlugInContext context) throws Exception
     * { WorkbenchContext workbenchContext = context.getWorkbenchContext(); new
     * FeatureInstaller(workbenchContext);
     * context.getFeatureInstaller().addMainMenuPlugin(this, new String[] {
     * MenuNames.RASTER }, INFO, false, getIcon(),
     * createEnableCheck(context.getWorkbenchContext())); }
     */

    public static MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        multiEnableCheck.add(checkFactory
                .createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));
        return multiEnableCheck;
    }

    public Icon getIcon() {
        return IconLoader.icon("information_16x16.png");
    }

    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn");
    }

    /*
     * Associate Byte, Megabytes, etc to Raster dimension
     */
    private static final String[] Q = new String[] { "", "KB", "MB", "GB",
            "TB", "PB", "EB" };

    /*
     * Return bytes as string
     */
    public String getAsString(long bytes) {
        for (int i = 6; i > 0; i--) {
            double step = Math.pow(1024, i);
            if (bytes > step)
                return String.format("%3.1f %s", bytes / step, Q[i]);
        }
        return Long.toString(bytes);
    }

    /*
     * Count the number of cells (of a Sextante monoband raster layer) with no
     * data value
     */
    public int nodata(PlugInContext context,
            OpenJUMPSextanteRasterLayer rstLayer) {
        int counter = 0;
        GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        int nx = rstLayer.getLayerGridExtent().getNX();
        int ny = rstLayer.getLayerGridExtent().getNY();
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                double value = gwrapper.getCellValueAsDouble(x, y, 0);
                if (value == rstLayer.getNoDataValue())
                    counter++;
            }
        }
        return counter;
    }

    /*
     * Check the Data type of the DataBuffer storing the pixel data.
     */
    public String dataType(PlugInContext context, RasterImageLayer rLayer) throws IOException {
        Raster r = rLayer.getRasterData(null);
        SampleModel sm = r.getSampleModel();
        datatype = sm.getDataType();
        switch (datatype) {
        case DataBuffer.TYPE_BYTE: {
            type = "byte";
            break;
        }
        case DataBuffer.TYPE_SHORT: {
            type = "short";
            break;
        }
        case DataBuffer.TYPE_USHORT: {
            type = "ushort";
            break;
        }
        case DataBuffer.TYPE_INT: {
            type = "int";
            break;
        }
        case DataBuffer.TYPE_FLOAT: {
            type = "float";
            break;
        }
        case DataBuffer.TYPE_DOUBLE: {
            type = "double";
            break;
        }
        case DataBuffer.TYPE_UNDEFINED: {
            type = "undefined";
            break;
        }
        }
        return type;
    }

    /*
     * Return the extension of the file as String
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toUpperCase();
        }
        return ext;
    }

    /*
     * Enumeration of File extension used in Sextante Raster Layer
     */
    public enum TypeFile {
        ASC, FLT, TIF, TIFF, JPG, JPEG, PNG, GIF, GRD, JP2, BMP
    }

    /*
     * Return type of the Sextante Raster Layer as String
     */
    public String filetype(File file) {
        TypeFile extension1 = TypeFile.valueOf(getExtension(file));
        switch (extension1) {
        case ASC: {
            filetype = "ASC - ESRI ASCII grid";
            break;
        }
        case FLT: {
            filetype = "FLT - ESRI Binary grid";
            break;
        }
        case TIF: {
            filetype = "GEOTIF/TIFF Tagged Image File Format";
            break;
        }
        case TIFF: {
            filetype = "GEOTIF/TIFF Tagged Image File Format";
            break;
        }
        case JPG: {
            filetype = "JPEG/JPG - Joint Photographic Experts Group";
            break;
        }
        case JPEG: {
            filetype = "JPEG/JPG - Joint Photographic Experts Group";
            break;
        }
        case PNG: {
            filetype = "PNG - Portable Network Graphics";
            break;
        }
        case GIF: {
            filetype = "GIF - Graphics Interchange Format";
            break;
        }
        case GRD: {
            filetype = "GRD - Surfer ASCII Grid";
            break;
        }
        case JP2: {
            filetype = "JPEG 2000 - Joint Photographic Experts Group";
            break;
        }
        case BMP: {
            filetype = "BMP - Windows Bitmap";
            break;
        }
        }
        return filetype;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        // -- not used here
        return true;
    }

    public void run(TaskMonitor monitor, final PlugInContext context)
            throws Exception {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), INFO, true);
        extent = rLayer.getWholeImageEnvelope();

        /*
         * Check the source file of selected Raster Image Ex.
         * C:/Document/Image/Test.jpg
         */
        String checkfile = rLayer.getImageFileName();

        String infotext = null;

        /*
         * Overwrite Locale to UK Decimal format ####.##
         */
        Locale locale = new Locale("en", "UK");
        String pattern = "###.########";
        DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);

        /*
         * Create different Dialog if Layer has datasource or not 1. Raster
         * Layer with no Datasource 2. Raster Layer monoband with datasource 3.
         * Raster Layer multiple bands with datasource
         */

        /*
         * 1. Raster Layer with no datasource
         */
        if (checkfile == null) {

            BufferedImage pi = rLayer.getImageForDisplay();
            pi.getWidth();
            int band = pi.getSampleModel().getNumBands();// Number of bands
            name = rLayer.getName();// Name of Layer
            extent = rLayer.getWholeImageEnvelope();// Extent of Layer
            double cellSize = (extent.getMaxX() - extent.getMinX())
                    / pi.getWidth();// Cell size

            infotext = LAYER_NAME + ": " + "\t" + name + "\n" + "\n" + NAMEFILE
                    + "\n" + "\t" + NAME + ": " + LAYER_IN_MEMORY + "\n" + "\t"
                    + TYPE + ": " + "..." + "\n" + "\t" + DIMENSION + ": "
                    + "..." + "\n" + "\t" + DIRECTORY + ": " + "..." + "\n"
                    + "\n" + EXTENT + "\n" + "\t" + XMIN + ": "
                    + df.format(extent.getMinX()) + "\n" + "\t" + YMIN + ": "
                    + df.format(extent.getMinY()) + "\n" + "\t" + XMAX + ": "
                    + df.format(extent.getMaxX()) + "\n" + "\t" + YMAX + ": "
                    + df.format(extent.getMaxY()) + "\n" + "\t" + AREA + ": "
                    + df.format(extent.getHeight() * extent.getWidth()) + "  ("
                    + df.format(extent.getWidth()) + " X "
                    + df.format(extent.getHeight()) + ")" + "\n" + "\t"
                    + CELL_SIZE + ": " + df.format(cellSize) + "\n" + "\t"
                    + CELL_NUM + ": " + (pi.getWidth() * pi.getHeight())
                    / (cellSize * cellSize) + "\n" + "\n" + RASTER + "\n"
                    + "\t" + DATATYPE + ": " + dataType(context, rLayer) + "\n"
                    + "\t" + COLORDEPTH + ": " + colordepth + " bpp" + "\n"
                    + "\t" + RASTER_SIZE + ": "
                    + df.format(rLayer.getOrigImageWidth()) + " X "
                    + df.format(rLayer.getOrigImageHeight()) + " pixel" + "\n"
                    + "\t" + BANDS + ": " + band + "\n";

            JTextArea textArea = new JTextArea(infotext);
            textArea.setEditable(false);
            textArea.setFont(new Font("Verdana", Font.BOLD, 12));
            Border border = BorderFactory.createLineBorder(Color.darkGray);
            textArea.setBorder(BorderFactory.createCompoundBorder(border,
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            JScrollPane scrollPane = new JScrollPane(textArea);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            scrollPane.setPreferredSize(new Dimension(600, 400));

            final String printtext = infotext;
            JButton jButton_Export = new javax.swing.JButton();
            jButton_Export.setText(EXPORT_TO_TXT);
            jButton_Export
                    .addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(
                                java.awt.event.ActionEvent evt) {
                            try {

                                JFileChooser fileChooser = GUIUtil
                                        .createJFileChooserWithOverwritePrompting();
                                fileChooser.setDialogTitle("Save to text file");
                                fileChooser.setFileFilter(GUIUtil
                                        .createFileFilter("text file",
                                                new String[] { "txt" }));
                                fileChooser.getCurrentDirectory();
                                fileChooser.setName(name);
                                int returnVal = fileChooser
                                        .showSaveDialog(context
                                                .getWorkbenchFrame());
                                File file = fileChooser.getSelectedFile();
                                BufferedWriter writer = null;
                                if (returnVal == JFileChooser.APPROVE_OPTION) {
                                    try {
                                        writer = new BufferedWriter(
                                                new FileWriter(file
                                                        .getAbsolutePath()
                                                        + ".txt"));
                                        writer.write(printtext);
                                        writer.close();
                                    } finally {
                                        context.getWorkbenchFrame()
                                                .setStatusMessage(FILE_SAVED);
                                    }
                                }
                            } catch (Exception ex) {

                                JOptionPane.showMessageDialog(null, this,
                                        ERROR, JOptionPane.ERROR_MESSAGE);

                            }
                        }
                    });
            /*
             * Add Scroll pannel with the text area to the dialog
             */
            dialog.addRow(scrollPane);
            /*
             * Create a new panel for the "Save to TXT" button
             */
            JPanel buttonPane = new JPanel();
            /*
             * Add "Save to TXT" button to the new paneò
             */
            FormUtils.addRowInGBL(buttonPane, 1, 0, jButton_Export);
            /*
             * Add the "Save to TXT" panel to the dialog
             */
            dialog.addRow(buttonPane);
            /*
             * MultiImput Dialog parameters
             */
            dialog.pack();
            GUIUtil.centreOnWindow(dialog);
            dialog.setCancelVisible(false);
            dialog.setVisible(true);
        }

        /*
         * The following section is for Layers with datasource
         */

        else {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);
            final File image = new File(rstLayer.getFilename());
            Raster r = rLayer.getRasterData(null);
            SampleModel sm = r.getSampleModel();
            ColorModel cm = PlanarImage.createColorModel(sm);

            /*
             * List of elements displayed in the TeatArea
             */
            minVal = df.format(rstLayer.getMinValue());// Min value of cells
            maxVal = df.format(rstLayer.getMaxValue());// Max value of cells
            numbands = rstLayer.getBandsCount();// Number of bands
            variance = rstLayer.getVariance();// Variance
            stdvar = df.format(Math.sqrt(variance));// Standard deviation
            covar = df.format(variance / rstLayer.getMeanValue());// Covariance
            meanVal = df.format(rstLayer.getMeanValue());// Mean value
            noVal = df.format(rstLayer.getNoDataValue());// No data value
            varVal = df.format(rstLayer.getVariance());// variance

            directory = image.getParent(); // Directory of file
            colordepth = cm.getPixelSize();// Color depth
            cellSize = df.format(rstLayer.getLayerCellSize());// Cell size
            name_raster = rstLayer.getName();// Name of layer
            name = image.getName();// Name of file
            X = rstLayer.getNX(); // Number of columns
            Y = rstLayer.getNY(); // Number of rows
            int cellnumber = X * Y; // Number of cells
            area = df.format(rLayer.getWholeImageEnvelope().getArea()); // Area
            width = df.format(rLayer.getWholeImageEnvelope().getWidth()); // Width
            height = df.format(rLayer.getWholeImageEnvelope().getHeight()); // Height
            size = image.length(); // Size in byte
            sizeMB = getAsString(size); // Size in Mega Bytes
            sum = df.format(rstLayer.getMeanValue()
                    * (X * Y - nodata(context, rstLayer)));// Sum
                                                           // of
                                                           // values

            try {
                /*
                 * 2. Raster Layer monoband with datasource
                 */
                if (numbands == 1) {
                    infotext = LAYER_NAME + ": " + "\t" + name_raster + "\n"
                            + "\n" + NAMEFILE + "\n" + "\t" + NAME + ": "
                            + name + "\n" + "\t" + TYPE + ": "
                            + filetype(image) + "\n" + "\t" + DIMENSION + ": "
                            + sizeMB + " (" + size + " bytes)" + "\n" + "\t"
                            + DIRECTORY + ": " + directory + "\n" + "\n"
                            + EXTENT + "\n" + "\t" + XMIN + ": "
                            + df.format(extent.getMinX()) + "\n" + "\t" + YMIN
                            + ": " + df.format(extent.getMinY()) + "\n" + "\t"
                            + XMAX + ": " + df.format(extent.getMaxX()) + "\n"
                            + "\t" + YMAX + ": " + df.format(extent.getMaxY())
                            + "\n" + "\t" + AREA + ": " + area + " (" + width
                            + " X " + height + ")" + "\n" + "\t" + CELL_SIZE
                            + ": " + cellSize + "\n" + "\t" + CELL_NUM + ": "
                            + cellnumber + "\n" + "\n" + RASTER + "\n" + "\t"
                            + DATATYPE + ": " + dataType(context, rLayer)
                            + "\n" + "\t" + COLORDEPTH + ": " + colordepth
                            + " bpp" + "\n" + "\t" + RASTER_SIZE + ": " + X
                            + " X " + Y + " pixel" + "\n" + "\t" + BANDS + ": "
                            + numbands + "\n" + "\n" + STATISTICS + "\n" + "\t"
                            + MAX + ": " + maxVal + "\n" + "\t" + MIN + ": "
                            + minVal + "\n" + "\t" + MEAN + ": " + meanVal
                            + "\n" + "\t" + SUM + ": " + sum + "\n" + "\t"
                            + NODATA + ": " + noVal + "\n" + "\t" + VARIANCE
                            + ": " + varVal + "\n" + "\t" + CVAR + ": " + covar
                            + "\n" + "\t" + STD + ": " + stdvar + "\n" + "\t"
                            + VALIDCELLS + ": "
                            + (X * Y - nodata(context, rstLayer)) + "\n" + "\t"
                            + NODATACELLS + ": " + nodata(context, rstLayer)
                            + "\n";

                } else {
                    /*
                     * 3. Raster Layer multiple bands with datasource
                     */
                    infotext = LAYER_NAME + ": " + "\t" + name_raster + "\n"
                            + "\n" + NAMEFILE + "\n" + "\t" + NAME + ": "
                            + name + "\n" + "\t" + TYPE + ": "
                            + filetype(image) + "\n" + "\t" + DIMENSION + ": "
                            + sizeMB + " (" + size + " bytes)" + "\n" + "\t"
                            + DIRECTORY + ": " + directory + "\n" + "\n"
                            + EXTENT + "\n" + "\t" + XMIN + ": "
                            + df.format(extent.getMinX()) + "\n" + "\t" + YMIN
                            + ": " + df.format(extent.getMinY()) + "\n" + "\t"
                            + XMAX + ": " + df.format(extent.getMaxX()) + "\n"
                            + "\t" + YMAX + ": " + df.format(extent.getMaxY())
                            + "\n" + "\t" + AREA + ": " + area + " (" + width
                            + " X " + height + ")" + "\n" + "\t" + CELL_SIZE
                            + ": " + cellSize + "\n" + "\t" + CELL_NUM + ": "
                            + cellnumber + "\n" + "\n" + RASTER + "\n" + "\t"
                            + DATATYPE + ": " + dataType(context, rLayer)
                            + "\n" + "\t" + COLORDEPTH + ": " + colordepth
                            + " bpp" + "\n" + "\t" + RASTER_SIZE + ": " + X
                            + " X " + Y + " pixel" + "\n" + "\t" + BANDS + ": "
                            + numbands + "\n";
                }

                JTextArea textArea = new JTextArea(infotext);
                textArea.setEditable(false);
                textArea.setFont(new Font("Verdana", Font.BOLD, 12));
                Border border = BorderFactory.createLineBorder(Color.darkGray);
                textArea.setBorder(BorderFactory.createCompoundBorder(border,
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                JScrollPane scrollPane = new JScrollPane(textArea);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                scrollPane.setPreferredSize(new Dimension(650, 400));

                // J Button to export//
                final String printtext = infotext;
                JButton jButton_Export = dialog.addButton(EXPORT_TO_TXT);

                // JButton jButton_Export = new javax.swing.JButton();
                // jButton_Export.setText("Export to text file");
                jButton_Export.setSize(15, 4);
                jButton_Export
                        .addActionListener(new java.awt.event.ActionListener() {
                            public void actionPerformed(
                                    java.awt.event.ActionEvent evt) {
                                try {

                                    JFileChooser fileChooser = GUIUtil
                                            .createJFileChooserWithOverwritePrompting();
                                    fileChooser.setDialogTitle(EXPORT_TO_TXT);
                                    fileChooser.setFileFilter(GUIUtil
                                            .createFileFilter("text file",
                                                    new String[] { "txt" }));
                                    fileChooser.setCurrentDirectory(image);
                                    fileChooser.getCurrentDirectory();
                                    fileChooser.setName(name);
                                    int returnVal = fileChooser
                                            .showSaveDialog(context
                                                    .getWorkbenchFrame());
                                    File file = fileChooser.getSelectedFile();
                                    BufferedWriter writer = null;
                                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                                        try {
                                            writer = new BufferedWriter(
                                                    new FileWriter(file
                                                            .getAbsolutePath()
                                                            + ".txt"));
                                            writer.write(printtext);
                                            writer.close();
                                        } finally {
                                            context.getWorkbenchFrame()
                                                    .setStatusMessage(
                                                            FILE_SAVED);
                                        }
                                    }
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(null, this,
                                            ERROR, JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        });
                /*
                 * Add Scroll pannel with the text area to the dialog
                 */
                dialog.addRow(scrollPane);
                /*
                 * Create a new panel for the "Save to TXT" button
                 */
                JPanel buttonPane = new JPanel();
                /*
                 * Add "Save to TXT" button to the new paneò
                 */
                FormUtils.addRowInGBL(buttonPane, 1, 0, jButton_Export);
                /*
                 * Add the "Save to TXT" panel to the dialog
                 */
                dialog.addRow(buttonPane);
                /*
                 * MultiImput Dialog parameters
                 */
                dialog.pack();
                GUIUtil.centreOnWindow(dialog);
                dialog.setCancelVisible(false);
                dialog.setVisible(true);
            } catch (NullPointerException e) {
                context.getWorkbenchFrame()
                        .warnUser(
                                I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
                context.getWorkbenchFrame().getOutputFrame()
                        .createNewDocument();
                context.getWorkbenchFrame()
                        .getOutputFrame()
                        .addText(
                                "SaveImageToRasterPlugIn Exception:"
                                        + new Object[] { e.toString() });
                return;
            }
        }
    }

}
