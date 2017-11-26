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

package com.vividsolutions.jump.plugin.edit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.RenderedOp;
import javax.media.jai.WarpPerspective;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openjump.core.rasterimage.WorldFileHandler;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoImageFactoryFileLayerLoader;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.DualPaneInputDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.imagery.ImageryUtils;

/**
 * Applies an {@link AffineTransformation} to a layer.
 *
 * @author Martin Davis
 * 
 *         [Giuseppe Aruta 2017_11_26] added trasformation for Image layers
 */
public class AffineTransformationPlugIn extends AbstractThreadedUiPlugIn {

    private DualPaneInputDialog dialog;
    private String layerName;
    private double originX = 0.0;
    private double originY = 0.0;
    private double transX = 0.0;
    private double transY = 0.0;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double shearX = 0.0;
    private double shearY = 0.0;
    private double rotationAngle = 0.0;
    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class
            .getName() + " - FILE CHOOSER DIRECTORY";

    private static final String IMAGE_OPTIONS = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.image-options");
    private static final String FORCE_IMAGEWARP = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.force-image-warp");
    private static final String RESIZE_IMAGE = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.resize-image");
    private static final String ALLOWED_IMAGES = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.allowed-files");
    private static final String SAVE = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.save");
    private static final String RESIZE_IMAGE_TOOLTIP = I18N
            .get("jump.plugin.edit.AffineTransformationPlugIn.resize-image-tooltip");

    // private static final String IMAGE_OPTIONS = "Image layer options";
    // private static final String FORCE_IMAGEWARP =
    // "Force affine transformation of images";
    // private static final String RESIZE_IMAGE = "Resize image to half size";
    // private static final String ALLOWED_IMAGES =
    // "Only BMP, GIF, JPG, JP2, PNG and TIF files can be transformed";
    // private static final String SAVE = "Save transformed image to PNG file";
    // private static final String RESIZE_IMAGE_TOOLTIP =
    // "Resize to half the size of image if trasformation requires too much memory";

    public AffineTransformationPlugIn() {
    }

    @Override
    public String getName() {
        return I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Affine-Transformation");
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(
                context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(this, new String[] {
                MenuNames.TOOLS, MenuNames.TOOLS_WARP }, getName() + "...",
                false, null, createEnableCheck(context.getWorkbenchContext()),
                -1);
    }

    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        dialog = new DualPaneInputDialog(context.getWorkbenchFrame(),
                getName(), true);
        setDialogValues(dialog, context);

        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    public void transform() {
        AffineTransformation trans = new AffineTransformation();

        trans.translate(-originX, -originY);

        if (scaleX != 1.0 || scaleY != 1.0) {
            trans.scale(scaleX, scaleY);
        }
        if (shearX != 0.0 || shearY != 0.0) {
            trans.shear(shearX, shearY);
        }
        if (rotationAngle != 0.0) {
            trans.rotate(Math.toRadians(rotationAngle));
        }
        AffineTransformation fromOriginTrans = AffineTransformation
                .translationInstance(originX, originY);
        trans.compose(fromOriginTrans);
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {

        monitor.allowCancellationRequests();
        monitor.report("Transforming layer...");
        reportNothingToUndoYet(context);
        AffineTransformation trans = new AffineTransformation();

        trans.translate(-originX, -originY);

        if (scaleX != 1.0 || scaleY != 1.0) {
            trans.scale(scaleX, scaleY);
        }
        if (shearX != 0.0 || shearY != 0.0) {
            trans.shear(shearX, shearY);
        }
        if (rotationAngle != 0.0) {
            trans.rotate(Math.toRadians(rotationAngle));
        }

        AffineTransformation fromOriginTrans = AffineTransformation
                .translationInstance(originX, originY);
        trans.compose(fromOriginTrans);

        trans.translate(transX, transY);
        if (dialog.getBoolean(FORCE_IMAGEWARP)) {
            forceImageToWarp(context, trans);

        }

        FeatureCollection fc = context.getLayerManager().getLayer(layerName)
                .getFeatureCollectionWrapper();

        FeatureCollection resultFC = new FeatureDataset(fc.getFeatureSchema());

        for (Iterator i = fc.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            Feature f2 = f.clone(true);
            f2.getGeometry().apply(trans);
            f2.getGeometry().geometryChanged();
            resultFC.add(f2);
        }

        createLayers(context, resultFC);
    }

    private void createLayers(PlugInContext context, FeatureCollection transFC) {
        Layer lyr = context.addLayer(StandardCategoryNames.RESULT,
                I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Affine")
                        + layerName, transFC);
        lyr.fireAppearanceChanged();
    }

    private String LAYER;
    private String ORIGIN_X;
    private String ORIGIN_Y;
    private String TRANS_DX;
    private String TRANS_DY;
    private String SCALE_X;
    private String SCALE_Y;
    private String ROTATE_ANGLE;
    private String SHEAR_X;
    private String SHEAR_Y;
    private String SRC_BASE_LAYER;
    private String DEST_BASE_LAYER;

    // private JRadioButton matchSegmentsRB;
    private JTextField originXField;
    private JTextField originYField;
    private JTextField transXField;
    private JTextField transYField;
    private JTextField scaleXField;
    private JTextField scaleYField;
    private JTextField shearXField;
    private JTextField shearYField;
    private JTextField rotateAngleField;

    private void setDialogValues(DualPaneInputDialog dialog,
            PlugInContext context) {

        String ORIGIN = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Anchor-Point");
        String ORIGIN_FROM_LL = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Set-to-Lower-Left");
        String ORIGIN_FROM_MIDPOINT = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Set-to-Midpoint");
        LAYER = GenericNames.LAYER;
        ORIGIN_X = "X";
        ORIGIN_Y = "Y";
        TRANS_DX = "DX";
        TRANS_DY = "DY";
        SCALE_X = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.X-Factor");
        SCALE_Y = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Y-Factor");
        ROTATE_ANGLE = GenericNames.ANGLE;
        SHEAR_X = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.X-Shear");
        SHEAR_Y = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Y-Shear");
        SRC_BASE_LAYER = GenericNames.SOURCE_LAYER;
        DEST_BASE_LAYER = GenericNames.TARGET_LAYER;
        String BASELINE_BUTTON = I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Compute-Parameters");

        dialog.setSideBarImage(new ImageIcon(getClass().getResource(
                "AffineTransformation.png")));
        dialog.setSideBarDescription(I18N
                .get("jump.plugin.edit.AffineTransformationPlugIn.Applies-an-Affine-Transformation-to-all-features-in-a-layer")
                + "  "
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.The-transformation-is-specified-by-a-combination-of-scaling-rotation-shearing-and-translation")
                + "  "
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Transformation-parameters-may-be-computed-from-two-layers-containing-baseline-vectors"));

        dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0),
                context.getLayerManager());
        dialog.addLabel("<HTML><B>" + ORIGIN + "</B></HTML>");

        originXField = dialog
                .addDoubleField(
                        ORIGIN_X,
                        originX,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Anchor-Point-X-value"));
        originYField = dialog
                .addDoubleField(
                        ORIGIN_Y,
                        originY,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Anchor-Point-Y-value"));

        JButton buttonOriginLL = dialog.addButton(ORIGIN_FROM_LL);
        buttonOriginLL.addActionListener(new OriginLLListener(true));

        JButton buttonOriginMid = dialog.addButton(ORIGIN_FROM_MIDPOINT);
        buttonOriginMid.addActionListener(new OriginLLListener(false));

        dialog.addLabel("<HTML><B>"
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Scaling")
                + "</B></HTML>");
        scaleXField = dialog
                .addDoubleField(
                        SCALE_X,
                        scaleX,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Scale-X-Factor"));
        scaleYField = dialog
                .addDoubleField(
                        SCALE_Y,
                        scaleY,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Scale-Y-Factor"));

        dialog.addLabel("<HTML><B>"
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Rotation")
                + "</B></HTML>");
        rotateAngleField = dialog
                .addDoubleField(
                        ROTATE_ANGLE,
                        rotationAngle,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Rotation-Angle-in-degrees"));

        dialog.addLabel("<HTML><B>"
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Shearing")
                + "</B></HTML>");
        shearXField = dialog
                .addDoubleField(
                        SHEAR_X,
                        shearX,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Shear-X-Factor"));
        shearYField = dialog
                .addDoubleField(
                        SHEAR_Y,
                        shearY,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Shear-Y-Factor"));

        dialog.addLabel("<HTML><B>"
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Translation")
                + "</B></HTML>");
        transXField = dialog
                .addDoubleField(
                        TRANS_DX,
                        transX,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Translation-X-value"));
        transYField = dialog
                .addDoubleField(
                        TRANS_DY,
                        transY,
                        20,
                        I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Translation-Y-value"));

        // dialog.startNewColumn();

        dialog.setRightPane();

        JButton setIdentityButton = dialog
                .addButton(I18N
                        .get("jump.plugin.edit.AffineTransformationPlugIn.Set-to-Identity"));
        setIdentityButton.addActionListener(new SetIdentityListener());

        dialog.addSeparator();
        dialog.addLabel("<HTML><B>"
                + I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Baseline-Vectors")
                + "</B></HTML>");
        dialog.addLayerComboBox(SRC_BASE_LAYER, context.getLayerManager()
                .getLayer(0), context.getLayerManager());
        dialog.addLayerComboBox(DEST_BASE_LAYER, context.getLayerManager()
                .getLayer(0), context.getLayerManager());

        JButton buttonParam = dialog.addButton(BASELINE_BUTTON);
        buttonParam.addActionListener(new UpdateParamListener());
        dialog.addSeparator();
        dialog.addLabel("<HTML><B>" + IMAGE_OPTIONS + "</B></HTML>");
        dialog.addCheckBox(FORCE_IMAGEWARP, false);
        dialog.getCheckBox(FORCE_IMAGEWARP).setToolTipText(ALLOWED_IMAGES);
        dialog.addCheckBox(RESIZE_IMAGE, false);
        dialog.getCheckBox(RESIZE_IMAGE).setToolTipText(RESIZE_IMAGE_TOOLTIP);

        dialog.addRow(new JPanel());

    }

    private void getDialogValues(MultiInputDialog dialog) {
        layerName = dialog.getLayer(LAYER).getName();
        originX = dialog.getDouble(ORIGIN_X);
        originY = dialog.getDouble(ORIGIN_Y);
        transX = dialog.getDouble(TRANS_DX);
        transY = dialog.getDouble(TRANS_DY);
        scaleX = dialog.getDouble(SCALE_X);
        scaleY = dialog.getDouble(SCALE_Y);
        shearX = dialog.getDouble(SHEAR_X);
        shearY = dialog.getDouble(SHEAR_Y);
        rotationAngle = dialog.getDouble(ROTATE_ANGLE);
    }

    private void updateOriginLL(boolean isLowerLeft) {
        Layer lyr = dialog.getLayer(LAYER);
        FeatureCollection fc = lyr.getFeatureCollectionWrapper();
        Envelope env = fc.getEnvelope();

        double x = env.getMinX();
        double y = env.getMinY();
        // if not LowerLeft, set to midpoint
        if (!isLowerLeft) {
            x = (env.getMinX() + env.getMaxX()) / 2;
            y = (env.getMinY() + env.getMaxY()) / 2;
        }
        originXField.setText(x + "");
        originYField.setText(y + "");
    }

    private String updateParams() {
        Layer layerSrc = dialog.getLayer(SRC_BASE_LAYER);
        Layer layerDest = dialog.getLayer(DEST_BASE_LAYER);

        FeatureCollection fcSrc = layerSrc.getFeatureCollectionWrapper();
        FeatureCollection fcDest = layerDest.getFeatureCollectionWrapper();

        AffineTransControlPointExtracter controlPtExtracter = new AffineTransControlPointExtracter(
                fcSrc, fcDest);
        String parseErrMsg;
        if (controlPtExtracter.getInputType() == AffineTransControlPointExtracter.TYPE_UNKNOWN) {
            parseErrMsg = controlPtExtracter.getParseErrorMessage();
            return parseErrMsg;
        }

        Coordinate[] srcPts = controlPtExtracter.getSrcControlPoints();
        Coordinate[] destPts = controlPtExtracter.getDestControlPoints();

        TransRotScaleBuilder trsBuilder = null;
        switch (srcPts.length) {
        case 2:
            trsBuilder = new TwoPointTransRotScaleBuilder(srcPts, destPts);
            break;
        case 3:
            trsBuilder = new TriPointTransRotScaleBuilder(srcPts, destPts);
            break;
        }

        if (trsBuilder != null)
            updateParams(trsBuilder);
        return null;
    }

    private void updateParams(TransRotScaleBuilder trsBuilder) {
        originXField.setText(trsBuilder.getOriginX() + "");
        originYField.setText(trsBuilder.getOriginY() + "");
        scaleXField.setText(trsBuilder.getScaleX() + "");
        scaleYField.setText(trsBuilder.getScaleY() + "");
        transXField.setText(trsBuilder.getTranslateX() + "");
        transYField.setText(trsBuilder.getTranslateY() + "");
        rotateAngleField.setText(trsBuilder.getRotationAngle() + "");
    }

    private void setToIdentity() {
        scaleXField.setText("1.0");
        scaleYField.setText("1.0");
        shearXField.setText("0.0");
        shearYField.setText("0.0");
        transXField.setText("0.0");
        transYField.setText("0.0");
        rotateAngleField.setText("0.0");
    }

    private class OriginLLListener implements ActionListener {
        private boolean isLowerLeft;

        OriginLLListener(boolean isLowerLeft) {
            this.isLowerLeft = isLowerLeft;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            updateOriginLL(isLowerLeft);
        }
    }

    private class UpdateParamListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String errMsg = updateParams();
            if (errMsg != null) {
                JOptionPane
                        .showMessageDialog(
                                null,
                                errMsg,
                                I18N.get("jump.plugin.edit.AffineTransformationPlugIn.Control-Point-Error"),
                                JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class SetIdentityListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setToIdentity();
        }
    }

    @SuppressWarnings("deprecation")
    public void forceImageToWarp(PlugInContext context,
            AffineTransformation trans) throws Exception {
        try {
            JFileChooser fileChooser = GUIUtil
                    .createJFileChooserWithOverwritePrompting();
            fileChooser.setDialogTitle(SAVE);
            // fileChooser.setFileFilter(filter);
            if (PersistentBlackboardPlugIn.get(context.getWorkbenchContext())
                    .get(FILE_CHOOSER_DIRECTORY_KEY) != null) {
                fileChooser.setCurrentDirectory(new File(
                        (String) PersistentBlackboardPlugIn.get(
                                context.getWorkbenchContext()).get(
                                FILE_CHOOSER_DIRECTORY_KEY)));
            }
            File outFile = null;
            int option;
            option = fileChooser.showSaveDialog(context.getWorkbenchFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                outFile = fileChooser.getSelectedFile();
            }

            String filePath = outFile.getAbsolutePath();
            outFile = new File(filePath + ".png");
            Layer lyr = context.getLayerManager().getLayer(layerName);
            Envelope inEnvelope = new Envelope();
            // Get the bufferedImage from a ReferencedImage layer
            BufferedImage InImageBuffer = ImageryUtils
                    .getBufferFromReferenceImageLayer(lyr);
            // Ad alpha chanel
            InImageBuffer = ImageryUtils.addAlphaChannel(InImageBuffer);
            if (dialog.getBoolean(RESIZE_IMAGE)) {
                InImageBuffer = ImageryUtils.resizeImage(InImageBuffer,
                        InImageBuffer.getWidth() / 2,
                        InImageBuffer.getHeight() / 2);
            }
            inEnvelope.expandToInclude(lyr.getFeatureCollectionWrapper()
                    .getEnvelope());
            Geometry P0 = new GeometryFactory().createPoint(new Coordinate(
                    inEnvelope.getMinX(), inEnvelope.getMinY()));
            Geometry P1 = new GeometryFactory().createPoint(new Coordinate(
                    inEnvelope.getMaxX(), inEnvelope.getMinY()));
            Geometry P2 = new GeometryFactory().createPoint(new Coordinate(
                    inEnvelope.getMaxX(), inEnvelope.getMaxY()));
            Geometry P3 = new GeometryFactory().createPoint(new Coordinate(
                    inEnvelope.getMinX(), inEnvelope.getMaxY()));
            Geometry P0_ = trans.transform(P0);
            Geometry P1_ = trans.transform(P1);
            Geometry P2_ = trans.transform(P2);
            Geometry P3_ = trans.transform(P3);
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
            pb.addSource(InImageBuffer);
            pb.add(warp);
            pb.add(new InterpolationNearest());
            RenderedOp outputOp = JAI.create("warp", pb);
            BufferedImage outImageBuffer = outputOp.getAsBufferedImage();
            GeometryFactory gf = new GeometryFactory();
            final Geometry outGeometry = trans.transform(gf
                    .toGeometry(inEnvelope));
            // Get Envelope from out Geometry
            // outoutEnvelope to use for transformed Image
            final Envelope outEnvelope = outGeometry.getEnvelope()
                    .getEnvelopeInternal();
            // Set input raster layer to invisible
            lyr.setVisible(false);
            // Save output (affined transformed) image TIF file
            // String path = System.getProperty("java.io.tmpdir");
            // File outFile = new File(path.concat(File.separator)
            // .concat(lyr.getName()).concat(".tif"));
            // outImageBuffer = imageToBufferedImage(outImageBuffer);
            ImageryUtils.saveToPng(outFile, outImageBuffer);
            // RasterImageIOUtils.saveImage(outFile, "tif",
            // imageToBufferedImage(outImageBuffer), outEnvelope);
            WorldFileHandler worldFileHandler = new WorldFileHandler(
                    outFile.getAbsolutePath(), false);
            worldFileHandler.writeWorldFile(outEnvelope,
                    outImageBuffer.getWidth(), outImageBuffer.getHeight());
            Registry registry = context.getWorkbenchContext().getRegistry();
            @SuppressWarnings("unchecked")
            List<FileLayerLoader> loaders = registry
                    .getEntries(FileLayerLoader.KEY);
            FileLayerLoader loader = null;
            for (FileLayerLoader fileLayerLoader : loaders) {
                if (fileLayerLoader instanceof GeoImageFactoryFileLayerLoader)
                    loader = fileLayerLoader;
            }
            URI uri = outFile.toURI();
            Map<String, Object> dp = new HashMap<String, Object>();
            dp.put(DataSource.URI_KEY, outFile.toURI().toString());
            dp.put(DataSource.FILE_KEY, outFile);
            dp.put(ImageryLayerDataset.ATTR_TYPE, "tif");
            // dp.put(context.toString(), StandardCategoryNames.WORKING);
            loader.open(null, uri, dp);
        } catch (RuntimeException localRuntimeException) {
            JOptionPane.showMessageDialog(null, ALLOWED_IMAGES, null,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
