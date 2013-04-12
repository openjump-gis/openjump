
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */


package org.openjump.core.ui.plugin.raster;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

public class ProfileGraphPlugIn extends AbstractPlugIn
{
    ProfileGraphTool profileTool;    
    private final static String sErrorSeeOutputWindow= I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FPlugIn.Error-see-output-window");
    private String sName = "Create Thiessen Polygons";
    
    public void initialize(PlugInContext context) throws Exception
    {    
    
   		this.sName = I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphPlugIn.Profile-Graph");

        //context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addPlugIn(getIcon(), this, new MultiEnableCheck(), context.getWorkbenchContext());
        context.getFeatureInstaller().addMainMenuItem(
        		this, 
				new String[] {MenuNames.RASTER}, 
				this.sName + "...", 
				false, 
				null,
				createEnableCheck(context.getWorkbenchContext()));
        profileTool = new ProfileGraphTool();
   }
    
    public boolean execute(PlugInContext context) throws Exception
    {
        try
        {
            context.getLayerViewPanel().setCurrentCursorTool(profileTool);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        	context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame().getOutputFrame().addText("MeasureM_FPlugIn Exception:" + e.toString());
            return false;
        }
    }
   
    private Icon getIcon()
    {
        return new ImageIcon(getClass().getResource("RulerM_F.gif"));
    }  
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class));
    }
}

