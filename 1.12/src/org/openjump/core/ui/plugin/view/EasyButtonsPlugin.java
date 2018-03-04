/*
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2009 Integrated Systems Analysts, Inc.
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
 * Fort Walton Beach, Florida 32548
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.view;

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugIn;

public class EasyButtonsPlugin extends ToolboxPlugIn {

  static final String TOOLBOX_NAME = I18N
      .get("org.openjump.core.ui.plugin.view.EasyButtonsPlugin.EZ-Buttons");
  private static final ImageIcon ICON = IconLoader.icon("fugue/keyboard-smiley.png");

  //private JPopupMenu popup = new JPopupMenu();
  private EasyPanel buttonPanel = null;

  public void initialize(final PlugInContext context) throws Exception {
    createMainMenuItem(new String[] { MenuNames.CUSTOMIZE }, getIcon(),
        context.getWorkbenchContext());
    // Wait 2 seconds because EasyButtonsPlugIn needs all menu items
    // to be initialized first
    // Initialization is done here rather than in initializeToolbox because
    // we want to be able to use EZKeys just after OpenJUMP initialization
    // (and before the first use of the plugin)
    new Thread() {
      @Override public void run() {
        try {
          Thread.sleep(2000);
          initializeToolbox(getToolbox(context.getWorkbenchContext()));
        } catch(InterruptedException e) {
          Logger.warn("Could not initialize EasyButtonsPlugin", e);
        }
      }
    }.start();

  }

  public String getName() {
    return TOOLBOX_NAME;
  }

  public Icon getIcon() {
    return ICON;
  }

  protected void initializeToolbox(ToolboxDialog toolbox) {
    if (buttonPanel != null) return;
    buttonPanel = new EasyPanel(toolbox);
    toolbox.getCenterPanel().add(buttonPanel, BorderLayout.CENTER);
    toolbox.setInitialLocation(new GUIUtil.Location(10, true, 10, true));
    toolbox.setResizable(false);
    try {
      toolbox.setIconImage(ICON.getImage());
    } catch (NoSuchMethodError e) {
      // IGNORE: this is 1.5 missing setIconImage()
    }
  }

  private void add(CursorTool tool, final boolean incremental,
      ToolboxDialog toolbox, final EasyPanel warpingPanel) {
    toolbox.add(tool, new EnableCheck() {
      public String check(JComponent component) {
        return null;
      }
    });
  }

}
