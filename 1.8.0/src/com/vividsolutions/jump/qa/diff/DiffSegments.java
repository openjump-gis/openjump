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
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * Finds all line segments in two
 * FeatureCollections which occur once only.
 */
public class DiffSegments {

  private FeatureCollection[] fc = new FeatureCollection[2];
  private UnmatchedEdgeExtracter[] uee = new UnmatchedEdgeExtracter[2];
  private TaskMonitor monitor;

  public DiffSegments(TaskMonitor monitor)
  {
    this.monitor = monitor;
  }

  public void setSegments(int index, FeatureCollection fc)
  {
    this.fc[index] = fc;
    uee[index] = new UnmatchedEdgeExtracter();
    for (Iterator it = fc.getFeatures().iterator(); it.hasNext(); ) {
      Feature f = (Feature) it.next();
      uee[index].add(f.getGeometry());
    }
  }

  /**
   * Returns all the subedges from fc which are unmatched.
   */
  public FeatureCollection computeDiffEdges(int index)
  {
    List diffEdges = new ArrayList();
    UnmatchedEdgeExtracter otherUee = uee[1 - index];
    for (Iterator i = fc[index].getFeatures().iterator(); i.hasNext(); ) {
      Feature f = (Feature) i.next();
      otherUee.getDiffEdges(f.getGeometry(), diffEdges);
    }
    return FeatureDatasetFactory.createFromGeometry(diffEdges);
  }

}
