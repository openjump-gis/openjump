package com.vividsolutions.jump.workbench.imagery.graphic;

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
 * Fort Walton Beach, Florida 32548
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.RenderingHints;
import java.io.File;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FileLoadDescriptor;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * An image whose source is a bitmap
 * 
 * Much of this code was donated by Larry Becker and Robert Littlefield of Integrated Systems Analysts, Inc. 
 */
public class GraphicImage
    implements ReferencedImage
    
{
  private File file;
  private WorldFile wf;
  private boolean initialload;
  private Envelope env;
  private String type = null;

  public GraphicImage(File file, WorldFile wf)
  {
    this.wf = wf;
    this.file = file;
    this.initialload = true;
    if(wf == null)
    	this.wf = WorldFile.DEFAULT;
    
	int i = file.getName().lastIndexOf(".");
	if(i>-1 && i<file.getName().length()){
		type = file.getName().substring(i+1).toUpperCase();
	}else{
		type = file.getName();
	}
  }
  public Envelope getEnvelope(){
	  if(env == null)
		  env = computeEnvelope();
	  return env;
  }

  private Envelope computeEnvelope()
  {
	  
	  double xm,xM, ym,yM;

	  RenderedOp image = null;
  	  if(file.exists() && file.isFile() && file.canRead()){
  		image = FileLoadDescriptor.create(file.getPath(),null,null,null);
  	  }else{
  		  return new Envelope();
  	  }

	  xm = wf.getXUpperLeft() + wf.getXSize() * (-0.5);
	  xM = wf.getXUpperLeft() + wf.getXSize() * (-0.5 + image.getWidth());

	  ym = wf.getYUpperLeft() + wf.getYSize() * (-0.5);
	  yM = wf.getYUpperLeft() + wf.getYSize() * (-0.5 + image.getHeight());
	  
    return new Envelope(xm, xM, ym, yM );
  }

//  public void paint(Feature f, java.awt.Graphics2D g, Viewport viewport)
//  {
//    // update the control frame, in case it has changed
//
//    AffineTransform originalTransform;
//    originalTransform = g.getTransform();
//    try {
//      transformByBaseline(g, viewport, (new GeometryFactory()).toGeometry(getEnvelope()));
//      //transformGraphics(g, viewport, env);
//      g.drawImage(image, 0, 0, null);
//    }
//    finally {
//      g.setTransform(originalTransform);
//    }
//  }
//
//  private void transformByBaseline(java.awt.Graphics2D g, Viewport viewport, Geometry geom)
//  {
//    Coordinate[] pts = geom.getCoordinates();
//    Coordinate anchorPt = pts[0];
//    Coordinate controlPt = pts[1];
//
//    // scale
//    double frameSize = anchorPt.distance(controlPt);
//    double imageWorldScale = frameSize/image.getWidth(null);
//    double imageViewScale = viewport.getScale() * imageWorldScale;
//    g.scale(imageViewScale, imageViewScale);
//
//    // translate
//    double maxYWorld = imageWorldScale * image.getHeight(null);
//    Coordinate topLeftWorld = new Coordinate(anchorPt.x, anchorPt.y + maxYWorld);
//    Point2D topLeftView = null;
//    try {
//      topLeftView = viewport.toViewPoint(topLeftWorld);
//    } catch(java.awt.geom.NoninvertibleTransformException e) {
//      e.printStackTrace();
//    }
//    g.translate(topLeftView.getX()/imageViewScale, topLeftView.getY()/imageViewScale);
//
//    // rotate
//    double dx = controlPt.x - anchorPt.x;
//    double dy = controlPt.y - anchorPt.y;
//    double theta = Math.atan2(dy, dx);
//    g.rotate(-theta, 0.0, image.getHeight(null));
//
//
//  }

  public void paint(Feature f, java.awt.Graphics2D g, Viewport viewport){
  
	  RenderedOp image = null;
  	  if(file.exists() && file.isFile() && file.canRead()){
  		image = FileLoadDescriptor.create(file.getPath(),null,null,null);
  	  }else{
  		  return;
  	  }
  
      int jpgPixelWidth = image.getWidth();
      int jpgPixelHeight = image.getHeight();
      
	  // Use features internal envelope for upper left coordinates [Ed Deen : Dec 1, 2006]
      // Note: This solves the problem where the user moves the image but only the bounding box moves ... a default worldfile 
      //	   variable was anchoring it.  Now, use the World File (if it exists) only on the initial load of the image.
      // TODO: Add these world file attributes into the actual image feature.  Then copy the world file information into
      //	   the feature at load and extract the following information always from the feature.
      //	   This will allow for sizing the images, sizing to fit, etc, etc. 
      //       and later the ability to write the actual World File out for
      //	   future referencing. [Ed Deen : Dec 1, 2006]
      // Note: Will also need to add the jpg_yres support as well, rotation, etc.
      double jpg_xres = wf.getXSize();  // Default wf.Xsize is always 1.0
	  double jpg_ulx = f.getGeometry().getEnvelopeInternal().getMinX();
	  double jpg_uly = f.getGeometry().getEnvelopeInternal().getMaxY();
      // Check for initial load 
      if (this.initialload == true)
      {
    	  // If Initial Load; check if World File exists 
          if (wf.getFilename() != null)
          {
        	  // If World File exists then use worldfile for initial upper left coordinates
        	  jpg_xres = wf.getXSize();
        	  jpg_ulx = wf.getXUpperLeft() - wf.getXSize()*0.5;
	          jpg_uly = wf.getYUpperLeft() + wf.getYSize()*0.5 - image.getHeight();
          }
          // Set Inital Load to false
          this.initialload = false;
      }
      else 
      {
          jpg_xres = f.getGeometry().getEnvelopeInternal().getWidth() / image.getWidth();
      }

      int image_x = 0; //x position of raster in final image in pixels
      int image_y = 0; //y position of raster in final image in pixels
      int image_w = viewport.getPanel().getWidth(); //width of raster in final image in pixels
      int image_h = viewport.getPanel().getHeight(); //height of raster in final image in pixels
      
      Envelope vpEnvelope = viewport.getEnvelopeInModelCoordinates();
      double view_res = 1 / viewport.getScale(); //panel resolution
      double rwViewLeft = vpEnvelope.getMinX();
      double rwViewRight = vpEnvelope.getMaxX();
      double rwViewTop = vpEnvelope.getMaxY();
      double rwViewBot = vpEnvelope.getMinY();
      
      //Here calculate the real world jpg edges.
      //NOTE: world file coordinates are center of pixels
      double halfPixel = 0.5 * jpg_xres;
      double rwJpgFileLeftEdge = jpg_ulx/* - halfPixel*/;
      double rwJpgFileRightEdge = rwJpgFileLeftEdge + (jpgPixelWidth * jpg_xres);
      double rwJpgFileTopEdge = jpg_uly/* + halfPixel*/;
      double rwJpgFileBotEdge = rwJpgFileTopEdge - (jpgPixelHeight * jpg_xres);
      
      double rwRasterLeft = Math.max(rwViewLeft, rwJpgFileLeftEdge);
      double rwRasterRight = Math.min(rwViewRight, rwJpgFileRightEdge);
      double rwRasterTop = Math.min(rwViewTop, rwJpgFileTopEdge);
      double rwRasterBot = Math.max(rwViewBot, rwJpgFileBotEdge);
      
      //check to see if this jpg is inside the view area
      if (!((rwJpgFileRightEdge <= rwViewLeft) || (rwJpgFileLeftEdge >= rwViewRight) || (rwJpgFileTopEdge <= rwViewBot) || (rwJpgFileBotEdge >= rwViewTop)))
      {
      	//calculate which pixels in the jpg file fit inside the view
          int jpgLeftPixel = (int)((rwRasterLeft - rwJpgFileLeftEdge) / jpg_xres); //trunc
          int jpgRightPixel = (int)((rwRasterRight - rwJpgFileLeftEdge) / jpg_xres); //trunc
          if (jpgRightPixel == jpgPixelWidth) jpgRightPixel = jpgPixelWidth - 1;
          int jpgTopPixel = (int)((rwJpgFileTopEdge - rwRasterTop) / jpg_xres); //trunc
          int jpgBotPixel = (int)((rwJpgFileTopEdge - rwRasterBot) / jpg_xres); //trunc
          if (jpgBotPixel == jpgPixelHeight) jpgBotPixel = jpgPixelHeight - 1;
          
          //calculate the real world coords of the included pixels
          double rwJpgLeft = rwJpgFileLeftEdge + (jpgLeftPixel * jpg_xres);
          double rwJpgRight = rwJpgFileLeftEdge + (jpgRightPixel * jpg_xres) + jpg_xres;
          double rwJpgTop = rwJpgFileTopEdge - (jpgTopPixel * jpg_xres);
          double rwJpgBot = rwJpgFileTopEdge - (jpgBotPixel * jpg_xres) - jpg_xres;
          
          //calculate the pixel offset on the panel of the included portion of the jpg file
          int leftOffset = round((rwRasterLeft - rwJpgLeft) / view_res);
          int rightOffset = round((rwJpgRight - rwRasterRight) / view_res);
          int topOffset = round((rwJpgTop - rwRasterTop) / view_res);
          int botOffset = round((rwRasterBot - rwJpgBot) / view_res);
          
          image_x = round(rwRasterLeft / view_res) - round(rwViewLeft / view_res);
          image_w = round(rwRasterRight / view_res) - round(rwRasterLeft / view_res);
          if (image_w <= 0) image_w = 1;
          
          image_y = round(rwViewTop / view_res) - round(rwRasterTop / view_res);
          image_h = round(rwRasterTop / view_res) - round(rwRasterBot / view_res);
          if (image_h <= 0) image_h = 1;
          
          image_x -= leftOffset;
          image_y -= topOffset;
          image_w += (leftOffset + rightOffset);
          image_h += (topOffset + botOffset);
          
          RenderingHints rh = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          g.setRenderingHints(rh);
          
          //parameters: destination corners then source corners
          //source corners are defined in terms of infinitely thin coordinates
          //which define the edges of the pixel space so that we have
          //to add 1 to the right bottom coordinate of the source rectangle
          //since jpgRightPixel & jpgBotPixel are defined in terms of array element position
          //any questions, see Java documentation for Graphics object
          Composite composite = g.getComposite();
          //[mmichaud 2012-10-10] fix bug 3525977: PNG rasters with void pixels not displayed
          //g.setComposite(AlphaComposite.Src);
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
          g.drawImage(image.getAsBufferedImage(), image_x, image_y, image_x + image_w, image_y + image_h, jpgLeftPixel, jpgTopPixel, jpgRightPixel + 1, jpgBotPixel + 1, viewport.getPanel());
          g.setComposite(composite);
	  }
  }

  private int round(double num)
  {
     return (int)Math.round(num); 
  }
  
	public String getType() {
		return type;
	}

}