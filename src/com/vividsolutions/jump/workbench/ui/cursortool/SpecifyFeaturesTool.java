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

package com.vividsolutions.jump.workbench.ui.cursortool;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeIntersector;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;

import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;

/**
 * Allows the user to specify features by clicking on them or drawing a box
 * around them. Works with invalid features (using EnvelopeIntersector).
 */
public abstract class SpecifyFeaturesTool extends DragTool {

	public SpecifyFeaturesTool(WorkbenchContext context) {
    super(context);
  }

  protected Iterator<Layer> candidateLayersIterator() {
		return getPanel().getLayerManager().iterator(Layer.class);
	}

	/**
	 * @param layer a Layer
	 * @param envelope
	 *            the envelope, which may have zero area
	 * @return those features of the layer that intersect the given envelope; an
	 *         empty FeatureCollection if no features intersect it
	 */
	private static Set<Feature> intersectingFeatures(Layer layer, Envelope envelope) {
		HashSet<Feature> intersectingFeatures = new HashSet<>();
		List<Feature> candidateFeatures = layer.getFeatureCollectionWrapper().query(
				envelope);
		for (Feature feature : candidateFeatures) {

			// optimization - if the feature envelope is completely inside the
			// query envelope it must be selected
			if (envelope.contains(feature.getGeometry().getEnvelopeInternal())
					|| EnvelopeIntersector.intersects(feature.getGeometry(),
							envelope)) {
				intersectingFeatures.add(feature);
			}
		}

		return intersectingFeatures;
	}

	public void mouseClicked(MouseEvent e) {
		try {
			super.mouseClicked(e);
			setViewSource(e.getPoint());
			setViewDestination(e.getPoint());
			fireGestureFinished();
		} catch (Throwable t) {
			getPanel().getContext().handleThrowable(t);
		}
	}

	protected Set<Feature> specifiedFeatures() throws NoninvertibleTransformException {
		HashSet<Feature> allFeatures = new LinkedHashSet<>();

		for (Set<Feature> features : layerToSpecifiedFeaturesMap().values()) {
			allFeatures.addAll(features);
		}

		return allFeatures;
	}

	/**
	 * Returns the layers containing the specified features, and the specified
	 * features themselves.
	 * @return a Map mapping layers to features of this layer and intersecting the box
	 * @throws NoninvertibleTransformException if a problem occurs during intersection operation
	 */
	protected Map<Layer,Set<Feature>> layerToSpecifiedFeaturesMap()
			throws NoninvertibleTransformException {
		return layerToSpecifiedFeaturesMap(candidateLayersIterator(),
				getBoxInModelCoordinates());
	}

	public static Map<Layer,Set<Feature>> layerToSpecifiedFeaturesMap(Iterator<Layer> layerIterator,
			Envelope boxInModelCoordinates)
			throws NoninvertibleTransformException {
		HashMap<Layer,Set<Feature>> layerToFeaturesMap = new HashMap<>();

		while (layerIterator.hasNext()) {
			Layer layer = layerIterator.next();

			if (!layer.isVisible()) {
				continue;
			}

			Set<Feature> intersectingFeatures = intersectingFeatures(layer,
					boxInModelCoordinates);

			if (intersectingFeatures.isEmpty()) {
				continue;
			}

			layerToFeaturesMap.put(layer, intersectingFeatures);
		}

		return layerToFeaturesMap;
	}

	/**
	 * @param layers Layers to filter in
	 * @return a collection of features belonging to layers and intersecting the box
	 * @throws NoninvertibleTransformException if a problem occurs during intersection operation
	 */
	protected Collection<Feature> specifiedFeatures(Collection<Layer> layers)
			throws NoninvertibleTransformException {
		ArrayList<Feature> specifiedFeatures = new ArrayList<>();
		Map<Layer,Set<Feature>> layerToSpecifiedFeaturesMap = layerToSpecifiedFeaturesMap();

		for (Layer layer : layerToSpecifiedFeaturesMap.keySet()) {

			if (!layers.contains(layer)) {
				continue;
			}

			specifiedFeatures.addAll(layerToSpecifiedFeaturesMap.get(layer));
		}

		return specifiedFeatures;
	}
}