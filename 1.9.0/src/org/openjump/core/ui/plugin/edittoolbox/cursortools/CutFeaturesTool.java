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
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.CoordinateListMetrics;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

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
  
  
  
  public class CutFeaturesTool
    extends MultiClickTool {

    Geometry geomSelected = null;
    Geometry geomDraw = null;

//    public CutFeaturesTool(EnableCheckFactory checkFactory) {
//    }

    PlugInContext context;

    public CutFeaturesTool(PlugInContext context) {
      this.context = context;
      setColor(Color.red);
      setStroke(new BasicStroke(1.5f,                      // Width
              BasicStroke.CAP_SQUARE,    // End cap
              BasicStroke.JOIN_ROUND,    // Join style
              10.0f,                     // Miter limit
              new float[]{10.0f, 5.0f}, // Dash pattern
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


    protected void gestureFinished()
            throws Exception {
      WorkbenchContext context = getWorkbench().getContext();
      reportNothingToUndoYet();

      SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();

      for (Layer activeLayer : selectionManager.getLayersWithSelectedItems()) {
        if (!activeLayer.isEditable()) {
          JOptionPane.showMessageDialog(null,
                  I18N.getMessage("plugin.EnableCheckFactory.selected-items-layers-must-be-editable",
                          new Object[]{Integer.valueOf(1)}), I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"),
                  1);
          return;
        }
      }

      for (Layer activeLayer : selectionManager.getLayersWithSelectedItems()) {
        if (activeLayer.isEditable()) {
          Collection selectedFeatures = context.getLayerViewPanel()
                  .getSelectionManager().getFeaturesWithSelectedItems(activeLayer);
          EditTransaction edtr = new EditTransaction(new ArrayList(), "cut polygon", activeLayer, true, true, context.getLayerViewPanel());
          for (Iterator k = selectedFeatures.iterator(); k.hasNext(); ) {
            Feature featureSelected = (Feature) k.next();
            this.geomSelected = featureSelected.getGeometry();
            this.geomDraw = getLineString();
            if ((this.geomSelected.isEmpty())) {
              return;
            }
            if (this.geomSelected.contains(this.geomDraw)) {
              return;
            }
            if ((this.geomSelected.getClass().getSimpleName().equals("GeometryCollection"))) {
              context.getWorkbench().getFrame().warnUser(
                      I18N.get("org.openjump.core.ui.plugin.tools.CutFeaturesTool.geometryCollection-cannot-be-processed"));
            }
            else if (this.geomDraw.intersects(this.geomSelected)) {
              if (this.geomSelected instanceof Polygon || this.geomSelected instanceof MultiPolygon) {
                edtr.deleteFeature(featureSelected);
                List<Geometry> div = splitPolygon(this.geomDraw, this.geomSelected);
                for (Geometry geom : div) {
                  Feature featureIntersect = featureSelected.clone(true);
                  FeatureUtil.copyAttributes(featureSelected, featureIntersect);
                  featureIntersect.setGeometry(geom);
                  edtr.createFeature(featureIntersect);
                }
              } else if (this.geomSelected instanceof LineString || this.geomSelected instanceof MultiLineString) {
                edtr.deleteFeature(featureSelected);
                List<Geometry> div = splitLines(this.geomDraw, this.geomSelected);
                for (Geometry geom : div) {
                  Feature featureIntersect = featureSelected.clone(true);
                  FeatureUtil.copyAttributes(featureSelected, featureIntersect);
                  featureIntersect.setGeometry(geom);
                  edtr.createFeature(featureIntersect);
                }
              } else {
            	// Point, MultiPoint, GeometryCollection : don't modify the selected feature
              }
            } else {
              // No intersection : don't modify the selected feature
            }
          }
          context.getLayerViewPanel().getSelectionManager().unselectItems(activeLayer);
          edtr.commit();
          edtr.clearEnvelopeCaches();
        }
      }
      //}
    }

    protected boolean isRollingBackInvalidEdits(WorkbenchContext context) {
      return
              context.getWorkbench().getBlackboard().get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
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


        if (getWorkbench().getBlackboard().get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
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
      List<Geometry> linearNetwork = new ArrayList();
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
      List<Geometry> result = new ArrayList<Geometry>();
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
      List<Geometry> linearNetwork = new ArrayList();
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
      List<Geometry> result = new ArrayList<Geometry>();
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

