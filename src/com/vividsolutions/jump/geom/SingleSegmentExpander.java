
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

import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.*;


/**
 * A heuristic used by Microscope to expand a single segment,
 * while maintaining its orientation.
 */
public class SingleSegmentExpander {
    private Coordinate[] adjPt = new Coordinate[2];

    public SingleSegmentExpander() {
    }

    public static Envelope getInsetEnvelope(Envelope env, double insetPct) {
        double insetX = (env.getWidth() * insetPct) / 2;
        double insetY = (env.getWidth() * insetPct) / 2;
        double inset = insetX;

        if (insetY < inset) {
            inset = insetY;
        }

        return new Envelope(env.getMinX() + inset, env.getMaxX() - inset,
            env.getMinY() + inset, env.getMaxY() - inset);
    }

    public boolean isApplicable(List segList, List ptList) {
        if (segList.size() < 1) {
            return false;
        }

        LineSegment seg = (LineSegment) segList.get(0);

        return allSegsEqual(seg, segList) && allPtsInSeg(seg, ptList);
    }

    private boolean allSegsEqual(LineSegment seg, List segList) {
        for (Iterator i = segList.iterator(); i.hasNext();) {
            LineSegment seg2 = (LineSegment) i.next();

            if (!seg.equalsTopo(seg2)) {
                return false;
            }
        }

        return true;
    }

    private boolean allPtsInSeg(LineSegment seg, List ptList) {
        for (Iterator i = ptList.iterator(); i.hasNext();) {
            Coordinate pt = (Coordinate) i.next();

            if (seg.p0.equals(pt)) {
                return true;
            }

            if (seg.p1.equals(pt)) {
                return true;
            }
        }

        return false;
    }

    public Coordinate[] expandSegment(LineSegment seg, Envelope env) {
        Envelope insetEnv = getInsetEnvelope(env, 0.2);
        double dx = seg.p1.x - seg.p0.x;
        double dy = seg.p1.y - seg.p0.y;

        if (Math.abs(dx) <= 1.0E-6) {
            double y0 = insetEnv.getMinY();
            double y1 = insetEnv.getMaxY();

            if (seg.p0.y < seg.p1.y) {
                y0 = insetEnv.getMaxY();
                y1 = insetEnv.getMinY();
            }

            adjPt[0] = new Coordinate(seg.p0.x, y0);
            adjPt[1] = new Coordinate(seg.p0.x, y1);

            return adjPt;
        }

        if (Math.abs(dy) <= 1.0E-6) {
            double x0 = insetEnv.getMinX();
            double x1 = insetEnv.getMaxX();

            if (seg.p0.x < seg.p1.x) {
                x0 = insetEnv.getMaxX();
                x1 = insetEnv.getMinX();
            }

            adjPt[0] = new Coordinate(x0, seg.p0.y);
            adjPt[1] = new Coordinate(x1, seg.p0.y);

            return adjPt;
        }

        // compute intersection of ray with inset Envelope
        adjPt[0] = rayEnvIntersection(seg.p0, seg.p1, insetEnv);
        adjPt[1] = rayEnvIntersection(seg.p1, seg.p0, insetEnv);

        return adjPt;
    }

    /**
     * Computes the intersection of the ray p0-p1 with one of the edges
     * of the envelope
     */
    private Coordinate rayEnvIntersection(Coordinate p0, Coordinate p1,
        Envelope env) {
        Coordinate x0 = segIntX(p0, p1, env.getMinX(), env.getMinY(),
                env.getMaxY());

        if (x0 != null) {
            return x0;
        }

        Coordinate x1 = segIntX(p0, p1, env.getMaxX(), env.getMinY(),
                env.getMaxY());

        if (x1 != null) {
            return x1;
        }

        Coordinate y0 = segIntY(p0, p1, env.getMinY(), env.getMinX(),
                env.getMaxX());

        if (y0 != null) {
            return y0;
        }

        Coordinate y1 = segIntY(p0, p1, env.getMaxY(), env.getMinX(),
                env.getMaxX());

        if (y1 != null) {
            return y1;
        }

        // should never reach here - if we do, need a tolerance in segInt routines
        return null;
    }

    /**
     * Computes the dot product of the vectors p-p0 and p-p1.
     * If the dot product is negative the vectors contain an obtuse angle,
     * if positive they contain an acute angle.  If the angle is acute,
     * the vectors can be considered to be "in the same direction".
     */
    private double dotProduct(Coordinate p, Coordinate p0, Coordinate p1) {
        double dx0 = p0.x - p.x;
        double dy0 = p0.y - p.y;
        double dx1 = p1.x - p.x;
        double dy1 = p1.y - p.y;

        return (dx0 * dx1) + (dy0 * dy1);
    }

    private Coordinate segIntX(Coordinate p0, Coordinate p1, double x,
        double miny, double maxy) {
        double m = (p1.y - p0.y) / (p1.x - p0.x);
        double y2 = (m * (x - p0.x)) + p0.y;

        if ((y2 > miny) && (y2 < maxy)) {
            Coordinate intPt = new Coordinate(x, y2);

            if (dotProduct(p0, p1, intPt) < 0.0) {
                return intPt;
            }
        }

        return null;
    }

    private Coordinate segIntY(Coordinate p0, Coordinate p1, double y,
        double minx, double maxx) {
        double m = (p1.x - p0.x) / (p1.y - p0.y);
        double x2 = (m * (y - p0.y)) + p0.x;

        if ((x2 > minx) && (x2 < maxx)) {
            Coordinate intPt = new Coordinate(x2, y);

            if (dotProduct(p0, p1, intPt) < 0.0) {
                return intPt;
            }
        }

        return null;
    }
}
