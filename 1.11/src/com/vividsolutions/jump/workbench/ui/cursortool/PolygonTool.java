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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public abstract class PolygonTool extends MultiClickTool {
    public PolygonTool()
    {
      setCloseRing(true);
      setMetricsDisplay(new CoordinateListMetrics());
    }

    /**
     * Callers should check whether the polygon returned is valid.
     */
    protected Polygon getPolygon() throws NoninvertibleTransformException {
        ArrayList closedPoints = new ArrayList(getCoordinates());

        if (!closedPoints.get(0).equals(closedPoints.get(closedPoints.size() - 1))) {
            closedPoints.add(new Coordinate((Coordinate) closedPoints.get(0)));
        }

        return new GeometryFactory().createPolygon(
            new GeometryFactory().createLinearRing(toArray(closedPoints)),
            null);
    }

    protected boolean checkPolygon() throws NoninvertibleTransformException {
        if (getCoordinates().size() < 3) {
            getPanel().getContext().warnUser(I18N.get("ui.cursortool.PolygonTool.the-polygon-must-have-at-least-3-points"));

            return false;
        }

        IsValidOp isValidOp = new IsValidOp(getPolygon());

        if (!isValidOp.isValid()) {
            getPanel().getContext().warnUser(isValidOp.getValidationError().getMessage());

            if (PersistentBlackboardPlugIn.get(getWorkbench().getContext())
                .get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
                return false;
            }
        }

        return true;
    }
}
