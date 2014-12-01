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
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringEndpointStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.CircleLineStringEndpointStyle;

/**
 * Retrieves a two-point-LineString layer and sets its styles.
 */
public abstract class AbstractVectorLayerFinder extends SystemLayerFinder {
    private Color color;

    public AbstractVectorLayerFinder(String layerName, LayerManagerProxy layerManagerProxy, Color color) {
        super(layerName, layerManagerProxy);
        this.color = color;
    }

    public List getVectors() {
        if (getLayer() == null) {
            return new ArrayList();
        }

        return FeatureUtil.toGeometries(getLayer().getFeatureCollectionWrapper().getFeatures());
    }

    protected void applyStyles(Layer layer) {
        if (null == layer.getStyle(ArrowLineStringEndpointStyle.class)) {
            layer.addStyle(new ArrowLineStringEndpointStyle.SolidEnd());
        }
        if (null == layer.getStyle(CircleLineStringEndpointStyle.Start.class)) {
            layer.addStyle(new CircleLineStringEndpointStyle.Start());
        }
        layer.getBasicStyle().setLineColor(color);
        layer.getBasicStyle().setFillColor(color);
        layer.getBasicStyle().setRenderingFill(false);
        layer.setDrawingLast(true);
    }

}
