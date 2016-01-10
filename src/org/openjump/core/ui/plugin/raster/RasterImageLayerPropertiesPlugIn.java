package org.openjump.core.ui.plugin.raster;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.media.jai.PlanarImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;


import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * @author Giuseppe Aruta (giuseppe_aruta[AT]yahoo.it)
 * @version 0.1 - 2013_05_27 (Giuseppe Aruta) Simple plugin that allows to view
 *          some properties of Sextante Raster Layer: name, file location,
 *          raster dimension (in cell), raster extension, X cell size, numbers
 *          of bands, min-max-mean of 1st band value (if the raster is monoband)
 * @version 0.2 - 2015_01_02 (Giuseppe Aruta) Advanced plugin. Displays File,
 *          Raster and cells data properties of Sextante Raster Layer and allows
 *          to save those information as TXT file
 * @version 0.3 - 2015_01_31. (Giuseppe Aruta) Used HTML instead of TXT frame.
 *          Info can be saved as HTML file
 * @version 0.4 - 2015_03_27. (Giuseppe Aruta) Added Raster Layer statistics tab
 *          with several info about cell values
 */

public class RasterImageLayerPropertiesPlugIn extends AbstractPlugIn {

    protected static final String TXTENDING = ".txt";
    private String minVal;
    private String maxVal;
    private String noVal;
    private String varVal;
    private int datatype;
    private String meanVal;
    private String directory;
    private String cellSizeX;
    private String cellSizeY;
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

    private final static String CANCEL = I18N.get("ui.OKCancelPanel.cancel");
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
    private final static String ENV = I18N
            .get("ui.plugin.LayerStatisticsPlugIn.envelope");
    private final static String DIRECTORY = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.directory");
    private final static String EXTENT = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.extent");
    private final static String BANDS = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands_number");
    private final static String BAND = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.band");
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
    private final static String UNSPECIFIED = I18N
            .get("coordsys.CoordinateSystem.unspecified");

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

    public static final ImageIcon ICON_STAT = IconLoader
            .icon("statistics16.png");

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
    public String dataType(PlugInContext context, RasterImageLayer rLayer)
            throws IOException {
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
        ASC, TXT, FLT, TIF, TIFF, JPG, JPEG, PNG, GIF, GRD, JP2, BMP
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
        case TXT: {
            filetype = "TXT - ESRI ASCII grid";
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

    public String InfoText(PlugInContext context, RasterImageLayer rLayer)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {
        String infotext = null;
        extent = rLayer.getWholeImageEnvelope();
        /*
         * Check the source file of selected Raster Image Ex.
         * C:/Document/Image/Test.jpg
         */
        String checkfile = rLayer.getImageFileName();
        BufferedImage pi = null;
        pi = rLayer.getImageForDisplay();
        pi.getWidth();
        int band = pi.getSampleModel().getNumBands();// Number of bands

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

            name = rLayer.getName();// Name of Layer
            extent = rLayer.getWholeImageEnvelope();// Extent of Layer
            double cellSizeX = (extent.getMaxX() - extent.getMinX())
                    / pi.getWidth();// Cell size
            double cellSizeY = (extent.getMaxY() - extent.getMinY())
                    / pi.getHeight();// Cell size
            
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
                    + CELL_SIZE + ": " + df.format(cellSizeX) + ", " + df.format(cellSizeY) + "\n" + "\t"
                    + CELL_NUM + ": " + (pi.getWidth() * pi.getHeight())
                    / (cellSizeX * cellSizeY) + "\n" + "\n" + RASTER + "\n"
                    + "\t" + DATATYPE + ": " + dataType(context, rLayer) + "\n"
                    + "\t" + COLORDEPTH + ": " + colordepth + " bpp" + "\n"
                    + "\t" + RASTER_SIZE + ": "
                    + df.format(rLayer.getOrigImageWidth()) + " X "
                    + df.format(rLayer.getOrigImageHeight()) + " pixel" + "\n"
                    + "\t" + BANDS + ": " + band + "\n";
        } else {
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
            cellSizeX = df.format(rstLayer.getLayerCellSize().x);// Cell size
            cellSizeY = df.format(rstLayer.getLayerCellSize().y);// Cell size
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

            infotext = "<HTML><BODY>";
            infotext += "<DIV style=\"width: 500px; text-justification: justify;\">";

            infotext += "<table border='0.1'>";
            infotext += "<tr><td bgcolor=#CCCCCC><b>" + LAYER_NAME
                    + "</b> </td><td>" + name_raster + "</td></tr>";
            infotext += "</table>";

            // infotext += "<b>" + NAMEFILE + "</b>" + "<br>";
            infotext += "<table border='0.1'>";
            infotext += "<tr><td bgcolor=#CCCCCC><b>" + NAMEFILE + "  "
                    + "</b></td><td><b>" + NAME + "</b></td><td>" + name
                    + "</td></tr>";
            // infotext += "<tr> <td></td> <td><b>" + NAME + "</b></td><td>"
            // + name + "</td></tr>";
            infotext += "<tr> <td></td> <td<b>" + TYPE + "</b></td><td>"
                    + filetype(image) + "</td></tr>";
            infotext += "<tr> <td></td> <td><b> " + DIMENSION + "</b></td><td>"
                    + sizeMB + " (" + size + " bytes)" + "</td></tr>";
            infotext += "<tr> <td></td> <td><b>" + DIRECTORY + "</b></td><td>"
                    + directory + "</td></tr>";
            infotext += "</table>";

            // infotext += "<br>";
            // infotext += "<b>" + RASTER + "</b>" + "<br>";
            infotext += "<table border='0.1'>";
            infotext += "<tr><td bgcolor=#CCCCCC><b>" + RASTER
                    + "</b></td><td><b>" + BANDS + "</b></td><td>" + numbands
                    + "</td></tr>";
            infotext += "<tr><td></td> <td><b>" + DATATYPE + "</b></td><td>"
                    + dataType(context, rLayer) + "</td></tr>";
            infotext += "<tr><td></td> <td><b>" + COLORDEPTH + "</b></td><td>"
                    + colordepth + "</td></tr>";
            infotext += "<tr><td></td> <td><b>" + RASTER_SIZE + "</b></td><td>"
                    + X + " X " + Y + " pixel" + "</td></tr>";
            infotext += "</table>";

            // infotext += "<br>";

            infotext += "<table border='0.1'>";
            infotext += "<tr><td bgcolor=#CCCCCC><b>" + EXTENT
                    + "</b></td><td><b>" + XMIN + "</b></td><td>"
                    + df.format(extent.getMinX()) + "</td><td>";
            infotext += "<tr><td></td> <td><b>" + XMAX + "</b></td><td>"
                    + df.format(extent.getMaxX()) + "</td><td>";
            infotext += "<tr><td></td> <td><b>" + YMIN + "</b></td><td>"
                    + df.format(extent.getMinY()) + "</td><td>";
            infotext += "<tr><td></td> <td><b>" + YMAX + "</b></td><td>"
                    + df.format(extent.getMaxY()) + "</td><td>";
            infotext += "<tr><td></td> <td><b>" + AREA + "</b></td><td>" + area
                    + " (" + width + " X " + height + ")" + "</td></tr>";// Extension
            infotext += "<tr><td></td> <td><b>" + CELL_SIZE + "</b></td><td>"
                    + cellSizeX + ", " + cellSizeY + "</td></tr>"; // Extension fo a cell
            infotext += "<tr><td></td> <td><b>" + CELL_NUM + "</td><td>"
                    + cellnumber + "</td></tr>"; // Number of cells
            infotext += "</table>";

            infotext += "</DIV></BODY></HTML>";

        }
        return infotext;

    }

    public String StatisticsText(PlugInContext context, RasterImageLayer rLayer)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {
        /*
         * Overwrite Locale to UK Decimal format ####.##
         */
        Locale locale = new Locale("en", "UK");
        String pattern = "###.########";
        DecimalFormat df = (DecimalFormat) NumberFormat
                .getNumberInstance(locale);
        df.applyPattern(pattern);

        String infotext = null;
        OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(rLayer);
        Envelope layerEnv = rLayer.getWholeImageEnvelope();
        // Get the statistics
        int numBands = rLayer.getNumBands();

        String bandstring = ": " + String.valueOf(numBands);
        df.format(rstLayer.getMinValue());
        df.format(rstLayer.getMaxValue());
        df.format(rstLayer.getMeanValue());
        int X = rstLayer.getNX(); // Number of columns
        int Y = rstLayer.getNY(); // Number of rows
        df.format(rstLayer.getMeanValue() * (X * Y - nodata(context, rstLayer)));
        // String nodata = df.format(rstLayer.getNoDataValue());// No data
        String nodata;
        nodata = df.format(rLayer.getNoDataValue());// No data
        df.format(rstLayer.getVariance());
        double var = rstLayer.getVariance();// Variance as double
        df.format(Math.sqrt(var));
        df.format(var / rstLayer.getMeanValue());
        int validcells = X * Y - nodata(context, rstLayer);// Number of
                                                           // valid cells

        int nodatacells = nodata(context, rstLayer);// number of no data
                                                    // cells

        infotext = "<HTML><BODY>";
        infotext += "<table border='0.1'>";
        infotext += "<tr><td><b>" + LAYER_NAME + "</b> </td><td>"
                + rLayer.getName() + "</td></tr>";
        infotext += "</table><br>";

        infotext += "<table border='0.1'>";
        infotext += "<tr><td><b>" + ENV + "</b> </td><td>"
                + layerEnv.toString() + "</td></tr>";
        infotext += "<tr><td><b>" + BANDS + "</b> </td><td>" + bandstring
                + "</td></tr>";
        infotext += "<tr><td><b>" + NODATA + "</b> </td><td>" + nodata
                + "</td></tr>";
        infotext += "<tr><td><b>" + VALIDCELLS + "</b> </td><td>"
                + Integer.toString(validcells) + "</td></tr>";
        infotext += "<tr><td><b>" + NODATACELLS + "</b> </td><td>"
                + Integer.toString(nodatacells) + "</td></tr>";
        infotext += "</table><br>";

        infotext += "<table border='1'>";
        infotext += "<tr><td bgcolor=#CCCCCC align='center'> " + BAND
                + "</td><td bgcolor=#CCCCCC align='center'> " + MIN
                + "</td><td bgcolor=#CCCCCC align='center'> " + MAX
                + "</td><td bgcolor=#CCCCCC align='center'> " + MEAN
                + "</td><td bgcolor=#CCCCCC align='center'> " + SUM
                + "</td><td bgcolor=#CCCCCC align='center'> " + VARIANCE
                + "</td><td bgcolor=#CCCCCC align='center'> " + STD
                + "</td><td bgcolor=#CCCCCC align='center'> " + CVAR

                + "</td></tr>";
        for (int b = 0; b < numBands; b++) {
            int numerobanda = b + 1;
            infotext += "</td><td align='right'>"
                    + numerobanda
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMinValue(b))// min
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMaxValue(b))// max
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMeanValue(b))// mean
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getMeanValue(b)
                            * (X * Y - nodata(context, rstLayer)))// sum

                    + "</td><td align='right'>"
                    + df.format(rstLayer.getVariance(b))// variance
                    + "</td><td align='right'>"
                    + df.format(Math.sqrt(rstLayer.getVariance(b)))// std
                    + "</td><td align='right'>"
                    + df.format(rstLayer.getVariance(b)
                            / rstLayer.getMeanValue(b))// cvar

                    + "</td></tr>";
        }
        infotext += "</table>";
        infotext += "</DIV></BODY></HTML>";

        return infotext;
    }

    public boolean execute(PlugInContext context) throws Exception {

        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);

        final WorkbenchContext wbcontext = context.getWorkbenchContext();

        // HTMLFrame out = context.getOutputFrame();
        final JInternalFrame frame = new JInternalFrame(INFO);

        // First panel Info general
        HTMLPanel info = new HTMLPanel();
        info.getRecordPanel().removeAll();
        info.createNewDocument();
        info.addHeader(1, INFO);
        info.append(InfoText(context, rLayer));

        // Second panel Statistics
        HTMLPanel statistics = new HTMLPanel();
        statistics.getRecordPanel().removeAll();
        statistics.createNewDocument();
        statistics.addHeader(1, STATISTICS);
        statistics.append(StatisticsText(context, rLayer));

        // -- OK button Panel
        JPanel okPanel = new JPanel();
        final JButton okButton = new JButton(CANCEL) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                return;
            }
        });
        okPanel.add(okButton);
        // -- End of OK Buttom

        /*
         * JTabbedPane tabbedPane = new JTabbedPane(); Border
         * mainComponentBorder = BorderFactory.createCompoundBorder(
         * BorderFactory.createEtchedBorder(),
         * BorderFactory.createEmptyBorder(5, 5, 5, 5));
         * tabbedPane.setBorder(mainComponentBorder); tabbedPane.add(out,
         * LAYER_STATISTICS); frame.add(tabbedPane, BorderLayout.CENTER);
         */

        JTabbedPane tabbedPane = new JTabbedPane();
        Border mainComponentBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPane.setBorder(mainComponentBorder);
        // tabbedPane.add(info, INFO);
        // tabbedPane.add(statistics,ICON_STAT, STATISTICS);
        tabbedPane.addTab(INFO, getIcon(), info, "");
        tabbedPane.addTab(STATISTICS, ICON_STAT, statistics, "");
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(okPanel, BorderLayout.SOUTH);

        frame.setClosable(true);
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setSize(800, 450);
        frame.setVisible(true);
        context.getWorkbenchFrame().addInternalFrame(frame);

        return true;
    }

}
