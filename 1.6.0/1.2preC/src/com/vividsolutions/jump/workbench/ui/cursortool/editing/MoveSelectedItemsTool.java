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

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class MoveSelectedItemsTool extends DragTool {
    private EnableCheckFactory checkFactory;
    private Shape selectedFeaturesShape;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private List verticesToSnap = null;

    public MoveSelectedItemsTool(EnableCheckFactory checkFactory) {
        this.checkFactory = checkFactory;
        setStroke(
            new BasicStroke(
                1,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[] { 3, 3 },
                0));
        allowSnapping();
    }

    protected void gestureFinished() throws java.lang.Exception {
        reportNothingToUndoYet();
        final Coordinate displacement = CoordUtil.subtract(getModelDestination(), getModelSource());
        ArrayList transactions = new ArrayList();
        for (Iterator i = getPanel().getSelectionManager().getLayersWithSelectedItems().iterator();
            i.hasNext();
            ) {
            Layer layerWithSelectedItems = (Layer) i.next();
            transactions.add(createTransaction(layerWithSelectedItems, displacement));
        }
        EditTransaction.commit(transactions);
    }

    private EditTransaction createTransaction(Layer layer, final Coordinate displacement) {
        EditTransaction transaction =
            EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                for (Iterator j = selectedItems.iterator(); j.hasNext();) {
                    Geometry item = (Geometry) j.next();
                    move(item, displacement);
                }

                return geometryWithSelectedItems;
            }
        }, getPanel(), getPanel().getContext(), getName(), layer, isRollingBackInvalidEdits(), false);
        return transaction;
    }

    private void move(Geometry geometry, final Coordinate displacement) {
        geometry.apply(new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                coordinate.setCoordinate(CoordUtil.add(coordinate, displacement));
            }
        });
    }

    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }

    public Icon getIcon() {
        return IconLoader.icon("Move.gif");
    }

    public void mousePressed(MouseEvent e) {
        try {
            if (!check(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))) {
                return;
            }

            if (!check(checkFactory.createSelectedItemsLayersMustBeEditableCheck())) {
                return;
            }

            verticesToSnap = null;
            selectedFeaturesShape = createSelectedItemsShape();
            super.mousePressed(e);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    private Collection verticesToSnap() {
        //Lazily initialized because not used if there are no snapping policies. [Jon Aquino]
        Envelope viewportEnvelope = getPanel().getViewport().getEnvelopeInModelCoordinates();

        if (verticesToSnap == null) {
            verticesToSnap = new ArrayList();
            for (Iterator i = getPanel().getSelectionManager().getSelectedItems().iterator();
                i.hasNext();
                ) {
                Geometry selectedItem = (Geometry) i.next();
                Coordinate[] coordinates = selectedItem.getCoordinates();

                for (int j = 0; j < coordinates.length; j++) {
                    if (viewportEnvelope.contains(coordinates[j])) {
                        verticesToSnap.add(coordinates[j]);
                    }
                }
            }

            if (verticesToSnap.size() > 100) {
                Collections.shuffle(verticesToSnap);
                verticesToSnap = verticesToSnap.subList(0, 99);
            }
        }

        return verticesToSnap;
    }

    private Shape createSelectedItemsShape() throws NoninvertibleTransformException {
        List itemsToRender = new ArrayList(getPanel().getSelectionManager().getSelectedItems());
        if (itemsToRender.size() > 100) {
            Collections.shuffle(itemsToRender);
            itemsToRender = itemsToRender.subList(0, 99);
        }

        GeometryCollection gc =
            geometryFactory.createGeometryCollection(
                (Geometry[]) itemsToRender.toArray(new Geometry[] {}));

        return getPanel().getJava2DConverter().toShape(gc);
    }

    protected Shape getShape() throws Exception {
        AffineTransform transform = new AffineTransform();
        transform.translate(
            getViewDestination().getX() - getViewSource().getX(),
            getViewDestination().getY() - getViewSource().getY());

        return transform.createTransformedShape(selectedFeaturesShape);
    }

    protected void setModelDestination(Coordinate modelDestination) {
        for (Iterator i = verticesToSnap().iterator(); i.hasNext();) {
            Coordinate vertex = (Coordinate) i.next();
            Coordinate displacement = CoordUtil.subtract(vertex, getModelSource());
            Coordinate snapPoint = snap(CoordUtil.add(modelDestination, displacement));

            if (getSnapManager().wasSnapCoordinateFound()) {
                this.modelDestination = CoordUtil.subtract(snapPoint, displacement);
                return;
            }
        }
        this.modelDestination = modelDestination;
    }

}
