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
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.Geometry;

public class MatchCollection
{

  private FeatureCollection inputFC;
  private Collection matchFeatures = new ArrayList();
  private Collection matchGeometries = new ArrayList();

  public MatchCollection(FeatureCollection fc, boolean splitIntoComponents)
  {
    this.inputFC = fc;
    init(fc, splitIntoComponents);
  }

  private void init(FeatureCollection fc, boolean splitIntoComponents)
  {
    for (Iterator i = fc.iterator(); i.hasNext(); )
    {
      Feature feat = (Feature) i.next();
      MatchFeature matchFeat = new MatchFeature(feat);
      matchFeatures.add(matchFeat);
      Geometry geom = feat.getGeometry();
      Collection list = MatchGeometry.splitGeometry(geom, splitIntoComponents);
      for (Iterator j = list.iterator(); j.hasNext(); ) {
        Geometry g = (Geometry) j.next();
        MatchGeometry matchGeom = new MatchGeometry(matchFeat, g);
        matchGeometries.add(matchGeom);
      }
    }
  }

  public Iterator geometryIterator() { return matchGeometries.iterator(); }
  /**
   * An iterator over all MatchFeatures in the collection.
   */
  public Iterator iterator() { return matchFeatures.iterator(); }

  public int size() { return matchFeatures.size(); }
  public int geometrySize() { return matchGeometries.size(); }


  /**
   * Updates the match flag for features based on the matches
   */
  public void computeFeatureMatches()
  {
    // set all feature matches to true
    for (Iterator i = matchFeatures.iterator(); i.hasNext(); ) {
      MatchFeature mf = (MatchFeature) i.next();
      mf.setMatched(true);
    }
    // clear feature matches if any feature geometry is unmatched
    for (Iterator j = matchGeometries.iterator(); j.hasNext(); ) {
      MatchGeometry mg = (MatchGeometry) j.next();
      if (! mg.isMatched())
        mg.getFeature().setMatched(false);
    }
  }

  /**
   * Ensures that if a feature is unmatched,
   * any features matched to its geometries are also unmatched
   */
  public void propagateUnmatchedFeatures()
  {
    for (Iterator j = matchGeometries.iterator(); j.hasNext(); ) {
      MatchGeometry mg = (MatchGeometry) j.next();
      if (! mg.getFeature().isMatched()) {
        MatchGeometry mgOpposite = mg.getMatch();
        if (mgOpposite != null)
          mgOpposite.getFeature().setMatched(false);
      }
    }
  }

  public FeatureCollection getUnmatchedFeatures()
  {
    FeatureCollection noMatch = new FeatureDataset(inputFC.getFeatureSchema());
    for (Iterator i = matchFeatures.iterator(); i.hasNext(); ) {
      MatchFeature mf = (MatchFeature) i.next();
      if (! mf.isMatched())
        noMatch.add(mf.getFeature());
    }
    return noMatch;
  }
}
