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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.warp.Triangulator;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.warp.WarpingVectorLayerFinder;

public class CopySelectedLayersToWarpingVectorsPlugIn extends AbstractPlugIn {

    public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1))
            .add(new EnableCheck() {
                public String check(JComponent component) {
                    return workbenchContext.getLayerNamePanel().getSelectedLayers().length == 1
                    && workbenchContext.getLayerNamePanel().getSelectedLayers()[0] == new WarpingVectorLayerFinder(workbenchContext).getLayer()
                    ? I18N.get("ui.plugin.CopySelectedLayersToWarpingVectorsPlugIn.a-layer-other-than")+"'" + new WarpingVectorLayerFinder(workbenchContext).getLayerName() + "' "+I18N.get("ui.plugin.CopySelectedLayersToWarpingVectorsPlugIn.must-be-selected") : null;
                }
            });
    }
    
    public static Collection removeNonVectorFeaturesAndWarn(Collection features, LayerViewPanelContext context) {
        ArrayList newFeatures = new ArrayList(features);
        Collection nonVectorFeatures = nonVectorFeatures(newFeatures);
        if (!nonVectorFeatures.isEmpty()) {
            context.warnUser(I18N.get("ui.plugin.CopySelectedLayersToWarpingVectorsPlugIn.skipped")+" " + nonVectorFeatures.size() + " non-two-point-linestring" + StringUtil.s(nonVectorFeatures.size()) + " e.g. " + ((Feature)nonVectorFeatures.iterator().next()).getGeometry().toText());
            newFeatures.removeAll(nonVectorFeatures);
        }    
        return newFeatures;
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        Collection newWarpingVectors = new ArrayList();
        Layer[] selectedLayers = context.getSelectedLayers();
        for (int i = 0; i < selectedLayers.length; i++) {
            if (selectedLayers[i] == new WarpingVectorLayerFinder(context).getLayer()) { continue; }
            newWarpingVectors.addAll(selectedLayers[i].getFeatureCollectionWrapper().getFeatures());
        }       
        newWarpingVectors = removeNonVectorFeaturesAndWarn(newWarpingVectors, context.getWorkbenchFrame());
        final Collection finalNewWarpingVectors = newWarpingVectors;
        final WarpingVectorLayerFinder finder = new WarpingVectorLayerFinder(context);
        execute(Layer.addUndo(finder.getLayerName(), context, new UndoableCommand(getName()) {
            public void execute() {
                if (finder.getLayer() == null) {
                    finder.createLayer();
                }
                finder.getLayer().getFeatureCollectionWrapper().addAll(
                    PasteItemsPlugIn.conform(
                        finalNewWarpingVectors,
                        finder.getLayer().getFeatureCollectionWrapper().getFeatureSchema()));
            }
            public void unexecute() {}
        }), context);
        return true;
    }

    private static Collection nonVectorFeatures(Collection candidates) {
        ArrayList nonVectorFeatures = new ArrayList();
        for (Iterator i = candidates.iterator(); i.hasNext(); ) {
            Feature candidate = (Feature) i.next();
            if (!Triangulator.vector(candidate.getGeometry())) { nonVectorFeatures.add(candidate); }
        }
        return nonVectorFeatures;
    }


}
