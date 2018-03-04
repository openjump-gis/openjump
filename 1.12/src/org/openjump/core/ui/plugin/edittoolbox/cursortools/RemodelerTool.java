package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import com.vividsolutions.jts.algorithm.distance.DiscreteHausdorffDistance;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.CoordinateListMetrics;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import org.openjump.core.ui.images.IconLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;
import java.util.List;

public class RemodelerTool extends MultiClickTool {

  public RemodelerTool() {
    setColor(Color.red);
    setStroke(new BasicStroke(1.5f, // Width
            BasicStroke.CAP_SQUARE,        // End cap
            BasicStroke.JOIN_ROUND,        // Join style
            10.0f,                // Miter limit
            new float[]{10.0f, 5.0f},      // Dash pattern
            0.0f));
    allowSnapping();
    setMetricsDisplay(new CoordinateListMetrics());
  }

  public Icon getIcon() {
    return IconLoader.icon("Remodeler.png");
  }

  public String getName() {
    return I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.RemodelerTool");
  }

  public Cursor getCursor() {
    return createCursor(com.vividsolutions.jump.workbench.ui.images.IconLoader
            .icon("splitPolygonCursor.png").getImage());
  }

  protected void gestureFinished() throws Exception {

    WorkbenchContext context = getWorkbench().getContext();
    Geometry geomSelected;
    LineString newPath;

    reportNothingToUndoYet();

    SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();

    for (Layer activeLayer : selectionManager.getLayersWithSelectedItems()) {
      if (!activeLayer.isEditable()) {
        JOptionPane.showMessageDialog(null,
                I18N.getMessage("plugin.EnableCheckFactory.selected-items-layers-must-be-editable", 1),
                I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), JOptionPane.WARNING_MESSAGE);
        return;
      }
    }

    for (Layer activeLayer : selectionManager.getLayersWithSelectedItems()) {
      activeLayer.getLayerManager().getUndoableEditReceiver().startReceiving();
      try {
        if (activeLayer.isEditable()) {
          Collection<Feature> selectedFeatures = context.getLayerViewPanel()
                  .getSelectionManager().getFeaturesWithSelectedItems(activeLayer);
          EditTransaction transaction = new EditTransaction(
                  new ArrayList(),
                  "Re-Model",
                  activeLayer,
                  true,
                  false,
                  context.getLayerViewPanel()
          );
          for (Feature featureSelected : selectedFeatures) {
            geomSelected = featureSelected.getGeometry();
            newPath = getLineString();
            Geometry newGeometry =geomSelected;
            if ((geomSelected.isEmpty())) {
              continue;
            }
            if (geomSelected.intersection(newPath).getNumPoints()<2) {
              continue;
            }
            if ((geomSelected.getClass().getSimpleName().equals("GeometryCollection"))) {
              context.getWorkbench().getFrame().warnUser(
                      I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.RemodelerTool.geometrycollection-cannot-be-processed"));
            }
            else if (newPath.intersects(geomSelected)) {
              if (geomSelected instanceof MultiPolygon) {
                Polygon[] polys = new Polygon[geomSelected.getNumGeometries()];
                for (int i = 0 ; i < geomSelected.getNumGeometries() ; i++) {
                  polys[i] = getNewPolygon((Polygon) geomSelected.getGeometryN(i), newPath);
                }
                newGeometry = geomSelected.getFactory().createMultiPolygon(polys);
              }
              else if (geomSelected instanceof Polygon) {
                newGeometry = getNewPolygon((Polygon)geomSelected, newPath);
              }
              else if (geomSelected instanceof MultiLineString) {
                LineString[] lines = new LineString[geomSelected.getNumGeometries()];
                for (int i = 0 ; i < geomSelected.getNumGeometries() ; i++) {
                  lines[i] = getNewLineString((LineString) geomSelected.getGeometryN(i), newPath);
                }
                newGeometry = geomSelected.getFactory().createMultiLineString(lines);
              }
              else if (geomSelected instanceof LineString) {
                newGeometry = getNewLineString((LineString) geomSelected, newPath);
              }
              transaction.modifyFeatureGeometry(featureSelected, newGeometry);
              featureSelected.setGeometry(newGeometry);
            }
            //else {// No intersection : don't modify the selected feature}
          }
          transaction.commit();
          activeLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
      }
      finally {
        activeLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
      }
    }
  }


  private Polygon getNewPolygon(Polygon selection, LineString newPath) {
    List<LinearRing> rings = new ArrayList<>();
    rings.add((LinearRing)selection.getExteriorRing());
    for (int i = 0 ; i < selection.getNumInteriorRing() ; i++) {
      rings.add((LinearRing)selection.getInteriorRingN(i));
    }
    boolean intersected = false;
    for (int i = 0 ; i < newPath.getNumPoints()-1 ; i++) {
      LineString segment = selection.getFactory()
              .createLineString(new Coordinate[]{
                      newPath.getCoordinateN(i),
                      newPath.getCoordinateN(i+1)});
      if (segment.intersects(selection.getExteriorRing())) {
        rings.set(0, getNewLinearRing((LinearRing)selection.getExteriorRing(), newPath));
        break;
      }
      for (int j = 0 ; j < selection.getNumInteriorRing() ; j++) {
        if (segment.intersects(selection.getInteriorRingN(j))) {
          rings.set(j+1, getNewLinearRing((LinearRing)selection.getInteriorRingN(j), newPath));
          intersected = true;
          break;
        }
        if (intersected) break;
      }
    }
    LinearRing ext = rings.get(0);
    rings.remove(0);
    LinearRing[] holes = rings.toArray(new LinearRing[0]);
    return selection.getFactory().createPolygon(ext, holes);
  }

  private LinearRing getNewLinearRing(LinearRing selection, LineString newPath) {

    newPath = clipNewPath(selection, newPath);

    // Compute the location of the insertion points in selection
    LocationIndexedLine selectionIndexedLine = new LocationIndexedLine(selection);
    LinearLocation loc1 = selectionIndexedLine.indexOf(newPath.getStartPoint().getCoordinate());
    LinearLocation loc2 = selectionIndexedLine.indexOf(newPath.getEndPoint().getCoordinate());
    //boolean direct = loc1.compareTo(loc2) <= 0;

    // Z-interpolation
    newPath.getPointN(0).getCoordinate().z = interpolateZ(loc1,selection);
    newPath.getPointN(newPath.getNumPoints()-1).getCoordinate().z = interpolateZ(loc2,selection);
    interpolateZ(newPath);
    // end of z-interpolation

    LinearLocation locMin = loc1.compareTo(loc2) <= 0 ? loc1 : loc2;
    LinearLocation locMax = loc1.compareTo(loc2) <= 0 ? loc2 : loc1;

    CoordinateList list = new CoordinateList();
    list.add(selectionIndexedLine.extractLine(locMax, selectionIndexedLine.getEndIndex()).getCoordinates(), false);
    list.add(selectionIndexedLine.extractLine(selectionIndexedLine.getStartIndex(), locMin).getCoordinates(), false);
    LineString subLinePassingThrough0 = selection.getFactory().createLineString(list.toCoordinateArray());
    LineString oppositeSubLine = (LineString)selectionIndexedLine.extractLine(locMin, locMax);

    double d1 = DiscreteHausdorffDistance.distance(newPath, subLinePassingThrough0);
    double d2 = DiscreteHausdorffDistance.distance(newPath, oppositeSubLine);

    if (d1 < d2) {
      // subLinePathThrough0 is replaced by newPath
      if (locMin == loc1) newPath = (LineString)newPath.reverse();

      list = new CoordinateList();
      for (int i = 0 ; i < oppositeSubLine.getNumPoints()-1 ; i++) {
        list.add(oppositeSubLine.getCoordinateN(i), false);
      }
      for (int i = 0 ; i < newPath.getNumPoints()-1 ; i++) {
        list.add(newPath.getCoordinateN(i), false);
      }
      list.closeRing();
      return selection.getFactory().createLinearRing(list.toCoordinateArray());
    }
    else {
      // oppositeSubLine is replaced by newPath
      if (locMin == loc2) newPath = (LineString)newPath.reverse();

      list = new CoordinateList();
      for (int i = 0 ; i < subLinePassingThrough0.getNumPoints()-1 ; i++) {
        list.add(subLinePassingThrough0.getCoordinateN(i), false);
      }
      for (int i = 0 ; i < newPath.getNumPoints()-1 ; i++) {
        list.add(newPath.getCoordinateN(i), false);
      }
      list.closeRing();
      return selection.getFactory().createLinearRing(list.toCoordinateArray());
    }
  }


  private LineString getNewLineString(LineString selection, LineString newPath){

    newPath = clipNewPath(selection, newPath);

    // Compute the location of the insertion points in selection
    LocationIndexedLine selectionIndexedLine = new LocationIndexedLine(selection);
    LinearLocation loc1 = selectionIndexedLine.indexOf(newPath.getStartPoint().getCoordinate());
    LinearLocation loc2 = selectionIndexedLine.indexOf(newPath.getEndPoint().getCoordinate());
    boolean direct = loc1.compareTo(loc2) <= 0;

    // Compute the new geometry
    CoordinateList list = new CoordinateList();
    list.add(selectionIndexedLine.extractLine(selectionIndexedLine.getStartIndex(),
            (direct?loc1:loc2)).getCoordinates(), false);
    if (!direct) {
      newPath = (LineString)newPath.reverse();
    }
    // Z-interpolation
    newPath.getPointN(0).getCoordinate().z = interpolateZ(direct?loc1:loc2,selection);
    newPath.getPointN(newPath.getNumPoints()-1).getCoordinate().z = interpolateZ(direct?loc2:loc1,selection);
    interpolateZ(newPath);
    // end of z-interpolation
    for (int i = 1 ; i < newPath.getNumPoints()-1 ; i++) {
      list.add(newPath.getCoordinateN(i), false);
    }
    list.add(selectionIndexedLine.extractLine((direct?loc2:loc1),
            selectionIndexedLine.getEndIndex()).getCoordinates(), false);
    return selection.getFactory().createLineString(list.toCoordinateArray());
  }


  private LineString clipNewPath(LineString selection, LineString newPath) {
    Coordinate c1 = firstIntersectionAlongNewPath(selection, newPath);
    Coordinate c2 = firstIntersectionAlongNewPath(selection, (LineString)newPath.reverse());
    // Extract the useful part of newPath
    LocationIndexedLine pathIndexedLine = new LocationIndexedLine(newPath);
    return (LineString)pathIndexedLine.extractLine(
            pathIndexedLine.indexOf(c1), pathIndexedLine.indexOf(c2));
  }


  // Walk along newPath from the start point and findthe first intersection with selection
  private Coordinate firstIntersectionAlongNewPath(LineString selection, LineString newPath) {
    for (int i = 0 ; i < newPath.getNumPoints()-1 ; i++) {
      LineSegment newPathSegment = new LineSegment(newPath.getCoordinateN(i), newPath.getCoordinateN(i+1));
      double minDist = Double.MAX_VALUE;
      Coordinate intersection = null;
      for (int j = 0 ; j < selection.getNumPoints()-1 ; j++) {
        LineSegment selectionSegment = new LineSegment(selection.getCoordinateN(j), selection.getCoordinateN(j+1));
        Coordinate newIntersection = newPathSegment.intersection(selectionSegment);
        if (newIntersection != null) {
          double dist = newIntersection.distance(newPathSegment.getCoordinate(0));
          if (dist < minDist) {
            minDist = dist;
            intersection = newIntersection;
          }
        }
      }
      if (intersection != null) return intersection;
    }
    return null;
  }


  protected boolean isRollingBackInvalidEdits(WorkbenchContext context) {
    return PersistentBlackboardPlugIn.get(context)
            .get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
  }


  private LineString getLineString() throws NoninvertibleTransformException {
    return new GeometryFactory().createLineString(toArray(getCoordinates()));
  }

  private double interpolateZ(LinearLocation loc, LineString lineString) {
    if (loc.getSegmentFraction()==0.0) {
      return lineString.getPointN(loc.getSegmentIndex()).getCoordinate().z;
    } else {
      double previousZ = lineString.getPointN(loc.getSegmentIndex()).getCoordinate().z;
      double nextZ = lineString.getPointN(loc.getSegmentIndex()+1).getCoordinate().z;
      if (Double.isNaN(previousZ) && Double.isNaN(nextZ)) return Double.NaN;
      else if (Double.isNaN(previousZ)) return nextZ;
      else if (Double.isNaN(nextZ)) return previousZ;
      else {
        return previousZ + (nextZ-previousZ)*loc.getSegmentFraction();
      }
    }
  }

  private void interpolateZbetweenIndices(LineString lineString, int start, int end) {
    double zi = lineString.getCoordinateN(start).z;
    double zj = lineString.getCoordinateN(end).z;
    if (Double.isNaN(zi) || Double.isNaN(zj)) return;
    double totalLength = 0;
    for (int i = start ; i < end ; i++) {
      totalLength += lineString.getPointN(i).distance(lineString.getPointN(i+1));
    }
    double dz = zj-zi;
    double partialLength = 0;
    for (int i = start+1 ; i < end ; i++) {
      partialLength += lineString.getPointN(i).distance(lineString.getPointN(i+1));
      lineString.getPointN(i).getCoordinate().z = zi + dz * (partialLength/totalLength);
    }
  }

  private void interpolateZ(LineString lineString) {
    int start = -1;
    for (int i = 0 ; i < lineString.getNumPoints() ; i++) {
      if (!Double.isNaN(lineString.getPointN(i).getCoordinate().z)) {
        if (start==-1) start = i;
        else {
          interpolateZbetweenIndices(lineString, start, i);
          start = -1;
        }
      }
    }
  }

}
