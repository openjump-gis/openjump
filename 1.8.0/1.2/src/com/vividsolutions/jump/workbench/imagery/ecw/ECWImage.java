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
import com.ermapper.ecw.JNCSException;
import com.ermapper.ecw.JNCSFileOpenFailedException;
import com.ermapper.ecw.JNCSInvalidSetViewException;
import com.ermapper.ecw.JNCSRenderer;
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
      double xM = ecwRenderer.originX + (double)(ecwRenderer.width-1)*ecwRenderer.cellIncrementX;
      double ym = ecwRenderer.originY + (double)(ecwRenderer.height-1)*ecwRenderer.cellIncrementY;
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
      System.out.println("image not visible");
      return;
    }

    // only set view if viewport has changed
    if(! viewportEnv.equals(lastViewportEnv)) {
      validSetView = false;
      lastViewportEnv = viewportEnv;
    }
    
    try{
      // Set the view
      int width = (int)viewport.toViewRectangle(viewportEnv).getWidth();
      int height = (int)viewport.toViewRectangle(viewportEnv).getHeight();
      double dWorldTLX = viewportEnv.getMinX();
      double dWorldTLY = viewportEnv.getMaxY();
      double dWorldBRX = viewportEnv.getMaxX();
      double dWorldBRY = viewportEnv.getMinY();

      // only set view if viewport has changed
      if(! validSetView) {
        ecwRenderer.setView(ecwRenderer.numBands, bandlist, dWorldTLX, dWorldTLY, dWorldBRX, dWorldBRY, width, height);
        validSetView = true;
        //System.out.println("setView called");
      }
      ecwRenderer.drawImage(g, 0, 0, width, height, dWorldTLX, dWorldTLY, dWorldBRX, dWorldBRY, viewport.getPanel());
    }
    catch (JNCSInvalidSetViewException e) {
      // this catches the "Supersampling not supported" exception
      validSetView = false;
  		throw new JUMPException(e.getMessage());
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