
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

import java.util.*;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;


public class InfoModel {

    /**
     * Releases references to the data, to facilitate garbage collection.
     * Important for MDI apps like the JCS Workbench.
     */
    public void dispose() {
        //new ArrayList to prevent ConcurrentModificationException [Jon Aquino]
        for (Iterator i = new ArrayList(getLayers()).iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            remove(layer);
        }
    
        //It would be nice to use a model proxy so that there could be multiple
        //views on the model, and when the last view calls #dispose on its proxy
        //the real model would be garbage collected (see the pattern employed by
        //LayerViewPanel). But each InfoTableModel has a reference to Features
        // -- would each Feature would need a proxy? This smells ugly.
        //So we'll go with an explicit #dispose method for now, and restrict
        //the model to one view (when that view is closed, it will call #dispose).
        //[Jon Aquino]
    }

    private Map<Layerable,LayerTableModel> layerToTableModelMap = new LinkedHashMap<>();
    private ArrayList listeners = new ArrayList();

    public InfoModel() {
    }

    public Collection getLayerTableModels() {
        return Collections.unmodifiableCollection(layerToTableModelMap.values());
    }

    public void add(Layer layer, Collection features) {
        boolean layerNew = !layerToTableModelMap.containsKey(layer);
        LayerTableModel layerTableModel = getTableModel(layer);
        layerTableModel.addAll(features);

        //Notify the listeners *after* adding the features to the table-panel model:
        //TablePanels need the data so they can properly size their columns. [Jon Aquino]
        if (layerNew) {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                InfoModelListener listener = (InfoModelListener) i.next();
                listener.layerAdded(layerTableModel);
            }
        }
    }

    public void remove(Layer layer) {
        if (!layerToTableModelMap.containsKey(layer)) return;
        LayerTableModel layerTableModel = getTableModel(layer);

        for (Iterator i = listeners.iterator(); i.hasNext();) {
            InfoModelListener listener = (InfoModelListener) i.next();
            listener.layerRemoved(layerTableModel);
        }

        layerToTableModelMap.get(layer).dispose();
        layerToTableModelMap.remove(layer);
    }

    public void clear() {
        ArrayList layers = new ArrayList(layerToTableModelMap.keySet());

        for (Iterator i = layers.iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            remove(layer);
        }
    }

    public LayerTableModel getTableModel(Layer layer) {
        if (!layerToTableModelMap.containsKey(layer)) {
            layerToTableModelMap.put(layer, new LayerTableModel(layer));
        }

        return layerToTableModelMap.get(layer);
    }

    public List<Layer> getLayers() {
        return Collections.unmodifiableList(new ArrayList(layerToTableModelMap.keySet()));
    }

    public void addListener(InfoModelListener listener) {
        listeners.add(listener);
    }
}
