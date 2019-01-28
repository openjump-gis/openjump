package org.openjump.core.rasterimage.sextante.rasterWrappers;

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

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
     * @param OpenJUMPSextanteRasterLayer
     * @param band
     *            (integer)
     * @return
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
                if (value != rstLayer.getNoDataValue()) {
                    data[x][y] = value;
                }
            }
        }
        return data;
    }

    /**
     * Convert the first band of OpenJUMPSextanteRasterLayer to a 2D Array as
     * double Useful for single banded raster
     * 
     * @param OpenJUMPSextanteRasterLayer
     * @return
     */
    public static double[][] rasterToMatrix(OpenJUMPSextanteRasterLayer rstLayer) {
        return rasterToMatrix(rstLayer, 0);
    }

    /**
     * Convert the first band of OpenJUMPSextanteRasterLayer to a 1D Array as
     * double
     * 
     * @param OpenJUMPSextanteRasterLayer
     * @param band
     * @return
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
     * @param OpenJUMPSextanteRasterLayer
     * @return
     */
    public static double[] rasterToArray(OpenJUMPSextanteRasterLayer rstLayer) {
        return rasterToArray(rstLayer, 0);
    }

    /**
     * Convert a band of OpenJUMPSextanteRasterLayer to a 1D Array as
     * double. Limit the array to min/max values
     * 
     * @param OpenJUMPSextanteRasterLayer
     * @param int band
     * @param double min
     * @param double max
     * @return
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
     * @param OpenJUMPSextanteRasterLayer
     * @param double min
     * @param double max
     * @return
     */

    public static double[] rasterToArray(OpenJUMPSextanteRasterLayer rstLayer,
            double min, double max) {
        return rasterToArray(rstLayer, 0, min, max);
    }

    /**
     * Convert a 2DArray (Matrix) as double to java.awt.image.Raster, to band 0
     * 
     * @param 2D Array as double
     * @param SampleModel
     * @return java.awt.image.Raster
     */
    public static Raster matrixToRaster(double[][] matrix, SampleModel model) {

        return matrixToRaster(matrix, model, 0);
    }

    /**
     * Convert a 2DArray (Matrix) as double to java.awt.image.Raster, defining
     * the band number
     * 
     * @param 2D Array as double
     * @param SampleModel
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
     * @param matrix
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

}
