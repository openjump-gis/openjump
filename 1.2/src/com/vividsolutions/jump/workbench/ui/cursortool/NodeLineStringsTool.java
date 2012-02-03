package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class NodeLineStringsTool extends AbstractClickSelectedLineStringsTool {
	private class Intersection implements Comparable {
		public Intersection(Coordinate coordinate, Feature featureA,
				Layer layerA, Feature featureB, Layer layerB) {
			this.coordinate = coordinate;
			this.featureA = featureA;
			this.layerA = layerA;
			this.featureB = featureB;
			this.layerB = layerB;
		}

		private Coordinate coordinate;

		private Feature featureA;

		private Feature featureB;

		private Layer layerA;

		private Layer layerB;

		public int compareTo(Object o) {
			return coordinate.compareTo(((Intersection) o).coordinate);
		}
	}

    private final static String sNoIntersectionsHere = I18N.get("com.vividsolutions.jump.workbench.ui.cursortool.NodeLineStringsTool.No-intersections-here");
    
    public String getName() {
        return I18N.get("com.vividsolutions.jump.workbench.ui.cursortool.NodeLineStringsTool.Node-LineStrings");
    }
    
	protected void gestureFinished(Collection nearbyLineStringFeatures)
			throws NoninvertibleTransformException {
		Intersection intersection = closest(getModelClickPoint(),
				CollectionUtil.select(
						properIntersections(nearbyLineStringFeatures,
								layerToSpecifiedFeaturesMap()), new Block() {
							public Object yield(Object intersection) {
								try {
									return getBoxInModelCoordinates()
											.contains(
													((Intersection) intersection).coordinate) ? Boolean.TRUE
											: Boolean.FALSE;
								} catch (NoninvertibleTransformException e) {
									// Not critical. Eat it. [Jon Aquino
									// 2004-10-25]
									return Boolean.FALSE;
								}
							}
						}));
		if (intersection == null) {
			getWorkbench().getFrame().warnUser(sNoIntersectionsHere);
			return;
		}
		if (!intersection.layerA.isEditable()) {
			warnLayerNotEditable(intersection.layerA);
			return;
		}
		if (!intersection.layerB.isEditable()) {
			warnLayerNotEditable(intersection.layerB);
			return;
		}
		new SplitLineStringsOp(Color.magenta).addSplit(intersection.featureA,
				intersection.coordinate, intersection.layerA, true).addSplit(
				intersection.featureB, intersection.coordinate,
				intersection.layerB, true).execute(getName(),
				isRollingBackInvalidEdits(), getPanel());
	}

	private Intersection closest(Point p, Collection intersections) {
		Intersection closestIntersection = null;
		double closestDistance = Double.MAX_VALUE;
		for (Iterator i = intersections.iterator(); i.hasNext();) {
			Intersection intersection = (Intersection) i.next();
			double distance = intersection.coordinate.distance(p
					.getCoordinate());
			if (distance < closestDistance) {
				closestIntersection = intersection;
				closestDistance = distance;
			}
		}
		return closestIntersection;
	}

	private Set properIntersections(Collection nearbyLineStringFeatures,
			Map layerToFeaturesMap) {
		TreeSet intersections = new TreeSet();
		for (Iterator i = nearbyLineStringFeatures.iterator(); i.hasNext();) {
			Feature a = (Feature) i.next();
			for (Iterator j = nearbyLineStringFeatures.iterator(); j.hasNext();) {
				Feature b = (Feature) j.next();
				if (a == b) {
					continue;
				}
				for (Iterator k = Arrays.asList(
						a.getGeometry().intersection(b.getGeometry())
								.getCoordinates()).iterator(); k.hasNext();) {
					Coordinate coordinate = (Coordinate) k.next();
					if (coordinate.equals2D(first(a))
							|| coordinate.equals2D(last(a))
							|| coordinate.equals2D(first(b))
							|| coordinate.equals2D(last(b))) {
						continue;
					}
					intersections.add(new Intersection(coordinate, a, layer(a,
							layerToFeaturesMap), b,
							layer(b, layerToFeaturesMap)));
				}
			}
		}
		return intersections;
	}

	private Coordinate first(Feature lineStringFeature) {
		return lineString(lineStringFeature).getCoordinateN(0);
	}

	private LineString lineString(Feature feature) {
		return (LineString) ((Feature) feature).getGeometry();
	}

	private Coordinate last(Feature lineStringFeature) {
		return lineString(lineStringFeature).getCoordinateN(
				lineString(lineStringFeature).getNumPoints() - 1);
	}

	public Icon getIcon() {
		return IconLoader.icon("SplitLinestringsAtIntersection.gif");
	}

}