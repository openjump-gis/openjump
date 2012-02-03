
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

import com.vividsolutions.jts.geom.*;


/**
 * A MicroscopePointAdjuster takes some line segments and an envelope, and
 * adjusts the points of the line segments within the envelope
 * so that small differences are visible.
 * Points will not be moved outside the envelope.
 */
public class MicroscopePointAdjuster {
    private static final Coordinate origin = new Coordinate(0.0, 0.0, 0.0);
    private List segList;
    private Envelope env;
    private double minSep;
    private Map adjPtMap = new TreeMap();

    public MicroscopePointAdjuster(List segList, Envelope env, double minSep) {
        this.segList = segList;
        this.env = env;
        this.minSep = minSep;
    }

    public Map getAdjustedPointMap() {
        computeAdjustments();

        return adjPtMap;
    }

    private void computeAdjustments() {
        // find all points in Envelope
        List ptsInEnv = findPointsInEnv(env);
        List segsInEnv = findSegmentsInEnv(env);

        SingleSegmentExpander ssex = new SingleSegmentExpander();

        if (ssex.isApplicable(segsInEnv, ptsInEnv)) {
            LineSegment seg = (LineSegment) segsInEnv.get(0);
            Coordinate[] adjPt = ssex.expandSegment(seg, env);
            adjPtMap.put(new Coordinate(seg.p0), adjPt[0]);
            adjPtMap.put(new Coordinate(seg.p1), adjPt[1]);
        } else {
            computeAdjustedPtMap(ptsInEnv);
        }
    }

    /**
     * Return a list of adjusted Segments.
     * Probably for testing only.
     */
    public List adjustSegments() {
        // find all points in Envelope
        List ptsInEnv = findPointsInEnv(env);
        computeAdjustedPtMap(ptsInEnv);

        return adjustSegs();
    }

    private List findPointsInEnv(Envelope env) {
        List ptsInEnv = new ArrayList();

        for (Iterator i = segList.iterator(); i.hasNext();) {
            LineSegment seg = (LineSegment) i.next();

            if (env.contains(seg.p0)) {
                ptsInEnv.add(seg.p0);
            }

            if (env.contains(seg.p1)) {
                ptsInEnv.add(seg.p1);
            }
        }

        return ptsInEnv;
    }

    private List findSegmentsInEnv(Envelope env) {
        List segsInEnv = new ArrayList();

        for (Iterator i = segList.iterator(); i.hasNext();) {
            LineSegment seg = (LineSegment) i.next();

            if (env.contains(seg.p0) && env.contains(seg.p1)) {
                segsInEnv.add(seg);
            }
        }

        return segsInEnv;
    }

    private void computeAdjustedPtMap(List ptsInEnv) {
        for (Iterator i = ptsInEnv.iterator(); i.hasNext();) {
            Coordinate pt = (Coordinate) i.next();
            Coordinate adjPt = computeAdjustment(pt);

            if (!adjPt.equals(pt)) {
                // copy key to ensure we don't have aliased modifications
                adjPtMap.put(new Coordinate(pt), adjPt);
            }
        }
    }

    private List adjustSegs() {
        List adjSegList = new ArrayList();

        for (Iterator i = segList.iterator(); i.hasNext();) {
            LineSegment seg = (LineSegment) i.next();

            LineSegment adjSeg = new LineSegment();
            adjSeg.p0 = adjustPt(seg.p0);
            adjSeg.p1 = adjustPt(seg.p1);
            adjSegList.add(adjSeg);
        }

        return adjSegList;
    }

    private Coordinate adjustPt(Coordinate p) {
        Coordinate adjMapPt = (Coordinate) adjPtMap.get(p);

        if (adjMapPt != null) {
            return new Coordinate(adjMapPt);
        }

        return new Coordinate(p);
    }

    private Coordinate computeAdjustment(Coordinate p) {
        Coordinate adjVec = new Coordinate();

        for (Iterator i = segList.iterator(); i.hasNext();) {
            LineSegment seg = (LineSegment) i.next();
            double dist = seg.distance(p);

            // if too close, compute an adjustment weight vector
            if (dist < minSep) {
                Coordinate adjWeightVec = adjustmentWeightVector(p, seg);
                adjVec.x += adjWeightVec.x;
                adjVec.y += adjWeightVec.y;
            }
        }

        Coordinate adjPt = new Coordinate(p);
        adjPt.x += adjVec.x;
        adjPt.y += adjVec.y;

        return adjPt;
    }

    private Coordinate adjustmentWeightVector(Coordinate p, LineSegment seg) {
        if (p.equals(seg.p0)) {
            return adjWeightEndPoint(p, seg.p1);
        }

        if (p.equals(seg.p1)) {
            return adjWeightEndPoint(p, seg.p0);
        }

        return adjWeightSegmentProximity(p, seg);
    }

    private Coordinate adjWeightEndPoint(Coordinate p, Coordinate p2) {
        Coordinate adjWeightVec = new Coordinate();
        adjWeightVec.x = p.x - p2.x;
        adjWeightVec.y = p.y - p2.y;

        double len = adjWeightVec.distance(origin);

        if (len > minSep) {
            return origin;
        }

        double scale = minSep / len;
        adjWeightVec.x *= scale;
        adjWeightVec.y *= scale;

        return adjWeightVec;
    }

    private Coordinate adjWeightSegmentProximity(Coordinate p, LineSegment seg) {
        Coordinate proj = seg.project(p);
        Coordinate adjWeightVec = new Coordinate();
        adjWeightVec.x = p.x - proj.x;
        adjWeightVec.y = p.y - proj.y;

        double len = adjWeightVec.distance(origin);

        /**
         * have to do something smarter if length is really small.
         * Probably test for which side of line point is and move it perp to line
         * a fixed amount.
         */
        double scale = minSep / len;
        adjWeightVec.x *= scale;
        adjWeightVec.y *= scale;

        return adjWeightVec;
    }
}
