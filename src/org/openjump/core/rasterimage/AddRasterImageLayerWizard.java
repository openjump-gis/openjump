package org.openjump.core.rasterimage;

import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.ccordsys.utils.ProjUtils;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;
import org.openjump.io.PropertiesHandler;
import org.openjump.util.metaData.MetaInformationHandler;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorV2Util;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class AddRasterImageLayerWizard extends AbstractWizardGroup {

    public static final String KEY = AddRasterImageLayerWizard.class.getName();

    private final WorkbenchContext workbenchContext;
    //private ChooseProjectPanel chooseProjectPanel;
    private SelectRasterImageFilesPanel selectFilesPanel;

    private File[] files;

    // --- pirol image variables ----
    protected PropertiesHandler properties = null;
    protected WorldFileHandler worldFileHandler = null;
    protected static String propertiesFile = "RasterImage.properties";
    protected String KEY_ALWAYS_ACCEPT_TWF_EXT = "alwaysCheckForTWFExtension";
    protected String KEY_ZOOM_TO_INSERTED_IMAGE = "zoomToImage";
    protected boolean alwaysLookForTFWExtension = true;
    protected boolean zoomToInsertedImage = false;
    private String imageFileName = "";
    private String cachedLayer = "default-layer-name";
    //public static String KEY_PATH = "path";

    // ------

    public AddRasterImageLayerWizard(WorkbenchContext workbenchContext) {
        super(
                I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image"),
                IconLoader.icon("mapSv2_13.png"),
                SelectRasterImageFilesPanel.KEY);
        this.workbenchContext = workbenchContext;
        //RasterImageLayer.setWorkbenchContext(workbenchContext);
    }

    public AddRasterImageLayerWizard(final WorkbenchContext workbenchContext,
            final File[] files) {
        this.workbenchContext = workbenchContext;
        this.files = files;
        // initPanels(workbenchContext);
    }

    @Override
    public void initialize(WorkbenchContext workbenchContext,
            WizardDialog dialog) {
        initPanels(workbenchContext);
        selectFilesPanel.setDialog(dialog);

    }

    private void initPanels(final WorkbenchContext workbenchContext) {
        // [mmichaud 2012-05-11] bug #3521266 : create selectFilesPanel only
        // once
        if (selectFilesPanel == null) {
            selectFilesPanel = new SelectRasterImageFilesPanel(workbenchContext);
            addPanel(selectFilesPanel);
        }
    }

    /**
     * Load the files selected in the wizard.
     * 
     * @param dialog
     *          The WizardDialog
     * @param monitor
     *          The task monitor.
     */
    @Override
    public void run(WizardDialog dialog, TaskMonitor monitor) {
        TaskMonitorV2Util.setTitle(monitor, I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image"));
        properties = new PropertiesHandler(
                AddRasterImageLayerWizard.propertiesFile);
        if (files == null) {
            final File[] selectedFiles = selectFilesPanel.getSelectedFiles();
            open(selectedFiles, monitor);
        } else {
            open(files, monitor);
        }
    }

    private void open(File[] files, TaskMonitor monitor) {
      monitor.allowCancellationRequests();
      //LayerManager layerManager = workbenchContext.getLayerManager();

        for (int i = 0; i < files.length; i++) {
          if (TaskMonitorV2Util.isCancelRequested(monitor))
            break;
          try {
            //layerManager.setFiringEvents(false);
            File file = files[i];
            TaskMonitorV2Util.report(monitor, i + 1, files.length, " - " + file.getName());
            open(file, monitor);
          } finally {
            //layerManager.setFiringEvents(true);
          }
        }

    }

    private void open(File file, TaskMonitor monitor) {
        try {
            try {
                properties.setProperty("path",
                        file.getPath());

                properties.store(" " + KEY_ZOOM_TO_INSERTED_IMAGE
                        + I18N.getInstance().get("RasterImagePlugIn.28")
                        + KEY_ALWAYS_ACCEPT_TWF_EXT
                        + I18N.getInstance().get("RasterImagePlugIn.29")
                        + "path"
                        + I18N.getInstance().get("RasterImagePlugIn.30"));

                final String selectedFilename = file.getPath();
                imageFileName = selectedFilename;
                cachedLayer = selectedFilename.substring(
                        selectedFilename.lastIndexOf(File.separator) + 1,
                        selectedFilename.lastIndexOf("."));

                // boolean imageAdded = false;

                final Point imageDimensions = RasterImageIO
                        .getImageDimensions(selectedFilename);
                final Envelope env = getGeoReferencing(selectedFilename,
                        alwaysLookForTFWExtension, imageDimensions,
                        workbenchContext);

                if (env != null) {
                    addImage(workbenchContext, env, imageDimensions);
                }

                OpenRecentPlugIn.get(workbenchContext).addRecentFile(file);

            } finally {
                // reader.close();
            }
        } catch (final Exception e) {
            monitor.report(e);
        }
    }

    
    //method from DataSourceQueryChooserOpenWizard
    private String chooseCategory(WorkbenchContext context) {
        return context.getLayerableNamePanel().getSelectedCategories().isEmpty() ? StandardCategoryNames.WORKING
          : context.getLayerableNamePanel()
            .getSelectedCategories()
            .iterator()
            .next()
            .toString();
      }
    
    
   // @SuppressWarnings("deprecation")
	private void addImage(WorkbenchContext context, Envelope envelope,
            Point imageDimensions) {

        if (context.getTask() == null) {
            context.getWorkbench().getFrame().addTaskFrame();
        }
 
        final String newLayerName = context.getLayerManager().uniqueLayerName(
                cachedLayer);

        String catName = chooseCategory(context);
        
       /* String catName = chooseCategory(context)StandardCategoryNames.WORKING;

        try {
            catName = ((Category) context
                    . getLayerableNamePanel().getSelectedCategories().toArray()[0])
                    .getName();
        } catch (final RuntimeException e1) {
            Logger.warn("AddRasterImageLayerWizard.addImage: " +
                    "error trying to get the name of the currently selected category", e1);
        }
*/
        final int layersAsideImage = context.getLayerManager()
                .getLayerables(Layerable.class).size();

        final RasterImageLayer rLayer = new RasterImageLayer(newLayerName,
                context.getLayerManager(), imageFileName, null, envelope);
        // [Giuseppe Aruta 04/01/2017] Store SRS info into
        // RasterImageLayer.class metadata
        try {
            // [Giuseppe Aruta 18/08/2018] applyed a patch to partially solve
            // bug 479 (OpenJUMp doen't recognize RasterImageLayer SRS on
            // loading
            // anymore)
            // rLayer.setSRSInfo(ProjUtils.getSRSInfoFromLayerSource(rLayer));
            final SRSInfo srsInfo = ProjUtils.getSRSInfoFromLayerSource(rLayer);
            srsInfo.complete();
            rLayer.setSrsInfo(srsInfo);
        } catch (Exception e) {
            Logger.error(e);
        }
        // #################################

        final MetaInformationHandler mih = new MetaInformationHandler(rLayer);
        // [sstein 28.Feb.2009] -- not sure if these keys should be translated
        mih.addMetaInformation("file-name", imageFileName);
        mih.addMetaInformation("resolution", imageDimensions.x + " (px) x "
                + imageDimensions.y + " (px)");
        // mih.addMetaInformation("real-world-width", new
        // Double(envelope.getWidth()));
        // mih.addMetaInformation("real-world-height", new
        // Double(envelope.getHeight()));
        mih.addMetaInformation("real-world-width", envelope.getWidth());
        mih.addMetaInformation("real-world-height", envelope.getHeight());
        // [Giuseppe Aruta 2017/11/13] Ass SRID and project source as
        // metadata. Those datas are saved into OJ project file and can be
        // reused
        // by the plugins
        mih.addMetaInformation("srid", rLayer.getSrsInfo().getCode());
        mih.addMetaInformation("srid-location", rLayer.getSrsInfo().getSource());

        // ###################################
        context.getLayerManager().addLayerable(catName, rLayer);

        if (zoomToInsertedImage || layersAsideImage == 0) {
            // logger.printDebug("zooming to image, layers: " +
            // layersAsideImage);
            try {
                context.getLayerViewPanel().getViewport().zoom(envelope);
            } catch (final NoninvertibleTransformException e) {
                // logger.printDebug(e.getMessage());
            }
        }

    }

    /**
     * TODO: [sstein] Feb.2009 - I discovered a 0.5px offset towards south-east
     * for the envelope, in comparison with images loaded with
     * Jon's/VividSolutions implementation, if the envelope is obtained from a
     * worldfile. Not sure what is correct. I.e. this implementation seems to
     * assume that the worldfile coordinate system origin is the corner of the
     * first pixel and not the center. I have corrected this in
     * WorldFileHandler.readWorldFile()
     * 
     * [Giuseppe Aruta - 04/27/2023]. The method has been optimized comparing to the previous version:<br>
     * <li>a) The first part that build the Envelope from image info has been removed and delegated
     *  to RasterImageIO.getGeoReferencing() method.
     * <li>b) In the case that the geographic information of the image are not find, the envelope is
     *  build from the view using the method getEnvelopeFromView()<br>
     * 
     * @param fileName the file name
     * @param alwaysLookForTFWExtension
     * @param imageDimensions
     * @param context
     * @return the RasterImage Envelope
     * @throws IOException
     * @throws NoninvertibleTransformException
     */
     protected Envelope getGeoReferencing(String fileName,
            boolean alwaysLookForTFWExtension, Point imageDimensions,
            WorkbenchContext context) throws Exception {
        Envelope env = null;
        env=RasterImageIO.getGeoReferencing(fileName,alwaysLookForTFWExtension, imageDimensions);
        if (env==null) {
            env=RasterImageIO.getEnvelopeFromView(fileName,imageDimensions,context);
        }
            //Create worldfile only if it doesn't exist
            //Previous version was creating a new worldfile al the times an image was loaded
            worldFileHandler = new WorldFileHandler(fileName, this.alwaysLookForTFWExtension);
            if (worldFileHandler.isWorldFileExistentForImage() == null) {
                worldFileHandler.writeWorldFile(env, imageDimensions.x, imageDimensions.y);
                final File fil = new File(fileName);
                final String MSG = I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.message",fil.getName());
                context.getWorkbench().getFrame().setStatusMessage(MSG);
            }
         
        return env;
    }

  
    
    
    
}
