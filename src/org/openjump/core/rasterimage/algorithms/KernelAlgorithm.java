package org.openjump.core.rasterimage.algorithms;

import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.LinkedHashMap;

import javax.media.jai.PlanarImage;

import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;
import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.ui.util.LayerableUtil;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class KernelAlgorithm {

    LinkedHashMap<String, float[]> subjects = createDataMap();
    LinkedHashMap<String, String> subjects2 = createTextMap();
    public static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();

    public LinkedHashMap<String, float[]> createDataMap() {
        final LinkedHashMap<String, float[]> map = new

        LinkedHashMap<String, float[]>();
        map.put(S_gradientEast, gradientEast);
        map.put(S_gradientNord, gradientNord);
        map.put(S_gradientNorthEast, gradientNorthEast);
        map.put(S_gradientNorthWest, gradientNorthWest);
        map.put(S_gradientSouth, gradientSouth);
        map.put(S_gradientWest, gradientWest);
        map.put(S_laplace3x3, laplace3x3);
        map.put(S_laplace5x5, laplace5x5);
        map.put(S_linedet_horizontal, linedet_horizontal);
        map.put(S_linedet_leftdiagonal, linedet_leftdiagonal);
        map.put(S_linedet_rightdiagonal, linedet_rightdiagonal);
        map.put(S_linedet_vertical, linedet_vertical);
        map.put(S_sobel_horizontal, sobel_horizontal);
        map.put(S_sobel_vertical, sobel_vertical);
        map.put(S_roberts_horizontal, roberts_horizontal);
        map.put(S_roberts_vertical, roberts_vertical);
        map.put(S_prewitt_vertical, prewitt_vertical);
        map.put(S_prewitt_horizontal, prewitt_horizontal);
        map.put(S_sharpening3x3, sharpening3x3);
        map.put(S_sharpening5x5, sharpening5x5);
        map.put(S_sharpeningI, sharpeningI);
        map.put(S_sharpeningII, sharpeningII);
        map.put(S_smoothing3x3, smoothing3x3);
        map.put(S_smoothing5x5, smoothing5x5);
        map.put(S_pointspread, pointspread);
        map.put(S_averageblur, averageblur);
        map.put(S_smoothaverageblur, smoothaverageblur);
        map.put(S_gaussianblur, gaussianblur);
        map.put(S_emboss, emboss);
        map.put(S_unsharpMasking, unsharp_masking);

        return map;
    }

    public LinkedHashMap<String, String> createTextMap() {
        final LinkedHashMap<String, String> map = new

        LinkedHashMap<String, String>();
        map.put(S_gradientEast, Description01_Gradient);
        map.put(S_gradientNord, Description01_Gradient);
        map.put(S_gradientNorthEast, Description01_Gradient);
        map.put(S_gradientNorthWest, Description01_Gradient);
        map.put(S_gradientSouth, Description01_Gradient);
        map.put(S_gradientWest, Description01_Gradient);
        map.put(S_laplace3x3, Description02_Laplace);
        map.put(S_laplace5x5, Description02_Laplace);
        map.put(S_linedet_horizontal, Description03_LineDetection);
        map.put(S_linedet_leftdiagonal, Description03_LineDetection);
        map.put(S_linedet_rightdiagonal, Description03_LineDetection);
        map.put(S_linedet_vertical, Description03_LineDetection);
        map.put(S_sobel_horizontal, Description10_Sobel);
        map.put(S_sobel_vertical, Description10_Sobel);
        map.put(S_roberts_horizontal, Description04_Roberts);
        map.put(S_roberts_vertical, Description04_Roberts);
        map.put(S_prewitt_vertical, Description05_Prewit);
        map.put(S_prewitt_horizontal, Description05_Prewit);
        map.put(S_sharpening3x3, Description06_Sharpening);
        map.put(S_sharpening5x5, Description06_Sharpening);
        map.put(S_sharpeningI, Description06_Sharpening);
        map.put(S_sharpeningII, Description06_Sharpening);
        map.put(S_smoothing3x3, Description07_Smoothing);
        map.put(S_smoothing5x5, Description07_Smoothing);
        map.put(S_pointspread, Description08_Point);
        map.put(S_averageblur, Description08_Point);
        map.put(S_smoothaverageblur, Description08_Others);
        map.put(S_gaussianblur, Description08_Others);
        map.put(S_emboss, Description08_Others);
        map.put(S_unsharpMasking, Description08_Others);

        return map;
    }

    public String Description01_Gradient = "Gradient filters can be used for edge detection in 45-degree increments";
    public String Description02_Laplace = "Laplacian filters are often used for edge detection. They are often applied to an image that has first been smoothed to reduce its sensitivity to noise.";
    public String Description03_LineDetection = "Line detection filters, like the gradient filters, can be used to perform edge detection. You may get better results if you apply a smoothing algorithm before an edge detection algorithm.";
    public String Description04_Roberts = "Roberts filters uses two 2 by 2 kernels to measure gradients in opposing diagonal directions";
    public String Description05_Prewit = "Mathematically, the operator uses two 3×3 kernels which are convolved with the original image to calculate approximations of the derivatives - one for horizontal changes, and one for vertical";
    public String Description06_Sharpening = "The Sharpening (high-pass) filter accentuates the comparative difference in the values with its neighbors.";
    public String Description07_Smoothing = "Smoothing (low-pass) filters smooth the data by reducing local variation and removing noise.The low-pass filter calculates the average (mean) value for each neighborhood. ";
    public String Description08_Point = "The point spread function portrays the distribution of light from a point source through a lense. This will introduce a slight blurring effect.";
    public String Description08_Others = "Blur and Emboss filters";
    public String Description10_Sobel = "Sobel filters are used to edge detection. The operator uses two 3×3 kernels which are convolved with the original image to calculate approximations of the derivatives – one for horizontal changes, and one for vertical.";

    public String S_gradientEast = "Gradient East";
    public String S_gradientNord = "Gradient North";
    public String S_gradientNorthEast = "Gradient North-East";
    public String S_gradientNorthWest = "Gradient North-West";
    public String S_gradientSouth = "Gradient South";
    public String S_gradientWest = "Gradient West";
    public String S_laplace3x3 = "Laplace 3x3";
    public String S_laplace5x5 = "Laplace 5x5";
    public String S_linedet_horizontal = "Line detection horizontal";
    public String S_linedet_leftdiagonal = "Line detection left diagonal";
    public String S_linedet_rightdiagonal = "Line detection right diagonal";
    public String S_linedet_vertical = "Line detection vertical";
    public String S_sobel_horizontal = "Sobel horizontal";
    public String S_sobel_vertical = "Soblel vertical";
    public String S_roberts_horizontal = "Roberts horizontal";
    public String S_roberts_vertical = "Roberts vertical";
    public String S_prewitt_vertical = "Prewit vertical";
    public String S_prewitt_horizontal = "Prewit horizontal";
    public String S_sharpening3x3 = "Sharpening 3x3";
    public String S_sharpening5x5 = "Sharpening 5x5";
    public String S_sharpeningI = "Sharpening I";
    public String S_sharpeningII = "Sharpening II";
    public String S_smoothing3x3 = "Smoothing 3x3";
    public String S_smoothing5x5 = "Smoothing 5x5";
    public String S_unsharpMasking = "Unsharp masking";
    public String S_pointspread = "Point spread";
    public String S_averageblur = "Avarage blur";
    public String S_smoothaverageblur = "Smooth avarage blur";
    public String S_gaussianblur = "Gaussian blur";
    public String S_emboss = "Emboss";

    //Gradient types (Edge detection)
    //Gradient filters can be used for edge detection in 45-degree increments. 

    public float[] gradientEast = { 1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f };
    public float[] gradientNord = { -1f, -2f, -1f, 0f, 0f, 0f, 1f, 2f, 1f };
    public float[] gradientNorthEast = { 0f, -1f, -2f, 1f, 0f, -1f, 2f, 1f, 0f };
    public float[] gradientNorthWest = { -2f, -1f, 0f, -1f, 0f, 1f, 0f, 1f, 2f };
    public float[] gradientSouth = { 1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f };
    public float[] gradientWest = { -1f, 0f, 1f, -2f, 0f, 2f, -1f, 0f, 1f };

    //Laplacian types  (Edge detection)
    //Laplacian filters are often used for edge detection. 
    //They are often applied to an image that has first been smoothed to reduce its sensitivity to noise.
    public float[] laplace3x3 = { 0f, -1f, 0f, -1f, 4f, -1f, 0f, -1f, 0f };
    public float[] laplace5x5 = { 0f, 0f, -1f, 0f, 0f, 0f, -1f, -2f, -1f, 0f,
            -1f, -2f, 17f, -2f, -1f, 0f, -1f, -2f, -1f, 0f, 0f, 0f, -1f, 0f, 0f };

    // Line detection types  (Edge detection)
    // Line detection filters, like the gradient filters, can be used to perform edge detection.
    // You may get better results if you apply a smoothing algorithm before an edge detection algorithm.
    public float[] linedet_horizontal = { -1f, -1f, -1f, -2f, -2f, -2f, -1f,
            -1f, -1f };
    public float[] linedet_leftdiagonal = { 2f, -1f, -1f, -1f, 2f, -1f, -1f,
            -1f, 2f };
    public float[] linedet_rightdiagonal = { -1f, -1f, 2f, -1f, 2f, -1f, 2f,
            -1f, -1f };
    public float[] linedet_vertical = { -1f, 0f, -1f, -1f, 2f, -1f, -1f, 0f,
            -1f };

    //Sobel types  (Edge detection)
    // The Sobel filter is used for edge detection.

    public float[] sobel_horizontal = { -1f, -2f, -1f, 0f, 0f, 0f, 1f, 2f, 1f };
    public float[] sobel_vertical = { -1f, 0f, 1f, -2f, 0f, 2f, -1f, 0f, -1f };

    //Roberts types
    //The Gradient-Roberts filter uses two 2 by 2 kernels to measure gradients in opposing diagonal 
    //directions
    public float[] roberts_horizontal = { 0f, -1f, 1f, 0f };
    public float[] roberts_vertical = { -1f, 0f, 0f, 1f };

    //Mathematically, the operator uses two 3×3 kernels which are convolved with the original image to calculate approximations of the derivatives - one for horizontal changes, and one for vertical
    public float[] prewitt_vertical = { -1f, 0f, 1f, -1f, 0f, 1f, -1f, 0f, 1f };
    public float[] prewitt_horizontal = { -1f, -1f, -1f, 0f, 0f, 0f, 1f, 1f, 1f };

    // Sharpening types.
    // The Sharpening (high-pass) filter accentuates the comparative difference in the values
    // with its neighbors. A high-pass filter calculates the focal sum statistic for each cell
    // of the input using a weighted kernel neighborhood. It brings out the boundaries between
    // features (for example, where a water body meets the forest), thus sharpening edges between
    // objects. The high-pass filter is referred to as an edge enhancement filter. 
    // The high-pass filter kernel identifies which cells to use in the neighborhood and how much
    // to weight them (multiply them by).
    //
    public float[] sharpening3x3 = { -1f, -1f, -1f, -1f, 9f, -1f, -1f, -1f, -1f };
    public float[] sharpening5x5 = { -1f, -3f, -4f, -3f, -1f, -3f, 0f, 6f, 0,
            -3f, -4f, 6f, 21f, 6f, -4f, -3f, 0f, 6f, 0, -3f, -1f, -3f, -4f,
            -3f, -1f };
    public float[] sharpeningI = { 0f, -1 / 4f, 0f, -1 / 4f, 2f, -1 / 4f, 0f,
            -1 / 4f, 0f };
    public float[] sharpeningII = { -1 / 4f, -1 / 4f, -1 / 4f, -1 / 4f, 3f,
            -1 / 4f, -1 / 4f, -1 / 4f, -1 / 4f };

    // Smoothing types.
    //Smoothing (low-pass) filters smooth the data by reducing local variation and removing noise. 
    //The low-pass filter calculates the average (mean) value for each neighborhood. 
    //The effect is that the high and low values within each neighborhood will be averaged out, 
    //reducing the extreme values in the data.
    public float[] smoothing_arithmatic_mean = { 0.111f, 0.111f, 0.111f,
            0.111f, 0.111f, 0.111f, 0.111f, 0.111f, 0.111f };
    public float[] smoothing3x3 = { 1f, 2f, 1f, 2f, 4f, 2f, 1f, 2f, 1f };
    public float[] smoothing5x5 = { 1f, 1f, 1f, 1f, 1f, 1f, 4f, 4f, 4f, 1f, 1f,
            4f, 12f, 4f, 1f, 1f, 4f, 4f, 4f, 1f, 1f, 1f, 1f, 1f, 1f };

    //Point spread type.
    //The point spread function portrays the distribution of light from a point source 
    //through a lense. This will introduce a slight blurring effect. 
    public float[] pointspread = { -0.627f, 0.352f, -0.627f, -0.352f, 2.923f,
            -0.352f, -0.627f, 0.352f, -0.627f };

    //Blur and Emboss
    public float[] averageblur = { 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f,
            1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f };
    public float[] smoothaverageblur = { 0f, 1 / 8f, 1 / 0f, 1 / 8f, 1 / 2f,
            1 / 8f, 0f, 1 / 8f, 0f };
    public float[] gaussianblur = { 1f / 256f, 4f / 256f, 6f / 256f, 4f / 256f,
            1f / 256f, 4f / 256f, 16f / 256f, 24f / 256f, 16f / 256f,
            4f / 256f, 6f / 256f, 24f / 256f, 36f / 256f, 24f / 256f,
            6f / 256f, 4f / 256f, 16f / 256f, 24f / 256f, 16f / 256f,
            4f / 256f, 1f / 256f, 4f / 256f, 6f / 256f, 4f / 256f, 1f / 256f };
    public float[] unsharp_masking = { -1f / 256f, -4f / 256f, -6f / 256f,
            -4f / 256f, -1f / 256f, -4f / 256f, -16f / 256f, -24f / 256f,
            -16f / 256f, -4f / 256f, -6f / 256f, -24f / 256f, 276f / 256f,
            -24f / 256f, -6f / 256f, -4f / 256f, -16f / 256f, -24f / 256f,
            -16f / 256f, -4f / 256f, -1f / 256f, -4f / 256f, -6f / 256f,
            -4f / 256f, -1f / 256f };

    public float[] emboss = { 2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f };

    public void filterRaster(File file, RasterImageLayer rLayer, float[] kernel)
            throws Exception {

        final Double dim = Math.sqrt(kernel.length);
        final int val = dim.intValue();
        final BufferedImageOp blur = new ConvolveOp(
                new Kernel(val, val, kernel));

        /*    final Raster r = rLayer.getRasterData(rLayer
                    .getRectangleFromEnvelope(rLayer.getWholeImageEnvelope()));
            final SampleModel sm = r.getSampleModel();
            final ColorModel colorModel = PlanarImage.createColorModel(sm);
            final BufferedImage src = new BufferedImage(colorModel,
                    (WritableRaster) r, false, null);*/

        final BufferedImage src = rLayer.getImage();
        final int type;

        if (LayerableUtil.isMonoband(rLayer)) {

            type = BufferedImage.TYPE_USHORT_GRAY;
        } else {

            type = DataBuffer.TYPE_FLOAT;
        }
        final BufferedImage result = blur.filter(src,
                new BufferedImage(src.getWidth(), src.getHeight(), type));

        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(file, result.getData(), rLayer
                .getWholeImageEnvelope(), rasterImageIO.new CellSizeXY(rLayer
                .getMetadata().getOriginalCellSize(), rLayer.getMetadata()
                .getOriginalCellSize()), rLayer.getMetadata().getNoDataValue());
    }

    public void filterRaster2(File file, RasterImageLayer rLayer, float[] kernel)
            throws Exception {

        final Double dim = Math.sqrt(kernel.length);
        final int val = dim.intValue();
        final BufferedImageOp blur = new ConvolveOp(
                new Kernel(val, val, kernel));

        /*    final Raster r = rLayer.getRasterData(rLayer
                    .getRectangleFromEnvelope(rLayer.getWholeImageEnvelope()));
            final SampleModel sm = r.getSampleModel();
            final ColorModel colorModel = PlanarImage.createColorModel(sm);
            final BufferedImage src = new BufferedImage(colorModel,
                    (WritableRaster) r, false, null);*/

        BufferedImage src = null;
        int type;
        if (LayerableUtil.isMonoband(rLayer)) {
            src = rLayer.getImage();
            type = BufferedImage.TYPE_USHORT_GRAY;
        } else {
            final Raster r = rLayer.getRasterData(rLayer
                    .getRectangleFromEnvelope(rLayer.getWholeImageEnvelope()));
            final SampleModel sm = r.getSampleModel();
            final ColorModel colorModel = PlanarImage.createColorModel(sm);
            src = new BufferedImage(colorModel, (WritableRaster) r, false, null);
            type = BufferedImage.TYPE_BYTE_INDEXED;
        }
        final BufferedImage result = blur.filter(src,
                new BufferedImage(src.getWidth(), src.getHeight(), type));
        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(file, result.getData(), rLayer
                .getWholeImageEnvelope(), rasterImageIO.new CellSizeXY(rLayer
                .getMetadata().getOriginalCellSize(), rLayer.getMetadata()
                .getOriginalCellSize()), rLayer.getMetadata().getNoDataValue());
    }

    public void load(File outFile, String name, String category)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        final RasterImageIO rasterImageIO = new RasterImageIO();
        final Point point = RasterImageIO.getImageDimensions(outFile
                .getAbsolutePath());
        final Envelope env = RasterImageIO.getGeoReferencing(
                outFile.getAbsolutePath(), true, point);

        final Viewport viewport = frame.getContext().getLayerViewPanel()
                .getViewport();
        final Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        final ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                frame.getContext(), outFile.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);
        final RasterImageLayer ril = new RasterImageLayer(name, frame
                .getContext().getLayerManager(), outFile.getAbsolutePath(),
                imageAndMetadata.getImage(), env);
        try {
            category = ((Category) frame.getContext().getLayerableNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (final RuntimeException e) {

        }
        frame.getContext().getLayerManager().addLayerable(category, ril);
    }

}
