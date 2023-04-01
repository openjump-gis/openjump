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
import org.locationtech.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;

public class DiffGeometry {

  private final static String FEATURES_PROCESSED = I18N.getInstance().get("jump.features-processed");

  private final FeatureCollection[] inputFC = new FeatureCollection[2];
  private final TaskMonitor monitor;
  private DiffGeometryMatcher diffMatcher = new ExactGeometryMatcher();
  private boolean splitIntoComponents = false;

  public DiffGeometry(FeatureCollection fc0, FeatureCollection fc1, TaskMonitor monitor) {
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
    return compute(inputFC[0], inputFC[1]);
  }


  private FeatureCollection[] compute(FeatureCollection fc0, FeatureCollection fc1) {
    DiffGeometryIndex diffIndex = new DiffGeometryIndex(fc1, diffMatcher, splitIntoComponents);

    monitor.report(I18N.getInstance().get("com.vividsolutions.jump.qa.diff.DiffGeometry.Matching-features"));
    FeatureCollection[] diffFC = new FeatureCollection[2];
    diffFC[0] = matchFeatures(fc0, diffIndex);
    diffFC[1] = new FeatureDataset(fc1.getFeatureSchema());
    diffFC[1].addAll(diffIndex.getUnmatchedFeatures());

    return diffFC;
  }

  private FeatureCollection matchFeatures(FeatureCollection fc0, DiffGeometryIndex diffIndex) {
    FeatureCollection noMatch = new FeatureDataset(fc0.getFeatureSchema());
    int count = 1;
    int totalItems = fc0.size();
    for (Feature f : fc0.getFeatures()) {

      monitor.report(count++, totalItems, FEATURES_PROCESSED);

      Geometry geom = f.getGeometry();

      Collection<Geometry> list = DiffGeometryIndex.splitGeometry(geom, splitIntoComponents);
      for (Geometry g : list) {
        if (! diffIndex.hasMatch(g))
          noMatch.add(f);
      }
    }
    return noMatch;
  }
}
