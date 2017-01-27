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
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

public class DiffGeometryComponents {

  private FeatureCollection[] inputFC = new FeatureCollection[2];
  private TaskMonitor monitor;
  private DiffGeometryMatcher diffMatcher = new ExactGeometryMatcher();
  private boolean splitIntoComponents = true;
  
  final static String sMatchingfeatures = I18N.get("com.vividsolutions.jump.qa.diff.DiffGeometry.Matching-features");
  final static String sGeometries = I18N.get("com.vividsolutions.jump.qa.diff.DiffGeometryComponents.geometries");

  public DiffGeometryComponents(FeatureCollection fc0,
                                FeatureCollection fc1,
                                TaskMonitor monitor)
  {
    inputFC[0] = fc0;
    inputFC[1] = fc1;
    this.monitor = monitor;
  }

  public void setNormalize(boolean normalizeGeometry)
  {
    diffMatcher = new NormalizedExactGeometryMatcher();
  }

  public void setSplitIntoComponents(boolean splitIntoComponents)
  {
    this.splitIntoComponents = splitIntoComponents;
  }

  public void setMatcher(DiffGeometryMatcher diffMatcher)
  {
    this.diffMatcher = diffMatcher;
  }

  public FeatureCollection[] diff()
  {
    MatchCollection[] mc = {
      new MatchCollection(inputFC[0], splitIntoComponents),
      new MatchCollection(inputFC[1], splitIntoComponents) };
    compute(mc[0], mc[1]);

    return new FeatureCollection[] {
      mc[0].getUnmatchedFeatures(),
      mc[1].getUnmatchedFeatures() };

  }

  private void compute(MatchCollection mc0, MatchCollection mc1)
  {
    MatchIndex index = new MatchIndex(mc1);

    monitor.report(sMatchingfeatures);
    FeatureCollection[] diffFC = new FeatureCollection[2];
    matchFeatures(mc0, index);

    // compute feature matches based on own geometries
    mc0.computeFeatureMatches();
    mc1.computeFeatureMatches();
    // compute matches based on matched geometries
    mc0.propagateUnmatchedFeatures();
    mc1.propagateUnmatchedFeatures();
  }

  private void matchFeatures(MatchCollection matchColl, MatchIndex index)
  {
    int count = 1;
    int totalItems = matchColl.geometrySize();
    for (Iterator i = matchColl.geometryIterator(); i.hasNext(); ) {
      monitor.report(count++, totalItems, sGeometries);
      MatchGeometry matchGeom = (MatchGeometry) i.next();
      index.testMatch(matchGeom, diffMatcher);
    }
  }

}
