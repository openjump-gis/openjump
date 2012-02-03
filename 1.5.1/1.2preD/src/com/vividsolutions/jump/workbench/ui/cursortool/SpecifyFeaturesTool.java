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

import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeIntersector;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * Allows the user to specify features by clicking on them or drawing a box
 * around them. Works with invalid features (using EnvelopeIntersector).
 */
public abstract class SpecifyFeaturesTool extends DragTool {
	public SpecifyFeaturesTool() {
	}

	protected Iterator candidateLayersIterator() {
		return getPanel().getLayerManager().iterator();
	}

	/**
	 * @param envelope
	 *            the envelope, which may have zero area
	 * @return those features of the layer that intersect the given envelope; an
	 *         empty FeatureCollection if no features intersect it
	 */
	private static Set intersectingFeatures(Layer layer, Envelope envelope) {
		HashSet intersectingFeatures = new HashSet();
		List candidateFeatures = layer.getFeatureCollectionWrapper().query(
				envelope);
		String a = "" + layer.getFeatureCollectionWrapper().getUltimateWrappee();
		String b = "" + layer.getFeatureCollectionWrapper().getUltimateWrappee().size();
		for (Iterator i = candidateFeatures.iterator(); i.hasNext();) {
			Feature feature = (Feature) i.next();

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

	protected Set specifiedFeatures() throws NoninvertibleTransformException {
		HashSet allFeatures = new HashSet();

		for (Iterator i = layerToSpecifiedFeaturesMap().values().iterator(); i
				.hasNext();) {
			Set features = (Set) i.next();
			allFeatures.addAll(features);
		}

		return allFeatures;
	}

	/**
	 * Returns the layers containing the specified features, and the specified
	 * features themselves.
	 */
	protected Map layerToSpecifiedFeaturesMap()
			throws NoninvertibleTransformException {
		return layerToSpecifiedFeaturesMap(candidateLayersIterator(),
				getBoxInModelCoordinates());
	}

	public static Map layerToSpecifiedFeaturesMap(Iterator layerIterator,
			Envelope boxInModelCoordinates)
			throws NoninvertibleTransformException {
		HashMap layerToFeaturesMap = new HashMap();

		for (Iterator i = layerIterator; i.hasNext();) {
			Layer layer = (Layer) i.next();

			if (!layer.isVisible()) {
				continue;
			}

			Set intersectingFeatures = intersectingFeatures(layer,
					boxInModelCoordinates);

			if (intersectingFeatures.isEmpty()) {
				continue;
			}

			layerToFeaturesMap.put(layer, intersectingFeatures);
		}

		return layerToFeaturesMap;
	}

	/**
	 * @param layers
	 *            Layers to filter in
	 */
	protected Collection specifiedFeatures(Collection layers)
			throws NoninvertibleTransformException {
		ArrayList specifiedFeatures = new ArrayList();
		Map layerToSpecifiedFeaturesMap = layerToSpecifiedFeaturesMap();

		for (Iterator i = layerToSpecifiedFeaturesMap.keySet().iterator(); i
				.hasNext();) {
			Layer layer = (Layer) i.next();

			if (!layers.contains(layer)) {
				continue;
			}

			specifiedFeatures.addAll((Collection) layerToSpecifiedFeaturesMap
					.get(layer));
		}

		return specifiedFeatures;
	}
}