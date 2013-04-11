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

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.noding.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jump.feature.Feature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to transform a collection of Features into a List of
 * SegmentStrings, and to store genealogy data in into their data field.
 */
public class Features2SegmentStringsWithData {

    private Features2SegmentStringsWithData() {}
    
    public static List<SegmentString> getSegmentStrings(Collection inputFeatures) {
        List<SegmentString> segmentStrings = new ArrayList<SegmentString>();
        for (Object o : inputFeatures) {
            Feature f = (Feature)o;
            segmentStrings.addAll(getSegmentStrings(f));
        }
        return segmentStrings;
    }
    
    public static List<SegmentString> getSegmentStrings(Feature f) {
        Geometry g = f.getGeometry();
        List<SegmentString> segmentStrings = new ArrayList<SegmentString>();
        for (int i = 0 ; i < g.getNumGeometries() ; i++) {
            Geometry component = g.getGeometryN(i);
            if (component instanceof Polygon) {
                add((Polygon)component, f, i, segmentStrings);
            }
            else if (component instanceof LineString) {
                add((LineString)component, f, i, 0, segmentStrings);
            }
        }
        return segmentStrings;
    }
    
    private static void add(Polygon poly, Feature f, int i, 
                                   List<SegmentString> segmentStrings) {
        add(poly.getExteriorRing(), f, i, 0, segmentStrings);
        for (int j = 0 ; j < poly.getNumInteriorRing() ; j++) {
            add(poly.getInteriorRingN(j), f, i, j+1, segmentStrings);
        }
    }
    
    private static void add(LineString line, Feature f, int i, int j,
                                      List<SegmentString> segmentStrings) {
        SegmentString ss = new NodedSegmentString(line.getCoordinates(), 
            new SegmentStringData(f, i, j));
        segmentStrings.add(ss);
    }
        
}
