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

package com.vividsolutions.jump.qa.diff;

import java.util.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.geom.*;
import com.vividsolutions.jump.algorithm.VertexHausdorffDistance;

public class UniqueSegmentsWithToleranceFinder {

  public static double maximumDistance(LineSegment seg1, LineSegment seg2) {

    double dist;
    dist = seg1.p0.distance(seg2.p0);
    double maxDist = dist;

    dist = seg1.p0.distance(seg2.p1);
    if (dist > maxDist) maxDist = dist;

    dist = seg1.p1.distance(seg2.p0);
    if (dist > maxDist) maxDist = dist;

    dist = seg1.p1.distance(seg2.p1);
    if (dist > maxDist) maxDist = dist;

    return maxDist;
  }

  private FeatureCollection queryFC;
  private SegmentIndex segIndex;
  private List<LineSegment> resultSegs = new ArrayList<>();
  private Envelope queryEnv = new Envelope();

  public UniqueSegmentsWithToleranceFinder(FeatureCollection fc0, FeatureCollection fc1) {
    queryFC = fc0;
    segIndex = new SegmentIndex(fc1);
  }

  public List<LineSegment> findUniqueSegments(double tolerance) {
    for (Iterator it = queryFC.iterator(); it.hasNext(); ) {
      Feature f = (Feature) it.next();
      Geometry geom = f.getGeometry();
      findUniqueSegments(geom, tolerance);
    }
    return resultSegs;
  }

  public void findUniqueSegments(Geometry geom, double tolerance) {
    List<Coordinate[]> coordArrays = CoordinateArrays.toCoordinateArrays(geom, false);
    for (Coordinate[] coordArray : coordArrays) {
      findUniqueSegments(coordArray, tolerance);
    }
  }

  public void findUniqueSegments(Coordinate[] coord, double tolerance) {
    for (int i = 0; i < coord.length - 1; i++) {
      LineSegment querySeg = new LineSegment(coord[i], coord[i + 1]);
      querySeg.normalize();
      queryEnv.init(querySeg.p0, querySeg.p1);
      Envelope queryEnvExp = EnvelopeUtil.expand(queryEnv, tolerance);
      List testSegs = segIndex.query(queryEnvExp);
      if (! hasSegmentWithinTolerance(querySeg, testSegs, tolerance))
        resultSegs.add(querySeg);
    }
  }

  private boolean hasSegmentWithinTolerance(LineSegment querySeg, List testSegs, double tolerance) {
    for (Iterator i = testSegs.iterator(); i.hasNext(); ) {
      LineSegment testSeg = (LineSegment) i.next();
      VertexHausdorffDistance vhd = new VertexHausdorffDistance(querySeg, testSeg);
      if (vhd.distance() < tolerance)
        return true;
    }
    return false;
  }

}
