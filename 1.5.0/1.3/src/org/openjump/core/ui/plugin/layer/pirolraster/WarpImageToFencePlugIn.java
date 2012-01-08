/*
 * Created on 09.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: WarpImageToFencePlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.PlugInContextTools;
import org.openjump.core.apitools.SelectionTools;
import org.openjump.core.rasterimage.CurrentLayerIsRasterImageLayerCheck;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;


/**
 * PlugIn to warp a RasterImage to the bounding box of the Fence. 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class WarpImageToFencePlugIn extends AbstractPlugIn {


    public WarpImageToFencePlugIn() {
        //super(new PersonalLogger(DebugUserIds.OLE));
    }

    /**
     *@inheritDoc
     */
    public String getIconString() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.WarpImageToFencePlugIn.Warp-Image-To-Fence");
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
        
        rLayer.setEnvelope(envWanted);
        
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

}
