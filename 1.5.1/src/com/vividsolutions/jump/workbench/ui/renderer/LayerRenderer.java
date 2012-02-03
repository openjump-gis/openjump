/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
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
package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

//[sstein] : 14.08.2005 added variable maxFeatures with getters and setters

public class LayerRenderer extends FeatureCollectionRenderer {
    private Layer layer;

    private LayerViewPanel panel;

    public static final String ALWAYS_USE_IMAGE_CACHING_KEY = LayerRenderer.class
            .getName()
            + " - ALWAYS USE IMAGE CACHING";

    public LayerRenderer(final Layer layer, LayerViewPanel panel) {
        //Use layer as the contentID [Jon Aquino]
        super(layer, panel, new ImageCachingFeatureCollectionRenderer(layer,
                panel) {
            protected ThreadSafeImage getImage() {
                if (!layer.isVisible()) {
                    return null;
                }

                return super.getImage();
            }

            public Runnable createRunnable() {
                if (!layer.isVisible()) {
                    //If the cached image is null, leave it alone. [Jon
                    // Aquino]
                    return null;
                }

                return super.createRunnable();
            }
        });
        this.layer = layer;
        this.panel = panel;
    }

    public Runnable createRunnable() {
        if (!render(layer, panel)) {
            return null;
        }
        return super.createRunnable();
    }

    public void copyTo(Graphics2D graphics) {
        if (!render(layer, panel)) {
            return;
        }
        super.copyTo(graphics);
    }

    public static boolean render(Layerable layerable, LayerViewPanel panel) {
        if (!layerable.isVisible()) {
            return false;
        }        
        if (!layerable.getLayerManager().getLayerables(Layerable.class).contains(layerable)) {
            // Get here after deleting a layer. [Jon Aquino 2005-03-29]
            return false;
        }
        return withinVisibleScaleRange(layerable, panel);
    }

    public static boolean withinVisibleScaleRange(Layerable layerable,
            LayerViewPanel panel) {
        // When working with scale, the max is less than the min.
        // [Jon Aquino 2005-03-01]
        Assert.isTrue(layerable.getMaxScale() == null
                || layerable.getMinScale() == null
                || layerable.getMaxScale().doubleValue() <= layerable
                        .getMinScale().doubleValue());
        if (!layerable.isScaleDependentRenderingEnabled()) {
            return true;
        }
        if (layerable.getMaxScale() != null
                && scale(panel) < layerable.getMaxScale().doubleValue()) {
            return false;
        }
        if (layerable.getMinScale() != null
                && scale(panel) > layerable.getMinScale().doubleValue()) {
            return false;
        }
        return true;
    }

    /**
     * @return the inverse of the viewport's scale; it is inverted so that it
     *         increases as the user zooms out, as is usually expected
     */
    private static double scale(LayerViewPanel panel) {
        return 1d / panel.getViewport().getScale();
    }

    protected Collection styles() {
        //new ArrayList to avoid ConcurrentModificationExceptions. [Jon Aquino]
        ArrayList styles = new ArrayList(layer.getStyles());
        styles.remove(layer.getVertexStyle());
        styles.remove(layer.getLabelStyle());

        //Move to last. [Jon Aquino]
        styles.add(layer.getVertexStyle());
        styles.add(layer.getLabelStyle());

        return styles;
    }

    protected boolean useImageCaching(Map layerToFeaturesMap) {
        if (layer.getBlackboard().get(ALWAYS_USE_IMAGE_CACHING_KEY, false)) {
            return true;
        }
        return super.useImageCaching(layerToFeaturesMap);
    }

    protected Map layerToFeaturesMap() {
        Envelope viewportEnvelope = panel.getViewport()
                .getEnvelopeInModelCoordinates();

        return Collections.singletonMap(layer, layer
                .getFeatureCollectionWrapper().query(viewportEnvelope));
    }
    
	/**
	 * @return Returns the number of maxFeatures to render
	 * as vector graphic.
	 */
	public int getMaxFeatures() {
		return super.getMaxFeatures();
	}
	/**
	 * @param maxFeatures The maximum number of Features to render
	 * as vector graphic.<p>
	 * Use this method before using method render(Object contentID) or render(Object contentID, boolean clearImageCache)  
	 */
	public void setMaxFeatures(int maxFeatures) {
		super.setMaxFeatures(maxFeatures);
	}    


}