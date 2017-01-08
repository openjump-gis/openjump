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

package com.vividsolutions.jump.workbench.ui.toolbox;

import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public abstract class ToolboxPlugIn extends AbstractPlugIn {
  // just one toolbox instance for plugins switching the same toolbox
  private static HashMap toolboxMap = new HashMap<String,ToolboxDialog>();

  public ToolboxDialog getToolbox() {
    return getToolbox(JUMPWorkbench.getInstance().getContext());
  }

  /**
   * @return the toolbox for this plug-in class.
   */
  public ToolboxDialog getToolbox(WorkbenchContext context) {
    String name = getName();
    ToolboxDialog toolbox = (ToolboxDialog) toolboxMap.get(name);
    if (toolbox == null) {
      toolbox = new ToolboxDialog(context);
      toolbox.setTitle(name);
      initializeToolbox(toolbox);
      toolbox.finishAddingComponents();
      
      toolboxMap.put(name, toolbox);
    }
    return toolbox;
  }

  protected abstract void initializeToolbox(ToolboxDialog toolbox);

  /**
   * Toolbox subclasses can override this method to implement their own
   * behaviour when the plug-in is called. Remember to call super.execute to
   * make the toolbox visible.
   */
  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    getToolbox(context.getWorkbenchContext()).setVisible(
        !getToolbox(context.getWorkbenchContext()).isVisible());
    return true;
  }

  /**
   * Creates a menu item with a checkbox beside it that appears when the toolbox
   * is visible.
   * 
   * @param icon
   *          null to leave unspecified
   */
  public void createMainMenuItem(String[] menuPath, Icon icon,
      final WorkbenchContext context) throws Exception {
    new FeatureInstaller(context).addMainMenuPlugin(this, menuPath, getName()
        + "...", true, icon, getEnableCheck());
  }

  public EnableCheck getEnableCheck() {
    return new EnableCheck() {
      // switch checkbox menu item on/off, depending on current visibility 
      public String check(JComponent component) {
        JDialog tb = getToolbox();
        if (component instanceof JCheckBoxMenuItem)
          ((JCheckBoxMenuItem) component).setSelected(tb.isVisible());
        return null;
      }
    };
  }
}
