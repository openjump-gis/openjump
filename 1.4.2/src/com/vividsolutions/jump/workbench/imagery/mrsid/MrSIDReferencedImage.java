package com.vividsolutions.jump.workbench.imagery.mrsid;

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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FileLoadDescriptor;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.ui.Viewport;

public class MrSIDReferencedImage implements ReferencedImage{
	
	private SIDInfo sidInfo;
	private String sidFilename;

	public MrSIDReferencedImage(SIDInfo info, String sidFilename) {
		this.sidInfo = info;
		this.sidFilename = sidFilename;
	}

	public Envelope getEnvelope() {

        double xm = sidInfo.getUpperLeftX();
        double xM = sidInfo.getUpperLeftX()+(sidInfo.getPixelWidth() * sidInfo.getXRes());
        double yM = sidInfo.getUpperLeftY()+(sidInfo.getPixelHeight() * sidInfo.getYRes());
        double ym = sidInfo.getUpperLeftY();
		
		return new Envelope(xm, xM, ym, yM);
	}

	public void paint(Feature f, Graphics2D g, Viewport viewport) throws JUMPException {
		
		 //view and panel refer to the workbench portion with which the user is interacting
        //raster refers to the visible portion of the SID file drawn onto the view panel
        //image refers to the created image onto which is drawn the raster extracted from the SID file
		        
        if (sidInfo == null)
        {
        	viewport.getPanel().getContext().setStatusMessage("Could not get SID info for " + sidFilename);               
        }
        else
        {
            int sidDRmin = 0; //LDB: added
            int sidDRmax = 255; //LDB: added

            int sidPixelWidth = sidInfo.getPixelWidth();
            int sidPixelHeight = sidInfo.getPixelHeight();
            double sid_xres = sidInfo.getXRes();
            double sid_ulx = sidInfo.getUpperLeftX(); //realworld coords
            double sid_uly = sidInfo.getUpperLeftY(); //realworld coords

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
            //java.awt.Toolkit.getDefaultToolkit().beep();
            
            //Here calculate the real world sid edges for level zero.
            //These will be recalculated for the final level later in the code
            //since the real world edges will walk away from the original edges
            //as we go to higher levels.
            //see paper on Georeferencing images.
            double halfPixel = 0.5 * sid_xres;
            double rwSidFileLeftEdge = sid_ulx - halfPixel;
            double rwSidFileRightEdge = rwSidFileLeftEdge + (sidPixelWidth * sid_xres);
            double rwSidFileTopEdge = sid_uly + halfPixel;
            double rwSidFileBotEdge = rwSidFileTopEdge - (sidPixelHeight * sid_xres);
            
            double rwRasterLeft = Math.max(rwViewLeft, rwSidFileLeftEdge);
            double rwRasterRight = Math.min(rwViewRight, rwSidFileRightEdge);
            double rwRasterTop = Math.min(rwViewTop, rwSidFileTopEdge);
            double rwRasterBot = Math.max(rwViewBot, rwSidFileBotEdge);
            
            //calculate the sid level which will return the number of pixels
            //that is closest to the number of view pixels so that we can
            //minimize the amount of needed stretching to make the file fit the view.
            double rwViewWidth = rwViewRight - rwViewLeft;
            double widthInFilePixels = rwViewWidth / sid_xres; //file pixels
            double widthInViewPixels = rwViewWidth / view_res; //view pixels
            int sidLevel = (int)Math.round(Math.log(widthInFilePixels / widthInViewPixels) / Math.log(2));
            if (sidLevel < 0) sidLevel = 0;
            if (sidLevel > sidInfo.getNumLevels()) sidLevel = sidInfo.getNumLevels();
            double lvlres = sid_xres * Math.pow(2, sidLevel);
            viewport.getPanel().getContext().setStatusMessage("MrSID  " + sidLevel + " OF " + sidInfo.getNumLevels());
            
            //calculate the number of pixels at this level
            int lvl = 0;
            int sidLvlPixelWidth = sidPixelWidth;
            int sidLvlPixelHeight = sidPixelHeight;
            
            while (lvl < sidLevel)
            {
                sidLvlPixelWidth = round(0.5 * sidLvlPixelWidth);
                sidLvlPixelHeight = round(0.5 * sidLvlPixelHeight);
                lvl++;
            }
            
            //now calculate the real world edges of the sid file at this level
            halfPixel = 0.5 * lvlres;
            rwSidFileLeftEdge = sid_ulx - halfPixel;
            rwSidFileRightEdge = rwSidFileLeftEdge + (sidLvlPixelWidth * lvlres);
            rwSidFileTopEdge = sid_uly + halfPixel;
            rwSidFileBotEdge = rwSidFileTopEdge - (sidLvlPixelHeight * lvlres);
            
            //check to see if this sid is inside the view area
            if (!((rwSidFileRightEdge <= rwViewLeft) || (rwSidFileLeftEdge >= rwViewRight) || (rwSidFileTopEdge <= rwViewBot) || (rwSidFileBotEdge >= rwViewTop)))
            {
                int sidLeftPixel = (int)((rwRasterLeft - rwSidFileLeftEdge) / lvlres); //trunc
                int sidRightPixel = (int)((rwRasterRight - rwSidFileLeftEdge) / lvlres); //trunc
                if (sidRightPixel == sidLvlPixelWidth) sidRightPixel = sidLvlPixelWidth - 1;
                int sidTopPixel = (int)((rwSidFileTopEdge - rwRasterTop) / lvlres); //trunc
                int sidBotPixel = (int)((rwSidFileTopEdge - rwRasterBot) / lvlres); //trunc
                if (sidBotPixel == sidLvlPixelHeight) sidBotPixel = sidLvlPixelHeight - 1;
                
                double rwSidLeft = rwSidFileLeftEdge + (sidLeftPixel * lvlres);
                double rwSidRight = rwSidFileLeftEdge + (sidRightPixel * lvlres) + lvlres;
                double rwSidTop = rwSidFileTopEdge - (sidTopPixel * lvlres);
                double rwSidBot = rwSidFileTopEdge - (sidBotPixel * lvlres) - lvlres;
                
                int leftOffset = round((rwRasterLeft - rwSidLeft) / view_res);
                int rightOffset = round((rwSidRight - rwRasterRight) / view_res);
                int topOffset = round((rwSidTop - rwRasterTop) / view_res);
                int botOffset = round((rwRasterBot - rwSidBot) / view_res);
                
                int sid_x = sidLeftPixel;
                int sid_y = sidTopPixel;
                
                int sid_w = sidRightPixel - sidLeftPixel + 1;
                if (sid_w <= 0) sid_w = 1;
                
                int sid_h = sidBotPixel - sidTopPixel + 1;
                if (sid_h <= 0) sid_h = 1;
                
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
                
                
                try
                {
                    File jpgDir = new File(MrSIDImageFactory.TMP_PATH);
                    File jpgFile = File.createTempFile("Temp", ".jpg", jpgDir);
                    String jpgFilename = jpgFile.getCanonicalPath();
                    
                    String [] runStr =
                    {
                    	MrSIDImageFactory.MRSIDDECODE,
                        //"C:\\ashsii\\jump\\etc\\mrsidgeodecode.exe",
                        "-i",
                        sidFilename,
                        "-s",
                        "" + sidLevel,
                        "-ulxy",
                        "" + sid_x,
                        "" + sid_y,
                        "-wh",
                        "" + sid_w,
                        "" + sid_h,
                        "-o",
                        jpgFilename,
                        "-jpg",
                        "-quiet",
                        "-coord",
                        "image",
                        "-drmin",
                        ""+sidDRmin,
                        "-drmax",
                        ""+sidDRmax
                                    };
                    
                    Process p = Runtime.getRuntime().exec(runStr);
                    p.waitFor();
                    p.destroy();
                    //--[sstein 03.Mai.2008] note: I checked, that until here everything is fine (i.e. the jpg image correctly created)
                    //						 with respect to creating a jpg file out of the sid image and store it in the tmp folder
	                if (((jpgFile.exists()) && (jpgFile.isFile()) && (jpgFile.canRead())))
	                {

	                	//-- [sstein 3.Mai.2008] old stuff
	                	/*  	
	                	//-- [sstein 02.04.2006] changed to javax to work with free JavaVM
	                	//   as proposed by Petter Reinholdtsen (see jpp-devel 12.03.2006)
	                	FileImageInputStream in = new FileImageInputStream(new File(jpgFilename));
	                	ImageReader decoder = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG").next();
	                	decoder.setInput(in);
	                	BufferedImage image = decoder.read(0);
	                	decoder.dispose();
	                    in.close();
	
	                    if (!sidInfo.getColorSpace().equals("GREYSCALE"))
	                    {
	                        RenderingHints rh = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	                        g.setRenderingHints(rh);
	                    }
	                    
	                    g.setComposite(AlphaComposite.Src);
	                    g.drawImage(image, image_x, image_y, image_w, image_h, viewport.getPanel());
	                    new File(jpgFilename).delete(); //so they don't accumulate in the tmp dir
	                	*/
	                	//--- [sstein 3.Mai.2008] new stuff (note: the grey values are a bit different now: lighter)                    
	                	RenderedOp image = FileLoadDescriptor.create(jpgFile.getPath(),null,null,null);
	                	
                        RenderingHints rh = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.setRenderingHints(rh);

	                    Composite composite = g.getComposite();
	                    g.setComposite(AlphaComposite.Src);
	                    BufferedImage img = image.getAsBufferedImage();
	                    g.drawImage(img, image_x, image_y, image_w, image_h, viewport.getPanel());
	                    g.setComposite(composite);
	                    //-- testing, since some files are not deleted properly
	                    boolean done = false;
	                    //int count=0;
	                    //while (!done){
		                done = jpgFile.delete(); //so they don't accumulate in the tmp dir
		                    //count++;
		                    //System.out.print(".");
		                    //if (count == 1000){
		                    	//done = true;
		                if (!done){jpgFile.delete();}
		                if (!done){jpgFile.deleteOnExit();}
		                    	//System.out.print("not deleted in 1000 rounds");
		                    //}
	                    //}
	                    //System.out.println("");
	                }
                    
                } catch (Throwable t)
                {
                	t.printStackTrace();
                	throw new JUMPException(t.getMessage());
                }
            }
        }
	}

    private int round(double num)
    {
       return (int)Math.round(num); 
    }

	public String getType() {
		return "MrSID";
	}

}
