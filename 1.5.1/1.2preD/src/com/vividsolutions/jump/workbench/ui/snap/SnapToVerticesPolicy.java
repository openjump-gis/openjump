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

package com.vividsolutions.jump.workbench.ui.snap;

import java.util.ArrayList;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.VerticesInFencePlugIn;

public class SnapToVerticesPolicy implements SnapPolicy {
    private GeometryFactory factory = new GeometryFactory();
    //On-screen features are cached. The cache is built lazily. [Jon Aquino]

    private Blackboard blackboard;
    public SnapToVerticesPolicy(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    public static final String ENABLED_KEY = SnapToVerticesPolicy.class.getName() + " - ENABLED";

    public Coordinate snap(LayerViewPanel panel, Coordinate originalPoint) {
        if (!blackboard.get(ENABLED_KEY, false)) {
            return null;
        }
        Geometry bufferedTransformedCursorLocation;
        bufferedTransformedCursorLocation =
                factory.createPoint(originalPoint).buffer(SnapManager.getToleranceInPixels(blackboard) / panel.getViewport().getScale());
        ArrayList vertices = new ArrayList();
        for (Iterator i =
            VisiblePointsAndLinesCache
                .instance(panel)
                .getTree()
                .query(bufferedTransformedCursorLocation.getEnvelopeInternal())
                .iterator();
            i.hasNext();
            ) {
            Geometry pointsAndLines = (Geometry) i.next();
            vertices.addAll(
                VerticesInFencePlugIn
                    .verticesInFence(pointsAndLines, bufferedTransformedCursorLocation, true)
                    .getCoordinates());
        }
        if (vertices.isEmpty()) {
            return null;
        }
        return CoordUtil.closest(vertices, originalPoint);
    }
}
