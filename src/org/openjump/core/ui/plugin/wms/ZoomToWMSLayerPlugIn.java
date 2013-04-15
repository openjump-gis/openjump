package org.openjump.core.ui.plugin.wms;

import java.util.Iterator;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.wms.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.util.*;

import org.apache.log4j.Logger;

public class ZoomToWMSLayerPlugIn extends AbstractPlugIn {
    
    private static Logger LOG = Logger.getLogger(AbstractParser.class);
    
    PlugInContext context;

    //final WorkbenchContext wbcontext = null;
    
    public void initialize( PlugInContext context ) throws Exception {
        this.context = context;
        final WorkbenchContext wbcontext = context.getWorkbenchContext();
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory( context
            .getWorkbenchContext() );


        EnableCheck enableCheck = new MultiEnableCheck()
            .add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck());

        context.getFeatureInstaller()
			.addMainMenuItem( this, new String[] { MenuNames.VIEW },
                I18N.get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer" )
                    + "{pos:9}", false, null, enableCheck );
        // Add PlugIn to WMSPopupMenu
        context.getFeatureInstaller().addPopupMenuItem(
            context.getWorkbenchFrame().getWMSLayerNamePopupMenu(), this,
            I18N.get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer" ), false,
            null, enableCheck );
        
    }

    
    public boolean execute( PlugInContext context ) throws Exception {
        this.context = context;
        boolean isSIDLayer = false;
        final WorkbenchContext wbcontext = context.getWorkbenchContext();
        Envelope envelope = new Envelope();
        String srs = null;
        for (Iterator i = wbcontext.getLayerNamePanel().selectedNodes(WMSLayer.class).iterator(); i.hasNext();) {
            WMSLayer layer = (WMSLayer) i.next();
            envelope.expandToInclude(layer.getEnvelope());
            srs = layer.getSRS();
        }
        if (envelope.getWidth() == 0.0 && envelope.getHeight() == 0.0) {
            context.getWorkbenchFrame().warnUser("No Bounding Box Available for " + srs);
            return false;
        }
        LOG.info("Zoom to " + wbcontext.getLayerNamePanel().selectedNodes(WMSLayer.class) + " : "  + envelope);
        context.getLayerViewPanel().getViewport().zoom(EnvelopeUtil.bufferByFraction(envelope, 0.03));
        return true;
    }
    
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.wms.ZoomToWMSLayerPlugIn.zoom-to-wms-layer");
    }

}