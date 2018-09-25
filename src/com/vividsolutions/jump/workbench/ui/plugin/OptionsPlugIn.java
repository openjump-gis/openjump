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

package com.vividsolutions.jump.workbench.ui.plugin;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;

import org.openjump.core.ui.DatasetOptionsPanel;
import org.openjump.core.ui.SelectionStyllingOptionsPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditOptionsPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import com.vividsolutions.jump.workbench.ui.OptionsPanelV2;
import com.vividsolutions.jump.workbench.ui.SnapVerticesToolsOptionsPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.network.ProxySettingsOptionsPanel;
import com.vividsolutions.jump.workbench.ui.snap.GridRenderer;

public class OptionsPlugIn extends AbstractPlugIn {
  
  public final static ImageIcon ICON = IconLoader.icon("fugue/wrench-screwdriver.png");
  private static OptionsPlugIn instance = null;

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    GUIUtil.centreOnWindow(dialog(context));
    dialog(context).setVisible(true);
    if (dialog(context).wasOKPressed()) {
      JInternalFrame[] frames = context.getWorkbenchFrame().getInternalFrames();
      for (int i = 0; i < frames.length; i++) {
        if (frames[i] instanceof LayerViewPanelProxy) {
          ((LayerViewPanelProxy) frames[i]).getLayerViewPanel()
              .getRenderingManager().render(GridRenderer.CONTENT_ID, true);
        }
      }
    }
    return dialog(context).wasOKPressed();
  }

  private static OptionsDialog dialog(PlugInContext context) {
    return OptionsDialog.instance(context.getWorkbenchContext().getWorkbench());
  }

  public void initialize(PlugInContext context) throws Exception {
    // don't double initialize
    if (instance != null)
      return;

    dialog(context).addTab(
        I18N.get("ui.plugin.OptionsPlugIn.view-edit"),
        GUIUtil.resize(IconLoader.icon("edit.gif"), 16),
        new EditOptionsPanel(PersistentBlackboardPlugIn.get(context.getWorkbenchContext())));
    dialog(context).addTab(
        I18N.get("ui.plugin.OptionsPlugIn.snap-vertices-tools"),
        GUIUtil.resize(IconLoader.icon("QuickSnap.gif"), 16),
        new SnapVerticesToolsOptionsPanel(context.getWorkbenchContext()
            .getWorkbench().getBlackboard()));
    // [Matthias Scholz 3. Sept 2010] SelectionStyllingOptionsPanel added
    dialog(context).addTab(I18N.get("ui.plugin.OptionsPlugIn.selection-style"),
        GUIUtil.resize(IconLoader.icon("Select.gif"), 16),
        new SelectionStyllingOptionsPanel(context.getWorkbenchContext()));
    // [Matthias Scholz 15. Sept 2010] DatasetOptionsPanel added
    dialog(context).addTab(I18N.get("ui.DatasetOptionsPanel.datasetOptions"),
        new DatasetOptionsPanel(context.getWorkbenchContext()));
    // add proxy panel
    OptionsPanelV2 proxypanel = ProxySettingsOptionsPanel.getInstance();
    dialog(context).addTab(proxypanel);
    
    instance = this;
  }

  // static execute method for usage in apple handler
  public static boolean execute(){
    if (instance!=null)
      return false;
    
    OptionsPlugIn p = new OptionsPlugIn();
    try {
      PlugInContext pc = JUMPWorkbench.getInstance().getContext().createPlugInContext();
      p.initialize(pc);
      return p.execute(pc);
    } catch (Exception e) {
      JUMPWorkbench.getInstance().getFrame().handleThrowable(e);
    }
    
    return false;
  }

  public Icon getIcon(int height) {
    // just one resolution for now 
    return ICON;
  }

}
