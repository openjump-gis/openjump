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
import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
public class SnapVerticesToSelectedVertexClickTool extends NClickTool {
    private EnableCheckFactory checkFactory;
    private GeometryFactory factory = new GeometryFactory();
    public SnapVerticesToSelectedVertexClickTool(EnableCheckFactory checkFactory) {
        super(1);
        this.checkFactory = checkFactory;
    }
    public Icon getIcon() {
        Assert.shouldNeverReachHere();
        return null;
    }
    protected void gestureFinished() throws java.lang.Exception {
        reportNothingToUndoYet();
        final Coordinate clickCoordinate = (Coordinate) getCoordinates().get(0);
        if (!check(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))) {
            return;
        }
        if (!check(checkFactory.createFenceMustBeDrawnCheck())) {
            return;
        }
        if (!check(new EnableCheck() {
            public String check(JComponent component) {
                if (!getPanel().getFence().contains(factory.createPoint(clickCoordinate))) {
                    return I18N.get("ui.cursortool.editing.SnapVerticesToSelectedVertexClickTool.please-click-inside-the-fence"); }
                if (getPanel().getSelectionManager().getSelectedItems().isEmpty()) {
                    return I18N.get("ui.cursortool.editing.SnapVerticesToSelectedVertexClickTool.select-a-feature-part-or-linestring-in-the-fence-containing-the-vertex-to-snap-to");
                        }
                if (getPanel().getSelectionManager().getSelectedItems().size() > 1) {
                    return I18N.get("ui.cursortool.editing.SnapVerticesToSelectedVertexClickTool.select-only-one-feature-part-or-linestring-containing-the-vertex-to-snap-to");
                        }
                return null; }
        })) {
            return;
        }
        new SnapVerticesOp().execute(
            getPanel().getFence(),
            getPanel().getLayerManager().getEditableLayers(),
            isRollingBackInvalidEdits(),
            getPanel(),
            getTaskFrame().getTask(),
            clickCoordinate,
            (Feature) getPanel()
                .getSelectionManager()
                .getFeaturesWithSelectedItems(
                    (Layer) getPanel()
                        .getSelectionManager()
                        .getLayersWithSelectedItems()
                        .iterator()
                        .next())
                .iterator()
                .next(),
            getWorkbench().getBlackboard().get(
                SnapVerticesOp.INSERT_VERTICES_IF_NECESSARY_KEY,
                true));
    }
    protected Shape getShape() throws NoninvertibleTransformException {
        return null;
    }

}
