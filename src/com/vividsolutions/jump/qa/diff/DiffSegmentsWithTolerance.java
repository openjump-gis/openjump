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


import com.vividsolutions.jump.feature.*;
import java.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.geom.LineSegmentUtil;

/**
 * <code>DiffEdges</code> find all line segments in two
 * FeatureCollections which occur once only.
 */
public class DiffSegmentsWithTolerance {
  private static GeometryFactory geomFactory = new GeometryFactory();

  private FeatureCollection[] inputFC = new FeatureCollection[2];
  private double tolerance;
  //private FeatureCollection[] fc = new FeatureCollection[2];
  private List diffGeom[] = new ArrayList[2];

  public DiffSegmentsWithTolerance(FeatureCollection fc0, FeatureCollection fc1, double tolerance)
  {
    inputFC[0] = fc0;
    inputFC[1] = fc1;
    this.tolerance = tolerance;
  }

  public FeatureCollection[] diff()
  {
    compute(inputFC[0], inputFC[1]);
    FeatureCollection[] diffFC = new FeatureCollection[2];
    diffFC[0] = FeatureDatasetFactory.createFromGeometry(diffGeom[0]);
    diffFC[1] = FeatureDatasetFactory.createFromGeometry(diffGeom[1]);
    return diffFC;
  }

  private void compute(FeatureCollection fc0, FeatureCollection fc1)
  {
    diffGeom[0] = findUniqueSegmentGeometries(fc0, fc1);
    diffGeom[1] = findUniqueSegmentGeometries(fc1, fc0);
  }
  private List findUniqueSegmentGeometries(FeatureCollection fc0, FeatureCollection fc1)
  {
    List segGeomList = new ArrayList();
    UniqueSegmentsWithToleranceFinder finder = new UniqueSegmentsWithToleranceFinder(fc0, fc1);
    List segs = finder.findUniqueSegments(tolerance);
    for (Iterator i = segs.iterator(); i.hasNext(); ) {
      LineSegment seg = (LineSegment) i.next();
      segGeomList.add(LineSegmentUtil.asGeometry(geomFactory, seg));
    }
    return segGeomList;
  }



}
