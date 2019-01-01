/*
 * Created on 03.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 12:01:50 +0200 (Fr, 06 Okt 2006) $
 *  $Id: RasterImageRenderer.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.rasterimage;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.ImageCachingRenderer;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRenderer;
import com.vividsolutions.jump.workbench.ui.renderer.ThreadSafeImage;
import com.vividsolutions.jump.workbench.ui.renderer.ThreadSafeImage.Drawer;

/**
 * TODO: comment class
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * modified: [sstein]: 16.Feb.2009 changed logger-entries to comments
 */
public class RasterImageRenderer extends ImageCachingRenderer {
    
    //protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
    
    protected boolean doneRendering = true;

    /**
     *@param contentID
     *@param panel
     */
    public RasterImageRenderer(Object contentID, LayerViewPanel panel) {
        super(contentID, panel);
    }
    
    protected RasterImageLayer getRasterImageLayer(){
        return (RasterImageLayer) this.getContentID();
    }
    
    // ############
    public ThreadSafeImage getImage() {
        if (!getLayer().isVisible()) {
            return null;
        }

        return super.getImage();
    }

    public Runnable createRunnable() {
        if (!LayerRenderer.render(getLayer(), panel)) {
            return null;
        }
        return super.createRunnable();
    }
    
    public void copyTo(Graphics2D graphics) {
        if (!LayerRenderer.render(getLayer(), panel)) {
            return;
        }   
        super.copyTo(graphics);
    }   

    private RasterImageLayer getLayer() {
        return (RasterImageLayer) getContentID();
    }
    // ############

    /**
     *@param image
     *@throws Exception
     */
    @Override
    protected void renderHook(ThreadSafeImage image) throws Exception {
        if (!getRasterImageLayer().isVisible()) {
            return;
        }
        
//        while(!this.doneRendering)
//            Thread.sleep(50);
        
        doneRendering = false;

        //Create the image outside the synchronized call to #draw, because it
        // takes
        //a few seconds, and we don't want to block repaints. [Jon Aquino]
        
        RasterImageLayer rLayer = getRasterImageLayer();
        
        final BufferedImage sourceImage = rLayer.createImage(panel);
        
        // Image is out of viewport
        if(rLayer.getActualImageEnvelope() == null) {
            return;
        }
        
        final Envelope realWorldCoordinates = new Envelope(rLayer.getActualImageEnvelope());
        final Point2D upperLeftCorner = panel.getViewport().toViewPoint(new Coordinate(realWorldCoordinates.getMinX(), realWorldCoordinates.getMaxY()));
        
//        // Draw a grey rect where the image is about to show up...
//        if (false && rLayer.getOrigImageWidth() * rLayer.getOrigImageHeight() > RasterImageLayer.getMaxPixelsForFastDisplayMode()){
//            final Rectangle drawingRect = rLayer.getDrawingRectangle(10, 10, rLayer.getEnvelope(), panel.getViewport());
//            
//            if (drawingRect!=null){
//                //logger.printDebug(drawingRect.toString());
//                Drawer defaultDrawer = new ThreadSafeImage.Drawer() {
//                    public void draw(Graphics2D g) throws Exception {
//                        g.translate( upperLeftCorner.getX() , upperLeftCorner.getY() );
//                        g.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, .5f));
//                        g.setColor(Color.lightGray);
//                        g.fillRect(drawingRect.x, drawingRect.y, drawingRect.width, -drawingRect.height);
//                    }
//                };
//                
//                image.draw(defaultDrawer);
//            }
//        }
        
        
        
        final int xOffset = rLayer.getXOffset();
        final int yOffset = rLayer.getYOffset();
        
        if (sourceImage==null){
            doneRendering = true;
            return;
        }        
        
        
        
        //Drawing can take a long time. If the renderer is cancelled during
        // this
        //time, don't draw when the request returns. [Jon Aquino]
        if (cancelled) {
            doneRendering = true;
            return;
        }
        
        Drawer drawer = new ThreadSafeImage.Drawer() {
            public void draw(Graphics2D g) throws Exception {

                g.translate( upperLeftCorner.getX() , upperLeftCorner.getY() );
                
                Object oldRenderingKey = g.getRenderingHint(RenderingHints.KEY_RENDERING);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                
                Object oldColorRenderingKey = g.getRenderingHint(RenderingHints.KEY_COLOR_RENDERING);
                g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                
                Object oldDitheringKey = g.getRenderingHint(RenderingHints.KEY_DITHERING);
                g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
                
                Object oldAplhaInterpolationKey = g.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                
                Object oldAAKey = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                g.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, .0f));
                
                g.setColor(Color.RED);
                g.fillRect(xOffset, yOffset, sourceImage.getWidth(), sourceImage.getHeight());
                
                g.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1.0f - (float) getRasterImageLayer().getTransparencyLevel()));
                
                g.drawImage(sourceImage, xOffset, yOffset, null);
                
                if (oldRenderingKey!=null)
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, oldRenderingKey);
                else
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
                if (oldColorRenderingKey!=null)
                    g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, oldColorRenderingKey);
                else
                    g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_DEFAULT);
                if (oldDitheringKey!=null)
                    g.setRenderingHint(RenderingHints.KEY_DITHERING, oldDitheringKey);
                else
                    g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DEFAULT);
                if (oldAplhaInterpolationKey!=null)
                    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAplhaInterpolationKey);
                else 
                    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
                if (oldAAKey!=null)
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAAKey);
                else 
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
            }
        };

       
        image.draw(drawer);
        
        doneRendering = true;
    }

}
