
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.Icon;
import javax.swing.Timer;

import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;


public abstract class AbstractZoomTool extends DragTool {

    static final double WHEEL_ZOOM_IN_FACTOR = 1.25;
    static final int BOX_TOLERANCE = 4;
    static final double ZOOM_IN_FACTOR = 2;
    
	protected Image origImage;
	protected Image auxImage = null;
    private boolean isAnimatingZoom = false;  //deafult to no zoom animation
    
    public boolean setAnimatingZoom(boolean animating) {
    	boolean previousValue = isAnimatingZoom;
    	isAnimatingZoom = animating;
    	return previousValue;
    }
    
    public boolean getAnimatingZoom(){
    	return isAnimatingZoom;
    }

    public Icon getIcon() {                         
        return IconLoader.icon("Magnify.gif");
    }
    public Cursor getCursor() {                             
        return createCursor(IconLoader.icon("MagnifyCursor.gif").getImage());
    }

    protected void gestureFinished() throws NoninvertibleTransformException {                   
    }
        
	public void mouseWheelMoved(MouseWheelEvent e) {
		int nclicks = e.getWheelRotation();  //negative is up/away
        try {
            double zoomFactor = (nclicks > 0)
                ? (1 / (Math.abs(nclicks)*WHEEL_ZOOM_IN_FACTOR)) : 
                	(Math.abs(nclicks)*WHEEL_ZOOM_IN_FACTOR);
            zoomAt(e.getPoint(), zoomFactor, false); 
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
	}
    
	/*
	 * Creates a new BufferedImage if the given image doesn't exist
	 * or is the wrong size for the panel.
	 * @param currImage an image buffer
	 * @return a new image, or the existing one if it's compatible
	 */
	public Image createImageIfNeeded(Image currImage)
	{
		if (currImage == null
				|| currImage.getHeight(null) != getPanel().getHeight()
				|| currImage.getWidth(null) != getPanel().getWidth()) {
			Graphics2D g = (Graphics2D) getPanel().getGraphics();
			Image img = g.getDeviceConfiguration().createCompatibleImage(
					getPanel().getWidth(), getPanel().getHeight(), Transparency.OPAQUE);
			return img;

		}
		//return getPanel().createBlankPanelImage();
		return currImage;
	}

	public void cacheImage() {
		origImage = createImageIfNeeded(origImage);
		getPanel().paint(origImage.getGraphics());		    
	}

        
    protected void zoomAt(Point2D p, double zoomFactor, boolean animatingZoom)
    throws NoninvertibleTransformException { //zoom while keeping cursor over same model point                         
		Viewport vp = getPanel().getViewport();
		Point2D zoomPoint = vp.toModelPoint(p);
		Envelope modelEnvelope = vp.getEnvelopeInModelCoordinates();           
		Coordinate centre = modelEnvelope.centre();
		double width = modelEnvelope.getWidth();
		double height = modelEnvelope.getHeight();
		double dx = (zoomPoint.getX() - centre.x) / zoomFactor;
		double dy = (zoomPoint.getY() - centre.y) / zoomFactor;
		Envelope zoomModelEnvelope = new Envelope(  
				zoomPoint.getX() - (0.5 * (width / zoomFactor)) - dx, 
				zoomPoint.getX() + (0.5 * (width / zoomFactor)) - dx,
				zoomPoint.getY() - (0.5 * (height / zoomFactor)) - dy,
				zoomPoint.getY() + (0.5 * (height / zoomFactor)) - dy);
    	vp.zoom(zoomModelEnvelope);   		
    		//getPanel().getViewport().zoomToViewPoint(p, zoomFactor);
    }

 
}
