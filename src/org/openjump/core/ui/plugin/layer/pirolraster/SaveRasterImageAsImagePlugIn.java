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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.WorldFileHandler;

import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.TIFFCodec;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GenericNames;

/**
 * This PlugIn saves a RasterImages to disk with its geographical position.
 * This class is based on Stefan Ostermanns SaveInterpolationAsImagePlugIn.
 * 
 * @author Ole Rahn, Stefan Ostermann,
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class SaveRasterImageAsImagePlugIn extends AbstractPlugIn {
    protected static final String TIFENDING = ".tif";
	protected static final String GEOENDING = ".tfw";
	
	//private static Logger logger = new PersonalLogger(DebugUserIds.OLE);
    private Properties properties = null;
    private static String propertiesFile = LoadSextanteRasterImagePlugIn.getPropertiesFile();
    private String lastPath;
	
    public SaveRasterImageAsImagePlugIn(){
        //super(SaveRasterImageAsImagePlugIn.logger);
    }
    
	/**
	 *@inheritDoc
	 */
	public boolean execute(PlugInContext context) throws Exception {
		BufferedImage image;
		/* standard Java save-dialog: */
		JFileChooser fc = new JFileChooser();
		
		fc.setFileFilter(new FileFilter() {
				            public boolean accept(File f) {
				                return f.isDirectory()
				                        || f.getName().toLowerCase().endsWith(TIFENDING);
				            }
				
				            public String getDescription() {
				                return "TIFF Image";
				            }
				        }
					);
		
		this.properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream(SaveRasterImageAsImagePlugIn.propertiesFile);
            this.properties.load(fis);
            this.lastPath = this.properties.getProperty(LoadSextanteRasterImagePlugIn.KEY_PATH);
            fis.close();
        } catch (FileNotFoundException e) {
            //SaveRasterImageAsImagePlugIn.logger.printDebug(e.getMessage());
        	context.getWorkbenchFrame().warnUser(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.SaveRasterImageAsImagePlugIn.File-not-found"));
        } catch (IOException e) {
            //SaveRasterImageAsImagePlugIn.logger.printDebug(e.getMessage());
        	context.getWorkbenchFrame().warnUser(GenericNames.ERROR);
        }
        
        if (this.lastPath != null){
            fc.setCurrentDirectory(new File(this.lastPath));
        }
        fc.setMultiSelectionEnabled(false);
		
		fc.setDialogTitle(this.getName());
		int returnVal = fc.showSaveDialog(fc);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String tifFileName = fc.getSelectedFile().getAbsolutePath();
			
			if (!tifFileName.toLowerCase().endsWith(TIFENDING.toLowerCase())){
			    tifFileName = tifFileName + TIFENDING;
			}
			
			File tifFile = new File(tifFileName);

			FileOutputStream tifOut = new FileOutputStream(tifFile);

			
			/* save tif image: */
            RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class); 
			image = rLayer.getImage().getAsBufferedImage();
			TIFFEncodeParam param = new TIFFEncodeParam();
			param.setCompression(TIFFEncodeParam.COMPRESSION_NONE);
			TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec.createImageEncoder("tiff", tifOut, param);
			encoder.encode(image);
			tifOut.close();
			
			/* save geodata: */
			Envelope envelope = rLayer.getEnvelope();


			WorldFileHandler worldFileHandler = new WorldFileHandler(tifFileName, false);
			worldFileHandler.writeWorldFile(envelope, image.getWidth(), image.getHeight());
	        
	        // Switch RAM mode of the RasterImage
            rLayer.setImageFileName(tifFileName);
            rLayer.setNeedToKeepImage(false);
	        
	        
		}
		return true;

	}

	/**
	 *@inheritDoc
	 */
	public void initialize(PlugInContext context) throws Exception {}
	
	public static MultiEnableCheck createEnableCheck(
			final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(
				workbenchContext);
		MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add( checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class) );

		return multiEnableCheck;
	}
	
    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.SaveRasterImageAsImagePlugIn.Save-Raster-Image-As-Image");
    }
    
    /**
     *@inheritDoc
     */
    public String getIconString() {
        return null;
    }
}
