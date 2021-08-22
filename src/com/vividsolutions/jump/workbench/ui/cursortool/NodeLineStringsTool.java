package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Icon;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class NodeLineStringsTool extends AbstractClickSelectedLineStringsTool {

  public NodeLineStringsTool(WorkbenchContext context) {
    super(context);
  }

  private static class Intersection implements Comparable<Intersection> {
		public Intersection(Coordinate coordinate, Feature featureA,
				Layer layerA, Feature featureB, Layer layerB) {
			this.coordinate = coordinate;
			this.featureA = featureA;
			this.layerA = layerA;
			this.featureB = featureB;
			this.layerB = layerB;
		}

		private final Coordinate coordinate;
		private final Feature featureA;
		private final Feature featureB;
		private final Layer layerA;
		private final Layer layerB;

		public int compareTo(Intersection o) {
			return coordinate.compareTo(o.coordinate);
		}
	}

    private final static String sNoIntersectionsHere =
				I18N.getInstance()
						.get("com.vividsolutions.jump.workbench.ui.cursortool.NodeLineStringsTool.No-intersections-here");
    
    public String getName() {
        return I18N.getInstance()
						.get("com.vividsolutions.jump.workbench.ui.cursortool.NodeLineStringsTool.Node-LineStrings");
    }
    
	protected void gestureFinished(Collection<Feature> nearbyLineStringFeatures)
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
									// Not critical. Eat it. [Jon Aquino 2004-10-25]
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

	private Intersection closest(Point p, Collection<Intersection> intersections) {
		Intersection closestIntersection = null;
		double closestDistance = Double.MAX_VALUE;
		for (Intersection intersection : intersections) {
			double distance = intersection.coordinate.distance(p
					.getCoordinate());
			if (distance < closestDistance) {
				closestIntersection = intersection;
				closestDistance = distance;
			}
		}
		return closestIntersection;
	}

	private Set<Intersection> properIntersections(Collection<Feature> nearbyLineStringFeatures,
			Map<Layer,Set<Feature>> layerToFeaturesMap) {
		TreeSet<Intersection> intersections = new TreeSet<>();
		for (Feature a : nearbyLineStringFeatures) {
			for (Feature b : nearbyLineStringFeatures) {
				if (a == b) {
					continue;
				}
				for (Coordinate coordinate :
						a.getGeometry().intersection(b.getGeometry()).getCoordinates()) {
					if (coordinate.equals2D(first(a))
							|| coordinate.equals2D(last(a))
							|| coordinate.equals2D(first(b))
							|| coordinate.equals2D(last(b))) {
						continue;
					}
					intersections.add(new Intersection(coordinate,
							a, layer(a, layerToFeaturesMap),
							b, layer(b, layerToFeaturesMap))
					);
				}
			}
		}
		return intersections;
	}

	private Coordinate first(Feature lineStringFeature) {
		return lineString(lineStringFeature).getCoordinateN(0);
	}

	private LineString lineString(Feature feature) {
		return (LineString) feature.getGeometry();
	}

	private Coordinate last(Feature lineStringFeature) {
		return lineString(lineStringFeature).getCoordinateN(
				lineString(lineStringFeature).getNumPoints() - 1);
	}

	public Icon getIcon() {
		return IconLoader.icon("SplitLinestringsAtIntersection.gif");
	}

}