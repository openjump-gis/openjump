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
import java.util.ArrayList;
import java.util.List;


/**
 * Created by UMichael on 05/12/2015.
 */
public class MakeValidPlugIn extends AbstractThreadedUiPlugIn {

    public static String SOURCE_LAYER        = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.source-layer");
    public static String DESCRIPTION         = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.description");
    //public static String REMOVE_DUPLICATES   = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.remove-duplicates");
    public static String DECOMPOSE_MULTI     = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.decompose-multi");
    public static String RESULT_LAYER_SUFFIX = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.result-layer-suffix");
    public static String REMOVE_DEGENERATE_PARTS = I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn.remove-degenerate-parts");

    private Layer layerA;
    //private boolean removeDuplicates;
    private boolean decomposeMulti;
    private boolean removeDegenerateParts;

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
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
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
        //final JCheckBox removeDuplicatesCB = dialog.addCheckBox(REMOVE_DUPLICATES, removeDuplicates, REMOVE_DUPLICATES);
        final JCheckBox decomposeMultiCB  =
                dialog.addCheckBox(DECOMPOSE_MULTI, decomposeMulti, DECOMPOSE_MULTI);
        final JCheckBox removeDegeneratePartsCB  =
                dialog.addCheckBox(REMOVE_DEGENERATE_PARTS, removeDegenerateParts, REMOVE_DEGENERATE_PARTS);

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layerA = dialog.getLayer(SOURCE_LAYER);
        //removeDuplicates = dialog.getBoolean(REMOVE_DUPLICATES);
        decomposeMulti = dialog.getBoolean(DECOMPOSE_MULTI);
        removeDegenerateParts = dialog.getBoolean(REMOVE_DEGENERATE_PARTS);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

        // Clone layerA
        FeatureCollection result1 = new FeatureDataset(layerA.getFeatureCollectionWrapper().getFeatureSchema());
        for (Object o : layerA.getFeatureCollectionWrapper().getFeatures()) {
            result1.add(((Feature)o).clone(true, true));
        }
        MakeValidOp makeValidOp = new MakeValidOp();
        for (Object o : result1.getFeatures()) {
            Feature feature = (Feature)o;
            Geometry validGeom = MakeValidOp.makeValid(feature.getGeometry());
            if (removeDegenerateParts) validGeom = removeDegenerateParts(feature.getGeometry(), validGeom);
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

    public static Geometry removeDegenerateParts(Geometry source, Geometry valid) {
        int sourceDim = source.getDimension();
        boolean isGeometryCollection = source.getClass().equals(com.vividsolutions.jts.geom.GeometryCollection.class);
        if (isGeometryCollection) return valid;
        List<Geometry> list = new ArrayList<Geometry>();
        for (int i = 0 ; i < valid.getNumGeometries() ; i++) {
            if (valid.getGeometryN(i).getDimension() < sourceDim) continue;
            else list.add(valid.getGeometryN(i));
        }
        return source.getFactory().buildGeometry(list);
    }

}
