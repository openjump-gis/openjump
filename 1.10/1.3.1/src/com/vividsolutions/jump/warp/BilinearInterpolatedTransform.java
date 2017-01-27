
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

import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.task.TaskMonitor;


/**
 * Bilinear interpolated triangulation transform, also known as "rubber sheeting".
 * See Saalfeld, Alan. 1985. A Fast Rubber-Sheeting Transformation Using
 * Simplical Coordinates. "The American Cartographer" 12:2, 169-173.
 */
public class BilinearInterpolatedTransform extends CoordinateTransform {
    private Map triangleMap;
    private TaskMonitor monitor;
    private int coordinatesTransformed = 0;

    /**
     * Creates a RubberSheetTransform using the given triangulation.
     * @param triangleMap a map of source Triangle to destination Triangle
     */
    public BilinearInterpolatedTransform(Map triangleMap, TaskMonitor monitor) {
        this.triangleMap = triangleMap;
        this.monitor = monitor;
        monitor.report("Transforming...");
    }

    /**
     * Maps one Coordinate to another.
     * @param  c  a Coordinate which must be inside one of the triangle keys passed
     *      into the constructor
     * @return the transformed Coordinate
     */
    public Coordinate transform(Coordinate c) {
        monitor.report(++coordinatesTransformed, -1, "coordinates");

        Triangle sourceTriangle = sourceTriangle(c);
        Assert.isTrue(sourceTriangle != null, "Unable to determine source triangle for " + c);

        Triangle destTriangle = destTriangle(sourceTriangle);

        return destTriangle.toEuclideanCoordinate(sourceTriangle.toSimplicialCoordinate(
                c));
    }

    private Triangle sourceTriangle(Coordinate c) {
 
        for (Iterator i = triangleMap.keySet().iterator(); i.hasNext();) {
            Triangle triangle = (Triangle) i.next();
            if (triangle.getEnvelope().contains(c) && triangle.contains(c)) {
                return triangle;
            }
        }

        return null;
    }

    private Triangle destTriangle(Triangle sourceTriangle) {
        return (Triangle) triangleMap.get(sourceTriangle);
    }
}
