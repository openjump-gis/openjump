package com.vividsolutions.jump.workbench.ui.plugin.imagery;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.RenderedOp;
import javax.media.jai.WarpPerspective;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openjump.core.apitools.IOTools;
import org.openjump.core.rasterimage.GridAscii;
import org.openjump.core.rasterimage.GridFloat;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoReferencedRaster;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class ImageryUtils {

    static String fileNameOrURL = "";
    static String sourcePathImage = null;

    /**
     * Save to Tiff
     * @deprecated see IOTools.saveGeoTIFF(BufferedImage bufferedImage, Envelope envelope, File tiffFile)
     * @param tiffFile
     *            : ex. C:/Folder/file.tif
     * @param bufferedImage
     *            : BufferedImage from a layer
     * @return
     */
    @Deprecated
    public static boolean saveToTiff(File tiffFile, BufferedImage bufferedImage) {
        ImageOutputStream ios = null;
        ImageWriter writer = null;
        try {
            // find an appropriate writer
            final Iterator it = ImageIO.getImageWritersByFormatName("TIF");
            if (it.hasNext()) {
                writer = (ImageWriter) it.next();
            } else {
                return false;
            }
            // setup writer
            ios = ImageIO.createImageOutputStream(tiffFile);
            writer.setOutput(ios);
            final ImageWriteParam writeParam = new ImageWriteParam(
                    Locale.ENGLISH);
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType("LZW");
            // convert to an IIOImage
            final IIOImage iioImage = new IIOImage(bufferedImage, null, null);

            writer.write(null, iioImage, writeParam);

        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * Save to PNG
     * 
     * @param pngFile
     *            : ex. C:/Folder/file.png
     * @param bufferedImage
     *            : BufferedImage from a layer
     * @return
     */

    public static boolean saveToPng(File pngFile, BufferedImage image) {
        try {
            ImageIO.write(image, "png", pngFile);

        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * Check if ImageLayer has been modified
     * 
     * @throws ReferencedImageException
     */

    public static boolean hasBeenModified(Layer layer) {
        final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (final Iterator i = featureCollection.iterator(); i.hasNext();) {
            final Feature feature = (Feature) i.next();
            sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
            if (sourcePathImage == null || sourcePathImage.length() < 5) {
                sourcePathImage = "";
            } else {
                sourcePathImage = sourcePathImage.substring(5);
            }

        }
        fileNameOrURL = sourcePathImage.replace("%20", " ");

        GeoReferencedRaster geoRaster = null;
        try {
            geoRaster = new GeoReferencedRaster(fileNameOrURL);
        } catch (final ReferencedImageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final Envelope originalEnv = geoRaster.getOriginalEnvelope();

        final Envelope targetEnv = geoRaster.getEnvelope();
        // targetEnv.expandToInclude(layer.getFeatureCollectionWrapper()
        // .getEnvelope());

        if (targetEnv.equals(originalEnv)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if the selected layer is BMP, JPG, GIF, PNG or TIF
     * 
     * @param layer
     * @return
     */

    public static boolean isCompatibleImageLayer(Layer layer) {
        final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (final Iterator i = featureCollection.iterator(); i.hasNext();) {
            final Feature feature = (Feature) i.next();
            sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
            if (sourcePathImage == null || sourcePathImage.length() < 5) {
                sourcePathImage = "";
            } else {
                sourcePathImage = sourcePathImage.substring(5);
            }

        }
        fileNameOrURL = sourcePathImage.replace("%20", " ");

        if (fileNameOrURL.toLowerCase().endsWith(".jpg")
                || fileNameOrURL.toLowerCase().endsWith(".jpeg")
                || fileNameOrURL.toLowerCase().endsWith(".gif")
                || fileNameOrURL.toLowerCase().endsWith(".png")
                || fileNameOrURL.toLowerCase().endsWith(".bmp")
                || fileNameOrURL.toLowerCase().endsWith(".jp2")
                || fileNameOrURL.toLowerCase().endsWith(".tif")
                || fileNameOrURL.toLowerCase().endsWith(".tiff")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get a bufferedImage from a ReferencedImage layer
     * 
     * @param layer
     * @return
     * @throws IOException
     */

    public static BufferedImage getBufferFromReferenceImageLayer2(Layer layer)
            throws IOException {

        final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (final Iterator<Feature> i = featureCollection.iterator(); i
                .hasNext();) {
            final Feature feature = i.next();
            sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
            if (sourcePathImage == null || sourcePathImage.length() < 5) {
                sourcePathImage = "";
            } else {
                sourcePathImage = sourcePathImage.substring(5);
            }

        }
        fileNameOrURL = sourcePathImage.replace("%20", " ");
        BufferedImage bImage = null;
        final FileImageInputStream is = new FileImageInputStream(new File(
                fileNameOrURL));
        final ImageReader reader = ImageIO.getImageReaders(is).next();
        reader.setInput(is);
        bImage = reader.read(0);
        return bImage;

    }

    /**
     * Get a bufferedImage from a ReferencedImage layer
     * 
     * @param layer
     * @return
     * @throws IOException
     */

    public static BufferedImage getBufferFromReferenceImageLayer(Layer layer)
            throws IOException {

        final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (final Iterator i = featureCollection.iterator(); i.hasNext();) {
            final Feature feature = (Feature) i.next();
            sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
            if (sourcePathImage == null || sourcePathImage.length() < 5) {
                sourcePathImage = "";
            } else {
                sourcePathImage = sourcePathImage.substring(5);
            }

        }
        fileNameOrURL = sourcePathImage.replace("%20", " ");
        BufferedImage bImage = null;
        if (fileNameOrURL.toLowerCase().endsWith(".jpg")
                || fileNameOrURL.toLowerCase().endsWith(".gif")
                || fileNameOrURL.toLowerCase().endsWith(".png")
                || fileNameOrURL.toLowerCase().endsWith(".bmp")
                || fileNameOrURL.toLowerCase().endsWith(".jp2")) {

            try {
                // Try with ImageIO
                bImage = ImageIO.read(new File(fileNameOrURL));
            } catch (final Exception ex) {
                // Try with JAI
                bImage = JAI.create("fileload", fileNameOrURL)
                        .getAsBufferedImage();
            }
        }
        if (fileNameOrURL.toLowerCase().endsWith(".tif")
                || fileNameOrURL.toLowerCase().endsWith(".tiff")) {
            try {
                bImage = ImageIO.read(new File(fileNameOrURL));

            } catch (final Exception ex) {

                final FileSeekableStream stream = new FileSeekableStream(
                        fileNameOrURL);
                final TIFFDecodeParam decodeParam = new TIFFDecodeParam();
                decodeParam.setDecodePaletteAsShorts(true);
                final ParameterBlock params = new ParameterBlock();
                params.add(stream);
                final RenderedOp image1 = JAI.create("tiff", params);
                bImage = image1.getAsBufferedImage();
            }
        } else if (fileNameOrURL.toLowerCase().endsWith(".flt")) {
            try {
                final GridFloat gf = new GridFloat(fileNameOrURL);
                gf.readGrid(null);
                bImage = gf.getBufferedImage();
            } catch (final Exception ex) {
                // Try with ImageIO
                bImage = ImageIO.read(new File(fileNameOrURL));
            }
        } else if (fileNameOrURL.toLowerCase().endsWith(".asc")
                || fileNameOrURL.toLowerCase().endsWith(".txt")) {
            try {
                final GridAscii ga = new GridAscii(fileNameOrURL);
                ga.readGrid(null);
                bImage = ga.getBufferedImage();
            } catch (final Exception ex) {
                // Try with ImageIO
                bImage = ImageIO.read(new File(fileNameOrURL));
            }

        } else if (fileNameOrURL.toLowerCase().endsWith(".ecw")) {

            try {
                // ECWImage ecw = new ECWImage(fileNameOrURL);
                bImage = ImageIO.read(new File(fileNameOrURL));
            } catch (final Exception ex) {
                // Try with ImageIO
                bImage = ImageIO.read(new File(fileNameOrURL));
            }

        }
        return bImage;
    }

    /**
     * Add an alpha channel to a BufferedImage
     * 
     * @param image
     * @return
     */

    public static BufferedImage addAlphaChannel(BufferedImage image) {
        final BufferedImage bufferedImage = new BufferedImage(
                image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return bufferedImage;
    }

    /**
     * Resize image
     * @param originalImage
     * @param width
     * @param height
     * @return
     */
    public static BufferedImage resizeImage(BufferedImage bufferedImage,
            int width, int height) {
        final BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = resizedImage.createGraphics();
        // g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
        g.drawImage(bufferedImage.getScaledInstance(width, height,
                Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        return resizedImage;
    }

    public static BufferedImage resizeImage_test(BufferedImage bufferedImage,
            int width, int height) {

        final BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bufferedImage, 0, 0, width, height, null);
        g.setComposite(AlphaComposite.Src);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.dispose();
        return resizedImage;
    }

    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class
            .getName() + " - FILE CHOOSER DIRECTORY";
    private final static JFCWithEnterAction fileChooser = new GUIUtil.FileChooserWithOverwritePrompting();
    private final static FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "GeoTIFF", "tif");
    private static final String SAVE = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.save");
    private static final String ALLOWED_IMAGES = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.allowed-files");
    static WorkbenchContext wcontex = JUMPWorkbench.getInstance().getContext();

    /**
     * Affine transformation of an image file loaded as ReferencedImageLayer.class
     * @param Layer.  (as ReferencedImageLayer.class)
     * @param AffineTransformation. com.vividsolutions.jts.geom.util.AffineTransformation
     * @param resizeImageToHalf. Resize image to half dimension
     * @throws Exception
     */
    public static void affineTransformation(Layer layer,
            AffineTransformation trans, boolean resizeImageToHalf)
            throws Exception {
        try {

            fileChooser.setDialogTitle(SAVE);
            fileChooser.setFileFilter(filter);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.addChoosableFileFilter(filter);
            if (PersistentBlackboardPlugIn.get(wcontex).get(
                    FILE_CHOOSER_DIRECTORY_KEY) != null) {
                fileChooser.setCurrentDirectory(new File(
                        (String) PersistentBlackboardPlugIn.get(wcontex).get(
                                FILE_CHOOSER_DIRECTORY_KEY)));
            }
            //    File outFile = null;
            File outFile = null;
            int option;
            option = fileChooser.showSaveDialog(wcontex.getWorkbench()
                    .getFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                outFile = fileChooser.getSelectedFile();
                final String filePath = outFile.getAbsolutePath();
                //   outFile = new File(filePath + ".png");
                outFile = new File(filePath + ".tif");

                final Envelope inEnvelope = new Envelope();
                // Get the bufferedImage from a ReferencedImage layer
                BufferedImage InImageBuffer = getBufferFromReferenceImageLayer2(layer);
                // Ad alpha chanel
                InImageBuffer = ImageryUtils.addAlphaChannel(InImageBuffer);
                if (resizeImageToHalf) {
                    InImageBuffer = ImageryUtils.resizeImage(InImageBuffer,
                            InImageBuffer.getWidth() / 2,
                            InImageBuffer.getHeight() / 2);
                }
                inEnvelope.expandToInclude(layer.getFeatureCollectionWrapper()
                        .getEnvelope());
                final Geometry P0 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMinX(),
                                inEnvelope.getMinY()));
                final Geometry P1 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMaxX(),
                                inEnvelope.getMinY()));
                final Geometry P2 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMaxX(),
                                inEnvelope.getMaxY()));
                final Geometry P3 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMinX(),
                                inEnvelope.getMaxY()));

                final Geometry P0_ = trans.transform(P0);
                final Geometry P1_ = trans.transform(P1);
                final Geometry P2_ = trans.transform(P2);
                final Geometry P3_ = trans.transform(P3);
                // Apply transformation from source points to target points
                // To use for the image buffer
                final WarpPerspective warp = new WarpPerspective(
                        PerspectiveTransform.getQuadToQuad(
                                P0.getCoordinate().x, P0.getCoordinate().y,
                                P1.getCoordinate().x, P1.getCoordinate().y,
                                P2.getCoordinate().x, P2.getCoordinate().y,
                                P3.getCoordinate().x, P3.getCoordinate().y,
                                P0_.getCoordinate().x, P0_.getCoordinate().y,
                                P1_.getCoordinate().x, P1_.getCoordinate().y,
                                P2_.getCoordinate().x, P2_.getCoordinate().y,
                                P3_.getCoordinate().x, P3_.getCoordinate().y));
                // Apply transformation to the image buffer
                // outImageBuffer to use for transformed Image

                final ParameterBlock pb = new ParameterBlock();
                pb.addSource(InImageBuffer);
                pb.add(warp);
                pb.add(new InterpolationNearest());
                final RenderedOp outputOp = JAI.create("warp", pb);
                final BufferedImage outImageBuffer = outputOp
                        .getAsBufferedImage();

                //    final Raster outRaster = outputOp.getData(null);
                final GeometryFactory gf = new GeometryFactory();
                final Geometry outGeometry = trans.transform(gf
                        .toGeometry(inEnvelope));
                // Get Envelope from out Geometry
                // outoutEnvelope to use for transformed Image
                final Envelope outEnvelope = outGeometry.getEnvelope()
                        .getEnvelopeInternal();
                // Set input raster layer to invisible
                layer.setVisible(false);
                outputOp.dispose();
                // Save output (affined transformed) image GeoTIF file
                IOTools.saveGeoTIFF(outImageBuffer, outEnvelope, outFile);

                IOTools.loadImageAsLayer(outFile, wcontex, "tif");
            } else if (option == JFileChooser.CANCEL_OPTION) {

                return;
            }

        } catch (final RuntimeException localRuntimeException) {
            JOptionPane.showMessageDialog(null, ALLOWED_IMAGES, null,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Affine transformation of an image file loaded as RasterImageLaye.class
     * @param RasterImageLayer.  
     * @param AffineTransformation. com.vividsolutions.jts.geom.util.AffineTransformation
     * @param resizeImageToHalf. Resize image to half dimension
     * @throws Exception
     */

    public static void affineTransformation(RasterImageLayer layer,
            AffineTransformation trans, boolean resizeImageToHalf)
            throws Exception {
        try {

            fileChooser.setDialogTitle(SAVE);
            fileChooser.setFileFilter(filter);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.addChoosableFileFilter(filter);
            if (PersistentBlackboardPlugIn.get(wcontex).get(
                    FILE_CHOOSER_DIRECTORY_KEY) != null) {
                fileChooser.setCurrentDirectory(new File(
                        (String) PersistentBlackboardPlugIn.get(wcontex).get(
                                FILE_CHOOSER_DIRECTORY_KEY)));
            }
            //    File outFile = null;
            File outFile = null;
            int option;
            option = fileChooser.showSaveDialog(wcontex.getWorkbench()
                    .getFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                outFile = fileChooser.getSelectedFile();
                final String filePath = outFile.getAbsolutePath();
                //   outFile = new File(filePath + ".png");
                outFile = new File(filePath + ".tif");

                final Envelope inEnvelope = new Envelope();
                // Get the bufferedImage from a ReferencedImage layer
                BufferedImage InImageBuffer = layer.getImage();
                // Ad alpha chanel
                InImageBuffer = ImageryUtils.addAlphaChannel(InImageBuffer);
                if (resizeImageToHalf) {
                    InImageBuffer = ImageryUtils.resizeImage(InImageBuffer,
                            InImageBuffer.getWidth() / 2,
                            InImageBuffer.getHeight() / 2);
                }
                inEnvelope.expandToInclude(layer.getWholeImageEnvelope());
                final Geometry P0 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMinX(),
                                inEnvelope.getMinY()));
                final Geometry P1 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMaxX(),
                                inEnvelope.getMinY()));
                final Geometry P2 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMaxX(),
                                inEnvelope.getMaxY()));
                final Geometry P3 = new GeometryFactory()
                        .createPoint(new Coordinate(inEnvelope.getMinX(),
                                inEnvelope.getMaxY()));

                final Geometry P0_ = trans.transform(P0);
                final Geometry P1_ = trans.transform(P1);
                final Geometry P2_ = trans.transform(P2);
                final Geometry P3_ = trans.transform(P3);
                // Apply transformation from source points to target points
                // To use for the image buffer
                final WarpPerspective warp = new WarpPerspective(
                        PerspectiveTransform.getQuadToQuad(
                                P0.getCoordinate().x, P0.getCoordinate().y,
                                P1.getCoordinate().x, P1.getCoordinate().y,
                                P2.getCoordinate().x, P2.getCoordinate().y,
                                P3.getCoordinate().x, P3.getCoordinate().y,
                                P0_.getCoordinate().x, P0_.getCoordinate().y,
                                P1_.getCoordinate().x, P1_.getCoordinate().y,
                                P2_.getCoordinate().x, P2_.getCoordinate().y,
                                P3_.getCoordinate().x, P3_.getCoordinate().y));
                // Apply transformation to the image buffer
                // outImageBuffer to use for transformed Image

                final ParameterBlock pb = new ParameterBlock();
                pb.addSource(InImageBuffer);
                pb.add(warp);
                pb.add(new InterpolationNearest());
                final RenderedOp outputOp = JAI.create("warp", pb);
                final BufferedImage outImageBuffer = outputOp
                        .getAsBufferedImage();

                //    final Raster outRaster = outputOp.getData(null);
                final GeometryFactory gf = new GeometryFactory();
                final Geometry outGeometry = trans.transform(gf
                        .toGeometry(inEnvelope));
                // Get Envelope from out Geometry
                // outoutEnvelope to use for transformed Image
                final Envelope outEnvelope = outGeometry.getEnvelope()
                        .getEnvelopeInternal();
                // Set input raster layer to invisible
                layer.setVisible(false);
                outputOp.dispose();
                // Save output (affined transformed) image GeoTIF file

                IOTools.saveGeoTIFF(outImageBuffer, outEnvelope, outFile);

                IOTools.loadImageAsLayer(outFile, wcontex, "tif");
            } else if (option == JFileChooser.CANCEL_OPTION) {

                return;
            }

        } catch (final RuntimeException localRuntimeException) {
            JOptionPane.showMessageDialog(null, ALLOWED_IMAGES, null,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
