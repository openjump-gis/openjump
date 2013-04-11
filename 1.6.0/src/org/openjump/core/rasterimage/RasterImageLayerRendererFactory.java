/*
 * Created on 04.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: RasterImageLayerRendererFactory.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.rasterimage;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.Renderer;
import com.vividsolutions.jump.workbench.ui.renderer.RendererFactory;

/**
 * TODO: comment class
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * 
 */
public class RasterImageLayerRendererFactory implements Renderer.ContentDependendFactory, RendererFactory<RasterImageLayer> {

    protected WorkbenchContext wbContext = null;
    
    public RasterImageLayerRendererFactory(WorkbenchContext wbContext) {
        super();
        this.wbContext = wbContext;
    }

    /**
     *@inheritDoc
     */
    public Renderer create(Object contentID) {
        return new RasterImageRenderer(contentID, this.wbContext.getLayerViewPanel());
    }
    
    // [mmichaud 2012-09-02] method added to conform to RendererFactory interface
    public RasterImageLayerRendererFactory() {}
    
    // [mmichaud 2012-09-02] method added to conform to RendererFactory interface
    public Renderer create(RasterImageLayer layer, LayerViewPanel panel, int maxFeatures) {
        return new RasterImageRenderer(layer, panel);
    }

}
