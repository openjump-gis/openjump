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

import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.Icon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditOptionsPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class AddNewLayerPlugIn extends AbstractPlugIn {
  public final Icon ICON = IconLoader.icon("famfam/page_white_add.png");
  
  public AddNewLayerPlugIn() {
    this.setShortcutKeys(KeyEvent.VK_L);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }

  public static FeatureCollection createBlankFeatureCollection() {
    FeatureSchema featureSchema = new FeatureSchema();
    featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    return new FeatureDataset(featureSchema);
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    Collection selectedCategories = context.getLayerNamePanel()
        .getSelectedCategories();
    Layer layer = context.addLayer(
            selectedCategories.isEmpty() ? StandardCategoryNames.WORKING
                : selectedCategories.iterator().next().toString(),
            I18N.get("ui.plugin.AddNewLayerPlugIn.new"),
            createBlankFeatureCollection());
    layer.setFeatureCollectionModified(false);
    if (context.getWorkbenchContext().getBlackboard().get(EditOptionsPanel.SINGLE_EDITABLE_LAYER_KEY, false)) {
      setAllLayersToUneditable(context);
    }
    layer.setEditable(true);
    ((EditingPlugIn) context.getWorkbenchContext().getBlackboard()
        .get(EditingPlugIn.KEY)).getToolbox(context.getWorkbenchContext())
        .setVisible(true);

    return true;
  }

  private void setAllLayersToUneditable(PlugInContext context) {
    for (Object object : context.getLayerNamePanel().getLayerManager().getLayerables(Layerable.class))  {
      ((Layerable)object).setEditable(false);
    }
  }

  @Override
  public EnableCheck getEnableCheck() {
    return EnableCheckFactory.getInstance()
        .createWindowWithLayerViewPanelMustBeActiveCheck();
  }

}
