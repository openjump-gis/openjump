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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GeometryEditor;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.Animations;
import com.vividsolutions.jump.workbench.ui.plugin.VerticesInFencePlugIn;

public class SnapVerticesOp {

    public static final String INSERT_VERTICES_IF_NECESSARY_KEY =
        SnapVerticesOp.class.getName() + " - INSERT_VERTICES_IF_NECESSARY";

    private final static String NO_TARGET_VERTICES_IN_FENCE_WARNING =
    	I18N.get("ui.cursortool.editing.SnapVerticesOp.fence-contains-no-vertices-of-the-selected-feature-part-or-linestring");

    public SnapVerticesOp() {}

    private Collection featuresInFence(Layer layer, Geometry fence, LayerViewPanel panel) {
        Collection featuresInFence =
            (Collection) panel.visibleLayerToFeaturesInFenceMap(fence).get(layer);

        if (featuresInFence == null) {
            return new ArrayList();
        }

        return featuresInFence;
    }        

    /**
     * @return null if the geometries have no vertices in the fence
     */
    public Coordinate pickTarget(
        Geometry targetGeometry,
        Geometry fence,
        Coordinate suggestedTarget)
        throws Exception {
        Collection verticesInFence =
            VerticesInFencePlugIn.verticesInFence(targetGeometry, fence, true).getCoordinates();

        if (verticesInFence.isEmpty()) {
            return null;
        }

        return CoordUtil.closest(verticesInFence, suggestedTarget);
    }

    /**
     * @param insertVerticesIfNecessary whether to insert vertices into
     * editable features with line segments (but not vertices) inside the fence
     */
    public boolean execute(
        Geometry fence,
        Collection editableLayers,
        boolean rollingBackInvalidEdits,
        final LayerViewPanel panel,
        Task task,
        Coordinate suggestedTarget,
        Feature targetFeature,
        boolean insertVerticesIfNecessary)
        throws Exception {

        Map editableLayerToFeaturesInFenceMap =
            editableLayerToFeaturesInFenceMap(editableLayers, fence, panel);

        Collection editableFeatures =
            CollectionUtil.concatenate(editableLayerToFeaturesInFenceMap.values());

        if (editableFeatures.isEmpty()) {
            panel.getContext().warnUser(I18N.get("ui.cursortool.editing.SnapVerticesOp.fence-contains-no-features-from-editable-layers"));
            return false;
        }

        if (VerticesInFencePlugIn
            .verticesInFence(targetFeature.getGeometry(), fence, true)
            .getCoordinates()
            .isEmpty()
            && VerticesInFencePlugIn
                .verticesInFence(FeatureUtil.toGeometries(editableFeatures), fence, true)
                .isEmpty()) {
            panel.getContext().warnUser(NO_TARGET_VERTICES_IN_FENCE_WARNING);
            return false;
        }

        Geometry targetGeometry = targetFeature.getGeometry();

        List transactions = new ArrayList();
        for (Iterator i = editableLayers.iterator(); i.hasNext();) {
            Layer editableLayer = (Layer) i.next();
            Collection featuresInFence =
                (Collection) editableLayerToFeaturesInFenceMap.get(editableLayer);
            EditTransaction transaction =
                new EditTransaction(
                    featuresInFence,
                    I18N.get("ui.cursortool.editing.SnapVerticesOp.snap-vertices-together"),
                    editableLayer,
                    rollingBackInvalidEdits,
                    false,
                    panel);
            transactions.add(transaction);
            if (insertVerticesIfNecessary) {
                insertVerticesIfNecessary(transaction, suggestedTarget, fence);
                //Target geometry may have had a vertex inserted. [Jon Aquino]
                if (featuresInFence.contains(targetFeature)) {
                    targetGeometry = transaction.getGeometry(targetFeature);
                }
            }
        }

        final Coordinate target = pickTarget(targetGeometry, fence, suggestedTarget);
        if (target == null) {
            //Can get here if targetFeature is not on the editable layer. [Jon Aquino]
            panel.getContext().warnUser(NO_TARGET_VERTICES_IN_FENCE_WARNING);
            return false;
        }

        boolean geometryChanged = moveVertices(transactions, fence, target);
        if (!geometryChanged) {
            return true;
        }

        return EditTransaction.commit(transactions, new EditTransaction.SuccessAction() {
            public void run() {
                try {
                    indicateSuccess(target, panel);
                } catch (Throwable t) {
                    panel.getContext().warnUser(t.toString());
                }
            }
        });
    }

    private boolean moveVertices(List transactions, Geometry fence, final Coordinate target) {
        boolean geometryChanged = false;
        for (Iterator i = transactions.iterator(); i.hasNext();) {
            EditTransaction transaction = (EditTransaction) i.next();
            //for (int j = 0; j < transaction.size(); j++) {
            for (Iterator<Feature> j = transaction.getFeatures().iterator() ; j.hasNext() ;) {
                //Geometry proposedGeometry = (Geometry) transaction.getGeometry(j);
                Feature feature = j.next();
                Geometry proposedGeometry = transaction.getGeometry(feature);
                move(
                    VerticesInFencePlugIn
                        .verticesInFence(proposedGeometry, fence, false)
                        .getCoordinates(),
                    target);
                try {
                    proposedGeometry = geometryEditor.removeRepeatedPoints(proposedGeometry);
                } catch (IllegalArgumentException e) {
                    Assert.isTrue(
                        e.getMessage().toLowerCase().indexOf("point") > -1
                            && e.getMessage().toLowerCase().indexOf(">") > -1,
                        "I assumed that we would get here only if too few points "
                            + "were passed into the Geometry constructor [Jon Aquino]");
                    proposedGeometry =
                        new Point(
                            target,
                            proposedGeometry.getPrecisionModel(),
                            proposedGeometry.getSRID());
                }
                //transaction.setGeometry(j, proposedGeometry);
                transaction.setGeometry(feature, proposedGeometry);
            }
            //Brute force check to see whether we should skip showing the animated
            //indicator [Jon Aquino]
            geometryChanged = geometryChanged || !coordinatesEqual(transaction, fence);
        }
        return geometryChanged;
    }

    private Map editableLayerToFeaturesInFenceMap(
        Collection editableLayers,
        Geometry fence,
        final LayerViewPanel panel) {
        Map editableLayerToFeaturesInFenceMap = new HashMap();
        for (Iterator i = editableLayers.iterator(); i.hasNext();) {
            Layer editableLayer = (Layer) i.next();
            Assert.isTrue(editableLayer.isEditable());
            editableLayerToFeaturesInFenceMap.put(
                editableLayer,
                featuresInFence(editableLayer, fence, panel));
        }
        return editableLayerToFeaturesInFenceMap;
    }

    private boolean coordinatesEqual(EditTransaction transaction, Geometry fence) {
        //for (int i = 0; i < transaction.size(); i++) {
        for (Iterator<Feature> i = transaction.getFeatures().iterator() ; i.hasNext() ; ) {
            Feature originalFeature = i.next();
            //Feature originalFeature = transaction.getFeature(i);
            //Geometry newGeometry = transaction.getGeometry(i);
            Geometry newGeometry = transaction.getGeometry(originalFeature);

            if (!coordinatesEqual(VerticesInFencePlugIn
                .verticesInFence(originalFeature.getGeometry(), fence, true)
                .getCoordinates(),
                VerticesInFencePlugIn
                    .verticesInFence(newGeometry, fence, true)
                    .getCoordinates())) {
                return false;
            }
        }

        return true;
    }

    private boolean coordinatesEqual(List a, List b) {
        if (a.size() != b.size()) {
            return false;
        }

        TreeSet A = new TreeSet(a);
        TreeSet B = new TreeSet(b);

        if (A.size() != B.size()) {
            return false;
        }

        Iterator Ai = A.iterator();
        Iterator Bi = B.iterator();

        while (Ai.hasNext()) {
            if (!Ai.next().equals(Bi.next())) {
                return false;
            }
        }

        return true;
    }

    private void indicateSuccess(Coordinate target, LayerViewPanel panel)
        throws NoninvertibleTransformException {
        Point2D center = panel.getViewport().toViewPoint(CoordUtil.toPoint2D(target));
        Animations.drawExpandingRing(center, false, Color.green, panel, null);
    }

    private void move(Collection verticesToMove, Coordinate target) {
        for (Iterator i = verticesToMove.iterator(); i.hasNext();) {
            Coordinate vertexToMove = (Coordinate) i.next();
            vertexToMove.setCoordinate(target);
        }
    }

    private int insertVerticesIfNecessary(
        final EditTransaction transaction,
        final Coordinate target,
        final Geometry fence)
        throws NoninvertibleTransformException {
        //Trick: Wrap count in array to avoid "must be declared final" warnings. [Jon Aquino]
        final int[] verticesInserted = new int[] { 0 };

        //for (int i = 0; i < transaction.size(); i++) {
        for (Iterator<Feature> i = transaction.getFeatures().iterator() ; i.hasNext(); ) {
            //GeometryEditor is being used in two ways here. GeometryEditor#edit 
            //recurses through GeometryCollection/Polygon elements (if any). 
            //GeometryEditor#insertVertex does the vertex insertion on each
            //Geometry or GeometryCollection/Polygon element. [Jon Aquino]
            Feature feature = i.next();
            transaction.setGeometry(feature, geometryEditor.edit(
                            transaction.getGeometry(feature),
                            new GeometryEditor.GeometryEditorOperation() {
                public Geometry edit(Geometry geometry) {
                    if (geometry instanceof Polygon) {
                        //Wait for the individual LinearRings to come in. [Jon Aquino]
                        return geometry;
                    }
                    if (geometry instanceof GeometryCollection) {
                        return geometry;
                    }
                    if (!fence.intersects(geometry)) {
                        //A part of the feature that doesn't lie inside the fence. [Jon Aquino]
                        return geometry;
                    }
                    if (!VerticesInFencePlugIn
                        .verticesInFence(geometry, fence, true)
                        .getCoordinates()
                        .isEmpty()) {
                        return geometry;
                    }
                    verticesInserted[0] = verticesInserted[0] + 1;
                    //Important to pass in the fence, so that vertex isn't inserted into
                    //a segment that doesn't intersect the fence. [Jon Aquino]
                    Geometry newGeometry = geometryEditor.insertVertex(geometry, target, fence);
                    Assert.isTrue(newGeometry != null);
                    return newGeometry;
                }
            }));

        }

        return verticesInserted[0];
    }

    private GeometryEditor geometryEditor = new GeometryEditor();
}
