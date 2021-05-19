package com.vividsolutions.jump.workbench.ui.renderer;

//[sstein] : 30.07.2005 added variable maxFeatures with getters and setters

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

public abstract class FeatureCollectionRenderer implements Renderer {
	
	private int maxFeatures = 100;
	
	private ImageCachingFeatureCollectionRenderer imageCachingFeatureCollectionRenderer;

	private SimpleFeatureCollectionRenderer simpleFeatureCollectionRenderer;

	private Renderer currentFeatureCollectionRenderer;

	public FeatureCollectionRenderer(Object contentID, LayerViewPanel panel) {
		this(contentID, panel, new ImageCachingFeatureCollectionRenderer(
				contentID, panel));
	}

	public FeatureCollectionRenderer(
			Object contentID,
			LayerViewPanel panel,
			ImageCachingFeatureCollectionRenderer imageCachingFeatureCollectionRenderer) {
		this.imageCachingFeatureCollectionRenderer = imageCachingFeatureCollectionRenderer;
		simpleFeatureCollectionRenderer = new SimpleFeatureCollectionRenderer(
				contentID, panel);
		currentFeatureCollectionRenderer = simpleFeatureCollectionRenderer;
	}

	public void clearImageCache() {
		imageCachingFeatureCollectionRenderer.clearImageCache();
		simpleFeatureCollectionRenderer.clearImageCache();
	}

	public boolean isRendering() {
		return currentFeatureCollectionRenderer.isRendering();
	}

	public Object getContentID() {
		return currentFeatureCollectionRenderer.getContentID();
	}

	public void copyTo(Graphics2D graphics) {
		currentFeatureCollectionRenderer.copyTo(graphics);
	}

	public Runnable createRunnable() {
		Map layerToFeaturesMap = layerToFeaturesMap();
		Collection styles = styles();
		imageCachingFeatureCollectionRenderer
				.setLayerToFeaturesMap(layerToFeaturesMap);
		imageCachingFeatureCollectionRenderer.setStyles(styles);
		simpleFeatureCollectionRenderer
				.setLayerToFeaturesMap(layerToFeaturesMap);
		simpleFeatureCollectionRenderer.setStyles(styles);
		currentFeatureCollectionRenderer = useImageCaching(layerToFeaturesMap) ? (Renderer) imageCachingFeatureCollectionRenderer
				: simpleFeatureCollectionRenderer;
		return currentFeatureCollectionRenderer.createRunnable();
	}

	protected boolean useImageCaching(Map<Layer, List<Feature>> layerToFeaturesMap) {
		return featureCount(layerToFeaturesMap) >= this.maxFeatures;
	}

	private int featureCount(Map<Layer, List<Feature>> layerToFeaturesMap) {
		int count = 0;
		for (List<Feature> features : layerToFeaturesMap.values()) {
			count += features.size();
		}

		return count;
	}

	protected abstract Map<Layer,Collection<Feature>> layerToFeaturesMap();

	protected abstract Collection<Style> styles();

	public void cancel() {
		imageCachingFeatureCollectionRenderer.cancel();
		simpleFeatureCollectionRenderer.cancel();
	}
	
	/**
	 * @return Returns the number of maxFeatures to render
	 * as vector graphic.
	 */
	public int getMaxFeatures() {
		return maxFeatures;
	}
	/**
	 * @param maxFeatures The maximum number of Features to render
	 * as vector graphic.
	 */
	public void setMaxFeatures(int maxFeatures) {
		this.maxFeatures = maxFeatures;
	}
	/**
	 * @return Returns the simpleFeatureCollectionRenderer.
	 */
	public SimpleFeatureCollectionRenderer getSimpleFeatureCollectionRenderer() {
		return simpleFeatureCollectionRenderer;
	}

}