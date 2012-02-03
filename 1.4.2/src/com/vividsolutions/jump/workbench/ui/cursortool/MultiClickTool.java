
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

import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;


/**
 *  A VisualIndicatorTool that allows the user to draw shapes with multiple
 *  vertices. Double-clicking ends the gesture.
 */
public abstract class MultiClickTool extends AbstractCursorTool {

//24.iii.03 Dropped drag handling because it's too easy to do a micro-drag when
//we mean a click. [Jon Aquino]

    private List coordinates = new ArrayList();
    private Coordinate tentativeCoordinate;
    // set this to true if rubber band should be closed
    private boolean closeRing = false;
    private CoordinateListMetrics metrics = null;
    private LayerViewPanel panel;
    private WorkbenchFrame frame;
    private boolean activated = false; //LDB: prevent multiple activate

    public MultiClickTool() {
    }

    protected void setMetricsDisplay(CoordinateListMetrics metrics)
    {
      this.metrics = metrics;
    }

    protected CoordinateListMetrics getMetrics() { return metrics; }

    protected void setCloseRing(boolean closeRing)
    {
      this.closeRing = closeRing;
    }

    /**
     * Will return an empty List once the shape is cleared.
     * @see MultiClickTool#clearShape
     */
    public List getCoordinates() {
        return Collections.unmodifiableList(coordinates);
    }

    public void cancelGesture() {
        //It's important to clear the data when #cancelGesture is called.
        //Otherwise, you get behaviour like the following:
        //  --  Combine a DragTool with a MultiClickTool using OrCompositeTool
        //  --  Drag a box. A box appears. Release the mouse.
        //  --  Move the mouse. You see a rubber band from MultiClickTool because
        //      the points haven't been cleared. [Jon Aquino]
        super.cancelGesture();
        coordinates.clear();
    }

    public void mouseReleased(MouseEvent e) {
        try {
            //Can't assert that coordinates is not empty at this point because
            //of the following situation: NClickTool, n=1, user double-clicks.
            //Two events are generated: clickCount=1 and clickCount=2.
            //When #mouseReleased is called with the clickCount=1 event,
            //coordinates is not empty. But then #finishGesture is called and the
            //coordinates are cleared. When #mouseReleased is then called with
            //the clickCount=2 event, coordinates is empty! [Jon Aquino]

            //Even though drawing is done in #mouseLocationChanged, call it here
            //also so that #isGestureInProgress returns true on a mouse click.
            //This is mainly for the benefit of OrCompositeTool, which
            //calls #isGestureInProgress. [Jon Aquino]
            //Can't do this in #mouseClicked because #finishGesture may be called
            //by #mouseReleased (below), which happens before #mouseClicked,
            //resulting in an IndexOutOfBoundsException in #redrawShape. [Jon Aquino]
            if (e.getClickCount() == 1) {
                //A double-click will generate two events: one with click-count = 1 and
                //another with click-count = 2. Handle the click-count = 1 event and
                //ignore the rest. Otherwise, the following problem can occur:
                //  --  A click-count = 1 event is generated; #redrawShape is called
                //  --  #isFinishingClick returns true; #finishGesture is called
                //  --  #finishGesture clears the points
                //  --  A click-count = 2 event is generated; #redrawShape is called.
                //      An IndexOutOfBoundsException is thrown because points is empty.
                //[Jon Aquino]
                tentativeCoordinate = snap(e.getPoint());
                redrawShape();
            }

            super.mouseReleased(e);

            //Check for finish at #mouseReleased rather than #mouseClicked.
            //#mouseReleased is a more general condition, as it applies to both
            //drags and clicks. [Jon Aquino]
            if (isFinishingRelease(e)) {
                finishGesture();
            }
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    protected void mouseLocationChanged(MouseEvent e) {
        try {
            if (coordinates.isEmpty()) {
                return;
            }

            tentativeCoordinate = snap(e.getPoint());
            redrawShape();
            displayMetrics(e);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    private void displayMetrics(MouseEvent e)
        throws NoninvertibleTransformException
    {
      if (metrics == null) return;
      if (isShapeOnScreen()) {
        ArrayList currentCoordinates = new ArrayList(getCoordinates());
        currentCoordinates.add(snap(getPanel().getViewport().toModelCoordinate(e.getPoint())));
        metrics.displayMetrics(currentCoordinates, getPanel());
      }
    }

    public void mouseMoved(MouseEvent e) {
        mouseLocationChanged(e);
    }

    public void mouseDragged(MouseEvent e) {
        mouseLocationChanged(e);
    }

    protected void add(Coordinate c) {
        coordinates.add(c);
    }

    public void mousePressed(MouseEvent e) {
        try {
            super.mousePressed(e);
            Assert.isTrue(e.getClickCount() > 0);

            //Don't add more than one point for double-clicks. A double-click will
            //generate two events: one with click-count = 1 and another with
            //click-count = 2. Handle the click-count = 1 event and ignore the rest.
            //[Jon Aquino]
            if (e.getClickCount() != 1) {
                return;
            }

            add(snap(e.getPoint()));
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    protected Shape getShape() throws NoninvertibleTransformException {
        GeneralPath path = new GeneralPath();
		// sometimes the coordinates are empty and we get an IndexOutOfBoundsExeption!
		// we get this if we use this tool, open a menu and then click with the
		// open menu in the map. In this moment we do not get the mousePressed
		// event and no coordinate will be added.
		if (!coordinates.isEmpty()) {
			Point2D firstPoint = getPanel().getViewport().toViewPoint((Coordinate)coordinates.get(0));
			path.moveTo((float) firstPoint.getX(), (float) firstPoint.getY());

			for (int i = 1; i < coordinates.size(); i++) { //start 1 [Jon Aquino]

				Coordinate nextCoordinate = (Coordinate) coordinates.get(i);
				Point2D nextPoint = getPanel().getViewport().toViewPoint(nextCoordinate);
				path.lineTo((int) nextPoint.getX(), (int) nextPoint.getY());
			}
			Point2D tentativePoint = getPanel().getViewport().toViewPoint(tentativeCoordinate);
			path.lineTo((int) tentativePoint.getX(), (int) tentativePoint.getY());
			// close path (for rings only)
			if (closeRing)
			  path.lineTo((int) firstPoint.getX(), (int) firstPoint.getY());

			}
        return path;
    }

    protected boolean isFinishingRelease(MouseEvent e) {
        return e.getClickCount() == 2;
    }

    protected Coordinate[] toArray(List coordinates) {
        return (Coordinate[]) coordinates.toArray(new Coordinate[] {  });
    }

    protected void finishGesture() throws Exception {
        clearShape();

        try {
            fireGestureFinished();
        } finally {
            //If exception occurs, cancel. [Jon Aquino]
            coordinates.clear();
        }
    }
    
    //-- [sstein: 24Mar2007] added for to allow to cancel last vertex per backspace
    public void deactivate()
    {
    	super.deactivate();
        if (frame != null) {
            frame.removeEasyKeyListener(keyListener); 
            activated = false;
        }
    }
    
    //-- [sstein: 24Mar2007] added for to allow to cancel last vertex per backspace
    public void activate(LayerViewPanel layerViewPanel)
    {
        super.activate(layerViewPanel);
        
        //following added to handle Backspace key deletes last vertex
        panel = layerViewPanel;
        frame = AbstractCursorTool.workbenchFrame(panel);
        
        if ((frame != null) & (!activated)) {  //LDB: prevent multiple activate
            frame.addEasyKeyListener(keyListener);
            activated = true;
        }
     }
    
    private KeyListener keyListener = new KeyListener() 
    {
        public void keyTyped(KeyEvent e) 
        {
        }

        public void keyPressed(KeyEvent e) 
        {
        }

        public void keyReleased(KeyEvent e) 
        {
        	if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
        	{
        		if (coordinates.size() > 1)
        			coordinates.remove(coordinates.size() - 1);
        		panel.repaint();
        	}
        }
    };
}
