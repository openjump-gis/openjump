package org.openjump.core.rasterimage;

import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
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

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.plugin.layer.pirolraster.LoadSextanteRasterImagePlugIn;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * @author Giuseppe Aruta - Extension codes to perform some RasterImage
 *         Input/Output operations
 * @version 1 [2015-03-11]
 */
public class RasterImageIOUtils {
    static Properties properties = null;
    private static String byteOrder = "LSBFIRST";
    private static String propertiesFile = LoadSextanteRasterImagePlugIn
            .getPropertiesFile();
    static NumberFormat cellFormat = null;
    // public static final Double DEFAULT_NODATA = Double.valueOf(-9999.0D);
    public static double defaultNoData = -99999.0D;

    /**
     * Export selected raster to TIF/TFW - using JAI TiffEncoder.class
     * 
     * @param file
     *            file to save es D:/Openjump/test.tif
     * @param rLayer
     *            Selected Raster Image Layer (RasterImageLayer.class)
     * @param Envelope
     *            envelope
     * @throws NoninvertibleTransformException
     *             , TiffReadingException, Exception
     */

    public static void saveTIF(File file, RasterImageLayer rLayer,
            Envelope envWanted) throws NoninvertibleTransformException,
            TiffReadingException, Exception {

        RasterImageIO rasterImageIO = new RasterImageIO();

        Raster raster = rLayer.getRasterData(rLayer
                .getRectangleFromEnvelope(envWanted));

        rasterImageIO.writeImage(file, raster, envWanted,
                rasterImageIO.new CellSizeXY(rLayer.getMetadata()
                        .getOriginalCellSize(), rLayer.getMetadata()
                        .getOriginalCellSize()), rLayer.getMetadata()
                        .getNoDataValue());
    }

    public static void saveTIF(File file, RasterImageLayer rLayer,
            Envelope envWanted, int band)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        BufferedImage bi = rLayer.getImage();
        int w = bi.getWidth();
        int h = bi.getHeight();

        for (int x = 0; x <= w; x++) {
            for (int y = 0; y <= h; y++) {

                int pixelCol = bi.getRGB(x, y);
                // mask out the non green,non-alpha color.
                // A is 0xFF000000
                // R is 0x00FF0000
                // G is 0x0000FF00
                // B is 0x000000FF
                if (band == 0) {
                    pixelCol &= 0x00FF0000;
                    bi.setRGB(x, y, pixelCol);
                } else if (band == 1) {
                    pixelCol &= 0x0000FF00;
                    bi.setRGB(x, y, pixelCol);

                } else if (band == 2) {
                    pixelCol &= 0x000000FF;
                    bi.setRGB(x, y, pixelCol);

                }

            }
        }

        RasterImageIO rasterImageIO = new RasterImageIO();

        Raster raster = bi.getData();// rLayer.getRasterData(rLayer.getRectangleFromEnvelope(envWanted));

        rasterImageIO.writeImage(file, raster, envWanted,
                rasterImageIO.new CellSizeXY(rLayer.getMetadata()
                        .getOriginalCellSize(), rLayer.getMetadata()
                        .getOriginalCellSize()), rLayer.getMetadata()
                        .getNoDataValue());
    }

    /**
     * Export selected raster to various image file formats using ImageIO
     * 
     * @param File
     *            destination file (absolute or relative path)
     * @param format
     *            Image I/O built-in support for both Oracle Java and OpenJDK is
     *            "gif", "png" and "tif". Oracle Java support also "jpg". Other
     *            formats might be work depending on installed codecs
     * @param BufferedImag
     *            bufferedimage
     * @param Envelope
     *            envelope
     * @throws IOException
     */

    public static void saveImage(File file, String format,
            BufferedImage bufferedimage, Envelope envelope) throws IOException {
        ImageIO.write(bufferedimage, format, file);
        WorldFileHandler worldFileHandler = new WorldFileHandler(
                file.getAbsolutePath(), false);
        worldFileHandler.writeWorldFile(envelope, bufferedimage.getWidth(),
                bufferedimage.getHeight());

    }

    /**
     * Export selected raster to TIF/TFW - using ImageIO.class
     * 
     * @param file
     *            file to save es D:/Openjump/test.tif
     * @param Envelope
     *            envelope of selected Image Layer (RasterImageLayer.class)
     * @param PlanarImage
     *            PlanarImage of selected Image Layer (RasterImageLayer.class)
     * @throws IOException
     */

    public static void saveTIF_ImageIO(File file, Envelope envelope,
            PlanarImage planarimage) throws IOException {
        ImageIO.write(planarimage, "tif", file);
        WorldFileHandler worldFileHandler = new WorldFileHandler(
                file.getAbsolutePath(), false);
        worldFileHandler.writeWorldFile(envelope, planarimage.getWidth(),
                planarimage.getHeight());
    };

    /**
     * Export selected raster to ArcView Gridded Ascii (ASC)
     * 
     * @param file
     *            file to save es D:/Openjump/test.asc
     * @param context
     *            Plugin Context
     * @param rLayer
     *            Selected Raster Image Layer (RasterImageLayer.class)
     * @param band
     *            Number of band to save (O=red, 1=green, 2=blue)
     * @throws IOException
     */

    public static void saveASC(File file, PlugInContext context,
            RasterImageLayer rLayer, int band) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(file);
            cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
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

            String sNoDataVal = Double.toString(rstLayer.getNoDataValue());
            /*
             * if (Math.floor(defaultNoData) == defaultNoData) sNoDataVal =
             * Integer.toString((int) defaultNoData); else { sNoDataVal =
             * Double.toString(defaultNoData); }
             */
            o.println("NODATA_value " + sNoDataVal);
            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            for (int y = 0; y < ny; y++) {
                StringBuffer b = new StringBuffer();
                for (int x = 0; x < nx; x++) {
                    double value = gwrapper.getCellValueAsDouble(x, y, band);

                    if (Double.isNaN(value)) {
                        value = defaultNoData;
                    }
                    if (Math.floor(value) == value)
                        b.append((int) value + " ");
                    else {
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

    /**
     * Export selected raster to ArcView Gridded Binary header (HDR)
     * 
     * @param file
     *            file to save es D:/Openjump/test.hdr
     * @param context
     *            . Plugin Context
     * @param rLayer
     *            . Selected Raster Image Layer (RasterImageLayer.class)
     * @throws IOException
     */

    public static void saveHDR(File outFile, PlugInContext context,
            RasterImageLayer rLayer) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(outFile);
            NumberFormat cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            Properties properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
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

            o.println("cellsize "
                    + Double.toString(rstLayer.getLayerCellSize().x));

            String sNoDataVal = Double.toString(rstLayer.getNoDataValue());
            //Uncomment this code. It seems to rewrite original nodata value with
            //a fix value modifying raster statistics if nodata cells exists
          /*  if (Math.floor(defaultNoData) == defaultNoData)
                sNoDataVal = Integer.toString((int) defaultNoData);
            else {
                sNoDataVal = Double.toString(defaultNoData);
            }*/
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

    /**
     * Export selected raster to ArcView Gridded Binary data (FLT)
     * 
     * @param file
     *            file to save es D:/Openjump/test.flt
     * @param context
     *            Plugin Context
     * @param rLayer
     *            Selected Raster Image Layer (RasterImageLayer.class)
     * @param band
     *            Number of the band to save (O=1st band (red), 1=2nd band
     *            (green), 2=3rd band (blue), etc)
     * @throws IOException
     */

    public static void saveFLT(File outFile, PlugInContext context,
            RasterImageLayer rLayer, int band) throws IOException {
        FileOutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(outFile);

            NumberFormat cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            Properties properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
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
                        bb.putFloat(value);
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

    /**
     * Export selected raster to Surfer ASCII Grid (GRD)
     * 
     * @param file
     *            file to save es D:/Openjump/test.grd
     * @param context
     *            . Plugin Context
     * @param rLayer
     *            . Selected Raster Image Layer (RasterImageLayer.class)
     * @param band
     *            . Number of band to save (O=1st band (red), 1=2nd band
     *            (green), 2=3rd band (blue), etc)
     * @throws IOException
     */

    public static void saveSurferGRD(File outfile, PlugInContext context,
            RasterImageLayer rLayer, int band) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(outfile);
            cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
                fis.close();
            } catch (FileNotFoundException localFileNotFoundException) {
            } catch (IOException e) {
                context.getWorkbenchFrame().warnUser(GenericNames.ERROR);
            }
            defaultNoData = rstLayer.getNoDataValue();

            Double xcMin = Double.valueOf(rLayer.getActualImageEnvelope()
                    .getMinX() + 0.5D * rstLayer.getLayerCellSize().x);
            Double ycMin = Double.valueOf(rLayer.getActualImageEnvelope()
                    .getMinY() + 0.5D * rstLayer.getLayerCellSize().y);
            Double xcMax = Double.valueOf(rLayer.getActualImageEnvelope()
                    .getMaxX() - 0.5D * rstLayer.getLayerCellSize().x);
            Double ycMax = Double.valueOf(rLayer.getActualImageEnvelope()
                    .getMaxY() - 0.5D * rstLayer.getLayerCellSize().y);

            PrintStream po = new PrintStream(out);
            po.println("DSAA");
            po.println(rLayer.getOrigImageWidth() + " "
                    + rLayer.getOrigImageHeight());
            po.println(xcMin + " " + xcMax);
            po.println(ycMin + " " + ycMax);
            po.println("0 1");
            po.println(rstLayer.getMinValue() + " " + rstLayer.getMaxValue());

            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            double nodata = rstLayer.getNoDataValue();
            for (int y = ny; y >= 0; y--) {
                StringBuffer b = new StringBuffer();
                for (int x = 0; x < nx; x++) {
                    double value = gwrapper.getCellValueAsDouble(x, y, band);

                    if (value == nodata) {
                        b.append("1.70141E+38 ");
                    } else if (Math.floor(value) == value)
                        b.append((int) value + " ");
                    else {
                        b.append(value + " ");
                    }

                }

                po.println(b);
            }
            po.close();

        } catch (Exception e) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame()
                    .getOutputFrame()
                    .addText(
                            "SaveImageToRasterPlugIn Exception:Export Part of FLT/ASC or modify raster to GRD not yet implemented. Please Use Sextante Plugin");
        } finally {
            if (out != null)
                out.close();
        }
    }

    /**
     * Export selected raster to XYZ table. If the file is a multi banded raster
     * each band is extracted to a proper band named "band1", "band2", etc
     * 
     * @param file
     *            file to save es D:/Openjump/test.xyz
     * @param context
     *            . Plugin Context
     * @param rLayer
     *            . Selected Raster Image Layer (RasterImageLayer.class)
     * @throws IOException
     */

    public static void saveXYZ(File outfile, PlugInContext context,
            RasterImageLayer rLayer) throws IOException {
        OutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);
            int numBands = rLayer.getNumBands();
            out = new FileOutputStream(outfile);
            cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
                fis.close();
            } catch (FileNotFoundException localFileNotFoundException) {
            } catch (IOException e) {
                context.getWorkbenchFrame().warnUser(GenericNames.ERROR);
            }

            PrintStream po = new PrintStream(out);
            po.append("coordinateX\tcoordinateY");
            for (int b = 0; b < numBands; b++) {
                int n = b + 1;
                po.append("\tband" + n);
            }
            po.append("\n");
            GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                    rstLayer, rstLayer.getLayerGridExtent());
            int nx = rstLayer.getLayerGridExtent().getNX();
            int ny = rstLayer.getLayerGridExtent().getNY();
            for (int y = 0; y < ny; y++) {
                StringBuffer b = new StringBuffer();
                for (int x = 0; x < nx; x++) {

                    double value0 = gwrapper.getCellValueAsDouble(x, y, 0);
                    double value1 = gwrapper.getCellValueAsDouble(x, y, 1);
                    double value2 = gwrapper.getCellValueAsDouble(x, y, 2);
                    Point2D pt = rstLayer.getLayerGridExtent()
                            .getWorldCoordsFromGridCoords(x, y);
                    double Xf = pt.getX();
                    double Yf = pt.getY();

                    if (Double.isNaN(value0) || Double.isNaN(value1)
                            || Double.isNaN(value2)) {
                        value0 = defaultNoData;
                        value1 = defaultNoData;
                        value2 = defaultNoData;
                    }
                    if (Math.floor(value0) == value0
                            || Math.floor(value1) == value1
                            || Math.floor(value2) == value2)
                        b.append(Xf + "\t" + Yf + "\t" + (double) value0 + "\t"
                                + (double) value1 + "\t" + (double) value2
                                + "\n");
                    else {
                        b.append(Xf + "\t" + Yf + "\t" + value0 + value1
                                + value2 + "\n");
                    }

                }
                po.println(b);
            }
            po.close();

        } catch (Exception e) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame()
                    .getOutputFrame()
                    .addText(
                            "Save To Raster Exception:Export image with no datasource to FLT/ASC/GRD/XYZ not yet implemented. Please Use Sextante Plugin");
        } finally {
            if (out != null)
                out.close();
        }
    }

    /**
     * Load TIF file into OpenJUMP workbench
     * 
     * @param File
     *            file to load es D:/Openjump/test.tif
     * @param PlugInContext
     *            Plugin Context
     * @param Category
     *            . Name of the category to load the file
     * @throws NoninvertibleTransformException
     *             , TiffReadingException, Exception
     */

    public static void loadTIF(File file, PlugInContext context, String category)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        RasterImageIO rasterImageIO = new RasterImageIO();
        Viewport viewport = context.getWorkbenchContext().getLayerViewPanel()
                .getViewport();
        Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                context.getWorkbenchContext(), file.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);
        Point point = RasterImageIO.getImageDimensions(file.getAbsolutePath());
        Envelope env = RasterImageIO.getGeoReferencing(file.getAbsolutePath(),
                true, point);

        RasterImageLayer ril = new RasterImageLayer(file.getName(), context
                .getWorkbenchContext().getLayerManager(),
                file.getAbsolutePath(), imageAndMetadata.getImage(), env);
        // String catName = StandardCategoryNames.RESULT;
        try {
            category = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e1) {
        }
        context.getLayerManager().addLayerable(category, ril);

    }

    /**
     * Load Arcview Gridded Binary file (HDR/FLT) into OpenJUMP workbench
     * 
     * @param file
     *            file to load
     * @param PlugInContext
     * @param Category
     *            . Name of the category to load the file
     * @throws NoninvertibleTransformException
     *             , TiffReadingException, Exception
     */

    public static void loadFLT(File file, PlugInContext context, String category)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        RasterImageIO rasterImageIO = new RasterImageIO();
        Viewport viewport = context.getWorkbenchContext().getLayerViewPanel()
                .getViewport();
        Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                context.getWorkbenchContext(), file.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);

        GridFloat gf = new GridFloat(file.getAbsolutePath());

        Envelope imageEnvelope = new Envelope(gf.getXllCorner(),
                gf.getXllCorner() + gf.getnCols() * gf.getCellSize(),
                gf.getYllCorner(), gf.getYllCorner() + gf.getnRows()
                        * gf.getCellSize());

        RasterImageLayer ril = new RasterImageLayer(file.getName(), context
                .getWorkbenchContext().getLayerManager(),
                file.getAbsolutePath(), imageAndMetadata.getImage(),
                imageEnvelope);
        // String catName = StandardCategoryNames.RESULT;
        try {
            category = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e) {

        }
        context.getLayerManager().addLayerable(category, ril);
    }

    /**
     * Load ArcView Gridded Ascii (ASC) file into OpenJUMP workbench
     * 
     * @param File
     *            file to load
     * @param PlugInContext
     * @param Category
     *            . Name of the category to load the file
     * @throws NoninvertibleTransformException
     *             , TiffReadingException, Exception
     */

    public static void loadASC(File file, PlugInContext context, String category)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        RasterImageIO rasterImageIO = new RasterImageIO();
        Viewport viewport = context.getWorkbenchContext().getLayerViewPanel()
                .getViewport();
        Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        ImageAndMetadata imageAndMetadatar = rasterImageIO.loadImage(
                context.getWorkbenchContext(), file.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);
        GridAscii gf = new GridAscii(file.getAbsolutePath());

        Envelope imageEnvelope = new Envelope(gf.getXllCorner(),
                gf.getXllCorner() + gf.getnCols() * gf.getCellSize(),
                gf.getYllCorner(), gf.getYllCorner() + gf.getnRows()
                        * gf.getCellSize());

        RasterImageLayer rasterlayer = new RasterImageLayer(file.getName(),
                context.getWorkbenchContext().getLayerManager(),
                file.getAbsolutePath(), imageAndMetadatar.getImage(),
                imageEnvelope);
        // String catName = StandardCategoryNames.RESULT;
        try {
            category = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e) {

        }
        context.getLayerManager().addLayerable(category, rasterlayer);
    }

    /**
     * Mask a selected range of values of a raster. The range will be defined by
     * a lower and upper values of the range. The values within the range will
     * be substituted by nodata value. The output file is a Arcview Gridded
     * Binary file (HDR/FLT)
     * 
     * @param file
     *            file to save es D:/Openjump/test.flt
     * @param context
     *            Plugin Context
     * @param rLayer
     *            Selected Raster Image Layer (RasterImageLayer.class)
     * @param int band Number of the band to save (O=1st band (red), 1=2nd band
     *        (green), 2=3rd band (blue), etc)
     * @param float mindata lower value of the mask range
     * @param float maxata upper value of the mask range
     * @param float nodata nodata value
     */

    public static void saveFLT_mask(File outFile, PlugInContext context,
            RasterImageLayer rLayer, int band, float mindata, float maxdata,
            float nodata) throws IOException {
        FileOutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(outFile);
            cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
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
                        if (value >= mindata && value <= maxdata) {
                            bb.putFloat(nodata);
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
                            "Save To Raster Exception:Export image with no datasource to FLT/ASC/GRD/XYZ not yet implemented. Please Use Sextante Plugin");
        } finally {
            if (out != null)
                out.close();
        }
    }

    /**
     * Extract a selected range of values of a raster. The range will be defined
     * by a lower and upper values of the range. All the raster cell values
     * outside this range of values will be substituted by nodata value.The
     * output file is a Arcview Gridded Binary file (HDR/FLT)
     * 
     * @param file
     *            file to save es D:/Openjump/test.flt
     * @param context
     *            Plugin Context
     * @param rLayer
     *            Selected Raster Image Layer (RasterImageLayer.class)
     * @param int band Number of the band to save (O=1st band (red), 1=2nd band
     *        (green), 2=3rd band (blue), etc)
     * @param float mindata lower value of the extract range
     * @param float maxata upper value of the e range
     * @param float nodata nodata value
     */

    public static void saveFLT_extract(File outFile, PlugInContext context,
            RasterImageLayer rLayer, int band, float mindata, float maxdata,
            float nodata) throws IOException {
        FileOutputStream out = null;
        try {
            OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
            rstLayer.create(rLayer);

            out = new FileOutputStream(outFile);
            cellFormat = NumberFormat.getNumberInstance();
            cellFormat.setMaximumFractionDigits(3);
            cellFormat.setMinimumFractionDigits(0);
            properties = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propertiesFile);
                properties.load(fis);
                properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
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
                        if (value >= mindata && value <= maxdata) {
                            bb.putFloat(value);
                        } else {
                            bb.putFloat(nodata);
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
                            "Save To Raster Exception:Export image with no datasource to FLT/ASC/GRD/XYZ not yet implemented. Please Use Sextante Plugin");
        } finally {
            if (out != null)
                out.close();
        }
    }

}
