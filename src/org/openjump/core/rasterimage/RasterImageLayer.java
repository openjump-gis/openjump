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
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import javax.media.jai.JAI;

import org.openjump.util.metaData.MetaDataMap;
import org.openjump.util.metaData.ObjectContainingMetaInformation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.AbstractLayerable;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
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
public final class RasterImageLayer extends AbstractLayerable implements ObjectContainingMetaInformation {
    
    protected static Blackboard blackboard = null;
    
    protected final static String BLACKBOARD_KEY_PLUGINCONTEXT = PlugInContext.class.getName();
    protected final static String BLACKBOARD_KEY_WORKBENCHCONTEXT = PlugInContext.class.getName();
    
    protected int lastImgProcessingMode = 0;
    
    protected final static int MODE_NONE = 0;
    protected final static int MODE_SCALINGFIRST = 1;
    protected final static int MODE_CLIPPINGFIRST = 2;
    protected final static int MODE_FASTDISPLAY = 3;
    
    protected Rectangle imagePart, visibleRect = null;
    
    protected double oldScaleXImg2Canvas;
    
    protected int xOffset, yOffset;
    
    //protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
    
    protected double transparencyLevel = .0f;

    
    protected static long availRAM = Runtime.getRuntime().maxMemory();
    protected static double freeRamFactor = 0.5;
    protected static double minRamToKeepFree = availRAM * freeRamFactor;
    //[sstein 9.Aug.2010]
    // The value below is set dynamically based on available memory
    //       now its 200x200px as min (originally it was 500x500)
    //protected static int maxPixelsForFastDisplayMode = 40000;
    protected static int maxPixelsForFastDisplayMode = 250000;

    protected String imageFileName = null;
    protected int origImageWidth, origImageHeight;
    protected boolean imageSet = false;
    protected BufferedImage image = null;
    protected int numBands = 0;
    
    //-- [sstein 2nd Aug 2010] new, since we scale the image now for display
    protected Raster actualRasterData = null;
    protected boolean rasterDataChanged = false; //may be needed for rescaling the image values
    protected boolean wasScaledForDisplay = false;
    //-- end
    
    protected BufferedImage imageProcessingStep1 = null, imageProcessingStep2 = null;

    
    protected Envelope actualImageEnvelope = null, visibleEnv = null, oldVisibleEnv;
    protected Envelope originalImageEnvelope = null;
    
    /**
     * Flag to decide, if events are fired automatically, if the appearance (imageEnvelope, etc.) changes.<br>
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

    //-- [sstein 26 June 2013] new as with ASCII grid imports nodata values can be defined
    protected double noDataValue = Double.NaN;
    protected double originalCellSize;
    protected double actualCellSize;
    //-- end

    private Metadata metadata;
    
    private Stats stats;
    
    private RasterSymbology symbology = null;
    private boolean symbologyChanged = false;
    private final UUID uuid = java.util.UUID.randomUUID();
    
    /**
     * for java2xml
     */
    public RasterImageLayer() {
        super();
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
    }
    
    /**
     * for java2xml
     *@return the Envelope as string
     */
    public String getXmlEnvelope(){
        return this.originalImageEnvelope.toString();
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
        
        this.setWholeImageEnvelope( new Envelope( minX, maxX, minY, maxY ) );
    }    

    /**
     *@param name name of the layer
     *@param layerManager
     * @param imageFileName
     *@param imageToDisplay the image (if already loaded) or null
     * @param wholeImageEnvelope
     */
    public RasterImageLayer(String name, LayerManager layerManager, String imageFileName, BufferedImage imageToDisplay, Envelope wholeImageEnvelope) {
        super(name, layerManager);
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
        this.imageFileName = imageFileName;
        this.originalImageEnvelope = wholeImageEnvelope;
        
        if (imageToDisplay != null)
            this.setImage(imageToDisplay);
//        if (newRaster != null)
//        	this.setRasterData(newRaster);
        //[sstein 9.Aug.2010]       
        long avram = getAvailRAM();
        if(avram > 256000000){
        	maxPixelsForFastDisplayMode = 250000; //500x500 px
        }
        if(avram > 750000000){
        	maxPixelsForFastDisplayMode = 4000000; //2000x2000 px
        }
        //[sstein end]
    }
    
    
    /**
     * Constructor to be used in case the image was not loaded from a file, so there is
     * no file name, but an image
     * 
     *@param name name of the layer
     *@param layerManager
     *@param imageToDisplay the image (if already loaded) or null
     *@param newRaster the raster (if already loaded) or null
     *@param envelope real-world coordinates of the image
     */
    public RasterImageLayer(String name, LayerManager layerManager, BufferedImage imageToDisplay, Raster newRaster, Envelope wholeImageEnvelope) {
        super(name, layerManager);
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
        this.setNeedToKeepImage(true);
        this.originalImageEnvelope = wholeImageEnvelope;
        
        if (imageToDisplay != null)
            this.setImage(imageToDisplay);
        else{
            //logger.printError("given image is NULL");
        }
        if (newRaster != null) {
//        	this.setRasterData(newRaster);
        }else{
            //logger.printError("given raster is NULL");
        }
        //[sstein 9.Aug.2010]
        long avram = getAvailRAM();
        if(avram > 256000000){
        	maxPixelsForFastDisplayMode = 250000; //500x500 px
        }
        if(avram > 750000000){
        	maxPixelsForFastDisplayMode = 563500; //750x750 px
        }
        //[sstein end]
    }

    @Override
    public Blackboard getBlackboard() {
        if (RasterImageLayer.blackboard == null)
            RasterImageLayer.blackboard = new Blackboard();
        
        return RasterImageLayer.blackboard;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        RasterImageLayer raster = null;
        if (this.isNeedToKeepImage()) {
            try {
                raster = new RasterImageLayer(getName(), getLayerManager(), getImageForDisplay(), getRasterData(null), new Envelope(getWholeImageEnvelope()));
            } catch (IOException ex) {
                Logger.error(ex);
            } catch (NoninvertibleTransformException ex) {
                Logger.error(ex);
            } catch (Exception ex) {
                Logger.error(ex);
            }
        } else {
            try {
                raster = new RasterImageLayer(getName(), getLayerManager(), getImageFileName(), getImageForDisplay(), new Envelope(getWholeImageEnvelope()));
            } catch (IOException ex) {
                Logger.error(ex);
            } catch (NoninvertibleTransformException ex) {
                Logger.error(ex);
            } catch (Exception ex) {
                Logger.error(ex);
            }
        }
        // clone must produce a layerable with the same name (as for Layer) not a unique name
        if (raster != null) {
            raster.getLayerManager().setFiringEvents(false);
            raster.setName(getName());
            raster.getLayerManager().setFiringEvents(true);
        }
        return raster;
    }
    
    /**
     * apply a scale operation to the image and return the
     * new image.
     */
    protected BufferedImage scaleImage(BufferedImage im, float xScale, float yScale) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(xScale);
        pb.add(yScale);

        return JAI.create("Scale", pb, null).getAsBufferedImage();
    }
    
    
    
    protected BufferedImage createOneColorImage(double scaleXImg2Canvas, double scaleYImg2Canvas){
        //logger.printDebug("fixing 1px scale: scaleXImg2Canvas = " + scaleXImg2Canvas + ", scaleYImg2Canvas = " + scaleYImg2Canvas);
        //logger.printDebug("this.imageProcessingStep1: " + this.imageProcessingStep1 .getWidth() + ", " + this.imageProcessingStep1.getHeight());
        
        scaleXImg2Canvas = Math.min( Math.abs(scaleXImg2Canvas), Math.abs(visibleRect.width) );
        scaleYImg2Canvas = Math.min( Math.abs(scaleYImg2Canvas), Math.abs(visibleRect.height) );
        
        //logger.printDebug("fixed 1px scale: scaleXImg2Canvas = " + scaleXImg2Canvas + ", scaleYImg2Canvas = " + scaleYImg2Canvas);
        
        BufferedImage bim = new BufferedImage(visibleRect.width, visibleRect.height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D grfcs = bim.createGraphics();
        
        grfcs.setColor(new Color(this.imageProcessingStep1.getRGB(0,0)));
        
        //logger.printDebug("color: " + new Color(this.imageProcessingStep1.getAsBufferedImage().getRGB(0,0)).toString());
        
        grfcs.fillRect( 0, 0, bim.getWidth(), bim.getHeight() );
        
        grfcs.dispose();
        
        return bim;
        
        //return PlanarImage.wrapRenderedImage(bim);
    }
    
    /**
     * Creates the image to draw
     * @param layerViewPanel
     * @return 
     */
    public BufferedImage createImage(LayerViewPanel layerViewPanel) {
        
        Viewport viewport = layerViewPanel.getViewport();
        
        if (!this.isVisible() || this.transparencyLevel >= 1.0){
            this.setImageProcessingMode(RasterImageLayer.MODE_NONE);
            this.clearImageAndRaster(true);
            //logger.printDebug("!visible");
            return null;
        }
        
        BufferedImage imageToDraw = null;

        try {
            
            //GeoTIFFRaster grr = new GeoTIFFRaster((new File(imageFileName)).toURI().toString());
            
            java.awt.Point imageDims = RasterImageIO.getImageDimensions(imageFileName);
            
            origImageWidth = imageDims.x;
            origImageHeight = imageDims.y;            
            visibleRect = viewport.getPanel().getVisibleRect();
            
            int visibleX1 = visibleRect.x;
            int visibleY1 = visibleRect.y;
            int visibleX2 = visibleX1 + visibleRect.width;
            int visibleY2 = visibleY1 + visibleRect.height;
            
            Coordinate upperLeftVisible = viewport.toModelCoordinate(nullpunkt);
            Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(visibleX2, visibleY2));
            
            Envelope newVisibleEnv = new Envelope(upperLeftVisible.x, lowerRightVisible.x, upperLeftVisible.y, lowerRightVisible.y);
            
            setImageSet(false);
            
            if (visibleEnv == null || visibleEnv.getMinX() != newVisibleEnv.getMinX() || visibleEnv.getMaxX() != newVisibleEnv.getMaxX() || 
                    visibleEnv.getMinY() != newVisibleEnv.getMinY() || visibleEnv.getMaxY() != newVisibleEnv.getMaxY() || symbologyChanged){
                visibleEnv = newVisibleEnv;
                    
                symbologyChanged = false;
                
                reLoadImage();
                if(image == null) {
                    return null;
                }                    
                
                Point2D upperLeftCornerOfImage = viewport.toViewPoint(new Coordinate(getActualImageEnvelope().getMinX(), getActualImageEnvelope().getMaxY()));
                Point2D lowerRightCornerOfImage = viewport.toViewPoint(new Coordinate(getActualImageEnvelope().getMaxX(), getActualImageEnvelope().getMinY()));

                double scaledWidth = lowerRightCornerOfImage.getX() - upperLeftCornerOfImage.getX();
                double scaledHeight = upperLeftCornerOfImage.getY() - lowerRightCornerOfImage.getY();

                long totalMem = Runtime.getRuntime().totalMemory();
                long freeMem = Runtime.getRuntime().freeMemory();
                long committedMemory = totalMem - freeMem;
                double maxMemoryToCommit = availRAM - minRamToKeepFree;
                boolean needFreeRAM = (committedMemory > maxMemoryToCommit);
                if(needFreeRAM == false){

                    setImage(stretchImageValuesForDisplay());
                    wasScaledForDisplay = true;

                    setNeedToKeepImage(true); //so small images are not reloaded every time
                }

                //[sstein end]                    
                imagePart = getVisibleImageCoordinatesOfImage( image.getWidth(), image.getHeight(), visibleEnv, getActualImageEnvelope() );

                double scaleXImg2Canvas = scaledWidth / image.getWidth();
                double scaleYImg2Canvas = scaledHeight / image.getHeight();

                if (imageProcessingStep2 == null || scaleXImg2Canvas != oldScaleXImg2Canvas ||
                        !RasterImageLayer.tilesAreNotNullAndCongruent( visibleEnv, oldVisibleEnv)){

                    imageProcessingStep1 = getVisiblePartOfTheImage( getImageForDisplay(), imagePart );

                    if ( imageProcessingStep1 != null) {
                        // avoid an 1 pixel by 1 pixel image to get scaled to thousands by thousands pixels causing an out of memory error
                        if (imagePart.width == 1 || imagePart.height == 1){
                            xOffset = 0;
                            yOffset = 0;
                            imageProcessingStep2 = createOneColorImage(scaleXImg2Canvas, scaleYImg2Canvas);
                        } else {
                            imageProcessingStep2 = getScaledImageMatchingVisible( imageProcessingStep1, scaleXImg2Canvas, scaleYImg2Canvas );
                        }

                    } else {
                        return null;
                    }

                    if (transparentColor!=null) transparencyColorNeedsToBeApplied = true;

                    imageProcessingStep1 = null;

                    xOffset = (int)(xOffset *scaleXImg2Canvas);
                    yOffset = (int)(yOffset *(-scaleYImg2Canvas));

                    oldScaleXImg2Canvas = scaleXImg2Canvas;

                    oldVisibleEnv = visibleEnv;
                }
    
                               
            }
            
            if (imageProcessingStep2 != null && transparencyColorNeedsToBeApplied ){
                imageToDraw = setupTransparency(imageProcessingStep2);
            } else if (imageProcessingStep2 != null) {
                imageToDraw = imageProcessingStep2;
            }
            

        } catch (Exception e){
            //logger.printError(e.getMessage());
            e.printStackTrace();
        }

        if (Runtime.getRuntime().freeMemory() < RasterImageLayer.getMinRamToKeepFree()){
            clearImageAndRaster(true);
        }
        
        if (imageToDraw != null) {
            return imageToDraw;
        } else if (imageProcessingStep2!=null) {
            return imageProcessingStep2;
        }
        
        
        return null;
    }
    
    /**
     * deletes image from RAM (if it is not to be kept and if the RAM consumption is high)
     * and calls the garbage collector, if the <code>garbageCollect</code> is true.
     *@param garbageCollect if true the garbage collector will be called (this parameter may be overridden, if there is not enough RAM available...)
     */
    public boolean clearImageAndRaster(boolean garbageCollect){
    	//TODO: [sstein 9.Aug.2010] not sure if below condition is correct, since it 
    	//       does not account for Xmx (max memory), only for the actual memory
    	//       Hence we should work with committed memory as I did above??? 
        boolean reallyNeedToFreeRAM = (Runtime.getRuntime().freeMemory() < minRamToKeepFree);
        if (!needToKeepImage && reallyNeedToFreeRAM ){
            //this.image = null;
            //rasterData = null; //[sstein 2Aug2010] line added
            wasScaledForDisplay = false; //[sstein 20Aug2010] line added
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
        if (image!=null)
            image.flush();
        image = null;
        
        if (imageProcessingStep1!=null)
            imageProcessingStep1.flush();
        imageProcessingStep1 = null;
        
        if (imageProcessingStep2!=null)
            imageProcessingStep2.flush();
        imageProcessingStep2 = null;
    
        if (garbageCollect){
            Runtime.getRuntime().gc();
        }
    }
    
    public void reLoadImage() throws IOException, NoninvertibleTransformException, FileNotFoundException, TiffTags.TiffReadingException, Exception{
        
        //if (image == null && !needToKeepImage){
        
        RasterImageIO rasterImageIO = new RasterImageIO();

        Viewport viewport = getWorkbenchContext().getLayerViewPanel().getViewport();
        if(!viewport.getEnvelopeInModelCoordinates().intersects(originalImageEnvelope) &&
                getWorkbenchContext().getLayerManager().getLayerables(Layerable.class).isEmpty()) {
            viewport.zoom(originalImageEnvelope);
        }
        
//        Rectangle visibleRect = viewport.getPanel().getVisibleRect();
            
        Resolution requestedRes = RasterImageIO.calcRequestedResolution(viewport);
        
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(getWorkbenchContext(), imageFileName, stats, viewport.getEnvelopeInModelCoordinates(), requestedRes);
        metadata = imageAndMetadata.getMetadata();
        image = imageAndMetadata.getImage();
        numBands = metadata.getStats().getBandCount();
        noDataValue = imageAndMetadata.getMetadata().getNoDataValue();
        stats = imageAndMetadata.getMetadata().getStats();
        originalImageEnvelope = imageAndMetadata.getMetadata().getOriginalImageEnvelope();
        actualImageEnvelope = imageAndMetadata.getMetadata().getActualEnvelope();
        originalCellSize = imageAndMetadata.getMetadata().getOriginalCellSize();        
        actualCellSize = imageAndMetadata.getMetadata().getActualCellSize();
        
        if(image != null) {
            setImage(image);
        }
        wasScaledForDisplay = false;
        
        if(image != null) {
            actualRasterData = image.copyData(null);      
        } else {
            actualRasterData = null;
        }
    }
    
    /**
     * use this to assign the raster data again
     * the method is called from  getRasterData();
     */
    public void reLoadImageButKeepImageForDisplay() throws IOException,
            NoninvertibleTransformException, FileNotFoundException, TiffTags.TiffReadingException, Exception{
       WorkbenchContext context = getWorkbenchContext();
       BufferedImage pi = getImageForDisplay();
       //[sstein 24.Sept.2010] commented out:
       //PlanarImage dontNeedThisImage = RasterImageLayer.loadImage( context, imageFileName); //causes error for .clone()
       this.setImage(pi);
    }
    
    protected BufferedImage stretchImageValuesForDisplay() throws NoninvertibleTransformException{
        
        int width = actualRasterData.getWidth();
        int height = actualRasterData.getHeight();  
        
        // Need to change image type to support transparency and apply symbology
        if(image.getColorModel() instanceof IndexColorModel) {
            return image;  
        }
        
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {

                if(symbology == null) {
                    if(stats.getBandCount() < 3) {
                        
                        RasterSymbology rasterSymbology = new RasterSymbology(RasterSymbology.TYPE_RAMP);
                        rasterSymbology.addColorMapEntry(metadata.getNoDataValue(), transparentColor);
                        rasterSymbology.addColorMapEntry(metadata.getStats().getMin(0), Color.WHITE);
                        rasterSymbology.addColorMapEntry(metadata.getStats().getMax(0), Color.BLACK);
                        setSymbology(rasterSymbology);
                        
                    } else {
                        double valueR = actualRasterData.getSampleDouble(col, row, 0);
                        double valueG = actualRasterData.getSampleDouble(col, row, 1);
                        double valueB = actualRasterData.getSampleDouble(col, row, 2);
                        double valueAlpha = 255;
                        if(stats.getBandCount() > 3) {
                            valueAlpha = actualRasterData.getSampleDouble(col, row, 3);
                        }
                        if(Double.isNaN(valueR) || Double.isInfinite(valueR) || valueR == noDataValue
                                || Double.isNaN(valueG) || Double.isInfinite(valueG) || valueG == noDataValue
                                || Double.isNaN(valueB) || Double.isInfinite(valueB) || valueB == noDataValue
                                || valueAlpha <= 0) {
                            newImage.setRGB(col, row, Color.TRANSLUCENT);
                            continue;
                        }
                        
                        int r = (int) ((valueR - stats.getMin(0)) * 255./(stats.getMax(0) - stats.getMin(0)));
                        if(r > 255) r = 255;
                        if(r < 0) r = 0;
                        int g = (int) ((valueG - stats.getMin(1)) * 255./(stats.getMax(1) - stats.getMin(0)));
                        if(g > 255) g = 255;
                        if(g < 0) g = 0;
                        int b = (int) ((valueB - stats.getMin(2)) * 255./(stats.getMax(2) - stats.getMin(0)));
                        if(b > 255) b = 255;
                        if(b < 0) b = 0;

                        int alpha = (int) valueAlpha;
                        
                        newImage.setRGB(col, row, new Color(r, g, b, alpha).getRGB());
                    }
                } else {
                    // Symbology exists
                    double value = actualRasterData.getSampleDouble(col, row, 0);
                    
                    /**
                     * If symbology min value is higher than raster min value
                     * the value becomes equal to the symbology min value
                     */
                    
                    Double[] symbologyClassLimits =  symbology.getColorMapEntries_tm().keySet().toArray(new Double[symbology.getColorMapEntries_tm().keySet().size()]);
                    double symbMinValue = symbologyClassLimits[0];
                    double symbFirstValue = symbologyClassLimits[0];
                    if(this.isNoData(symbFirstValue)) {
                        symbMinValue = symbologyClassLimits[1];
                    }
                    
                    if(!this.isNoData(value) && value < symbMinValue) {
                        value = symbMinValue;
                    }
                    
                    Color color = symbology.getColor(value);
                    
                    if((Double.isNaN(value) || Double.isInfinite(value) || this.isNoData(value))
                            && color == null) {
                        newImage.setRGB(col, row, Color.TRANSLUCENT);
                        continue;
                    }
                    
                    // Transparency is a combination of total layer transparency
                    // and single cell transparency
                    int transparency = 
                            (int)(((1 - symbology.getTransparency()) * 
                            (color.getAlpha() / 255d)) * 255);
                    newImage.setRGB(col, row, new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            transparency).getRGB());
                }  
            }
        }

        return newImage;
    }
    
    /**
     * @return Envelope with the real world coordinates of the image
     */
    public Envelope getWholeImageEnvelope() {
        return originalImageEnvelope;
    }
    
    public Envelope getActualImageEnvelope() {
        return actualImageEnvelope;
    }
    
    /**
     * Sets the Envelope object containing the real world coordinates (e.g. WGS84) of the image - this needs to be set (if it wasn't given to the constructor)!
     *@param envelope the Envelope
     */
    private void setWholeImageEnvelope(Envelope envelope) {
        originalImageEnvelope = envelope;
        
        forceTotalRepaint();
        
        if (this.isFiringAppearanceEvents())
            this.fireAppearanceChanged();
    }
    
    private void setActualImageEnvelope(Envelope envelope) {
        actualImageEnvelope = envelope;
        
        forceTotalRepaint();
        
        if (isFiringAppearanceEvents())
            fireAppearanceChanged();
    }
    
    /**
     * for java2xml
     *@return the Envelope as string
     */
    public String getXmlWholeImageEnvelope(){
        return this.originalImageEnvelope.toString();
    }
    
    public String getXmlActualImageEnvelope() {
        return this.actualImageEnvelope.toString();
    }
    
    /**
     * for java2xml
     *@param envStr the Envelope as string
     */
    public void setXmlWholeImageEnvelope(String envStr){
        String coords = envStr.substring(envStr.indexOf("[")+1, envStr.indexOf("]"));
        
        String[] coordArray = coords.split(",");
        
        String[] xCoords = coordArray[0].split(":");
        String[] yCoords = coordArray[1].split(":");
        
        double minX = java.lang.Double.parseDouble(xCoords[0]);
        double maxX = java.lang.Double.parseDouble(xCoords[1]);
        
        double minY = java.lang.Double.parseDouble(yCoords[0]);
        double maxY = java.lang.Double.parseDouble(yCoords[1]);
        
        this.setWholeImageEnvelope( new Envelope( minX, maxX, minY, maxY ) );
    }
    
    public void setXmlActualImageEnvelope(String envStr){
        String coords = envStr.substring(envStr.indexOf("[")+1, envStr.indexOf("]"));
        
        String[] coordArray = coords.split(",");
        
        String[] xCoords = coordArray[0].split(":");
        String[] yCoords = coordArray[1].split(":");
        
        double minX = java.lang.Double.parseDouble(xCoords[0]);
        double maxX = java.lang.Double.parseDouble(xCoords[1]);
        
        double minY = java.lang.Double.parseDouble(yCoords[0]);
        double maxY = java.lang.Double.parseDouble(yCoords[1]);
        
        this.setActualImageEnvelope( new Envelope( minX, maxX, minY, maxY ) );
    }
    
    /**
     * Method to change the coordinates of the image and later apply the
     * changes to the RasterImageLayer by using {@link RasterImageLayer#setGeometryAsEnvelope(Geometry)}.
     *@return return the imageEnvelope (= bounding box) as a geometry, 
     */
    public Polygon getWholeImageEnvelopeAsGeometry(){
        Coordinate[] coordinates = new Coordinate[5];
        
        coordinates[0] = new Coordinate(originalImageEnvelope.getMinX(), originalImageEnvelope.getMaxY());
        coordinates[1] = new Coordinate(originalImageEnvelope.getMaxX(), originalImageEnvelope.getMaxY());
        coordinates[2] = new Coordinate(originalImageEnvelope.getMaxX(), originalImageEnvelope.getMinY());
        coordinates[3] = new Coordinate(originalImageEnvelope.getMinX(), originalImageEnvelope.getMinY());
        coordinates[4] = new Coordinate(originalImageEnvelope.getMinX(), originalImageEnvelope.getMaxY());
        
        GeometryFactory gf = new GeometryFactory();
        
        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }
    
    public Polygon getActualImageEnvelopeAsGeometry(){
        Coordinate[] coordinates = new Coordinate[5];
        
        coordinates[0] = new Coordinate(actualImageEnvelope.getMinX(), actualImageEnvelope.getMaxY());
        coordinates[1] = new Coordinate(actualImageEnvelope.getMaxX(), actualImageEnvelope.getMaxY());
        coordinates[2] = new Coordinate(actualImageEnvelope.getMaxX(), actualImageEnvelope.getMinY());
        coordinates[3] = new Coordinate(actualImageEnvelope.getMinX(), actualImageEnvelope.getMinY());
        coordinates[4] = new Coordinate(actualImageEnvelope.getMinX(), actualImageEnvelope.getMaxY());
        
        GeometryFactory gf = new GeometryFactory();
        
        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }
    
    /**
     * Method to set the coordinates of the image, e.g. after changing them after using {@link RasterImageLayer#getEnvelopeAsGeometry()}.
     */
    public void setGeometryAsWholeImageEnvelope(Geometry geometry){
        setWholeImageEnvelope(geometry.getEnvelopeInternal());
    }
    
    public void setGeometryAsActualImageEnvelope(Geometry geometry){
        setActualImageEnvelope(geometry.getEnvelopeInternal());
    }
    
    /**
     * Add transparency to the image (more exactly: to each pixel which a color == this.transparentColor)
     *@param pImage the image
     */
    private BufferedImage setupTransparency(BufferedImage bim){
        //BufferedImage bim = pImage.getAsBufferedImage();
        
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
                    Color color = new Color(bim.getRGB(w, h));
                    
                    argb[0] = fullTransparencyAlpha;
                    argb[1] = color.getRed();
                    argb[2] = color.getGreen();
                    argb[3] = color.getBlue();
                    
                    bim.setRGB(w,h,1,1,argb,0,1);
                }
            }
        }
        
        return bim;
    }
    
    private void setImageProcessingMode( int nr ){
        if (lastImgProcessingMode != nr){
            if (imageProcessingStep1!=null)
                imageProcessingStep1.flush();
            imageProcessingStep1 = null;
            
            if (imageProcessingStep2!=null)
                imageProcessingStep2.flush();
            imageProcessingStep2 = null;

            imagePart = null;
            
            oldScaleXImg2Canvas = -1;
            
            if (Runtime.getRuntime().freeMemory() < RasterImageLayer.getMinRamToKeepFree()){
                Runtime.getRuntime().gc();
            }
            
            lastImgProcessingMode = nr;
        }
    }
    

    private static boolean tilesAreNotNullAndCongruent( Envelope oldVisibleEnv, Envelope newVisibleEnv ){

        if (oldVisibleEnv == null || newVisibleEnv == null) return true;
        return (oldVisibleEnv.getMinX() == newVisibleEnv.getMinX() &&
                oldVisibleEnv.getMaxX() == newVisibleEnv.getMaxX() &&
                oldVisibleEnv.getMinY() == newVisibleEnv.getMinY() &&
                oldVisibleEnv.getMaxY() == newVisibleEnv.getMaxY());
        
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
    
    protected BufferedImage getScaledImageMatchingVisible( BufferedImage toBeScaled, double XscaleImg2Canvas, double YscaleImg2Canvas ){

        if (toBeScaled==null) return null;
        
        int scaledWidth = (int)(toBeScaled.getWidth() * XscaleImg2Canvas);
        int scaledHeight = (int)(toBeScaled.getHeight() * Math.abs(YscaleImg2Canvas) );
        
        if (scaledWidth<=0 || scaledHeight<=0) return null;
        
        return scaleImage(toBeScaled, (float)XscaleImg2Canvas, (float)Math.abs(YscaleImg2Canvas) );

    }
    
    public BufferedImage getTileAsImage( Envelope wantedEnvelope ) throws IOException, NoninvertibleTransformException, FileNotFoundException, TiffTags.TiffReadingException, Exception{
        double imgWidth = image.getWidth();
        double imgHeight = image.getHeight();
        Envelope imageEnv = getActualImageEnvelope();
        
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
        
        if (image==null){
            reLoadImage();
        }
        
        BufferedImage imgTile = this.image.getSubimage(xOffset, yOffset, width, height);
        
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
        
        this.clearImageAndRaster(false);
        
        return result;
    }
    
//    public Raster getTileAsRaster( Envelope wantedEnvelope ) throws IOException{
//
//        double imgWidth = origImageWidth;
//        double imgHeight = origImageHeight;
//        Envelope imageEnv = originalImageEnvelope;
//        
//        double minVisibleX = Math.max(wantedEnvelope.getMinX(), imageEnv.getMinX());
//        double minVisibleY = Math.max(wantedEnvelope.getMinY(), imageEnv.getMinY());
//        
//        double maxVisibleX = Math.min(wantedEnvelope.getMaxX(), imageEnv.getMaxX());
//        double maxVisibleY = Math.min(wantedEnvelope.getMaxY(), imageEnv.getMaxY());
//        
//        double offset2VisibleX = imageEnv.getMinX() - wantedEnvelope.getMinX();
//        double offset2VisibleY = wantedEnvelope.getMaxY() - imageEnv.getMaxY();
//        
//        double scaleX = imgWidth / imageEnv.getWidth();
//        double scaleY = imgHeight / imageEnv.getHeight();
//        
//        // use local variables!
//        int xOffset, yOffset, width, height;
//        
//        if (offset2VisibleX >= 0){
//            xOffset = 0;
//        } else {
//            xOffset = (int)(-offset2VisibleX * scaleX);
//        }
//        
//        if (offset2VisibleY >= 0){
//            yOffset = 0;
//        } else {
//            yOffset = (int)(-offset2VisibleY * scaleY);
//        }
//        
//        width = (int)((maxVisibleX-minVisibleX) * scaleX);
//        height =  (int)((maxVisibleY-minVisibleY) * scaleY);
//        
//        if (width < imgWidth && height < imgHeight){ 
//            width += 1;
//            height += 1;
//        }
//        
//        
//        if (width <= 0 || height <= 0) return null;
//        
//        int wantedWidth = (int)(wantedEnvelope.getWidth() * scaleX);
//        int wantedHeight = (int)(wantedEnvelope.getHeight() * scaleY);
//        
//        Rectangle subset = new Rectangle(xOffset, yOffset, wantedWidth, wantedHeight);
//        Raster rasterData = RasterImageIO.loadRasterData(imageFileName, subset);
//        return rasterData;
//        
//        
////        ColorModel colorModel = PlanarImage.createColorModel(rasterData.getSampleModel());
////        BufferedImage bufimg = new BufferedImage(colorModel, (WritableRaster) rasterData, false, null);
////        PlanarImage pimage = PlanarImage.wrapRenderedImage(bufimg);
////        
////        BufferedImage imgTile = pimage.getAsBufferedImage( new Rectangle(xOffset, yOffset, width, height), pimage.getColorModel());
////        
////        BufferedImage result = new BufferedImage( wantedWidth, wantedHeight, BufferedImage.TYPE_INT_ARGB);
////        
////        Graphics2D graf = result.createGraphics();
////        
////        Color backgroundColor = (this.transparentColor!=null)?this.transparentColor:Color.white;
////        
////        graf.fillRect(0,0,wantedWidth, wantedHeight);
////        
////        int xTileOffset,yTileOffset;
////        
////        if (xOffset > 0){
////            xTileOffset = 0;
////        } else {
////            xTileOffset = (int)(offset2VisibleX * scaleX);
////        }
////        
////        if (yOffset > 0){
////            yTileOffset = 0;
////        } else {
////            yTileOffset = (int)(offset2VisibleY * scaleY);
////        }
////        
////        graf.drawImage(imgTile, xTileOffset, yTileOffset, imgTile.getWidth(), imgTile.getHeight(), backgroundColor, null);
////        graf.dispose();
////        
////        this.clearImageAndRaster(false);
////        
////        return result.getData();
//    }
    
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
        
        Rectangle rect = getVisibleImageCoordinatesOfImage(imgWidth, imgHeight, newVisibleEnv, imageEnv);
        
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
            xOffset = 0;
        } else {
            xOffset = (int)(-offset2VisibleX * scaleX);
        }
        
        if (offset2VisibleY >= 0){
            yOffset = 0;
        } else {
            yOffset = (int)(-offset2VisibleY * scaleY);
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
        
        return new Rectangle(xOffset, yOffset, width, height);
    }
    
   
    protected Rectangle getVisibleImageCoordinatesOfImage( BufferedImage img, Envelope visible, Envelope imageEnv ){
        return this.getVisibleImageCoordinatesOfImage( img.getWidth(), img.getHeight(), visible, imageEnv );
    }
    
    protected BufferedImage getVisiblePartOfTheImage( BufferedImage img, Rectangle desiredImageArea ){
        if (desiredImageArea==null){
            return null;
        }
        if ( desiredImageArea.width > 0 && desiredImageArea.height > 0 ){
            if (desiredImageArea.width + desiredImageArea.x <= img.getWidth() &&
                    desiredImageArea.height + desiredImageArea.y <= img.getHeight() ) {
                
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(img);
                pb.add((float) desiredImageArea.x);
                pb.add((float) desiredImageArea.y);
                pb.add((float) desiredImageArea.width);
                pb.add((float) desiredImageArea.height);
                
                return JAI.create("crop", pb).getAsBufferedImage();
                
            }

            //return PlanarImage.wrapRenderedImage( img.getAsBufferedImage( new Rectangle(desiredImageArea.x, desiredImageArea.y, desiredImageArea.width, desiredImageArea.height), img.getColorModel() ));
            //logger.printWarning("desired area invalid: " + (desiredImageArea.width + desiredImageArea.x) + ", " + (desiredImageArea.height + desiredImageArea.y) + "; image dimensions: " + img.getWidth() + ", " + img.getHeight());
        } 
        return null;
    }

    public BufferedImage getImage() {
        return image;
    }
    
    
    /**
     * Sets the image that will be shown in the map (also sets some interally used flags)
     *@param image image that will be shown in the map
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        //origImageWidth = image.getWidth();
        //origImageHeight = image.getHeight();
        imageSet = true;
    }
    
    
    
    public void setImageSet(boolean imageSet) {
        this.imageSet = imageSet;
    }

    public boolean isImageNull(){
        return this.image == null;
    }
    
    /**
     * returns the image, this can be modified - i.e. is just a representation. 
     *@return the image
     */
    public BufferedImage getImageForDisplay() throws IOException, NoninvertibleTransformException, FileNotFoundException, TiffTags.TiffReadingException, Exception{
        if (image == null)
            reLoadImage();
        return image;
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
            this.clearImageAndRaster(true);
        
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
    
    @Override
    public MetaDataMap getMetaInformation() {
        return metaInformation;
    }

    @Override
    public void setMetaInformation(MetaDataMap metaInformation) {
        this.metaInformation = metaInformation;
    }

    public Raster getRasterData(Rectangle subset) throws IOException {
        
        Raster raster = RasterImageIO.loadRasterData(imageFileName, subset);        
        return raster;
        
    }
    
    public Rectangle getRectangleFromEnvelope(Envelope envelope) throws IOException {
        
        double imgWidth = origImageWidth;
        double imgHeight = origImageHeight;
        Envelope imageEnv = originalImageEnvelope;
        
        double minVisibleX = Math.max(envelope.getMinX(), imageEnv.getMinX());
        double minVisibleY = Math.max(envelope.getMinY(), imageEnv.getMinY());
        
        double maxVisibleX = Math.min(envelope.getMaxX(), imageEnv.getMaxX());
        double maxVisibleY = Math.min(envelope.getMaxY(), imageEnv.getMaxY());
        
        double offset2VisibleX = imageEnv.getMinX() - envelope.getMinX();
        double offset2VisibleY = envelope.getMaxY() - imageEnv.getMaxY();
        
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
        
        int wantedWidth = (int)(envelope.getWidth() * scaleX);
        int wantedHeight = (int)(envelope.getHeight() * scaleY);
        
        Rectangle subset = new Rectangle(xOffset, yOffset, wantedWidth, wantedHeight);
        return subset;
        
    }
    
//    /**
//     * to set the Raster data, use also setImage()
//     * @param newRaster
//     */
//    public void setRasterData(Raster newRaster) {
//            rasterData = newRaster;
//            setRasterDataChanged(true);
//    }

	
    /**
     * @return the rasterDataChanged
     */
    public boolean isRasterDataChanged() {
            return rasterDataChanged;
    }

    /**
     * @param rasterDataChanged the rasterDataChanged to set
     */
    public void setRasterDataChanged(boolean rasterDataChanged) {
            this.rasterDataChanged = rasterDataChanged;
    }
	
    /**
     * Default value is NaN. The value can be different when data have been read, 
     * for example, from (ESRI) ASCII grid images
     * @return value that is written when a cell does not contain data
     */
    public double getNoDataValue() {
        return noDataValue;
    }    
    
    /**
     * TODO: sstein test - 25.Sept.2013
     * @return
     */
    public int getNumBands(){
    	return numBands;
    }
    
    public void dispose() {
    // TODO: probably a good idea to remove resources when the layer is closed up
    // dunno what is needed to clean up Sextante though, hence leave it for now
    }
           
    public Double getCellValue(Coordinate coordinate, int band) throws RasterDataNotFoundException, IOException {
        
        return getCellValue(coordinate.x, coordinate.y, band);
        
    }
    
    public Double getCellValue(int col, int row, int band) throws RasterDataNotFoundException, IOException {

        int pos = row * origImageWidth + col;
        if(pos <0 || pos > origImageWidth * origImageHeight) return null;
                       
        return RasterImageIO.readCellValue(imageFileName, col, row, band);
        
    }

    public Double getCellValue(double coordX, double coordY, int band) throws RasterDataNotFoundException, IOException {
        
        double cellSizeX = (originalImageEnvelope.getMaxX() - originalImageEnvelope.getMinX()) / origImageWidth;
        double cellSizeY = (originalImageEnvelope.getMaxY() - originalImageEnvelope.getMinY()) / origImageHeight;
        
        int col = (int) Math.floor((coordX - originalImageEnvelope.getMinX()) / cellSizeX);
        int row = origImageHeight - (int) Math.floor((coordY - originalImageEnvelope.getMinY()) / cellSizeY) - 1;
        
        if(col <0 || col >= origImageWidth || row <0 || row >= origImageHeight) return null;
        
        int pos = row * origImageWidth + col;
        
        return RasterImageIO.readCellValue(imageFileName, col, row, band);
        
    }   
    
    public boolean isNoData(double value) {
        if(Double.isInfinite(noDataValue) && Double.isInfinite(value)) {
            return true;
        }
        if(Double.isNaN(noDataValue) && Double.isNaN(value)) {
            return true;
        }
        return(value == noDataValue || (float)value == (float)noDataValue);
    }

    public Metadata getMetadata() {
        return metadata;
    }
    
    public class RasterDataNotFoundException extends Exception {
    
    }
    
    public static Rectangle getViewportRectangle(WorkbenchContext workbenchContext) {
        
        Rectangle viewportRectangle = new Rectangle(
                0,
                0,
                workbenchContext.getLayerViewPanel().getVisibleRect().width,
                workbenchContext.getLayerViewPanel().getVisibleRect().height);
        return viewportRectangle;
                
    }
    
    public RasterSymbology getSymbology() {
        return symbology;
    }
    
    public void setSymbology(RasterSymbology symbology) throws NoninvertibleTransformException {
        this.symbology = symbology;
        symbologyChanged = true;
//        setImage(stretchImageValuesForDisplay());
        
//        forceTotalRepaint();
//        if (isFiringAppearanceEvents()) {
//            fireAppearanceChanged();
//        }
        imageProcessingStep2 = null;
        LayerViewPanel layerViewPanel = getWorkbenchContext().getLayerViewPanel();
        if(layerViewPanel != null) {
            layerViewPanel.getViewport().update();
        }
    }

    public Raster getActualRasterData() {
        return actualRasterData;
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
  
    /*
     *  Giuseppe Aruta 26/01/2015
     *  Some experimental boolean methods 
     */
    
    /**
     * Check if selected sextante raster layer is Temporary layer
     * Both layers in memory and layes stored into TEMP folder are considered
     * as "Temporary layers"
     */
    public boolean isTemporaryLayer() {
  	  if (imageFileName.contains(System.getProperty("java.io.tmpdir"))) {
  	          return true;
  	      } else{
  	    	  return false;
  	      }
  	  } 
   
 
   
    private final static String NODATASOURCELAYER= I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.nodatasourcelayer.message");
	/**
     *@return the file path of a RasterImageLayer.class
     *eg. C/File/imagename.tif. If the file path is a TEMP folder
     * it returns that the layer has no datasource
     */
    public String getFilePath() {
    	String fileName = null;
    	if (!imageFileName.contains(System.getProperty("java.io.tmpdir"))) {
    		fileName = getImageFileName();
         } else{
        	 fileName = NODATASOURCELAYER;
         }
    	return fileName;
    	}
    
    
    
}
