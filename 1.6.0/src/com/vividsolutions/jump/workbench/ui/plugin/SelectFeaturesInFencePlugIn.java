
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;


public class SelectFeaturesInFencePlugIn extends AbstractPlugIn {
    public SelectFeaturesInFencePlugIn() {
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
      context.getFeatureInstaller().addMainMenuPlugin(this,
          new String[] { MenuNames.EDIT, MenuNames.SELECTION }, getName(), false,
          null, createEnableCheck(context.getWorkbenchContext()));
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        context.getLayerViewPanel().getSelectionManager().clear();
        execute(context.getLayerViewPanel(), context.getLayerNamePanel(),
            context.getLayerViewPanel().getFence(), true, false);

        return true;
    }

    public static void execute(
        LayerViewPanel layerViewPanel,
        LayerNamePanel layerNamePanel,
        Geometry fence,
        boolean skipUnselectedLayers,
        boolean mentionModifierHelp) {
        Collection selectedLayers = Arrays.asList(layerNamePanel.getSelectedLayers());
        Map layerToFeaturesInFenceMap = layerViewPanel.visibleLayerToFeaturesInFenceMap(fence);

        for (Iterator i = layerToFeaturesInFenceMap.keySet().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();

            if (layer == new FenceLayerFinder(layerViewPanel).getLayer()) {
                continue;
            }

            if (skipUnselectedLayers && !selectedLayers.contains(layer)) {
                continue;
            }

            layerViewPanel.getSelectionManager().getFeatureSelection().selectItems(
                layer,
                (Collection) layerToFeaturesInFenceMap.get(layer));
        }

    }

    public static MultiEnableCheck createEnableCheck(
        WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
                                     .add(checkFactory.createFenceMustBeDrawnCheck())
                                     .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(
                1));
    }
}
