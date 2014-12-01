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

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;

import org.openjump.core.ui.plugin.view.helpclassescale.InstallShowScalePlugIn;
import org.openjump.core.ui.plugin.view.helpclassescale.ShowScaleRenderer;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * - initializes renderplugin - plugin calculates the actual scale and draws the
 * text (and a white rectangle around) in the map window all things are done in
 * ShowScaleRenderer
 * 
 * @author sstein
 */
public class ShowScalePlugIn extends AbstractPlugIn {
  public static final Icon ICON = IconLoader.icon("show_scale_text.png");

  public boolean execute(PlugInContext context) throws Exception {
    InstallShowScalePlugIn myInstallScalePlugIn = new InstallShowScalePlugIn();
    reportNothingToUndoYet(context);
    ShowScaleRenderer.setEnabled(
        !ShowScaleRenderer.isEnabled(context.getLayerViewPanel()),
        context.getLayerViewPanel());
    context.getLayerViewPanel().getRenderingManager()
        .render(ShowScaleRenderer.CONTENT_ID);

    return true;
  }

  @Override
  public EnableCheck getEnableCheck() {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance();
    final WorkbenchContext workbenchContext = JUMPWorkbench.getInstance()
        .getContext();
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
        new EnableCheck() {
          public String check(JComponent component) {
            ((JCheckBoxMenuItem) component).setSelected(ShowScaleRenderer
                .isEnabled(workbenchContext.getLayerViewPanel()));
            return null;
          }
        });
  }

}
