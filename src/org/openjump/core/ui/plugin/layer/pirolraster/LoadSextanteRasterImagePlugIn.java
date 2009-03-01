/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Code is based on code from com.vividsolutions.jump.workbench.ui.plugin.wms.AddWMSQueryPlugIn.java
 * and on code from e-mail 
 * Message-ID: <5369d92b041105233553cb5ae8@mail.gmail.com>
 * Date: Fri, 5 Nov 2004 23:35:15 -0800
 * From: Jonathan Aquino <jonathan.aquino@gmail.com>
 * To: JUMP Users Discussion <jump-users@lists.jump-project.org>
 * Subject: [jump-users] BeanShell to create an image layer from a file or URL	!!! gif/jpeg/png
 *
 * $Id: RasterImagePlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $Rev: 1.15 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP, removed debug entries
 */

package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.rasterimage.GeoTiffConstants;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.WorldFileHandler;
import org.openjump.io.PropertiesHandler;
import org.openjump.util.metaData.MetaInformationHandler;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class LoadSextanteRasterImagePlugIn extends AbstractPlugIn {
    private String imageFileName = "";
    private String cachedLayer = "default-layer-name";
    
    protected WorldFileHandler worldFileHandler = null;
    //protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
    
    protected PropertiesHandler properties = null;
    protected static String propertiesFile = "RasterImage.properties";
    protected String lastPath = null;
    public static String KEY_PATH = "path";
    protected String KEY_ALLWAYSACCEPT_TWF_EXT = "allwaysCheckForTWFExtension";
    protected boolean allwaysLookForTFWExtension = true;
    protected String KEY_ZOOM_TO_INSERTED_IMAGE = "zoomToImage";
    protected boolean zoomToInsertedImage = true;
    
    protected static PlugInContext plugInContext = null;
    
    public LoadSextanteRasterImagePlugIn() {
        //super(logger);
    }
    
    public static String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Add-Sextante-Raster-Image");
    }
    
    private boolean addImage(WorkbenchContext context, Envelope envelope, Point imageDimensions) {

        String newLayerName = context.getLayerManager().uniqueLayerName(cachedLayer);

        String catName = StandardCategoryNames.WORKING;

        try {
           catName = ((Category) context.createPlugInContext()
                    .getLayerNamePanel().getSelectedCategories().toArray()[0])
                    .getName();
        } catch (RuntimeException e1) {
            //logger.printDebug(e1.getMessage());
        }
        

        int layersAsideImage = context.getLayerManager().getLayerables(Layerable.class).size();
        
        RasterImageLayer rLayer = new RasterImageLayer(newLayerName, context.getLayerManager(), this.imageFileName, null, envelope);
        
        // #################################
        
        MetaInformationHandler mih = new MetaInformationHandler(rLayer);
        
        mih.addMetaInformation(I18N.get("file-name"), this.imageFileName);
        mih.addMetaInformation(I18N.get("resolution"), imageDimensions.x + " (px) x " + imageDimensions.y + " (px)");
        mih.addMetaInformation(I18N.get("real-world-width"), new Double(envelope.getWidth()));
        mih.addMetaInformation(I18N.get("real-world-height"), new Double(envelope.getHeight()));
        
        // ###################################

        
        context.getLayerManager().addLayerable( catName, rLayer );


        if (zoomToInsertedImage || layersAsideImage==0){
            //logger.printDebug("zooming to image, layers: " + layersAsideImage);
	        try {
	            context.getLayerViewPanel().getViewport().zoom(envelope);
	        } catch (NoninvertibleTransformException e) {
	            //logger.printDebug(e.getMessage());
	        }
        }


        return true;
    }

    public void initialize(PlugInContext context) throws Exception {
        plugInContext = context;
		WorkbenchContext workbenchContext = context.getWorkbenchContext();
		
        RasterImageLayer.setWorkbenchContext(context.getWorkbenchContext());
        
        if (context.getWorkbenchContext().getLayerViewPanel() == null){
            //logger.printWarning("rendering manager is NULL");
        }

        //[sstein] 22.Feb.2009 -- now intialized in OpenJUMPConfiguration
        //RenderingManager.putRendererForLayerable(RasterImageLayer.class, new RasterImageLayerRendererFactory(context.getWorkbenchContext()));
        //context.getWorkbenchFrame().getNodeClassToPopupMenuMap().put(RasterImageLayer.class, RasterImageContextMenu.getInstance(context));
        
    	FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        
        featureInstaller.addMainMenuItem(
        		this, 
        		new String[] { MenuNames.LAYER }, 
        		this.getName() + "..." + "{pos:1}", 
        		false, 
        		null, 
        		null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        JPopupMenu layerNamePopupMenu = context.getWorkbenchContext().getWorkbench().getFrame().getCategoryPopupMenu();

        layerNamePopupMenu.addSeparator();
        
        PasteRasterImageLayersPlugIn pasteRasterImagePlugIn = new PasteRasterImageLayersPlugIn();
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, pasteRasterImagePlugIn, I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.PasteRasterImageLayersPlugIn.paste-raster-layer") + "...", false, null, pasteRasterImagePlugIn.createEnableCheck(context.getWorkbenchContext()));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, this, I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Add-Sextante-Raster-Image") + "...", false, null, null);
        
        layerNamePopupMenu.addSeparator();

    }

    public boolean execute(final PlugInContext context)
    throws Exception {
        reportNothingToUndoYet(context);
        
        JFileChooser fileChooser = new JFileChooser();
        this.properties = new PropertiesHandler(LoadSextanteRasterImagePlugIn.propertiesFile);
        try {
            this.properties.load();
            this.lastPath = this.properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
            this.allwaysLookForTFWExtension = this.properties.getPropertyAsBoolean(this.KEY_ALLWAYSACCEPT_TWF_EXT, this.allwaysLookForTFWExtension);
            this.zoomToInsertedImage = this.properties.getPropertyAsBoolean(this.KEY_ZOOM_TO_INSERTED_IMAGE, this.zoomToInsertedImage);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.lastPath != null) {
            fileChooser.setCurrentDirectory(new File(this.lastPath));
        }
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileFilter() {
            public String getDescription() {
                return I18N.get("supported-image-types");
            }
            public boolean accept(File f) {
                if ((f.exists() && f.isFile() && (
                f.getName().toLowerCase().endsWith(".gif") ||
                f.getName().toLowerCase().endsWith(".jpg") ||
                f.getName().toLowerCase().endsWith(".tif") ||
                f.getName().toLowerCase().endsWith(".tiff") ||
                f.getName().toLowerCase().endsWith(".png")
                )) || f.isDirectory()
                ) {
                    return true;
                }
                return false;
            }
        });
        fileChooser.showOpenDialog(context.getWorkbenchFrame());
        
        if (fileChooser.getSelectedFile() == null || !fileChooser.getSelectedFile().exists())
           return false;
        this.properties.setProperty(LoadSextanteRasterImagePlugIn.KEY_PATH, fileChooser.getSelectedFile().getPath());
        
        this.properties.store(" " + this.KEY_ZOOM_TO_INSERTED_IMAGE + I18N.get("RasterImagePlugIn.28") + this.KEY_ALLWAYSACCEPT_TWF_EXT + I18N.get("RasterImagePlugIn.29") + LoadSextanteRasterImagePlugIn.KEY_PATH + I18N.get("RasterImagePlugIn.30"));
        
        String selectedFilename = fileChooser.getSelectedFile().getPath();
        this.imageFileName = selectedFilename;
        this.cachedLayer = selectedFilename.substring(selectedFilename
               .lastIndexOf(File.separator) + 1, selectedFilename
               .lastIndexOf("."));
        
        boolean imageAdded = false;
        
        Point imageDimensions = RasterImageLayer.getImageDimensions(context.getWorkbenchContext(), selectedFilename);
        Envelope env = this.getGeoReferencing(selectedFilename, this.allwaysLookForTFWExtension, imageDimensions, context);
        
        
        if (env != null){
            imageAdded = this.addImage(context.getWorkbenchContext(), env, imageDimensions);
        }
        
        return imageAdded;
    }
    
    protected Envelope getGeoReferencing(String fileName, boolean allwaysLookForTFWExtension, Point imageDimensions, PlugInContext context) throws IOException{
        double minx, maxx, miny, maxy;
        Envelope env = null;
        
        this.worldFileHandler = new WorldFileHandler(fileName, allwaysLookForTFWExtension);
        
        if (imageDimensions == null){
            //logger.printError("can not determine image dimensions");
        	context.getWorkbenchFrame().warnUser(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
            return null;
        }
        
        if (this.worldFileHandler.isWorldFileExistentForImage()!=null) {
            //logger.printDebug(PirolPlugInMessages.getString("worldfile-found"));
            env = this.worldFileHandler.readWorldFile(imageDimensions.x, imageDimensions.y);
        }
            
        if (env == null) {
            
            boolean isGeoTiff = false; 
            
            if ( fileName.toLowerCase().endsWith(".tif") || fileName.toLowerCase().endsWith(".tiff") ) {
                //logger.printDebug("checking for GeoTIFF");
                
                Coordinate tiePoint = null, pixelOffset = null, pixelScale = null;
                double[] doubles = null;
                
                FileSeekableStream fileSeekableStream = new FileSeekableStream(fileName);
                TIFFDirectory tiffDirectory = new TIFFDirectory(fileSeekableStream, 0);
                
                TIFFField[] availTags = tiffDirectory.getFields();
                
                for (int i=0; i<availTags.length; i++){
                    if (availTags[i].getTag() == GeoTiffConstants.ModelTiepointTag){
                        doubles = availTags[i].getAsDoubles();
                        
                        if (doubles.length != 6){
                            //logger.printError("unsupported value for ModelTiepointTag (" + GeoTiffConstants.ModelTiepointTag + ")");
                        	context.getWorkbenchFrame().warnUser("unsupported value for ModelTiepointTag (" + GeoTiffConstants.ModelTiepointTag + ")");
                            break;
                        }
                        
                        if (doubles[0]!=0 || doubles[1]!=0 || doubles[2]!=0){
                            if (doubles[2]==0)
                                pixelOffset = new Coordinate(doubles[0],doubles[1]);
                            else
                                pixelOffset = new Coordinate(doubles[0],doubles[1],doubles[2]);
                        }
                        
                        if (doubles[5]==0)
                            tiePoint = new Coordinate(doubles[3],doubles[4]);
                        else
                            tiePoint = new Coordinate(doubles[3],doubles[4],doubles[5]);
                        
                        //logger.printDebug("ModelTiepointTag (po): " + pixelOffset);
                        //logger.printDebug("ModelTiepointTag (tp): " + tiePoint);
                    } else if (availTags[i].getTag() == GeoTiffConstants.ModelPixelScaleTag){
                        // Karteneinheiten pro pixel x bzw. y
                        
                        doubles = availTags[i].getAsDoubles();
                        
                        if (doubles[2]==0)
                            pixelScale = new Coordinate(doubles[0],doubles[1]);
                        else
                            pixelScale = new Coordinate(doubles[0],doubles[1],doubles[2]);
                        
                        //logger.printDebug("ModelPixelScaleTag (ps): " + pixelScale);
                    } else {
                        //logger.printDebug("tiff field: " + availTags[i].getType() + ", "+ availTags[i].getTag()  + ", "+ availTags[i].getCount());
                    }
                    
                }
                
                fileSeekableStream.close();
                
                if (tiePoint!=null && pixelScale!=null){
                    isGeoTiff = true;
                    Coordinate upperLeft = null, lowerRight = null;
                    
                    if (pixelOffset==null){
                        upperLeft = tiePoint;
                    } else {
                        upperLeft = new Coordinate( tiePoint.x - (pixelOffset.x * pixelScale.x), tiePoint.y - (pixelOffset.y * pixelScale.y));
                    }
                    
                    lowerRight = new Coordinate( upperLeft.x + (imageDimensions.x * pixelScale.x), upperLeft.y - (imageDimensions.y * pixelScale.y));
                    
                    //logger.printDebug("upperLeft: " + upperLeft);
                    //logger.printDebug("lowerRight: " + lowerRight);
                    
                    env = new Envelope(upperLeft, lowerRight);
                }
                
            }
            
            if (!isGeoTiff || env==null){
                //logger.printDebug(PirolPlugInMessages.getString("no-worldfile-found"));
                context.getWorkbenchFrame().warnUser(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
                WizardDialog d = new WizardDialog(
                       context.getWorkbenchFrame(),
                       I18N.get("RasterImagePlugIn.34")
                               + this.worldFileHandler.getWorldFileName()
                               + I18N.get("RasterImagePlugIn.35"),
                       context.getErrorHandler());
                d.init(new WizardPanel[] { new RasterImageWizardPanel() });
                //Set size after #init, because #init calls #pack. [Jon Aquino]
                d.setSize(500, 400);
                GUIUtil.centreOnWindow(d);
                d.setVisible(true);
                
                if (!d.wasFinishPressed()) {
                    //logger.printWarning("user canceled");
                    return null;
                }
                
                minx = Double.parseDouble((String) d
                       .getData(RasterImageWizardPanel.MINX_KEY));
                maxx = Double.parseDouble((String) d
                       .getData(RasterImageWizardPanel.MAXX_KEY));
                miny = Double.parseDouble((String) d
                       .getData(RasterImageWizardPanel.MINY_KEY));
                maxy = Double.parseDouble((String) d
                       .getData(RasterImageWizardPanel.MAXY_KEY));
    
                env = new Envelope(minx, maxx, miny, maxy);
            }
            // creating world file
            this.worldFileHandler = new WorldFileHandler(fileName, this.allwaysLookForTFWExtension);
            this.worldFileHandler.writeWorldFile(env, imageDimensions.x, imageDimensions.y);
        }
        
        return env;
    }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return null;
    }

    public static PlugInContext getPlugInContext() {
        return plugInContext;
    }

}
