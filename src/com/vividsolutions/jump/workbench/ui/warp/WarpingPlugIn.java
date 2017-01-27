
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.warp;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugIn;

public class WarpingPlugIn extends ToolboxPlugIn {

    public void initialize(PlugInContext context) throws Exception {
        createMainMenuItem(
            new String[] { MenuNames.TOOLS, MenuNames.TOOLS_WARP },
            GUIUtil.toSmallIcon(IconLoader.icon("GoalFlag.gif")),
            context.getWorkbenchContext());
    }

    protected void initializeToolbox(ToolboxDialog toolbox) {
        WarpingPanel warpingPanel = new WarpingPanel(toolbox);
        toolbox.getCenterPanel().add(warpingPanel, BorderLayout.CENTER);
        add(new DrawWarpingVectorTool(), false, toolbox, warpingPanel);
        add(new DeleteWarpingVectorTool(), false, toolbox, warpingPanel);
        toolbox.getToolBar().addSeparator();        
        add(new DrawIncrementalWarpingVectorTool(warpingPanel), true, toolbox, warpingPanel);
        add(new DeleteIncrementalWarpingVectorTool(warpingPanel), true, toolbox, warpingPanel);
        //Set y so it is positioned below Editing toolbox. [Jon Aquino]
        toolbox.setInitialLocation(new GUIUtil.Location(20, true, 175, false));   
    }

    private void add(
        CursorTool tool,
        final boolean incremental,
        ToolboxDialog toolbox,
        final WarpingPanel warpingPanel) {
        //Logic for enabling either the incremental-warping-vector tools or the warping-vector
        //tools, depending on whether the Warp Incrementally checkbox is selected or not. [Jon Aquino]
        toolbox.add(tool, new EnableCheck() {
            public String check(JComponent component) {
                if (incremental && warpingPanel.isWarpingIncrementally()) {
                    return null;
                }
                if (!incremental && !warpingPanel.isWarpingIncrementally()) {
                    return null;
                }
                return I18N.get("ui.warp.WarpingPlugIn.incremental-warping-must-be")+" " + (incremental ? I18N.get("ui.warp.WarpingPlugIn.enabled") : I18N.get("ui.warp.WarpingPlugIn.disabled"));
            }
        });
    }

}
