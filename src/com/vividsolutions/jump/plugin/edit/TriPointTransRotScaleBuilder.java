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


package com.vividsolutions.jump.plugin.edit;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.geom.*;

/**
 * Class used by {@link AffineTransformationPlugIn} to build a transformation from a src
 * Coordinate[3] array and a dest Coordinate[3] array.
 *
 * @author Martin Davis
 */

class TriPointTransRotScaleBuilder extends TransRotScaleBuilder {

  /**
   * Creates a builder from two Coordinate[3] arrays defining the src and dest control points
   *
   * @param srcPt the two Coordinates defining the src vector
   * @param destPt the two Coordinates defining the dest vector
   */
  TriPointTransRotScaleBuilder(Coordinate[] srcPt, Coordinate[] destPt)
  {
    super(srcPt, destPt);
  }

  protected void compute(Coordinate[] srcPt, Coordinate[] destPt)
  {
    //For now just extract a Y scale from the third pt.
    //In future could do shear too.

    originX = srcPt[1].x;
    originY = srcPt[1].y;

    double srcLenBase = srcPt[1].distance(srcPt[2]);
    double destLenBase = destPt[1].distance(destPt[2]);

    double srcLenSide = srcPt[0].distance(srcPt[1]);
    double destLenSide = destPt[0].distance(destPt[1]);

    boolean isZeroLength = (srcLenBase == 0.0
                            || destLenBase == 0.0
                            || srcLenSide == 0.0
                            || destLenSide == 0.0
                            );


    if (! isZeroLength) {
      scaleX = destLenBase / srcLenBase;
      scaleY = destLenSide / srcLenSide;

      double angleSrc = Angle.angle(srcPt[1], srcPt[2]);
      double angleDest = Angle.angle(destPt[1], destPt[2]);
      double angleRad = angleDest - angleSrc;
      angle = Math.toDegrees(angleRad);
    }

    dx = destPt[1].x - srcPt[1].x;
    dy = destPt[1].y - srcPt[1].y;
  }

}
