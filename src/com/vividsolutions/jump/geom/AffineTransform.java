
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
 * An AffineTransform applies an affine transforms to a JTS Geometry.
 * The transform is done in-place. If the object must not be changed,
 * it should be cloned and the transform applied to the clone.
 *
 * <b>NOTE: INCOMPLETE IMPLEMENTATION</b>
 * 
 * @deprecated see wrap package (curently used by AffineTransformPlugIn) and
 * com.vividsolutions.jts.geom.util.AffineTransformation in JTS library
 * 
 * AffineTransformPlugIn already use AffineTransform in the warp package [michaudm]
 * Next step is to deprecate AffineTransform in the warp package and to use
 * directly com.vividsolutions.jts.geom.util.AffineTransformation for AffineTransformPlugIn
 */
public class AffineTransform implements CoordinateFilter {
    //There is an AffineTransform in the warp package. I wonder if it can be
    //used instead of this class. [Jon Aquino]

    private Coordinate transPt = null;

    public AffineTransform() {
    }

    /**
     * Append a translation to the transform.
     *
     * @param p the vector to translate by
     */
    public void translate(Coordinate p) {
        if (transPt == null) {
            transPt = new Coordinate(p);
        } else {
            transPt.x += p.x;
            transPt.y += p.y;
            transPt.z += p.z;
        }
    }

    public void apply(Geometry g) {
        g.apply(this);
    }

    public void filter(Coordinate coord) {
        coord.x += transPt.x;
        coord.y += transPt.y;
        coord.z += transPt.z;
    }
}
