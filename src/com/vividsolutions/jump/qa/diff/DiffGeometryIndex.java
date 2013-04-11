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
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

public class DiffGeometryIndex
{

  public static Collection splitGeometry(Geometry geom, boolean splitIntoComponents)
  {
    Collection list = new ArrayList();
    if (splitIntoComponents && geom instanceof GeometryCollection) {
      GeometryCollection geomColl = (GeometryCollection) geom;
      for (GeometryCollectionIterator gci = new GeometryCollectionIterator(geomColl); gci.hasNext(); ) {
        Geometry component = (Geometry) gci.next();
        if (! (component instanceof GeometryCollection)) {
          list.add(component);
        }
      }
    }
    else {
      // simply return input geometry in a list
      list.add(geom);
    }
    return list;
  }

  private SpatialIndex index;
  private DiffGeometryMatcher diffMatcher;
  private boolean splitIntoComponents;
  private Collection featureList;

  public DiffGeometryIndex(
      FeatureCollection fc,
      DiffGeometryMatcher diffMatcher,
      boolean splitIntoComponents)
  {
    this.diffMatcher = diffMatcher;
    this.splitIntoComponents = splitIntoComponents;
    buildIndex(fc);
  }

  public boolean hasMatch(Geometry testGeom)
  {
    diffMatcher.setQueryGeometry(testGeom);

    List closeFeatList = index.query(diffMatcher.getQueryGeometry().getEnvelopeInternal());
    for (Iterator j = closeFeatList.iterator(); j.hasNext(); ) {
      FeatureGeometry closeFeat = (FeatureGeometry) j.next();

      if (diffMatcher.isMatch(closeFeat.getGeometry())) {
        closeFeat.setMatched(true);
        return true;
      }
    }
    return false;
  }

  private void buildIndex(FeatureCollection fc)
  {
    featureList = new ArrayList();
    index = new STRtree();
    for (Iterator i = fc.iterator(); i.hasNext(); )
    {
      Feature feat = (Feature) i.next();
      Geometry geom = feat.getGeometry();
      Collection list = splitGeometry(geom, splitIntoComponents);
      for (Iterator j = list.iterator(); j.hasNext(); ) {
        Geometry g = (Geometry) j.next();
        FeatureGeometry featGeom = new FeatureGeometry(feat, g);
        index.insert(featGeom.getGeometry().getEnvelopeInternal(), featGeom);
        featureList.add(featGeom);
      }
    }
  }

  public Collection getUnmatchedFeatures()
  {
    Set unmatchedFeatureSet = new TreeSet(new FeatureUtil.IDComparator());
    for (Iterator i = featureList.iterator(); i.hasNext(); ) {
      FeatureGeometry fg = (FeatureGeometry) i.next();
      if (! fg.isMatched()) {
        unmatchedFeatureSet.add(fg.getFeature());
      }
    }
    return unmatchedFeatureSet;
  }

  public class FeatureGeometry
  {
    private Feature feat;
    private Geometry geom;
    private boolean isMatched = false;

    public FeatureGeometry(Feature feat, Geometry geom)
    {
      this.feat = feat;
      this.geom = geom;
    }
    public Feature getFeature() { return feat; }
    public Geometry getGeometry() { return geom; }

    public void setMatched(boolean isMatched) { this.isMatched = isMatched; }
    public boolean isMatched() { return isMatched; }
  }

}
