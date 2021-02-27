package org.openjump.core.rasterimage.sextante.rasterWrappers;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.RasterFactory;

import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;

/**
 * Wrapper to convert
 * org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer to 2D
 * Array (Matrix) or to a 1D Array.
 * 
 * @author Giuseppe Aruta [2018-05-18]
 *
 */
public class GridRasterWrapper {
    /**
     * Convert a OpenJUMPSextanteRasterLayer to a 2D Array as double
     * 
     * @param rstLayer
     *              OpenJUMP Sextante Raster Layer
     * @param band
     *              (integer)
     * @return
     *              the matrix representing this Raster as a 2-dimensional double array
     */
    public static double[][] rasterToMatrix(
            OpenJUMPSextanteRasterLayer rstLayer, int band) {
        final int nx = rstLayer.getLayerGridExtent().getNX();
        final int ny = rstLayer.getLayerGridExtent().getNY();
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        final double[][] data = new double[nx][ny];
        for (int x = 0; x < nx; x++) {// cols
            for (int y = 0; y < ny; y++) {// rows
                final double value = gwrapper.getCellValueAsDouble(x, y, band);

                data[x][y] = value;

            }
        }
        return data;
    }

    /**
     * create an empty 2D Array from a OpenJUMPSextanteRasterLayer
     *
     * @param rstLayer
     *              OpenJUMP Sextante Raster Layer
     * @param band
     *            (integer)
     * @return
     *            a 2-dimensional double array sized after this raster
     *            and filled with nodata values
     */

    public static double[][] rasterToEmptyMatrix(
            OpenJUMPSextanteRasterLayer rstLayer, int band) {
        final int nx = rstLayer.getLayerGridExtent().getNX();
        final int ny = rstLayer.getLayerGridExtent().getNY();

        final double[][] data = new double[nx][ny];
        for (int x = 0; x < nx; x++) {// cols
            for (int y = 0; y < ny; y++) {// rows

                data[x][y] = rstLayer.getNoDataValue();

            }
        }
        return data;
    }

    /**
     * Convert the first band of OpenJUMPSextanteRasterLayer to a 2D Array as
     * double Useful for single banded raster
     *
     * @param rstLayer
     *              OpenJUMP Sextante Raster Layer
     * @return
     *              the matrix representing this OpenJUMPSextanteRasterLayer
     *              as a 2-dimensional double array
     */
    public static double[][] rasterToMatrix(OpenJUMPSextanteRasterLayer rstLayer) {
        return rasterToMatrix(rstLayer, 0);
    }

    /**
     * Convert the first band of OpenJUMPSextanteRasterLayer to a 1D Array as
     * double
     *
     * @param rstLayer OpenJUMPSextanteRasterLayer to convert to a double array
     * @param band the Raster band to export
     * @return a 1-dimensional double array containing all the
     *      OpenJUMPSextanteRasterLayer values contained in a band
     */
    public static double[] rasterToArray(OpenJUMPSextanteRasterLayer rstLayer,
            int band) {
        final int nx = rstLayer.getLayerGridExtent().getNX();
        final int ny = rstLayer.getLayerGridExtent().getNY();
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        final double[] data = new double[nx * ny];
        int i = 0;
        for (int x = 0; x < nx; x++) {// cols
            for (int y = 0; y < ny; y++) {// rows
                final double value = gwrapper.getCellValueAsDouble(x, y, band);
                if (value != rstLayer.getNoDataValue()) {
                    data[i] = value;
                    i++;
                }
            }
        }
        final double[] data2 = new double[i];
        System.arraycopy(data, 0, data2, 0, i);
        return data2;
    }

    /**
     * Convert the first band of OpenJUMPSextanteRasterLayer to a 1D Array as
     * double Useful for single banded raster
     *
     * @param rstLayer
     *              OpenJUMP Sextante Raster Layer
     * @return a 1-dimensional double array containing all
     *      OpenJUMPSextanteRasterLayer values contained in band 0
     */
    public static double[] rasterToArray(OpenJUMPSextanteRasterLayer rstLayer) {
        return rasterToArray(rstLayer, 0);
    }

    /**
     * Convert a band of OpenJUMPSextanteRasterLayer to a 1D Array as
     * double. Limit the array to min/max values
     *
     * @param rstLayer OpenJUMP Sextante Raster Layer
     * @param band the Raster band to analyze
     * @param min minimum value to copy
     * @param max maximum value to copy
     * @return a 1-dimensional double array containing all the Raster values
     *      for this band which are strictly higher than min and lower than max
     */
    public static double[] rasterToArray(OpenJUMPSextanteRasterLayer rstLayer,
            int band, double min, double max) {
        final int nx = rstLayer.getLayerGridExtent().getNX();
        final int ny = rstLayer.getLayerGridExtent().getNY();
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        final double[] data = new double[nx * ny];
        int i = 0;
        for (int x = 0; x < nx; x++) {// cols
            for (int y = 0; y < ny; y++) {// rows
                final double value = gwrapper.getCellValueAsDouble(x, y, band);
                if (value != rstLayer.getNoDataValue() & value > min
                        & value < max) {
                    data[i] = value;
                    i++;
                }
            }
        }
        final double[] data2 = new double[i];
        System.arraycopy(data, 0, data2, 0, i);
        return data2;
    }

    /**
     * Convert the first band of of OpenJUMPSextanteRasterLayer to a 1D Array as
     * double. Limit the array to min/max values
     *
     * @param rstLayer
     *              OpenJUMP Sextante Raster Layer
     * @param min minimum value to copy
     * @param max maximum value to copy
     * @return a 1-dimensional double array containing all the Raster values
     *      for band 0 which are strictly higher than min and lower than max
     */

    public static double[] rasterToArray(OpenJUMPSextanteRasterLayer rstLayer,
            double min, double max) {
        return rasterToArray(rstLayer, 0, min, max);
    }

    /**
     * Convert a 2DArray (Matrix) as double to java.awt.image.Raster, to band 0
     * 
     * @param matrix 2D Array as double
     * @param model Sample Model
     * @return java.awt.image.Raster
     */
    public static Raster matrixToRaster(double[][] matrix, SampleModel model) {

        return matrixToRaster(matrix, model, 0);
    }

    /**
     * Convert a 2DArray (Matrix) as double to java.awt.image.Raster, defining
     * the band number
     * 
     * @param matrix 2D Array as double
     * @param model Sample Model
     * @param band
     *            number
     * @return java.awt.image.Raster
     */
    public static Raster matrixToRaster(double[][] matrix, SampleModel model,
            int band) {
        final int w = matrix.length;
        final int h = matrix[0].length;
        final WritableRaster raster = Raster.createWritableRaster(model,
                new Point(0, 0));
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                raster.setSample(i, j, band, matrix[i][j]);
            }
        }
        return raster;
    }

    /**
     * Convert a java.awt.image.Raster to 2DArray (Matrix) as double
     * 
     * @param raster input raster
     * @return 2DArray (Matrix) as double[][]
     */

    public static double[][] rasterToMatrix(Raster raster) {
        try {
            final int w = raster.getWidth(), h = raster.getHeight();
            final double pixels[][] = new double[w][h];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    pixels[x][y] = raster.getSampleDouble(x, y, 0);
                }
            }
            return pixels;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert a double 2DArray (matrix)  to java.awt.image.WritableRaster (single band)
     * @param matrix 2D double Array
     * @return WritableRaster
     */

    public static WritableRaster matrixToRaster(double[][] matrix) {
        final int w = matrix.length;
        final int h = matrix[0].length;
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, w, h, 1, null);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                raster.setSample(i, j, 0, matrix[i][j]);
            }
        }
        return raster;
    }

}
