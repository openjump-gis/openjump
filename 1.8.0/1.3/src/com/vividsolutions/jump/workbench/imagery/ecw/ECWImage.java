package com.vividsolutions.jump.workbench.imagery.ecw;

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
 
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import com.ermapper.ecw.JNCSException;
import com.ermapper.ecw.JNCSFileOpenFailedException;
import com.ermapper.ecw.JNCSInvalidSetViewException;
import com.ermapper.ecw.JNCSRenderer;
import com.ermapper.util.JNCSDatasetPoint; // added
import com.ermapper.util.JNCSWorldPoint; // added
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * A {@link ReferencedImage} for ECW files
 */
public class ECWImage
    implements ReferencedImage
{

  private Envelope imageEnv = new Envelope();
  private JNCSRenderer ecwRenderer;
  private int[] bandlist;
  private boolean validSetView = false;
  private Envelope lastViewportEnv = new Envelope();

  // debugging only
  int count = 0;

  public ECWImage(String location) throws JUMPException {
    init(location);
  }

  private void init(String location) throws JUMPException
  {
    try {
      ecwRenderer = new JNCSRenderer(location, false);
      double xm = ecwRenderer.originX;
      double yM = ecwRenderer.originY;
      double xM = ecwRenderer.originX + (double)(ecwRenderer.width)*ecwRenderer.cellIncrementX;
      double ym = ecwRenderer.originY + (double)(ecwRenderer.height)*ecwRenderer.cellIncrementY;
      // image enveloppe
      imageEnv = new Envelope(xm, xM, ym, yM);
      
      // use all bands
      bandlist = new int[ecwRenderer.numBands];
      for (int i=0; i< ecwRenderer.numBands; i++) {bandlist[i] = i;}
    }
    catch (JNCSFileOpenFailedException e){
    	throw new JUMPException(e.getMessage());
    }
    catch (JNCSException e){
    	throw new JUMPException(e.getMessage());
    }
  }

  public Envelope getEnvelope() { return imageEnv; }

  public void paint(Feature f, java.awt.Graphics2D g, Viewport viewport) throws JUMPException
  {
    Envelope viewportEnv = viewport.getEnvelopeInModelCoordinates();
    if(! imageEnv.intersects(viewportEnv)) {
      return;
    }

    // only set view if viewport has changed
    if(! viewportEnv.equals(lastViewportEnv)) {
      validSetView = false;
      lastViewportEnv = viewportEnv;
    }
    
    try{
      // width and height of the viewport
      int width = viewport.getPanel().getWidth();
      int height = viewport.getPanel().getHeight();
      // viewport in model coordinates
      double dWorldTLX = viewportEnv.getMinX();
      double dWorldTLY = viewportEnv.getMaxY();
      double dWorldBRX = viewportEnv.getMaxX();
      double dWorldBRY = viewportEnv.getMinY();
      
      // only set view if viewport has changed
      if(! validSetView) {
        // Compute the rectangle including all the pixels to display
        
        // Compute topleft corner
        // As convertWorldToDataset returns 0 from dWorldTLX-incr/2 to
        // dWorldTLX+incr/2, we have to translate the topleft corner to make
        // sure the topleft pixel will be displayed
        JNCSDatasetPoint firstCell = ecwRenderer.convertWorldToDataset(dWorldTLX-ecwRenderer.cellIncrementX/2.0, dWorldTLY-ecwRenderer.cellIncrementY/2.0);
        // If the top left corner of the viewport is negative (image corner is
        // inside the viewport), display the image from column 0
        int firstColumn = Math.max(0, firstCell.x);
        int firstLine = Math.max(0, firstCell.y);
        // tlCorner is the world coordinate of the topleft corner of the topleft
        // pixel to be drawn
        JNCSWorldPoint tlCorner = ecwRenderer.convertDatasetToWorld(firstColumn, firstLine);
        
        // Compute bottomRight corner
        // As convertWorldToDataset returns 0 from dWorldTLX-incr/2 to
        // dWorldTLX+incr/2, we have to translate the topleft corner to make
        // sure the bottomRight pixel will be displayed
        JNCSDatasetPoint lastCell = ecwRenderer.convertWorldToDataset(dWorldBRX-ecwRenderer.cellIncrementX/2.0, dWorldBRY-ecwRenderer.cellIncrementY/2.0);
        // If the image bottomRight corner is inside the viewport,
        // display the image up to lastCell + 1
        int lastColumn = (int)Math.min(ecwRenderer.width-1, lastCell.x+1);
        int lastLine = (int)Math.min(ecwRenderer.height-1, lastCell.y+1);
        // brCorner is the world coordinate of the bottomRight corner of the
        // bottomRight pixel to be drawn
        JNCSWorldPoint brCorner = ecwRenderer.convertDatasetToWorld(lastColumn+1, lastLine+1);
        
        Envelope finalEnvelope = new Envelope(tlCorner.x, brCorner.x, brCorner.y, tlCorner.y);

        int nbColumns = (lastColumn-firstColumn)+1;
        int nbLines = (lastLine-firstLine)+1;
        width = width<=nbColumns?width:nbColumns;
        height = height<=nbLines?height:nbLines;
        
        BufferedImage ecwImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pRGBArray = new int[width];
        try {
            ecwRenderer.setView(ecwRenderer.numBands, bandlist, firstColumn, firstLine, lastColumn, lastLine, width, height);
        } catch(Exception e) {e.printStackTrace();}
        for (int line=0; line < height; line++) {
            ecwRenderer.readLineRGBA(pRGBArray);
            ecwImage.setRGB(0, line, width, 1, pRGBArray, 0, width);
        }
        Rectangle2D finalRect = viewport.toViewRectangle(finalEnvelope);
        
        // debugging only
        //System.out.println("Image size     : " + ecwRenderer.width + " x " + ecwRenderer.height);
        //System.out.println("Pixel size     : " + ecwRenderer.cellIncrementX + " x " + ecwRenderer.cellIncrementY);
        //System.out.println("Image envelope : " + imageEnv);
        //System.out.println("First pixel to display : " + firstColumn + "," + firstLine);
        //System.out.println("Last  pixel to display : " + lastColumn + "," + lastLine);
        
        g.drawImage(ecwImage,
                     (int)finalRect.getMinX(),
                     (int)finalRect.getMinY(),
                     (int)finalRect.getMaxX(),
                     (int)finalRect.getMaxY(),
                     0,0, ecwImage.getWidth(), ecwImage.getHeight(),
                     java.awt.Color.WHITE,
                     viewport.getPanel());
        

      }
    }
    catch(Exception e) {
      validSetView = false;
  		throw new JUMPException(e.getMessage());
    }
  }

  public void close() {
    ecwRenderer.close(true);
  }

	public String getType() {
		return "ECW";
	}


}