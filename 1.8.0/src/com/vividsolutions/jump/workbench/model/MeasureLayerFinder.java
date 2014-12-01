package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel;
import org.openjump.core.ui.plugin.tools.MeasurementStyle;

/**
 * A special LayerFinder for the AdvancedMeasureTool.
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class MeasureLayerFinder extends SystemLayerFinder {
	public static final String FEATURE_ATTRIBUTE_AREA = "area";
	public static final String FEATURE_ATTRIBUTE_LENGTH = "length";
	public static final String FEATURE_ATTRIBUTE_POINTS = "points";

	public static final String LAYER_NAME = I18N.get("model.MeasureLayerFinder.measure");

	private static Layer measureLayer = null;
	WorkbenchContext context = null;

	public MeasureLayerFinder(LayerManagerProxy layerManagerProxy, WorkbenchContext context) {
		super(LAYER_NAME, layerManagerProxy);
		this.context = context;
	}


	@Override
	protected void applyStyles(Layer layer) {
		Object font;
		Object color;
		Object string;

		// get the persitent values from the Blackboard and apply
		Blackboard blackboard = PersistentBlackboardPlugIn.get(context);

		// first the basic styling, such as line and fill
		BasicStyle basicStyle = layer.getBasicStyle();
		color = blackboard.get(AdvancedMeasureOptionsPanel.BB_LINE_COLOR, AdvancedMeasureOptionsPanel.DEFAULT_LINE_COLOR);
		if (color instanceof Color) basicStyle.setLineColor((Color) color);
		basicStyle.setAlpha(100); // TODO or wait for JDK7 with a new ColorChooser ;-)
		basicStyle.setLineWidth(2); // TODO: There must be a version two ;-)
		basicStyle.setRenderingLine(blackboard.get(AdvancedMeasureOptionsPanel.BB_LINE_PAINT, AdvancedMeasureOptionsPanel.DEFAULT_LINE_PAINT));
		color = blackboard.get(AdvancedMeasureOptionsPanel.BB_FILL_COLOR, AdvancedMeasureOptionsPanel.DEFAULT_FILL_COLOR);
		if (color instanceof Color) basicStyle.setFillColor((Color) color);
		basicStyle.setRenderingFill(blackboard.get(AdvancedMeasureOptionsPanel.BB_FILL_PAINT, AdvancedMeasureOptionsPanel.DEFAULT_FILL_PAINT));
		layer.setDrawingLast(true);

		// and second the special measurement stylings
		MeasurementStyle measurementStyle = new MeasurementStyle();
		measurementStyle.setEnabled(true);
		// summary
		measurementStyle.setPaintSummaryLength(blackboard.get(AdvancedMeasureOptionsPanel.BB_SUMMARY_PAINT_LENGTH, AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_PAINT_LENGTH));
		measurementStyle.setPaintSummaryArea(blackboard.get(AdvancedMeasureOptionsPanel.BB_SUMMARY_PAINT_AREA, AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_PAINT_AREA));
		font = blackboard.get(AdvancedMeasureOptionsPanel.BB_SUMMARY_FONT, AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_FONT);
		if (font instanceof Font) measurementStyle.setSummaryFont((Font) font);
		color = blackboard.get(AdvancedMeasureOptionsPanel.BB_SUMMARY_FONT_COLOR, AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_COLOR);
		if (color instanceof Color) measurementStyle.setSummaryColor((Color) color);
		// vertex
		measurementStyle.setVertexPaintDistance(blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_PAINT_DISTANCE, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_PAINT_DISTANCE));
        measurementStyle.setVertexPaintDistanceRelative(blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_PAINT_DISTANCE_RELATIVE, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_PAINT_DISTANCE_RELATIVE));
		font = blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FONT, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FONT);
		if (font instanceof Font) measurementStyle.setVertexFont((Font) font);
		color = blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FONT_COLOR, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FONT_COLOR);
		if (color instanceof Color) measurementStyle.setVertexFontColor((Color) color);

		measurementStyle.setVertexPaint(blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_PAINT, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_PAINT));
		color = blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FIRST_COLOR, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FIRST_COLOR);
		if (color instanceof Color) measurementStyle.setVertexFirstColor((Color) color);
		string = blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FIRST_FORM, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FIRST_FORM);
		if (string instanceof String) measurementStyle.setVertexFirstForm((String) string);
		measurementStyle.setVertexFirstSize(blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FIRST_SIZE, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FIRST_SIZE));
		color = blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FOLLOWING_COLOR, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FOLLOWING_COLOR);
		if (color instanceof Color) measurementStyle.setVertexFollowingColor((Color) color);
		string = blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FOLLOWING_FORM, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FOLLOWING_FORM);
		if (string instanceof String) measurementStyle.setVertexFollowingForm((String) string);
		measurementStyle.setVertexFollowingSize(blackboard.get(AdvancedMeasureOptionsPanel.BB_VERTEX_FOLLOWING_SIZE, AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FOLLOWING_SIZE));

		// finally add the Style
		layer.addStyle(measurementStyle);
	}

	/**
	 * Builds a new Feature from a Geometry with the given FeatureSchema.
	 * This Feature can later be added to Layer.
	 * 
	 * @param measureGeometry
	 * @param schema
	 * @return a new Feature having measureGeometry as Geometry and schema as FeatureSchema
	 */
	private Feature toFeature(Geometry measureGeometry, FeatureSchema schema) {
        Feature feature = new BasicFeature(schema);
        feature.setGeometry(measureGeometry);
		feature.setAttribute(FEATURE_ATTRIBUTE_LENGTH, measureGeometry.getLength());
		feature.setAttribute(FEATURE_ATTRIBUTE_AREA, measureGeometry instanceof Polygon ? measureGeometry.getArea() : 0);
		feature.setAttribute(FEATURE_ATTRIBUTE_POINTS, measureGeometry.getNumPoints());

        return feature;
    }

	/**
	 * Sets the new measure Geometry. This clears all old Features on the
	 * measurelayer and add's the new Features.
	 *
	 * @param measureGeometry
	 */
	public void setMeasure(Geometry measureGeometry) {
        if (getLayer() == null) {
			initLayer();
		}

        if (measureGeometry != null) {
            getLayer().getFeatureCollectionWrapper().add(toFeature(measureGeometry,
                    getLayer().getFeatureCollectionWrapper().getFeatureSchema()));
        }

	}

	/**
	 * Return the measurelayer. If not exists until now, create a new one.
	 *
	 * @return the measure layer
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
				// we are only interested on the measureLayer
				if (e.getLayer().equals(measureLayer)) {
					Collection<Feature> features = e.getFeatures();
					for (Feature feature : features) {
						feature.setAttribute(FEATURE_ATTRIBUTE_LENGTH, feature.getGeometry().getLength());
						feature.setAttribute(FEATURE_ATTRIBUTE_AREA, feature.getGeometry() instanceof Polygon ? feature.getGeometry().getArea() : 0);
						feature.setAttribute(FEATURE_ATTRIBUTE_POINTS, feature.getGeometry().getNumPoints());
					}
				}
			}

		});
		measureLayer.getFeatureCollectionWrapper().getFeatureSchema().addAttribute(FEATURE_ATTRIBUTE_AREA, AttributeType.DOUBLE);
		measureLayer.getFeatureCollectionWrapper().getFeatureSchema().addAttribute(FEATURE_ATTRIBUTE_LENGTH, AttributeType.DOUBLE);
		measureLayer.getFeatureCollectionWrapper().getFeatureSchema().addAttribute(FEATURE_ATTRIBUTE_POINTS, AttributeType.INTEGER);
	}

}
