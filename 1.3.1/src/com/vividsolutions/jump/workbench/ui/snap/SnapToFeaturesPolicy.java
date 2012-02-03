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

import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.geom.InteriorPointFinder;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class SnapToFeaturesPolicy implements SnapPolicy {
    private Blackboard blackboard;
    public SnapToFeaturesPolicy(Blackboard blackboard) {
        this.blackboard = blackboard;
    }
    public SnapToFeaturesPolicy() {
        this(new Blackboard());
        blackboard.put(ENABLED_KEY, true);
    }
    public static final String ENABLED_KEY = SnapToFeaturesPolicy.class.getName() + " - ENABLED";
    private InteriorPointFinder interiorPointFinder = new InteriorPointFinder();
    //On-screen features are cached. The cache is built lazily. [Jon Aquino]
    private GeometryFactory factory = new GeometryFactory();
    public Coordinate snap(LayerViewPanel panel, Coordinate originalCoordinate) {
        if (!blackboard.get(ENABLED_KEY, false)) {
            return null;
        }
        Geometry bufferedTransformedCursorLocation;
        bufferedTransformedCursorLocation =
            factory.createPoint(originalCoordinate).buffer(SnapManager.getToleranceInPixels(blackboard) / panel.getViewport().getScale());
        for (Iterator i =
            VisiblePointsAndLinesCache
                .instance(panel)
                .getTree()
                .query(bufferedTransformedCursorLocation.getEnvelopeInternal())
                .iterator();
            i.hasNext();
            ) {
            Geometry candidate = (Geometry) i.next();
            if (!(candidate instanceof GeometryCollection)) {
                Geometry intersection = candidate.intersection(bufferedTransformedCursorLocation);
                if (intersection.isEmpty()) {
                    continue;
                }
                return interiorPointFinder.findPoint(intersection);
            }

            GeometryCollection col = (GeometryCollection) candidate;
            for (int k = 0; k < col.getNumGeometries(); ++k) {
                Geometry intersection = col.getGeometryN(k).intersection(bufferedTransformedCursorLocation);
                if (!intersection.isEmpty()) {
                    return interiorPointFinder.findPoint(intersection);
                }
            }
        }
        return null;
    }
}
