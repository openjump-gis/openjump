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

/**
 * Computes a translation, scale and rotation
 * from two vectors (each with a start and end point)
 *
 * @author Martin Davis
 * @version 1.0
 */
abstract class TransRotScaleBuilder {
  protected double originX = 0.0;
  protected double originY = 0.0;
  protected double scaleX = 0.0;
  protected double scaleY = 0.0;
  protected double dx = 0.0;
  protected double dy = 0.0;
  protected double angle = 0.0;  // in degrees

  public TransRotScaleBuilder(Coordinate[] srcPts, Coordinate[] destPts)
  {
    compute(srcPts, destPts);
  }

  protected abstract void compute(Coordinate[] srcPts, Coordinate[] destPts);

  public double getOriginX() { return originX; }
  public double getOriginY() { return originY; }

  public boolean isScale() { return scaleX > 0.0; }
  public double getScaleX() { return scaleX; }
  public double getScaleY() { return scaleY; }

  public boolean isTranslate() { return dx != 0.0 | dy != 0.0; }
  public double getTranslateX() { return dx; }
  public double getTranslateY() { return dy; }

  public double getRotationAngle() { return angle; }

}
