package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.algorithm.LengthSubstring;
import com.vividsolutions.jump.algorithm.LengthToPoint;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;

public class SplitLineStringsOp {
	private Color colour;

	public SplitLineStringsOp addSplit(Feature feature, Coordinate target,
			Layer layer, boolean moveSplitToTarget) {
		splits.add(new Split(feature, split((LineString) feature.getGeometry(),
				target, moveSplitToTarget), layer));
		return this;
	}

	public SplitLineStringsOp(Color colour) {
		this.colour = colour; }
	
	public void execute(String name, boolean rollingBackInvalidEdits,
			LayerViewPanel panel) {
		execute(splits, name, rollingBackInvalidEdits, panel);
	}

	private Collection splits = new ArrayList();

	private void assertIndependent(Collection splits) {
		Collection splitsEncountered = new ArrayList();
		for (Iterator i = splits.iterator(); i.hasNext();) {
			Split split = (Split) i.next();
			Assert.isTrue(!splitsEncountered.contains(split));
			splitsEncountered.add(split);
		}
	}

	private EditTransaction transaction(final Split split, final String name,
			boolean rollingBackInvalidEdits, LayerViewPanel panel) {
		//Bind SelectionManager [Jon Aquino 2004-10-25]
		final SelectionManager selectionManager = panel.getSelectionManager();
		EditTransaction transaction = new EditTransaction(
				Collections.EMPTY_LIST, name, split.layer,
				rollingBackInvalidEdits, true, panel) {
			protected UndoableCommand createCommand() {
				final UndoableCommand command = super.createCommand();
				return new UndoableCommand(name) {
					public void execute() {
						boolean oldFeatureWasSelected = selectionManager
								.getFeaturesWithSelectedItems().contains(
										split.oldFeature);
						command.execute();
						if (oldFeatureWasSelected) {
							selectionManager.getFeatureSelection().selectItems(
									split.layer,
									Arrays.asList(split.newFeatures));
						}
					}

					public void unexecute() {
						boolean newFeatureWasSelected = selectionManager
								.getFeaturesWithSelectedItems().contains(
										split.newFeatures[0])
								|| selectionManager
										.getFeaturesWithSelectedItems()
										.contains(split.newFeatures[1]);
						command.unexecute();
						if (newFeatureWasSelected) {
							selectionManager.getFeatureSelection().selectItems(
									split.layer,
									Collections.singleton(split.oldFeature));
						}
					}
				};
			}
		};
		transaction.deleteFeature(split.oldFeature);
		transaction.createFeature(split.newFeatures[0]);
		transaction.createFeature(split.newFeatures[1]);
		return transaction;
	}

	private void execute(final Collection splits, final String name,
			final boolean rollingBackInvalidEdits, final LayerViewPanel panel) {
		assertIndependent(splits);
		EditTransaction.commit(CollectionUtil.collect(splits, new Block() {
			public Object yield(Object split) {
				return transaction((Split) split, name,
						rollingBackInvalidEdits, panel);
			}
		}), new EditTransaction.SuccessAction() {
			public void run() {
				try {
					Animations.drawExpandingRings(new HashSet(CollectionUtil
							.collect(splits, new Block() {
								public Object yield(Object split) {
									try {
										return panel
												.getViewport()
												.toViewPoint(
														((Split) split).newLineStrings[0]
																.getEndPoint()
																.getCoordinate());
									} catch (NoninvertibleTransformException e) {
										// Not a big deal. Eat it. [Jon Aquino
										// 2004-10-25]
										return new Point2D.Double();
									}
								}
							})), true, colour, panel, new float[] { 5, 5 });
				} catch (Throwable t) {
					panel.getContext().warnUser(t.toString());
				}
			}
		});
	}

	protected LineString[] split(LineString lineString, Coordinate target,
			boolean moveSplitToTarget) {
		LineString[] lineStrings = new LineString[] {
				LengthSubstring.getSubstring(lineString, 0, LengthToPoint
						.length(lineString, target)),
				LengthSubstring.getSubstring(lineString, LengthToPoint.length(
						lineString, target), lineString.getLength()) };
		if (moveSplitToTarget) {
			last(lineStrings[0]).setCoordinate(target);
			first(lineStrings[1]).setCoordinate(target);
		}
		if (Double.isNaN(last(lineStrings[0]).z)) {
			last(lineStrings[0]).z = interpolateZ(lineStrings);
		}
		if (Double.isNaN(first(lineStrings[1]).z)) {
			first(lineStrings[1]).z = interpolateZ(lineStrings);
		}
		return lineStrings;
	}

	private double interpolateZ(LineString[] lineStrings) {
		Coordinate a = secondToLast(lineStrings[0]);
		Coordinate b = last(lineStrings[0]);
		Coordinate c = second(lineStrings[1]);
		if (Double.isNaN(a.z)) {
			return Double.NaN;
		}
		if (Double.isNaN(c.z)) {
			return Double.NaN;
		}
		return a.z + (c.z - a.z) * a.distance(b)
				/ (a.distance(b) + b.distance(c));
	}

	private Coordinate first(LineString lineString) {
		return lineString.getCoordinateN(0);
	}

	private Coordinate second(LineString lineString) {
		return lineString.getCoordinateN(1);
	}

	private Coordinate last(LineString lineString) {
		return lineString.getCoordinateN(lineString.getNumPoints() - 1);
	}

	private Coordinate secondToLast(LineString lineString) {
		return lineString.getCoordinateN(lineString.getNumPoints() - 2);
	}

	private class Split {
		private Feature[] newFeatures;

		public Split(Feature oldFeature, LineString[] newLineStrings,
				Layer layer) {
			this.oldFeature = oldFeature;
			this.newLineStrings = newLineStrings;
			this.layer = layer;
			this.newFeatures = new Feature[] {
					clone(oldFeature, newLineStrings[0]),
					clone(oldFeature, newLineStrings[1]) };
		}

		private Feature oldFeature;

		private LineString[] newLineStrings;

		private Layer layer;

		private Feature clone(Feature feature, LineString lineString) {
			Feature clone = (Feature) feature.clone();
			clone.setGeometry(lineString);
			return clone;
		}
	}
}