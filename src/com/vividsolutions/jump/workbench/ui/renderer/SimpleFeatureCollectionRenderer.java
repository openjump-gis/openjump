package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Graphics2D;
import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

/**
 * @see ImageCachingFeatureCollectionRenderer
 * @see FeatureCollectionRenderer
 */
public class SimpleFeatureCollectionRenderer extends SimpleRenderer {

	public SimpleFeatureCollectionRenderer(Object contentID,
			LayerViewPanel panel) {
		super(contentID, panel);
	}

	private void paint(Graphics2D g, Collection features, Layer layer,
			Style style) throws Exception {
		if (!layer.isVisible()) {
			return;
		}
		if (!style.isEnabled()) {
			return;
		}
		style.initialize(layer);
		//new ArrayList to avoid ConcurrentModificationException. [Jon Aquino]
		for (Iterator i = new ArrayList(features).iterator(); i.hasNext();) {
			final Feature feature = (Feature) i.next();
			if (cancelled) {
				return;
			}
			if (feature.getGeometry().isEmpty()) {
				continue;
			}
			style.paint(feature, g, panel.getViewport());
		}
	}

	protected void paint(Graphics2D g) throws Exception {
		for (Iterator i = styles.iterator(); i.hasNext();) {
			Style style = (Style) i.next();
			if (cancelled) {
				return;
			}
			for (Iterator j = layerToFeaturesMap.keySet().iterator(); j
					.hasNext();) {
				Layer layer = (Layer) j.next();
				if (cancelled) {
					return;
				}
				Collection features = (Collection) layerToFeaturesMap
						.get(layer);
				paint(g, features, layer, style);
			}
		}
	}

	private Collection styles = new ArrayList();

	private Map layerToFeaturesMap = new HashMap();

	protected void setLayerToFeaturesMap(Map layerToFeaturesMap) {
		this.layerToFeaturesMap = layerToFeaturesMap;
	}

	protected void setStyles(Collection styles) {
		this.styles = styles;
	}

}