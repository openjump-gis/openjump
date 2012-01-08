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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SpecifyFeaturesTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.VerticesInFencePlugIn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;


public class SnapVerticesTool extends SpecifyFeaturesTool {
    private EnableCheckFactory checkFactory;

    public SnapVerticesTool(EnableCheckFactory checkFactory) {
        this.checkFactory = checkFactory;
        setColor(Color.green.darker());
        setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0));

        //The cursor is 18 pixels long and wide
        setViewClickBuffer(8);
    }

    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();

        if (!check(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))) {
            return;
        }

        Coordinate suggestedTarget = EnvelopeUtil.centre(getBoxInModelCoordinates());
        final Feature targetFeature = targetFeature(suggestedTarget,
                getBoxInModelCoordinates());

        if (!check(new EnableCheck() {
                    public String check(JComponent component) {
                        return targetFeature == null
                            ? I18N.get("ui.cursortool.editing.SnapVerticesTool.no-vertices-or-edges-here") : null;
                    }
                })
        ) {
            return;
        }

        snapVertices(getPanel().getLayerManager().getEditableLayers(),
            suggestedTarget, targetFeature);
    }

    protected void snapVertices(Collection editableLayers,
        Coordinate suggestedTarget, final Feature targetFeature)
        throws Exception, NoninvertibleTransformException {
        new SnapVerticesOp().execute(EnvelopeUtil.toGeometry(
                getBoxInModelCoordinates()), editableLayers,
            isRollingBackInvalidEdits(), getPanel(), getTaskFrame().getTask(),
            suggestedTarget, targetFeature,
            getWorkbench().getBlackboard().get(SnapVerticesOp.INSERT_VERTICES_IF_NECESSARY_KEY,
                true));
    }

    private Feature targetFeature(Coordinate suggestedTarget, Envelope fence)
        throws NoninvertibleTransformException {
        Feature targetFeature = targetFeature(suggestedTarget, fence, false);

        if (targetFeature == null) {
            targetFeature = targetFeature(suggestedTarget, fence, true);
        }

        return targetFeature;
    }

    private Feature targetFeature(Coordinate suggestedTarget, Envelope fence,
        boolean fromEditableLayers) throws NoninvertibleTransformException {
        ArrayList candidateFeatures = new ArrayList();
        Map layerToSpecifiedFeaturesMap = layerToSpecifiedFeaturesMap();

        for (Iterator i = layerToSpecifiedFeaturesMap.keySet().iterator();
                i.hasNext();) {
            Layer layer = (Layer) i.next();

            if (layer.isEditable() != fromEditableLayers) {
                continue;
            }

            if (layer.getName().equals(FenceLayerFinder.LAYER_NAME)) {
                continue;
            }

            candidateFeatures.addAll((Collection) layerToSpecifiedFeaturesMap.get(
                    layer));
        }

        Feature targetFeature = null;
        double distanceToTargetVertices = -1;

        for (Iterator i = candidateFeatures.iterator(); i.hasNext();) {
            Feature candidate = (Feature) i.next();
            double distanceToCandidateVertices = distanceToVertices(suggestedTarget,
                    candidate, fence);

            if (distanceToCandidateVertices == -1) {
                continue;
            }

            if (targetFeature == null ||
                    distanceToCandidateVertices < distanceToTargetVertices) {
                targetFeature = candidate;
                distanceToTargetVertices = distanceToCandidateVertices;
            }
        }

        return targetFeature;

        //If anyone ever modifies this method to look for the feature with the
        //closest *segment* rather than the closest vertex (and I think that this 
        //would be a good modification because it would snap lines to vertices
        //rather than vice versa), remember to handle points and multipoints,
        //which have no segments. Also, remember to count only the portion of
        //the line segment lying inside the fence (actually I don't account for this
        //in GeometryEditor#insertVertex (used by SnapVerticesOp); this omission 
        //could lead to some snaps occurring outside the fence in rare cases). [Jon Aquino]
    }

    /**
     * @return -1 if the feature has no vertices in the Envelope
     */
    private double distanceToVertices(Coordinate referenceCoordinate,
        Feature feature, Envelope vertexFilter) {
        double distanceToVertices = -1;

        for (Iterator i = VerticesInFencePlugIn.verticesInFence(
                    feature.getGeometry(),
                    EnvelopeUtil.toGeometry(vertexFilter), true).getCoordinates()
                                               .iterator(); i.hasNext();) {
            Coordinate vertex = (Coordinate) i.next();
            double distanceToVertex = vertex.distance(referenceCoordinate);

            if (distanceToVertices == -1 ||
                    distanceToVertex < distanceToVertices) {
                distanceToVertices = distanceToVertex;
            }
        }

        return distanceToVertices;
    }

    public Icon getIcon() {
        return IconLoader.icon("QuickSnap.gif");
    }

    public Cursor getCursor() {
        return AbstractCursorTool.createCursor(IconLoader.icon(
                "QuickSnapCursor.gif").getImage());
    }

    protected Envelope getBoxInModelCoordinates()
        throws NoninvertibleTransformException {
        return EnvelopeUtil.expand(new Envelope(getModelSource(),
                getModelDestination()), modelClickBuffer());
    }

    protected Shape getShape() throws Exception {
        return getPanel().getViewport().toViewRectangle(getBoxInModelCoordinates());
    }
}
