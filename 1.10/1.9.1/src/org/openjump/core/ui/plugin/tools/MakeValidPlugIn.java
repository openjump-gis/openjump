package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
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
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;


/**
 * A plugIn to repair invalid geometries
 */
public class MakeValidPlugIn extends AbstractThreadedUiPlugIn {

    public static String SOURCE_LAYER        = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.source-layer");
    public static String DESCRIPTION         = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.description");
    public static String RESULT_LAYER_SUFFIX = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.result-layer-suffix");

    public static String PRESERVE_GEOM_DIM =
            I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.preserve-geom-dim");
    public static String PRESERVE_GEOM_DIM_TOOLTIP =
            I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.preserve-geom-dim-tooltip");

    //public static String PRESERVE_COORD_DIM  =
    //        I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.preserve-coord-dim");
    //public static String PRESERVE_COORD_DIM_TOOLTIP  =
    //        I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.preserve-coord-dim-tooltip");

    public static String REMOVE_DUPLICATE_COORD =
            I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.remove-duplicate-coord");
    public static String REMOVE_DUPLICATE_COORD_TOOLTIP =
            I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.remove-duplicate-coord-tooltip");

    public static String DECOMPOSE_MULTI
            = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.decompose-multi");
    public static String DECOMPOSE_MULTI_TOOLTIP
            = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.decompose-multi-tooltip");

    private Layer layerA;
    private boolean preserveGeomDim = true;
    //private boolean preserveCoordDim = true;
    private boolean removeDuplicateCoord = true;
    private boolean decomposeMulti = false;

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
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY},
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

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layerA = dialog.getLayer(SOURCE_LAYER);
        preserveGeomDim = dialog.getBoolean(PRESERVE_GEOM_DIM);
        //preserveCoordDim = dialog.getBoolean(PRESERVE_COORD_DIM);
        removeDuplicateCoord = dialog.getBoolean(REMOVE_DUPLICATE_COORD);
        decomposeMulti = dialog.getBoolean(DECOMPOSE_MULTI);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

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
    }

}
