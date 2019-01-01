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

/**
 * Indexes a MatchCollection to optimize the spatial queries
 * used during matching.
 */
public class MatchIndex
{

  private SpatialIndex index;

  public MatchIndex(MatchCollection matchColl)
  {
    buildIndex(matchColl);
  }

  private void buildIndex(MatchCollection matchColl)
  {
    index = new STRtree();
    for (Iterator i = matchColl.geometryIterator(); i.hasNext(); )
    {
      MatchGeometry matchGeom = (MatchGeometry) i.next();
      index.insert(matchGeom.getGeometry().getEnvelopeInternal(), matchGeom);
    }
  }

  public void testMatch(MatchCollection matchColl, DiffGeometryMatcher diffMatcher)
  {
    for (Iterator i = matchColl.geometryIterator(); i.hasNext(); ) {
      MatchGeometry matchGeom = (MatchGeometry) i.next();
      testMatch(matchGeom, diffMatcher);
    }
  }

  public boolean testMatch(MatchGeometry testGeom, DiffGeometryMatcher diffMatcher)
  {
    diffMatcher.setQueryGeometry(testGeom.getGeometry());

    List resultList = index.query(diffMatcher.getQueryGeometry().getEnvelopeInternal());
    for (Iterator j = resultList.iterator(); j.hasNext(); ) {
      MatchGeometry matchGeom = (MatchGeometry) j.next();
      if (! matchGeom.isMatched()) {
        if (diffMatcher.isMatch(matchGeom.getGeometry())) {
          matchGeom.setMatch(testGeom);
          testGeom.setMatch(matchGeom);
          return true;
        }
      }
    }
    return false;
  }

}
