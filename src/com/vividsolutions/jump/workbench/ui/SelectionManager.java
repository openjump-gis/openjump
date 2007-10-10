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

package com.vividsolutions.jump.workbench.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.ui.renderer.SelectionBackgroundRenderer;

/**
 * Provides aggregate information for selected features, parts, and linestrings.
 * Note that there is a difference between "selected features" and "features with
 * selected items": the former consists of wholly selected features; the latter,
 * wholly and partially selected features. To access a specific level of selection,
 * use #getFeatureSelection, #getPartSelection, or #getLineStringSelection.
 * "Parts" are components of GeometryCollections.
 * <p>
 * To get wholly selected features (i.e. not those that just have selected
 * parts or linestrings), use <code>getFeatureSelection().getFeaturesWithSelectedItems()</code>
 * <p>
 * To get features that are selected or have selected parts or linestrings,
 * use <code>getFeaturesWithSelectedItems()</code>
 * <p>
 * Yes there is a way to listen for selection events: LayerViewPanel#addListener.
 */
public class SelectionManager {
    private FeatureSelection featureSelection;
    private PartSelection partSelection;
    private LineStringSelection lineStringSelection;
    private LayerManagerProxy layerManagerProxy;
    private LayerViewPanel panel;

    /**
     * A feature may get split into two or more -- for example, if two linestrings of a feature
     * are selected. 
     */
    public Collection createFeaturesFromSelectedItems() {
        ArrayList newFeatures = new ArrayList();
        for (Iterator i = getLayersWithSelectedItems().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            newFeatures.addAll(createFeaturesFromSelectedItems(layer));
        }
        return newFeatures;
    }

    public Collection createFeaturesFromSelectedItems(Layer layer) {
        ArrayList newFeatures = new ArrayList();
        for (Iterator i = getFeaturesWithSelectedItems(layer).iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            for (Iterator j = getSelectedItems(layer, feature).iterator(); j.hasNext();) {
                Geometry item = (Geometry) j.next();
                Feature newFeature = (Feature) feature.clone();
                newFeature.setGeometry(item);
                newFeatures.add(newFeature);
            }
        }
        return newFeatures;
    }

    private boolean panelUpdatesEnabled = true;

    public SelectionManager(LayerViewPanel panel, LayerManagerProxy layerManagerProxy) {
        this.panel = panel;
        this.layerManagerProxy = layerManagerProxy;
        featureSelection = new FeatureSelection(this);
        lineStringSelection = new LineStringSelection(this);
        partSelection = new PartSelection(this);
        featureSelection.setParent(null);
        featureSelection.setChild(partSelection);
        partSelection.setParent(featureSelection);
        partSelection.setChild(lineStringSelection);
        lineStringSelection.setParent(partSelection);
        lineStringSelection.setChild(null);
        selections =
            Collections.unmodifiableList(
                Arrays.asList(
                    new Object[] { featureSelection, partSelection, lineStringSelection }));
        addLayerListenerTo(layerManagerProxy.getLayerManager());
    }

    private LayerListener layerListener = new LayerListener() {
        public void featuresChanged(FeatureEvent e) {
            if (e.getType() == FeatureEventType.DELETED) {
                unselectItems(e.getLayer(), e.getFeatures());
            }
            if (e.getType() == FeatureEventType.GEOMETRY_MODIFIED) {
                unselectFromFeaturesWithModifiedItemCounts(
                    e.getLayer(),
                    e.getFeatures(),
                    e.getOldFeatureClones());
            }
        }

        public void layerChanged(LayerEvent e) {
            if (!(e.getLayerable() instanceof Layer)) {
                return;
            }
            if (e.getType() == LayerEventType.REMOVED
                || e.getType() == LayerEventType.VISIBILITY_CHANGED) {
                unselectItems((Layer) e.getLayerable());
            }
        }

        public void categoryChanged(CategoryEvent e) {}
    };

    private void addLayerListenerTo(LayerManager layerManager) {
        layerManager.addLayerListener(layerListener);
    }

    public void clear() {
        boolean originalPanelUpdatesEnabled = arePanelUpdatesEnabled();
        setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = selections.iterator(); i.hasNext();) {
                AbstractSelection selection = (AbstractSelection) i.next();
                selection.unselectItems();
            }
        } finally {
            setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public FeatureSelection getFeatureSelection() {
        return featureSelection;
    }

    public LineStringSelection getLineStringSelection() {
        return lineStringSelection;
    }

    /**
     * @return AbstractSelections
     */
    public Collection getSelections() {
        return selections;
    }

    private List selections;
    
    /**
     * "items" rather than "geometries" because the user may have selected a part
     * of a Geometry (an element of a GeometryCollection or a ring of a Polygon).
     * @return a collection of Geometries
     */
    public Collection getSelectedItems() {
        ArrayList selectedItems = new ArrayList();
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            selectedItems.addAll(selection.getSelectedItems());
        }
        return selectedItems;
    }

    public Collection getSelectedItems(Layer layer) {
        ArrayList selectedItems = new ArrayList();
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            selectedItems.addAll(selection.getSelectedItems(layer));
        }
        return selectedItems;
    }

    public Collection getSelectedItems(Layer layer, Feature feature) {
        ArrayList selectedItems = new ArrayList();
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            selectedItems.addAll(selection.getSelectedItems(layer, feature));
        }
        return selectedItems;
    }

    /**
     * @param geometry the feature's Geometry or equivalent; that is, a clone or
     * similar enough Geometry from which Geometries can be retrieved using
     * the selection indices
     */
    public Collection getSelectedItems(Layer layer, Feature feature, Geometry geometry) {
        ArrayList selectedItems = new ArrayList();
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            selectedItems.addAll(selection.getSelectedItems(layer, feature, geometry));
        }
        return selectedItems;
    }

    public Collection getLayersWithSelectedItems() {
        HashSet layersWithSelectedItems = new HashSet();
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            layersWithSelectedItems.addAll(selection.getLayersWithSelectedItems());
        }
        return layersWithSelectedItems;
    }

    public PartSelection getPartSelection() {
        return partSelection;
    }

    public void updatePanel() {
        if (!panelUpdatesEnabled) {
            return;
        }
        panel.fireSelectionChanged();
        panel.getRenderingManager().render(SelectionBackgroundRenderer.CONTENT_ID);
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            panel.getRenderingManager().render(selection.getRendererContentID());
        }
    }

    public void setPanelUpdatesEnabled(boolean panelUpdatesEnabled) {
        this.panelUpdatesEnabled = panelUpdatesEnabled;
    }

    public Collection getFeaturesWithSelectedItems(Layer layer) {
        HashSet featuresWithSelectedItems = new HashSet();
        for (Iterator i = selections.iterator(); i.hasNext();) {
            AbstractSelection selection = (AbstractSelection) i.next();
            featuresWithSelectedItems.addAll(selection.getFeaturesWithSelectedItems(layer));
        }
        return featuresWithSelectedItems;
    }

    public void unselectItems(Layer layer) {
        boolean originalPanelUpdatesEnabled = arePanelUpdatesEnabled();
        setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = selections.iterator(); i.hasNext();) {
                AbstractSelection selection = (AbstractSelection) i.next();
                selection.unselectItems(layer);
            }
        } finally {
            setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectItems(Layer layer, Collection features) {
        boolean originalPanelUpdatesEnabled = arePanelUpdatesEnabled();
        setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = selections.iterator(); i.hasNext();) {
                AbstractSelection selection = (AbstractSelection) i.next();
                selection.unselectItems(layer, features);
            }
        } finally {
            setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectFromFeaturesWithModifiedItemCounts(
        Layer layer,
        Collection features,
        Collection oldFeatureClones) {
        boolean originalPanelUpdatesEnabled = arePanelUpdatesEnabled();
        setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = selections.iterator(); i.hasNext();) {
                AbstractSelection selection = (AbstractSelection) i.next();
                selection.unselectFromFeaturesWithModifiedItemCounts(
                    layer,
                    features,
                    oldFeatureClones);
            }
        } finally {
            setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public Collection getFeaturesWithSelectedItems() {
        ArrayList featuresWithSelectedItems = new ArrayList();
        for (Iterator i = getLayersWithSelectedItems().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            featuresWithSelectedItems.addAll(getFeaturesWithSelectedItems(layer));
        }
        return featuresWithSelectedItems;
    }

    public boolean arePanelUpdatesEnabled() {
        return panelUpdatesEnabled;
    }

    public void dispose() {
        layerManagerProxy.getLayerManager().removeLayerListener(layerListener);    
    }

}
