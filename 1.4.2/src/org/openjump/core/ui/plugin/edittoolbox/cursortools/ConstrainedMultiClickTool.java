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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintManager;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;

/**
 *  A VisualIndicatorTool that allows the user to draw shapes with multiple
 *  vertices. Double-clicking ends the gesture.
 */
public abstract class ConstrainedMultiClickTool extends AbstractCursorTool
{
    
    //24.iii.03 Dropped drag handling because it's too easy to do a micro-drag when
    //we mean a click. [Jon Aquino]
	final static String lengthST =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.length");
	final static String angleST =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.angle");
	final static String degrees =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.degrees");
	
    protected List coordinates = new ArrayList();
    protected Coordinate tentativeCoordinate;
    protected boolean drawClosed = true;
    private ConstraintManager constraintManager;
//    private LayerViewPanel panel;
    private WorkbenchFrame frame;
        
    public ConstrainedMultiClickTool()
    {
    }
        
    public boolean isRightMouseButtonUsed() //we want the right click to close the poly
    {
        return true;
    }
    
    /**
     * Will return an empty List once the shape is cleared.
     * @see MultiClickTool#clearShape
     */
    public List getCoordinates()
    {
        return Collections.unmodifiableList(coordinates);
    }
    
    public void cancelGesture()
    {
        //It's important to clear the data when #cancelGesture is called.
        //Otherwise, you get behaviour like the following:
        //  --  Combine a DragTool with a MultiClickTool using OrCompositeTool
        //  --  Drag a box. A box appears. Release the mouse.
        //  --  Move the mouse. You see a rubber band from MultiClickTool because
        //      the points haven't been cleared. [Jon Aquino]
//        java.awt.Toolkit.getDefaultToolkit().beep();
//        if (!altKeyDown)
//        {
            super.cancelGesture();
            coordinates.clear();
//        }
    }
    
    public void mouseReleased(MouseEvent e)
    {
        try
        {
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
            if (e.getClickCount() == 1)
            {
                //A double-click will generate two events: one with click-count = 1 and
                //another with click-count = 2. Handle the click-count = 1 event and
                //ignore the rest. Otherwise, the following problem can occur:
                //  --  A click-count = 1 event is generated; #redrawShape is called
                //  --  #isFinishingClick returns true; #finishGesture is called
                //  --  #finishGesture clears the points
                //  --  A click-count = 2 event is generated; #redrawShape is called.
                //      An IndexOutOfBoundsException is thrown because points is empty.
                //[Jon Aquino]
                tentativeCoordinate = doConstraint(e);
                redrawShape();
            }
            
            super.mouseReleased(e);
            
            //Check for finish at #mouseReleased rather than #mouseClicked.
            //#mouseReleased is a more general condition, as it applies to both
            //drags and clicks. [Jon Aquino]
            if (isFinishingRelease(e))
            {
                finishGesture();
            }
        } catch (Throwable t)
        {
            getPanel().getContext().handleThrowable(t);
        }
    }
    
    protected Coordinate doConstraint(MouseEvent e) throws NoninvertibleTransformException
    {
        Coordinate retPt = snap(e.getPoint());
        retPt = constraintManager.constrain(getPanel(), getCoordinates(), retPt, e);
        return retPt;
    }
    
    protected void mouseLocationChanged(MouseEvent e)
    {
//        if (!altKeyDown) //do this so that we don't get feed back on shape when alt down
//        {
        try
        {
            if (coordinates.isEmpty()) return;
            
            tentativeCoordinate = doConstraint(e);
            Coordinate startPt = (Coordinate)coordinates.get(coordinates.size() - 1);
            double length = startPt.distance(tentativeCoordinate);
            double angle = constraintManager.getBearing(startPt, tentativeCoordinate);
            DecimalFormat df2 = new DecimalFormat("##0.0#");
            DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
            //getPanel().getContext().setStatusMessage("length = " + df3.format(length) + ";  angle = " + df2.format(angle) + " degrees");
            getPanel().getContext().setStatusMessage(lengthST + ": " + df3.format(length) + ";  " + angleST + ": " + df2.format(angle) + " " + degrees);
//            double length = Math.round(startPt.distance(tentativeCoordinate) * 1000.0) / 1000.0;
//            double angle = Math.round(constraintManager.getBearing(startPt, tentativeCoordinate) * 100.0) / 100.0;
//            getPanel().getContext().setStatusMessage(" length = " + length + ";  angle = " + angle + " degrees");
            redrawShape();
        } catch (Throwable t)
        {
            getPanel().getContext().handleThrowable(t);
        }
//        }
    }
    
    public void mouseMoved(MouseEvent e)
    {
        mouseLocationChanged(e);
    }
    
    public void mouseDragged(MouseEvent e)
    {
        mouseLocationChanged(e);
    }

    protected void add(Coordinate c)
    {
        coordinates.add(c);
    }
    
    public void mousePressed(MouseEvent e)
    {
        try
        {   
            super.mousePressed(e);
            Assert.isTrue(e.getClickCount() > 0);
            
            //Don't add more than one point for double-clicks. A double-click will
            //generate two events: one with click-count = 1 and another with
            //click-count = 2. Handle the click-count = 1 event and ignore the rest.
            //[Jon Aquino]
            if (e.getClickCount() != 1)
            {
                return;
            }
            
            add(doConstraint(e));

        } catch (Throwable t)
        {
            getPanel().getContext().handleThrowable(t);
        }
    }
    
    protected Shape getShape() throws NoninvertibleTransformException
    {
        Point2D firstPoint = getPanel().getViewport().toViewPoint((Coordinate)coordinates.get(0));
        GeneralPath path = new GeneralPath();
        path.moveTo((float) firstPoint.getX(), (float) firstPoint.getY());
        
        for (int i = 1; i < coordinates.size(); i++)
        { //start 1 [Jon Aquino]
            
            Coordinate nextCoordinate = (Coordinate) coordinates.get(i);
            Point2D nextPoint = getPanel().getViewport().toViewPoint(nextCoordinate);
            path.lineTo((int) nextPoint.getX(), (int) nextPoint.getY());
        }
        Point2D tentativePoint = getPanel().getViewport().toViewPoint(tentativeCoordinate);
        path.lineTo((int) tentativePoint.getX(), (int) tentativePoint.getY());
        if (drawClosed)
            path.lineTo((float) firstPoint.getX(), (float) firstPoint.getY());
        
        return path;
    }
    
    protected boolean isFinishingRelease(MouseEvent e)
    {
        return ((e.getClickCount() == 2) || (e.getButton() == MouseEvent.BUTTON3));
    }
    
    protected Coordinate[] toArray(List coordinates)
    {
        return (Coordinate[]) coordinates.toArray(new Coordinate[]
        {  });
    }
    
    protected void finishGesture() throws Exception
    {
        clearShape();
        
        try
        {
            fireGestureFinished();
        } finally
        {
            //If exception occurs, cancel. [Jon Aquino]
            coordinates.clear();
        }
    }
    
    public void deactivate()
    {
    	super.deactivate();
        if (frame != null) 
            frame.removeEasyKeyListener(keyListener);    	
    }
    protected Coordinate getIntersection(Coordinate p1, Coordinate p2, Coordinate p3, Coordinate p4)  //find intersection of two lines
    {
        Coordinate e = new Coordinate(0,0);
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
    
    public void activate(LayerViewPanel layerViewPanel)
    {
        super.activate(layerViewPanel);
        constraintManager = new ConstraintManager(getWorkbench().getContext());
        
        //following added to handle Backspace key deletes last vertex
        panel = layerViewPanel;
        frame = AbstractCursorTool.workbenchFrame(panel);
        
        if (frame != null) 
            frame.addEasyKeyListener(keyListener);
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
    		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
    			try {
    				finishGesture();
    			}catch (Exception ex) {
    				getPanel().getContext().handleThrowable(ex);
    			};
    	}
    };
}
