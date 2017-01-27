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

public class MatchGeometry {

  public static Collection<Geometry> splitGeometry(Geometry geom, boolean splitIntoComponents) {
    Collection<Geometry> list = new ArrayList<>();
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

  private MatchFeature feature;
  private Geometry geom;
  private MatchGeometry matchGeom = null;

  public MatchGeometry(MatchFeature feature, Geometry geom)
  {
    this.feature = feature;
    this.geom = geom;
  }
  public MatchFeature getFeature() { return feature; }
  public Geometry getGeometry() { return geom; }

  public MatchGeometry getMatch() { return matchGeom; }
  public void setMatch(MatchGeometry matchGeom) { this.matchGeom = matchGeom; }
  public boolean isMatched() { return matchGeom != null; }
}
