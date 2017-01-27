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
import java.util.SortedSet;
import java.util.TreeSet;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;

/**
 * Selects all Features which were not selected in layers where at least one
 * feature was selected.
 *
 * @author beckerl
 */
public class InvertSelectionPlugIn extends AbstractPlugIn {

    private String name = I18N
        .get("org.openjump.core.ui.plugin.edit.InvertSelectionPlugIn.invert-selection");

    public InvertSelectionPlugIn() {
        super();
        this.setShortcutKeys(KeyEvent.VK_I);
        this.setShortcutModifiers(KeyEvent.CTRL_MASK);
    }

    public void initialize(PlugInContext context) throws Exception {
    }

    public boolean execute(final PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        
        Collection<Feature> oldSelectedFeatures;
        Collection<Feature> newSelectedFeatures = new ArrayList<>();
        LayerViewPanel layerViewPanel = context.getWorkbenchContext().getLayerViewPanel();
        SelectionManager selectionManager = layerViewPanel.getSelectionManager();
        
        // Layers process
        Collection<Layer> layers = selectionManager.getLayersWithSelectedItems();
        for (Layer layer : layers) {
            // Invisible layers are just cleared
            newSelectedFeatures.clear();
            oldSelectedFeatures = selectionManager.getFeaturesWithSelectedItems(layer);
            selectionManager.getFeatureSelection().unselectItems(layer);
            if (layer.isVisible()) {
                // Get an ordered set of old selected identifiers
                SortedSet<Integer> ids = new TreeSet<>();
                for (Feature oldF : oldSelectedFeatures) {
                    ids.add(oldF.getID());
                }
                FeatureCollection featureCollection = layer.getFeatureCollectionWrapper();
                for (Feature feature : featureCollection.getFeatures()) {
                    if (!ids.contains(feature.getID())) {
                        newSelectedFeatures.add(feature);
                    }
                }
            }
            selectionManager.getFeatureSelection().selectItems(layer, newSelectedFeatures);
        }
        return true;
    }

    public MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
      return new MultiEnableCheck().add(
          checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
          checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
    }

    public String getName() {
      return name;
    }
}

