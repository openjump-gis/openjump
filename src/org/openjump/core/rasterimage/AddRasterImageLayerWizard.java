package org.openjump.core.rasterimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.ccordsys.utils.ProjUtils;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.rasterimage.styler.SLDHandler;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;
import org.openjump.io.PropertiesHandler;
import org.openjump.util.metaData.MetaInformationHandler;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorV2Util;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

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

    private void addImage(WorkbenchContext context, Envelope envelope,
            Point imageDimensions) {

        if (context.getTask() == null) {
            context.getWorkbench().getFrame().addTaskFrame();
        }

        final String newLayerName = context.getLayerManager().uniqueLayerName(
                cachedLayer);

        String catName = StandardCategoryNames.WORKING;

        if (!context.createPlugInContext().getLayerNamePanel().getSelectedCategories().isEmpty()) {
            try {
                catName = ((Category) context.createPlugInContext()
                    .getLayerNamePanel().getSelectedCategories().toArray()[0])
                    .getName();
            } catch (final RuntimeException e1) {
                Logger.warn("AddRasterImageLayerWizard.addImage: " +
                    "error trying to get the name of the currently selected category", e1);
            }
        } else {
            Logger.warn("AddRasterImageLayerWizard.addImage: a category must be selected");
        }

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
       	// [Giuseppe Aruta 2024_08_19]
		// This part of code allows to read and apply a style to a RasterImageLayer.
		// The style must be stored as SLD file with the same name of the layer.
		if (rLayer.getNumBands() == 1) {// Currently OpenJUMP can read/write symbology only for
			// monoband raster files
			String sldS = new File(imageFileName).getAbsolutePath().replace("tif", "sld");
			File sldFile = new File(sldS);
			if (sldFile.exists() && !sldFile.isDirectory()) {
				try {
					RasterSymbology finalRasterSymbolizer = SLDHandler.read(sldFile);
					rLayer.setSymbology(finalRasterSymbolizer);
				} catch (Exception e) {
					Logger.error("cannot decode sld file: " + e);
				}
			}

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
            WorkbenchContext context) throws IOException,
            NoninvertibleTransformException {
        double minx, maxx, miny, maxy;
        Envelope env = null;

        worldFileHandler = new WorldFileHandler(fileName,
                alwaysLookForTFWExtension);

        if (imageDimensions == null) {
            // logger.printError("can not determine image dimensions");
            context.getWorkbench()
                    .getFrame()
                    .warnUser(
                            I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
            return null;
        }

        if (worldFileHandler.isWorldFileExistentForImage() != null) {
            // logger.printDebug(PirolPlugInMessages.getString("worldfile-found"));
            env = worldFileHandler.readWorldFile(imageDimensions.x,
                    imageDimensions.y);
        }

        if (env == null) {

            boolean isGeoTiff = false;

            if (fileName.toLowerCase().endsWith(".tif")
                    || fileName.toLowerCase().endsWith(".tiff")) {
                // logger.printDebug("checking for GeoTIFF");
                env = TiffUtilsV2.getEnvelope(new File(fileName));
                // TiffUtilsV2 returns an enveloppe even if no geotags are found
                // If env = image size, image is not considered as georeferenced
                isGeoTiff = env.getWidth() != imageDimensions.getX() ||
                    env.getHeight() != imageDimensions.getY();
            } else if (fileName.toLowerCase().endsWith(".flt")) {
                isGeoTiff = true;
                final GridFloat gf = new GridFloat(fileName);

                final Coordinate upperLeft = new Coordinate(gf.getXllCorner(),
                        gf.getYllCorner() + gf.getnRows() * gf.getCellSize());
                final Coordinate lowerRight = new Coordinate(gf.getXllCorner()
                        + gf.getnCols() * gf.getCellSize(), gf.getYllCorner());

                env = new Envelope(upperLeft, lowerRight);

            } else if (fileName.toLowerCase().endsWith(".asc")
                    || fileName.toLowerCase().endsWith(".txt")) {
                isGeoTiff = true;
                final GridAscii ga = new GridAscii(fileName);

                final Coordinate upperLeft = new Coordinate(ga.getXllCorner(),
                        ga.getYllCorner() + ga.getnRows() * ga.getCellSize());
                final Coordinate lowerRight = new Coordinate(ga.getXllCorner()
                        + ga.getnCols() * ga.getCellSize(), ga.getYllCorner());

                env = new Envelope(upperLeft, lowerRight);
            }

            if (!isGeoTiff) {
                // logger.printDebug(PirolPlugInMessages.getString("no-worldfile-found"));

                final Viewport viewport = context.getLayerViewPanel()
                        .getViewport();
                final Rectangle visibleRect = viewport.getPanel()
                        .getVisibleRect();

                // 015-04-10 [Giuseppe Aruta] Calculate local coordinates
                // as if the image is anchored to the view
                final File fil = new File(fileName);
                final BufferedImage bufImg = ImageIO.read(fil);
                final int width = bufImg.getWidth();
                final int height = bufImg.getHeight();
                final int visibleX1 = visibleRect.x;
                final int visibleY1 = visibleRect.y;
                final int visibleX2 = visibleX1 + width;// visibleRect.width;
                final int visibleY2 = visibleY1 + height;// visibleRect.height;
                final int visibleX3 = visibleX1 + visibleRect.width;
                final int visibleY3 = visibleY1 + visibleRect.height;
                final Coordinate upperLeftVisible = viewport
                        .toModelCoordinate(new Point(0, 0));
                final Coordinate lowerRightVisible1 = viewport
                        .toModelCoordinate(new Point(visibleX2, visibleY2));
                final Coordinate lowerRightVisible2 = viewport
                        .toModelCoordinate(new Point(visibleX3, visibleY3));

                context.getWorkbench()
                        .getFrame()
                        .warnUser(
                                I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
                final WizardDialog d = new WizardDialog(
                        context.getWorkbench().getFrame(),
                        I18N.getInstance().get(
                                "org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found-message",
                                fil.getName())
                        /*
                         * I18N.getInstance().get("RasterImagePlugIn.34") +
                         * this.worldFileHandler.getWorldFileName() +
                         * I18N.getInstance().get("RasterImagePlugIn.35")
                         */
                        , context.getErrorHandler());
                d.init(new WizardPanel[] { new RasterImageWizardPanel() });

                // 2015-04-10 [Giuseppe Aruta]wizard dialog now shows local
                // coordinates if the image is anchored to /local view
                RasterImageWizardPanel.minxTextField.setText(Double
                        .toString(upperLeftVisible.x));
                RasterImageWizardPanel.maxxTextField.setText(Double
                        .toString(lowerRightVisible1.x));
                RasterImageWizardPanel.minyTextField.setText(Double
                        .toString(upperLeftVisible.y));
                RasterImageWizardPanel.maxyTextField.setText(Double
                        .toString(lowerRightVisible1.y));

                // Set size after #init, because #init calls #pack. [Jon Aquino]
                d.setSize(700, 350);
                GUIUtil.centreOnWindow(d);
                d.setVisible(true);

                if (!d.wasFinishPressed()) {
                    // logger.printWarning("user canceled");
                    return null;
                }

                try {

                    if (RasterImageWizardPanel.warpCheckBox.isSelected()) {
                        env = new Envelope(upperLeftVisible.x,
                                lowerRightVisible2.x, upperLeftVisible.y,
                                lowerRightVisible2.y);
                    } else {

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
                } catch (final java.lang.NumberFormatException e) {

                    env = new Envelope(upperLeftVisible.x,
                            lowerRightVisible1.x, upperLeftVisible.y,
                            lowerRightVisible1.y);

                }

            }

            // creating world file
            worldFileHandler = new WorldFileHandler(fileName,
                    this.alwaysLookForTFWExtension);
            worldFileHandler.writeWorldFile(env, imageDimensions.x,
                    imageDimensions.y);
            final File fil = new File(fileName);
            final String MSG = I18N.getInstance().get(
                            "org.openjump.core.rasterimage.AddRasterImageLayerWizard.message",
                            fil.getName());
            context.getWorkbench().getFrame().setStatusMessage(MSG);

        }

        return env;
    }

}
