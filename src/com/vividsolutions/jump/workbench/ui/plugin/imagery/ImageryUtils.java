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
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.openjump.core.rasterimage.GridAscii;
import org.openjump.core.rasterimage.GridFloat;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoReferencedRaster;
import com.vividsolutions.jump.workbench.model.Layer;

public class ImageryUtils {

    static String fileNameOrURL = "";
    static String sourcePathImage = null;

    /**
     * Save to Tiff
     * 
     * @param tiffFile
     *            : ex. C:/Folder/file.tif
     * @param bufferedImage
     *            : BufferedImage from a layer
     * @return
     */
    public static boolean saveToTiff(File tiffFile, BufferedImage bufferedImage) {
        ImageOutputStream ios = null;
        ImageWriter writer = null;
        try {
            // find an appropriate writer
            Iterator it = ImageIO.getImageWritersByFormatName("TIF");
            if (it.hasNext()) {
                writer = (ImageWriter) it.next();
            } else {
                return false;
            }
            // setup writer
            ios = ImageIO.createImageOutputStream(tiffFile);
            writer.setOutput(ios);
            ImageWriteParam writeParam = new ImageWriteParam(
                    Locale.ENGLISH);
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType("LZW");
            // convert to an IIOImage
            IIOImage iioImage = new IIOImage(bufferedImage, null, null);

            writer.write(null, iioImage, writeParam);

        } catch (IOException e) {
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

        } catch (IOException e) {
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
        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
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
        } catch (ReferencedImageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Envelope originalEnv = geoRaster.getOriginalEnvelope();

        Envelope targetEnv = geoRaster.getEnvelope();
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
        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
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

    public static BufferedImage getBufferFromReferenceImageLayer(Layer layer)
            throws IOException {

        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
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
            } catch (Exception ex) {
                // Try with JAI
                bImage = JAI.create("fileload", fileNameOrURL)
                        .getAsBufferedImage();
            }
        }
        if (fileNameOrURL.toLowerCase().endsWith(".tif")
                || fileNameOrURL.toLowerCase().endsWith(".tiff")) {
            try {
                bImage = ImageIO.read(new File(fileNameOrURL));

            } catch (Exception ex) {

                FileSeekableStream stream = new FileSeekableStream(
                        fileNameOrURL);
                TIFFDecodeParam decodeParam = new TIFFDecodeParam();
                decodeParam.setDecodePaletteAsShorts(true);
                ParameterBlock params = new ParameterBlock();
                params.add(stream);
                RenderedOp image1 = JAI.create("tiff", params);
                bImage = image1.getAsBufferedImage();
            }
        } else if (fileNameOrURL.toLowerCase().endsWith(".flt")) {
            try {
                GridFloat gf = new GridFloat(fileNameOrURL);
                gf.readGrid(null);
                bImage = gf.getBufferedImage();
            } catch (Exception ex) {
                // Try with ImageIO
                bImage = ImageIO.read(new File(fileNameOrURL));
            }
        } else if (fileNameOrURL.toLowerCase().endsWith(".asc")
                || fileNameOrURL.toLowerCase().endsWith(".txt")) {
            try {
                GridAscii ga = new GridAscii(fileNameOrURL);
                ga.readGrid(null);
                bImage = ga.getBufferedImage();
            } catch (Exception ex) {
                // Try with ImageIO
                bImage = ImageIO.read(new File(fileNameOrURL));
            }

        } else if (fileNameOrURL.toLowerCase().endsWith(".ecw")) {

            try {
                // ECWImage ecw = new ECWImage(fileNameOrURL);
                bImage = ImageIO.read(new File(fileNameOrURL));
            } catch (Exception ex) {
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
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return bufferedImage;
    }

    /**
     * 
     * @param originalImage
     * @param width
     * @param height
     * @return
     */
    public static BufferedImage resizeImage(BufferedImage bufferedImage,
            int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        // g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
        g.drawImage(bufferedImage.getScaledInstance(width, height,
                Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        return resizedImage;
    }

    public static BufferedImage resizeImage_test(BufferedImage bufferedImage,
            int width, int height) {

        BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
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

}
