package org.openjump.core.rasterimage.styler;

import org.openjump.core.rasterimage.styler.ui.GUIUtils;
import org.openjump.core.rasterimage.styler.ui.RasterStylesDialog;
import org.openjump.core.rasterimage.styler.ui.RasterLegendDialog;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;

/**
 * Plugin for displaying the raster (ASC, FLT formats) legend. 
 * The menu is actived only is a raster layer is selected.
 * @author GeomaticaEAmbiente
 */
public class RasterLegendPlugIn implements ThreadedPlugIn {
    
    @Override
    public void initialize(PlugInContext context) throws Exception {
       
        /* Add item to pop-up menu, only for rasters */
        JPopupMenu menu = RasterImageContextMenu.getInstance(context);
        context.getFeatureInstaller().addPopupMenuPlugin(
                menu, 
                this, 
                getName(), 
                false, 
                null, 
                createEnableCheck(context.getWorkbenchContext()));
        
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        
        RasterImageLayer rasterImageLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        String bboardKey = GUIUtils.getBBKey(rasterImageLayer.getImageFileName());
        RasterStylesDialog symbologyDialog;
        RasterSymbology rasterStyler = null;
        double noDataValue = 0d;
        //Check if the RasterStyles of raster has been set.
        if(context.getWorkbenchContext().getBlackboard().get(bboardKey)!= null){            
            
            symbologyDialog = (RasterStylesDialog) context.getWorkbenchContext().getBlackboard().get(bboardKey);
            rasterStyler  = symbologyDialog.getFinalRasterSymbolizer();
            noDataValue = symbologyDialog.getNoDataValue();
        }
        
        if(rasterStyler != null){
            
            if(rasterStyler.getColorMapEntries_tm().size() > 40){
                JOptionPane.showMessageDialog(context.getWorkbenchFrame(), 
                        bundle.getString("LegendDialog.More40Colors.message"), 
                        RasterStylesExtension.extensionName, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            
            RasterLegendDialog legendDialog = new RasterLegendDialog(context.getWorkbenchFrame(), 
                    false, rasterStyler,noDataValue, rasterImageLayer.getName());
           
            legendDialog.setLocationRelativeTo(context.getWorkbenchFrame());
            legendDialog.setMinimumSize(new Dimension(200, 300));
            legendDialog.setPreferredSize(new Dimension(200, 400));
            legendDialog.setAlwaysOnTop(true);
            legendDialog.setVisible(true);
            

        }
        
        return false;
        
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
      
    @Override
    public String getName() {
        return bundle.getString("LegendPlugIn.PlugInName.text");
    }
    
    public Icon getIcon() {
        return IconLoader.icon("eye.png");
    }
    
    
    public static MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext); 
        multiEnableCheck.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck());
        multiEnableCheck.add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class) );
        multiEnableCheck.add(checkFactory.createRasterImageLayerExactlyNBandsMustExistCheck(1));
        
        multiEnableCheck.add(new EnableCheck() {

            @Override
            public String check(JComponent component) {
                
                RasterImageLayer ril = (RasterImageLayer) LayerTools.getSelectedLayerable(workbenchContext, RasterImageLayer.class);
                String bboardKey = GUIUtils.getBBKey(ril.getImageFileName());
                return (workbenchContext.getBlackboard().get(bboardKey) != null) ? null : "Sign";
            }
        });
//        
    return multiEnableCheck;
  }
    
//    private static String bboardKey;
    private final java.util.ResourceBundle bundle = 
            java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle"); // NOI18N

    

    
    
}
