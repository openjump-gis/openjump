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
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jump.util.Counter;
import com.vividsolutions.jump.util.CoordinateArrays;

public class UnmatchedEdgeExtracter {

  public static LineString toLinestring(LineSegment lineseg, GeometryFactory fact)
  {
    Coordinate[] coords = { lineseg.p0, lineseg.p1 };
    return fact.createLineString(coords);
  }

  /**
   * following should be done with anonymous inner classes for strategy
   */
  private boolean isDiff;   // hack to control how matching is done
  private Map lineMap = new TreeMap();

  public UnmatchedEdgeExtracter() {
  }

  public void add(Geometry geom)
  {
    // don't need to worry about orienting polygons
    add(CoordinateArrays.toCoordinateArrays(geom, false));
  }
  public void add(LineString line)
  {
    add(line.getCoordinates());
  }
  public void add(List coordArrays)
  {
    for (Iterator i = coordArrays.iterator(); i.hasNext(); ) {
      add((Coordinate[]) i.next());
    }
  }
  public void add(Coordinate[] coord)
  {
    for (int i = 0; i < coord.length - 1; i++) {
      LineSegment lineseg = new LineSegment(coord[i], coord[i + 1]);
      lineseg.normalize();

      Counter counter = (Counter) lineMap.get(lineseg);
      if (counter == null) {
        lineMap.put(lineseg, new Counter(1));
      }
      else {
        counter.increment();
      }
    }
  }

  /**
   * This function operates in two different modes depending on the value of isDiff.
   * If isDiff is true, the function returns true if the lineseg is present
   * at all in the map.
   * Is isDiff is false, the function returns true if the lineseg appears more than once
   * in the map.
   *
   * @param lineseg
   * @return true if the lineseg has a match
   */
  public boolean isMatched(LineSegment lineseg)
  {
    Counter counter = (Counter) lineMap.get(lineseg);
    if (counter == null) return false;
    if (isDiff) {
      return true;
    }
    else {
      if (counter.getValue() > 1) return true;
      return false;
    }
  }

  /**
   * Compute a list of all subsequences of segments in the
   * LineString line which do not appear in the map.
   */
  public void getDiffEdges(Geometry geom, List edgeList)
  {
    getEdges(CoordinateArrays.toCoordinateArrays(geom, false), true, edgeList);
  }
  /**
   * Compute a list of all subsequences of segments in the
   * LineString line which appear in the line only once.
   */
  public void getUnmatchedEdges(Geometry geom, List edgeList)
  {
    getEdges(CoordinateArrays.toCoordinateArrays(geom, false), false, edgeList);
  }

  private void getEdges(List coordArrays, boolean isDiff, List edgeList)
  {
    for (Iterator i = coordArrays.iterator(); i.hasNext(); ) {
      getEdges((Coordinate[]) i.next(), isDiff, edgeList);
    }
  }
  private void getEdges(Coordinate[] coord, boolean isDiff, List edgeList)
  {
    this.isDiff = isDiff;
    GeometryFactory fact = new GeometryFactory();
    // start is the index of the start of each line segment in the list
    int start = 0;
    while (start < coord.length - 1) {
      int end = getUnmatchedSequenceEnd(coord, start);
      if (start < end) {
        Coordinate[] edgeCoord = new Coordinate[end - start + 1];
        int edgeIndex = 0;
        for (int i = start; i <= end; i++) {
          edgeCoord[edgeIndex++] = coord[i];
        }
        LineString edge = fact.createLineString(edgeCoord);
        edgeList.add(edge);
        start = end;
      }
      else {
        start++;
      }
    }
  }

  /**
   * If no sequence matches, the value returned is equal to start
   */
  public int getUnmatchedSequenceEnd(Coordinate[] coord, int start)
  {
    LineSegment lineseg = new LineSegment();
    int index = start;
    // loop while segments are unmatched
    while (index < coord.length - 1) {
      lineseg.setCoordinates(coord[index], coord[index + 1]);
      lineseg.normalize();
      // if this segment is matched, exit loop
      if (isMatched(lineseg)) {
        break;
      }
      index++;
    }
    return index;
  }
}
