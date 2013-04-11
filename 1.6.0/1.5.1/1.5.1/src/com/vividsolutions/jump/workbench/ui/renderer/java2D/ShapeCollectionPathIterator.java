
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

package com.vividsolutions.jump.workbench.ui.renderer.java2D;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.util.Collection;
import java.util.Iterator;


public class ShapeCollectionPathIterator implements PathIterator {
    private Iterator shapeIterator;
    private PathIterator currentPathIterator = new PathIterator() {
            public int getWindingRule() {
                throw new UnsupportedOperationException();
            }

            public boolean isDone() {
                return true;
            }

            public void next() {
            }

            public int currentSegment(float[] coords) {
                throw new UnsupportedOperationException();
            }

            public int currentSegment(double[] coords) {
                throw new UnsupportedOperationException();
            }
        };

    private AffineTransform affineTransform;
    private boolean done = false;

    public ShapeCollectionPathIterator(Collection shapes,
        AffineTransform affineTransform) {
        shapeIterator = shapes.iterator();
        this.affineTransform = affineTransform;
        next();
    }

    public int getWindingRule() {
        //WIND_NON_ZERO is more accurate than WIND_EVEN_ODD, and can be comparable
        //in speed. (See http://www.geometryalgorithms.com/Archive/algorithm_0103/algorithm_0103.htm#Winding%20Number)
        //[Jon Aquino]
        //Nah, switch back to WIND_EVEN_ODD -- WIND_NON_ZERO requires that the
        //shell and holes be oriented in a certain way. [Jon Aquino]
        return PathIterator.WIND_EVEN_ODD;
    }

    public boolean isDone() {
        return done;
    }

    public void next() {
        currentPathIterator.next();

        if (currentPathIterator.isDone() && !shapeIterator.hasNext()) {
            done = true;

            return;
        }

        if (currentPathIterator.isDone()) {
            currentPathIterator = ((Shape) shapeIterator.next()).getPathIterator(affineTransform);
        }
    }

    public int currentSegment(float[] coords) {
        return currentPathIterator.currentSegment(coords);
    }

    public int currentSegment(double[] coords) {
        return currentPathIterator.currentSegment(coords);
    }
}
