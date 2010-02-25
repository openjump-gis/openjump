
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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;
import javax.swing.Timer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;


public abstract class AbstractZoomTool extends DragTool {

    static final double WHEEL_ZOOM_IN_FACTOR = 1.15;
    static final int BOX_TOLERANCE = 4;
    static final double ZOOM_IN_FACTOR = 2;
    
	protected Image origImage;
	protected Image auxImage = null;
	protected double scale = 1d;
	protected int mouseWheelCount = 0;
	protected Point2D.Double zoomTo = new Point2D.Double(0,0);
    private boolean isAnimatingZoom = false;  //deafult to no zoom animation
	private Timer mouseWheelUpdateTimer = null;
    
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
    
    private Point2D cursor = null;
        
	public void mouseWheelMoved(MouseWheelEvent e) {
		int nclicks = e.getWheelRotation();  //negative is up/away
		mouseWheelCount = mouseWheelCount + nclicks;
		if (mouseWheelCount == 0)
			scale = 1d;
		else if (mouseWheelCount < 0)
			scale = Math.abs(mouseWheelCount)*WHEEL_ZOOM_IN_FACTOR;
		else
			scale = 1 / (mouseWheelCount*WHEEL_ZOOM_IN_FACTOR);
        try {
			cursor = e.getPoint();
			if (mouseWheelUpdateTimer == null) {
				RenderingManager renderManager = getPanel().getRenderingManager();
				renderManager.setPaintingEnabled(false);
				mouseWheelUpdateTimer = new Timer(700,
						new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							//zoomAt(getCentre(), scale, false);
							zoomAt(cursor, scale, false);
							mouseWheelUpdateTimer.stop();
							mouseWheelUpdateTimer = null;
							mouseWheelCount = 0;
							origImage = null;
							RenderingManager renderManager = getPanel().getRenderingManager();
							renderManager.setPaintingEnabled(true);
							renderManager.repaintPanel();  		
						} catch (NoninvertibleTransformException e1) {
						} 
					}
				});
				mouseWheelUpdateTimer.start();
				mouseWheelUpdateTimer.setRepeats(false);
				cacheImage();
				//scaleImageAtCentre(scale);
				scaleImageAtPoint(cursor, scale);
			} else {
				mouseWheelUpdateTimer.restart();
				//scaleImageAtCentre(scale);
				scaleImageAtPoint(cursor, scale);
			}
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
	}
    
	protected void scaleImageAtCentre(double zoomFactor) {
	    double w = getPanel().getWidth();
	    double h = getPanel().getHeight();
	    double dx = (w - (w * zoomFactor)) / 2;
	    double dy = (h - (h * zoomFactor)) / 2;
		drawImage((int) dx, (int) dy,  zoomFactor);		
	}
	
	protected void scaleImageAtPoint(Point2D p, double zoomFactor) {
	    double w = getPanel().getWidth();
	    double h = getPanel().getHeight();
	    double dx = (w - (w * zoomFactor)) / 2;
	    double dy = (h - (h * zoomFactor)) / 2;
	    
	    double cx = w/2;
	    double cy = h/2;
	    double x1 = p.getX() - cx;
	    double y1 = p.getY() - cy;	    
	    double x2 = x1 - (x1 * zoomFactor);
	    double y2 = y1 - (y1 * zoomFactor);
	    dx += x2;
	    dy += y2;
		drawImage((int) dx, (int) dy,  zoomFactor);		
	}

	/**
	 * Creates a new Image if currImage doesn't exist
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

	protected Point2D getCentre()
	throws NoninvertibleTransformException { //zoom while keeping cursor over same model point                         
		double x = getPanel().getWidth() / 2;
		double y = getPanel().getHeight() / 2;
		return new Point2D.Double(x,y);
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

	 public void drawImage(int dx, int dy, double scale) {		    
		    double w = origImage.getWidth(getPanel());
		    double h = origImage.getHeight(getPanel());
		    double w2 = w * scale;
		    double h2 = h * scale;
		    auxImage = createImageIfNeeded(auxImage);
		    Graphics2D g = (Graphics2D)auxImage.getGraphics();
		    RenderingHints rh = new RenderingHints(RenderingHints.KEY_INTERPOLATION, 
		    		RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		    g.setRenderingHints(rh);
		    g.setComposite(AlphaComposite.Src);
		    g.setColor(Color.WHITE);
		    g.fillRect(0, 0, auxImage.getWidth(getPanel()), auxImage.getHeight(getPanel()));
		    g.drawImage(origImage,(int) dx,(int) dy,(int) w2,(int) h2, getPanel());
		    getPanel().getGraphics().drawImage(auxImage, 0, 0, getPanel());
		  }
 
}
