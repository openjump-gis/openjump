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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * Superclass for holding a user-selected collection of {@link Feature} items.
 */
// [mmichaud 2011-09-20] use generics and improve memory usage (initialize
// lists with size 1 only)
public abstract class AbstractSelection {
    
    private Map<Layer,Map<Feature,Set<Integer>>> layerMap = new HashMap<>();

    public abstract String getRendererContentID();

    private AbstractSelection child;

    private AbstractSelection parent;

    private SelectionManager selectionManager;

    public AbstractSelection(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    /**
     * Each selection implementation has the responsability to return the
     * list of items composing a parent geometry.
     */
    public abstract List<Geometry> items(Geometry geometry);

    /**
     * Returns items having these indices in geometry.
     * @param geometry geometry to analyse
     * @param indices indices of wanted geometries
     */
    public List<Geometry> items(Geometry geometry, Collection<Integer> indices) {
        List<Geometry> allItems = items(geometry);
        List<Geometry> items = new ArrayList<>(1);
        for (Integer index : indices) {
            items.add(allItems.get(index));
        }
        return items;
    }

    /**
     * Returns a mapping of each feature to indices of selected items in this
     * feature.
     * Note that some features in the map may not have selected items.
     */
    public Map<Feature,Set<Integer>> getFeatureToSelectedItemIndexCollectionMap(Layer layer) {
        if (!layerMap.containsKey(layer)) {
            layerMap.put(layer, new LinkedHashMap<Feature,Set<Integer>>());
        }
        return layerMap.get(layer);
    }

    /**
     * Returns indices of selected items in this feature.
     */
    public Set<Integer> getSelectedItemIndices(Layer layer, Feature feature) {
        Set<Integer> indices = getFeatureToSelectedItemIndexCollectionMap(layer).get(feature);
        return indices == null ? Collections.<Integer>emptySet() : indices;
    }

    /**
     * Returns a mapping of each feature to selected items in this feature.
     */
    public Map<Feature,List<Geometry>> getFeatureToSelectedItemCollectionMap(Layer layer) {
        Map<Feature,List<Geometry>> collectionMap = new LinkedHashMap<>();
        for (Feature feature : getFeatureToSelectedItemIndexCollectionMap(layer).keySet()) {
            Set<Integer> set = getSelectedItemIndices(layer, feature);
            if (set != null) {
                collectionMap.put(feature, items(feature.getGeometry(), set));
            }
        }
        return collectionMap;
    }

    public Collection<Layer> getLayersWithSelectedItems() {
        List<Layer> layersWithSelectedItems = new ArrayList<>();
        for (Layer layer : layerMap.keySet()) {
            if (!getFeaturesWithSelectedItems(layer).isEmpty()) {
                layersWithSelectedItems.add(layer);
            }
        }
        return layersWithSelectedItems;
    }

    public Collection<Feature> getFeaturesWithSelectedItems() {
        List<Feature> featuresWithSelectedItems = new ArrayList<>();
        for (Layer layer : layerMap.keySet()) {
            featuresWithSelectedItems.addAll(getFeaturesWithSelectedItems(layer));
        }
        return featuresWithSelectedItems;
    }

    public Collection<Feature> getFeaturesWithSelectedItems(Layer layer) {
        List<Feature> featuresWithSelectedItems = new ArrayList<>();
        for (Feature feature : getFeatureToSelectedItemIndexCollectionMap(layer).keySet()) {
            if (!getFeatureToSelectedItemIndexCollectionMap(layer).get(feature).isEmpty()) {
                featuresWithSelectedItems.add(feature);
            }
        }
        return featuresWithSelectedItems;
    }

    public Collection<Geometry> getSelectedItems() {
        ArrayList<Geometry> selectedItems = new ArrayList<>(1);
        for (Layer layer : layerMap.keySet()) {
            selectedItems.addAll(getSelectedItems(layer));
        }
        return selectedItems;
    }

    public Collection<Geometry> getSelectedItems(Layer layer) {
        List<Geometry> selectedItems = new ArrayList<>(1);
        for (Feature feature : getFeatureToSelectedItemIndexCollectionMap(layer).keySet()) {
            selectedItems.addAll(getSelectedItems(layer, feature));
        }
        return selectedItems;
    }

    public Collection<Geometry> getSelectedItems(Layer layer, Feature feature) {
        return getSelectedItems(layer, feature, feature.getGeometry());
    }

    /**
     * @param geometry the feature's Geometry or equivalent; that is, a clone or
     * similar enough Geometry from which Geometries can be retrieved using
     * the selection indices.
     */
    public Collection<Geometry> getSelectedItems(Layer layer, Feature feature, Geometry geometry) {
        Set<Integer> indices = getFeatureToSelectedItemIndexCollectionMap(layer).get(feature);
        if (indices == null) indices = Collections.emptySet();
        return items(geometry, indices);
    }

    /**
     * Returns indices of items in geometry.
     */
    public Set<Integer> indices(Geometry geometry, Collection<Geometry> items) {
        List<Geometry> allItems = items(geometry);
        Set<Integer> indices = new LinkedHashSet<>(1);
        for (Geometry item : items) {
            indices.add(allItems.indexOf(item));
        }
        return indices;
    }

    public void unselectItems(Layer layer, Map<Feature,List<Geometry>> featureToItemCollectionMap) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Feature feature : featureToItemCollectionMap.keySet()) {
                List<Geometry> items = featureToItemCollectionMap.get(feature);
                if (items != null) unselectItems(layer, feature, items);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void selectItems(Layer layer, Map<Feature,List<Geometry>> featureToItemCollectionMap) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Feature feature : featureToItemCollectionMap.keySet()) {
                List<Geometry> items = featureToItemCollectionMap.get(feature);
                if (items != null) selectItems(layer, feature, items);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void selectItems(Layer layer, Feature feature, Collection<Geometry> items) {
        Collection<Geometry> itemsToSelect = itemsNotSelectedInAncestors(layer, feature, items);
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            unselectInDescendants(layer, feature, itemsToSelect);
            Set<Integer> featureIndices = getFeatureToSelectedItemIndexCollectionMap(layer).get(feature);
            if (featureIndices == null) {
                featureIndices = new LinkedHashSet<Integer>(1);
                getFeatureToSelectedItemIndexCollectionMap(layer).put(feature, featureIndices);
            }
            Set<Integer> itemIndices = indices(feature.getGeometry(), itemsToSelect);
            if (itemIndices != null) {
                featureIndices.addAll(itemIndices);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectItems(Layer layer, Feature feature, Collection<Geometry> items) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            Set<Integer> itemsIndices = indices(feature.getGeometry(), items);
            Set<Integer> featureIndices = getFeatureToSelectedItemIndexCollectionMap(layer).get(feature);
            if (featureIndices != null && itemsIndices != null) {
                featureIndices.removeAll(itemsIndices);
                // [mmichaud 2013-01-10] remove feature from layerMap if no more item is selected
                if (featureIndices.isEmpty()) {
                    getFeatureToSelectedItemIndexCollectionMap(layer).remove(feature);
                }
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public Collection<Geometry> itemsNotSelectedInAncestors(Layer layer, Feature feature, Collection<Geometry> items) {
        ArrayList<Geometry> itemsNotSelectedInAncestors = new ArrayList<>(1);
        if (layer.isSelectable()) {
            for (Geometry item : items) {
                if (!selectedInAncestors(layer, feature, item)) {
                    itemsNotSelectedInAncestors.add(item);
                }
            }
        }
        return itemsNotSelectedInAncestors;
    }

    protected abstract boolean selectedInAncestors(Layer layer, Feature feature, Geometry item);

    protected abstract void unselectInDescendants(Layer layer, Feature feature, Collection<Geometry> items);

    public void selectItems(Layer layer, Feature feature) {
        selectItems(layer, feature, items(feature.getGeometry()));
    }

    public void selectItems(Layer layer, Collection features) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Object object : features) {
                Feature feature = (Feature) object;
                selectItems(layer, feature);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectFromFeaturesWithModifiedItemCounts(
        Layer layer,
        Collection<Feature> features,
        Collection<Feature> oldFeatureClones) {
        List<Feature> featuresToUnselect = new ArrayList<>(1);
        Iterator j = oldFeatureClones.iterator();
        j.hasNext();
        for (Feature feature : features) {
            Feature oldFeatureClone = (Feature) j.next();
            if (items(feature.getGeometry()).size()
                != items(oldFeatureClone.getGeometry()).size()) {
                featuresToUnselect.add(feature);
            }
        }
        unselectItems(layer, featuresToUnselect);
    }

    public void unselectItems() {
        layerMap.clear();
        updatePanel();
    }

    public void unselectItems(Layer layer) {
        layerMap.remove(layer);
        updatePanel();
    }

    public void unselectItems(Layer layer, Collection features) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Object object : features) {
                unselectItems(layer, (Feature)object);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectItems(Layer layer, Feature feature) {
        getFeatureToSelectedItemIndexCollectionMap(layer).remove(feature);
        // [mmichaud 2011-09-24 : fix 2792806]
        //updatePanel();
    }

    public void unselectItem(Layer layer, Feature feature, int selectedItemIndex) {
        Set<Integer> indices = getFeatureToSelectedItemIndexCollectionMap(layer).get(feature);
        if (indices != null) {
            indices.remove(selectedItemIndex);
            if (indices.isEmpty()) {
                getFeatureToSelectedItemIndexCollectionMap(layer).remove(feature);
            }
        }
    }

    private void updatePanel() {
        selectionManager.updatePanel();
    }

    public void setChild(AbstractSelection child) {
        this.child = child;
    }

    public void setParent(AbstractSelection parent) {
        this.parent = parent;
    }

    protected AbstractSelection getChild() {
        return child;
    }

    protected AbstractSelection getParent() {
        return parent;
    }

}
