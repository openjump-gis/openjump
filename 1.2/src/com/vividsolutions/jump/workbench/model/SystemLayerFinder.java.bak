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

package com.vividsolutions.jump.workbench.model;

import java.awt.Color;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * A "system-maintained layer" has a fixed set of styles and is identified by
 * name. For example, the vector layer has blue features with arrowheads and
 * is named "Warping Vectors". A SystemLayerFinder class will find a particular
 * system-maintained layer, and can create it if necessary.
 */
public abstract class SystemLayerFinder {
    private String layerName;
    private LayerManagerProxy layerManagerProxy;

    public SystemLayerFinder(String layerName, LayerManagerProxy layerManagerProxy) {
        this.layerManagerProxy = layerManagerProxy;
        this.layerName = layerName;
    }

    public String getLayerName() {
        return layerName;
    }

    public Layer createLayer() {
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        FeatureDataset dataset = new FeatureDataset(schema);
        Layer layer =
            new Layer(
                layerName,
                Color.blue,
                dataset,
                layerManagerProxy.getLayerManager()) {
            public boolean isFeatureCollectionModified() {
                    //Prevent save prompt. [Jon Aquino]
    return false;
            }
        };
        boolean firingEvents = layerManagerProxy.getLayerManager().isFiringEvents();
        //Can't fire events because this Layerable hasn't been added to the
        //LayerManager yet. [Jon Aquino]
        layerManagerProxy.getLayerManager().setFiringEvents(false);

        try {
            applyStyles(layer);
        } finally {
            layerManagerProxy.getLayerManager().setFiringEvents(firingEvents);
        }

        layerManagerProxy.getLayerManager().addLayer(StandardCategoryNames.SYSTEM, layer);

        return layer;
    }

    /**
     * @return the layer, or null if there is no layer
     */
    public Layer getLayer() {
        Layer layer = layerManagerProxy.getLayerManager().getLayer(layerName);

        if (layer == null) {
            //Don't automatically create the layer. For example, #getLayer may be
            //called by an EnableCheck; we wouldn't want the layer to get created
            //in this case. [Jon Aquino]
            return null;
        }

        return layer;
    }

    protected abstract void applyStyles(Layer layer);
}
