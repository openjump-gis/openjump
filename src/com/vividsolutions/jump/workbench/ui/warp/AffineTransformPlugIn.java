/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.warp;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.RenderedOp;
import javax.media.jai.WarpPerspective;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageIOUtils;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.WorldFileHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.util.AssertionFailedException;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.warp.AffineTransform;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Applies an affine transform to the selected layers. The affine transform is
 * specified using three vectors drawn by the user.
 * 
 * @date 2015_06_18 Giuseppe Aruta (giuseppe_aruta[AT]yahoo.it) Modified plugin.
 *       It allowas now to perform a 3-vector affine transformation either to a
 *       vector or to an image loaded via Sextante Raster Framework.
 * @date 2017_11_10 Giuseppe Aruta (giuseppe_aruta[AT]yahoo.it) Added some
 *       method to call it from WarpPanel
 */

public class AffineTransformPlugIn extends AbstractPlugIn {

    public static String path;

    public static EnableCheck getEnableCheck(
            EnableCheckFactory enableCheckFactory) {
        return new MultiEnableCheck().add(
                enableCheckFactory.createAtLeastNLayerablesMustExistCheck(1))
                .add(enableCheckFactory
                        .createBetweenNAndMVectorsMustBeDrawnCheck(1, 3));

    }

    @Override
    public String getName() {
        return I18N
                .get("com.vividsolutions.jump.workbench.ui.warp.AffineTransformPlugIn");
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] { MenuNames.TOOLS, MenuNames.TOOLS_WARP },
                getName(), false, null,
                getEnableCheck(context.getCheckFactory()));
    }

    @Override
    public boolean execute(final PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        Layerable layer = LayerTools.getSelectedLayerable(context,
                Layerable.class);

        if (layer instanceof RasterImageLayer) {

            affineTransformRaster(context);

        } else if (layer instanceof Layer
                && ((Layer) layer).getStyle(ReferencedImageStyle.class) == null) {
            affineTransformVector(context);

            // We exlude for now image files loaded via Layer.class
        } else if (layer instanceof Layer
                && ((Layer) layer).getStyle(ReferencedImageStyle.class) != null) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            I18N.get("com.vividsolutions.jump.workbench.ui.warp.AffineTransformPlugIn.message1"),
                            null, JOptionPane.INFORMATION_MESSAGE);
            return false;
            // WE exclude WMS layer
        } else {
            JOptionPane
                    .showMessageDialog(
                            null,
                            I18N.get("com.vividsolutions.jump.workbench.ui.warp.AffineTransformPlugIn.message2"),
                            null, JOptionPane.INFORMATION_MESSAGE);
            return false;

        }

        return true;
    }

    /*
     * Affine transformation of a Vector Layer
     */
    public static void affineTransformVector(PlugInContext context)
            throws JUMPException {
        AffineTransform transform = affineTransform(context);
        FeatureCollection featureCollection = transform.transform(context
                .getSelectedLayer(0).getFeatureCollectionWrapper());
        context.getLayerManager().addLayer(
                StandardCategoryNames.WORKING,
                I18N.get("ui.warp.AffineTransformPlugIn.affined") + " "
                        + context.getSelectedLayer(0).getName(),
                featureCollection);
        checkValid(featureCollection, context);
    }

    /*
     * Affine transformation of a Sextante Raster Layer This method uses JAI
     * WarpPerspective.class to warp a bufferedimage
     */
    public static void affineTransformRaster(PlugInContext context)
            throws Exception {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);

        BufferedImage inImageBuffer = rLayer.getImage();// ImageIO.read(new
        if (rLayer.getNumBands() == 1) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            I18N.get("com.vividsolutions.jump.workbench.ui.warp.AffineTransformPlugIn.message3"),
                            null, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // /Save file to TMP folder with a unique name
        Random rnd = new Random();
        int n = 1000 + rnd.nextInt(9000);
        String random = Integer.toString(n);
        String part = I18N.get("ui.warp.AffineTransformPlugIn.affined") + " ";
        String fileName = part + rLayer.getName() + "_" + random + ".tif";
        File outFile = new File(System.getProperty("java.io.tmpdir")
                .concat(File.separator).concat(fileName));

        AffineTransform transform = affineTransform(context);

        // Get the envelope of the image
        final Envelope inEnvelope = rLayer.getWholeImageEnvelope();

        // Get the envelope of the image as geometry
        Geometry inGeometry = rLayer.getWholeImageEnvelopeAsGeometry();

        // Get the four corner of the image envelope as points
        Geometry P0 = new GeometryFactory().createPoint(new Coordinate(
                inEnvelope.getMinX(), inEnvelope.getMinY()));
        Geometry P1 = new GeometryFactory().createPoint(new Coordinate(
                inEnvelope.getMaxX(), inEnvelope.getMinY()));
        Geometry P2 = new GeometryFactory().createPoint(new Coordinate(
                inEnvelope.getMaxX(), inEnvelope.getMaxY()));
        Geometry P3 = new GeometryFactory().createPoint(new Coordinate(
                inEnvelope.getMinX(), inEnvelope.getMaxY()));

        // Affine transformation the four source corners/points
        Geometry P0_ = transform.transform(P0);
        Geometry P1_ = transform.transform(P1);
        Geometry P2_ = transform.transform(P2);
        Geometry P3_ = transform.transform(P3);

        // Apply transformation from source points to target points
        // To use for the image buffer
        WarpPerspective warp = new WarpPerspective(
                PerspectiveTransform.getQuadToQuad(P0.getCoordinate().x,
                        P0.getCoordinate().y, P1.getCoordinate().x,
                        P1.getCoordinate().y, P2.getCoordinate().x,
                        P2.getCoordinate().y, P3.getCoordinate().x,
                        P3.getCoordinate().y, P0_.getCoordinate().x,
                        P0_.getCoordinate().y, P1_.getCoordinate().x,
                        P1_.getCoordinate().y, P2_.getCoordinate().x,
                        P2_.getCoordinate().y, P3_.getCoordinate().x,
                        P3_.getCoordinate().y));

        // Apply transformation to the image buffer
        // outImageBuffer to use for transformed Image
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImageBuffer);
        pb.add(warp);
        pb.add(new InterpolationNearest());
        RenderedOp outputOp = JAI.create("warp", pb);
        final BufferedImage outImageBuffer = outputOp.getAsBufferedImage();

        // Apply affine transformation to original image envelope geometry
        final Geometry outGeometry = transform.transform(inGeometry);

        // Get Envelope from out Geometry
        // outoutEnvelope to use for transformed Image
        final Envelope outEnvelope = outGeometry.getEnvelope()
                .getEnvelopeInternal();

        // Set input raster layer to invisible
        rLayer.setVisible(false);

        // Save output (affined transformed) image TIF file
        RasterImageIOUtils.saveImage(outFile, "tif", outImageBuffer,
                outEnvelope);

        // Load output image file to OpenJUMP Working category
        String catName = StandardCategoryNames.WORKING;
        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException localRuntimeException) {
        }
        RasterImageIOUtils.loadTIF(outFile, context, catName);

    }

    public static void checkValid(FeatureCollection featureCollection,
            PlugInContext context) {
        for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();

            if (!feature.getGeometry().isValid()) {
                context.getLayerViewPanel()
                        .getContext()
                        .warnUser(
                                I18N.get("ui.warp.AffineTransformPlugIn.some-geometries-are-not-valid"));

                return;
            }
        }
    }

    public static void saveTIF(File file, Envelope envelope,
            BufferedImage planarimage) throws Exception {
        ImageIO.write(planarimage, "tif", file);
        WorldFileHandler worldFileHandler = new WorldFileHandler(
                file.getAbsolutePath(), false);
        worldFileHandler.writeWorldFile(envelope, planarimage.getWidth(),
                planarimage.getHeight());
    };

    private static Coordinate vectorCoordinate(int n, boolean tip,
            PlugInContext context, WarpingVectorLayerFinder vectorLayerManager) {
        LineString vector = (LineString) vectorLayerManager.getVectors().get(n);

        return tip ? vector.getCoordinateN(1) : vector.getCoordinateN(0);
    }

    private static AffineTransform affineTransform(PlugInContext context) {
        WarpingVectorLayerFinder vlm = new WarpingVectorLayerFinder(context);

        switch (vlm.getVectors().size()) {
        case 1:
            return new AffineTransform(
                    vectorCoordinate(0, false, context, vlm), vectorCoordinate(
                            0, true, context, vlm));
        case 2:
            return new AffineTransform(
                    vectorCoordinate(0, false, context, vlm), vectorCoordinate(
                            0, true, context, vlm), vectorCoordinate(1, false,
                            context, vlm), vectorCoordinate(1, true, context,
                            vlm));
        case 3:
            return new AffineTransform(
                    vectorCoordinate(0, false, context, vlm), vectorCoordinate(
                            0, true, context, vlm), vectorCoordinate(1, false,
                            context, vlm), vectorCoordinate(1, true, context,
                            vlm), vectorCoordinate(2, false, context, vlm),
                    vectorCoordinate(2, true, context, vlm));
        }

        JOptionPane
                .showMessageDialog(
                        null,
                        "You should draw between 1 to 3 vectors to apply an affine transformation of the image",
                        "Info", 1);

        return null;
    }

    public static void warning(String paramString) {
        throw new AssertionFailedException(paramString != null ? paramString
                : "");
    }

    public Icon createEnableCheck(WorkbenchContext workbenchContext) {
        return null;
    }

    private static Coordinate vectorCoordinatePublic(int n, boolean tip,
            WarpingVectorLayerFinder vectorLayerManager) {
        LineString vector = (LineString) vectorLayerManager.getVectors().get(n);

        return tip ? vector.getCoordinateN(1) : vector.getCoordinateN(0);
    }

    public static AffineTransform affineTransformPublic() {
        WarpingVectorLayerFinder vlm = new WarpingVectorLayerFinder(
                JUMPWorkbench.getInstance().getContext());

        switch (vlm.getVectors().size()) {
        case 1:
            return new AffineTransform(vectorCoordinatePublic(0, false, vlm),
                    vectorCoordinatePublic(0, true, vlm));
        case 2:
            return new AffineTransform(vectorCoordinatePublic(0, false, vlm),
                    vectorCoordinatePublic(0, true, vlm),
                    vectorCoordinatePublic(1, false, vlm),
                    vectorCoordinatePublic(1, true, vlm));
        case 3:
            return new AffineTransform(vectorCoordinatePublic(0, false, vlm),
                    vectorCoordinatePublic(0, true, vlm),
                    vectorCoordinatePublic(1, false, vlm),
                    vectorCoordinatePublic(1, true, vlm),
                    vectorCoordinatePublic(2, false, vlm),
                    vectorCoordinatePublic(2, true, vlm));
        }

        JUMPWorkbench.getInstance().getFrame().getContext().getLayerViewPanel()
                .getContext()
                .warnUser(I18N.get("ui.warp.WarpingPanel.warning_1"));

        return null;
    }

}