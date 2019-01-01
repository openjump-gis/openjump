package org.openjump.core.ui.util;

import java.lang.reflect.Constructor;
import java.security.InvalidParameterException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.workbench.Logger;

public class GeometryUtils {

  /**
   * Method to scale a selected geometry of a scale factor
   * 
   * @param input
   *          geometry
   * @param double. Scale factor (50 = half, 100 = no rescale, 200 = scale two
   *        times)
   */
  public static void scaleGeometry(Geometry geometry, final double scale) {
    final Coordinate center = geometry.getCentroid().getCoordinate();
    geometry.apply(new CoordinateFilter() {
      public void filter(Coordinate coordinate) {
        coordinate.x = center.x + (scale / 100) * (coordinate.x - center.x);
        coordinate.y = center.y + (scale / 100) * (coordinate.y - center.y);
      }
    });
  }

  /**
   * Method to clock wise rotate a geometry of a defined angle
   * 
   * @param input
   *          geometry
   * @param angle
   *          in degree
   */
  public static void rotateGeometry(Geometry geometry, final double angle) {
    final Coordinate center = geometry.getCentroid().getCoordinate();
    geometry.apply(new CoordinateFilter() {
      public void filter(Coordinate coordinate) {
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        double x = coordinate.x - center.x;
        double y = coordinate.y - center.y;
        coordinate.x = center.x + (x * cosAngle) - (y * sinAngle);
        coordinate.y = center.y + (y * cosAngle) + (x * sinAngle);
      }
    });
  }

  /**
   * Method to counterclock wise rotate a geometry of a defined angle
   * 
   * @param input
   *          geometry
   * @param angle
   *          in degree
   */
  public static void rotateGeometry(Geometry geometry, final int angle) {
    final Coordinate center = geometry.getCentroid().getCoordinate();
    double Deg2Rad = 0.0174532925199432;
    double radiansAngle = 0.0;
    radiansAngle = Deg2Rad * (-angle);
    final double cosAngle = Math.cos(radiansAngle);
    final double sinAngle = Math.sin(radiansAngle);
    geometry.apply(new CoordinateFilter() {
      public void filter(Coordinate coordinate) {
        double x = coordinate.x - center.x;
        double y = coordinate.y - center.y;
        coordinate.x = center.x + (x * cosAngle) - (y * sinAngle);
        coordinate.y = center.y + (y * cosAngle) + (x * sinAngle);
      }
    });
  }

  /**
   * Move a geometry to a defined coordinate
   * 
   * @param geometry
   * @param coordinate
   *          to move
   */
  public static void centerGeometry(final Geometry geometry,
      final Coordinate displacement) {
    geometry.apply(new CoordinateFilter() {
      public void filter(Coordinate coordinate) {
        coordinate.setCoordinate(CoordUtil.add(coordinate, displacement));
      }
    });
  }

  /**
   * creates an empty geometry matching the geom type set already or an empty
   * geom collection if that fails
   * 
   * @return geometry
   */
  public static Geometry createEmptyGeometry(Class geometryClass,
      GeometryFactory geometryFactory) {
    if (geometryClass == null)
      throw new InvalidParameterException("Class must not be null");
    if (geometryFactory == null)
      geometryFactory = new GeometryFactory();
    
    try {
      for (Constructor<Geometry> c : geometryClass.getConstructors()) {
        Class[] paramTypes = c.getParameterTypes();
        int paramCount = paramTypes.length;
        if (paramCount > 0
            && paramTypes[paramCount - 1] == GeometryFactory.class) {
          Object[] params = new Object[paramCount];
          params[paramCount - 1] = geometryFactory;
          return c.newInstance(params);
        }
      }
    } catch (Exception e) {
      Logger.debug(e);
    }

    return null;
  }

  public static Geometry createEmptyGeometry(String geometryName,
      GeometryFactory geometryFactory) throws ClassNotFoundException {
    
    Class geometryClass = Class.forName("com.vividsolutions.jts.geom."+geometryName);
    return createEmptyGeometry(geometryClass, geometryFactory);
  }
}
