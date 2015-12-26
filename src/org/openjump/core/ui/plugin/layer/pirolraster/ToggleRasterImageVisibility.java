/*
 * Created on 04.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: ToggleRasterImageVisibility.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * TODO: comment class
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
public class ToggleRasterImageVisibility extends AbstractPlugIn implements PopupMenuListener {

    protected WorkbenchContext wbContext = null;
    
    protected JCheckBoxMenuItem menuItem = null;
    
    public ToggleRasterImageVisibility(WorkbenchContext wbContext) {
        //super(new PersonalLogger(DebugUserIds.OLE));
        this.wbContext = wbContext;
    }

    /**
     *@inheritDoc
     */
    public String getIconString() {
        return "visibility.png";
    }

    /**
     *@inheritDoc
     */
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ToggleRasterImageVisibility.Toggle-Raster-Image-Visibility");
    }

    
    /**
     *@inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        
        if (rLayer==null){
            context.getWorkbenchFrame().warnUser(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return false;
        }
        
        rLayer.setVisible(!rLayer.isVisible());
        
        rLayer.fireAppearanceChanged();
        
        return true;
    }

    /**
     *@param arg0
     */
    public void popupMenuCanceled(PopupMenuEvent arg0) {
    }

    /**
     *@param arg0
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
    }

    /**
     *@param arg0
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(this.wbContext.createPlugInContext(), RasterImageLayer.class);
        
        if (rLayer==null) return;
        
        if (this.menuItem!=null)
            menuItem.setSelected(rLayer.isVisible());
        
        
    }

    public JCheckBoxMenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(JCheckBoxMenuItem menuItem) {
        this.menuItem = menuItem;
    }

    
}
