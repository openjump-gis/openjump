/*
 * Created on 29.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: ExtractSelectedPartOfImage.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Random;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.PlugInContextTools;
import org.openjump.core.apitools.SelectionTools;
import org.openjump.core.rasterimage.CurrentLayerIsRasterImageLayerCheck;
import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;

import com.sun.media.jai.codecimpl.util.RasterFactory;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * PlugIn that extracts a selected part (fence) of a raster image to a new
 * raster image layer.<br>
 * Some parts were taken from Stefan Ostermann's SaveInterpolationAsImagePlugIn.
 * 
 * @author Ole Rahn, (Stefan Ostermann) <br>
 * <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck, <br>
 *         Project: PIROL (2005), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $ [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 * @version $Rev: 4386 [Giuseppe Aruta] - 6.Apr.2015 - added random number to File name
 */
public class ExtractSelectedPartOfImage extends AbstractPlugIn {

    public ExtractSelectedPartOfImage() {
        // super(new PersonalLogger(DebugUserIds.OLE));
    }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return "extractPart.png";
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.Extract-Selected-Part-Of-Image");
    }

    /**
     * @inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);

        Random rnd = new Random();
        int n = 1000 + rnd.nextInt(9000);
        String random = Integer.toString(n);
        String part = I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.part-of");
        String fileName = part + rLayer.getName() + "_" + random + ".tif";

        String newLayerName = context
                .getLayerManager()
                .uniqueLayerName(
                        I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.part-of")
                                + rLayer.getName() + ".tif");
        // String extension =
        // rLayer.getImageFileName().substring(rLayer.getImageFileName().lastIndexOf("."),
        // rLayer.getImageFileName().length());

        File outFile = new File(System.getProperty("java.io.tmpdir")
                .concat(File.separator).concat(fileName)); // .concat(extension));

        Geometry fence = SelectionTools.getFenceGeometry(context);
        Envelope envWanted = fence.getEnvelopeInternal().intersection(
                rLayer.getWholeImageEnvelope());

        Rectangle subset = rLayer.getRectangleFromEnvelope(envWanted);
        Raster raster = rLayer.getRasterData(subset);
        ;

        if (rLayer.getImage().getColorModel() instanceof IndexColorModel) {
            SampleModel sampleModel = rLayer.getImage().getSampleModel();
            IndexColorModel indexColorModel = (IndexColorModel) rLayer
                    .getImage().getColorModel();
            DataBuffer dataBufferIn = raster.getDataBuffer();
            DataBufferByte dataBufferOut = new DataBufferByte(subset.width
                    * subset.height * 3, 3);
            int index = 0;
            int nCells = subset.height * subset.width;
            for (int r = 0; r < subset.height; r++) {
                for (int c = 0; c < subset.width; c++) {
                    int value = dataBufferIn.getElem(index);
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

            int[] bankIndices = new int[3];
            bankIndices[0] = 0;
            bankIndices[1] = 1;
            bankIndices[2] = 2;

            int[] bandOffsets = new int[3];
            bandOffsets[0] = 0;
            bandOffsets[1] = raster.getWidth() * raster.getHeight();
            bandOffsets[2] = 2 * raster.getWidth() * raster.getHeight();

            WritableRaster wRaster = RasterFactory.createBandedRaster(
                    dataBufferOut, raster.getWidth(), raster.getHeight(),
                    raster.getWidth(), bankIndices, bandOffsets,
                    new Point(0, 0));
            raster = wRaster;
        }

        RasterImageIO rasterImageIO = new RasterImageIO();

        rasterImageIO.writeImage(outFile, raster, envWanted,
                rasterImageIO.new CellSizeXY(rLayer.getMetadata()
                        .getOriginalCellSize(), rLayer.getMetadata()
                        .getOriginalCellSize()), rLayer.getMetadata()
                        .getNoDataValue());

        String catName = StandardCategoryNames.WORKING;
        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e1) {
        }

        Point point = RasterImageIO.getImageDimensions(outFile
                .getAbsolutePath());
        Envelope env = RasterImageIO.getGeoReferencing(
                outFile.getAbsolutePath(), true, point);

        Viewport viewport = context.getWorkbenchContext().getLayerViewPanel()
                .getViewport();
        Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                context.getWorkbenchContext(), outFile.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);
        RasterImageLayer ril = new RasterImageLayer(outFile.getName(), context
                .getWorkbenchContext().getLayerManager(),
                outFile.getAbsolutePath(), imageAndMetadata.getImage(), env);

        context.getLayerManager().addLayerable(catName, ril);
        ril.setName(newLayerName);
        return true;

        // if (rLayer==null){
        //            context.getWorkbenchFrame().warnUser(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
        // return false;
        // }
        //
        // Geometry fence = SelectionTools.getFenceGeometry(context);
        // Envelope envWanted = fence.getEnvelopeInternal();
        //
        // BufferedImage partOfImageWanted = rLayer.getTileAsImage(envWanted);
        // Raster partOfRasterWanted = rLayer.getTileAsRaster(envWanted);
        // //[sstein 2 Aug 2010] need to add as we have now the image for
        // display plus the data
        //
        // if (partOfImageWanted==null){
        // context.getWorkbenchFrame().warnUser(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.fence-in-wrong-region"));
        // return false;
        // }
        //
        // boolean returnVal = this.putImageIntoMap(partOfImageWanted,
        // partOfRasterWanted, envWanted, rLayer, context);
        //
        // return returnVal;
    }

    protected boolean putImageIntoMap(BufferedImage partOfImage,
            Raster partOfRaster, Envelope envelope, RasterImageLayer rLayer,
            PlugInContext context) {
        if (partOfImage == null)
            return false;

        String newLayerName = context
                .getLayerManager()
                .uniqueLayerName(
                        I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.part-of")
                                + rLayer.getName());

        RasterImageLayer newRasterLayer = new RasterImageLayer(newLayerName,
                context.getLayerManager(), partOfImage, partOfRaster, envelope);

        String catName = StandardCategoryNames.WORKING;

        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e1) {
        }

        context.getLayerManager().addLayerable(catName, newRasterLayer);

        return true;
    }

    public static MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {

        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        multiEnableCheck.add(checkFactory
                .createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));
        multiEnableCheck.add(checkFactory.createFenceMustBeDrawnCheck());

        EnableCheck enableCheck = new CurrentLayerIsRasterImageLayerCheck(
                PlugInContextTools.getContext(workbenchContext));
        multiEnableCheck.add(enableCheck);

        return multiEnableCheck;
    }

    public void initialize(PlugInContext context) throws Exception {
    }

}
