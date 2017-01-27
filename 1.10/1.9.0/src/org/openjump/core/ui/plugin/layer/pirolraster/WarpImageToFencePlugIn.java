/*
 * Created on 09.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: WarpImageToFencePlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.Point;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Random;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.PlugInContextTools;
import org.openjump.core.apitools.SelectionTools;
import org.openjump.core.rasterimage.CurrentLayerIsRasterImageLayerCheck;
import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;

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
 * PlugIn to warp a RasterImage to the bounding box of the Fence.
 * 
 * @author Ole Rahn <br>
 * <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck, <br>
 *         Project: PIROL (2006), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $ [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 * @version $Rev: 4387 [Giuseppe Aruta] - 6.Apr.2015 - added random number to File name 
 */
public class WarpImageToFencePlugIn extends AbstractPlugIn {

    public WarpImageToFencePlugIn() {
        // super(new PersonalLogger(DebugUserIds.OLE));
    }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.WarpImageToFencePlugIn.Warp-Image-To-Fence");
    }

    /**
     * @inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);

        if (rLayer == null) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return false;
        }

        Random rnd = new Random();
        int n = 1000 + rnd.nextInt(9000);
        String random = Integer.toString(n);
        String warped = I18N.get("ui.warp.WarpingPanel.warped");
        String fileName = warped + "_" + rLayer.getName() + "_" + random
                + ".tif";

        String newLayerName = context.getLayerManager().uniqueLayerName(
                warped + "_" + rLayer.getName());
        File outFile = new File(System.getProperty("java.io.tmpdir")
                .concat(File.separator).concat(fileName));

        Geometry fence = SelectionTools.getFenceGeometry(context);
        Envelope envWanted = fence.getEnvelopeInternal();

        float xScale = (float) (envWanted.getWidth() / rLayer
                .getWholeImageEnvelope().getWidth());
        float yScale = (float) (envWanted.getHeight() / rLayer
                .getWholeImageEnvelope().getHeight());

        RasterImageIO rasterImageIO = new RasterImageIO();

        // Get whole image
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                context.getWorkbenchContext(), rLayer.getImageFileName(),
                rLayer.getMetadata().getStats(), null, null);

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(imageAndMetadata.getImage());
        pb.add(xScale);
        pb.add(yScale);

        RenderedOp outputOp = JAI.create("Scale", pb, null);

        rasterImageIO.writeImage(outFile, outputOp.copyData(), envWanted,
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
        imageAndMetadata = rasterImageIO.loadImage(
                context.getWorkbenchContext(), outFile.getAbsolutePath(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);
        RasterImageLayer ril = new RasterImageLayer(outFile.getName(), context
                .getWorkbenchContext().getLayerManager(),
                outFile.getAbsolutePath(), imageAndMetadata.getImage(), env);

        context.getLayerManager().addLayerable(catName, ril);
        ril.setName(newLayerName);
        // rLayer.setWholeImageEnvelope(envWanted);

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

}
