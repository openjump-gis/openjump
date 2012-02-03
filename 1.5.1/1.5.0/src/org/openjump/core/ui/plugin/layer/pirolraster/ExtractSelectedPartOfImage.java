/*
 * Created on 29.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: ExtractSelectedPartOfImage.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.PlugInContextTools;
import org.openjump.core.apitools.SelectionTools;
import org.openjump.core.rasterimage.CurrentLayerIsRasterImageLayerCheck;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * PlugIn that extracts a selected part (fence) of a raster image to a new raster image layer.<br>
 * Some parts were taken from Stefan Ostermann's SaveInterpolationAsImagePlugIn.
 *
 * @author Ole Rahn, (Stefan Ostermann)
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class ExtractSelectedPartOfImage extends AbstractPlugIn {
    
    public ExtractSelectedPartOfImage(){
        //super(new PersonalLogger(DebugUserIds.OLE));
    }

    /**
     *@inheritDoc
     */
    public String getIconString() {
        return "extractPart.png"; //$NON-NLS-1$
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.Extract-Selected-Part-Of-Image");
    }
    
    /**
     *@inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        
        if (rLayer==null){
            context.getWorkbenchFrame().warnUser(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected")); //$NON-NLS-1$
            return false;
        }
        
        Geometry fence = SelectionTools.getFenceGeometry(context);
        Envelope envWanted = fence.getEnvelopeInternal();
        
        BufferedImage partOfImageWanted = rLayer.getTileAsImage(envWanted);
        Raster partOfRasterWanted = rLayer.getTileAsRaster(envWanted); //[sstein 2 Aug 2010] need to add as we have now the image for display plus the data
        
        if (partOfImageWanted==null){
            context.getWorkbenchFrame().warnUser(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.fence-in-wrong-region"));
            return false;
        }
        
        boolean returnVal = this.putImageIntoMap(partOfImageWanted, partOfRasterWanted, envWanted, rLayer, context);
        
        return returnVal;
    }
    
    protected boolean putImageIntoMap(BufferedImage partOfImage, Raster partOfRaster, Envelope envelope, RasterImageLayer rLayer, PlugInContext context){
		if (partOfImage==null) return false;
	
		String newLayerName = context.getLayerManager().uniqueLayerName(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExtractSelectedPartOfImage.part-of") + rLayer.getName());
        
        RasterImageLayer newRasterLayer = new RasterImageLayer(newLayerName, context.getLayerManager(), partOfImage, partOfRaster, envelope);
		
		String catName = StandardCategoryNames.WORKING;
		
		try {
            catName = ((Category)context.getLayerNamePanel().getSelectedCategories().toArray()[0]).getName();
        } catch (RuntimeException e1) {}
        
        context.getLayerManager().addLayerable(catName, newRasterLayer);

        return true;
    }
    
    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {

        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        multiEnableCheck.add( checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class) );
        multiEnableCheck.add( checkFactory.createFenceMustBeDrawnCheck() );
        
        EnableCheck enableCheck = new CurrentLayerIsRasterImageLayerCheck(PlugInContextTools.getContext(workbenchContext));		
		multiEnableCheck.add(enableCheck);
        
        return multiEnableCheck;
	}
    
    public void initialize(PlugInContext context) throws Exception {}

}
