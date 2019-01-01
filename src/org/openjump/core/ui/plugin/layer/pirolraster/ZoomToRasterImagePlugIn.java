/*
 * Created on 04.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: ZoomToRasterImagePlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.util.Iterator;

import javax.swing.ImageIcon;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * TODO: comment class
 * 
 * @author Ole Rahn <br>
 * <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck, <br>
 *         Project: PIROL (2006), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $ [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class ZoomToRasterImagePlugIn extends AbstractPlugIn {

    public ZoomToRasterImagePlugIn() {
        // super(new PersonalLogger(DebugUserIds.OLE));
    }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.ZoomToRasterImagePlugIn.Zoom-To-Raster-Image");
    }

    /**
     * @inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {

        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        final WorkbenchContext wbcontext = context.getWorkbenchContext();
        Envelope envelope = new Envelope();
        if (rLayer == null) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return false;
        }
        /*
         * Giuseppe Aruta 2015-01-19 Expanded zoom capability. Now it is possible
         * to zoom to the whole extension of 2 or more selected RasterImageLayer
         */
        for (Iterator i = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).iterator(); i.hasNext();) {
            RasterImageLayer layer = (RasterImageLayer) i.next();
            envelope.expandToInclude(layer.getWholeImageEnvelope());

        }

        context.getLayerViewPanel().getViewport()
                .zoom(EnvelopeUtil.bufferByFraction(envelope, 0.03));

        /*
         * context.getLayerViewPanel().getViewport()
         * .zoom(rLayer.getWholeImageEnvelope());
         */

        return true;
    }

    public static final ImageIcon ICON = IconLoader.icon("zoom.gif");
}
