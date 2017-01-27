
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

import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.*;


/**
 * Optimized code for spatial predicates
 * between
 * {@link LineSegment}s and {@link Envelope}s.
 */
public class LineSegmentEnvelopeIntersector {

    private static final LineIntersector lineInt = new RobustLineIntersector();

    public LineSegmentEnvelopeIntersector() {
    }

    public boolean touches(LineSegment seg, Envelope env) {
        return touches(seg.p0, seg.p1, env);
    }

    public boolean touches(Coordinate p0, Coordinate p1, Envelope env) {
        Envelope lineEnv = new Envelope(p0, p1);

        if (!lineEnv.intersects(env)) {
            return false;
        }

        // now test either endpoint is inside
        if (env.contains(p0)) {
            return true;
        }

        if (env.contains(p1)) {
            return true;
        }

        // test whether the segment intersects any of the envelope sides
        // the coordinates of the envelope, in CW order
        Coordinate env0 = new Coordinate(env.getMinX(), env.getMinY());
        Coordinate env1 = new Coordinate(env.getMinX(), env.getMaxY());
        Coordinate env2 = new Coordinate(env.getMaxX(), env.getMaxY());
        Coordinate env3 = new Coordinate(env.getMaxX(), env.getMinY());

        lineInt.computeIntersection(p0, p1, env0, env1);

        if (lineInt.hasIntersection()) {
            return true;
        }

        lineInt.computeIntersection(p0, p1, env1, env2);

        if (lineInt.hasIntersection()) {
            return true;
        }

        lineInt.computeIntersection(p0, p1, env2, env3);

        if (lineInt.hasIntersection()) {
            return true;
        }

        lineInt.computeIntersection(p0, p1, env3, env0);

        if (lineInt.hasIntersection()) {
            return true;
        }

        return false;
    }
}
