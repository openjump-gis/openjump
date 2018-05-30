package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.util.LineStringExtracter;
import com.vividsolutions.jts.geom.util.PointExtracter;
import com.vividsolutions.jts.geom.util.PolygonExtracter;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
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
import java.util.Collections;
import java.util.List;

/**
 * PlugIn to erase geometries of a layer with the geometries from another layer
 */
public class EraseLayerAWithLayerBPlugIn extends AbstractThreadedUiPlugIn {

    public static String LAYER_A             = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.source-layer");
    public static String LAYER_B             = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.eraser-layer");
    public static String DESCRIPTION         = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.description");
    public static String SHOW_NEW_VERTICES   = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.show-new-vertices");
    public static String DECOMPOSE_MULTI     = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.decompose-multi");
    public static String MINUS               = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.minus");
    public static String VERTEX_LAYER_SUFFIX = I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn.vertex-layer-suffix");

    private Layer layerA;
    private Layer layerB;
    private boolean showNewVertices;
    private boolean decomposeMulti;

    public EraseLayerAWithLayerBPlugIn() {
    }

    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.tools.EraseLayerAWithLayerBPlugIn");
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
        return new MultiEnableCheck().add(checkFactory.createAtLeastNLayersMustExistCheck(2));
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
        Layer candidateB = layerB == null ? context.getCandidateLayer(1) : layerB;
        final JComboBox layerComboBoxA    = dialog.addLayerComboBox(LAYER_A, candidateA, context.getLayerManager());
        final JComboBox layerComboBoxB    = dialog.addLayerComboBox(LAYER_B, candidateB, context.getLayerManager());
        final JCheckBox showNewVerticesCB = dialog.addCheckBox(SHOW_NEW_VERTICES, showNewVertices, SHOW_NEW_VERTICES);
        final JCheckBox decomposeMultiCB  = dialog.addCheckBox(DECOMPOSE_MULTI, decomposeMulti, DECOMPOSE_MULTI);

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layerA = dialog.getLayer(LAYER_A);
        layerB = dialog.getLayer(LAYER_B);
        showNewVertices = dialog.getBoolean(SHOW_NEW_VERTICES);
        decomposeMulti = dialog.getBoolean(DECOMPOSE_MULTI);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

        // index layerB
        STRtree index = new STRtree();
        for (Object o : layerB.getFeatureCollectionWrapper().getFeatures()) {
            Geometry g = ((Feature)o).getGeometry();
            index.insert(g.getEnvelopeInternal(), g);
        }



        // Clone layerA
        FeatureCollection result1 = new FeatureDataset(layerA.getFeatureCollectionWrapper().getFeatureSchema());
        for (Object o : layerA.getFeatureCollectionWrapper().getFeatures()) {
            Feature newFeature = ((Feature)o).clone(false, true);
            newFeature.setGeometry(getHomogeneousGeometry(newFeature.getGeometry()));
            result1.add(newFeature);
        }
        for (Object o : result1.getFeatures()) {
            Feature feature = (Feature)o;
            List<Geometry> candidates = index.query(feature.getGeometry().getEnvelopeInternal());
            for (Geometry b : candidates) {
                feature.setGeometry(getHomogeneousGeometry(erase(
                        feature.getGeometry(),
                        getHomogeneousGeometry(b)
                )));
            }
        }
        FeatureCollection result2 = new FeatureDataset(result1.getFeatureSchema());
        for (Object o : result1.getFeatures()) {
            Geometry geometry = ((Feature)o).getGeometry();
            if (!geometry.isEmpty()) {
                if (geometry instanceof GeometryCollection && decomposeMulti) {
                    for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
                        Feature f = ((Feature)o).clone(false, false);
                        f.setGeometry(geometry.getGeometryN(i));
                        result2.add(f);
                    }
                } else {
                    result2.add((Feature)o);
                }
            }
        }
        workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                layerA.getName() + " " + MINUS + " " + layerB.getName(), result2);

        if (showNewVertices) {
            FeatureSchema schemaVertices = new FeatureSchema();
            schemaVertices.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
            schemaVertices.addAttribute("fid", AttributeType.INTEGER);
            FeatureCollection fcVertices = new FeatureDataset(schemaVertices);
            List cList = new ArrayList();
            for (Object o : layerA.getFeatureCollectionWrapper().getFeatures()) {
                Geometry g = ((Feature)o).getGeometry();
                for (Coordinate c : g.getCoordinates()) {
                    cList.add(c);
                }
            }
            Collections.sort(cList);
            GeometryFactory gf = new GeometryFactory();
            for (Object o : result2.getFeatures()) {
                Geometry g = ((Feature)o).getGeometry();
                for (Coordinate c : g.getCoordinates()) {
                    if (Collections.binarySearch(cList, c) < 0) {
                        Feature f = new BasicFeature(schemaVertices);
                        f.setGeometry(gf.createPoint(c));
                        f.setAttribute("fid", ((Feature)o).getID());
                        fcVertices.add(f);
                    }
                }
            }
            workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                    layerA.getName() + " - " + VERTEX_LAYER_SUFFIX, fcVertices);
        }
        layerA.setVisible(false);
    }

    private Geometry erase(Geometry a, Geometry b) {
        return a.difference(b);
    }

    private Geometry getHomogeneousGeometry(Geometry geom) {
        if (geom.isEmpty()) return geom;
        int dim = geom.getDimension();
        List list = new ArrayList();
        switch(dim) {
            case 0 :
                PointExtracter.getPoints(geom, list);
                break;
            case 1 :
                LineStringExtracter.getLines(geom, list);
                break;
            case 2 :
                PolygonExtracter.getPolygons(geom, list);
                break;
            default :
                return geom;
        }
        return geom.getFactory().buildGeometry(list);
    }

}
