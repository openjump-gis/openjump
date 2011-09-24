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

package com.vividsolutions.jump.workbench.ui.cursortool;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.AbstractSelection;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;

public abstract class SelectTool extends DragTool {
    
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }
    
    public void mouseClicked(MouseEvent e) {
        try {
            super.mouseClicked(e);
            setViewSource(e.getPoint());
            setViewDestination(e.getPoint());
            fireGestureFinished();
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    protected void gestureFinished() throws NoninvertibleTransformException {
        reportNothingToUndoYet();

        if (!wasShiftPressed()) {
            getPanel().getSelectionManager().clear();
        }

        Map layerToFeaturesInFenceMap =
            getPanel().visibleLayerToFeaturesInFenceMap(
                EnvelopeUtil.toGeometry(getBoxInModelCoordinates()));

        Collection layers = layerToFeaturesInFenceMap.keySet();
        if (selectedLayersOnly()) {
            layers.retainAll(Arrays.asList(getTaskFrame().getLayerNamePanel().getSelectedLayers()));
        }
        for (Iterator i = layers.iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();

            if (layer.getName().equals(FenceLayerFinder.LAYER_NAME)) {
                continue;
            }

            //Disable panel updates -- we'll manually repaint the selection and
            //fire the selection-changed event. [Jon Aquino]
            boolean originalPanelUpdatesEnabled =
                getPanel().getSelectionManager().arePanelUpdatesEnabled();
            getPanel().getSelectionManager().setPanelUpdatesEnabled(false);
            try {
                Map<Feature,List<Geometry>> featureToItemsToSelectMap =
                    featureToItemsInFenceMap(
                        (Collection) layerToFeaturesInFenceMap.get(layer),
                        layer,
                        false);
                Map<Feature,List<Geometry>> featureToItemsToUnselectMap =
                    featureToItemsInFenceMap(
                        (Collection) layerToFeaturesInFenceMap.get(layer),
                        layer,
                        true);
                selection.selectItems(layer, featureToItemsToSelectMap);
                if (wasShiftPressed()) {
                    selection.unselectItems(layer, featureToItemsToUnselectMap);
                }
            } finally {
                getPanel().getSelectionManager().setPanelUpdatesEnabled(
                    originalPanelUpdatesEnabled);
            }
        }

        getPanel().getSelectionManager().updatePanel();
    }

    protected boolean selectedLayersOnly() {
        return wasControlPressed();
    }

    private String rendererID;
    
    protected SelectTool(String rendererID) {
        this.rendererID = rendererID;
    }

    protected AbstractSelection selection;

    /**
     * @param selected whether to return selected items or deselected items
     */
    private Map<Feature,List<Geometry>> featureToItemsInFenceMap(
            Collection features, Layer layer, boolean selected)
                                        throws NoninvertibleTransformException {
        Map<Feature,List<Geometry>> featureToSelectedItemsMap =
            selection.getFeatureToSelectedItemCollectionMap(layer);
        Map<Feature,List<Geometry>> featureToItemsInFenceMap = 
            new HashMap<Feature,List<Geometry>>();
        for (Iterator i = features.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            List<Geometry> selectedItems = featureToSelectedItemsMap.get(feature);
            if (selectedItems == null) selectedItems = Collections.EMPTY_LIST;
            List<Geometry> itemsToReturn = itemsInFence(feature);
            if (selected) {
                itemsToReturn.retainAll(selectedItems);
            } else {
                itemsToReturn.removeAll(selectedItems);
            }
            featureToItemsInFenceMap.put(feature, itemsToReturn);
        }
        return featureToItemsInFenceMap;
    }

    private List<Geometry> itemsInFence(Feature feature) throws NoninvertibleTransformException {
        List<Geometry> itemsInFence = new ArrayList<Geometry>(1);
        Geometry fence = EnvelopeUtil.toGeometry(getBoxInModelCoordinates());
        for (Geometry selectedItem : selection.items(feature.getGeometry())) {
            if (LayerViewPanel.intersects(selectedItem, fence)) {
                itemsInFence.add(selectedItem);
            }
        }
        return itemsInFence;
    }

    public Icon getIcon() {
        return null;
    }

}
