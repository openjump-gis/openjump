package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;

public abstract class AbstractClickSelectedLineStringsTool extends
		SpecifyFeaturesTool {
	public AbstractClickSelectedLineStringsTool() {
		setViewClickBuffer(10);
	}

	protected static final String NO_SELECTED_LINESTRINGS_HERE_MESSAGE = "No selected LineStrings here";

	protected void warnLayerNotEditable(Layer layer) {
		getWorkbench().getFrame()
				.warnUser(layer.getName() + " is not editable");
	}

	public String getName() {
		return super.getName().replaceAll("Line String", "LineString");
	}

	protected Coordinate getModelSource() {
		return getModelDestination();
	}

	protected Shape getShape(Point2D source, Point2D destination)
			throws Exception {
		return null;
	}

	protected Point getModelClickPoint() {
		return new GeometryFactory().createPoint(getModelDestination());
	}

	protected void gestureFinished() throws Exception {
		reportNothingToUndoYet();
		if (!check(checkFactory().createAtLeastNLayersMustBeEditableCheck(1))) {
			return;
		}
		if (!check(checkFactory().createAtLeastNItemsMustBeSelectedCheck(1))) {
			return;
		}
		Collection nearbyLineStringFeatures = CollectionUtil.select(
				CollectionUtil.concatenate(layerToSpecifiedFeaturesMap()
						.values()), new Block() {
					public Object yield(Object feature) {
						return getPanel().getSelectionManager()
								.getFeaturesWithSelectedItems().contains(
										feature)
								&& ((Feature) feature).getGeometry() instanceof LineString ? Boolean.TRUE
								: Boolean.FALSE;
					}
				});
		if (nearbyLineStringFeatures.isEmpty()) {
			getWorkbench().getFrame().warnUser(
					NO_SELECTED_LINESTRINGS_HERE_MESSAGE);
			return;
		}
		gestureFinished(nearbyLineStringFeatures);
	}

	private EnableCheckFactory checkFactory() {
		return new EnableCheckFactory(getWorkbench().getContext());
	}

	protected abstract void gestureFinished(Collection nearbyLineStringFeatures)
			throws NoninvertibleTransformException;

	protected Layer layer(Feature feature, Map layerToSpecifiedFeaturesMap) {
		for (Iterator i = layerToSpecifiedFeaturesMap.keySet().iterator(); i
				.hasNext();) {
			Layer layer = (Layer) i.next();
			Collection features = (Collection) layerToSpecifiedFeaturesMap
					.get(layer);
			if (features.contains(feature)) {
				return layer;
			}
		}
		Assert.shouldNeverReachHere();
		return null;
	}

}