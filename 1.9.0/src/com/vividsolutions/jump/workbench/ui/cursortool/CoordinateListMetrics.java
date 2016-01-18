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
package com.vividsolutions.jump.workbench.ui.cursortool;

import java.util.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.geom.Angle;

/**
 * Generates strings describing metrics for coordinate lists.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class CoordinateListMetrics
{
	String sArea = I18N.get("ui.cursortool.CoordinateListMetrics.Area");
	String sAzimuth = I18N.get("ui.cursortool.CoordinateListMetrics.Azimuth");
	String sAngle = I18N.get("ui.cursortool.CoordinateListMetrics.Angle");
	String sDistance = I18N.get("ui.cursortool.CoordinateListMetrics.Distance"); 
	
  public CoordinateListMetrics() {
  }

  public void displayMetrics(List coordinates, LayerViewPanel panel)
  {
    displayMetrics(coordinates, panel, false);
  }

  /**
   * Display the coordinates metrics with the option to compute the distance for
   * a closed geometry.
   *
   * @param coordinates
   * @param panel
   * @param closedDistance
   */
  public void displayMetrics(List coordinates, LayerViewPanel panel, boolean closedDistance) {
	panel.getContext().setStatusMessage(getMetricsString(coordinates, panel, closedDistance));
  }

  public String getMetricsString(List coordinates, LayerViewPanel panel)
  {
	return getMetricsString(coordinates, panel, false);
  }

  /**
   * Get's the the coordinates metrics with the option to compute the distance
   * for a closed geometry.
   * 14-3-2014 (Giuseppe Aruta) Added compute of last distance (between last point 
   *            and the cursor and the compute of azimuth
   *
   * @param coordinates
   * @param panel
   * @param closedDistance
   * @return the String representing the geometry metrics
   */
  public String getMetricsString(List coordinates, LayerViewPanel panel, boolean closedDistance)
  {
    double dist = distance(coordinates, closedDistance);
    String dispStr = sDistance + ": " + panel.format(dist);
    
    double distlast = distancelast(coordinates, closedDistance);
    dispStr += " (" + panel.format(distlast)+")";

    double angle = angle(coordinates);
    dispStr += "   " + sAngle + ": " + panel.format(angle);
    
    double azimuth = azimuth(coordinates);
    dispStr += "   " + sAzimuth + ": " + panel.format(azimuth);

    if (coordinates.size() > 2) {
      double area = area(coordinates);
      dispStr += "   " + sArea + ": " + panel.format(area);
    }
    return dispStr;
  }

  public static double distance(List coordinates) {
	return distance(coordinates, false);
  }

  /**
   * Computes the distance with the option to compute the distance for
   * a closed geometry.
   *
   * @param coordinates
   * @param closedDistance
   * @return the sum of the distance between coordinates 
   */
  public static double distance(List coordinates, boolean closedDistance)
  {
    double distance = 0;
    for (int i = 1; i < coordinates.size(); i++) {
      distance += ((Coordinate) coordinates.get(i - 1)).distance((Coordinate) coordinates.get(
      i));
    }
	// compute the last distance part from the last coordinate to the first, if we are in closed mode
	if (coordinates.size() > 2 && closedDistance) {
		distance += ((Coordinate) coordinates.get(coordinates.size() - 1)).distance((Coordinate) coordinates.get(0));
	}
    return distance;
  }

  /**
   * Computes the angle between the last 2 segments in the coordinates list
   *
   * @param coordinates
   * @return the angle in degrees
   */
  public static double angle(List coordinates)
  {
    int size = coordinates.size();
    if (size <= 1) return 0.0;
    Coordinate p1 = (Coordinate) coordinates.get(size - 2);
    Coordinate p2 = (Coordinate) coordinates.get(size - 1);
    // if only 2 coords, compute angle relative to X axis
    Coordinate p0 = null;
    if (size > 2)
      p0 = (Coordinate) coordinates.get(size - 3);
    else
      p0 = new Coordinate(p1.x + 1.0, p1.y);

    double angRad = Angle.angleBetween(p1, p0, p2);
    return Math.toDegrees(angRad);
  }

  /**
   * Computes the area for the coordinates list.
   * The area is forcd to be positive.
   * The coordinate list can be open, and the closing coordinate is supplied
   */
  private static double area(List coordinates)
  {
    if (coordinates.size() < 3) return 0.0;
    double sum = 0.0;
    for (int i = 0; i < coordinates.size(); i++) {
      Coordinate b = (Coordinate) coordinates.get(i);
      int nexti = i + 1;
      if (nexti > coordinates.size() - 1)
        nexti = 0;
      Coordinate c = (Coordinate) coordinates.get(nexti);
      sum += (b.x + c.x) * (c.y - b.y);
    }
    double signedArea = -sum / 2.0;
    if (signedArea >= 0)
      return signedArea;
    return -signedArea;
  }

  /**
   * Giuseppe Aruta (Peppe - ma15569) 03-14-2014
   * Computes the angle facing North (upper side of the view)
   *
   * @param coordinates
   * @return the angle in degrees
   */
  public static double azimuth(List coordinates, boolean closedDistance)
  	{
	  	int size = coordinates.size();
	  	if (size <= 1) return 0.0D;
	  	Coordinate p1 = (Coordinate)coordinates.get(size - 2);
	  	Coordinate p2 = (Coordinate)coordinates.get(size - 1);
	  	if (size > 2) {
		} else {
		}
	  	double d = 0.0D;
	  	LineSegment ls = new LineSegment(p1, p2);
	  	d = ls.angle();
	  	double DEG = 90.0D - d * 57.295779513082323D;
	  	double DEG1 = DEG;
	  	if (DEG < 0.0D)
	  		DEG1 += 360.0D;
	  	return DEG1;
  		}

  	public double azimuth(List coordinates) {
  		return azimuth(coordinates, false);
  	}
  

  	 /**
     * Giuseppe Aruta (Peppe - ma15569) 03-14-2014
     * Computes the angle between the last drawn segment 
     * and the position of the cursor
     *
     * @param coordinates
     * @param closedDistance
     * @return the the distance between coordinates of last point and coordinates of cursor
     */
  public static double distancelast(List coordinates, boolean closedDistance)
	   {
	     double distance = 0.0D;
	    double lastSegmentLength = 0.0D;
    for (int i = 1; i < coordinates.size(); i++)
	     {
	      distance += ((Coordinate)coordinates.get(i - 1)).distance((Coordinate)coordinates.get(i));
	      if (i == coordinates.size() - 1) {lastSegmentLength = ((Coordinate)coordinates.get(i - 1)).
	    		  distance((Coordinate)coordinates.get(i));
	       }
	     }
	    return lastSegmentLength;
	  }

   public double distancelast(List coordinates)
	   {
	     return distancelast(coordinates, false);
	   	}
   
   /**
    * Giuseppe Aruta 2015_01_10 code from ConstrainedMamager.class return
    * Bearing in degrees (between EW axes, -180 to +180) from 2 coordinates
    */
   public static double bearing(List coordinates, boolean closedDistance) {

       int size = coordinates.size();
       if (size <= 1)
           return 0.0D;
       Coordinate p1 = (Coordinate) coordinates.get(size - 2);
       Coordinate p2 = (Coordinate) coordinates.get(size - 1);
       if (size > 2) {
       } else {
       }
       Coordinate r = new Coordinate(p2.x - p1.x, p2.y - p1.y);
       double rMag = Math.sqrt(r.x * r.x + r.y * r.y);
       if (rMag == 0.0) {
           return 0.0;
       } else {
           double rCos = r.x / rMag;
           double rAng = Math.acos(rCos);

           if (r.y < 0.0)
               rAng = -rAng;
           return rAng * 360.0 / (2 * Math.PI);
       }
   }

   public double bearing(List coordinates) {
       return distancelast(coordinates, false);
   }
  
}