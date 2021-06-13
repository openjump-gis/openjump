package org.openjump.core.rasterimage.algorithms;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;

import com.sun.media.jai.codecimpl.util.RasterFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.MathUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class GenericRasterAlgorithm {

    private final static String sSaved = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final static String SCouldNotSave = I18N.getInstance().get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");

    public static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();

    /**
     * Modify nodata tag a values to a defined input and save to tif file
     * @param outputFile
     *        file to save. Eg "C:/folder/filename.tif" (always add extension)
     * @param rasterImageLayer
     *        input RasterImageLayer
     * @param band
     *        input band
     * @param oldnodata
     *        original nodata value
      * @param newnodata
     *        new nodata value
     * @throws IOException if an IOException occurs during getRasterData or writeImage operations
     */

    public void save_ChangeNoData(File outputFile,
            RasterImageLayer rasterImageLayer, int band, double oldnodata,
            double newnodata) throws IOException {
        final Raster ras = rasterImageLayer.getRasterData(null);
        final Envelope env = rasterImageLayer.getWholeImageEnvelope();
        final double cellSizeX = env.getWidth() / ras.getWidth();
        final double cellSizeY = env.getHeight() / ras.getHeight();
        final int width = ras.getWidth();
        final int height = ras.getHeight();
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                final double value = ras.getSampleDouble(x, y, band);// gwrapper.getCellValueAsFloat(x, y, band);
                if (value == oldnodata) {
                    raster.setSample(x, y, band, newnodata);
                } else {
                    raster.setSample(x, y, band, value);
                }
            }
        }
        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(outputFile, raster, env,
                rasterImageIO.new CellSizeXY(cellSizeX, cellSizeY), newnodata);
    }

    /**
     * 
     * @param outputFile output file
     * @param rasterImageLayer raster image layer
     * @param band th eband to process
     * @param nodata nodata value
     * @throws IOException if an IOException occurs
     */
    public void save_ResetNoDataTag(File outputFile,
            RasterImageLayer rasterImageLayer, int band, double nodata)
            throws IOException {
        final Raster ras = rasterImageLayer.getRasterData(null);

        final Envelope env = rasterImageLayer.getWholeImageEnvelope();
        final double cellSizeX = env.getWidth() / ras.getWidth();
        final double cellSizeY = env.getHeight() / ras.getHeight();
        final int width = ras.getWidth();
        final int height = ras.getHeight();
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, width, height, 1, null);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final double value = ras.getSampleDouble(x, y, band);
                raster.setSample(x, y, band, value);
            }
        }
        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(outputFile, raster, env,
                rasterImageIO.new CellSizeXY(cellSizeX, cellSizeY), nodata);
    }

    public void save_LimitValidData(File outputFile,
            RasterImageLayer rasterImageLayer, int band, double mindata,
            double maxdata) throws IOException {
        final Raster ras = rasterImageLayer.getRasterData(null);
        final double nodata = rasterImageLayer.getNoDataValue();
        final Envelope env = rasterImageLayer.getWholeImageEnvelope();
        final double cellSizeX = env.getWidth() / ras.getWidth();
        final double cellSizeY = env.getHeight() / ras.getHeight();
        final int width = ras.getWidth();
        final int height = ras.getHeight();
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, width, height, 1, null);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                final double value = ras.getSampleDouble(x, y, band);// gwrapper.getCellValueAsFloat(x, y, band);
                if (value >= mindata && value <= maxdata) {
                    raster.setSample(x, y, band, nodata);
                } else {
                    raster.setSample(x, y, band, value);
                }
            }
        }
        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(outputFile, raster, env,
                rasterImageIO.new CellSizeXY(cellSizeX, cellSizeY), nodata);
    }

    /**
     * Extract a raster defining limits of output
     * @param outputFile output file
     * @param rasterImageLayer raster image layer
     * @param band the band to process
     * @param mindata  minimum data value to be extracted
     * @param maxdata maximum data value to be extracted
     * @throws IOException if an IOException occurs
     */
    public void save_ExtractValidData(File outputFile,
            RasterImageLayer rasterImageLayer, int band, double mindata,
            double maxdata) throws IOException {
        final Raster ras = rasterImageLayer.getRasterData(null);
        final double nodata = rasterImageLayer.getNoDataValue();
        final Envelope env = rasterImageLayer.getWholeImageEnvelope();
        final double cellSizeX = env.getWidth() / ras.getWidth();
        final double cellSizeY = env.getHeight() / ras.getHeight();
        final int width = ras.getWidth();
        final int height = ras.getHeight();
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, width, height, 1, null);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final double value = ras.getSampleDouble(x, y, band);// gwrapper.getCellValueAsFloat(x, y, band);
                if (value >= mindata && value <= maxdata) {
                    raster.setSample(x, y, band, value);
                } else {
                    raster.setSample(x, y, band, nodata);
                }
            }
        }
        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(outputFile, raster, env,
                rasterImageIO.new CellSizeXY(cellSizeX, cellSizeY), nodata);
    }

    /**
     * Reset the values to a defined number of decimals
     * @param outputFile output file
     * @param rLayer raster image layer
     * @param band the band to process
     * @param n Number of decimal to set the values
     * @throws IOException if an IOException occurs
     */
    public void save_ChangeDecimalValues(File outputFile,
            RasterImageLayer rLayer, int band, int n) throws IOException {
        final Raster ras = rLayer.getRasterData(null);
        final double nodata = rLayer.getNoDataValue();
        final Envelope env = rLayer.getWholeImageEnvelope();
        final double cellSizeX = env.getWidth() / ras.getWidth();
        final double cellSizeY = env.getHeight() / ras.getHeight();
        final int width = ras.getWidth();
        final int height = ras.getHeight();
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final double value = ras.getSampleDouble(x, y, band);
                raster.setSample(x, y, band, MathUtil.round(value, n));
            }
        }
        final RasterImageIO rasterImageIO = new RasterImageIO();
        rasterImageIO.writeImage(outputFile, raster, env,
                rasterImageIO.new CellSizeXY(cellSizeX, cellSizeY), nodata);

    }

    protected void saved(File file) {
        frame.setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved() {
        frame.warnUser(SCouldNotSave);

    }

    //static DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(
    //        Locale.ENGLISH);

    /**
     * Crop a RasterImageLayer to a defined envelope and save to tif file
     * @param outputFile
     *        file to save. Eg "C:/folder/filename.tif" (always add extension)
     * @param rasterLayer
     *        input RasterImageLayer
     * @param envelope
     *        input envelope to crop RasterImageLayer
     * @throws IOException if an IOException occurs
     */
    public void save_CropToEnvelope(File outputFile,
            RasterImageLayer rasterLayer, Envelope envelope) throws IOException {

        final Rectangle subset = rasterLayer.getRectangleFromEnvelope(envelope);
        Raster raster = rasterLayer.getRasterData(subset);
        if (rasterLayer.getImage().getColorModel() instanceof IndexColorModel && subset != null) {
            final IndexColorModel indexColorModel = (IndexColorModel) rasterLayer
                    .getImage().getColorModel();
            final DataBuffer dataBufferIn = raster.getDataBuffer();
            final DataBufferByte dataBufferOut = new DataBufferByte(
                    subset.width * subset.height * 3, 3);
            int index = 0;
            final int nCells = subset.height * subset.width;
            for (int r = 0; r < subset.height; r++) {
                for (int c = 0; c < subset.width; c++) {
                    final int value = dataBufferIn.getElem(index);
                    // if(indexColorModel.getAlpha(value) == 255) {
                    // dataBufferOut.setElem(0, index, rLayer.getNoDataValue());
                    // }
                    dataBufferOut.setElem(0, index,
                            indexColorModel.getRed(value));
                    dataBufferOut.setElem(1, index + nCells,
                            indexColorModel.getGreen(value));
                    dataBufferOut.setElem(2, index + nCells * 2,
                            indexColorModel.getBlue(value));
                    index++;
                }
            }

            final int[] bankIndices = new int[3];
            bankIndices[0] = 0;
            bankIndices[1] = 1;
            bankIndices[2] = 2;

            final int[] bandOffsets = new int[3];
            bandOffsets[0] = 0;
            bandOffsets[1] = raster.getWidth() * raster.getHeight();
            bandOffsets[2] = 2 * raster.getWidth() * raster.getHeight();

            raster = RasterFactory.createBandedRaster(
                    dataBufferOut, raster.getWidth(), raster.getHeight(),
                    raster.getWidth(), bankIndices, bandOffsets,
                    new Point(0, 0));

        }
        final RasterImageIO rasterImageIO = new RasterImageIO();

        rasterImageIO.writeImage(outputFile, raster, envelope,
                rasterImageIO.new CellSizeXY(rasterLayer.getMetadata()
                        .getOriginalCellSize(), rasterLayer.getMetadata()
                        .getOriginalCellSize()), rasterLayer.getMetadata()
                        .getNoDataValue());

    }

    public void save_CropToGeometry(File outputFile,
            RasterImageLayer rasterLayer, Geometry geometry) throws IOException {
        final Envelope env = geometry.getEnvelopeInternal();
        final Rectangle subset = rasterLayer.getRectangleFromEnvelope(env);
        final Raster raster = rasterLayer.getRasterData(subset);
        final int width = raster.getWidth();
        final int height = raster.getHeight();
        final int band = rasterLayer.getNumBands();

        final WritableRaster raster2 = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, width, height, band, null);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double value;
                final GeometryFactory gf = new GeometryFactory();
                final Geometry point = gf.createPoint(new Coordinate(x, y));

                for (int b = 0; b < band; b++) {
                    if (point.intersects(geometry)) {
                        value = raster.getSampleDouble(x, y, b);
                    } else {
                        value = rasterLayer.getNoDataValue();
                    }
                    raster2.setSample(x, y, b, value);
                }
            }
        }

        final RasterImageIO rasterImageIO = new RasterImageIO();

        rasterImageIO.writeImage(outputFile, raster2, env,
                rasterImageIO.new CellSizeXY(rasterLayer.getMetadata()
                        .getOriginalCellSize(), rasterLayer.getMetadata()
                        .getOriginalCellSize()), rasterLayer.getMetadata()
                        .getNoDataValue());

    }

    public void save_WarpToEnvelope(File file, RasterImageLayer rLayer,
            Envelope env) throws Exception {

        final float xScale = (float) (env.getWidth() / rLayer
                .getWholeImageEnvelope().getWidth());
        final float yScale = (float) (env.getHeight() / rLayer
                .getWholeImageEnvelope().getHeight());

        final RasterImageIO rasterImageIO = new RasterImageIO();

        // Get whole image
        final ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(/*frame
                .getContext(),*/ rLayer.getImageFileName(), rLayer.getMetadata()
                .getStats(), null, null);

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(imageAndMetadata.getImage());
        pb.add(xScale);
        pb.add(yScale);

        final RenderedOp outputOp = JAI.create("Scale", pb, null);

        rasterImageIO.writeImage(file, outputOp.copyData(), env,
                rasterImageIO.new CellSizeXY(rLayer.getMetadata()
                        .getOriginalCellSize(), rLayer.getMetadata()
                        .getOriginalCellSize()), rLayer.getMetadata()
                        .getNoDataValue());

    }

    /**
     * Load a file into the workbench
     * @param inputFile
     *          eg. "new File(C:/folder/fileName.tif)"
     * @param category
     *          eg. "Working"
     * @throws Exception if an IOException occurs in RasterImageIO.getImageDimensions
     *      or if a general Exception occurs in RasterImageIO.getGeoReferencing or in
     *      RasterImageIO.loadImage
     */
    public void load(File inputFile, String category)
            throws Exception {

        final RasterImageIO rasterImageIO = new RasterImageIO();
        final Point point = RasterImageIO.getImageDimensions(inputFile
                 .getAbsolutePath());
        final Envelope env = RasterImageIO.getGeoReferencing(
                inputFile.getAbsolutePath(), true, point);
         
        final Viewport viewport = frame.getContext().getLayerViewPanel()
                .getViewport();
        final Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        final ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                /*frame.getContext(),*/ inputFile.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);
        final RasterImageLayer ril = new RasterImageLayer(inputFile.getName(),
                frame.getContext().getLayerManager(),
                inputFile.getAbsolutePath(), imageAndMetadata.getImage(), env);
        try {
            category = ((Category) frame.getContext().getLayerableNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (final RuntimeException e) {
            Logger.warn("GenericRasterAlgorithm.load(\"" + inputFile + "\",\"" + category +
                    "\") : error trying to get the name of the currently selected category", e);
        }
        frame.getContext().getLayerManager().addLayerable(category, ril);
    }

}
