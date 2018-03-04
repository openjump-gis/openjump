/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 * Stefan Steiniger
 * perriger@gmx.de
 */

package org.openjump.core.ui.plugin.edit;

import org.openjump.core.ui.plugin.edit.helpclassesselection.DrawFenceTool;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;


/**
 * Selects items within a fence of the actual selected layers
 * and informs about the number of selected items.
 * 
 * @author sstein
 *
 * created:  16.05.2005
 *
 */
public class SelectItemsByFenceFromSelectedLayersPlugIn extends AbstractPlugIn{    

    public void initialize(PlugInContext context) throws Exception {
		    context.getFeatureInstaller().addMainMenuPlugin(this,
		        new String[] {MenuNames.EDIT, MenuNames.SELECTION},
				I18N.get("org.openjump.core.ui.plugin.edit.SelectItemsByFenceFromSelectedLayersPlugIn.select-features-by-polygon-from-selected-layers"), 
				false, null,
				createEnableCheck(context.getWorkbenchContext()));
		}
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);        
        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));        
    }
        
	public boolean execute(PlugInContext context) throws Exception{
	    
		this.reportNothingToUndoYet(context);
        try
        {
            CursorTool polyTool = DrawFenceTool.create((LayerNamePanelProxy) context.getActiveInternalFrame(), context);
            context.getLayerViewPanel().setCurrentCursorTool(polyTool);
        }
        catch (Exception e)
        {
            context.getWorkbenchFrame().warnUser("DrawFenceTool Exception:" + e.toString());
            return false;
        }

		System.gc();		
	    return true;
	}

     
}
