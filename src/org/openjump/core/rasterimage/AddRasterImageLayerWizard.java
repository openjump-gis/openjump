package org.openjump.core.rasterimage;

import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;

import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.plugin.file.open.ChooseProjectPanel;
import org.openjump.core.ui.plugin.layer.pirolraster.LoadSextanteRasterImagePlugIn;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;
import org.openjump.io.PropertiesHandler;
import org.openjump.util.metaData.MetaInformationHandler;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class AddRasterImageLayerWizard extends AbstractWizardGroup {

  public static final String KEY = AddRasterImageLayerWizard.class.getName();

  private WorkbenchContext workbenchContext;
  private ChooseProjectPanel chooseProjectPanel;
  private SelectRasterImageFilesPanel selectFilesPanel;
  
  private File[] files;
  
  //--- pirol image variables  ----
  protected PropertiesHandler properties = null;
  protected WorldFileHandler worldFileHandler = null;
  protected static String propertiesFile = "RasterImage.properties";
  protected String KEY_ALLWAYSACCEPT_TWF_EXT = "allwaysCheckForTWFExtension";
  protected String KEY_ZOOM_TO_INSERTED_IMAGE = "zoomToImage";
  protected boolean allwaysLookForTFWExtension = true;
  protected boolean zoomToInsertedImage = true;
  private String imageFileName = "";
  private String cachedLayer = "default-layer-name";
  //------ 
  
  public AddRasterImageLayerWizard(WorkbenchContext workbenchContext) {	 
	  super(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image"), IconLoaderFamFam.icon("image.png"),
			  SelectRasterImageFilesPanel.KEY);	  
	  this.workbenchContext = workbenchContext;
  }


  public AddRasterImageLayerWizard(final WorkbenchContext workbenchContext,
		  							final File[] files) {
	  this.workbenchContext = workbenchContext;
	  this.files = files;
	  //initPanels(workbenchContext);
  }

  
  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
    initPanels(workbenchContext);
    selectFilesPanel.setDialog(dialog);

  }
  
  private void initPanels(final WorkbenchContext workbenchContext) {
	    selectFilesPanel = new SelectRasterImageFilesPanel(workbenchContext);
	    addPanel(selectFilesPanel);
	  }
  
  /**
   * Load the files selected in the wizard.
   * 
   * @param monitor The task monitor.
   */
  public void run(WizardDialog dialog, TaskMonitor monitor) {
      this.properties = new PropertiesHandler(AddRasterImageLayerWizard.propertiesFile);
      if (files == null) {
    	  File[] selectedFiles = selectFilesPanel.getSelectedFiles();
    	  open(selectedFiles, monitor);
      } else {
    	  open(files, monitor);
      }
  }
  
  private void open(File[] files, TaskMonitor monitor) {
	  for (File file : files) {
		  open(file, monitor);
	  }
  }

  public void open(File file, TaskMonitor monitor) {
	  try {
		  //workbenchContext.getWorkbench().getFrame().warnUser("would load: " + file.getName());

		  try {
		        this.properties.setProperty(LoadSextanteRasterImagePlugIn.KEY_PATH, file.getPath());
		        
		        this.properties.store(" " + this.KEY_ZOOM_TO_INSERTED_IMAGE + I18N.get("RasterImagePlugIn.28") + this.KEY_ALLWAYSACCEPT_TWF_EXT + I18N.get("RasterImagePlugIn.29") + LoadSextanteRasterImagePlugIn.KEY_PATH + I18N.get("RasterImagePlugIn.30"));
		        
		        String selectedFilename = file.getPath();
		        this.imageFileName = selectedFilename;
		        this.cachedLayer = selectedFilename.substring(selectedFilename
		               .lastIndexOf(File.separator) + 1, selectedFilename
		               .lastIndexOf("."));
		        
		        boolean imageAdded = false;
		        
		        Point imageDimensions = RasterImageLayer.getImageDimensions(workbenchContext, selectedFilename);
		        Envelope env = this.getGeoReferencing(selectedFilename, this.allwaysLookForTFWExtension, imageDimensions, this.workbenchContext);
		        
		        
		        if (env != null){
		            imageAdded = this.addImage(workbenchContext, env, imageDimensions);
		        }
	  
			  OpenRecentPlugIn.get(workbenchContext).addRecentFile(file);

		  } finally {
			  //reader.close();
		  }
	  } catch (Exception e) {
		  monitor.report(e);
	  }
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
      
      RasterImageLayer rLayer = new RasterImageLayer(newLayerName, context.getLayerManager(), this.imageFileName, null, null, envelope);
      
      // #################################
      
      MetaInformationHandler mih = new MetaInformationHandler(rLayer);
      // [sstein 28.Feb.2009] -- not sure if these keys should be translated
      mih.addMetaInformation("file-name", this.imageFileName);
      mih.addMetaInformation("resolution", imageDimensions.x + " (px) x " + imageDimensions.y + " (px)");
      mih.addMetaInformation("real-world-width", new Double(envelope.getWidth()));
      mih.addMetaInformation("real-world-height", new Double(envelope.getHeight()));
      
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

  /**
   * TODO: [sstein] Feb.2009 - I discovered a 0.5px offset towards south-east for 
   * the envelope, in comparison with images loaded with Jon's/VividSolutions implementation, 
   * if the envelope is obtained from a worldfile. 
   * Not sure what is correct. I.e. this implementation seems to assume that the worldfile 
   * coordinate system origin is the corner of the first pixel and not the center. 
   * I have corrected this in WorldFileHandler.readWorldFile()  
   * @param fileName
   * @param allwaysLookForTFWExtension
   * @param imageDimensions
   * @param context
   * @return the RasterImage Envelope
   * @throws IOException
   */
  protected Envelope getGeoReferencing(String fileName, boolean allwaysLookForTFWExtension, Point imageDimensions, WorkbenchContext context) throws IOException{
      double minx, maxx, miny, maxy;
      Envelope env = null;
      
      this.worldFileHandler = new WorldFileHandler(fileName, allwaysLookForTFWExtension);
      
      if (imageDimensions == null){
    	  //logger.printError("can not determine image dimensions");
    	  context.getWorkbench().getFrame().warnUser(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
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
                    	  context.getWorkbench().getFrame().warnUser("unsupported value for ModelTiepointTag (" + GeoTiffConstants.ModelTiepointTag + ")");
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
              
            }else if(fileName.toLowerCase().endsWith(".flt")){
                isGeoTiff = true;
                GridFloat gf = new GridFloat(fileName);

                Coordinate upperLeft = new Coordinate(gf.getXllCorner(), gf.getYllCorner() + gf.getnRows() * gf.getCellSize());
                Coordinate lowerRight = new Coordinate(gf.getXllCorner() + gf.getnCols() * gf.getCellSize(), gf.getYllCorner());

                env = new Envelope(upperLeft, lowerRight);

            }else if(fileName.toLowerCase().endsWith(".asc")){
                isGeoTiff = true;
                GridAscii ga = new GridAscii(fileName);

                Coordinate upperLeft = new Coordinate(ga.getXllCorner(), ga.getYllCorner() + ga.getnRows() * ga.getCellSize());
                Coordinate lowerRight = new Coordinate(ga.getXllCorner() + ga.getnCols() * ga.getCellSize(), ga.getYllCorner());

                env = new Envelope(upperLeft, lowerRight);
            }
          
          if (!isGeoTiff || env==null){
              //logger.printDebug(PirolPlugInMessages.getString("no-worldfile-found"));
        	  context.getWorkbench().getFrame().warnUser(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
              WizardDialog d = new WizardDialog(
                     context.getWorkbench().getFrame(),
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

}
