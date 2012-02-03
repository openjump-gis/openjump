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

package com.vividsolutions.jump.workbench.ui.plugin.clipboard;

import java.util.Iterator;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;


public abstract class LayerableClipboardPlugIn extends AbstractPlugIn {
    public LayerableClipboardPlugIn() {
    }

    protected Layerable cloneLayerable(Layerable layerable) {
        if (layerable instanceof Layer) {
            return cloneLayer((Layer) layerable);
        }

        if (layerable instanceof WMSLayer) {
            try {
                return (Layerable) ((WMSLayer) layerable).clone();
            } catch (CloneNotSupportedException e) {
                Assert.shouldNeverReachHere();
            }
        }
        
        if (layerable instanceof RasterImageLayer) {
            try {
                return (Layerable) ((RasterImageLayer) layerable).clone();
            } catch (CloneNotSupportedException e) {
                Assert.shouldNeverReachHere();
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    protected Layer cloneLayer(Layer layer) {
        LayerManager dummyLayerManager = new LayerManager();
        dummyLayerManager.setFiringEvents(false);

        Layer clone = new Layer();
        clone.setLayerManager(dummyLayerManager);

        //If this is the fence layer, #setName will call #applyStyles, which requires
        //that the clone have a BasicStyle. So set the styles before setting the
        //name. [Jon Aquino]
        clone.setStyles(layer.cloneStyles());
        clone.setName(layer.getName());
        clone.setFeatureCollection(cloneFeatureCollection(
                layer.getFeatureCollectionWrapper()));

        return clone;
    }

    private FeatureCollection cloneFeatureCollection(
        FeatureCollection featureCollection) {
        FeatureDataset d = new FeatureDataset(featureCollection.getFeatureSchema());

        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            d.add((Feature) f.clone());
        }

        return d;
    }
}
