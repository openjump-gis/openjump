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

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * Inputs and outputs are in model space, not view space.
 */
public class SnapManager {
    private static final String TOLERANCE_IN_PIXELS_KEY = SnapManager.class.getName() +
        " - TOLERANCE IN PIXELS";
    private ArrayList policies = new ArrayList();
    private boolean snapCoordinateFound;

    public SnapManager() {
    }

    public Coordinate snap(LayerViewPanel panel, Coordinate originalCoordinate) {
        for (Iterator i = policies.iterator(); i.hasNext();) {
            SnapPolicy policy = (SnapPolicy) i.next();
            Coordinate snapCoordinate = policy.snap(panel, originalCoordinate);

            if (snapCoordinate != null) {
                snapCoordinateFound = true;

                return snapCoordinate;
            }
        }

        snapCoordinateFound = false;

        return originalCoordinate;
    }

    public void addPolicies(Collection policies) {
        this.policies.addAll(policies);
    }

    public boolean wasSnapCoordinateFound() {
        return snapCoordinateFound;
    }

    public static int getToleranceInPixels(Blackboard blackboard) {
        return blackboard.get(TOLERANCE_IN_PIXELS_KEY, 10);
    }

    public static void setToleranceInPixels(int toleranceInPixels, Blackboard blackboard) {
        blackboard.put(TOLERANCE_IN_PIXELS_KEY, toleranceInPixels);
    }
}
