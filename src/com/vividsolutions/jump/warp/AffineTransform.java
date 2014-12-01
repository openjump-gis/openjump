
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

package com.vividsolutions.jump.warp;

import Jama.Matrix;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * An AffineTransform implementation that is initialized by specifying
 * three points and the three points they map to.
 * <p>From http://graphics.lcs.mit.edu/classes/6.837/F01/Lecture07/lecture07.pdf:
 * <pre>
 *      [ x1_ ] = [  x1 y1 1  0  0  0  ] [ a11 ]
 *      [ y1_ ] = [  0  0  0  x1 y1 1  ] [ a12 ]
 *      [ x2_ ] = [  x2 y2 1  0  0  0  ] [ a13 ]
 *      [ y2_ ] = [  0  0  0  x2 y2 1  ] [ a21 ]
 *      [ x3_ ] = [  x3 y3 1  0  0  0  ] [ a22 ]
 *      [ y3_ ] = [  0  0  0  x3 y3 1  ] [ a23 ]
 * x_ = X a
 * Solution: a = Xinv x_
 * </pre>
 */
public class AffineTransform extends CoordinateTransform {
    private Matrix a;

    /**
     * A transformation that maps p1 to p1_ via a translation (no rotation or shear).
     * @param p1 a point
     * @param p1_ the point it maps to
     */
    public AffineTransform(Coordinate p1, Coordinate p1_) {
        Coordinate p2 = new Coordinate(p1.x + 10, p1.y);
        Coordinate p2_ = new Coordinate(p1_.x + 10, p1_.y);
        Coordinate p3 = new Coordinate(p1.x, p1.y + 10);
        Coordinate p3_ = new Coordinate(p1_.x, p1_.y + 10);
        initialize(p1, p1_, p2, p2_, p3, p3_);
    }

    /**
     * A transformation that maps p1 to p1_ and p2 to p2_ via a translation,
     * rotation, and scaling (no "relative" shear).
     * @param p1 a point
     * @param p1_ the point p1 maps to
     * @param p2 another point
     * @param p2_ the point p2 maps to
     */
    public AffineTransform(Coordinate p1, Coordinate p1_, Coordinate p2,
        Coordinate p2_) {
        Coordinate p3 = rotate90(p1, p2);
        Coordinate p3_ = rotate90(p1_, p2_);
        initialize(p1, p1_, p2, p2_, p3, p3_);
    }

    /**
     * A transformation that maps p1 to p1_, p2 to p2_ and p3 to p3_.
     * @param p1 a point
     * @param p1_ the point p1 maps to
     * @param p2 another point
     * @param p2_ the point p2 maps to
     * @param p3 another point
     * @param p3_ the point p3 maps to
     */
    public AffineTransform(Coordinate p1, Coordinate p1_, Coordinate p2,
        Coordinate p2_, Coordinate p3, Coordinate p3_) {
        initialize(p1, p1_, p2, p2_, p3, p3_);
    }

    /**
     * Determines where a point would end up if it were rotated 90 degrees about
     * another point.
     * @param a the fixed point
     * @param b the point to rotate (b itself will not be changed)
     * @return b rotated 90 degrees clockwise about a
     */
    public static Coordinate rotate90(Coordinate a, Coordinate b) {
        return new Coordinate(b.y - a.y + a.x, a.x - b.x + a.y);
    }

    private void initialize(Coordinate p1, Coordinate p1_, Coordinate p2,
        Coordinate p2_, Coordinate p3, Coordinate p3_) {
        double[][] Xarray = {
            { p1.x, p1.y, 1, 0, 0, 0 },
            { 0, 0, 0, p1.x, p1.y, 1 },
            { p2.x, p2.y, 1, 0, 0, 0 },
            { 0, 0, 0, p2.x, p2.y, 1 },
            { p3.x, p3.y, 1, 0, 0, 0 },
            { 0, 0, 0, p3.x, p3.y, 1 }
        };
        Matrix X = new Matrix(Xarray);
        double[][] x_array = {
            { p1_.x },
            { p1_.y },
            { p2_.x },
            { p2_.y },
            { p3_.x },
            { p3_.y }
        };
        Matrix x_ = new Matrix(x_array);
        a = X.solve(x_);
    }

    /**
     * Applies the affine transform to a point.
     * From http://graphics.lcs.mit.edu/classes/6.837/F01/Lecture07/lecture07.pdf:
     * <pre>
     *      [ x_ ] = [  a11 a12 a13  ] [ x ]
     *      [ y_ ] = [  a21 a22 a23  ] [ y ]
     *      [ 1  ] = [  0   0   1    ] [ 1 ]
     * </pre>
     * @param c the input to the affine transform
     * @return the result of applying the affine transform to c
     */
    public Coordinate transform(Coordinate c) {
        double x_ = (a.get(0, 0) * c.x) + (a.get(1, 0) * c.y) + a.get(2, 0);
        double y_ = (a.get(3, 0) * c.x) + (a.get(4, 0) * c.y) + a.get(5, 0);

        return new Coordinate(x_, y_);
    }
}
