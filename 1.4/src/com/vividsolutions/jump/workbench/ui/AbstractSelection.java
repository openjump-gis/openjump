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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.workbench.model.Layer;

    /**
     * Superclass for holding a user-selected collection of {@link
     * Feature} items.
     */

public abstract class AbstractSelection {
    private Map layerMap = new HashMap();

    public abstract String getRendererContentID();

    private AbstractSelection child;

    private AbstractSelection parent;

    private SelectionManager selectionManager;

    public AbstractSelection(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    public abstract List items(Geometry geometry);

    public Collection items(Geometry geometry, Collection indices) {
        List allItems = items(geometry);
        ArrayList items = new ArrayList();
        for (Iterator i = indices.iterator(); i.hasNext();) {
            Integer index = (Integer) i.next();
            items.add(allItems.get(index.intValue()));
        }
        return items;
    }

    /**
     * Note that some features in the map may not have selected items.
     */
    public CollectionMap getFeatureToSelectedItemIndexCollectionMap(Layer layer) {
        if (!layerMap.containsKey(layer)) {
            layerMap.put(layer, new CollectionMap(HashMap.class, HashSet.class));
        }
        return (CollectionMap) layerMap.get(layer);
    }

    public Collection getSelectedItemIndices(Layer layer, Feature feature) {
        Collection indices = getFeatureToSelectedItemIndexCollectionMap(layer).getItems(feature);
        return indices == null ? new ArrayList() : indices;
    }

    /**
     * Note that some features in the map may not have selected items.
     */
    public CollectionMap getFeatureToSelectedItemCollectionMap(Layer layer) {
        CollectionMap collectionMap = new CollectionMap();
        for (Iterator i = getFeatureToSelectedItemIndexCollectionMap(layer).keySet().iterator();
            i.hasNext();
            ) {
            Feature feature = (Feature) i.next();
            collectionMap.put(
                feature,
                items(
                    feature.getGeometry(),
                    getFeatureToSelectedItemIndexCollectionMap(layer).getItems(feature)));
        }
        return collectionMap;
    }

    public Collection getLayersWithSelectedItems() {
        ArrayList layersWithSelectedItems = new ArrayList();
        for (Iterator i = layerMap.keySet().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            if (!getFeaturesWithSelectedItems(layer).isEmpty()) {
                layersWithSelectedItems.add(layer);
            }
        }
        return layersWithSelectedItems;
    }

    public Collection getFeaturesWithSelectedItems() {
        ArrayList featuresWithSelectedItems = new ArrayList();
        for (Iterator i = layerMap.keySet().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            featuresWithSelectedItems.addAll(getFeaturesWithSelectedItems(layer));
        }
        return featuresWithSelectedItems;
    }

    public Collection getFeaturesWithSelectedItems(Layer layer) {
        ArrayList featuresWithSelectedItems = new ArrayList();
        for (Iterator i = getFeatureToSelectedItemIndexCollectionMap(layer).keySet().iterator();
            i.hasNext();
            ) {
            Feature feature = (Feature) i.next();
            if (!getFeatureToSelectedItemIndexCollectionMap(layer).getItems(feature).isEmpty()) {
                featuresWithSelectedItems.add(feature);
            }
        }
        return featuresWithSelectedItems;
    }

    public Collection getSelectedItems() {
        ArrayList selectedItems = new ArrayList();
        for (Iterator i = new ArrayList(layerMap.keySet()).iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            selectedItems.addAll(getSelectedItems(layer));
        }
        return selectedItems;
    }

    public Collection getSelectedItems(Layer layer) {
        ArrayList selectedItems = new ArrayList();
        for (Iterator i = getFeatureToSelectedItemIndexCollectionMap(layer).keySet().iterator();
            i.hasNext();
            ) {
            Feature feature = (Feature) i.next();
            selectedItems.addAll(getSelectedItems(layer, feature));
        }
        return selectedItems;
    }

    public Collection getSelectedItems(Layer layer, Feature feature) {
        return getSelectedItems(layer, feature, feature.getGeometry());
    }

    /**
     * @param geometry the feature's Geometry or equivalent; that is, a clone or
     * similar enough Geometry from which Geometries can be retrieved using
     * the selection indices.
     */
    public Collection getSelectedItems(Layer layer, Feature feature, Geometry geometry) {
        return items(
            geometry,
            getFeatureToSelectedItemIndexCollectionMap(layer).getItems(feature));
    }

    public Collection indices(Geometry geometry, Collection items) {
        List allItems = items(geometry);
        ArrayList indices = new ArrayList();
        for (Iterator i = items.iterator(); i.hasNext();) {
            Object item = i.next();
            indices.add(new Integer(allItems.indexOf(item)));
        }
        return indices;
    }

    public void unselectItems(Layer layer, CollectionMap featureToItemCollectionMap) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = featureToItemCollectionMap.keySet().iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                unselectItems(layer, feature, featureToItemCollectionMap.getItems(feature));
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void selectItems(Layer layer, CollectionMap featureToItemCollectionMap) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = featureToItemCollectionMap.keySet().iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                selectItems(layer, feature, featureToItemCollectionMap.getItems(feature));
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void selectItems(Layer layer, Feature feature, Collection items) {
        Collection itemsToSelect = itemsNotSelectedInAncestors(layer, feature, items);
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            unselectInDescendants(layer, feature, itemsToSelect);
            getFeatureToSelectedItemIndexCollectionMap(layer).addItems(
                feature,
                indices(feature.getGeometry(), itemsToSelect));
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectItems(Layer layer, Feature feature, Collection items) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            getFeatureToSelectedItemIndexCollectionMap(layer).removeItems(
                feature,
                indices(feature.getGeometry(), items));
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public Collection itemsNotSelectedInAncestors(Layer layer, Feature feature, Collection items) {
        ArrayList itemsNotSelectedInAncestors = new ArrayList();

        if ( layer.isSelectable() ) {
            for (Iterator i = items.iterator(); i.hasNext();) {
                Geometry item = (Geometry) i.next();
                if (!selectedInAncestors(layer, feature, item)) {
                    itemsNotSelectedInAncestors.add(item);
                }
            }
        }

        return itemsNotSelectedInAncestors;
    }

    protected abstract boolean selectedInAncestors(Layer layer, Feature feature, Geometry item);

    protected abstract void unselectInDescendants(Layer layer, Feature feature, Collection items);

    public void selectItems(Layer layer, Feature feature) {
        selectItems(layer, feature, items(feature.getGeometry()));
    }

    public void selectItems(Layer layer, Collection features) {
        boolean originalPanelUpdatesEnabled = selectionManager.arePanelUpdatesEnabled();
        selectionManager.setPanelUpdatesEnabled(false);
        try {
            for (Iterator i = new ArrayList(features).iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                selectItems(layer, feature);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectFromFeaturesWithModifiedItemCounts(
        Layer layer,
        Collection features,
        Collection oldFeatureClones) {
        ArrayList featuresToUnselect = new ArrayList();
        Iterator j = oldFeatureClones.iterator();
        j.hasNext();
        for (Iterator i = features.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
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
            for (Iterator i = features.iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                unselectItems(layer, feature);
            }
        } finally {
            selectionManager.setPanelUpdatesEnabled(originalPanelUpdatesEnabled);
        }
        updatePanel();
    }

    public void unselectItems(Layer layer, Feature feature) {
        getFeatureToSelectedItemIndexCollectionMap(layer).remove(feature);
        updatePanel();
    }

    public void unselectItem(Layer layer, Feature feature, int selectedItemIndex) {
        getFeatureToSelectedItemIndexCollectionMap(layer).removeItem(
            feature,
            new Integer(selectedItemIndex));
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
