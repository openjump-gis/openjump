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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.CheckBoxed;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * pretty much {@link ToolboxPlugIn} but cleaned up plus propagating errors
 * from initializeToolbox() -&gt; getToolbox() -&gt; execute() making implementations
 * easier and more failsafe
 */
public abstract class ToolboxPlugInV2 extends AbstractPlugIn implements CheckBoxed {
  // just one toolbox instance for plugins switching the same toolbox
  private static HashMap toolboxMap = new HashMap<String,ToolboxDialog>();

  /**
   * @return the toolbox for this plug-in class.
   * @throws Exception if an Exception occurs during Toolbox initialization
   */
  public ToolboxDialog getToolbox() throws Exception {
    String name = getName();
    ToolboxDialog toolbox = (ToolboxDialog) toolboxMap.get(name);
    if (toolbox == null) {
      // allow implementation to return null or throw errors
      toolbox=initializeToolbox();
      toolbox.finishAddingComponents();
      toolboxMap.put(name, toolbox);
    }
    return toolbox;
  }

  protected abstract ToolboxDialog initializeToolbox() throws Exception;

  /**
   * Toolbox subclasses can override this method to implement their own
   * behaviour when the plug-in is called. Remember to call super.execute to
   * make the toolbox visible.
   */
  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    ToolboxDialog tb = getToolbox();
    tb.setVisible(!tb.isVisible());
    return true;
  }

  public EnableCheck getEnableCheck() {
    return new EnableCheck() {
      // switch checkbox menu item on/off, depending on current visibility 
      public String check(JComponent component) {
        //// do not initialize toolbox here, it delays menu opening!
        // JDialog tb = getToolbox();
        //// but assume not initialized/null equals not activated :)
        String name = getName();
        ToolboxDialog tb = (ToolboxDialog) toolboxMap.get(name);
        if (component instanceof JCheckBoxMenuItem)
          ((JCheckBoxMenuItem) component).setSelected(tb!=null && tb.isVisible());
        return null;
      }
    };
  }
}
