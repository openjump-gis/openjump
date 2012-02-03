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

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.*;

/**
 * Reduces the precision of a {@link Geometry}
 * according to the supplied CoordinatePrecisionReducer
 */
public class GeometryPrecisionReducer
{
  private CoordinatePrecisionReducer coordPrecReducer;
  private GeometryEditor geomEdit = new GeometryEditor();

  public GeometryPrecisionReducer(NumberPrecisionReducer numberPrecReducer)
  {
    this.coordPrecReducer = new CoordinatePrecisionReducer(numberPrecReducer);
  }

  public Geometry reduce(Geometry geom)
  {
    return geomEdit.edit(geom, new PrecisionReducerCoordinateOperation());
  }

  private class PrecisionReducerCoordinateOperation
      extends GeometryEditor.CoordinateOperation
  {
    public Coordinate[] edit(Coordinate[] coordinates, Geometry geom)
    {
      for (int i = 0; i < coordinates.length; i++) {
        coordPrecReducer.reducePrecision(coordinates[i]);
      }
      // remove repeated points
      CoordinateList noRepeatedCoordList = new CoordinateList(coordinates, false);
      Coordinate[] noRepeatedCoord = noRepeatedCoordList.toCoordinateArray();

      /**
       * Check to see if the removal of repeated points
       * collapsed the coordinate List to an invalid length
       * for the type of the parent geometry.
       * If this is the case, return the orginal coordinate list.
       * Note that the returned geometry will still be invalid, since it
       * has fewer unique coordinates than required. This check simply
       * ensures that the Geometry constructors won't fail.
       * It is not necessary to check for Point collapses, since the coordinate list can
       * never collapse to less than one point
       */
      if (geom instanceof LinearRing && noRepeatedCoord.length <= 3) return coordinates;
      if (geom instanceof LineString && noRepeatedCoord.length <= 1) return coordinates;

      return noRepeatedCoord;
    }
  }
}
