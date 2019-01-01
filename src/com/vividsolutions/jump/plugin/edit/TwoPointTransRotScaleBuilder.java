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
 * Coordinate[2] array and a dest Coordinate[2] array.
 *
 * @author Martin Davis
 */

class TwoPointTransRotScaleBuilder extends TransRotScaleBuilder {

  /**
   * Creates a builder from two Coordinate[2] arrays defining the src and dest vectors.
   *
   * @param srcVector the two Coordinates defining the src vector
   * @param destVector the two Coordinates defining the dest vector
   */
  TwoPointTransRotScaleBuilder(Coordinate[] srcVector, Coordinate[] destVector)
  {
    super(srcVector, destVector);
  }

  protected void compute(Coordinate[] srcVector, Coordinate[] destVector)
  {
    originX = srcVector[0].x;
    originY = srcVector[0].y;

    double srcLen = srcVector[0].distance(srcVector[1]);
    double destLen = destVector[0].distance(destVector[1]);

    boolean isZeroLength = (srcLen == 0.0 || destLen == 0.0);

    if (! isZeroLength) {
      scaleX = destLen / srcLen;
      scaleY = scaleX;

      double angleSrc = Angle.angle(srcVector[0], srcVector[1]);
      double angleDest = Angle.angle(destVector[0], destVector[1]);
      double angleRad = angleDest - angleSrc;
      angle = Math.toDegrees(angleRad);
    }

    dx = destVector[0].x - srcVector[0].x;
    dy = destVector[0].y - srcVector[0].y;
  }

}
