
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

package com.vividsolutions.jump.geom;

import java.util.*;

import org.locationtech.jts.geom.*;
import com.vividsolutions.jump.util.CoordinateArrays;


/**
 * Magnifies a given area of a set of Geometry's.
 */
public class GeometryMicroscope {

    private final List<Geometry> geomList;
    private final Envelope env;
    private final double minSep;

    public GeometryMicroscope(List<Geometry> geomList, Envelope env, double minSep) {
        this.geomList = geomList;
        this.env = env;
        this.minSep = minSep;
    }

    public List<Geometry> getAdjusted() {
        List<LineSegment> segList = getSegList();
        MicroscopePointAdjuster mpa = new MicroscopePointAdjuster(segList, env,
                minSep);
        Map<Coordinate,Coordinate> ptMap = mpa.getAdjustedPointMap();
        applyAdjustment(ptMap);

        return geomList;
    }

    private void applyAdjustment(Map<Coordinate,Coordinate> ptMap) {
        CoordinateAdjusterFilter coordAdjFilter = new CoordinateAdjusterFilter(ptMap);

        for (Geometry geom : geomList) {
            geom.apply(coordAdjFilter);
        }
    }

    private List<LineSegment> getSegList() {
        List<LineSegment> segList = new ArrayList<>();

        for (Geometry geom : geomList) {
            List<Coordinate[]> coordArrayList =
                CoordinateArrays.toCoordinateArrays(geom, false);
            addSegments(coordArrayList, segList);
        }

        return segList;
    }

    private void addSegments(List<Coordinate[]> coordArrayList, List<LineSegment> segList) {
        LineSegmentEnvelopeIntersector linesegEnvInt = new LineSegmentEnvelopeIntersector();

        // for now just return all segs
        // in future, only return segs which intersect env
        for (Coordinate[] coord : coordArrayList) {
            for (int j = 0; j < (coord.length - 1); j++) {
                LineSegment seg = new LineSegment(coord[j], coord[j + 1]);
                if (linesegEnvInt.touches(seg, env)) {
                    segList.add(seg);
                }
            }
        }
    }

    public static class CoordinateAdjusterFilter implements CoordinateFilter {

        Map<Coordinate,Coordinate> ptMap;

        CoordinateAdjusterFilter(Map<Coordinate,Coordinate> ptMap) {
            this.ptMap = ptMap;
        }

        public void filter(Coordinate p) {
            Coordinate adj = ptMap.get(p);
            if (adj != null) {
                p.x = adj.x;
                p.y = adj.y;
            }
        }
    }

}
