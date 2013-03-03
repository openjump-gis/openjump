
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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;


/**
 *  The default implementation draws a selection box, but this can be overridden
 *  (even to draw nothing).
 */
public abstract class DragTool extends AbstractCursorTool {
    public static final int DEFAULT_VIEW_CLICK_BUFFER = 2;
    private int viewClickBuffer = DEFAULT_VIEW_CLICK_BUFFER;
    /** Modify using #setSource */
    protected Coordinate modelSource = null;
    /** Modify using #setDestination */
    protected Coordinate modelDestination = null;
    private boolean dragApproved = false;

    
    public void deactivate() {
      cancelGesture();
      super.deactivate();
    }

    /**
     * Begins handling of the drag. Subclasses can prevent handling of the drag
     * by overriding this method and not calling it.
     */
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        dragApproved = true;
        try {
            setViewSource(e.getPoint());
        } catch (NoninvertibleTransformException x) {
            getPanel().getContext().handleThrowable(x);
        }
        //Probably doesn't make sense to snap the source. Note that MoveSelectedItem's
        //override of #snap assumes that it is only used on the destination. [Jon Aquino]
    }
    
    /**
     * A click is converted into a box by being expanded by this amount in the
     * four directions.
     */
    protected void setViewClickBuffer(int clickBuffer) {
        this.viewClickBuffer = clickBuffer;
    }

    protected boolean wasClick() {
        return getModelSource().equals(getModelDestination());
    }

    protected Envelope getBoxInModelCoordinates()
        throws NoninvertibleTransformException {
        double minX = Math.min(getModelSource().x, getModelDestination().x);
        double maxX = Math.max(getModelSource().x, getModelDestination().x);
        double minY = Math.min(getModelSource().y, getModelDestination().y);
        double maxY = Math.max(getModelSource().y, getModelDestination().y);

        if (wasClick()) {
            minX -= modelClickBuffer();
            maxX += modelClickBuffer();
            minY -= modelClickBuffer();
            maxY += modelClickBuffer();
        }

        return new Envelope(minX, maxX, minY, maxY);
    }

    protected double modelClickBuffer() {
        return viewClickBuffer / getPanel().getViewport().getScale();
    }


    public void mouseDragged(MouseEvent e) {
        try {
            if (!dragApproved) {
                //dragApproved will be false if:
                //  --  the drag began outside the panel
                //  --  a subclass wanted to prevent handling of the drag by overriding
                //      #mousePressed and not calling it; for example, EditDelineationTool.
                //[Jon Aquino]
                return;
            }

            setViewDestination(e.getPoint());
            redrawShape();
            super.mouseDragged(e);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    protected Coordinate getModelSource() {
        return modelSource;
    }

    protected Coordinate getModelDestination() {
        return modelDestination;
    }        

    protected void setModelSource(Coordinate source) {
        this.modelSource = source;
    }
    
    protected void setViewSource(Point2D source) throws NoninvertibleTransformException {
        setModelSource(getPanel().getViewport().toModelCoordinate(source));
    }
    
    protected void setViewDestination(Point2D destination) throws NoninvertibleTransformException {
        setModelDestination(getPanel().getViewport().toModelCoordinate(destination));
    }    

    protected void setModelDestination(Coordinate destination) {
        this.modelDestination = snap(destination);
    }

    public void mouseReleased(MouseEvent e) {
        try {
            super.mouseReleased(e);
            boolean dragComplete = isShapeOnScreen();
            clearShape();

            if (dragComplete) {
                fireGestureFinished();
            }

            dragApproved = false;
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    protected Shape getShape() throws Exception {
        return getShape(getViewSource(), getViewDestination());
    }
    
    protected Point2D getViewSource() throws NoninvertibleTransformException {
        return getPanel().getViewport().toViewPoint(getModelSource());
    }
    
    protected Point2D getViewDestination() throws NoninvertibleTransformException {
        return getPanel().getViewport().toViewPoint(getModelDestination());
    }    

    /**
     *@return    null if nothing should be drawn
     */
    protected Shape getShape(Point2D source, Point2D destination)
        throws Exception {
        double minX = Math.min(source.getX(), destination.getX());
        double minY = Math.min(source.getY(), destination.getY());
        double maxX = Math.max(source.getX(), destination.getX());
        double maxY = Math.max(source.getY(), destination.getY());

        return new Rectangle.Double(minX, minY, maxX - minX, maxY - minY);
    }
    
}
