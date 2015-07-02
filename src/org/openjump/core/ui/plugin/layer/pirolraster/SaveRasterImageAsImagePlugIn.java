/*
 * Created on 29.06.2005 for Pirol
 *
 * SVN header information:
 * $Author: LBST-PF-3\orahn $
 * $Rev: 2509 $
 * $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 * $Id: SaveRasterImageAsImagePlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.io.File;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageIOUtils;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * This PlugIn saves a RasterImages to disk with its geographical position. This
 * class is based on Stefan Ostermanns SaveInterpolationAsImagePlugIn.
 * 
 * @author Ole Rahn, Stefan Ostermann, <br>
 * <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck, <br>
 *         Project: PIROL (2005), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $ [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 * @version $Rev: 4347 $ [Giuseppe Aruta] - 22.Mar.2015 - rewrite class using
 *          new RasterImage I/O components. This version allows to export no
 *          data cell value to the output tif
 * @version $Rev: 4348 $ [Giuseppe Aruta] - 22.Mar.2015 - add export to ASC and
 *          FLT
 */
public class SaveRasterImageAsImagePlugIn extends AbstractPlugIn {

    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class
            .getName() + " - FILE CHOOSER DIRECTORY";

    private static String ERROR = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window");
    private static String PLUGINNAME = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.SaveRasterImageAsImagePlugIn.Save-Raster-Image-As-Image");
    private static String SAVED = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private static HashMap extensions;

    public static final String TIF_EXTENSION = "TIF";
    private static File file;
    private static File fileHDR;

    public SaveRasterImageAsImagePlugIn() {
        this.extensions = new HashMap();
        this.extensions.put("ASC", "ASC");
        this.extensions.put("FLT", "FLT");
        this.extensions.put("TIF", "TIF");
    }

    @Override
    public String getName() {
        return PLUGINNAME;
    }

    public static final ImageIcon ICON = IconLoader.icon("disk_dots.png");

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        saveSingleRaster(context);

        return true;

    }

    public static void saveSingleRaster(PlugInContext context) {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        Envelope env = rLayer.getWholeImageEnvelope();
        int bands = rLayer.getNumBands();
        JFileChooser fileChooser = GUIUtil
                .createJFileChooserWithOverwritePrompting();
        fileChooser.setDialogTitle(PLUGINNAME);

        if (PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).get(
                FILE_CHOOSER_DIRECTORY_KEY) != null) {
            fileChooser.setCurrentDirectory(new File(
                    (String) PersistentBlackboardPlugIn.get(
                            context.getWorkbenchContext()).get(
                            FILE_CHOOSER_DIRECTORY_KEY)));
        }

        fileChooser.setMultiSelectionEnabled(false);
        if (bands == 1) {

            fileChooser.setFileFilter(GUIUtil.createFileFilter("Raster grid",
                    new String[] { "asc", "flt", "tif" }));
        } else {
            // Restricted export to "gif" and "png" for ImageIO-OpenJDK. Other
            // format are accepted if relative encoders are installed through
            // imageIO-ext
            fileChooser.setFileFilter(GUIUtil.createFileFilter("Raster image",
                    new String[] { "tif" }));

        }

        int option;

        option = fileChooser.showSaveDialog(context.getWorkbenchFrame());

        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            fileHDR = fileChooser.getSelectedFile();
            file = FileUtil.addExtensionIfNone(file, "tif");
            String extension = FileUtil.getExtension(file);
            fileHDR = FileUtil.removeExtensionIfAny(fileHDR);
            fileHDR = FileUtil.addExtensionIfNone(fileHDR, "hdr");
            String catName = StandardCategoryNames.WORKING;
            try {
                catName = ((Category) context.getLayerNamePanel()
                        .getSelectedCategories().toArray()[0]).getName();
            } catch (RuntimeException e1) {
            }

            int band;

            band = 0;

            try {
                String trueExtension = (String) extensions.get(extension
                        .toUpperCase());
                if (trueExtension.equalsIgnoreCase("ASC")) {
                    RasterImageIOUtils.saveASC(file, context, rLayer, band);
                    RasterImageIOUtils.loadASC(file, context, catName);

                } else if (trueExtension.equalsIgnoreCase("FLT")) {
                    RasterImageIOUtils.saveFLT(file, context, rLayer, band);
                    RasterImageIOUtils.saveHDR(fileHDR, context, rLayer);
                    RasterImageIOUtils.loadFLT(file, context, catName);

                } else if (trueExtension.equalsIgnoreCase("TIF")) {
                    RasterImageIOUtils.saveTIF(file, rLayer, env);
                    RasterImageIOUtils.loadTIF(file, context, catName);
                }
            } catch (Exception e) {
                context.getWorkbenchFrame().warnUser(ERROR);
                context.getWorkbenchFrame().getOutputFrame()
                        .createNewDocument();
                context.getWorkbenchFrame()
                        .getOutputFrame()
                        .addText(
                                "SaveImageToRasterPlugIn Exception:"
                                        + new Object[] { e.toString() });
                return;
            }

            //Giuseppe Aruta July 2 2015
            //Since this plugin now export to different raster format
            //It is more convinient to load the new file instead to
            //substitute the old with the new. User will decide if to save it to (or to
            //remove it from) the project
            // rLayer.setImageFileName(file.getPath());
            rLayer.setNeedToKeepImage(false);
            context.getWorkbenchFrame().setStatusMessage(SAVED);

        }

        return;

    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        multiEnableCheck.add(checkFactory
                .createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));

        return multiEnableCheck;
    }

}