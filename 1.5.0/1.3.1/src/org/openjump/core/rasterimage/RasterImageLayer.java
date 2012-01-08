/*
 * Created on 03.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 12:01:50 +0200 (Fr, 06 Okt 2006) $
 *  $Id: RasterImageLayer.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.rasterimage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.openjump.util.metaData.MetaDataMap;
import org.openjump.util.metaData.ObjectContainingMetaInformation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.AbstractLayerable;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * Layer representing a georeferenced raster image (e.g. an areal photography) in OpenJump.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * modified: [sstein]: 16.Feb.2009 changed logger-entries to comments, used frame.warnUser
 */
public class RasterImageLayer extends AbstractLayerable implements ObjectContainingMetaInformation {
    
    protected static Blackboard blackboard = null;
    
    protected final static String BLACKBOARD_KEY_PLUGINCONTEXT = PlugInContext.class.getName();
    protected final static String BLACKBOARD_KEY_WORKBENCHCONTEXT = PlugInContext.class.getName();
    
    protected int lastImgProcessingMode = 0;
    
    protected final static int MODE_NONE = 0;
    protected final static int MODE_SCALINGFIRST = 1;
    protected final static int MODE_CLIPPINGFIRST = 2;
    protected final static int MODE_FASTDISPLAY = 3;
    
    protected Rectangle bildAusschnitt, alterBildAusschnitt, visibleRect = null;
    
    protected double scaleXImg2Canvas, oldScaleXImg2Canvas, scaleYImg2Canvas;
    
    protected int xOffset, yOffset;
    
    //protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
    
    protected double transparencyLevel = .0f;

    
    protected static long availRAM = Runtime.getRuntime().maxMemory();
    protected static double freeRamFactor = 0.5;
    protected static double minRamToKeepFree = availRAM * freeRamFactor;
    protected static int maxPixelsForFastDisplayMode = 250000;

    protected String imageFileName = null;
    protected int origImageWidth, origImageHeight;
    protected boolean imageSet = false;
    protected PlanarImage image = null;
    
    protected PlanarImage imageProcessingStep1 = null, imageProcessingStep2 = null;

    
    protected Envelope envelope = null, visibleEnv = null;
    
    /**
     * Flag to decide, if events are fired automatically, if the appearance (envelope, etc.) changes.<br>
     * default: true
     */
    protected boolean firingAppearanceEvents = true;
    
    /**
     * Flag to control if the image should be deleted from RAM as soon as possible to save RAM or if it should be keeped e.g. because it was generated
     * dynamically and can not be loaded from a file again, once it was deleted.
     */
    protected boolean needToKeepImage = false;
    
    protected static final Point nullpunkt = new Point(0,0);
    
    protected Color transparentColor = null;
    protected boolean transparencyColorNeedsToBeApplied = false;


    /**
     * for java2xml
     */
    public RasterImageLayer() {
        super();
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
    }

    /**
     *@param name name of the layer
     *@param layerManager
     *@param image the image (if already loaded) or null
     *@param envelope real-world coordinates of the image
     */
    public RasterImageLayer(String name, LayerManager layerManager, String imageFileName, RenderedImage image, Envelope envelope) {
        super(name, layerManager);
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
        this.imageFileName = imageFileName;
        this.envelope = envelope;
        
        if (image != null)
            this.setImage(javax.media.jai.PlanarImage.wrapRenderedImage(image));
    }
    
    
    /**
     * Constructor to be used in case the image was not loaded from a file, so there is
     * no file name, but an image
     * 
     *@param name name of the layer
     *@param layerManager
     *@param image the image
     *@param envelope real-world coordinates of the image
     */
    public RasterImageLayer(String name, LayerManager layerManager, RenderedImage image, Envelope envelope) {
        super(name, layerManager);
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
        this.setNeedToKeepImage(true);
        this.envelope = envelope;
        
        if (image != null)
            this.setImage(javax.media.jai.PlanarImage.wrapRenderedImage(image));
        else{
            //logger.printError("given image is NULL");
        }
    }

    /**
     *@inheritDoc
     */
    public Blackboard getBlackboard() {
        if (RasterImageLayer.blackboard == null)
            RasterImageLayer.blackboard = new Blackboard();
        
        return RasterImageLayer.blackboard;
    }

    public Object clone() throws CloneNotSupportedException {
        if (this.isNeedToKeepImage())
            return new RasterImageLayer(this.getName(), this.getLayerManager(), this.getImage(), new Envelope(this.getEnvelope()));
        
        return new RasterImageLayer(this.getName(), this.getLayerManager(), this.getImageFileName(), this.getImage(), new Envelope(this.getEnvelope()));
    }
    
    /**
     * apply a scale operation to the image and return the
     * new image.
     */
    protected PlanarImage scaleImage(PlanarImage im, float xScale, float yScale) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(xScale);
        pb.add(yScale);

        return JAI.create("Scale", pb, null);
    }
    
    
    
    protected PlanarImage createOneColorImage(){
        //logger.printDebug("fixing 1px scale: scaleXImg2Canvas = " + scaleXImg2Canvas + ", scaleYImg2Canvas = " + scaleYImg2Canvas);
        //logger.printDebug("this.imageProcessingStep1: " + this.imageProcessingStep1 .getWidth() + ", " + this.imageProcessingStep1.getHeight());
        
        scaleXImg2Canvas = Math.min( Math.abs(scaleXImg2Canvas), Math.abs(visibleRect.width) );
        scaleYImg2Canvas = Math.min( Math.abs(scaleYImg2Canvas), Math.abs(visibleRect.height) );
        
        //logger.printDebug("fixed 1px scale: scaleXImg2Canvas = " + scaleXImg2Canvas + ", scaleYImg2Canvas = " + scaleYImg2Canvas);
        
        BufferedImage bim = new BufferedImage(visibleRect.width, visibleRect.height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D grfcs = bim.createGraphics();
        
        grfcs.setColor(new Color(this.imageProcessingStep1.getAsBufferedImage().getRGB(0,0)));
        
        //logger.printDebug("color: " + new Color(this.imageProcessingStep1.getAsBufferedImage().getRGB(0,0)).toString());
        
        grfcs.fillRect( 0, 0, bim.getWidth(), bim.getHeight() );
        
        grfcs.dispose();
        
        return PlanarImage.wrapRenderedImage(bim);
    }
    
    /**
     * Creates the image to draw
     */
    public BufferedImage createImage(LayerViewPanel layerViewPanel) {
        
        Viewport viewport = layerViewPanel.getViewport();
        
        if (!this.isVisible() || this.transparencyLevel >= 1.0){
            this.setImageProcessingMode(RasterImageLayer.MODE_NONE);
            this.clearImage(true);
            //logger.printDebug("!visible");
            return null;
        }
        
        BufferedImage imageToDraw = null;

        try {
            Point2D upperLeftCornerOfImage = null;
            Point2D lowerRightCornerOfImage = null;
            
            try {
                upperLeftCornerOfImage = viewport.toViewPoint(new Coordinate(this.getEnvelope().getMinX(), this.getEnvelope().getMaxY()));
                lowerRightCornerOfImage = viewport.toViewPoint(new Coordinate(this.getEnvelope().getMaxX(), this.getEnvelope().getMinY()));
            } catch(java.awt.geom.NoninvertibleTransformException ne) {
                //logger.printError(ne.getLocalizedMessage());
                ne.printStackTrace();
                return null;
            }


            visibleRect = viewport.getPanel().getVisibleRect();
            
            int visibleX1 = visibleRect.x;
            int visibleY1 = visibleRect.y;
            int visibleX2 = visibleX1 + visibleRect.width;
            int visibleY2 = visibleY1 + visibleRect.height;
            
            Coordinate upperLeftVisible = viewport.toModelCoordinate(nullpunkt);
            Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(visibleX2, visibleY2));
            
            Envelope newVisibleEnv = new Envelope(upperLeftVisible.x, lowerRightVisible.x, upperLeftVisible.y, lowerRightVisible.y);

            double scaledWidth = lowerRightCornerOfImage.getX() - upperLeftCornerOfImage.getX();
            double scaledHeight = upperLeftCornerOfImage.getY() - lowerRightCornerOfImage.getY();
            
            if (!this.isImageSet()){
                this.reLoadImage();
            }
            
            scaleXImg2Canvas = scaledWidth / this.origImageWidth;
            scaleYImg2Canvas = scaledHeight / this.origImageHeight;
            
            if (this.visibleEnv == null || this.visibleEnv.getMinX() != newVisibleEnv.getMinX() || this.visibleEnv.getMaxX() != newVisibleEnv.getMaxX() || 
                    this.visibleEnv.getMinY() != newVisibleEnv.getMinY() || this.visibleEnv.getMaxY() != newVisibleEnv.getMaxY() ){
                this.visibleEnv = newVisibleEnv;
                
                if ( (this.origImageWidth * this.origImageHeight) < RasterImageLayer.getMaxPixelsForFastDisplayMode() ){
                    
                    // faster display (uses more RAM) for small images
                    this.setImageProcessingMode(RasterImageLayer.MODE_FASTDISPLAY);
                    
                    if (this.isImageNull()){
                        this.reLoadImage();
                    }
                    
                    this.bildAusschnitt = this.getVisibleImageCoordinatesOfImage( this.origImageWidth, this.origImageHeight, this.visibleEnv, this.getEnvelope() );
                    
                    if (this.imageProcessingStep2 == null || (scaleXImg2Canvas != this.oldScaleXImg2Canvas || !RasterImageLayer.tilesAreNotNullAndCongruent( this.bildAusschnitt, this.alterBildAusschnitt))){
    
                        this.imageProcessingStep1 = this.getVisiblePartOfTheImage( this.getImage(), this.bildAusschnitt );
                        
                        if ( this.imageProcessingStep1 != null) {
                            // avoid an 1 pixel by 1 pixel image to get scaled to thousands by thousands pixels causing an out of memory error
                            if (this.bildAusschnitt.width == 1 || this.bildAusschnitt.height == 1){
                                this.xOffset = 0;
                                this.yOffset = 0;
                                this.imageProcessingStep2 = this.createOneColorImage();
                            } else {
                                this.imageProcessingStep2 = this.getScaledImageMatchingVisible( this.imageProcessingStep1, scaleXImg2Canvas, scaleYImg2Canvas );
                            }

                        } else {
                            return null;
                        }
                        
                        if (this.transparentColor!=null) transparencyColorNeedsToBeApplied = true;
                        
                        this.imageProcessingStep1 = null;
                        
                        this.xOffset = (int)(this.xOffset *scaleXImg2Canvas);
                        this.yOffset = (int)(this.yOffset *(-scaleYImg2Canvas));
                        
                        this.oldScaleXImg2Canvas = this.scaleXImg2Canvas;
                        
                        this.alterBildAusschnitt = this.bildAusschnitt;
                    }
    
                } else if ( (scaleXImg2Canvas >= 1) || (scaledWidth > 2500 && visibleRect.width < 1500)){
                    
                    this.setImageProcessingMode(RasterImageLayer.MODE_CLIPPINGFIRST);

                    this.bildAusschnitt = this.getVisibleImageCoordinatesOfImage( this.origImageWidth, this.origImageHeight, visibleEnv, this.getEnvelope() );
                    
                    if (this.imageProcessingStep2 == null || (scaleXImg2Canvas != this.oldScaleXImg2Canvas || !RasterImageLayer.tilesAreNotNullAndCongruent( this.bildAusschnitt, this.alterBildAusschnitt))){
    
                        if (!RasterImageLayer.tilesAreNotNullAndCongruent(this.alterBildAusschnitt, this.bildAusschnitt)){
                            this.imageProcessingStep1 = null;
                            
                            if (this.isImageNull()){
                                this.reLoadImage();
                            }
                            
                            this.imageProcessingStep1 = this.getVisiblePartOfTheImage( this.getImage(), this.bildAusschnitt );
                            
                            this.clearImage(false);
                        }
                        
                        if ( this.imageProcessingStep1 != null) {
                            
                            // avoid an 1 pixel by 1 pixel image to get scaled to thousands by thousands pixels causing an out of memory error
                            if (this.bildAusschnitt.width == 1 || this.bildAusschnitt.height == 1){
                                this.xOffset = 0;
                                this.yOffset = 0;
                                this.imageProcessingStep2 = this.createOneColorImage();
                            } else {
                                this.imageProcessingStep2 = this.getScaledImageMatchingVisible( this.imageProcessingStep1, scaleXImg2Canvas, scaleYImg2Canvas );
                            }
                        } else {
                            return null;
                        }
                        
                        if (this.transparentColor!=null) transparencyColorNeedsToBeApplied = true;
                                                
                        this.imageProcessingStep1 = null;
                        
                        this.xOffset = (int)(this.xOffset *scaleXImg2Canvas);
                        this.yOffset = (int)(this.yOffset *(-scaleYImg2Canvas));
                        
                        this.oldScaleXImg2Canvas = this.scaleXImg2Canvas;
                        
                        this.alterBildAusschnitt = this.bildAusschnitt;
                    }
                    
                } else {
                    this.setImageProcessingMode(RasterImageLayer.MODE_SCALINGFIRST);
                    
                    if ( scaleXImg2Canvas != this.oldScaleXImg2Canvas || this.imageProcessingStep1 == null){
                        this.imageProcessingStep1 = null;
                        
                        if (this.isImageNull()){
                            this.reLoadImage();
                        }
                        this.imageProcessingStep1 = this.getScaledImageMatchingVisible( this.getImage(), scaleXImg2Canvas, scaleYImg2Canvas );
                        this.clearImage(false);
                        
                        this.oldScaleXImg2Canvas = this.scaleXImg2Canvas;
                    } 
                    if ( this.imageProcessingStep1 != null ){
                        this.bildAusschnitt = this.getVisibleImageCoordinatesOfImage( this.imageProcessingStep1, visibleEnv, this.getEnvelope() );
                        if ( this.imageProcessingStep2 == null || !RasterImageLayer.tilesAreNotNullAndCongruent( this.bildAusschnitt, this.alterBildAusschnitt)){
                            this.imageProcessingStep2 = this.getVisiblePartOfTheImage( PlanarImage.wrapRenderedImage(this.imageProcessingStep1), this.bildAusschnitt );
                            this.alterBildAusschnitt = this.bildAusschnitt;
                        }
                    } else {
                        return null;
                    }
                    
                    if (this.transparentColor!=null) transparencyColorNeedsToBeApplied = true;
                    
                }
                
            }
            
            if (this.imageProcessingStep2 != null && transparencyColorNeedsToBeApplied ){
                imageToDraw = this.setupTransparency(this.imageProcessingStep2);
            } else if (this.imageProcessingStep2 != null) {
                imageToDraw = this.imageProcessingStep2.getAsBufferedImage();
            }
            

        } catch (Exception e){
            //logger.printError(e.getMessage());
            e.printStackTrace();
        }

        if (Runtime.getRuntime().freeMemory() < RasterImageLayer.getMinRamToKeepFree()){
            this.clearImage(true);
        }
        
        if (imageToDraw != null) {
            return imageToDraw;
        } else if (this.imageProcessingStep2!=null) {
            return this.imageProcessingStep2.getAsBufferedImage();
        }
        
        
        return null;
    }
    
    /**
     * deletes image from RAM (if it is not to be keeped and if the RAM consumption is high)
     * and calls the garbage collector, if the <code>garbageCollect</code> is true.
     *@param garbageCollect if true the garbage collector will be called (this parameter may be overridden, if there is not enough RAM available...)
     */
    public boolean clearImage(boolean garbageCollect){
        boolean reallyNeedToFreeRAM = (Runtime.getRuntime().freeMemory() < minRamToKeepFree);
        if (!this.needToKeepImage && reallyNeedToFreeRAM ){
            this.image = null;
        }
        if (garbageCollect){
            Runtime.getRuntime().gc();
        }
        return reallyNeedToFreeRAM;
    }
    
    /**
     * flushes all images from the RAM.
     *@param garbageCollect if true the garbage collector will be called (this parameter may be overridden, if there is not enough RAM available...)
     */
    public void flushImages(boolean garbageCollect){
        if (this.image!=null)
            this.image.dispose();
        this.image = null;
        
        if (this.imageProcessingStep1!=null)
            this.imageProcessingStep1.dispose();
        this.imageProcessingStep1 = null;
        
        if (this.imageProcessingStep2!=null)
            this.imageProcessingStep2.dispose();
        this.imageProcessingStep2 = null;
    
        if (garbageCollect){
            Runtime.getRuntime().gc();
        }
    }
    
    public void reLoadImage(){
        WorkbenchContext context = this.getWorkbenchContext();
        
        if (this.image == null && !this.needToKeepImage){
            this.setImage( RasterImageLayer.loadImage( context, imageFileName  ) );
        } else if (this.image == null && this.needToKeepImage){
            //logger.printError("was advised to keep image, but there is none!");
        }
    }
    
    /**
     * Returns the dimensions (width and height in px) of the image as a <code>Point</code> object.
     * The clue is that only the image file's header is read to get this information, so it's quite
     * fast, because the image was not entirely read.  
     *@param context the WorkbenchContext
     *@param filenameOrURL the image file's name or URL
     *@return a point which's x is the image's width and y is the height
     */
    public static Point getImageDimensions(WorkbenchContext context, String filenameOrURL) {
        
        if (!filenameOrURL.toLowerCase().endsWith(".jpg")){
            try {
                // JAI required!!
                javax.media.jai.PlanarImage pImage = javax.media.jai.JAI.create("fileload", filenameOrURL);
                if (pImage != null) {
                    return new Point(pImage.getWidth(), pImage.getHeight());
                }
            } catch (Throwable e) {
                //logger.printError(e.getLocalizedMessage());
                if (e.getMessage().indexOf("Planar (band-sequential) format TIFF is not supported") > -1) {
                	context.getWorkbench().getFrame().warnUser("unsupported-tiff");
                } else {
                	context.getWorkbench().getFrame().warnUser("problems-loading-image"+ e.getMessage());
                }
            }
        } else {
            
            BufferedImage image = null;

            try {
                image = ImageIO.read(new File(filenameOrURL));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return new Point(image.getWidth(), image.getHeight());
            
        }
        
        return null;
    }
    
    public final static PlanarImage loadImage(WorkbenchContext context, String filenameOrURL) {

         if (filenameOrURL.toLowerCase().endsWith(".gif")  || filenameOrURL.toLowerCase().endsWith(".png")
                 || filenameOrURL.toLowerCase().endsWith(".tif") || filenameOrURL.toLowerCase().endsWith(".tiff")) {
             try {
                 javax.media.jai.PlanarImage pImage = javax.media.jai.JAI.create("fileload", filenameOrURL);
                 return pImage;
             } catch (Throwable e) {
                 //logger.printError(e.getMessage());
                 if (e.getMessage().indexOf("Planar (band-sequential) format TIFF is not supported") > -1) {
                	 context.getWorkbench().getFrame().warnUser(I18N.get("unsupported-tiff"));
                 } else {
                     context.getWorkbench().getFrame().warnUser(I18N.get("problems-loading-image")+ e.getMessage());
                 }
                 e.printStackTrace();
                 return null;
             }
         } else if (filenameOrURL.toLowerCase().endsWith(".jpg")){
             BufferedImage image = null;

             try {
                 image = ImageIO.read(new File(filenameOrURL));
             } catch (IOException e) {
                 e.printStackTrace();
             }
             
             return PlanarImage.wrapRenderedImage(image);

         }
         //logger.printError("unsupported image format"); 
         return null;
     }
    
    /**
     * @return Envelope with the real world coordinates of the image
     */
    public Envelope getEnvelope() {
        return envelope;
    }
    /**
     * Sets the Envelope object containing the real world coordinates (e.g. WGS84) of the image - this needs to be set (if it wasn't given to the constructor)!
     *@param envelope the Envelope
     */
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
        
        this.forceTotalRepaint();
        
        if (this.isFiringAppearanceEvents())
            this.fireAppearanceChanged();
    }
    
    /**
     * for java2xml
     *@return the Envelope as string
     */
    public String getXmlEnvelope(){
        return this.envelope.toString();
    }
    
    /**
     * for java2xml
     *@param envStr the Envelope as string
     */
    public void setXmlEnvelope(String envStr){
        String coords = envStr.substring(envStr.indexOf("[")+1, envStr.indexOf("]"));
        
        String[] coordArray = coords.split(",");
        
        String[] xCoords = coordArray[0].split(":");
        String[] yCoords = coordArray[1].split(":");
        
        double minX = java.lang.Double.parseDouble(xCoords[0]);
        double maxX = java.lang.Double.parseDouble(xCoords[1]);
        
        double minY = java.lang.Double.parseDouble(yCoords[0]);
        double maxY = java.lang.Double.parseDouble(yCoords[1]);
        
        this.setEnvelope( new Envelope( minX, maxX, minY, maxY ) );
    }
    
    /**
     * Method to change the coordinates of the image and later apply the
     * changes to the RasterImageLayer by using {@link RasterImageLayer#setGeometryAsEnvelope(Geometry)}.
     *@return return the envelope (= bounding box) as a geometry, 
     */
    public Polygon getEnvelopeAsGeometry(){
        Coordinate[] coordinates = new Coordinate[5];
        
        coordinates[0] = new Coordinate(this.envelope.getMinX(), this.envelope.getMaxY());
        coordinates[1] = new Coordinate(this.envelope.getMaxX(), this.envelope.getMaxY());
        coordinates[2] = new Coordinate(this.envelope.getMaxX(), this.envelope.getMinY());
        coordinates[3] = new Coordinate(this.envelope.getMinX(), this.envelope.getMinY());
        coordinates[4] = new Coordinate(this.envelope.getMinX(), this.envelope.getMaxY());
        
        GeometryFactory gf = new GeometryFactory();
        
        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }
    
    /**
     * Method to set the coordinates of the image, e.g. after changing them after using {@link RasterImageLayer#getEnvelopeAsGeometry()}.
     */
    public void setGeometryAsEnvelope(Geometry geometry){
        this.setEnvelope(geometry.getEnvelopeInternal());
    }
    
    
    /**
     * Add transparency to the image (more exactly: to each pixel which a color == this.transparentColor)
     *@param pImage the image
     */
    private BufferedImage setupTransparency(PlanarImage pImage){
        BufferedImage bim = pImage.getAsBufferedImage();
        
        ColorModel cm = bim.getColorModel();
        int fullTransparencyAlpha = 255;
        
        if (this.getTransparentColor()==null){
            return null;
        }
        
        int transparentColor = this.getTransparentColor().getRGB();
        
        int currentColor = -1;
        int[] argb = new int[4];
        
        if (!cm.hasAlpha()){
            bim = RasterImageLayer.makeBufferedImage(bim);
            cm = bim.getColorModel();
           
        }
        
        for( int w=0; w<bim.getWidth(); w++){
            for (int h=0; h<bim.getHeight(); h++){
                
                currentColor = bim.getRGB(w,h);
                
                if (currentColor==transparentColor){
                    
                    argb[0] = fullTransparencyAlpha;
                    argb[1] = cm.getRed(currentColor);
                    argb[2] = cm.getGreen(currentColor);
                    argb[3] = cm.getBlue(currentColor);
                    
                    bim.setRGB(w,h,1,1,argb,0,1);
                }
            }
        }
        
        return bim;
    }
    
    private void setImageProcessingMode( int nr ){
        if (lastImgProcessingMode != nr){
            if (this.imageProcessingStep1!=null)
                this.imageProcessingStep1.dispose();
            this.imageProcessingStep1 = null;
            
            if (this.imageProcessingStep2!=null)
                this.imageProcessingStep2.dispose();
            this.imageProcessingStep2 = null;

            this.bildAusschnitt = null;
            this.alterBildAusschnitt = null;
            
            this.oldScaleXImg2Canvas = -1;
            
            if (Runtime.getRuntime().freeMemory() < RasterImageLayer.getMinRamToKeepFree()){
                Runtime.getRuntime().gc();
            }
            
            this.lastImgProcessingMode = nr;
        }
    }
    

    
    private static boolean tilesAreNotNullAndCongruent( Rectangle r1, Rectangle r2 ){
        boolean result;
        if (r1 == null || r2 == null) result = false;
        else result = (r1.x == r2.x && r1.y == r2.y && r1.width == r2.width && r1.height == r2.height);
        return result;
    }
    
    /**
     * creates a BufferedImage out of an Image
     *@param im the Image
     *@return the BufferedImage
     */
    public static final BufferedImage makeBufferedImage(Image im) { 
        BufferedImage copy = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_ARGB); 
        // create a graphics context 
        Graphics2D g2d = copy.createGraphics(); 
        // copy image 
        g2d.drawImage(im,0,0,null); 
        g2d.dispose(); 
        return copy; 
    }
    
    protected PlanarImage getScaledImageMatchingVisible( PlanarImage toBeScaled, double XscaleImg2Canvas, double YscaleImg2Canvas ){

        if (toBeScaled==null) return null;
        
        int scaledWidth = (int)(toBeScaled.getWidth() * XscaleImg2Canvas);
        int scaledHeight = (int)(toBeScaled.getHeight() * Math.abs(YscaleImg2Canvas) );
        
        if (scaledWidth<=0 || scaledHeight<=0) return null;
        
        return this.scaleImage(toBeScaled, (float)XscaleImg2Canvas, (float)Math.abs(YscaleImg2Canvas) );

    }
    
    public BufferedImage getTileAsImage( Envelope wantedEnvelope ){
        double imgWidth = this.origImageWidth;
        double imgHeight = this.origImageHeight;
        Envelope imageEnv = this.getEnvelope();
        
        double minVisibleX = Math.max(wantedEnvelope.getMinX(), imageEnv.getMinX());
        double minVisibleY = Math.max(wantedEnvelope.getMinY(), imageEnv.getMinY());
        
        double maxVisibleX = Math.min(wantedEnvelope.getMaxX(), imageEnv.getMaxX());
        double maxVisibleY = Math.min(wantedEnvelope.getMaxY(), imageEnv.getMaxY());
        
        double offset2VisibleX = imageEnv.getMinX() - wantedEnvelope.getMinX();
        double offset2VisibleY = wantedEnvelope.getMaxY() - imageEnv.getMaxY();
        
        double scaleX = imgWidth / imageEnv.getWidth();
        double scaleY = imgHeight / imageEnv.getHeight();
        
        // use local variables!
        int xOffset, yOffset, width, height;
        
        if (offset2VisibleX >= 0){
            xOffset = 0;
        } else {
            xOffset = (int)(-offset2VisibleX * scaleX);
        }
        
        if (offset2VisibleY >= 0){
            yOffset = 0;
        } else {
            yOffset = (int)(-offset2VisibleY * scaleY);
        }
        
        width = (int)((maxVisibleX-minVisibleX) * scaleX);
        height =  (int)((maxVisibleY-minVisibleY) * scaleY);
        
        if (width < imgWidth && height < imgHeight){ 
            width += 1;
            height += 1;
        }
        
        
        if (width <= 0 || height <= 0) return null;
        
        int wantedWidth = (int)(wantedEnvelope.getWidth() * scaleX);
        int wantedHeight = (int)(wantedEnvelope.getHeight() * scaleY);
        
        if (this.image==null){
            this.reLoadImage();
        }
        
        BufferedImage imgTile = this.image.getAsBufferedImage( new Rectangle(xOffset, yOffset, width, height), this.image.getColorModel());
        
        BufferedImage result = new BufferedImage( wantedWidth, wantedHeight, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D graf = result.createGraphics();
        
        Color backgroundColor = (this.transparentColor!=null)?this.transparentColor:Color.white;
        
        graf.fillRect(0,0,wantedWidth, wantedHeight);
        
        int xTileOffset,yTileOffset;
        
        if (xOffset > 0){
            xTileOffset = 0;
        } else {
            xTileOffset = (int)(offset2VisibleX * scaleX);
        }
        
        if (yOffset > 0){
            yTileOffset = 0;
        } else {
            yTileOffset = (int)(offset2VisibleY * scaleY);
        }
        
        graf.drawImage(imgTile, xTileOffset, yTileOffset, imgTile.getWidth(), imgTile.getHeight(), backgroundColor, null);
        graf.dispose();
        
        this.clearImage(false);
        
        return result;
    }
    
    protected WorkbenchContext getWorkbenchContext(){
        return (WorkbenchContext)this.getBlackboard().get(BLACKBOARD_KEY_WORKBENCHCONTEXT);
    }
    
    public static void setWorkbenchContext(WorkbenchContext wContext){
        if (blackboard==null)
            blackboard = new Blackboard();
        
        blackboard.put(BLACKBOARD_KEY_WORKBENCHCONTEXT, wContext);
    }
    
    public Rectangle getDrawingRectangle( double imgWidth, double imgHeight, Envelope imageEnv, Viewport viewport ) throws NoninvertibleTransformException{
        
        Rectangle visible = viewport.getPanel().getVisibleRect();
        
        Point2D upperLeftCorner = null;
        Point2D lowerRightCorner = null;
        
        try {
            upperLeftCorner = viewport.toViewPoint(new Coordinate(imageEnv.getMinX(), imageEnv.getMaxY()));
            lowerRightCorner = viewport.toViewPoint(new Coordinate(imageEnv.getMaxX(), imageEnv.getMinY()));
        } catch(java.awt.geom.NoninvertibleTransformException ne) {
            //logger.printError(ne.getLocalizedMessage());
            ne.printStackTrace();
            return null;
        }
        
        int visibleX1 = visible.x;
        int visibleY1 = visible.y;
        int visibleX2 = visibleX1 + visible.width;
        int visibleY2 = visibleY1 + visible.height;
        
        Coordinate upperLeftVisible = viewport.toModelCoordinate(nullpunkt);
        Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(visibleX2, visibleY2));
        
        Envelope newVisibleEnv = new Envelope(upperLeftVisible.x, lowerRightVisible.x, upperLeftVisible.y, lowerRightVisible.y);
        
        Rectangle rect = this.getVisibleImageCoordinatesOfImage(imgWidth, imgHeight, newVisibleEnv, imageEnv);
        
        if (rect==null) return null;
        
        double scaledWidth = lowerRightCorner.getX() - upperLeftCorner.getX();
        double scaledHeight = upperLeftCorner.getY() - lowerRightCorner.getY();
        
        
        double scaleXImg2Canvas = scaledWidth / imgWidth;
        double scaleYImg2Canvas = scaledHeight / imgHeight;
        
        rect.width *= scaleXImg2Canvas;
        rect.height *= scaleYImg2Canvas;
        
        return rect;
    }
    
    protected Rectangle getVisibleImageCoordinatesOfImage( double imgWidth, double imgHeight, Envelope visible, Envelope imageEnv ){
        double minVisibleX = Math.max(visible.getMinX(), imageEnv.getMinX());
        double minVisibleY = Math.max(visible.getMinY(), imageEnv.getMinY());
        
        double maxVisibleX = Math.min(visible.getMaxX(), imageEnv.getMaxX());
        double maxVisibleY = Math.min(visible.getMaxY(), imageEnv.getMaxY());
        
        double offset2VisibleX = imageEnv.getMinX() - visible.getMinX();
        double offset2VisibleY = visible.getMaxY() - imageEnv.getMaxY();
        
        double scaleX = imgWidth / imageEnv.getWidth();
        double scaleY = imgHeight / imageEnv.getHeight();
        
        if (offset2VisibleX >= 0){
            this.xOffset = 0;
        } else {
            this.xOffset = (int)(-offset2VisibleX * scaleX);
        }
        
        if (offset2VisibleY >= 0){
            this.yOffset = 0;
        } else {
            this.yOffset = (int)(-offset2VisibleY * scaleY);
        }
        
        int width = (int)((maxVisibleX-minVisibleX) * scaleX);
        int height =  (int)((maxVisibleY-minVisibleY) * scaleY);
        
        if (width < imgWidth && height < imgHeight){ 
            width += 1;
            height += 1;
        }
        
        
        if (width <= 0 || height <= 0){
            return null;
        }
        
        return new Rectangle(this.xOffset, this.yOffset, width, height);
    }
    
   
    protected Rectangle getVisibleImageCoordinatesOfImage( PlanarImage img, Envelope visible, Envelope imageEnv ){
        return this.getVisibleImageCoordinatesOfImage( img.getWidth(), img.getHeight(), visible, imageEnv );
    }
    
    protected PlanarImage getVisiblePartOfTheImage( PlanarImage img, Rectangle desiredImageArea ){
        if (desiredImageArea==null){
            return null;
        }
        if ( desiredImageArea.width > 0 && desiredImageArea.height > 0 ){
            if (desiredImageArea.width + desiredImageArea.x <= img.getWidth() &&
                    desiredImageArea.height + desiredImageArea.y <= img.getHeight() )
                return PlanarImage.wrapRenderedImage( img.getAsBufferedImage( new Rectangle(desiredImageArea.x, desiredImageArea.y, desiredImageArea.width, desiredImageArea.height), img.getColorModel() ));
            //logger.printWarning("desired area invalid: " + (desiredImageArea.width + desiredImageArea.x) + ", " + (desiredImageArea.height + desiredImageArea.y) + "; image dimensions: " + img.getWidth() + ", " + img.getHeight());
        } 
        return null;
    }

    /**
     * Sets the image that will be shown in the map (also sets some interally used flags)
     *@param image image that will be shown in the map
     */
    public void setImage(javax.media.jai.PlanarImage image) {
        this.image = image;
        origImageWidth = image.getWidth();
        origImageHeight = image.getHeight();
        
        imageSet = true;
    }
    
    
    
    public void setImageSet(boolean imageSet) {
        this.imageSet = imageSet;
    }

    public boolean isImageNull(){
        return this.image == null;
    }
    
    /**
     * returns the image 
     *@return the image
     */
    public PlanarImage getImage(){
        if (this.image == null)
            this.reLoadImage();
        return this.image;
    }
    
    /**
     *@return true, if the image object was set at least once, else false
     */
    public boolean isImageSet() {
        return imageSet;
    }
    
    /**
     * Returns the transparency level of the image. The transparencyLevel controlls the transparency level of the whole image (all pixels). It
     * is independent of the transparency color, that replaces a certain color in the image. 
     * The transparencyLevel is expressed as a float within a range from 0.0 (no transparency) to 1.0 (full transparency).
     *@return the transparency level of the image
     */
    public double getTransparencyLevel() {
        return transparencyLevel;
    }
    
  
    /**
     * Sets the transparency level of the image. This controlls the transparency level of the whole image (all pixels). It
     * is independent of the transparency color, that replaces a certain color in the image. 
     * The transparencyLevel is expressed as a float within a range from 0.0 (no transparency) to 1.0 (full transparency).
     *@param transparencyLevel the transparency level of the image
     */
    public void setTransparencyLevel(double transparencyLevel) {
        if (transparencyLevel != this.transparencyLevel){
            this.transparencyLevel = transparencyLevel;
            
            if (this.isFiringAppearanceEvents()) 
                this.fireAppearanceChanged();
        }
    }
    /**
     * Sets the transparency level of the image. This controlls the transparency level of the whole image (all pixels). It
     * is independent of the transparency color, that replaces a certain color in the image. 
     * The transparencyLevel is expressed as a percentage within a range from 0 (no transparency) to 100 (full transparency).
     *@param transparencyInPercent the transparency level of the image
     */
    public void setTransparencyLevelInPercent(int transparencyInPercent) {
        double tLevel = transparencyInPercent/100.0;
        if (tLevel != this.transparencyLevel){
            this.transparencyLevel = tLevel;
            if (this.isFiringAppearanceEvents()) 
                this.fireAppearanceChanged();
        }
    }
    
    /**
     * Gets the color which will be drawn with a zero opacity in the Jump map
     *@return color that will be replaced with transparent pixels
     */
    public Color getTransparentColor() {
        return transparentColor;
    }
    
    /**
     * for java2xml
     */
    public String getTransparentColorAsString() {
    	if (this.getTransparentColor()==null)
    		return "null";
    	
        String hexColor = Integer.toHexString(this.getTransparentColor().getRGB());
        if (hexColor.length()>6){
        	hexColor = hexColor.substring(2);
        }
        return hexColor;
    }
    
    /**
     * for java2xml
     */
    public void setTransparentColorAsString(String hexColorString) {
    	if (hexColorString==null || hexColorString.equals("null")){
    		return;
    	}
    	
    	int rgb = Integer.parseInt(hexColorString.toUpperCase(), 16);
    	
    	Color tColor = new Color(rgb);

    	this.setTransparentColor(tColor);
    }
    
    /**
     * Sets the color which will be drawn with a zero opacity in the Jump map
     *@param transparentColor the color for transparency
     */
    public void setTransparentColor(Color transparentColor) {
        if (this.transparentColor != transparentColor && (this.transparentColor==null || !this.transparentColor.equals(transparentColor))){
         
            this.transparentColor = transparentColor;
            
            this.forceTotalRepaint();
            
            if (this.isFiringAppearanceEvents()) 
                this.fireAppearanceChanged();
        }
    }
    
    /**
     * After this method was invoked, the image will be
     * completely re-rendered (not using caches) the next time.
     */
    protected void forceTotalRepaint(){
        this.visibleEnv = null;
        this.setImageProcessingMode(RasterImageLayer.MODE_NONE);
    }

    /**
     *@return the current offset (to the viewport's <code>(0,0)</code>) in x direction 
     */
    public int getXOffset() {
        return xOffset;
    }

    /**
     *@return the current offset (to the viewport's <code>(0,0)</code>) in y direction 
     */
    public int getYOffset() {
        return yOffset;
    }
    
    public static double getFreeRamFactor() {
        return freeRamFactor;
    }

    public static void setFreeRamFactor(double freeRamFactor) {
        //logger.printDebug("setting freeRamFactor to " + freeRamFactor);
        RasterImageLayer.freeRamFactor = freeRamFactor;
        RasterImageLayer.minRamToKeepFree = RasterImageLayer.availRAM * RasterImageLayer.freeRamFactor;
        RasterImageLayer.maxPixelsForFastDisplayMode = (int)((RasterImageLayer.availRAM - RasterImageLayer.minRamToKeepFree)/(1024*1024) * 3000);
        //logger.printDebug("maxPixelsForFastDisplayMode: " + maxPixelsForFastDisplayMode);
    }


    public static long getAvailRAM() {
        return availRAM;
    }

    public static int getMaxPixelsForFastDisplayMode() {
        return maxPixelsForFastDisplayMode;
    }

    public static double getMinRamToKeepFree() {
        return minRamToKeepFree;
    }

    /**
     * Sets the image's files name (if image is not to be keeped) - this needs to be set!
     *@param imageFileName the file name of the image
     */
    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
        this.setNeedToKeepImage(false);
    }
    
    
    /**
     * 
     *@return the file name of the image represented by this instance of the <code>RasterImageLayer</code>
     */
    public String getImageFileName() {
        return imageFileName;
    }

    /**
     * check, if image will be keeped in RAM or if it will be reloaded from a file
     * if needed
     *@return true if image will be keeped in RAM, else false
     */
    public boolean isNeedToKeepImage() {
        return needToKeepImage;
    }
    /**
     * toogle, if image will be keeped in RAM or if it will be reloaded from a file
     * if needed
     *@param needToKeepImage true if image is supposed be keeped in RAM, else false
     */
    public void setNeedToKeepImage(boolean needToKeepImage) {
        this.needToKeepImage = needToKeepImage;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        this.flushImages(true);
    }

    /**
     *@return the height of the source image
     */
    public int getOrigImageHeight() {
        return origImageHeight;
    }

    /**
     *@return the width of the source image
     */
    public int getOrigImageWidth() {
        return origImageWidth;
    }

    
    /**
     * for java2xml
     *@param origImageHeight
     */
    public void setOrigImageHeight(int origImageHeight) {
        this.origImageHeight = origImageHeight;
    }

    /**
     * for java2xml
     *@param origImageWidth
     */
    public void setOrigImageWidth(int origImageWidth) {
        this.origImageWidth = origImageWidth;
    }

    /**
     * shows or hides the image in the Jump map
     *@param visible
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        if (!visible)
            this.clearImage(true);
        
        if (this.isFiringAppearanceEvents())
            this.fireAppearanceChanged();
    }
    

    /**
     * @see #firingAppearanceEvents
     *@return true if appearance events are fired automatically, false if not
     */
    public boolean isFiringAppearanceEvents() {
        return firingAppearanceEvents;
    }

    /**
     * @see #firingAppearanceEvents
     *@param firingAppearanceEvents true if appearance events are to be fired automatically, false if not
     */
    public void setFiringAppearanceEvents(boolean firingAppearanceEvents) {
        this.firingAppearanceEvents = firingAppearanceEvents;
    }    
    
    // Metainformation stuff
    
    protected MetaDataMap metaInformation = null;
    
    public MetaDataMap getMetaInformation() {
        return metaInformation;
    }

    public void setMetaInformation(MetaDataMap metaInformation) {
        this.metaInformation = metaInformation;
    }
    
}
