package org.openjump.core.rasterimage.sextante.rasterWrappers;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;

import com.sun.media.jai.codecimpl.util.DataBufferDouble;

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
     * Convert a 2DArray (Matrix) as double to java.awt.image.Raster
     * 
     * @param 2D Array as double
     * @param width
     * @param height
     * @return
     */
    public static Raster matrixToRaster(double[][] a, int width, int height) {
        final double raw[] = new double[width * height];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, raw, i * width, width);
        }
        final DataBuffer buffer = new DataBufferDouble(raw, raw.length);
        final SampleModel sampleModel = new ComponentSampleModel(
                DataBuffer.TYPE_DOUBLE, width, height, 1, width * 1,
                new int[] { 0 });
        final Raster raster = Raster.createRaster(sampleModel, buffer, null);
        return raster;
    }

}
