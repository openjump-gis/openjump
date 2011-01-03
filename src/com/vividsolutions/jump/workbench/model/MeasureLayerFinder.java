package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import java.awt.Color;
import java.util.Collection;
import java.util.List;
import org.openjump.core.ui.plugin.tools.MeasurementStyle;

/**
 * A special LayerFinder for the AdvanncedMeasureTool.
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class MeasureLayerFinder extends SystemLayerFinder {
	public static final String FEATURE_ATTRIBUTE_AREA = "area";
	public static final String FEATURE_ATTRIBUTE_LENGTH = "length";
	public static final String FEATURE_ATTRIBUTE_POINTS = "points";

	public static final String LAYER_NAME = I18N.get("model.MeasureLayerFinder.measure");

	private static Layer measureLayer = null;

	public MeasureLayerFinder(LayerManagerProxy layerManagerProxy) {
		super(LAYER_NAME, layerManagerProxy);
	}


	@Override
	protected void applyStyles(Layer layer) {
		layer.getBasicStyle().setLineColor(Color.red);
		layer.getBasicStyle().setAlpha(100);
		layer.getBasicStyle().setRenderingLine(true);
		layer.getBasicStyle().setFillColor(Color.red);
		layer.getBasicStyle().setRenderingFill(true);
		layer.setDrawingLast(true);

		MeasurementStyle measurementStyle = new MeasurementStyle();
		measurementStyle.setEnabled(true);
		layer.addStyle(measurementStyle);
	}

	private Feature toFeature(Geometry measureGeometry, FeatureSchema schema) {
        Feature feature = new BasicFeature(schema);
        feature.setGeometry(measureGeometry);
		feature.setAttribute(FEATURE_ATTRIBUTE_LENGTH, measureGeometry.getLength());
		feature.setAttribute(FEATURE_ATTRIBUTE_AREA, measureGeometry instanceof Polygon ? measureGeometry.getArea() : 0);
		feature.setAttribute(FEATURE_ATTRIBUTE_POINTS, measureGeometry.getNumPoints());

        return feature;
    }

	public void setMeasure(Geometry measureGeometry) {
        if (getLayer() == null) {
			initLayer();
		}

        if (measureGeometry != null) {
            getLayer().getFeatureCollectionWrapper().clear();
            getLayer().getFeatureCollectionWrapper().add(toFeature(measureGeometry,
                    getLayer().getFeatureCollectionWrapper().getFeatureSchema()));
        }

	}

	/**
	 * @return the layer
	 */
	public Layer getMeasureLayer() {
		if (getLayer() == null) {
			initLayer();
		}
		return measureLayer;
	}

	/**
	 * Initialises the Layer for our measurement needs.
	 */
	private void initLayer() {
		measureLayer = createLayer();
		measureLayer.setSelectable(false);
		// Add a LayerListener to compute the FeatureAttributes, if a Feature was changed
		measureLayer.getLayerManager().addLayerListener(new LayerAdapter() {
			@Override
			public void featuresChanged(FeatureEvent e) {
				super.featuresChanged(e);
				Collection<Feature> features = e.getFeatures();
				for (Feature feature : features) {
					feature.setAttribute(FEATURE_ATTRIBUTE_LENGTH, feature.getGeometry().getLength());
					feature.setAttribute(FEATURE_ATTRIBUTE_AREA, feature.getGeometry() instanceof Polygon ? feature.getGeometry().getArea() : 0);
					feature.setAttribute(FEATURE_ATTRIBUTE_POINTS, feature.getGeometry().getNumPoints());
				}
			}

		});
		measureLayer.getFeatureCollectionWrapper().getFeatureSchema().addAttribute(FEATURE_ATTRIBUTE_AREA, AttributeType.DOUBLE);
		measureLayer.getFeatureCollectionWrapper().getFeatureSchema().addAttribute(FEATURE_ATTRIBUTE_LENGTH, AttributeType.DOUBLE);
		measureLayer.getFeatureCollectionWrapper().getFeatureSchema().addAttribute(FEATURE_ATTRIBUTE_POINTS, AttributeType.INTEGER);
	}

}
