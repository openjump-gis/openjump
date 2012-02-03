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
 * 
 * 02.04.2006 changed by sstein to make compilation with free JVMs possible (see below)
 */


package org.openjump.io;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.Viewport;

public class SIDLayer extends WMSLayer
{
	final static String sLayer = I18N.get("org.openjump.io.SIDLayer.Layer");
	final static String couldNotGetSIDinfoFor = I18N.get("org.openjump.io.SIDLayer.Could-not-get-SID-info-for");
	final static String sLevel = I18N.get("org.openjump.io.SIDLayer.level");
	final static String sOf = I18N.get("org.openjump.io.SIDLayer.of");
	
    private SID_Info_List sidInfoList;	
    private List imageFilenames = new ArrayList();
    private List deletedSIDs = new ArrayList();
    private int sidPixelWidth = 0;
    private int sidPixelHeight = 0;
    private double sid_xres = 1;
    private double sid_xrot = 0;
    private double sid_yrot = 0;
    private double sid_yres = 1;
    private double sid_ulx = 0; //realworld coords
    private double sid_uly = 0; //realworld coords
    private String sid_colorspace;
    private int maxLevel = 0; 
    private PlugInContext callingContext;

    /**
     *  Called by Java2XML
     */
    public SIDLayer() 
    {
    	sidInfoList = new SID_Info_List();
    }

    public SIDLayer(PlugInContext context, List layerNames) throws IOException
    {  //this code copied from WMSLayer and AbstractLayerable constructors
    		
        callingContext = context;
        LayerManager layerManager = context.getLayerManager();
        String name = "MrSID " + sLayer;
        Assert.isTrue(name != null);
        Assert.isTrue(layerManager != null);
        setLayerManager(layerManager);

        //Can't fire events because this Layerable hasn't been added to the
        //LayerManager yet. [Jon Aquino]
        boolean firingEvents = layerManager.isFiringEvents();
        layerManager.setFiringEvents(false);

        try {
            setName(layerManager.uniqueLayerName(name));
        } finally {
            layerManager.setFiringEvents(firingEvents);
        }
        
        sidInfoList = new SID_Info_List(layerNames);
    }

    private int round(double num)
    {
       return (int)Math.round(num); 
    }
     
    public Image createImage(LayerViewPanel panel) throws IOException
    {        
        //view and panel refer to the workbench portion with which the user is interacting
        //raster refers to the visible portion of the SID file drawn onto the view panel
        //image refers to the created image onto which is drawn the raster extracted from the SID file
        BufferedImage newImage = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)newImage.getGraphics();
        g.setColor(new Color(0,0,0,0)); //alpha channel = 0 for transparent background
        g.fillRect(0, 0, panel.getWidth(), panel.getHeight());       
        List imageFilenames = sidInfoList.getFileNames();
        
        for (Iterator i = imageFilenames.iterator(); i.hasNext();)
        {
            Object currObj = i.next();
            String sidFilename = (String) currObj;
            SID_Info sidInfo = sidInfoList.getInfo(sidFilename);
            if (sidInfo == null)
            {
                callingContext.getWorkbenchFrame().getOutputFrame().addText("Could not get SID info for " + sidFilename);               
            }
            else
            {
                int sidPixelWidth = sidInfo.getPixelWidth();
                int sidPixelHeight = sidInfo.getPixelHeight();
                double sid_xres = sidInfo.getXRes();
                double sid_ulx = sidInfo.getULX(); //realworld coords
                double sid_uly = sidInfo.getULY(); //realworld coords

                int image_x = 0; //x position of raster in final image in pixels
                int image_y = 0; //y position of raster in final image in pixels
                int image_w = panel.getWidth(); //width of raster in final image in pixels
                int image_h = panel.getHeight(); //height of raster in final image in pixels
                
                Viewport viewport = panel.getViewport();
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
                if (sidLevel > maxLevel) sidLevel = maxLevel;
                double lvlres = sid_xres * Math.pow(2, sidLevel);
                panel.getContext().setStatusMessage("MrSID " + sLevel + " " + sidLevel + " " + sOf + " " + maxLevel);
                
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
                    
                    File jpgDir = new File(AddSIDLayerPlugIn.TMP_PATH);
                    File jpgFile = File.createTempFile("Temp", ".jpg", jpgDir);
                    String jpgFilename = jpgFile.getCanonicalPath();
                    
                    try
                    {
                        String [] runStr =
                        {
                            AddSIDLayerPlugIn.MRSIDDECODE,
                            //"C:\\ashsii\\jump\\etc\\mrsiddecode.exe",
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
                            "image"
                        };
                        
                        Process p = Runtime.getRuntime().exec(runStr);
                        p.waitFor();
                        p.destroy();
                        
                    } catch (Throwable t)
                    {
                        t.printStackTrace();
                    }
                    
                    if (((jpgFile.exists()) && (jpgFile.isFile()) && (jpgFile.canRead())))
                    {
                    	//-- [sstein 02.04.2006] changed to javax to work with free JavaVM
                    	//   as proposed by Petter Reinholdtsen (see jpp-devel 12.03.2006)
                    	FileImageInputStream in = new FileImageInputStream(new File(jpgFilename));
                    	ImageReader decoder = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG").next();
                    	decoder.setInput(in);
                    	BufferedImage image = decoder.read(0);
                    	decoder.dispose();
                        in.close();

                        if (!sid_colorspace.equals("GREYSCALE"))
                        {
                            RenderingHints rh = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g.setRenderingHints(rh);
                        }
                        
                        g.setComposite(AlphaComposite.Src);
                        g.drawImage(image, image_x, image_y, image_w, image_h, panel);
                        new File(jpgFilename).delete(); //so they don't accumulate in the tmp dir
                    }
                }
            }
        }
        
        return newImage;
    }
    
    protected SID_Info readWorldFile(String filename) throws IOException, FileNotFoundException
    {
        String wfname = (filename.indexOf('.') < 0) ? filename : filename.substring(0, filename.indexOf('.'));
        wfname = wfname + ".sdw";
        File file = new File(wfname);
        
        if (!file.exists()) return null;
        if (!(file.isFile() && file.canRead())) return null;
        
        FileReader wf = new FileReader(wfname);
        BufferedReader in = new BufferedReader(wf);
        double sid_xres = Double.parseDouble(in.readLine());
        double sid_xrot = Double.parseDouble(in.readLine());
        double sid_yrot = Double.parseDouble(in.readLine());
        double sid_yres = Double.parseDouble(in.readLine());
        double sid_ulx = Double.parseDouble(in.readLine());
        double sid_uly = Double.parseDouble(in.readLine());
        SID_Info sidInfo = new SID_Info(filename,
        							0,
									0,
        		                    sid_xres, 
        		                    sid_xrot, 
        		                    sid_yrot, 
        		                    sid_yres, 
        		                    sid_ulx, 
        		                    sid_uly);
        in.close();
        wf.close();
        return sidInfo;
    }
    
    protected SID_Info readInfo(String sidFilename) //throws IOException
    {
        int sidPixelWidth = 0;
        int sidPixelHeight = 0;
        double sid_xres = 1;
        double sid_xrot = 0;
        double sid_yrot = 0;
        double sid_yres = 1;
        double sid_ulx = 0; //realworld coords
        double sid_uly = 0; //realworld coords
        maxLevel = 0;
        String infoFilename = AddSIDLayerPlugIn.TMP_PATH + "MrSIDinfo.txt";
        int numInfoItems = 0;

        try
        {
            String [] runStr =
            {
                AddSIDLayerPlugIn.MRSIDINFO,
                sidFilename,
                "-sid",
                "-quiet",
                "-log",
                infoFilename
            };
            
            Process p = Runtime.getRuntime().exec(runStr);
            p.waitFor();
            p.destroy();
            
            File file = new File(infoFilename);
            
            if (!(file.exists() && file.isFile() && file.canRead()))
                return null;  //this could happen if mrsidinfo.exe couldn't produce a file
            
        	//read the info
            FileReader fin = new FileReader(infoFilename);
            BufferedReader in = new BufferedReader(fin);
            String lineIn = in.readLine();
            
            while (in.ready())
            {
                String value = "";
                
                if (lineIn.indexOf("width:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sidPixelWidth = Integer.parseInt(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("height:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sidPixelHeight = Integer.parseInt(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("number of levels:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    maxLevel = Integer.parseInt(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("X UL:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sid_ulx = Double.parseDouble(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("Y UL:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sid_uly = Double.parseDouble(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("X res:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sid_xres = Double.parseDouble(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("Y res:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sid_yres = Double.parseDouble(value.trim());
                    numInfoItems++;
                }
                
                if (lineIn.indexOf("color space:") != -1)
                {
                    value = lineIn.substring(lineIn.indexOf(":") + 1);
                    sid_colorspace = value.trim();
                    numInfoItems++;
                }
                
                lineIn = in.readLine();
            }
            
            in.close();
            fin.close();
            
            if (numInfoItems == 8)
            	return new SID_Info(sidFilename,
            			sidPixelWidth,
						sidPixelHeight,
						sid_xres, 
						sid_xrot, 
						sid_yrot, 
						sid_yres, 
						sid_ulx, 
						sid_uly);
            else
            	return null;
            
        } catch (Throwable t)
		{
        	t.printStackTrace();
        	return null;
		}        
    }
    
    public void addImageFilename(String imageFilename) 
    {
        sidInfoList.addInfo(imageFilename);
    }

    public List getImageFilenames() 
    {
    	return sidInfoList.getFileNames();
    }
    
    private Blackboard blackboard = new Blackboard();
    public Blackboard getBlackboard()
    {
        return blackboard;
    }   

    private class SID_Info
	{
    	private String fileName;
	    private int pixelWidth;
	    private int pixelHeight;
	    private double xres;
	    private double xrot;
	    private double yrot;
	    private double yres;
	    private double ulx; //realworld coords
	    private double uly; //realworld coords
    	
    	SID_Info(String fileName,
    		     int pixelWidth,
				 int pixelHeight,
				 double xres,
				 double xrot,
				 double yrot,
				 double yres,
				 double ulx,
				 double uly)
		{
    		this.fileName = fileName;
    		this.pixelWidth = pixelWidth;
    		this.pixelHeight = pixelHeight;
    		this.xres = xres;
    		this.xrot = xrot;
    		this.yrot = yrot;
    		this.yres = yres;
    		this.ulx = ulx;
    		this.uly = uly;
		}
    	
    	String getFileName()
    	{
    		return fileName;
    	}

    	int getPixelWidth()
    	{
    		return pixelWidth;
    	}

    	int getPixelHeight()
    	{
    		return pixelHeight;
    	}

    	double getXRes()
    	{
    		return xres;
    	}

    	double getXRot()
    	{
    		return xrot;
    	}

    	double getYRot()
    	{
    		return yrot;
    	}

    	double getYRes()
    	{
    		return yres;
    	}

    	double getULX()
    	{
    		return ulx;
    	}

    	double getULY()
    	{
    		return uly;
    	}

	}
    
    private class SID_Info_List
	{
    	private Vector infoList = new Vector(50, 10);
    	
    	SID_Info_List()
		{
    		
		}
    	
    	SID_Info_List(List fileNames)
		{
            for (Iterator i = fileNames.iterator(); i.hasNext();)
            {				
            	String fileName = (String) i.next();
                SID_Info sidInfo = readInfo(fileName);
                if (sidInfo == null)
                	callingContext.getWorkbenchFrame().getOutputFrame().addText("Could not get SID info for " + fileName);
                else
                	infoList.add(sidInfo);
            }
		}
    	
    	public void addInfo(String fileName)
    	{
            SID_Info sidInfo = readInfo(fileName);
            if (sidInfo != null)
            	infoList.add(sidInfo);
    	}
    	
    	public SID_Info getInfo(String filename)
		{
            for (Iterator i = infoList.iterator(); i.hasNext();)
            {
            	SID_Info sidInfo = (SID_Info) i.next();
            	if (sidInfo.getFileName().equals(filename))
            		return sidInfo;
            }
            return null;
		}
        
    	public List getFileNames()
        {
        	List imageFilenames = new ArrayList();
            for (Iterator i = infoList.iterator(); i.hasNext();)
            	imageFilenames.add(((SID_Info) i.next()).getFileName());
        	
        	return Collections.unmodifiableList(imageFilenames);
        }
	}

}

