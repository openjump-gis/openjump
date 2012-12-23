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

package com.vividsolutions.jump.workbench.ui.snap;

import java.awt.Color;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collection;

import javax.swing.Icon;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;


/**
 *  Visually indicates the snap point with a coloured dot.
 */
public class SnapIndicatorTool extends AbstractCursorTool {
    private Point2D indicatorLocation;
    private Color snappedColor;
    private Color unsnappedColor;
    private double diameter;

    public SnapIndicatorTool(Collection snapPolicies) {
        this(Color.green, Color.red, 8, snapPolicies);
    }

    public SnapIndicatorTool(Color snappedColor, Color unsnappedColor,
        double diameter, Collection snapPolicies) {
      getSnapManager().addPolicies(snapPolicies);
      setFilling(true);
      this.snappedColor = snappedColor;
      this.unsnappedColor = unsnappedColor;
      this.diameter = diameter;
    }

    public Icon getIcon() {
        return null;
    }

    protected void gestureFinished() throws Exception {
        Assert.shouldNeverReachHere();
    }

    public void mouseDragged(MouseEvent e) {
        mouseLocationChanged(e);
    }

    public void mouseMoved(MouseEvent e) {
        mouseLocationChanged(e);
    }

    protected Shape getShape() throws NoninvertibleTransformException {
        return new Ellipse2D.Double(indicatorLocation.getX() - (diameter / 2),
            indicatorLocation.getY() - (diameter / 2), diameter, diameter);
    }

    private void mouseLocationChanged(MouseEvent e) {
        try {
            clearShape();
            indicatorLocation = getPanel().getViewport().toViewPoint(snap(e.getPoint()));
            setColor(getSnapManager().wasSnapCoordinateFound() ? snappedColor : unsnappedColor);
            redrawShape();
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    public boolean isGestureInProgress() {
        //Override the default implementation because, yes, the shape is on screen,
        //but the user is not making a gesture. [Jon Aquino]
        return false;
    }
}
