
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
 * Giuseppe Aruta (2013) 
 * This class derives from  OpenJUMP original AffineTransformation class
 * and it implements a ProjectiveTransform that is initialized by specifying
 * four couples of points (source to target).
 * The matrix is a dapted from http://graphics.lcs.mit.edu/classes/6.837/F01/Lecture07/lecture07.pdf
 * <pre>
 *      [ x1_ ] = [  x1 y1 1  0  0  0  -x1x1_ -y1x1_ ] [ a11 ]
 *      [ y1_ ] = [  0  0  0  x1 y1 1  -x1y1_ -y1y1_ ] [ a12 ]
 *      [ x2_ ] = [  x2 y2 1  0  0  0  -x2x2_ -y2x2_ ] [ a13 ]
 *      [ y2_ ] = [  0  0  0  x2 y2 1  -x2y2_ -y2y2_ ] [ a21 ]
 *      [ x3_ ] = [  x3 y3 1  0  0  0  -x3x3_ -y3x3_ ] [ a22 ]
 *      [ y3_ ] = [  0  0  0  x3 y3 1  -x3y3_ -y3y3_ ] [ a23 ]
 *      [ x4_ ] = [  x3 y3 1  0  0  0  -x4x4_ -y4x4_ ] [ a31 ]
 *      [ y4_ ] = [  0  0  0  x3 y3 1  -x4y4_ -y4y4_ ] [ a32 ]
 * </pre>
 * See also http://en.wikipedia.org/wiki/Homography for Homographic transformations
 * and 
 */
public class ProjectiveTransform extends CoordinateTransform {
    private Matrix a;


    /**
     * A transformation that maps p1 to p1_, p2 to p2_, p3 to p3_ and p4 to p4_
     * @param p1 a point
     * @param p1_ the point p1 maps to
     * @param p2 another point
     * @param p2_ the point p2 maps to
     * @param p3 another point
     * @param p3_ the point p3 maps to
     * @param p4 another point
     * @param p4_ the point p4 maps to
     */
    public ProjectiveTransform(Coordinate p1, Coordinate p1_, Coordinate p2,
        Coordinate p2_, Coordinate p3, Coordinate p3_, Coordinate p4, Coordinate p4_) {
        initialize(p1, p1_, p2, p2_, p3, p3_, p4, p4_);
    }

    
    private void initialize(Coordinate p1, Coordinate p1_, Coordinate p2,
        Coordinate p2_, Coordinate p3, Coordinate p3_, Coordinate p4, Coordinate p4_) {
        double[][] Xarray = {
            { p1.x,  p1.y,  1,  0,  0,  0,   -p1_.x*p1.x,  -p1_.x*p1.y},
            { 0,  0,  0,  p1.x,  p1.y,  1,   -p1_.y*p1.x,  -p1_.y*p1.y},
            { p2.x,  p2.y,  1,  0,  0,  0 ,  -p2_.x*p2.x,  -p2_.x*p2.y},
            { 0,  0,  0,  p2.x,  p2.y,  1 ,  -p2_.y*p2.x,  -p2_.y*p2.y},
            { p3.x,  p3.y,  1,  0,  0,  0 ,  -p3_.x*p3.x,  -p3_.x*p3.y},
            { 0,  0,  0,  p3.x,  p3.y,  1,   -p3_.y*p3.x,  -p3_.y*p3.y},
            { p4.x,  p4.y,  1,  0,  0,  0 ,  -p4_.x*p4.x,  -p4_.x*p4.y},
            { 0,  0,  0,  p4.x,  p4.y,  1,   -p4_.y*p4.x,  -p4_.y*p4.y}
        };
        Matrix X = new Matrix(Xarray);
        double[][] x_array = {
            { p1_.x },
            { p1_.y }, 
            { p2_.x },
            { p2_.y },
            { p3_.x },
            { p3_.y },
            { p4_.x },
            { p4_.y }
        };
        Matrix x_ = new Matrix(x_array);
        a = X.solve(x_);
    }
 
    
    /**
     * Applies the prospective transform to a point.
     * From http://graphics.lcs.mit.edu/classes/6.837/F01/Lecture07/lecture07.pdf:
     * <pre>
     *      [ wx_ ] = [  P11 P12 P13  ] [ x ]
     *      [ wy_ ] = [  P21 P22 P23  ] [ y ]
     *      [ w   ] = [  P31 P32   1  ] [ 1 ]
     * </pre>
     * @param c the input to the projective transform
     * @return the result of applying the projective transform to c
     */
    @Override
	public Coordinate transform(Coordinate c) {
    	
    
    	 double wx_ = (a.get(0, 0) * c.x) + (a.get(1, 0) * c.y) + a.get(2, 0);
         double wy_ = (a.get(3, 0) * c.x) + (a.get(4, 0) * c.y) + a.get(5, 0);
    	 double w = (a.get(6, 0) * c.x) + (a.get(7, 0) * c.y) + 1;
        double x_ = wx_/w;
        double y_ = wy_/w;

        return new Coordinate(x_, y_);
    }
}
