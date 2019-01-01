package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.geom.MakeValidOp;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.renderer.style.RingVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.util.ArrayList;


/**
 * A plugIn to repair invalid geometries
 */
public class MakeValidPlugIn extends AbstractThreadedUiPlugIn {

    private final static String KEY = MakeValidPlugIn.class.getName();

    public static String SOURCE_LAYER              = I18N.get(KEY + ".source-layer");
    public static String DESCRIPTION               = I18N.get(KEY + ".description");
    public static String RESULT_LAYER_SUFFIX       = I18N.get(KEY + ".result-layer-suffix");

    public static String PRESERVE_GEOM_DIM         = I18N.get(KEY + ".preserve-geom-dim");
    public static String PRESERVE_GEOM_DIM_TOOLTIP = I18N.get(KEY + ".preserve-geom-dim-tooltip");

    public static String REMOVE_DUPLICATE_COORD    = I18N.get(KEY + ".remove-duplicate-coord");
    public static String REMOVE_DUPLICATE_COORD_TOOLTIP = I18N.get(KEY + ".remove-duplicate-coord-tooltip");

    public static String DECOMPOSE_MULTI           = I18N.get(KEY + ".decompose-multi");
    public static String DECOMPOSE_MULTI_TOOLTIP   = I18N.get(KEY + ".decompose-multi-tooltip");

    public static String CORRECT_CURRENT_LAYER     = I18N.get(KEY + ".correct-current-layer");
    public static String CORRECT_CURRENT_LAYER_TOOLTIP = I18N.get(KEY + ".correct-current-layer-tooltip");
    public static String CREATE_NEW_LAYER          = I18N.get(KEY + ".create-new-layer");
    public static String CREATE_NEW_LAYER_TOOLTIP  = I18N.get(KEY + ".create-new-layer-tooltip");
    public static String ERROR_LAYER_SUFFIX        = I18N.get(KEY + ".error-layer-suffix");

    private Layer layerA;
    private boolean preserveGeomDim = true;
    //private boolean preserveCoordDim = true;
    private boolean removeDuplicateCoord = true;
    private boolean decomposeMulti = false;
    private boolean correctCurrentLayer = false;
    private boolean createNewLayer = true;

    public MakeValidPlugIn() {
    }

    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn");
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(
                this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_QA},
                getName() + "...", false, null,
                createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), getName(), true);
        initDialog(dialog, context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
        dialog.setSideBarDescription(DESCRIPTION);
        Layer candidateA = layerA == null ? context.getCandidateLayer(0) : layerA;
        final JComboBox layerComboBoxA    =
                dialog.addLayerComboBox(SOURCE_LAYER, candidateA, context.getLayerManager());
        final JCheckBox preserveGeomDimCB =
                dialog.addCheckBox(PRESERVE_GEOM_DIM, preserveGeomDim, PRESERVE_GEOM_DIM_TOOLTIP);
        //final JCheckBox preserveCoordDimCB =
        //        dialog.addCheckBox(PRESERVE_COORD_DIM, preserveCoordDim, PRESERVE_COORD_DIM_TOOLTIP);
        final JCheckBox removeDuplicateCoordCB =
              dialog.addCheckBox(REMOVE_DUPLICATE_COORD, removeDuplicateCoord, REMOVE_DUPLICATE_COORD);
        final JCheckBox decomposeMultiCB =
                dialog.addCheckBox(DECOMPOSE_MULTI, decomposeMulti, DECOMPOSE_MULTI_TOOLTIP);
        final JRadioButton correctCurrentLayerRB =
                dialog.addRadioButton(CORRECT_CURRENT_LAYER, "MODE", correctCurrentLayer, CORRECT_CURRENT_LAYER_TOOLTIP);
        final JRadioButton createNewLayerRB =
                dialog.addRadioButton(CREATE_NEW_LAYER, "MODE", createNewLayer, CREATE_NEW_LAYER_TOOLTIP);

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layerA = dialog.getLayer(SOURCE_LAYER);
        preserveGeomDim = dialog.getBoolean(PRESERVE_GEOM_DIM);
        //preserveCoordDim = dialog.getBoolean(PRESERVE_COORD_DIM);
        removeDuplicateCoord = dialog.getBoolean(REMOVE_DUPLICATE_COORD);
        decomposeMulti = dialog.getBoolean(DECOMPOSE_MULTI);
        correctCurrentLayer = dialog.getBoolean(CORRECT_CURRENT_LAYER);
        createNewLayer = dialog.getBoolean(CREATE_NEW_LAYER);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

        MakeValidOp makeValidOp = new MakeValidOp();
        makeValidOp.setPreserveGeomDim(preserveGeomDim);
        //makeValidOp.setPreserveCoordDim(preserveCoordDim);
        makeValidOp.setPreserveDuplicateCoord(!removeDuplicateCoord);

        if (correctCurrentLayer) {
            correctCurrentLayer(context, makeValidOp);
        } else if (createNewLayer) {
            createNewLayer(context, makeValidOp);
        } else {
            assert true : "Should never reach here !";
        }

        /*
        // Clone layerA
        FeatureCollection result1 = new FeatureDataset(layerA.getFeatureCollectionWrapper().getFeatureSchema());
        for (Object o : layerA.getFeatureCollectionWrapper().getFeatures()) {
            result1.add(((Feature)o).clone(true, true));
        }
        MakeValidOp makeValidOp = new MakeValidOp();
        makeValidOp.setPreserveGeomDim(preserveGeomDim);
        //makeValidOp.setPreserveCoordDim(preserveCoordDim);
        makeValidOp.setPreserveDuplicateCoord(!removeDuplicateCoord);
        for (Object o : result1.getFeatures()) {
            Feature feature = (Feature)o;
            Geometry validGeom = makeValidOp.makeValid(feature.getGeometry());
            feature.setGeometry(validGeom);
        }
        if (decomposeMulti) {
            FeatureCollection result2 = new FeatureDataset(result1.getFeatureSchema());
            for (Object o : result1.getFeatures()) {
                Geometry geometry = ((Feature) o).getGeometry();
                if (!geometry.isEmpty()) {
                    if (geometry instanceof GeometryCollection) {
                        for (int i = 0; i < geometry.getNumGeometries(); i++) {
                            Feature f = ((Feature) o).clone(false, false);
                            f.setGeometry(geometry.getGeometryN(i));
                            result2.add(f);
                        }
                    } else {
                        result2.add((Feature) o);
                    }
                }
            }
            workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                    layerA.getName() + " - " + RESULT_LAYER_SUFFIX, result2);
        } else {
            workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                    layerA.getName() + " - " + RESULT_LAYER_SUFFIX, result1);
        }
        */
    }

    private void correctCurrentLayer(PlugInContext context, MakeValidOp makeValidOp) {
        FeatureCollection fc = layerA.getFeatureCollectionWrapper();

        EditTransaction transaction = new EditTransaction(new ArrayList(),
                this.getName(), layerA,
                this.isRollingBackInvalidEdits(context), true,
                context.getWorkbenchFrame());

        FeatureSchema errorSchema = new FeatureSchema();
        errorSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        errorSchema.addAttribute("ERROR", AttributeType.STRING);
        errorSchema.addAttribute("SOURCE_FID", AttributeType.INTEGER);
        FeatureCollection errors = new FeatureDataset(errorSchema);

        for (Feature feature : fc.getFeatures()) {
            IsValidOp op = new IsValidOp(feature.getGeometry());
            if (!op.isValid()) {
                Feature error = new BasicFeature(errorSchema);
                error.setGeometry(feature.getGeometry().getFactory()
                        .createPoint(op.getValidationError().getCoordinate()));
                error.setAttribute("ERROR", op.getValidationError().getMessage());
                error.setAttribute("SOURCE_FID", feature.getID());
                errors.add(error);
                Geometry fixedGeometry = makeValidOp.makeValid(feature.getGeometry());
                if (decomposeMulti && fixedGeometry.getNumGeometries() > 1) {
                    transaction.deleteFeature(feature);
                    for (int i = 0 ; i < fixedGeometry.getNumGeometries() ; i++) {
                        Feature newFeature = feature.clone(false, false);
                        newFeature.setGeometry(fixedGeometry.getGeometryN(i));
                        transaction.createFeature(newFeature);
                    }
                } else {
                    transaction.modifyFeatureGeometry(feature, fixedGeometry);
                }
            }
        }

        workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                layerA.getName() + " - " + ERROR_LAYER_SUFFIX, errors);
        Layer errorLayer = workbenchContext.getLayerManager().getLayer(layerA.getName() + " - " + ERROR_LAYER_SUFFIX);
        errorLayer.removeStyle(errorLayer.getStyle(VertexStyle.class));
        errorLayer.addStyle(new RingVertexStyle());
        errorLayer.getBasicStyle().setLineWidth(4);
        errorLayer.getVertexStyle().setEnabled(true);

        transaction.commit();
    }

    private void createNewLayer(PlugInContext context, MakeValidOp makeValidOp) {
        // Clone layerA
        FeatureCollection result1 = new FeatureDataset(layerA.getFeatureCollectionWrapper().getFeatureSchema());
        for (Object o : layerA.getFeatureCollectionWrapper().getFeatures()) {
            result1.add(((Feature)o).clone(true, true));
        }
        for (Object o : result1.getFeatures()) {
            Feature feature = (Feature)o;
            Geometry validGeom = makeValidOp.makeValid(feature.getGeometry());
            feature.setGeometry(validGeom);
        }
        if (decomposeMulti) {
            FeatureCollection result2 = new FeatureDataset(result1.getFeatureSchema());
            for (Object o : result1.getFeatures()) {
                Geometry geometry = ((Feature) o).getGeometry();
                if (!geometry.isEmpty()) {
                    if (geometry instanceof GeometryCollection) {
                        for (int i = 0; i < geometry.getNumGeometries(); i++) {
                            Feature f = ((Feature) o).clone(false, false);
                            f.setGeometry(geometry.getGeometryN(i));
                            result2.add(f);
                        }
                    } else {
                        result2.add((Feature) o);
                    }
                }
            }
            workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                    layerA.getName() + " - " + RESULT_LAYER_SUFFIX, result2);
        } else {
            workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                    layerA.getName() + " - " + RESULT_LAYER_SUFFIX, result1);
        }
    }

}
