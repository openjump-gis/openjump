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

import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import javax.media.jai.JAI;

import com.vividsolutions.jump.workbench.model.*;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.openjump.util.metaData.MetaDataMap;
import org.openjump.util.metaData.ObjectContainingMetaInformation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Timer;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
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
// TODO ObjectContainingMetaInformation seems a bit redundant with Blackboard
public final class RasterImageLayer extends GeoReferencedLayerable
        implements ObjectContainingMetaInformation, Disposable {
    
    //protected static Blackboard blackboard = null;
    
    //protected final static String BLACKBOARD_KEY_PLUGINCONTEXT = PlugInContext.class.getName();
    //protected final static String BLACKBOARD_KEY_WORKBENCHCONTEXT = PlugInContext.class.getName();
    
    protected int lastImgProcessingMode = 0;
    
    protected final static int MODE_NONE = 0;
    protected final static int MODE_SCALINGFIRST = 1;
    protected final static int MODE_CLIPPINGFIRST = 2;
    protected final static int MODE_FASTDISPLAY = 3;
    
    protected Rectangle imagePart, visibleRect = null;
    
    protected double oldScaleXImg2Canvas;
    
    protected int xOffset, yOffset;

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
    //protected Raster actualRasterData = null;
    protected boolean rasterDataChanged = false; //may be needed for rescaling the image values
    //-- end

    protected BufferedImage scaledBufferedImage = null;

    protected Envelope actualImageEnvelope = null, visibleEnv = null, oldVisibleEnv;
    //protected Envelope originalImageEnvelope = null;
    
    /**
     * Flag to decide, if events are fired automatically, if the appearance (imageEnvelope, etc.) changes.<br>
     * default: true
     */
    protected boolean firingAppearanceEvents = true;
    
    /**
     * Flag to control if the image should be deleted from RAM as soon as possible to save RAM
     * or if it should be kept e.g. because it was generated
     * dynamically and can not be loaded from a file again, once it was deleted.
     */
    protected boolean needToKeepImage = false;

    protected Color transparentColor = null;
    protected boolean transparencyColorNeedsToBeApplied = false;

    //-- [sstein 26 June 2013] new as with ASCII grid imports nodata values can be defined
    protected double noDataValue = Double.NaN;
    protected double originalCellSize;
    protected double actualCellSize;
    //-- end

    private Metadata metadata;
    private int bitsPerPixel = -1;
    
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
        return getEnvelope().toString();
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
     * @param name name of the layer
     * @param layerManager the LayerManager
     * @param imageFileName the name of the image file
     * @param imageToDisplay the image (if already loaded) or null
     * @param wholeImageEnvelope the image envelope in model (real world) coordinates
     */
    public RasterImageLayer(String name,
                            LayerManager layerManager,
                            String imageFileName,
                            BufferedImage imageToDisplay,
                            Envelope wholeImageEnvelope) {
        super(name, layerManager);
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
        this.imageFileName = imageFileName;
        setEnvelope(wholeImageEnvelope);
        //this.originalImageEnvelope = wholeImageEnvelope;
        
        if (imageToDisplay != null)
            this.setImage(imageToDisplay);

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
     *@param layerManager the LayerManager
     *@param imageToDisplay the image (if already loaded) or null
     *@param newRaster the raster (if already loaded) or null
     *@param wholeImageEnvelope the image envelope in model (real world) coordinates
     */
    public RasterImageLayer(String name,
                            LayerManager layerManager,
                            BufferedImage imageToDisplay,
                            Raster newRaster,
                            Envelope wholeImageEnvelope) {
        super(name, layerManager);

        if (imageToDisplay == null || newRaster == null) {
            Logger.warn("imageToDisplay and newRaster must not be null");
            return;
        }
        
        getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
        
        this.setNeedToKeepImage(true);
        setEnvelope(wholeImageEnvelope);
        //this.originalImageEnvelope = wholeImageEnvelope;
        
        this.setImage(imageToDisplay);

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

    //@Override
    //public Blackboard getBlackboard() {
    //    if (getBlackboard() == null)
    //        RasterImageLayer.blackboard = new Blackboard();
    //
    //    return RasterImageLayer.blackboard;
    //}

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (imageFileName == null) ?
                new RasterImageLayer(
                        getName(),
                        getLayerManager(),
                        getImage(),
                        getActualRasterData(),
                        getEnvelope())
                : new RasterImageLayer(
                        getName(),
                        getLayerManager(),
                        getImageFileName(),
                        getImage(),
                        getEnvelope());
        //RasterImageLayer raster = null;
        //try {
        //    BufferedImage im = image == null ? getImageForDisplay() : image;
        //    raster = new RasterImageLayer(getName(), getLayerManager(), getImageFileName(),
        //            getImageForDisplay(), new Envelope(getWholeImageEnvelope()));
        //    raster.needToKeepImage = needToKeepImage;
        //} catch (Exception ex) {
        //    Logger.error(ex);
        //}
        //// clone must produce a layerable with the same name (as for Layer) not a unique name
        //if (raster != null) {
        //    raster.getLayerManager().setFiringEvents(false);
        //    raster.setName(getName());
        //    raster.getLayerManager().setFiringEvents(true);
        //}
        //return raster;
    }
    
    /**
     * apply a scale operation to the image and return the new image.
     */
    protected BufferedImage scaleImage(BufferedImage im, float xScale, float yScale) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(xScale);
        pb.add(yScale);
        // @TODO Try to replace JAI by Graphics2D after migration to 2.0
        //BufferedImage resizedImage =
        //        new BufferedImage((int)(im.getWidth()*xScale), (int)(im.getHeight()*yScale), im.getType());
        //Graphics2D graphics2D = resizedImage.createGraphics();
        //graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        //graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        //graphics2D.drawImage(im, 0, 0, (int)(im.getWidth()*xScale), (int)(im.getHeight()*yScale), null);
        //graphics2D.dispose();
        //return resizedImage;
        return JAI.create("Scale", pb, null).getAsBufferedImage();
    }


    /**
     * Create a single color image in the case where a single image pixel is visible
     * @param color color of the pixel
     * @return a single color BufferedImage
     */
    protected BufferedImage createOneColorImage(Color color){
        BufferedImage bim = new BufferedImage(visibleRect.width, visibleRect.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grfcs = bim.createGraphics();
        grfcs.setColor(color);
        grfcs.fillRect( 0, 0, bim.getWidth(), bim.getHeight() );
        grfcs.dispose();
        return bim;
    }
    
    /**
     * Creates the image to draw
     * @param layerViewPanel the LayerViewPanel where the image will be drawn
     * @return the BufferedImage to be drawn
     */
    public BufferedImage createImage(LayerViewPanel layerViewPanel) {
        
        Viewport viewport = layerViewPanel.getViewport();
        
        if (!this.isVisible() || this.transparencyLevel >= 1.0) {
            this.setImageProcessingMode(RasterImageLayer.MODE_NONE);
            this.clearImageAndRaster(true);
            return null;
        }

        BufferedImage imageToDraw = null;

        try {

            java.awt.Point imageDims = RasterImageIO.getImageDimensions(imageFileName);

            assert imageDims != null;
            origImageWidth = imageDims.x;
            origImageHeight = imageDims.y;
            visibleRect = viewport.getPanel().getVisibleRect();

            int visibleX1 = visibleRect.x;
            int visibleY1 = visibleRect.y;
            int visibleX2 = visibleX1 + visibleRect.width;
            int visibleY2 = visibleY1 + visibleRect.height;

            // Viewport envelope in model coordinates
            Coordinate upperLeftVisible = viewport.toModelCoordinate(new Point(visibleX1, visibleY1));
            Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(visibleX2, visibleY2));
            Envelope newVisibleEnv = new Envelope(upperLeftVisible, lowerRightVisible);

            setImageSet(false);
            
            if (visibleEnv == null || visibleEnv.getMinX() != newVisibleEnv.getMinX() || visibleEnv.getMaxX() != newVisibleEnv.getMaxX() || 
                    visibleEnv.getMinY() != newVisibleEnv.getMinY() || visibleEnv.getMaxY() != newVisibleEnv.getMaxY() || symbologyChanged){
                visibleEnv = newVisibleEnv;
                    
                symbologyChanged = false;
                this.setNeedToKeepImage(false);
                if (bitsPerPixel == -1) {
                    if (imageFileName.toLowerCase().endsWith(".flt")) bitsPerPixel = 16;
                    else if (imageFileName.toLowerCase().endsWith(".asc")) bitsPerPixel = 16;
                    else if (imageFileName.toLowerCase().endsWith(".txt")) bitsPerPixel = 16;
                    else {
                        try {
                            bitsPerPixel = Imaging.getImageInfo(new File(imageFileName)).getBitsPerPixel();
                        } catch(ImageReadException e) {
                            Logger.warn("Can't get ImageInfo of " + imageFileName, e);
                        }
                    }
                }
                clearImageAndRaster(true);
                // Check that there is enough free memory for the image + 1% of available memory + 10Mb
                if (getAvailRAM()-getCommittedMemory() <
                        origImageWidth*origImageHeight*bitsPerPixel/8.0 + getAvailRAM()*0.01 + 10*1024*1024) {
                    layerViewPanel.getContext().warnUser("Low Memory : image " +
                            imageFileName + " will not be displayed");
                    System.out.println("" + (getAvailRAM()-getCommittedMemory())/1024 + "kb < " +
                            (origImageWidth*origImageHeight*bitsPerPixel/8)/1024 + "kb " +
                            getAvailRAM()*0.01/1024 + "kb + 10240");
                    return null;
                } else {
                    Logger.debug("Reload image");
                }
                // Load the part of the image intersecting the viewport and setting this.image
                reLoadImage(layerViewPanel);
                if(image == null) {
                    // If image does not intersect viewport, it is null
                    return null;
                }                    

                // Coordinates of actual image in viewport coordinates
                Point2D upperLeftCornerOfImage = viewport.toViewPoint(new Coordinate(getActualImageEnvelope().getMinX(), getActualImageEnvelope().getMaxY()));
                Point2D lowerRightCornerOfImage = viewport.toViewPoint(new Coordinate(getActualImageEnvelope().getMaxX(), getActualImageEnvelope().getMinY()));

                double scaledWidth = lowerRightCornerOfImage.getX() - upperLeftCornerOfImage.getX();
                double scaledHeight = upperLeftCornerOfImage.getY() - lowerRightCornerOfImage.getY();

                // Apply symbology to this.image
                imageToDraw = stretchImageValuesForDisplay();
                layerViewPanel.getViewport().update();
                setImage(imageToDraw);

                //if(getCommittedMemory() + minRamToKeepFree < availRAM){
                    //setNeedToKeepImage(true); //so small images are not reloaded every time
                //}

                //[sstein end]
                //Compute envelope of the visible part
                imagePart = getVisibleImageCoordinatesOfImage(image.getWidth(), image.getHeight(),
                        visibleEnv, getActualImageEnvelope());

                double scaleXImg2Canvas = scaledWidth / image.getWidth();
                double scaleYImg2Canvas = scaledHeight / image.getHeight();

                if (scaledBufferedImage == null || scaleXImg2Canvas != oldScaleXImg2Canvas ||
                        !RasterImageLayer.tilesAreNotNullAndCongruent( visibleEnv, oldVisibleEnv)){

                    scaledBufferedImage = getVisiblePartOfTheImage( getImageForDisplay(layerViewPanel), imagePart );

                    if (scaledBufferedImage != null) {
                        // avoid an 1 pixel by 1 pixel image to get scaled to thousands by thousands pixels causing an out of memory error
                        if (imagePart.width == 1 || imagePart.height == 1){
                            xOffset = 0;
                            yOffset = 0;
                            scaledBufferedImage = createOneColorImage(
                                    new Color(scaledBufferedImage.getRGB(0,0)));
                        } else {
                            scaledBufferedImage = getScaledImageMatchingVisible(scaledBufferedImage, scaleXImg2Canvas, scaleYImg2Canvas );
                        }

                    } else {
                        return null;
                    }

                    if (transparentColor!=null) transparencyColorNeedsToBeApplied = true;

                    xOffset = (int)(xOffset *scaleXImg2Canvas);
                    yOffset = (int)(yOffset *(-scaleYImg2Canvas));

                    oldScaleXImg2Canvas = scaleXImg2Canvas;

                    oldVisibleEnv = visibleEnv;
                }
    
                               
            }

            if (scaledBufferedImage != null && transparencyColorNeedsToBeApplied ){
                imageToDraw = setupTransparency(scaledBufferedImage);
            } else if (scaledBufferedImage != null) {
                imageToDraw = scaledBufferedImage;
            }
            

        } catch (Exception e){
            Logger.warn(e);
        }
        if (imageToDraw != null) {
            return imageToDraw;
        } else {
            return scaledBufferedImage;
        }
    }

    protected long getCommittedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
    
    /**
     * Deletes image from RAM (if it is not to be kept and if the RAM consumption is high)
     * and calls the garbage collector, if <code>garbageCollect</code> is true.
     * @param garbageCollect if true the garbage collector will be called (this parameter
     *                       may be overridden, if there is not enough RAM available...)
     */
    public boolean clearImageAndRaster(boolean garbageCollect){
        boolean reallyNeedToFreeRAM = (availRAM-getCommittedMemory() < minRamToKeepFree);
        if (!needToKeepImage && reallyNeedToFreeRAM ){
            flushImages(garbageCollect);
        }
        else if (garbageCollect){
            Runtime.getRuntime().gc();
        }
        return reallyNeedToFreeRAM;
    }
    
    /**
     * flushes all images from the RAM.
     * @param garbageCollect if true the garbage collector will be called (this parameter may be overridden, if there is not enough RAM available...)
     */
    public void flushImages(boolean garbageCollect){
        if (image!=null)
            image.flush();
        image = null;
        if (scaledBufferedImage!=null)
            scaledBufferedImage.flush();
        scaledBufferedImage = null;
        if (garbageCollect){
            Runtime.getRuntime().gc();
        }
    }
    
    public void reLoadImage(LayerViewPanel layerViewPanel) throws Exception {
        
        //if (image == null && !needToKeepImage){
        
        RasterImageIO rasterImageIO = new RasterImageIO();

        Viewport viewport = layerViewPanel.getViewport();
        if(!viewport.getEnvelopeInModelCoordinates().intersects(getEnvelope())
                && layerViewPanel.getLayerManager().getLayerables(Layerable.class).isEmpty()
        ) {
            viewport.zoom(getEnvelope());
        }

        Resolution requestedRes = RasterImageIO.calcRequestedResolution(viewport);
        long start = Timer.milliSecondsSince(0);
        Logger.debug("Try reading "+getName());
        // Get the part of the image intersecting the viewport
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(imageFileName,
                stats, viewport.getEnvelopeInModelCoordinates(), requestedRes);
        Logger.debug("Reading '"+getName()+"' took "+Timer.secondsSinceString(start)+"s.");
        metadata = imageAndMetadata.getMetadata();
        image = imageAndMetadata.getImage();
        numBands = metadata.getStats().getBandCount();
        noDataValue = imageAndMetadata.getMetadata().getNoDataValue();
        stats = imageAndMetadata.getMetadata().getStats();
        setEnvelope(imageAndMetadata.getMetadata().getOriginalImageEnvelope());
        actualImageEnvelope = imageAndMetadata.getMetadata().getActualEnvelope();
        originalCellSize = imageAndMetadata.getMetadata().getOriginalCellSize();        
        actualCellSize = imageAndMetadata.getMetadata().getActualCellSize();

        if(image != null) {
            setImage(image);
        }
    }
    
    ///**
    // * use this to assign the raster data again
    // * the method is called from  getRasterData();
    // */
    //public void reLoadImageButKeepImageForDisplay() throws Exception {
    //   BufferedImage pi = getImageForDisplay(layerViewPanel);
    //   //[sstein 24.Sept.2010] commented out:
    //   //PlanarImage dontNeedThisImage = RasterImageLayer.loadImage( context, imageFileName); //causes error for .clone()
    //   this.setImage(pi);
    //}
    
    protected BufferedImage stretchImageValuesForDisplay() throws NoninvertibleTransformException{

        Raster actualRasterData = image.copyData(null);
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
                        
                        final RasterSymbology rasterSymbology;
                        if (metadata.getStats().getMin(0) == metadata
                                .getStats().getMax(0)) {
                            rasterSymbology = new RasterSymbology(RasterSymbology.TYPE_SINGLE);
                        } else {
                            rasterSymbology = new RasterSymbology(RasterSymbology.TYPE_RAMP);
                        }
                        if (!Double.isNaN(metadata.getNoDataValue())) {
                            rasterSymbology.addColorMapEntry(metadata.getNoDataValue(), transparentColor);
                        }
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
                    

                    // If symbology min value is higher than raster min value
                    // the value becomes equal to the symbology min value
                    Double[] symbologyClassLimits =  symbology.getColorMapEntries_tm().keySet().toArray(new Double[0]);
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
        return getEnvelope();
    }
    
    public Envelope getActualImageEnvelope() {
        return actualImageEnvelope;
    }
    
    /**
     * Sets the Envelope containing the real world coordinates (e.g. WGS84) of the image
     * this needs to be set (if it wasn't given to the constructor)!
     * @param envelope the Envelope
     */
    private void setWholeImageEnvelope(Envelope envelope) {
        setEnvelope(envelope);

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
        return getEnvelope().toString();
    }
    
    public String getXmlActualImageEnvelope() {
        return actualImageEnvelope.toString();
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
     * changes to the RasterImageLayer by using
     * {@link RasterImageLayer#setGeometryAsWholeImageEnvelope(Geometry)}.
     * @return return the imageEnvelope (= bounding box) as a geometry,
     */
    public Geometry getWholeImageEnvelopeAsGeometry(){
        return new GeometryFactory().toGeometry(getEnvelope());
    }
    
    public Geometry getActualImageEnvelopeAsGeometry(){
        return new GeometryFactory().toGeometry(actualImageEnvelope);
    }
    
    /**
     * Method to set the coordinates of the image, e.g. after changing them after using
     * {@link RasterImageLayer#getWholeImageEnvelopeAsGeometry()}.
     */
    public void setGeometryAsWholeImageEnvelope(Geometry geometry){
        setWholeImageEnvelope(geometry.getEnvelopeInternal());
    }
    
    public void setGeometryAsActualImageEnvelope(Geometry geometry){
        setActualImageEnvelope(geometry.getEnvelopeInternal());
    }
    
    /**
     * Add transparency to the image (more exactly: to each pixel which a color == this.transparentColor)
     *@param bim the image
     */
    private BufferedImage setupTransparency(BufferedImage bim){
        //BufferedImage bim = pImage.getAsBufferedImage();
        
        ColorModel cm = bim.getColorModel();
        int fullTransparencyAlpha = 255;
        
        if (this.getTransparentColor()==null){
            return null;
        }
        
        int transparentColor = this.getTransparentColor().getRGB();
        
        int[] argb = new int[4];
        
        if (!cm.hasAlpha()){
            bim = RasterImageLayer.makeBufferedImage(bim);
            //cm = bim.getColorModel();
           
        }
        
        for( int w=0; w<bim.getWidth(); w++){
            for (int h=0; h<bim.getHeight(); h++){

                if (bim.getRGB(w,h)==transparentColor){
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
     * @param im the Image
     * @return the BufferedImage
     */
    public static BufferedImage makeBufferedImage(Image im) {
        BufferedImage copy = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_ARGB); 
        Graphics2D g2d = copy.createGraphics();
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

    /*
    public BufferedImage getTileAsImage( Envelope wantedEnvelope ) throws Exception{

        double imgWidth = image.getWidth();
        double imgHeight = image.getHeight();
        Envelope imageEnv = getActualImageEnvelope();
        Envelope visiblePartOfImage = wantedEnvelope.intersection(imageEnv);
        if (visiblePartOfImage.isNull()) return null;
        
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
        
        width = (int)(visiblePartOfImage.getWidth() * scaleX);
        height =  (int)(visiblePartOfImage.getHeight() * scaleY);

        if (width < imgWidth) width += 1;
        if (height < imgHeight) height += 1;
        
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
    */

    
    //protected WorkbenchContext getWorkbenchContext(){
    //    return (WorkbenchContext)this.getBlackboard().get(BLACKBOARD_KEY_WORKBENCHCONTEXT);
    //}
    
    //public static void setWorkbenchContext(WorkbenchContext wContext){
    //    if (blackboard==null)
    //        blackboard = new Blackboard();
    //
    //    blackboard.put(BLACKBOARD_KEY_WORKBENCHCONTEXT, wContext);
    //}
    
    public Rectangle getDrawingRectangle( double imgWidth, double imgHeight, Envelope imageEnv, Viewport viewport ) throws NoninvertibleTransformException{
        
        Rectangle visible = viewport.getPanel().getVisibleRect();
        
        Point2D upperLeftCorner;
        Point2D lowerRightCorner;
        
        try {
            upperLeftCorner = viewport.toViewPoint(new Coordinate(imageEnv.getMinX(), imageEnv.getMaxY()));
            lowerRightCorner = viewport.toViewPoint(new Coordinate(imageEnv.getMaxX(), imageEnv.getMinY()));
        } catch(java.awt.geom.NoninvertibleTransformException ne) {
            ne.printStackTrace();
            return null;
        }
        
        int visibleX1 = visible.x;
        int visibleY1 = visible.y;
        int visibleX2 = visibleX1 + visible.width;
        int visibleY2 = visibleY1 + visible.height;
        
        Coordinate upperLeftVisible = viewport.toModelCoordinate(new Point(visibleX1, visibleY1));
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

    /**
     * Returns the visible part of the image in image coordinate
     * @param imgWidth original image width
     * @param imgHeight original image height
     * @param viewportEnv viewport in model coordinates
     * @param imageEnv image envelope in model coordinates
     * @return visible part of the image in image coordinates
     */
    protected Rectangle getVisibleImageCoordinatesOfImage(double imgWidth, double imgHeight,
                                                          Envelope viewportEnv, Envelope imageEnv ){

        Envelope visiblePartOfImage = viewportEnv.intersection(imageEnv);
        if (visiblePartOfImage.isNull()) return null;

        // Offset from upperleft corner of viewport to upperleft corner of image in model coordinates
        double offset2VisibleX = imageEnv.getMinX() - viewportEnv.getMinX();
        double offset2VisibleY = viewportEnv.getMaxY() - imageEnv.getMaxY();

        // Scale to convert from model coordinates to image coordinates
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
        
        int width = (int)(visiblePartOfImage.getWidth() * scaleX);
        int height =  (int)(visiblePartOfImage.getHeight() * scaleY);

        if (width < imgWidth) width += 1;
        if (height < imgHeight) height += 1;

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
     * @return the image
     */
    public BufferedImage getImageForDisplay(LayerViewPanel layerViewPanel) throws Exception {
        if (image == null)
            reLoadImage(layerViewPanel);
        return image;
    }
    
    /**
     * @return true, if the image object was set at least once, else false
     */
    public boolean isImageSet() {
        return imageSet;
    }
    
    /**
     * Returns the transparency level of the image. The transparencyLevel controlls the transparency level of the whole image (all pixels). It
     * is independent of the transparency color, that replaces a certain color in the image. 
     * The transparencyLevel is expressed as a float within a range from 0.0 (no transparency) to 1.0 (full transparency).
     * @return the transparency level of the image
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

        if (!Objects.equals(this.transparentColor, transparentColor)){
         
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
        RasterImageLayer.freeRamFactor = freeRamFactor;
        RasterImageLayer.minRamToKeepFree = RasterImageLayer.availRAM * RasterImageLayer.freeRamFactor;
        RasterImageLayer.maxPixelsForFastDisplayMode = (int)((RasterImageLayer.availRAM - RasterImageLayer.minRamToKeepFree)/(1024*1024) * 3000);
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
     * Sets the image's files name (if image is not to be kept) - this needs to be set!
     * @param imageFileName the file name of the image
     */
    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
        this.setNeedToKeepImage(false);
    }
    
    
    /**
     * 
     * @return the file name of the image represented by this instance of the <code>RasterImageLayer</code>
     */
    public String getImageFileName() {
        return imageFileName;
    }

    /**
     * check, if image will be kept in RAM or if it will be reloaded from a file
     * if needed
     * @return true if image will be kept in RAM, else false
     */
    public boolean isNeedToKeepImage() {
        return needToKeepImage;
    }
    /**
     * toogle, if image will be kept in RAM or if it will be reloaded from a file
     * if needed
     * @param needToKeepImage true if image is supposed be kept in RAM, else false
     */
    public void setNeedToKeepImage(boolean needToKeepImage) {
        this.needToKeepImage = needToKeepImage;
    }

    @Override
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
     * @param origImageHeight height of original image
     */
    public void setOrigImageHeight(int origImageHeight) {
        this.origImageHeight = origImageHeight;
    }

    /**
     * for java2xml
     * @param origImageWidth width of original image
     */
    public void setOrigImageWidth(int origImageWidth) {
        this.origImageWidth = origImageWidth;
    }

    /**
     * shows or hides the image in the Jump map
     * @param visible set true if image must be visible
     */
    @Override
	public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        if (!visible)
            this.clearImageAndRaster(true);
        
        if (this.isFiringAppearanceEvents())
            this.fireAppearanceChanged();
    }
    

    /**
     * @see #firingAppearanceEvents
     * @return true if appearance events are fired automatically, false if not
     */
    public boolean isFiringAppearanceEvents() {
        return firingAppearanceEvents;
    }

    /**
     * @see #firingAppearanceEvents
     * @param firingAppearanceEvents true if appearance events are to be fired automatically, false if not
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

        return RasterImageIO.loadRasterData(imageFileName, subset);
    }
    
    public Rectangle getRectangleFromEnvelope(Envelope envelope) {
        
        double imgWidth = origImageWidth;
        double imgHeight = origImageHeight;
        Envelope imageEnv = getEnvelope();
        
        double offset2VisibleX = imageEnv.getMinX() - envelope.getMinX();
        double offset2VisibleY = envelope.getMaxY() - imageEnv.getMaxY();
        
        double scaleX = imgWidth / imageEnv.getWidth();
        double scaleY = imgHeight / imageEnv.getHeight();
        
        // use local variables!
        int xOffset, yOffset;
        
        if (offset2VisibleX >= 0){
            xOffset = 0;
        } else {
            xOffset = (int)Math.round((-offset2VisibleX * scaleX));
        }
        
        if (offset2VisibleY >= 0){
            yOffset = 0;
        } else {
            yOffset = (int)Math.round((-offset2VisibleY * scaleY));
        }
        
        int wantedWidth = (int)Math.round((envelope.getWidth() * scaleX));
        int wantedHeight = (int)Math.round((envelope.getHeight() * scaleY));
        
        return new Rectangle(xOffset, yOffset, wantedWidth, wantedHeight);
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
     * @return number of bands
     */
    public int getNumBands(){
    	return numBands;
    }
    
    public void dispose() {
        // TODO: probably a good idea to remove resources when the layer is closed up
        // TiffUtilsV2 contains a cache to avoid reading image files again and again
        // but which can hold file lock for ever if entries are not removed.
        TiffUtilsV2.removeFromGeoRastercache(new File(imageFileName));
    }
           
    public Double getCellValue(Coordinate coordinate, int band) throws IOException {
        
        return getCellValue(coordinate.x, coordinate.y, band);
    }
    
    public Double getCellValue(int col, int row, int band) throws IOException {

        int pos = row * origImageWidth + col;
        if(pos <0 || pos > origImageWidth * origImageHeight) return null;
                       
        return RasterImageIO.readCellValue(imageFileName, col, row, band);
        
    }

    public Double getCellValue(double coordX, double coordY, int band) throws IOException {
        
        double cellSizeX = (getEnvelope().getMaxX() - getEnvelope().getMinX()) / origImageWidth;
        double cellSizeY = (getEnvelope().getMaxY() - getEnvelope().getMinY()) / origImageHeight;
        
        int col = (int) Math.floor((coordX - getEnvelope().getMinX()) / cellSizeX);
        int row = origImageHeight - (int) Math.floor((coordY - getEnvelope().getMinY()) / cellSizeY) - 1;
        
        if(col <0 || col >= origImageWidth || row <0 || row >= origImageHeight) return null;

        return RasterImageIO.readCellValue(imageFileName, col, row, band);

        /*
        if (stats.getBandCount()<3) {
            value = RasterImageIO.readCellValue(imageFileName, col, row, band);
            } else {
            	try {
                	value =  imageProcessingStep2.getData().getSampleFloat(col, row, band);
            			
                		//	imageProcessingStep2.getData().getSampleFloat(col, row, band);//actualRasterData.getSampleDouble(col, row, band);
                } catch (ArrayIndexOutOfBoundsException e) {
                	value = RasterImageIO.readCellValue(imageFileName, col, row, band);
                }
            }
        */
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
    
    public static class RasterDataNotFoundException extends Exception {}
    
    public static Rectangle getViewportRectangle(WorkbenchContext workbenchContext) {
        
        return new Rectangle(
                0,
                0,
                workbenchContext.getLayerViewPanel().getVisibleRect().width,
                workbenchContext.getLayerViewPanel().getVisibleRect().height);
    }
    
    public RasterSymbology getSymbology() {
        return symbology;
    }
    
    public void setSymbology(RasterSymbology symbology) throws NoninvertibleTransformException {
        this.symbology = symbology;
        symbologyChanged = true;
        scaledBufferedImage = null;
        //LayerViewPanel layerViewPanel = getWorkbenchContext().getLayerViewPanel();
        //if(layerViewPanel != null) {
        //    layerViewPanel.getViewport().update();
        //}
    }

    public Raster getActualRasterData() {
        return image.copyData(null);
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
        return imageFileName.contains(System.getProperty("java.io.tmpdir"));
    }
 
   
    private final static String NODATASOURCELAYER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.nodatasourcelayer.message");

	/**
     * @return the file path of a RasterImageLayer.class
     * eg. C/File/imagename.tif. If the file path is a TEMP folder
     * it returns that the layer has no datasource
     */
    public String getFilePath() {
    	String fileName;
    	if (!imageFileName.contains(System.getProperty("java.io.tmpdir"))) {
    		fileName = getImageFileName();
         } else{
        	 fileName = NODATASOURCELAYER;
         }
    	return fileName;
    }
   
    //[Giuseppe Aruta 04/01/2017] SRS info for RasterImageLayer.class
    //private SRSInfo srsInfo;

    //public SRSInfo getSRSInfo() {
    //    return srsInfo;
    //}

    //public void setSRSInfo(SRSInfo srs) {
    //    this.srsInfo = srs;
    //}
    
}
