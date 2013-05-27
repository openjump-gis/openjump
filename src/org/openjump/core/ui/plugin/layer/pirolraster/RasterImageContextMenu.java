/*
 * Created on 18.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2446 $
 *  $Date: 2006-09-12 12:57:25 +0000 (Di, 12 Sep 2006) $
 *  $Id: RasterImageContextMenu.java 2446 2006-09-12 12:57:25Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.MenuElement;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn;

import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.TitledPopupMenu;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.MoveLayerablePlugIn;

/**
 * TODO: comment class
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2446 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class RasterImageContextMenu extends TitledPopupMenu {
    
    private static final long serialVersionUID = -8757500299734680615L;

    /** singleton */
    private static RasterImageContextMenu contextMenu = null;
    
    /**
     * use this method to get an instance of the context menu.
     * The menu will be instantiated when this method is called the first time.
     *@return an instance of the context menu
     */
    public static RasterImageContextMenu getInstance(PlugInContext context){
        if (contextMenu==null){
            contextMenu = new RasterImageContextMenu(context);
        }
        return contextMenu;
    }

    /**
     * @see #getInstance(PlugInContext)
     */
    private RasterImageContextMenu(PlugInContext context) {
        super();
        
        FeatureInstaller featureInstaller = context.getFeatureInstaller();
        final WorkbenchFrame wbFrame = context.getWorkbenchFrame();
        
        this.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                LayerNamePanel panel = ((LayerNamePanelProxy) wbFrame.getActiveInternalFrame())
                        .getLayerNamePanel();
                setTitle(((Layerable) panel.selectedNodes(RasterImageLayer.class).iterator().next()).getName());
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        
        
        ToggleRasterImageVisibility toggleRasterImageVisibility = new ToggleRasterImageVisibility(context.getWorkbenchContext());
        this.addPopupMenuListener(toggleRasterImageVisibility);
        featureInstaller.addPopupMenuItem(this,
                toggleRasterImageVisibility, toggleRasterImageVisibility.getName(),
                true, null, null);
        
        MenuElement[] elements = this.getSubElements();
        JCheckBoxMenuItem menuItem = null;
        
        for (int i=0; i<elements.length; i++){
            if ( JCheckBoxMenuItem.class.isInstance(elements[i]) ){
                if ( ((JCheckBoxMenuItem)elements[i]).getText().startsWith(toggleRasterImageVisibility.getName()) ){
                    ((JCheckBoxMenuItem)elements[i]).setSelected(true);
                    menuItem = (JCheckBoxMenuItem)elements[i];
                }
            }
        }
        
        toggleRasterImageVisibility.setMenuItem(menuItem);
        
        this.addSeparator(); // ===================
        /*
         * Giuseppe Aruta 2013_05_27 Add RasterImagePropertiesPlugIn
         */
        RasterImageLayerPropertiesPlugIn rasterImageLayerPropertiesPlugIn = new RasterImageLayerPropertiesPlugIn();
        featureInstaller.addPopupMenuItem(this,
        		rasterImageLayerPropertiesPlugIn , rasterImageLayerPropertiesPlugIn .getName() + "...", false,
                rasterImageLayerPropertiesPlugIn.getIcon(),
                null);
               
        ChangeRasterImagePropertiesPlugIn changeRasterImagePropertiesPlugIn = new ChangeRasterImagePropertiesPlugIn();
        featureInstaller.addPopupMenuItem(this,
                changeRasterImagePropertiesPlugIn, changeRasterImagePropertiesPlugIn.getName() + "...",
                false, null, null);
        
        ExtractSelectedPartOfImage extractPartPlugIn = new ExtractSelectedPartOfImage();
        featureInstaller.addPopupMenuItem(this,
                extractPartPlugIn, extractPartPlugIn.getName() + "...", false,
                //GUIUtil.toSmallIcon((ImageIcon) extractPartPlugIn.getIcon()),
                null,
                ExtractSelectedPartOfImage.createEnableCheck(context.getWorkbenchContext()));
        
        SaveRasterImageAsImagePlugIn saveRasterImageAsImagePlugIn = new SaveRasterImageAsImagePlugIn();
        featureInstaller.addPopupMenuItem(this, 
                saveRasterImageAsImagePlugIn, saveRasterImageAsImagePlugIn.getName() + "...",false,
                null,SaveRasterImageAsImagePlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        this.addSeparator(); // ===================
        
        ZoomToRasterImagePlugIn zoomToRasterImagePlugIn = new ZoomToRasterImagePlugIn();
        featureInstaller.addPopupMenuItem(this, 
                zoomToRasterImagePlugIn, zoomToRasterImagePlugIn.getName(),false,
                null,null);
        
        this.addSeparator(); // ===================
        
        WarpImageToFencePlugIn warpImageToFencePlugIn = new WarpImageToFencePlugIn();
        featureInstaller.addPopupMenuItem(this, 
                warpImageToFencePlugIn, warpImageToFencePlugIn.getName() + "...",false,
                null,WarpImageToFencePlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        ExportEnvelopeAsGeometryPlugIn exportEnvelopeAsGeometryPlugIn = new ExportEnvelopeAsGeometryPlugIn();
        featureInstaller.addPopupMenuItem(this, 
                exportEnvelopeAsGeometryPlugIn, exportEnvelopeAsGeometryPlugIn.getName(),false,
                null,null);
        
        this.addSeparator(); // ===================
        
        MoveLayerablePlugIn moveUpPlugIn = MoveLayerablePlugIn.UP;
        featureInstaller.addPopupMenuItem(this, moveUpPlugIn,
                moveUpPlugIn.getName() + "...", false, null, moveUpPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        MoveLayerablePlugIn moveDownPlugIn = MoveLayerablePlugIn.DOWN;
        featureInstaller.addPopupMenuItem(this, moveDownPlugIn,
                moveDownPlugIn.getName() + "...", false, null, moveDownPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        this.addSeparator(); // ===================
        
        CutSelectedRasterImageLayersPlugIn cutSelectedRasterImageLayersPlugIn = new CutSelectedRasterImageLayersPlugIn();
        featureInstaller.addPopupMenuItem(this,
                cutSelectedRasterImageLayersPlugIn, cutSelectedRasterImageLayersPlugIn.getName(), false, null,
                cutSelectedRasterImageLayersPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        CopySelectedRasterImageLayersPlugIn copySelectedRasterImageLayersPlugIn = new CopySelectedRasterImageLayersPlugIn();
        featureInstaller.addPopupMenuItem(this,
                copySelectedRasterImageLayersPlugIn, copySelectedRasterImageLayersPlugIn.getName(), false, null,
                copySelectedRasterImageLayersPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        PasteRasterImageLayersPlugIn pasteRasterImageLayersPlugIn = new PasteRasterImageLayersPlugIn();
        featureInstaller.addPopupMenuItem(this,
        		pasteRasterImageLayersPlugIn, pasteRasterImageLayersPlugIn.getName(), false, null,
        		pasteRasterImageLayersPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
        RemoveSelectedRasterImageLayersPlugIn removeSelectedLayersPlugIn = new RemoveSelectedRasterImageLayersPlugIn();
        featureInstaller.addPopupMenuItem(this,
                removeSelectedLayersPlugIn, removeSelectedLayersPlugIn.getName(), 
                false, null, removeSelectedLayersPlugIn.createEnableCheck(context.getWorkbenchContext()));
    }

}
