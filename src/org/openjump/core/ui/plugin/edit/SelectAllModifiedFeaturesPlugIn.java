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
 */

package org.openjump.core.ui.plugin.edit;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Selects all Features with the modified flag set
 * 
 * @author beckerl
 * 
 */
public class SelectAllModifiedFeaturesPlugIn extends AbstractPlugIn {

  public SelectAllModifiedFeaturesPlugIn() {
    super(I18N.getInstance().get("org.openjump.core.ui.plugin.edit.SelectAllModifiedFeaturesPlugIn.select-all-modified-features"));
    this.setShortcutKeys(KeyEvent.VK_A);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK+KeyEvent.SHIFT_MASK);
  }

  public void initialize(PlugInContext context) throws Exception {
    context
        .getFeatureInstaller()
        .addMainMenuPlugin(
            this,
            new String[] { MenuNames.EDIT, MenuNames.SELECTION },
            getName(),
            false, null, createEnableCheck(context.getWorkbenchContext()));
  }

  public boolean execute(final PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    List<Feature> selectedFeatures = new ArrayList<>();
    LayerViewPanel layerViewPanel = context.getWorkbenchContext()
        .getLayerViewPanel();
    layerViewPanel.getSelectionManager().clear();
    Collection<Layer> layers = context.getLayerManager().getLayers();
    for (Layer layer : layers) {
      selectedFeatures.clear();

      if (layer.isVisible()) {
        FeatureCollection featureCollection = layer
            .getFeatureCollectionWrapper();
        for (Feature feature : featureCollection.getFeatures()) {
          if (feature instanceof BasicFeature
              && ((BasicFeature) feature).isModified()) {
            selectedFeatures.add(feature);
          }
        }
      }
      if (selectedFeatures.size() > 0)
        layerViewPanel.getSelectionManager().getFeatureSelection()
            .selectItems(layer, selectedFeatures);
    }
    return true;
  }

  public MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
    return new MultiEnableCheck().add(checkFactory
        .createWindowWithLayerViewPanelMustBeActiveCheck());
  }
}
