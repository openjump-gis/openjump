/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.edittoolbox.tab;

import java.awt.event.MouseEvent;
import java.util.List;

import org.openjump.core.geomutils.GeoUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.math.Vector2D;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * Constraint Manager
 * Improved by ioan on 2017-08-05 to honour IncrementalAngle constraint even on the first segment
 */
public class ConstraintManager {
  public static final String CONSTRAIN_LENGTH_ENABLED_KEY = "CONSTRAIN_LENGTH - ENABLED";
  public static final String CONSTRAIN_INCREMENTAL_ANGLE_ENABLED_KEY = "CONSTRAIN_INCREMENTAL_ANGLE - ENABLED";
  public static final String CONSTRAIN_ANGLE_ENABLED_KEY = "CONSTRAIN_ANGLE - ENABLED";
  public static final String LENGTH_CONSTRAINT_KEY = "LENGTH_CONSTRAINT";
  public static final String INCREMENTAL_ANGLE_SIZE_KEY = "INCREMENTAL_ANGLE_CONSTRAINT";
  public static final String ANGLE_SIZE_KEY = "ANGLE_CONSTRAINT";
  public static final String RELATIVE_ANGLE_KEY = "RELATIVE_ANGLE_CONSTRAINT";
  public static final String ABSOLUTE_ANGLE_KEY = "ABSOLUTE_ANGLE_CONSTRAINT";
  public static final String CONSTRAIN_RECTANGLE_RATIO_ENABLED_KEY = "CONSTRAIN_RECTANGLE_RATIO - ENABLED";
  public static final String RATIO_WIDTH_KEY = "RATIO_WIDTH_CONSTRAINT";
  public static final String RATIO_HEIGHT_KEY = "RATIO_HEIGHT_CONSTRAINT";
  protected LayerViewPanel panel;
  WorkbenchContext workbenchContext;

  public ConstraintManager(WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
  }

  public Coordinate constrain(LayerViewPanel panel, List coordinates,
      Coordinate targetPt, MouseEvent e) {
    if (coordinates == null)
      return targetPt;
    this.panel = panel;
    int numPts = coordinates.size();
    boolean shiftConstrain = (e.isShiftDown() && (numPts > 0)); // was 1
    boolean ctrlConstrain = (e.isControlDown() && (numPts > 1));
    Coordinate retPt = (Coordinate) targetPt.clone();

    if ((PersistentBlackboardPlugIn.get(workbenchContext)
        .get(CONSTRAIN_LENGTH_ENABLED_KEY, false)) && (numPts >= 1)) {
      double lengthConstraint = PersistentBlackboardPlugIn.get(workbenchContext)
          .getDouble(LENGTH_CONSTRAINT_KEY);
      if (lengthConstraint > 0) {
        Coordinate anchorPt = (Coordinate) coordinates.get(numPts - 1);
        double run = targetPt.x - anchorPt.x;
        double rise = targetPt.y - anchorPt.y;
        double prevLength = anchorPt.distance(targetPt);
        double t1 = Math.round(prevLength / lengthConstraint);
        double newLength = t1 * lengthConstraint;
        double ratio = 1;
        if (prevLength != 0) // this happens when mouse clicked on first point
                             // without mouse move
          ratio = newLength / prevLength;
        retPt.x = anchorPt.x + (ratio * run);
        retPt.y = anchorPt.y + (ratio * rise);
      }
    }

    if (PersistentBlackboardPlugIn.get(workbenchContext)
        .get(CONSTRAIN_INCREMENTAL_ANGLE_ENABLED_KEY, false)) {
      if (shiftConstrain) {
        int incrementalAngleConstraint = PersistentBlackboardPlugIn
            .get(workbenchContext).getInt(INCREMENTAL_ANGLE_SIZE_KEY);
        // If only one point, set startPt to next to last point
        Coordinate startPt = new Coordinate();
        if (numPts == 1)
          startPt = (Coordinate) coordinates.get(numPts - 1);
        else
          startPt = (Coordinate) coordinates.get(numPts - 2);
        Coordinate endPt = (Coordinate) coordinates.get(numPts - 1);
        retPt = constrainIncrementalAngle(startPt, endPt, retPt,
            incrementalAngleConstraint);
      }
    }

    if (PersistentBlackboardPlugIn.get(workbenchContext)
        .get(CONSTRAIN_ANGLE_ENABLED_KEY, false) && (numPts > 1)) {
      double theta = -PersistentBlackboardPlugIn.get(workbenchContext)
          .getDouble(ANGLE_SIZE_KEY);

      if (PersistentBlackboardPlugIn.get(workbenchContext)
          .getBoolean(RELATIVE_ANGLE_KEY)) {
        if (shiftConstrain) {
          Coordinate startPt = (Coordinate) coordinates.get(numPts - 2);
          Coordinate endPt = (Coordinate) coordinates.get(numPts - 1);
          double length = endPt.distance(retPt);
          Coordinate newPt = constructVector(startPt, endPt, length);
          retPt = GeoUtils.rotPt(newPt, endPt, theta);
        }
      }

      else // ABSOLUTE_ANGLE_KEY
      {
        if (e.isShiftDown() && (numPts >= 1)) {
          Coordinate startPt = (Coordinate) coordinates.get(numPts - 1);
          Coordinate endPt = (Coordinate) startPt.clone();
          endPt.x += startPt.distance(retPt);
          retPt = GeoUtils.rotPt(endPt, startPt, theta);
        }
      }
    }

    if (PersistentBlackboardPlugIn.get(workbenchContext)
        .get(CONSTRAIN_INCREMENTAL_ANGLE_ENABLED_KEY, false)) {
      if (ctrlConstrain) {
        int incrementalAngleConstraint = PersistentBlackboardPlugIn
            .get(workbenchContext).getInt(INCREMENTAL_ANGLE_SIZE_KEY);
        Coordinate startPt = (Coordinate) coordinates.get(1);
        Coordinate endPt = (Coordinate) coordinates.get(0);
        Coordinate p1 = (Coordinate) coordinates.get(numPts - 1);
        Coordinate p2 = (Coordinate) retPt.clone();
        Coordinate p3 = (Coordinate) coordinates.get(0);
        Coordinate p4 = constrainIncrementalAngle(startPt, endPt, retPt,
            incrementalAngleConstraint);
        Coordinate intxPt = GeoUtils.getIntersection(p1, p2, p3, p4);
        if (intxPt.z == 0) // z <> 0 means that the lines are parallel
          retPt = new Coordinate(intxPt.x, intxPt.y);
      }
    }
    return retPt;
  }

  protected Coordinate constrainIncrementalAngle(Coordinate startPt,
      Coordinate endPt, Coordinate targetPt, int angleConstraint) {

    // this version uses the JTS Vector class to make a unit vector along
    // the direction from the start to end point,
    // if there's only one point then start along the x angle
    Vector2D endVector = new Vector2D(endPt);
    // unit direction vector along vector defined by start->end
    Vector2D dirVect;
    if (!startPt.equals2D(endPt))
      dirVect = new Vector2D(startPt, endPt).normalize();
    else
      dirVect = new Vector2D(1, 0); // x axis

    Vector2D targetVect = new Vector2D(endPt, targetPt);
    double theta = 360 / angleConstraint;

    double currDist = Double.POSITIVE_INFINITY;
    Coordinate retPt = new Coordinate();
    Vector2D outVector;

    for (int i = 0; i < angleConstraint - 1; i++) {
      dirVect = dirVect.rotate(theta * Math.PI / 180);
      // use dot product to get component of target vector along the unit
      // direction vector
      double dotProduct = dirVect.dot(targetVect);
      // Take the dot product times the unit direction vector and
      // add it to the endpoint vector
      outVector = endVector.add(dirVect.multiply(dotProduct));
      double newDist = targetPt.distance(outVector.toCoordinate());
      if (newDist < currDist) {
        currDist = newDist;
        retPt = outVector.toCoordinate();
      }
    }
    return retPt;
  }

  /*
  public Coordinate constrainRectangleToRatio(LayerViewPanel panel,
      List coordinates, Coordinate targetPt, MouseEvent e)
      throws NoninvertibleTransformException {
    if (coordinates == null)
      return targetPt;
    this.panel = panel;
    boolean shiftConstrain = ((PersistentBlackboardPlugIn.get(workbenchContext)
        .get(CONSTRAIN_RECTANGLE_RATIO_ENABLED_KEY, false))
        && (e.isShiftDown()));

    double ratioWidth = PersistentBlackboardPlugIn.get(workbenchContext)
        .get(RATIO_WIDTH_KEY, 1.0);
    double ratioHeight = PersistentBlackboardPlugIn.get(workbenchContext)
        .get(RATIO_HEIGHT_KEY, 1.0);
    double ratio = ratioWidth / ratioHeight;

    if ((coordinates.size() >= 1) && (shiftConstrain)) {
      Coordinate firstCoordinate = (Coordinate) coordinates.get(0);
      double yLength = Math.abs((firstCoordinate.x - targetPt.x)) / ratio;
      if (targetPt.y > firstCoordinate.y)
        return new Coordinate(targetPt.x, firstCoordinate.y + yLength);
      else
        return new Coordinate(targetPt.x, firstCoordinate.y - yLength);
    }
    return targetPt;
  }
  */

  private Coordinate constructVector(Coordinate startPt, Coordinate endPt,
      double dist)
  // this routine will use startPt & endPt to determine the slope of the new
  // vector
  // it will calculate a new vector from endPt with the slope and dist
  {
    double run = endPt.x - startPt.x;
    double rise = endPt.y - startPt.y;
    double sideLength = startPt.distance(endPt);
    double ratio = dist / sideLength;
    return new Coordinate(endPt.x + (ratio * run), endPt.y + (ratio * rise));
  }

  public double getBearing(Coordinate startPt, Coordinate endPt)
  // return Bearing in degrees (-180 to +180) from startPt to endPt
  {
    Coordinate r = new Coordinate(endPt.x - startPt.x, endPt.y - startPt.y);
    double rMag = Math.sqrt(r.x * r.x + r.y * r.y);
    if (rMag == 0.0) {
      return 0.0;
    } else {
      double rCos = r.x / rMag;
      double rAng = Math.acos(rCos);

      if (r.y < 0.0)
        rAng = -rAng;
      return rAng * 360.0 / (2 * Math.PI);
    }
  }

}
