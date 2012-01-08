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
package com.vividsolutions.jump.plugin.edit;

import java.util.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.geom.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.task.*;

/**
 * Extracts the unique segments from a FeatureCollection.
 *
 * Replace UniqueSegmentExtracter, adding the capability to return segments
 * occuring between minOccur and maxOccur times in the dataset.
 * Now, this class can do the same as FeatureSegmentCounter in JCS and should replace it
 * [Michael Michaud 2007-05-15]
 *
 * @author Martin Davis / Michael Michaud
 * @version 1.1
 */
public class SegmentsExtracter
{
  private static final GeometryFactory factory = new GeometryFactory();

  private Set segmentSet = new TreeSet();
  // Segments are added to a TreeMap (LineSegment is Comparable)
  private Map segmentMap = new TreeMap();
  // private LineSegment querySegment = new LineSegment();
  private boolean countZeroLengthSegments = true;
  private TaskMonitor monitor;
  private Geometry fence = null;
  // private LineSegmentEnvelopeIntersector lineEnvInt;

  public SegmentsExtracter() {
  }

  /**
   * Creates a new counter.
   *
   * @param monitor
   */
  public SegmentsExtracter(TaskMonitor monitor) {
    this.monitor = monitor;
  }

/*
  public void setFence(Geometry fence)
  {
    this.fence = fence;
    //lineEnvInt = new LineSegmentEnvelopeIntersector();
  }
*/

  public void add(FeatureCollection fc)
  {
    monitor.allowCancellationRequests();
    int totalFeatures = fc.size();
    int j = 0;
    for (Iterator i = fc.iterator(); i.hasNext() && ! monitor.isCancelRequested(); ) {
      Feature feature = (Feature) i.next();
      j++;
      monitor.report(j, totalFeatures, "features");
      add(feature);
    }
  }

  public void add(Feature f)
  {
    Geometry g = f.getGeometry();
    // skip if using fence and feature is not in fence
    if (fence != null && ! g.intersects(fence))
      return;

    List coordArrays = CoordinateArrays.toCoordinateArrays(g, true);
    for (Iterator i = coordArrays.iterator(); i.hasNext(); ) {
      Coordinate[] coord = (Coordinate[]) i.next();
      for (int j = 0; j < coord.length - 1; j++) {
        // skip if using fence AND seg is not in fence
      /*
      if (fence != null) {
        LineString segLine = factory.createLineString(new Coordinate[] { coord[j], coord[j + 1] });
        if (! fence.intersects(segLine))
          continue;
      }
      */
        add(coord[j], coord[j + 1]);
      }
    }
  }

  public void add(Coordinate p0, Coordinate p1)
  {
    // check for zero-length segment
    boolean isZeroLength = p0.equals(p1);
    if (! countZeroLengthSegments && isZeroLength)
      return;

    LineSegment lineseg = new LineSegment(p0, p1);
    lineseg.normalize();

    SegmentCount count = (SegmentCount) segmentMap.get(lineseg);
    if (count == null) {
      segmentMap.put(lineseg, new SegmentCount(1));
    }
    else {
      count.increment();
    }
    
    //segmentSet.add(lineseg);
  }

  public Collection getSegments()
  {
        return segmentMap.keySet();
  }
  
  public Collection getSegments(int minOccurs, int maxOccurs)
  {
      List segmentList = new ArrayList();
      for (Iterator it = segmentMap.entrySet().iterator() ; it.hasNext() ; ) {
          Map.Entry entry = (Map.Entry)it.next();
          LineSegment ls = (LineSegment)entry.getKey();
          int count = ((SegmentCount)entry.getValue()).getCount();
          if (count>=minOccurs && count<=maxOccurs) {
              segmentList.add(ls);
          }
      }
      return segmentList;
  }
  
  public class SegmentCount {
    private int count = 0;
    public SegmentCount(int value) {
      this.count = value;
    }
    public int getCount() { return count; }
    public void increment() {count++;}
  }

}