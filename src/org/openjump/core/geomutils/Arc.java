/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */
 
package org.openjump.core.geomutils;

import org.openjump.core.geomutils.GeoUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Arc
{
    protected Coordinate center = new Coordinate(0,0);
    protected Coordinate start = new Coordinate(0,0);
    protected double radius = 0.0;
    protected double angle = 0.0;
    protected double arcTolerance = 0.1;
    
    public Arc(Coordinate center, Coordinate start, double angle)
    {
        this.center = center;
        this.start = start;
        this.angle = angle;
        radius = Math.sqrt(center.distance(start) * center.distance(start));
    }
    
    public void setArcTolerance(double arcTolerance)
    {
        this.arcTolerance = arcTolerance;
    }
    
    public Polygon getPoly()
    {
        if (angle == 360.0)
        {
            CoordinateList polyCoords = arcAnglePts(angle, start, center);
            return new GeometryFactory().createPolygon( new GeometryFactory().createLinearRing(polyCoords.toCoordinateArray()),null);
        }
        else
        {
            CoordinateList polyCoords = new CoordinateList();
            polyCoords.add(center);
            polyCoords.add(start);
            CoordinateList coordinates = arcAnglePts(angle, start, center);
            polyCoords.add(coordinates.toCoordinateArray(), true);
            polyCoords.add(center);
            return new GeometryFactory().createPolygon( new GeometryFactory().createLinearRing(polyCoords.toCoordinateArray()),null);
        }
    }
    
    public LineString getLineString()
    {
        CoordinateList coordinates = arcAnglePts(angle, start, center);
        return new GeometryFactory().createLineString(coordinates.toCoordinateArray());
    }
    
    
    /*
     * Giuseppe Aruta - 2015_01_10 - Get the last point of an arc
     */

    public Point getLastPointArc() {
        if (this.angle == 360.0D) {
            return new GeometryFactory().createPoint(new Coordinate(GeoUtils
                    .rotPt(this.start, this.center, this.angle)));
        }
        CoordinateList polyCoords = new CoordinateList();
        CoordinateList coordinates = arcAnglePts(this.angle, this.start,
                this.center);
        polyCoords.add(coordinates.toCoordinateArray(), true);
        polyCoords.add(new Coordinate(GeoUtils.rotPt(this.start, this.center,
                this.angle)));
        return new GeometryFactory().createPoint(new Coordinate(GeoUtils.rotPt(
                this.start, this.center, this.angle)));
    }

    /*
     * Giuseppe Aruta - 2015_01_10 - Get the middle point of an arc
     */

    public Point getMiddlePointArc() {
        if (this.angle == 360.0D) {
            double anglem = this.angle / 2.0D;
            return new GeometryFactory().createPoint(new Coordinate(GeoUtils
                    .rotPt(this.start, this.center, anglem)));
        }
        double anglem = this.angle / 2.0D;
        CoordinateList polyCoords = new CoordinateList();
        CoordinateList coordinates = arcAnglePts(anglem, this.start,
                this.center);
        polyCoords.add(coordinates.toCoordinateArray(), true);
        polyCoords.add(new Coordinate(GeoUtils.rotPt(this.start, this.center,
                anglem)));
        return new GeometryFactory().createPoint(new Coordinate(GeoUtils.rotPt(
                this.start, this.center, anglem)));
    }
    
    public CoordinateList getCoordinates()
    {
        return arcAnglePts(angle, start, center);
    }
    
    protected CoordinateList arcAnglePts(double angle, Coordinate pt, Coordinate center)
    {
        CoordinateList coordinates = new CoordinateList();
        int n = getPtsFromTolerance(center.distance(pt), angle, arcTolerance);
        if (n < 3) n = 3;
        double ai = angle / n;
        coordinates.add(new Coordinate(pt));
        
        for (int i = 1; i < n; i++) //add all but the last one
        {
            Coordinate p2 = GeoUtils.rotPt(pt, center, ai*i);
            coordinates.add(new Coordinate(p2));
        }
        
        if (angle == 360.0)
        {
            coordinates.add(new Coordinate(pt)); //close the circle
        }
        else
        {
            coordinates.add(new Coordinate(GeoUtils.rotPt(pt, center, angle))); //add the last point
        }
        return coordinates;
    }
    
    protected int getPtsFromTolerance(double radius, double angle, double tolerance)
    {
        //Tolerance is the distance from the center of a chord to the arc
        //For the given arc, this function will return the number of points
        //that is needed to draw the arc with the given tolerance
        
        final double epsilon = 0.00001;
        int n;
        
        if (radius < epsilon)
        {
            n = 1;
        }
        else
        {
            if ((tolerance / radius) > 0.333)
            {
                n = 1;
            }
            else
            {
                double theta = Math.toDegrees(2 * Math.acos((radius - tolerance) / radius));
                if (theta < epsilon)
                {
                    n = 0;
                }
                else
                {
                    n = (int) Math.floor(Math.abs(angle) / theta) + 1;
                }
            }
        }
        return n;
    }
}