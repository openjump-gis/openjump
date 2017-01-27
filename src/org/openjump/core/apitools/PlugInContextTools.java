/*
 * Created on 23.02.2005 for PIROL
 *
 * CVS header information:
 *  $RCSfile: PlugInContextTools.java,v $
 *  $Revision: 1.8 $
 *  $Date: 2005/07/21 09:56:55 $
 *  $Source: D:/CVS/cvsrepo/pirolPlugIns/utilities/PlugInContextTools.java,v $
 */
package org.openjump.core.apitools;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;

/**
 * Class to easily extract a PlugInContext object out of diverse kinds of objects.
 * 
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Revision: 1.8 $
 * 
 */
public class PlugInContextTools extends ToolToMakeYourLifeEasier {

    /**
     * Get the PlugIn context out of a toolbox.
     *@return the current plugIn context
     */
    public static PlugInContext getContext( ToolboxDialog toolbox ){
        return PlugInContextTools.getContext(toolbox.getContext());
    }
    
    /**
     * Get the PlugIn context out of a WorkbenchContext.
     *@return the current plugIn context
     */
    public static PlugInContext getContext( WorkbenchContext wbContext ){
        return wbContext.createPlugInContext();
    }
    
    /**
     * Get the PlugIn context out of a WorkbenchFrame.
     *@return the current plugIn context
     */
    public static PlugInContext getContext( WorkbenchFrame wbFrame ){
        return PlugInContextTools.getContext(wbFrame.getContext());
    }
    
    /**
     * Get the PlugIn context out of a PlugInContext (get a refreshed "snapshot").
     *@return the current plugIn context
     */
    public static PlugInContext getContext( PlugInContext oldContext ){
        /*
         * refresh old context ("a snapshot of the system for use by plug-ins") 
         */
        return PlugInContextTools.getContext(oldContext.getWorkbenchContext());
    }
    
    /**
     * Get the PlugIn context out of an AbstractCursorTool.
     *@return the current plugIn context
     */
    public static PlugInContext getContext( AbstractCursorTool tool ){
        return PlugInContextTools.getContext(tool.getWorkbench().getContext());
    }
    
    /**
     * Experimental: Get the PlugIn context out of an LayerViewPanel.
     *@return the current plugIn context
     */
    public static PlugInContext getContext( LayerViewPanel layerViewPanel ){
        // TODO: change to something bullet proof
        try {
            return  PlugInContextTools.getContext( ((WorkbenchFrame)layerViewPanel.getContext()) );
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (layerViewPanel.getCurrentCursorTool()!=null)
            return PlugInContextTools.getContext((AbstractCursorTool)layerViewPanel.getCurrentCursorTool());
        return null;
    }

}
