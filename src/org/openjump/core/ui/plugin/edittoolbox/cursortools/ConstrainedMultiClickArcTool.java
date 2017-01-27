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

package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import org.openjump.core.geomutils.Arc;
import org.openjump.core.geomutils.GeoUtils;
import org.openjump.core.geomutils.MathVector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;

/**
 *  A VisualIndicatorTool that allows the user to draw shapes with multiple
 *  vertices. Double-clicking ends the gesture.
 */
public abstract class ConstrainedMultiClickArcTool extends ConstrainedMultiClickTool
{
    protected boolean clockwise = true;
    protected double fullAngle = 0.0;
    
    protected Shape getShape() throws NoninvertibleTransformException
    {
        if (coordinates.size() > 1)
        {
            GeneralPath path = new GeneralPath();
            Coordinate firstCoordinate = (Coordinate) coordinates.get(0);
            Point2D firstPoint = getPanel().getViewport().toViewPoint(firstCoordinate);
            path.moveTo((float) firstPoint.getX(), (float) firstPoint.getY());
            
            Coordinate secondCoordinate = (Coordinate) coordinates.get(1);
            Point2D secondPoint = getPanel().getViewport().toViewPoint(secondCoordinate);
            path.lineTo((float) secondPoint.getX(), (float) secondPoint.getY());

            MathVector v1 = (new MathVector(secondCoordinate)).vectorBetween(new MathVector(firstCoordinate));
            MathVector v2 = (new MathVector(tentativeCoordinate)).vectorBetween(new MathVector(firstCoordinate));
            double arcAngle = v1.angleDeg(v2);
            
            boolean toRight = new GeoUtils().pointToRight(tentativeCoordinate, firstCoordinate, secondCoordinate);
            
            boolean cwQuad = ((fullAngle >= 0.0) &&(fullAngle <= 90.0) && clockwise);
            boolean ccwQuad = ((fullAngle < 0.0) &&(fullAngle >= -90.0) && !clockwise);
            if ((arcAngle <= 90.0) && (cwQuad || ccwQuad))
            {
                if (toRight)
                    clockwise = true;
                else
                    clockwise = false;
            }
            
            if ((fullAngle > 90.0) || (fullAngle < -90))
            {
                if ((clockwise && !toRight) || (!clockwise && toRight))
                    fullAngle = 360 - arcAngle;
                else
                   fullAngle = arcAngle; 
            }
            else
            {
                fullAngle = arcAngle;
            }

            if (!clockwise)
                fullAngle = -fullAngle;
            
            Arc arc = new Arc(firstCoordinate, secondCoordinate, fullAngle);
            CoordinateList coords = arc.getCoordinates();

            for (int i = 1; i < coords.size(); i++)
            {
                Point2D nextPoint = getPanel().getViewport().toViewPoint((Coordinate) coords.get(i));
                path.lineTo((int) nextPoint.getX(), (int) nextPoint.getY());
            }
            return path;
        }
        else
        {
            return super.getShape();
        }
    }
 }
