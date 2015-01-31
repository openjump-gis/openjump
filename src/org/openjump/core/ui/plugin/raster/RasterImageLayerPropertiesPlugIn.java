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
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

import org.apache.log4j.Logger;
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
 * @version 0.1 - 2013_05_27 Simple plugin that allows to view some properties
 *          of Sextante Raster Layer: name, file location, raster dimension (in
 *          cell), raster extension, X cell size, numbers of bands, min-max-mean
 *          of 1st band value (if the raster is monoband)
 * @version 0.2 - 2015_01_02 Advanced plugin. Displays File, Raster and cells
 *          data properties of Sextante Raster Layer and allows to save those
 *          information as TXT file
 * @version 0.2 - 2015_01_31. Used HTML instead of TXT frame. Info can be saved
 *           as HTML file         
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

    private static final Logger LOGGER = Logger.getLogger(HTMLFrame.class);
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
                    + cellSize + "</td></tr>"; // Extension fo a cell
            infotext += "<tr><td></td> <td><b>" + CELL_NUM + "</td><td>"
                    + cellnumber + "</td></tr>"; // Number of cells
            infotext += "</table>";

            infotext += "</DIV></BODY></HTML>";

        }
        return infotext;

    }

    public boolean execute(PlugInContext context) throws Exception {

        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);

        final WorkbenchContext wbcontext = context.getWorkbenchContext();

        // HTMLFrame out = context.getOutputFrame();
        final JInternalFrame frame = new JInternalFrame(INFO);
        JTabbedPane tabbedPane = new JTabbedPane();
        Border mainComponentBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPane.setBorder(mainComponentBorder);

        HTMLPanel out = new HTMLPanel();
        out.getRecordPanel().removeAll();
        out.createNewDocument();
        // out.setForeground(Color.gray);
        // out.setBackgroundColor(SystemColor.window);
        // out.setBackground(Color.green);
        out.addHeader(1, INFO);
        out.append(InfoText(context, rLayer));

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

        frame.add(out, BorderLayout.CENTER);
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
