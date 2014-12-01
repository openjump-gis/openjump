package org.openjump.core.ui.plugin.mousemenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToClickPlugIn;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import org.openjump.core.ui.images.IconLoader;

public class ZoomOutPlugIn extends AbstractPlugIn {
    
    public static final ImageIcon ICON = IconLoader.icon("zoom_out.png");
    
    public void initialize(PlugInContext context) throws Exception { 
    
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
  
        featureInstaller.addPopupMenuItem(popupMenu, this,
            new String[] {I18N.get("ui.MenuNames.ZOOM")}, 
            getName(),
            false, 
            GUIUtil.toSmallIcon(ICON),
            ZoomInPlugIn.createEnableCheck(workbenchContext));
    }
    
    public ZoomOutPlugIn(){
    }
    
    public boolean execute(final PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        new ZoomToClickPlugIn(0.5).execute(context);
        return true;
    }
    
    public String getName(){
        return I18N.get("org.openjump.core.ui.plugin.mousemenu.ZoomOutPlugIn"); 
    }
    
     
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(
            checkFactory.createWindowWithSelectionManagerMustBeActiveCheck()
        );
    }
    
}
