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

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.geom.CoordUtil;

public abstract class RectangleTool extends DragTool {
    public RectangleTool() {
    }

    protected Polygon getRectangle() throws NoninvertibleTransformException {
        Envelope e = new Envelope(
                getModelSource().x,
                getModelDestination().x,
                getModelSource().y,
                getModelDestination().y);

        return new GeometryFactory().createPolygon(
            new GeometryFactory().createLinearRing(
                new Coordinate[] {
                    new Coordinate(e.getMinX(), e.getMinY()),
                    new Coordinate(e.getMinX(), e.getMaxY()),
                    new Coordinate(e.getMaxX(), e.getMaxY()),
                    new Coordinate(e.getMaxX(), e.getMinY()),
                    new Coordinate(e.getMinX(), e.getMinY())}),
            null);
    }

    private Collection verticesToSnap(Coordinate source, Coordinate destination) {
        ArrayList verticesToSnap = new ArrayList();
        verticesToSnap.add(destination);
        verticesToSnap.add(new Coordinate(source.x, destination.y));
        verticesToSnap.add(new Coordinate(destination.x, source.y));

        return verticesToSnap;
    }

    protected void setModelDestination(Coordinate modelDestination) {
        for (Iterator i = verticesToSnap(getModelSource(), modelDestination).iterator(); i.hasNext();) {
            Coordinate vertex = (Coordinate) i.next();
            Coordinate snappedVertex = snap(vertex);

            if (getSnapManager().wasSnapCoordinateFound()) {
                this.modelDestination = CoordUtil.add(modelDestination, CoordUtil.subtract(snappedVertex, vertex));
                return;
            }

        }
        this.modelDestination = modelDestination;
        return;
    }
    protected void setModelSource(Coordinate modelSource) {
        this.modelSource = snap(modelSource);
    }

}
