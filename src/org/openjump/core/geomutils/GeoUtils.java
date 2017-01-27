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

import java.util.ArrayList;
import java.util.BitSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.UniqueCoordinateArrayFilter;
import com.vividsolutions.jump.feature.Feature;

/**
 * @author Larry
 *
 */
public class GeoUtils
{
	public static final int emptyBit = 0;
	public static final int pointBit = 1;
	public static final int lineBit = 2;
	public static final int polyBit = 3;
	
    public GeoUtils()
    {
    }
    
    public static double mag(Coordinate q)
    {
    	return Math.sqrt(q.x * q.x + q.y * q.y );
    }

    public static double distance(Coordinate p1, Coordinate p2)
    {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt((dx*dx)+(dy*dy));
    }

    public static Coordinate unitVec(Coordinate q)
    {
		double m = mag(q);
		if (m == 0) m = 1;
		return new Coordinate(q.x / m, q.y / m);
    }

    public static Coordinate vectorAdd(Coordinate q, Coordinate r)
    {   //return the Coordinate by vector adding r to q
         return new Coordinate(q.x + r.x, q.y + r.y);
    }
    
    public static Coordinate vectorBetween(Coordinate q, Coordinate r)
    {   //return the Coordinate by vector subtracting q from r
         return new Coordinate(r.x - q.x, r.y - q.y);
    }

    public static Coordinate vectorTimesScalar(Coordinate q, double m)
    {   //return the Coordinate by vector subracting r from q
         return new Coordinate(q.x * m, q.y * m);
    }
        
    public static double dot(Coordinate p, Coordinate q)
    {
    	return p.x * q.x + p.y * q.y;
    }

    public static Coordinate rotPt(Coordinate inpt, Coordinate rpt, double theta)
    {   //rotate inpt about rpt by theta degrees (+ clockwise)
        double tr = Math.toRadians(theta);
        double ct = Math.cos(tr);
        double st = Math.sin(tr);
        double x = inpt.x - rpt.x;
        double y = inpt.y - rpt.y;
        double xout = rpt.x + x * ct + y * st;
        double yout = rpt.y + y * ct - st * x;
        return new Coordinate(xout, yout);
    }
   
    public static boolean pointToRight(Coordinate pt, Coordinate p1, Coordinate p2)
    {   //true if pt is to the right of the line from p1 to p2
        double a = p2.x - p1.x;
        double b = p2.y - p1.y;
        double c = p1.y * a - p1.x * b;
        double fpt = a * pt.y - b * pt.x - c; //Ay - Bx - C = 0
        return (fpt < 0.0);
    }
    
    public static Coordinate perpendicularVector(Coordinate v1, Coordinate v2, double dist, boolean toLeft) 
    {
    	//return perpendicular Coordinate vector from v1 of dist specified to left of v1-v2}
    	Coordinate v3 = vectorBetween(v1,v2);
    	Coordinate v4 = new Coordinate();     	
    	if (toLeft)
    	{
    		v4.x = -v3.y;
    		v4.y = v3.x;
    	}
    	else
    	{
    		v4.x = v3.y;
    		v4.y = -v3.x;
    	}
       	return vectorAdd(v1, vectorTimesScalar( unitVec(v4), dist));
     }
    
     
    public static double getBearing180(Coordinate startPt, Coordinate endPt)
    {   //return Bearing in degrees (-180 to +180) from startPt to endPt
        Coordinate r = new Coordinate(endPt.x - startPt.x, endPt.y - startPt.y);
        double rMag = Math.sqrt(r.x * r.x + r.y * r.y );
        if (rMag == 0.0)
        {
            return 0.0;
        }
        else
        {
            double rCos = r.x / rMag;
            double rAng = Math.acos(rCos);
            
            if (r.y < 0.0)
                rAng = -rAng;
            return rAng * 360.0 / (2 * Math.PI);
        }
    }

    public static double getBearingRadians(Coordinate startPt, Coordinate endPt)
    {   //return Bearing in degrees (-PI to +PE) from startPt to endPt
        Coordinate r = new Coordinate(endPt.x - startPt.x, endPt.y - startPt.y);
        double rMag = Math.sqrt(r.x * r.x + r.y * r.y );
        if (rMag == 0.0)
        {
            return 0.0;
        }
        else
        {
            double rCos = r.x / rMag;
            double rAng = Math.acos(rCos);           
            if (r.y < 0.0)
                rAng = -rAng;
           return rAng;
        }
    }

    public static double getBearing360(Coordinate startPt, Coordinate endPt)
    {  //return Bearing in degrees (0 - 360) from startPt to endPt
        double bearing = getBearing180(startPt, endPt);
        if (bearing < 0)
        {
            bearing = 360 + bearing;
        }
        return bearing;
    }
    
    public static double theta(Coordinate p1, Coordinate p2)
    {   //this function returns the order of the angle from p1 to p2
        //special use in ConvexHullWrap
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double t = ax + ay;
        if (t != 0.0)
            t = dy / t;
        if (dx < 0.0)
            t = 2.0 - t;
        else
        {
            if (dy < 0.0)
                t = 4.0 + t;
        }
        return (t * 90.0);
    }
    
    public static CoordinateList ConvexHullWrap( CoordinateList coords )
    {
        //The convex hull is the linestring made by the points on the outside of a cloud of points.
        //Thmin = 0, e package wrapping algorithm - see  Algorithms by Sedgewick
        //modified to handle colinear points 28 Jan 2005 by LDB and RFL @ ISA
        //this version removes colinear points on the hull except for the corners
        CoordinateList newcoords = new CoordinateList();
        int n = coords.size();
        int i, m;
        double t, minAngle, dist, distMax, v, vdist;
        Coordinate[] p = new Coordinate[n+1];
        for (i=0; i<n; i++)
        {
            p[i] = coords.getCoordinate(i);
        }
        int min = 0;
        for (i = 1; i < n; i++)
        {
            if (p[i].y < p[min].y)
                min = i;
        }
        p[n] = coords.getCoordinate(min);
        minAngle = 0.0;
        distMax = 0.0;
        for (m = 0; m < n; m++)
        {
            //swap(p, m, min);
            Coordinate temp = p[m];
            p[m] = p[min];
            p[min] = temp;
            min = n;
            v = minAngle;
            vdist = distMax;
            minAngle = 360.0;
            for (i = m+1; i <= n; i++)
            {
                t = theta(p[m], p[i]);
                dist = p[m].distance(p[i]);
                if ((t > v) || ((t == v) && (dist > vdist)))
                {
                    if ((t < minAngle) || ((t == minAngle) && (dist > distMax)))
                    {
                        min = i;
                        minAngle = t;
                        distMax = dist;
                    }
                }
            }
            if (min == n)
            { //sentinal found
                for (int j = 0; j <= m; j++)
                    newcoords.add(p[j],true);
                if (!(p[0].equals2D(p[m])))
                {
                    newcoords.add(p[0],true);
                }
                
                LinearRing lr = new GeometryFactory().createLinearRing(newcoords.toCoordinateArray());
                if (! clockwise(lr))
                {
                	CoordinateList newcoordsCW = new CoordinateList();
                	for (int j = newcoords.size() - 1; j >=0; j--)
                		newcoordsCW.add(newcoords.getCoordinate(j));
                	return newcoordsCW;
                }
                else
                {	
                	return newcoords;
                }
            }
        }
        return newcoords; //should never get here
    }
    
    public static double getDistance(Coordinate pt, Coordinate p0, Coordinate p1)
    {   //will return the distance from pt to the line segment p0-p1
    	return pt.distance(getClosestPointOnSegment(pt, p0, p1));
    }
    
    public static Coordinate getClosestPointOnSegment(Coordinate pt, Coordinate p0, Coordinate p1)
    {   //will return the coordinate on the line segment p0-p1 which is closest to pt
        double X0, Y0, X1, Y1, Xv, Yv, Xr, Yr, Xp0r, Yp0r, Xp1r, Yp1r;
        double Xp, Yp;
        double t, VdotV, DistP0toR, DistP1toR;
        Coordinate coordOut = new Coordinate(0,0);
        
        X0 = p0.x; Y0 = p0.y;
        X1 = p1.x; Y1 = p1.y;
        Xr = pt.x; Yr = pt.y;
        Xv = X1 - X0; Yv = Y1 - Y0;
        VdotV = Xv * Xv + Yv * Yv;
        
        Xp0r = Xr - X0;
        Yp0r = Yr - Y0;
        DistP0toR = Math.sqrt(Xp0r * Xp0r + Yp0r * Yp0r);
        
        if (VdotV == 0.0) //degenerate line (p0, p1 the same)
        {
        	coordOut.x = p0.x;
        	coordOut.y = p0.y;
            return coordOut; 
        }
        
        t = (Xp0r * Xv + Yp0r * Yv) / VdotV; //Dot(VectorBetween(P0, R), V) / VdotV
        
        if ((t >= 0.0) && (t <= 1.0)) //P(t) is between P0 and P1
        {
            Xp = (X0 + t * Xv) - Xr; //VectorBetween(R, VectorAdd(P0, VectorTimesScalar(V, t)))}
            Yp = (Y0 + t * Yv) - Yr;
            coordOut.x = pt.x + Xp;
            coordOut.y = pt.y + Yp;
        }
        else //P(t) is outside the interval P0 to P1
        {
            Xp1r = Xr - X1;
            Yp1r = Yr - Y1;
            DistP1toR = Math.sqrt(Xp1r*Xp1r + Yp1r*Yp1r);
            
            if (DistP1toR < DistP0toR) // Min( Dist(P0, R), Dist(P1, R) ))
            {
            	coordOut = new Coordinate(p1);
            	coordOut.x = p1.x;
            	coordOut.y = p1.y;
            }
            else
            {
            	coordOut = new Coordinate(p0);
            	coordOut.x = p0.x;
            	coordOut.y = p0.y;
            }
        }
        return coordOut;
    }

    public static Coordinate getClosestPointOnLine(Coordinate pt, Coordinate p0, Coordinate p1)
    {   //returns the nearest point from pt to the infinite line defined by p0-p1
        MathVector vpt = new MathVector(pt);
        MathVector vp0 = new MathVector(p0);
        MathVector vp1 = new MathVector(p1);
        MathVector v = vp0.vectorBetween(vp1);
        double vdotv = v.dot(v);
        
        if (vdotv == 0.0) //degenerate line (ie: P0 = P1)
        {
            return p0;
        }
        else
        {
            double t = vp0.vectorBetween(vpt).dot(v) / vdotv;
            MathVector vt = v.scale(t);
            vpt = vp0.add(vt);
            return vpt.getCoord();
        }
    }
    
    public static Coordinate along(double d, Coordinate q, Coordinate r)
    {   //return the point at distance d along vector from q to r
        double ux, uy, m;
        Coordinate n = (Coordinate)r.clone();
        ux = r.x - q.x;
        uy = r.y - q.y;
        m = Math.sqrt(ux * ux + uy * uy );
        if (m != 0)
        {
            ux = d * ux / m;
            uy = d * uy / m;
            n.x = q.x + ux;
            n.y = q.y + uy;
        }
        return n;
    }
    
    public static double interiorAngle(Coordinate p1, Coordinate p2, Coordinate p3) {
  	//return the angle in radians between vectors p2-p1 and p2-p3  from 0 to 180
  	//NOTE: this routine returns POSITIVE angles only
    	Coordinate p = vectorBetween(p1,p2);  //relativize the position vectors
    	Coordinate q = vectorBetween(p3,p2);
    	double arg = dot(p,q) / (mag(p)*mag(q));
    	if (arg < -1.0) arg = -1.0;
    	else if (arg > 1.0) arg = 1.0;
    	return Math.toDegrees(Math.acos(arg));
    }
    
    /**
     * @param ring - LinearRing represented as LineString to analyze
     * @return - Coordinate[] with first point of passed LineString in [0] 
     * followed by [1-length] with x as distance and y as angle.
     * The angle will be the  will be the absolute bearing in the range 0-360.
     * The original LineString and Coordinate points are unmodified.
     */
    public static Coordinate[] getDistanceBearingArray(LineString ring) {
     	Coordinate[] coords = new Coordinate[ring.getNumPoints()];
   	 	Coordinate p1 = new Coordinate(ring.getCoordinateN(0));
     	coords[0] = p1;
		for (int i = 1; i<coords.length; i++ ) {
 			coords[i] = new Coordinate(ring.getCoordinateN(i));
	   	 	Coordinate p2 = coords[i];
	   	 	double angle = getBearing360(p1,p2);
	   	 	double distance = p1.distance(p2);
	   	 	p1.x = p2.x;
	   	 	p1.y = p2.y;
	   	 	coords[i].x = distance;
	   	 	coords[i].y = angle;	   	 	
		}
     	return coords;
    }
        
   /**
     * @param ring - LinearRing represented as LineString to analyze
     * @return - Coordinate[] array of with x as distance and y as 
     * interior angles in degrees 0 to +180.
     * The angles at each index in the array are the interior angles at the 
     * vertex position in the (closed polygon) ring.  Every array position if filled.
     * The distances are the distance at a vertex to the following point.  For [n-2] 
     * the distance is computed to the [n-1] position assuming the ring is closed.
     */
    public static Coordinate[] getDistanceAngleArray(LineString ring) {
    	int n = ring.getNumPoints();
     	Coordinate[] coords = new Coordinate[n];    	
		for (int i = 0; i<coords.length; i++ ) {
		 	Coordinate pb = ring.getCoordinateN(	//previous Index
		 			(i == 0) ? n-2 : i-1);
		 	Coordinate p = ring.getCoordinateN(i);
			Coordinate pn = ring.getCoordinateN(	//next Index(
					(i == n-1) ? 1 : i+1);
	   	 	double angle = interiorAngle(pb,p,pn);
	   	 	double distance = p.distance(pn);
	   	 	coords[i] = new Coordinate(distance,angle,Double.NaN);
		}
     	return coords;
    }
    
   /**
     * @param ring - a LineString representing a linear ring
     * @return - an array of Coordinate points with colinear points removed.
     * The original LineString and Coordinate points are unmodified.
     */
    public static LinearRing removeRedundantPoints(LineString ring) {
    	final double epsilon = 1E-6; //probably too coarse for lat/long maps
     	Coordinate[] coords = new Coordinate[ring.getNumPoints()];
     	int n = coords.length;
    	boolean[] remove = new boolean[n];
 		for (int i = 0; i<n; i++ ) {
    		coords[i] = new Coordinate(ring.getCoordinateN(i));
			remove[i] = false;
		}
		Coordinate p2 = null;
		Coordinate p3 = null;
		for (int i = 0; i<coords.length; i++ ) {
    	 	Coordinate p1 = coords[i];
    		if (i > 1) {
     		    double dist = getDistance( p2, p1, p3); //distance from p2 to segment p1-p3
	    		boolean colinear = (dist <= epsilon);
	    		if (colinear) {
		    		remove[i-1] = colinear;
		    		n--;	    			
	    		}
    		}
    		p3 = p2;
    		p2 = p1;
 		}
		Coordinate[] newCoords = new Coordinate[n];
		int j=0;
    	for (int i=0; i<coords.length; i++) {
        	if (!remove[i]) 
        		newCoords[j++] = new Coordinate(coords[i]);
    	}
    	LinearRing linearRing = new LinearRing(newCoords,ring.getPrecisionModel(),ring.getSRID());
     	return linearRing;
    }
  
    public static Geometry reducePoints(Geometry geo, double tolerance)
    { //uses Douglas-Peucker algorithm
        CoordinateList coords = new CoordinateList();
        UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
        geo.apply(filter);
        coords.add( filter.getCoordinates() ,false);
        
        //need to do this since UniqueCoordinateArrayFilter keeps the poly from being closed
        if ((geo instanceof Polygon) || (geo instanceof LinearRing))
        {
            coords.add(coords.getCoordinate(0));
        }
        
        int maxIndex = coords.size() - 1;
        int temp = maxIndex;
        do
        {
            temp = maxIndex;
            int i = 0;
            do //generate every possible corridor
            {
                Coordinate anchor = coords.getCoordinate(i);
                boolean pointDeleted = false;
                int k = maxIndex;
                do
                {
                    Coordinate floater = coords.getCoordinate(k);
                    double dmax = -1.0;
                    int j = k;
                    
                    while (j > (i+1))
                    {
                        j--;
                        Coordinate pt = coords.getCoordinate(j);
                        Coordinate cp = getClosestPointOnLine(pt, anchor, floater);
                        double d = pt.distance(cp);
                        
                        if (d > dmax)
                        {
                            dmax = d;
                            k = j;
                        }
                    }
                    
                    if ((dmax < tolerance) && (dmax > -1.0) && (maxIndex > 1))
                    {
                        pointDeleted = true;
                        coords.remove(k); maxIndex--;
                        k = maxIndex;
                    }
                    
                } while (!(pointDeleted || (k <= (i+1)))); //until PointDeleted or (k<=(i+1))
                i++;
            } while (i <= (maxIndex - 2));
        } while (temp != maxIndex);
        
        if (geo instanceof LineString)
        {
            return new GeometryFactory().createLineString(coords.toCoordinateArray());
        }
        else if (geo instanceof LinearRing)
        {
            return new GeometryFactory().createLinearRing(coords.toCoordinateArray());
        }
        else if (geo instanceof Polygon)
        {
            return new GeometryFactory().createPolygon(
            new GeometryFactory().createLinearRing(coords.toCoordinateArray()),
            null);
        }
        else
        {
            return geo;
        }
    }
    
    public static boolean clockwise(Geometry geo)
    {
        if ((geo instanceof Polygon) || (geo instanceof LinearRing))
        {   //calculates the area; neg means clockwise
            //from CRC 25th Edition Page 284
            double t1, t2;
            double geoArea;
            Coordinate[] geoCoords = geo.getCoordinates();
            int maxIndex = geoCoords.length - 1;
            t1 = geoCoords[maxIndex].x * geoCoords[0].y;
            t2 = - geoCoords[0].x * geoCoords[maxIndex].y;
            
            for (int i = 0; i < maxIndex; i++)
            {
                t1 += (geoCoords[i].x   * geoCoords[i+1].y);
                t2 -= (geoCoords[i+1].x * geoCoords[i].y);
            }
            
            geoArea = 0.5 * (t1 + t2);
            return (geoArea < 0);
        }
        else
        {
            return true;
        }
    }  

    public static Coordinate intersect(Coordinate P1, Coordinate P2, Coordinate P3, Coordinate P4) //find intersection of two lines
    {
        Coordinate V = new Coordinate((P2.x - P1.x), (P2.y - P1.y));
        Coordinate W = new Coordinate((P4.x - P3.x), (P4.y - P3.y));
        double n = W.y * (P3.x - P1.x) - W.x * (P3.y - P1.y);
        double d = W.y * V.x - W.x * V.y;
        
        if (d != 0.0)
        {
            double t1 = n / d;
            Coordinate E = new Coordinate((P1.x + V.x * t1),(P1.y + V.y * t1));
            return E;
        }
        else //lines are parallel; no intersection
        {
            return null;
        }
    }
    
    public static Coordinate getIntersection(Coordinate p1, Coordinate p2, Coordinate p3, Coordinate p4)  //find intersection of two lines
    {
        Coordinate e = new Coordinate(0,0,0);
        Coordinate v = new Coordinate(0,0);
        Coordinate w = new Coordinate(0,0);
        
        double t1 = 0;
        double n = 0;
        double d = 0;
        
        v.x = p2.x - p1.x;
        v.y = p2.y - p1.y;
        
        w.x = p4.x - p3.x;
        w.y = p4.y - p3.y;
        
        n = w.y * (p3.x - p1.x) - w.x * (p3.y - p1.y);
        d = w.y * v.x - w.x * v.y;	//determinant of 2x2 matrix with v and w
        
        if (d != 0.0)			//zero only if lines are parallel}
        {
            t1 = n / d;
            e.x = p1.x + v.x * t1;
            e.y = p1.y + v.y * t1;
        }
        else //lines are parallel
        {
            e.z = 999;	//make not equal to zero to show that lines are parallel
        }
        return e;
    }   
    
    public static Coordinate intersectSegments(Coordinate P1, Coordinate P2, Coordinate P3, Coordinate P4) 
    {
    	//find intersection of two line segment that meet the criteria expressed by onP1P2 & onP3P4
        Coordinate V = new Coordinate((P2.x - P1.x), (P2.y - P1.y));
        Coordinate W = new Coordinate((P4.x - P3.x), (P4.y - P3.y));
        double n1 = W.y * (P3.x - P1.x) - W.x * (P3.y - P1.y);
        double n2 = V.y * (P3.x - P1.x) - V.x * (P3.y - P1.y);
        double d = W.y * V.x - W.x * V.y;
        
        if (d != 0.0)
        {
            double t1 = n1 / d;
            double t2 = n2 / d;
            Coordinate E = new Coordinate((P1.x + V.x * t1),(P1.y + V.y * t1));
            double epsilon = 0.001;
            double lowbound = 0.0-epsilon;
            double hibound	 = 1.0+epsilon;
            boolean onP1P2 = (t1 >= lowbound) && (t1 <= hibound);
    		boolean onP3P4 = (t2 >= lowbound) && (t2 <= hibound);
    		if (onP1P2 && onP3P4)
    			return E;
    		else
    			return null; //the intersection point does not lie on one or both segments
        }
        else //lines are parallel; no intersection
        {
            return null;
        }
    }
    
    public static Coordinate getCenter(Coordinate p1, Coordinate p2, Coordinate p3)
    {
        double x = p1.x + ((p2.x - p1.x) / 2.0);
        double y = p1.y + ((p2.y - p1.y) / 2.0);
        Coordinate p12 = new Coordinate(x, y);
        
        if (pointToRight(p3, p1, p2))
        	p1 = rotPt(p1, p12, -90.0);
        else
        	p1 = rotPt(p1, p12, 90.0);
        
        x = p2.x + ((p3.x - p2.x) / 2.0);
        y = p2.y + ((p3.y - p2.y) / 2.0);
        Coordinate p23 = new Coordinate(x, y);
        
        if (pointToRight(p1, p3, p2))
        	p3 = rotPt(p3, p23, -90.0);
        else
        	p3 = rotPt(p3, p23, 90.0);
        
        Coordinate center = intersect(p1, p12, p3, p23);
        
        if (center == null) //no intersection; lines parallel
            return p2;
        else
            return center;
    }
    
    public static BitSet setBit(BitSet bitSet, Geometry geometry)
    {
        BitSet newBitSet = (BitSet) bitSet.clone();
        if      (geometry.isEmpty())                  newBitSet.set(emptyBit);
        else if (geometry instanceof Point)           newBitSet.set(pointBit);
        else if (geometry instanceof MultiPoint)      newBitSet.set(pointBit);
        else if (geometry instanceof LineString)      newBitSet.set(lineBit);
        else if (geometry instanceof LinearRing)      newBitSet.set(lineBit);
        else if (geometry instanceof MultiLineString) newBitSet.set(lineBit);
        else if (geometry instanceof Polygon)         newBitSet.set(polyBit);
        else if (geometry instanceof MultiPolygon)    newBitSet.set(polyBit);
        else if (geometry instanceof GeometryCollection)
        {
            GeometryCollection geometryCollection = (GeometryCollection) geometry;
            for (int i = 0; i < geometryCollection.getNumGeometries(); i++)
                newBitSet = setBit(newBitSet, geometryCollection.getGeometryN(i));
        }
        return newBitSet;
    }
    
    public static LineString MakeRoundCorner(Coordinate A, Coordinate B, Coordinate C, Coordinate D, double r, boolean arcOnly)
    {
        MathVector Gv = new MathVector();
        MathVector Hv;
        MathVector Fv;
        Coordinate E = intersect(A, B, C, D);	//vector solution
        
        if (E != null) //non-parallel lines
        {
            MathVector Ev = new MathVector(E);
            
            if (E.distance(B) > E.distance(A)) //find longest distance from intersection
            {   //these equations assume B and D are closest to the intersection
                //reverse points
                Coordinate temp = A;
                A = B;
                B = temp;
            }
            
            if (E.distance(D) > E.distance(C)) //find longest distance from intersection
            {   //these equations assume B and D are closest to the intersection
                //reverse points
                Coordinate temp = C;
                C = D;
                D = temp;
            }
            
            MathVector Av = new MathVector(A);
            MathVector Cv = new MathVector(C);
            double alpha = Ev.vectorBetween(Av).angleRad(Ev.vectorBetween(Cv)) / 2.0; //we only need the half angle
            double h1 = Math.abs(r / Math.sin(alpha));  //from definition of sine solved for h
            
            if ((h1 * h1 - r * r) >= 0)
            {
                double d1 = Math.sqrt(h1 * h1 - r * r);	//pythagorean theorem}
                double theta = Math.PI / 2.0 - alpha; //sum of triangle interior angles = 180 degrees
                theta = theta * 2.0;		      //we only need the double angle}
                //we now have the angles and distances needed for a vector solution: 
                //we must find the points G and H by vector addition. 
                //Gv = Ev.add(Av.vectorBetween(Ev).unit().scale(d1));
                //Hv = Ev.add(Cv.vectorBetween(Ev).unit().scale(d1));
                //Fv = Ev.add(Gv.vectorBetween(Ev).rotateRad(alpha).unit().scale(h1));
                Gv = Ev.add(Ev.vectorBetween(Av).unit().scale(d1));
                Hv = Ev.add(Ev.vectorBetween(Cv).unit().scale(d1));
                Fv = Ev.add(Ev.vectorBetween(Gv).rotateRad(alpha).unit().scale(h1));
                
                if (Math.abs(Fv.distance(Hv) - Fv.distance(Gv)) > 1.0) //rotated the wrong dirction
                {
                    Fv = Ev.add(Ev.vectorBetween(Gv).rotateRad(-alpha).unit().scale(h1));
                    theta = -theta;
                }
                
                CoordinateList coordinates = new CoordinateList();
                if (!arcOnly) coordinates.add(C);
                Arc arc = new Arc(Fv.getCoord(), Hv.getCoord(), Math.toDegrees(theta));
                LineString lineString = arc.getLineString();
                coordinates.add(lineString.getCoordinates(), false);
                if (!arcOnly) coordinates.add(A);
                return new GeometryFactory().createLineString(coordinates.toCoordinateArray());
            }
        }
       return null;
    }
 
    public static boolean geometriesEqual(Geometry geo1, Geometry geo2)
    {
    	if ((! (geo1 instanceof GeometryCollection)) &&
    		(! (geo2 instanceof GeometryCollection)))
    	    return geo1.equals(geo2);
    	
    	if ((! (geo1 instanceof GeometryCollection)) &&
        	(  (geo2 instanceof GeometryCollection)))
        	return false;
        	
    	if ((  (geo1 instanceof GeometryCollection)) &&
            (! (geo2 instanceof GeometryCollection)))
            	return false;
            	
    	//at this point both are instanceof GeometryCollection
    	int numGeos1 = ((GeometryCollection)geo1).getNumGeometries();
    	int numGeos2 = ((GeometryCollection)geo2).getNumGeometries();
    	if (numGeos1 != numGeos2) return false;
    	
    	for (int index = 0; index < numGeos1; index++)
    	{
    		Geometry internalGeo1 = ((GeometryCollection)geo1).getGeometryN(index);
    		Geometry internalGeo2 = ((GeometryCollection)geo2).getGeometryN(index);
    		if (! geometriesEqual(internalGeo1, internalGeo2))
    			return false;
    	}
    	
    	return true;
    };  
    
    public static double getDistanceFromPointToGeometry(Coordinate coord, Geometry geo)
    {
    	//will return distance to nearest edge of closed polys or GeometryCollections including holes
    	//unlike jts which returns zero for any point inside a poly
    	double closestDist = 999999999;
    	
    	for (int i = 0; i < geo.getNumGeometries(); i++)
    	{
    		double newDist;
    		Geometry internalGeo = geo.getGeometryN(i);
    		
    		if (internalGeo instanceof Point)
    		{
    			newDist = coord.distance(internalGeo.getCoordinate());
    			if (newDist < closestDist) closestDist = newDist;
    		}
    		
    		else if (internalGeo instanceof LineString)
    		{
    			Coordinate[] coords = internalGeo.getCoordinates();
    			for (int j = 0; j < coords.length - 1; j++)
    			{
    				newDist = GeoUtils.getDistance(coord, coords[j], coords[j + 1]);
    				if (newDist < closestDist) closestDist = newDist; 
    			}
    		}
    		
    		else if (internalGeo instanceof Polygon)
    		{
    			Geometry newGeo = internalGeo.getBoundary();
    			newDist = getDistanceFromPointToGeometry(coord, newGeo);
    			if (newDist < closestDist) closestDist = newDist;
    		}
    		
    		else if (internalGeo instanceof MultiPoint)
    		{
    			Coordinate[] coords = internalGeo.getCoordinates();
    			for (int k = 0; k < coords.length; k++)
    			{
    				newDist = coord.distance(coords[k]);
    				if (newDist < closestDist) closestDist = newDist;
    			}
    		}
    		
    		else //remaining geometry types are multi or collections
    		{
		    	for (int m = 0; m < internalGeo.getNumGeometries(); m++)
		    	{
	    			newDist = getDistanceFromPointToGeometry(coord, internalGeo.getGeometryN(m));
	    			if (newDist < closestDist) closestDist = newDist;
		    	}
    		}
    	}
    	
    	return closestDist;
    }
    
    public static boolean geometryIsSegmentOf(Geometry geo1, Geometry geo2)
    {
    	//true if geo1 matches with a segment of geo2
    	
    	if (geo1.getNumPoints() > geo2.getNumPoints())
    		return false;
    	
    	int numGeos1 = geo1.getNumGeometries();
    	int numGeos2 = geo2.getNumGeometries();
    	
    	if ((numGeos1 == 1) && (numGeos2 == 1))
    	{
    		Coordinate[] coords1 = geo1.getCoordinates();
    		Coordinate[] coords2 = geo2.getCoordinates();
    		int i1 = 0;
    		int i2 = 0;
    		
    		while (i2 < coords2.length)
    		{
    			if (coords1[0].equals2D(coords2[i2])) break;
    			i2++;
    		}
    		
    		if (i2 == coords2.length) return false;
    		
    		while ((i1 < coords1.length) && (i2 < coords2.length))
    		{
    			if (! coords1[i1].equals2D(coords2[i2])) return false;
    			i1++;
    			i2++;
    		}
    		
    		return (i1 == coords1.length);
    	}
    	else
    	{
    		boolean foundMatch = false;
    		
	    	for (int i = 0; i < numGeos1; i++)
	    	{
	    		foundMatch = false;
	    		
	    		for (int j = 0; j < numGeos2; j++)
	    		{
	    			if (geometryIsSegmentOf(geo1.getGeometryN(i), geo2.getGeometryN(j)))
	    			{
	    				foundMatch = true;
	    				break;
	    			}
	    		}
	    		
	    		if (! foundMatch) return false;
	    	}
	    	
	    	return foundMatch;
    	}
    };  
    
    /**
     * Generate a plume Polygon using Coordinate[] bounded on each end by circles of radius1 and radius2.
     * @param coords - Coordinate[] sequence of points derived from a LineString.
     * @param radius1 - the radius around the first point in coords.
     * @param radius2 - the radius around the last point in coords.
     * @return Geometry containing Polygon of plume.
     */
    public static Geometry createPlume(Coordinate[] coords, double radius1, double radius2) {
    	int n = coords.length;
    	double radiusInc = (radius2 - radius1) / (n-1);
    	double r1 = radius1;
    	Geometry plume = null;
    	for (int i=0; i<n-1; i++) {
    		Coordinate p0 = coords[i];
    		Coordinate p1 = coords[i+1];
    		double r2 = r1+radiusInc;
    		Polygon buf = taperedBufferSegment(p0, p1, r1, r2);
    		if (plume == null)
    			plume = buf;
    		else 
    			plume = plume.union(buf);				
    		r1 = r2;
    	}
    	return plume;
    }
    
    public static Polygon taperedBufferSegment(Coordinate p0, Coordinate p1, double d1, double d2) {
		ArrayList<Coordinate> coordList = new ArrayList<Coordinate>();
		Coordinate p0Right = perpendicularVector(p0, p1, d1, false);
		Coordinate p0Left = perpendicularVector(p0, p1, d1, true);
		coordList.addAll(new Arc(p0, p0Right, 180.0).getCoordinates());
		coordList.add(p0Left);		
		Coordinate p1Left = perpendicularVector(p1, p0, d2, false);
		coordList.addAll(new Arc(p1, p1Left, 180.0).getCoordinates());		
		Coordinate p1Right = perpendicularVector(p1, p0, d2, true);
		coordList.add(p1Right);
		coordList.add(p0Right);
		Coordinate[] coords = coordList.toArray(new Coordinate[0]);
		LinearRing linearRing = new GeometryFactory().createLinearRing(coords);
		return new GeometryFactory().createPolygon(linearRing, null);
	}

}