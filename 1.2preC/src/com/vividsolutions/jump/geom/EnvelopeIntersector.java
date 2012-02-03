
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

import com.vividsolutions.jts.algorithm.SimplePointInAreaLocator;
import com.vividsolutions.jts.geom.*;


public class EnvelopeIntersector {
    private static LineSegmentEnvelopeIntersector lineSegmentEnvelopeIntersector =
        new LineSegmentEnvelopeIntersector();

    public EnvelopeIntersector() {
    }

    /**
     * Returns whether the Geometry and the Envelope intersect, even if the
     * Geometry is invalid. Checks whether any points or line segments of the
     * geometry intersect the Envelope, or whether the Envelope is wholly
     * contained in any polygons.
     * <p>
     * This code will only work correctly on valid geometries.
     * (For instance, it may return <code>false</code> if the Envelope is
     * wholly contained in an <b>invalid</b> polygon.
     *
     * @param envelope the Envelope to test
     * @param geometry the Geometry to test, whether valid or not
     * @return whether the Envelope intersects the Geometry
     */
    public static boolean intersects(Geometry geometry, Envelope envelope) {
        if (envelope.isNull()) {
            return false;
        }

        if (!envelope.intersects(geometry.getEnvelopeInternal())) {
            return false;
        }

        if (geometry instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) geometry;

            for (int i = 0; i < gc.getNumGeometries(); i++) {
                if (intersects(gc.getGeometryN(i), envelope)) {
                    return true;
                }
            }

            return false;
        }

        if (intersectsBoundary(geometry, envelope)) {
            return true;
        }

        if (geometry instanceof Polygon) {
            return contains((Polygon) geometry,
                new Coordinate(envelope.getMinX(), envelope.getMinY()));
        }

        return false;
    }

    private static boolean contains(Polygon polygon, Coordinate c) {
        return SimplePointInAreaLocator.containsPointInPolygon(c, polygon);
    }

    private static boolean intersectsBoundary(Geometry geometry,
        final Envelope envelope) {
        final BooleanWrapper intersects = new BooleanWrapper(false);
        geometry.apply(new GeometryComponentFilter() {
                public void filter(Geometry geometry) {
                    Coordinate[] coordinates = geometry.getCoordinates();

                    if (intersects.value) {
                        return;
                    }

                    if (envelope.contains(coordinates[0])) {
                        intersects.value = true;
                    }

                    for (int i = 1; i < coordinates.length; i++) { //1

                        if (intersectsLineSegment(coordinates[i],
                                    coordinates[i - 1], envelope)) {
                            intersects.value = true;
                        }
                    }
                }
            });

        return intersects.value;
    }

    private static boolean intersectsLineSegment(Coordinate a, Coordinate b,
        Envelope envelope) {
        return lineSegmentEnvelopeIntersector.touches(a, b, envelope);
    }

    private static class BooleanWrapper {
        public boolean value;

        public BooleanWrapper(boolean value) {
            this.value = value;
        }
    }
}
