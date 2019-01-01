  package org.openjump.core.ui.plugin.edittoolbox.cursortools;
  
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.CoordinateListMetrics;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JOptionPane;

/*
 * Giuseppe Aruta Dec 4 2015
 * This tool partially derives from SplitPolygonPlugIn.class from Kosmo SAIG
 * It has been modified to be used both with selected Linestrings and Polygons.
 * It works only with one editable/selectable layer, already selected on layer list
 * A warning message will points, multigeometries or geometry collections are
 */
  
  
public class CutFeaturesTool extends MultiClickTool {

    public CutFeaturesTool(PlugInContext context) {
      setColor(Color.red);
      setStroke(new BasicStroke(1.5f,    // Width
              BasicStroke.CAP_SQUARE,    // End cap
              BasicStroke.JOIN_ROUND,    // Join style
              10.0f,                     // Miter limit
              new float[]{10.0f, 5.0f},  // Dash pattern
              0.0f));
      allowSnapping();
      setMetricsDisplay(new CoordinateListMetrics());
    }

    public Icon getIcon() {
      return IconLoader.icon("splitPolygon.png");
    }

    public String getName() {
      return I18N.get("org.openjump.core.ui.plugin.tools.CutFeaturesTool");
    }

    public Cursor getCursor() {
      return createCursor(IconLoader.icon("splitPolygonCursor.png").getImage());
    }

    protected void gestureFinished() throws Exception {

      WorkbenchContext context = getWorkbench().getContext();
      Geometry geomSelected;
      Geometry cuttingLine;

      reportNothingToUndoYet();

      SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();

      for (Layer activeLayer : selectionManager.getLayersWithSelectedItems()) {
        if (!activeLayer.isEditable()) {
          JOptionPane.showMessageDialog(null,
                  I18N.getMessage("plugin.EnableCheckFactory.selected-items-layers-must-be-editable", 1),
                  I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), 1);
          return;
        }
      }

      for (Layer activeLayer : selectionManager.getLayersWithSelectedItems()) {
        activeLayer.getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
          List<Feature> addedFeatures = new ArrayList<>();
          List<Feature> removedFeatures = new ArrayList<>();
          if (activeLayer.isEditable()) {
            Collection<Feature> selectedFeatures = context.getLayerViewPanel()
                    .getSelectionManager().getFeaturesWithSelectedItems(activeLayer);
            //EditTransaction edtr = new EditTransaction(new ArrayList(), "cut polygon", activeLayer, true, true, context.getLayerViewPanel());
            for (Feature featureSelected : selectedFeatures) {
              geomSelected = featureSelected.getGeometry();
              cuttingLine = getLineString();
              if ((geomSelected.isEmpty())) {
                continue;
              }
              if (geomSelected.contains(cuttingLine)) {
                continue;
              }
              if ((geomSelected.getClass().getSimpleName().equals("GeometryCollection"))) {
                context.getWorkbench().getFrame().warnUser(
                        I18N.get("org.openjump.core.ui.plugin.tools.CutFeaturesTool.geometryCollection-cannot-be-processed"));
              }
              else if (cuttingLine.intersects(geomSelected)) {
                if (geomSelected instanceof Polygon || geomSelected instanceof MultiPolygon) {
                  removedFeatures.add(featureSelected);
                  //edtr.deleteFeature(featureSelected);
                  List<Geometry> div = splitPolygon(cuttingLine, geomSelected);
                  for (Geometry geom : div) {
                    Feature featureIntersect = featureSelected.clone(true);
                    FeatureUtil.copyAttributes(featureSelected, featureIntersect);
                    featureIntersect.setGeometry(geom);
                    //edtr.createFeature(featureIntersect);
                    addedFeatures.add(featureIntersect);
                  }
                } else if (geomSelected instanceof LineString || geomSelected instanceof MultiLineString) {
                  removedFeatures.add(featureSelected);
                  //edtr.deleteFeature(featureSelected);
                  List<Geometry> div = splitLines(cuttingLine, geomSelected);
                  for (Geometry geom : div) {
                    Feature featureIntersect = featureSelected.clone(true);
                    FeatureUtil.copyAttributes(featureSelected, featureIntersect);
                    featureIntersect.setGeometry(geom);
                    //edtr.createFeature(featureIntersect);
                    addedFeatures.add(featureIntersect);
                  }
                } else {
                  // Point, MultiPoint, GeometryCollection : don't modify the selected feature
                }
              } else {
                // No intersection : don't modify the selected feature
              }
            }
            context.getLayerViewPanel().getSelectionManager().unselectItems(activeLayer);
            //edtr.commit();
            //edtr.clearEnvelopeCaches();
            UndoableCommand command = createCommand(activeLayer, removedFeatures, addedFeatures);
            command.execute();
            activeLayer.getLayerManager().getUndoableEditReceiver().receive(command.toUndoableEdit());
          }
        }
        finally {
          activeLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
      }
      //}
    }

    private UndoableCommand createCommand(final Layer layer,
                                  final Collection<Feature> removedFeatures,
                                  final Collection<Feature> addedFeatures) {
      return new UndoableCommand(getName()) {
        public void execute() {
          layer.getFeatureCollectionWrapper().removeAll(removedFeatures);
          layer.getFeatureCollectionWrapper().addAll(addedFeatures);
        }

        public void unexecute() {
          layer.getFeatureCollectionWrapper().removeAll(addedFeatures);
          layer.getFeatureCollectionWrapper().addAll(removedFeatures);
        }
      };
    }

    protected boolean isRollingBackInvalidEdits(WorkbenchContext context) {
      return
              PersistentBlackboardPlugIn.get(context).get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
    }

    protected boolean checkLineString() throws NoninvertibleTransformException {
      if (getCoordinates().size() < 2) {

        getPanel().getContext().warnUser(
                I18N.get("ui.cursortool.editing.DrawLineString.the-linestring-must-have-at-least-2-points"));

        return false;
      }

      IsValidOp isValidOp = new IsValidOp(getLineString());

      if (!isValidOp.isValid()) {
        getPanel().getContext().warnUser(
                isValidOp.getValidationError().getMessage());


        if (PersistentBlackboardPlugIn.get(getWorkbench().getContext())
                .get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
          return false;
        }
      }

      return true;
    }

    private LineString getLineString() throws NoninvertibleTransformException {
      return
              new GeometryFactory().createLineString(toArray(getCoordinates()));
    }


    public static List<Geometry> splitPolygon(Geometry digitizedGeometry, Geometry geomSel)
            throws Exception {
      // Create linearNetwork with selected geometry linear components and digitized linestring
      List<Geometry> linearNetwork = new ArrayList<>();
      linearNetwork.addAll(LinearComponentExtracter.getLines(digitizedGeometry));
      linearNetwork.addAll(LinearComponentExtracter.getLines(geomSel));
      Geometry nodedLinearNetwork = UnaryUnionOp.union(linearNetwork);

      // Polygonize
      Polygonizer polygonizer = new Polygonizer();
      polygonizer.add(nodedLinearNetwork);
      Collection<Geometry> polys = polygonizer.getPolygons();

      // To select new polygons matching original polygon
      // use a buffer (buffer size is relative to coordinates to be
      // able to manage geographic as well as projected coordinates)
      Envelope env = geomSel.getEnvelopeInternal();
      double maxAbsX = Math.max(Math.abs(env.getMaxX()), Math.abs(env.getMinX()));
      double maxAbsY = Math.max(Math.abs(env.getMaxY()), Math.abs(env.getMinY()));
      Geometry buffer = geomSel.buffer(Math.ulp(Math.max(maxAbsX, maxAbsY))*1000);
      Iterator<Geometry> it = polys.iterator();
      List<Geometry> result = new ArrayList<>();
      while (it.hasNext()) {
        Geometry geometry = it.next();
        if ((buffer.contains(geometry))) {
          result.add(geometry);
        }
      }
      if (result.size() == 1) {
        result.clear();
        result.add(geomSel);
      };
      return result;
    }

    public static List<Geometry> splitLines(Geometry digitizedGeometry, Geometry geomSel) throws Exception {
    // Create linearNetwork with selected geometry linear components and digitized linestring
      List<Geometry> linearNetwork = new ArrayList<>();
      linearNetwork.addAll(LinearComponentExtracter.getLines(digitizedGeometry));
      linearNetwork.addAll(LinearComponentExtracter.getLines(geomSel));
      Geometry nodedLinearNetwork = UnaryUnionOp.union(linearNetwork);

      // To select new polygons matching original polygon
      // use a buffer (buffer size is relative to coordinates to be
      // able to manage geographic as well as projected coordinates)
      Envelope env = geomSel.getEnvelopeInternal();
      double maxAbsX = Math.max(Math.abs(env.getMaxX()), Math.abs(env.getMinX()));
      double maxAbsY = Math.max(Math.abs(env.getMaxY()), Math.abs(env.getMinY()));
      Geometry buffer = geomSel.buffer(Math.ulp(Math.max(maxAbsX, maxAbsY))*1000);
      List<Geometry> result = new ArrayList<>();
      for (int i = 0 ; i < nodedLinearNetwork.getNumGeometries() ; i++) {
        Geometry geometry = nodedLinearNetwork.getGeometryN(i);
        if ((buffer.contains(geometry))) {
          result.add(geometry);
        }
      }
      return result;
    }
  
  
    public Layer getSelectedLayer() {
      Collection<Layer> editableLayers = getPanel().getLayerManager().getEditableLayers();
      
      if (editableLayers.isEmpty()) {
        return null;
      }
      return editableLayers.iterator().next();
    }
  }

