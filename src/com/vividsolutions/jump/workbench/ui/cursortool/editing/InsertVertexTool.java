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
package com.vividsolutions.jump.workbench.ui.cursortool.editing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;

import javax.swing.Icon;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GeometryEditor;
import com.vividsolutions.jump.workbench.ui.cursortool.Animations;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


public class InsertVertexTool extends NClickTool {

	private static final int PIXEL_RANGE = 5;
	private final EnableCheckFactory checkFactory;
	public InsertVertexTool(WorkbenchContext context) {
		super(context, 1);
		this.checkFactory = EnableCheckFactory.getInstance(context);
	}

	private double modelRange() {
		return PIXEL_RANGE / getPanel().getViewport().getScale();
	}

	private Collection<Feature> featuresInRange(Coordinate modelClickCoordinate,
			Layer layer) {
		Point modelClickPoint = new GeometryFactory()
				.createPoint(modelClickCoordinate);
		Collection<Feature> featuresWithSelectedItems = getPanel().getSelectionManager()
				.getFeaturesWithSelectedItems(layer);
		if (featuresWithSelectedItems.isEmpty()) {
			return new ArrayList<>();
		}
		List<Feature> featuresInRange = new ArrayList<>();
		for (Feature candidate : featuresWithSelectedItems) {
			if (modelClickPoint.distance(candidate.getGeometry()) <= modelRange()) {
				featuresInRange.add(candidate);
			}
		}
		return featuresInRange;
	}

	private Coordinate modelClickCoordinate() {
		return getCoordinates().get(0);
	}

	private LineSegment segmentInRange(Geometry geometry, Coordinate target) {
		//It's possible that the geometry may have no segments in range; for
		// example, if it
		//is empty, or if only has points in range. [Jon Aquino]
		LineSegment closest = null;
		List<Coordinate[]> coordArrays = CoordinateArrays.toCoordinateArrays(geometry, false);
		for (Coordinate[] coordinates : coordArrays) {
			for (int j = 1; j < coordinates.length; j++) { //1
				LineSegment candidate = new LineSegment(coordinates[j - 1],
						coordinates[j]);
				if (candidate.distance(target) > modelRange()) {
					continue;
				}
				if ((closest == null)
						|| (candidate.distance(target) < closest
								.distance(target))) {
					closest = candidate;
				}
			}
		}
		return closest;
	}
	private Coordinate newVertex(LineSegment segment, Coordinate target) {
		Coordinate closestPoint = segment.closestPoint(target);
		if (!closestPoint.equals(segment.p0)
				&& !closestPoint.equals(segment.p1)) {
		   	if (Double.isNaN(segment.p0.z) || Double.isNaN(segment.p1.z))
		   	 			return closestPoint;
			closestPoint.z = (segment.p0.z + segment.p1.z) / 2d;

		}
		//No good to make the new vertex one of the endpoints. If the segment
		// is tiny (less than 6 pixels), pick the midpoint. [Jon Aquino]
		double threshold = 6 / getPanel().getViewport().getScale();
		if (segment.getLength() < threshold) {
			return CoordUtil.average(segment.p0, segment.p1);
		}
		//Segment is not so tiny. Pick a point 3 pixels from the end. [Jon Aquino]
		double offset = 3 / getPanel().getViewport().getScale();
		Coordinate unitVector = closestPoint.equals(segment.p0) ? CoordUtil
				.divide(CoordUtil.subtract(segment.p1, segment.p0), segment
						.getLength()) : CoordUtil.divide(CoordUtil.subtract(
				segment.p0, segment.p1), segment.getLength());
		return CoordUtil.add(closestPoint, CoordUtil.multiply(offset,
				unitVector));
	}
	protected static class SegmentContext {
		public SegmentContext(Layer layer, Feature feature, LineSegment segment) {
			this.layer = layer;
			this.feature = feature;
			this.segment = segment;
		}
		private final LineSegment segment;
		private final Feature feature;
		private final Layer layer;
		public Feature getFeature() {
			return feature;
		}
		public Layer getLayer() {
			return layer;
		}
		public LineSegment getSegment() {
			return segment;
		}
	}
	private SegmentContext findSegment(Layer layer, Collection<Feature> features,
			Coordinate target) {
		Assert.isTrue(layer.isEditable());
		for (Feature feature : features) {
			for (Geometry selectedItem : getPanel().getSelectionManager()
						.getSelectedItems(layer, feature)) {
				LineSegment segment = segmentInRange(selectedItem, target);
				if (segment != null) {
					return new SegmentContext(layer, feature, segment);
				}
			}
		}
		return null;
	}

	private SegmentContext findSegment(Map<Layer,Collection<Feature>> layerToFeaturesMap, Coordinate target) {
		for (Layer layer : layerToFeaturesMap.keySet()) {
			Collection<Feature> features = layerToFeaturesMap.get(layer);
			SegmentContext segmentContext = findSegment(layer, features, target);
			if (segmentContext != null) {
				return segmentContext;
			}
		}
		return null;
	}
	protected void gestureFinished() throws java.lang.Exception {
		reportNothingToUndoYet();
		if (!check(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))) {
			return;
		}
		if (!check(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))) {
			return;
		}
		Map<Layer,Collection<Feature>> layerToFeaturesInRangeMap = layerToFeaturesInRangeMap();
		if (layerToFeaturesInRangeMap.isEmpty()) {
			getPanel().getContext().warnUser(I18N.getInstance().get("ui.cursortool.editing.InsertVertexTool.no-selected-editable-items-here"));
			return;
		}
		SegmentContext segment = findSegment(layerToFeaturesInRangeMap,
				modelClickCoordinate());
		if (segment == null) {
			getPanel().getContext().warnUser(I18N.getInstance().get("ui.cursortool.editing.InsertVertexTool.no-selected-line-segments-here"));
			return;
		}
		final Coordinate newVertex = newVertex(segment.getSegment(),
				modelClickCoordinate());
		Geometry newGeometry = new GeometryEditor().insertVertex(segment
				.getFeature().getGeometry(), segment.getSegment().p0, segment
				.getSegment().p1, newVertex);
		gestureFinished(newGeometry, newVertex, segment);
	}

	protected void gestureFinished(Geometry newGeometry, final Coordinate newVertex, SegmentContext segment) {
		EditTransaction transaction = new EditTransaction(
				Collections.singletonList(segment.getFeature()), getName(),
				segment.getLayer(), isRollingBackInvalidEdits(), false,
				getPanel());
		//transaction.setGeometry(0, newGeometry);
		transaction.setGeometry(segment.getFeature(), newGeometry);
		transaction.commit(new EditTransaction.SuccessAction() {
			public void run() {
				try {
					Animations.drawExpandingRing(getPanel().getViewport()
							.toViewPoint(newVertex), false, Color.green,
							getPanel(), null);
				} catch (Throwable t) {
					getPanel().getContext().warnUser(t.toString());
				}
			}
		});
	}

	private Map<Layer,Collection<Feature>> layerToFeaturesInRangeMap() {
		Map<Layer,Collection<Feature>> layerToFeaturesInRangeMap = new HashMap<>();
		for (Layer editableLayer : getPanel().getLayerManager().getEditableLayers()) {
			Collection<Feature> featuresInRange = featuresInRange(
					modelClickCoordinate(), editableLayer);
			if (!featuresInRange.isEmpty()) {
				layerToFeaturesInRangeMap.put(editableLayer, featuresInRange);
			}
		}
		return layerToFeaturesInRangeMap;
	}

	public Icon getIcon() {
		return IconLoader.icon("InsertVertex.gif");
	}

	public Cursor getCursor() {
		return createCursor(IconLoader.icon("PlusCursor.gif").getImage());
	}
}
