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

package org.openjump.core.ui.plugin.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;

import javax.swing.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.ThreadQueue;
/**
 * Zooms the image in the current task window.
 * Uses raster scaling operations.
 *
 * @author Larry Becker
 * @version 1.01
 */
public class ZoomRealtimeTool extends DragTool
{
  private static final double ZOOM_FACTOR = 5d;
  private static final double ZOOM_OUT_LIMIT = 0.1d;
  private boolean dragging = false;
  private Image origImage;
  private Image auxImage = null;
  private double scale = 1d;
  private Point2D.Double zoomTo = new Point2D.Double(0,0);
  private boolean rightMouse = false;
//  private RenderThreadListener renderThreadListener = new RenderThreadListener();
  
  private static final String sName = I18N.get("org.openjump.core.ui.plugin.tools.ZoomRealtimeTool.Zoom-Realtime");

  public ZoomRealtimeTool() {
  }

  public Cursor getCursor() {
    return createCursor(IconLoader.icon("MagnifyCursor2.gif").getImage());
  }

  public Icon getIcon() {
    return IconLoader.icon("Magnify3.gif");
	//return IconLoaderFamFam.icon("map_magnify.png");
  }

  public boolean isRightMouseButtonUsed() {                              
      return true;
  }

  public String getName(){
	  return sName;
  }
  
  public void mouseDragged(MouseEvent e) {
    try {
      rightMouse = SwingUtilities.isRightMouseButton(e);
      if (!dragging) {
        dragging = true;
        getPanel().getRenderingManager().setPaintingEnabled(false);
        cacheImage();
      }

      drawImage(e.getPoint());
      super.mouseDragged(e);
    } catch (Throwable t) {
      getPanel().getContext().handleThrowable(t);
    }
  }

  public void mouseReleased(MouseEvent e) {
    if (!dragging) { 
     	try {
    		getPanel().getViewport().zoomToViewPoint(e.getPoint(), 1.0d);
    	} catch (NoninvertibleTransformException ex){
    		return;
    	}

    }

    getPanel().getRenderingManager().setPaintingEnabled(true);
    dragging = false;
    super.mouseReleased(e);
  }

  protected Shape getShape(Point2D source, Point2D destination) {
    return null;
  }

  protected void gestureFinished() throws NoninvertibleTransformException {
    reportNothingToUndoYet();
	RenderingManager renderManager = getPanel().getRenderingManager();
	renderManager.setPaintingEnabled(false);
// LDB: substitute the following code for the ..Queue().add below if available
//	getPanel().getRenderingManager().setRenderingMode(new Runnable() {
//		public void run() {
//			RenderingManager renderManager = getPanel().getRenderingManager();
//			renderManager.setPaintingEnabled(true);
//			renderManager.repaintPanel();  		
//		}
//	},RenderingManager.INTERACTIVE);

    getPanel().getViewport().zoomToViewPoint(zoomTo,scale);
    
    renderManager.getDefaultRendererThreadQueue().add(
			new Runnable() {
				public void run() {
					RenderingManager renderManager = getPanel().getRenderingManager();
					renderManager.setPaintingEnabled(true);
					renderManager.repaintPanel();  		
				}
			});
   }

  private void cacheImage() {
	origImage = createImageIfNeeded(origImage);
    getPanel().paint(origImage.getGraphics());
    
  }

  private void drawImage(Point p) throws NoninvertibleTransformException {
	double xdrag = p.getX() - getViewSource().getX(); 
	double ydrag = p.getY() - getViewSource().getY(); 
	double scaleFactor;
	if (rightMouse)
		scaleFactor = ZOOM_FACTOR * xdrag; //to reverse do:(getViewSource().getX() - p.getX());
	else 
		scaleFactor = ZOOM_FACTOR * ydrag;
    double w = origImage.getWidth(getPanel());
    double h = origImage.getHeight(getPanel());
    scale =  (h + scaleFactor) / h; //normalize
    scale = (scale < ZOOM_OUT_LIMIT) ? ZOOM_OUT_LIMIT : scale;
    double w2 = w * scale;
    double h2 = h * scale;
    double dx = (w - w2) / 2;
    double dy = (h - h2) / 2;
	double xoff = 0;
	double yoff = 0;
	if (rightMouse)
		yoff = ydrag/scale;
	else
		xoff = xdrag/scale;
	zoomTo.x = dx + w2/2 - xoff;
    zoomTo.y = dy + h2/2 - yoff;
	if (rightMouse) 
		dy += ydrag;
	else
		dx += xdrag;		
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
  
  /*
   * Creates a new BufferedImage if the given image doesn't exist
   * or is the wrong size for the panel.
   * @param currImage an image buffer
   * @return a new image, or the existing one if it's compatible
   */
  private Image createImageIfNeeded(Image currImage)
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
}