
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

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.MathUtil;


/**
 * Utility functions for working with Coordinates.
 */
public class CoordUtil {
    /**
     * Returns the average of two Coordinates.
     * @param c1 one coordinate
     * @param c2 another coordinate
     * @return a new Coordinate with the average x and average y
     */
    public static Coordinate average(Coordinate c1, Coordinate c2) {
    	if (Double.isNaN(c1.z) || Double.isNaN(c2.z))
    		return new Coordinate(MathUtil.avg(c1.x, c2.x), MathUtil.avg(c1.y, c2.y));
    	else
    		return new Coordinate(MathUtil.avg(c1.x, c2.x), MathUtil.avg(c1.y, c2.y),
    				MathUtil.avg(c1.z, c2.z));
    }

    /**
     * @param coordinates not empty
     */
    public static Coordinate average(Collection coordinates) {
        Assert.isTrue(!coordinates.isEmpty());

        double xSum = 0;
        double ySum = 0;
        double zSum = 0;
        boolean zNanFound = false;
        
        for (Iterator i = coordinates.iterator(); i.hasNext();) {
            Coordinate coordinate = (Coordinate) i.next();
            xSum += coordinate.x;
            ySum += coordinate.y;
        	if (!Double.isNaN(coordinate.z))
        		zSum += coordinate.z;
        	else
        		zNanFound = true;
       }
        if (zNanFound)
	        return new Coordinate(xSum / coordinates.size(),
	            ySum / coordinates.size());
        else
        	return new Coordinate(xSum / coordinates.size(),
    	            ySum / coordinates.size(),  zSum / coordinates.size());
    }

    /**
     * @param coordinates not empty
     */
    public static Coordinate closest(Collection coordinates, Coordinate p) {
        Assert.isTrue(!coordinates.isEmpty());

        Coordinate closest = (Coordinate) coordinates.iterator().next();

        for (Iterator i = coordinates.iterator(); i.hasNext();) {
            Coordinate candidate = (Coordinate) i.next();

            if (p.distance(candidate) < p.distance(closest)) {
                closest = candidate;
            }
        }

        return closest;
    }

    /**
     * Adds two coordinates.
     * @param c1 the first coordinate
     * @param c2 the second coordinate
     * @return a new coordinate: c1 + c2
     */
    public static Coordinate add(Coordinate c1, Coordinate c2) {
    	if (Double.isNaN(c1.z) || Double.isNaN(c2.z))
    		return new Coordinate(c1.x + c2.x, c1.y + c2.y);
    	else
    		return new Coordinate(c1.x + c2.x, c1.y + c2.y, c1.z + c2.z);
    }

    /**
     * Subtracts two coordinates.
     * @param c1 the first coordinate
     * @param c2 the second coordinate
     * @return a new coordinate: c1 - c2
     */
    public static Coordinate subtract(Coordinate c1, Coordinate c2) {
    	if (Double.isNaN(c1.z) || Double.isNaN(c2.z))
    		return new Coordinate(c1.x - c2.x, c1.y - c2.y);
    	else
    		return new Coordinate(c1.x - c2.x, c1.y - c2.y, c1.z - c2.z);
   }

    /**
     * Multiplies a scalar and a coordinate.
     * @param d the scalar
     * @param c the coordinate
     * @return a new coordinate: d * c
     */
    public static Coordinate multiply(double d, Coordinate c) {
       	if (Double.isNaN(c.z))
       		return new Coordinate(d * c.x, d * c.y);
       	else
       		return new Coordinate(d * c.x, d * c.y, d * c.z);
    }

    /**
     * Divides a coordinate by a scalar.
     * @param c the coordinate
     * @param d the scalar   *
     * @return a new coordinate: c / d
     */
    public static Coordinate divide(Coordinate c, double d) {
       	if (Double.isNaN(c.z))
       		return new Coordinate(c.x / d, c.y / d);
       	else
       		return new Coordinate(c.x / d, c.y / d, c.z / d);
    }

    public static Coordinate toCoordinate(Point2D point) {
        return new Coordinate(point.getX(), point.getY());
    }

    public static Point2D toPoint2D(Coordinate coordinate) {
        return new Point2D.Double(coordinate.x, coordinate.y);
    }
    
    public static Point2D add(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() + b.getX(), a.getY() + b.getY());
    }

    public static Point2D subtract(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }    
}
