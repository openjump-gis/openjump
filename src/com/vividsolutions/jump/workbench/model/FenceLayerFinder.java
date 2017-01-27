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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;


/**
 * Retrieves the layer containing the single Fence polygon (if any) and sets its styles.
 */
public class FenceLayerFinder extends SystemLayerFinder {

    public static final String LAYER_NAME = I18N.get("model.FenceLayerFinder.fence");
    public FenceLayerFinder(LayerManagerProxy layerManagerProxy) {
        super(LAYER_NAME, layerManagerProxy);
    }

    public Geometry getFence() {
        if (getLayer() == null) {
            return null;
        }

        if (getLayer().getFeatureCollectionWrapper().isEmpty()) {
            return null;
        }

        return (getLayer().getFeatureCollectionWrapper().iterator().next()).getGeometry();
    }

    protected void applyStyles(Layer layer) {
        layer.getBasicStyle().setLineColor(Color.blue);
        layer.getBasicStyle().setRenderingLine(true);
        layer.getBasicStyle().setRenderingFill(false);
        layer.setDrawingLast(true);
    }

    private Feature toFeature(Geometry fence, FeatureSchema schema) {
        Feature feature = new BasicFeature(schema);
        feature.setGeometry(fence);

        return feature;
    }

    public void setFence(Geometry fence) {
        if (getLayer() == null) {
            createLayer();
        }

        if (fence != null) {
            getLayer().getFeatureCollectionWrapper().clear();            
            getLayer().getFeatureCollectionWrapper().add(toFeature(fence,
                    getLayer().getFeatureCollectionWrapper().getFeatureSchema()));
        }

    }
}
