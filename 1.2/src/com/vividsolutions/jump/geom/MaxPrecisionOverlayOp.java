
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

import com.vividsolutions.jts.geom.*;


/**
 * Increases the number of cases
 * JTS overlay operations can handle without robustness errors
 * by removing "excess" precision from the input geometries.
 */
public class MaxPrecisionOverlayOp {
    public MaxPrecisionOverlayOp() {
    }

    public static double getMinInAbsValue(double x0, double x1) {
        double absx0 = Math.abs(x0);
        double absx1 = Math.abs(x1);

        if (absx0 < absx1) {
            return x0;
        }

        return x1;
    }

    private void printBits(double x) {
        System.out.println("double value = " + x);
        System.out.println(Long.toBinaryString(Double.doubleToLongBits(x)));
        System.out.println(Long.toHexString(Double.doubleToLongBits(x)));
    }

    public Geometry intersection(Geometry g0, Geometry g1) {
        Envelope env = new Envelope();
        env.expandToInclude(g0.getEnvelopeInternal());
        env.expandToInclude(g1.getEnvelopeInternal());

        printBits(env.getMinX());
        printBits(env.getMaxX());
        printBits(env.getMinY());
        printBits(env.getMaxY());

        Coordinate minPt = new Coordinate();
        minPt.x = getMinInAbsValue(env.getMinX(), env.getMaxX());
        minPt.y = getMinInAbsValue(env.getMinY(), env.getMaxY());

        minPt.x = 475136.0; // 477000.0;
        minPt.y = 5366784.0; //5360000.0;

        Coordinate negMinPt = new Coordinate(minPt);
        negMinPt.x = -negMinPt.x;
        negMinPt.y = -negMinPt.y;

        AffineTransform trans = new AffineTransform();
        trans.translate(negMinPt);

        Geometry g0Copy = (Geometry) g0.clone();
        Geometry g1Copy = (Geometry) g1.clone();
        printBits(g1Copy.getCoordinate().x);
        g0Copy.apply(new CoordinatePrecisionReducer());
        g1Copy.apply(new CoordinatePrecisionReducer());

        //    trans.apply(g0Copy);
        //    trans.apply(g1Copy);
        printBits(g1Copy.getCoordinate().x);
        printBits(2345.626654971);

        System.out.println(g0Copy);
        System.out.println(g1Copy);

        Geometry result = g0Copy.intersection(g1Copy);

        System.out.println(result.getArea() / g0Copy.getArea());

        trans.translate(minPt);
        trans.apply(result);

        return result;
    }

    public class CoordinatePrecisionReducer implements CoordinateFilter {
        private static final double POW10 = 1000.0;

        public void filter(Coordinate p) {
            double x = p.x * POW10;
            x = Math.floor(x);

            //x -= 477000.0 * POW10;
            p.x = x;

            double y = p.y * POW10;
            y = Math.floor(y);

            //y -= 5360000.0 * POW10;
            p.y = y;
        }
    }
}
