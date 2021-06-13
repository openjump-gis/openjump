package com.vividsolutions.jump.plugin.edit;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;

import java.util.ArrayList;


public class GeometrySnapperPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

  private static final String LAYER = I18N.get("ui.GenericNames.LAYER");
  private static final String REF_LAYER = I18N.get("ui.GenericNames.REFERENCE_LAYER");
  private static final String TOLERANCE = I18N.get("ui.plugin.analysis.GeometrySnapperPlugIn.tolerance");
  private static final String AUTO_SNAP = I18N.get("ui.plugin.analysis.GeometrySnapperPlugIn.auto-snap");


  private String layer;
  private String refLayer;
  private double tolerance = 1E-12;
  private MultiInputDialog dialog;

  public String getName() {
    return I18N.get("ui.plugin.analysis.GeometrySnapperPlugIn.Geometry-Snapper");
  }

  public void initialize(PlugInContext context) throws Exception {
    FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    featureInstaller.addMainMenuPlugin(
        this,
        new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY},
        this.getName() + "...", false, null,
        createEnableCheck(context.getWorkbenchContext()));
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {
    dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) {
      return false;
    }
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    monitor.allowCancellationRequests();

    monitor.report(I18N.get("ui.plugin.analysis.GeometrySnapperPlugIn.snapping"));

    FeatureCollection fc = context.getLayerManager().getLayer(layer)
        .getFeatureCollectionWrapper();
    STRtree index = new STRtree();
    for (Feature f : context.getLayerManager().getLayer(refLayer)
        .getFeatureCollectionWrapper().getFeatures()) {
      Geometry geometry = f.getGeometry();
      for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
        Geometry g = geometry.getGeometryN(i);
        index.insert(g.getEnvelopeInternal(), g);
      }
    }

    EditTransaction transaction = new EditTransaction(new ArrayList(),
        this.getName(), context.getLayerManager().getLayer(layer),
        this.isRollingBackInvalidEdits(context), true,
        context.getWorkbenchFrame());

    for (Feature feature : fc.getFeatures()) {
      Envelope env = feature.getGeometry().getEnvelopeInternal();
      env.expandBy(tolerance);
      Geometry refGeom = feature.getGeometry().getFactory().buildGeometry(index.query(env));
      Geometry newGeom = new GeometrySnapper(feature.getGeometry()).snapTo(refGeom, tolerance);
      if (!newGeom.equals(feature.getGeometry())) {
        transaction.modifyFeatureGeometry(feature, newGeom);
      }
    }

    transaction.commit();
  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {

    dialog.setSideBarDescription(
        I18N.get("ui.plugin.analysis.GeometrySnapperPlugIn.description")
    );

    Layer layer1 = (layer == null || context.getLayerManager().getLayer(layer) == null) ?
        context.getCandidateLayer(0) : context.getLayerManager().getLayer(layer);
    Layer layer2 = (refLayer == null || context.getLayerManager().getLayer(refLayer) == null) ?
        (context.getLayerManager().getLayers().size() > 1 ?
            context.getCandidateLayer(1) : context.getCandidateLayer(0))
        : context.getLayerManager().getLayer(layer);

    dialog.addLayerComboBox(LAYER, layer1, context.getLayerManager());
    dialog.addLayerComboBox(REF_LAYER, layer2, context.getLayerManager());
    dialog.addDoubleField(TOLERANCE, tolerance, 12);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    layer = dialog.getLayer(LAYER).getName();
    refLayer = dialog.getLayer(REF_LAYER).getName();
    tolerance = dialog.getDouble(TOLERANCE);
  }
}
