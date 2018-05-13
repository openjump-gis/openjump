package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

/**
 * @see SimpleFeatureCollectionRenderer
 * @see FeatureCollectionRenderer
 */
public class ImageCachingFeatureCollectionRenderer extends ImageCachingRenderer {

	public ImageCachingFeatureCollectionRenderer(Object contentID,
			LayerViewPanel panel) {
		super(contentID, panel);
	}

	private Collection styles = new ArrayList();

	private Map layerToFeaturesMap = new HashMap();

	protected void setLayerToFeaturesMap(Map layerToFeaturesMap) {
		this.layerToFeaturesMap = layerToFeaturesMap;
	}

	protected void setStyles(Collection styles) {
		this.styles = styles;
	}

	protected void renderHook(ThreadSafeImage image, Collection features,
			Layer layer, final Style style) throws Exception {
		if (!layer.isVisible()) {
			return;
		}
		if (style == null || !style.isEnabled()) {
			return;
		}
		style.initialize(layer);
		//new ArrayList to avoid ConcurrentModificationException. [Jon Aquino]

		// Revert to the original list, as the features may be a LazyList
		// containing a huge number of features from a database. Monitor the
		// frequency of ConcurrentModificationException errors. [Jon Aquino
		// 2005-03-02]
		for (Iterator i = features.iterator(); i.hasNext();) {
      // Clone the feature to optimize rendering process in the case where
      // feature attributes are not in memory (but read from disk or database)
			final Feature feature = ((Feature) i.next()).clone();
			if (cancelled) {
				break;
			}
			if (feature.getGeometry() == null || feature.getGeometry().isEmpty()) {
				continue;
			}
			//Because image.draw is synchronized, it might be faster to do
			//several paints inside #draw. [Jon Aquino]
			image.draw(new ThreadSafeImage.Drawer() {
				public void draw(Graphics2D g) throws Exception {
					style.paint(feature, g, panel.getViewport());
				}
			});
		}
	}

	protected void renderHook(ThreadSafeImage image) throws Exception {
		for (Iterator i = styles.iterator(); i.hasNext();) {
			Style style = (Style) i.next();
			for (Iterator j = layerToFeaturesMap.keySet().iterator(); j
					.hasNext();) {
				Layer layer = (Layer) j.next();
				Collection features = (Collection) layerToFeaturesMap
						.get(layer);
				renderHook(image, features, layer, style);
			}
		}
	}

}