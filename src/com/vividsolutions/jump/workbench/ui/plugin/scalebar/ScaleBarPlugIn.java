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

package com.vividsolutions.jump.workbench.ui.plugin.scalebar;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ScaleBarPlugIn extends AbstractPlugIn {
  public static final Icon ICON = IconLoader.icon("show_scale.png");

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    ScaleBarRenderer.setEnabled(
        !ScaleBarRenderer.isEnabled(context.getLayerViewPanel()),
        context.getLayerViewPanel());
    context.getLayerViewPanel().getRenderingManager()
        .render(ScaleBarRenderer.CONTENT_ID);

    return true;
  }

  @Override
  public EnableCheck getEnableCheck() {
    EnableCheckFactory checkFactory = getContext().getCheckFactory();
    final WorkbenchContext workbenchContext = JUMPWorkbench.getInstance()
        .getContext();
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
        new EnableCheck() {
          public String check(JComponent component) {
            ((JCheckBoxMenuItem) component).setSelected(ScaleBarRenderer
                .isEnabled(workbenchContext.getLayerViewPanel()));
            return null;
          }
        });
  }

}
