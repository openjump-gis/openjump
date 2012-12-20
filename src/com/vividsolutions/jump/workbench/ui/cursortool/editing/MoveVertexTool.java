
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
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;

public class MoveVertexTool extends DragTool {

    public final static int TOLERANCE = 5;

    private EnableCheckFactory checkFactory;

    public MoveVertexTool(EnableCheckFactory checkFactory) {
    	
        this.checkFactory = checkFactory;
        setColor(new Color(194, 179, 205));
        setStroke(new BasicStroke(5));
        allowSnapping();
    }

    public Cursor getCursor() {
        return createCursor(IconLoader.icon("MoveVertexCursor3.gif").getImage());
    }

    public Icon getIcon() {
        return IconLoader.icon("MoveVertex.gif");
    }

    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();

        //#execute(UndoableCommand) will be called. [Jon Aquino]
        moveVertices(getModelSource(), getModelDestination());
    }

    public void mousePressed(final MouseEvent e) {
        try {
            if (!check(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))) {
                return;
            }
            if (!check(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))) {
                return;
            }
            else if (!check(new EnableCheck() {
                public String check(JComponent component) {
                    try {
                        return !nearSelectionHandle(e.getPoint())
                            ? I18N.get("ui.cursortool.MoveVertexTool.no-editable-selection-handles-here")
                            : null;
                            } catch (Exception e) {
                        return e.toString(); }
                }

            })) {
                return;
            }
            else if (!e.isShiftDown()) {
                super.mousePressed(e);
            }
            else ;
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    private boolean nearSelectionHandle(Point2D p) throws NoninvertibleTransformException {
        final Envelope buffer = vertexBuffer(getPanel().getViewport().toModelCoordinate(p));
        final boolean[] result = new boolean[] { false };
        for (Layer layer : getPanel().getSelectionManager().getLayersWithSelectedItems()) {
            if (!layer.isEditable()) {
                continue;
            }
            for (Object object : getPanel().getSelectionManager().getSelectedItems(layer)) {
                Geometry item = (Geometry) object;
                item.apply(new CoordinateFilter() {
                    public void filter(Coordinate coord) {
                        if (buffer.contains(coord)) {
                            result[0] = true;
                        }
                    }
                });
            }
        }
        return result[0];
    }

    private Envelope vertexBuffer(Coordinate c) throws NoninvertibleTransformException {
        double tolerance = TOLERANCE / getPanel().getViewport().getScale();
        return vertexBuffer(c, tolerance);
    }

    public void moveVertices(Coordinate initialLocation, Coordinate finalLocation)
        throws Exception {
        final Envelope oldVertexBuffer = vertexBuffer(initialLocation);
        ArrayList transactions = new ArrayList();
        for (Object object : getPanel().getSelectionManager().getLayersWithSelectedItems()) {
            Layer layerWithSelectedItems = (Layer) object;
            if (!layerWithSelectedItems.isEditable()) {
                continue;
            }
            transactions.add(createTransaction(layerWithSelectedItems, oldVertexBuffer, finalLocation));
        }
        EditTransaction.commit(transactions);
    }

    private EditTransaction createTransaction(
        Layer layer,
        final Envelope oldVertexBuffer,
        final Coordinate newVertex) {
        return EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                for (Object object : selectedItems) {
                    Geometry item = (Geometry) object;
                    edit(item);
                }
                return geometryWithSelectedItems;
            }
            private void edit(Geometry selectedItem) {
                selectedItem.apply(new CoordinateFilter() {
                    public void filter(Coordinate coordinate) {
                        if (oldVertexBuffer.contains(coordinate)) {
                            coordinate.x = newVertex.x;
                            coordinate.y = newVertex.y;
                        }
                    }
                });
            }
        }, getPanel(), getPanel().getContext(), getName(), layer, isRollingBackInvalidEdits(), false);
    }

    protected Shape getShape(Point2D source, Point2D destination) throws Exception {
        double radius = 20;

        return new Ellipse2D.Double(
            destination.getX() - (radius / 2),
            destination.getY() - (radius / 2),
            radius,
            radius);
    }

    private Envelope vertexBuffer(Coordinate vertex, double tolerance) {
        return new Envelope(
            vertex.x - tolerance,
            vertex.x + tolerance,
            vertex.y - tolerance,
            vertex.y + tolerance);
    }
}
