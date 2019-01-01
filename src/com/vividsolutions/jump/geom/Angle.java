
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

import com.vividsolutions.jts.geom.Coordinate;


/**
 * Utility functions for working with angles.
 */
public class Angle {

  public static final double PI_TIMES_2 = 2.0 * Math.PI;
  public static final double PI_OVER_2 = Math.PI / 2.0;
  public static final double PI_OVER_4 = Math.PI / 4.0;

  /** General constant representing counterclockwise orientation */
  public static int COUNTERCLOCKWISE = 0;

  /** General constant representing clockwise orientation */
  public static int CLOCKWISE = 1;

  /** General constant representing no orientation */
  public static int NONE = 2;

  /**
   * Converts from radians to degrees.
   * @param radians an angle in radians
   * @return the angle in degrees
   */
  public static double toDegrees(double radians) {
      return (radians * 180) / (Math.PI);
  }

  /**
   * Returns the angle of the vector from p0 to p1.
   * The angle will be between -Pi and Pi.
   * @return the angle (in radians) that p0-p1 makes with the positive x-axis.
   */
  public static double angle(Coordinate p0, Coordinate p1) {
      double dx = p1.x - p0.x;
      double dy = p1.y - p0.y;
      return Math.atan2(dy, dx);
  }

  /**
   * Returns the angle of the vector from (0,) to p.
   * The angle will be between -Pi and Pi.
   * @return the angle (in radians) that p makes with the positive x-axis.
   */
  public static double angle(Coordinate p) {
      return Math.atan2(p.y, p.x);
  }

  /**
   * Converts from degrees to radians.
   * @param angleDegrees an angle in degrees
   * @return the angle in radians
   */
  public static double toRadians(double angleDegrees) {
      return (angleDegrees * Math.PI) / 180.0;
  }

  /**
   * Returns the angle between two vectors. Will be between 0 and Pi.
   * @param tail the tail of each vector
   * @param tip1 the tip of one vector
   * @param tip2 the tip of the other vector
   * @return the angle between tail-tip1 and tail-tip2
   */
  public static double angleBetween(Coordinate tail, Coordinate tip1,
      Coordinate tip2) {
      double a1 = angle(tail, tip1);
      double a2 = angle(tail, tip2);

      return diff(a1, a2);
  }

  /**
   * Computes the interior angle between two segments of a ring.
   * The ring is assumed to be oriented in a clockwise direction.
   * @param p0 a point of the ring
   * @param p1 the next point of the ring
   * @param p2 the next point of the ring
   * @return the interior angle based at <code>p1</code>
   */
  public static double interiorAngle(Coordinate p0, Coordinate p1, Coordinate p2)
  {
    double anglePrev = Angle.angle(p1, p0);
    double angleNext = Angle.angle(p1, p2);
    return Math.abs(angleNext - anglePrev);
  }

  /**
   * Returns whether an angle must turn clockwise or counterclockwise
   * to overlap another angle.
   * @param a1 an angle in radians
   * @param a2 an angle in radians
   * @return whether a1 must turn CLOCKWISE, COUNTERCLOCKWISE or NONE to
   * overlap a2.
   */
  public static int getTurn(double a1, double a2) {
      double crossproduct = Math.sin(a2 - a1);

      if (crossproduct > 0) {
          return COUNTERCLOCKWISE;
      }

      if (crossproduct < 0) {
          return CLOCKWISE;
      }

      return NONE;
  }

  /**
   * Computes the normalized value of an angle, which is the
   * equivalent angle lying between -Pi and Pi.
   *
   * @param angle the angle to compute the normalized value of
   * @return the normalized value of the angle
   */
  public static double normalize(double angle)
  {
    while (angle > Math.PI)
      angle -= PI_TIMES_2;
    while (angle < -Math.PI)
      angle += PI_TIMES_2;
    return angle;
  }
  /**
   * Returns the angle between two vectors.
   * @param a1 the angle of one vector, between -Pi and Pi
   * @param a2 the angle of the other vector, between -Pi and Pi
   * @return the angle (in radians) between the two vectors, between 0 and Pi
   */
  public static double diff(double a1, double a2) {
      double da;

      if (a1 < a2) {
          da = a2 - a1;
      } else {
          da = a1 - a2;
      }

      if (da > Math.PI) {
          da = (2 * Math.PI) - da;
      }

      return da;
  }

}
