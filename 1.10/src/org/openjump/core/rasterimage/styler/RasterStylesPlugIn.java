package org.openjump.core.rasterimage.styler;

import org.openjump.core.rasterimage.styler.ui.NoDataValueDialog;
import org.openjump.core.rasterimage.styler.ui.RasterStylesDialog;

import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import java.util.List;

import javax.swing.JPopupMenu;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.styler.ui.GUIUtils;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;

public class RasterStylesPlugIn implements ThreadedPlugIn {

    @Override
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates. //NOI18N
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {

        /* Add item to pop-up menu, only for rasters */
        JPopupMenu menu = RasterImageContextMenu.getInstance(context);
        context.getFeatureInstaller().addPopupMenuPlugin(
                menu,
                this,
                getName(),
                false,
                IconLoader.icon("color_wheel.png"),
                createEnableCheck(context.getWorkbenchContext()));
        
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        
        RasterImageLayer rasterImageLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        RasterStylesDialog symbologyDialog;
        
        String bboardKey = GUIUtils.getBBKey(String.valueOf(rasterImageLayer.getUUID()));        
                
        if(context.getWorkbenchContext().getBlackboard().get(bboardKey) != null){            
            
            symbologyDialog = (RasterStylesDialog) context.getWorkbenchContext().getBlackboard().get(bboardKey);
            symbologyDialog.setLocationRelativeTo(context.getWorkbenchFrame());
            symbologyDialog.setVisible(true);
            
        } else {
            Double noDataValue;
            //check if getNoDataValue() method in RasterImageLayer exist
            try{

                noDataValue = rasterImageLayer.getNoDataValue();

            } catch (NoSuchMethodError e) {

                NoDataValueDialog noDataDialog = new NoDataValueDialog(context.getWorkbenchFrame(), true);
                noDataDialog.setLocationRelativeTo(context.getWorkbenchFrame());
                noDataDialog.setVisible(true);
                
                noDataValue = noDataDialog.getNoDataValue();
                
                if(noDataValue == null) {
                    return false;
                }
            }           
            
            symbologyDialog = new RasterStylesDialog(
                    context.getWorkbenchFrame(), true, context.getWorkbenchContext(),
                    rasterImageLayer, 0);
            symbologyDialog.setLocationRelativeTo(context.getWorkbenchFrame());
            symbologyDialog.setVisible(true);
                        
        }
        
        return false;
        
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class));
        multiEnableCheck.add(checkFactory.createRasterImageLayerExactlyNBandsMustExistCheck(1));

        return multiEnableCheck;
    }
    
    @Override
    public String getName() {
        return java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.RasterStylesExtension.Name");
    }
    
}
