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

package com.vividsolutions.jump.geom.precision;

/**
 * Reduces the precision of a number
 * by rounding it off after scaling by a given scale factor.
 */
public class NumberPrecisionReducer {

  /**
   * Computes the scale factor for a given number of decimal places.
   * A negative value for decimalPlaces indicates the scale factor
   * should be divided rather than multiplied. The negative sign
   * is carried through to the computed scale factor.
   * @param decimalPlaces
   * @return the scale factor
   */
  public static double scaleFactorForDecimalPlaces(int decimalPlaces)
  {
    int power = Math.abs(decimalPlaces);
    int sign = decimalPlaces >= 0 ? 1 : -1;
    double scaleFactor = 1.0;
    for (int i = 1; i <= power; i++) {
      scaleFactor *= 10.0;
    }
    return scaleFactor * sign;
  }

  private double scaleFactor = 0.0;
  private boolean multiplyByScaleFactor = true;

  public NumberPrecisionReducer() {
  }

  /**
   * A negative value for scaleFactor indicates
   * that the precision reduction will eliminate significant digits
   * to the left of the decimal point.
   * (I.e. the scale factor
   * will be divided rather than multiplied).
   * A zero value for scaleFactor will result in no precision reduction being performed.
   * A scale factor is normally an integer value.
   *
   * @param scaleFactor
   */
  public NumberPrecisionReducer(double scaleFactor)
  {
    setScaleFactor(scaleFactor);
  }

  public void setScaleFactor(double scaleFactor)
  {
    this.scaleFactor = Math.abs(scaleFactor);
    multiplyByScaleFactor = scaleFactor >= 0;
  }


  public double reducePrecision(double d)
  {
    // sanity check
    if (scaleFactor == 0.0) return d;

    if (multiplyByScaleFactor) {
      double scaled = d * scaleFactor;
      return Math.floor(scaled + 0.5) / scaleFactor;
    }
    else {
      double scaled = d / scaleFactor;
      return Math.floor(scaled + 0.5) * scaleFactor;
    }

  }

}
