
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

package com.vividsolutions.jump.workbench.ui.zoom;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.MathUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jts.geom.Coordinate;


public class ZoomTool extends DragTool {
    /**
     * If the selection box has side length less than this value, the input
     * will be considered to be a click rather than a box.
     */
    private static final int BOX_TOLERANCE = 4;
    private static final double ZOOM_IN_FACTOR = 2;
    private static final double WHEEL_ZOOM_IN_FACTOR = 1.25;

    public ZoomTool() {                        
        setColor(Color.black);
    }

    public Icon getIcon() {                         
        return IconLoader.icon("Magnify.gif");
    }

    public String getName() {                            
        return I18N.get("ui.zoom.ZoomTool.zoom-in-out");
    }

    public Cursor getCursor() {                             
        return createCursor(IconLoader.icon("MagnifyCursor.gif").getImage());
    }

    protected void gestureFinished() throws NoninvertibleTransformException {                   
        reportNothingToUndoYet();

        double minX = Math.min(getViewSource().getX(), getViewDestination().getX());
        double maxX = Math.max(getViewSource().getX(), getViewDestination().getX());
        double minY = Math.min(getViewSource().getY(), getViewDestination().getY());
        double maxY = Math.max(getViewSource().getY(), getViewDestination().getY());

        //<<TODO:REFACTORING>> MD feels we should move this logic into
        //Viewport, in case other classes want to use it [Jon Aquino]
        double widthOfNewViewAsPerceivedByOldView = maxX - minX;
        double heightOfNewViewAsPerceivedByOldView = maxY - minY;

        if ((widthOfNewViewAsPerceivedByOldView == 0) &&
                (heightOfNewViewAsPerceivedByOldView == 0)) {
            //Handled by #mouseClicked
            return;
        }

        if ((widthOfNewViewAsPerceivedByOldView < BOX_TOLERANCE) &&
                (heightOfNewViewAsPerceivedByOldView < BOX_TOLERANCE)) {
            zoomAt(new Point2D.Double(MathUtil.avg(minX, maxX),
                    MathUtil.avg(minY, maxY)), ZOOM_IN_FACTOR);

            return;
        }

        Point2D centreOfNewViewAsPerceivedByOldView = new Point2D.Double(minX +
                (widthOfNewViewAsPerceivedByOldView / 2),
                minY + (heightOfNewViewAsPerceivedByOldView / 2));
        getPanel().getViewport().zoom(centreOfNewViewAsPerceivedByOldView,
            widthOfNewViewAsPerceivedByOldView,
            heightOfNewViewAsPerceivedByOldView);

        //<<TODO:ERGONOMICS>> If user draws a tiny square (e.g. 3 pixels), treat it as
        //a click. [Jon Aquino]
    }

    public void mouseClicked(MouseEvent e) {                      
        try {
            double zoomFactor = SwingUtilities.isRightMouseButton(e)
                ? (1 / ZOOM_IN_FACTOR) : ZOOM_IN_FACTOR;
            zoomAt(e.getPoint(), zoomFactor);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    public void mousePressed(MouseEvent e) {                   
        try {
            if (SwingUtilities.isLeftMouseButton(e)) {
                super.mousePressed(e);
            }
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    private void zoomAt(Point2D p, double zoomFactor)
        throws NoninvertibleTransformException {                                
        getPanel().getViewport().zoomToViewPoint(p, zoomFactor);
    }

    public boolean isRightMouseButtonUsed() {                              
        return true;
    }
    
    
    public void mouseDragged(MouseEvent e) {                      
        super.mouseDragged(e);
    }

    public void mouseReleased(MouseEvent e) {                             
        super.mouseReleased(e);
    }
    
	public void mouseWheelMoved(MouseWheelEvent e) {
		int nclicks = e.getWheelRotation();  //negative is up/away
        try {
            double zoomFactor = (nclicks > 0)
                ? (1 / (Math.abs(nclicks)*WHEEL_ZOOM_IN_FACTOR)) : 
                	(Math.abs(nclicks)*WHEEL_ZOOM_IN_FACTOR);
            //zoomAt(e.getPoint(), zoomFactor);  //zoom to cursor 
            zoomAt(getPanel().getViewport().toViewPoint(  //zoom to center
            		getPanel().getViewport().
            		getEnvelopeInModelCoordinates().centre()), zoomFactor);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
	}

    public void activate(LayerViewPanel layerViewPanel) {                        
        super.activate(layerViewPanel);
    }

    public void deactivate() {                       
        super.deactivate();
    }

    public void mouseEntered(MouseEvent e) {                         
        super.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e) {                         
        super.mouseExited(e);
    }

    public void mouseMoved(MouseEvent e) {                            
        super.mouseMoved(e);
    }

}
