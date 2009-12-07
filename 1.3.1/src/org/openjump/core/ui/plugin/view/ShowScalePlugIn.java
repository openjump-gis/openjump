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
/*****************************************************
 * created:  original version by Vivid Solution
 * last modified:  03.06.2005
 * 
 * - initializes renderplugin
 * - plugin calculates the actual scale and draws the text
 *   (and a white rectangle around) in the map window
 *   all things are done in ShowScaleRenderer		
 *
 * @author sstein 
 * TODO how to put a mark on the menue item if tool is activated?
 *****************************************************/

package org.openjump.core.ui.plugin.view;

import org.openjump.core.ui.plugin.view.helpclassescale.InstallShowScalePlugIn;
import org.openjump.core.ui.plugin.view.helpclassescale.ShowScaleRenderer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
* - initializes renderplugin
* - plugin calculates the actual scale and draws the text
*   (and a white rectangle around) in the map window
*   all things are done in ShowScaleRenderer
* 
* @author sstein
*/ 
public class ShowScalePlugIn extends AbstractPlugIn {
	
	public void initialize(PlugInContext context) throws Exception {

	    context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
		        new String[]
				{MenuNames.VIEW},
				I18N.get("org.openjump.core.ui.plugin.view.ShowScalePlugIn.show-scale")+"{pos:14}", 
				false, 
				null, 
				createEnableCheck(context.getWorkbenchContext()));
	    InstallShowScalePlugIn myISSP = new InstallShowScalePlugIn();
	    myISSP.initialize(context);
	}
	
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        
        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    public boolean execute(PlugInContext context) throws Exception {
	    InstallShowScalePlugIn myInstallScalePlugIn = new InstallShowScalePlugIn();
        reportNothingToUndoYet(context);
        ShowScaleRenderer.setEnabled(!ShowScaleRenderer.isEnabled(
                context.getLayerViewPanel()), context.getLayerViewPanel());
        context.getLayerViewPanel().getRenderingManager().render(ShowScaleRenderer.CONTENT_ID);

        return true;
    }
}
