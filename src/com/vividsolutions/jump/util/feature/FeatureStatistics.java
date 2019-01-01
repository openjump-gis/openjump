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

package com.vividsolutions.jump.util.feature;

import java.util.List;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;

/** 
 * Computes Feature Statistics for a {@link FeatureCollection}.
 * See also {@link org.openjump.core.ui.plugin.tools.aggregate} package
 */
public class FeatureStatistics {
    public static double[] minMaxValue(FeatureCollection fc, String col) {
        double[] minMax = new double[] { 0.0, 0.0 };
        int adjDistanceIndex = -1;

        List features = fc.getFeatures();

        for (int i = 0; i < features.size(); i++) {
            Feature f = (Feature) features.get(i);

            if (adjDistanceIndex == -1) {
                adjDistanceIndex = f.getSchema().getAttributeIndex(col);
            }

            double adjDistance = f.getDouble(adjDistanceIndex);

            if ((i == 0) || (adjDistance < minMax[0])) {
                minMax[0] = adjDistance;
            }

            if ((i == 0) || (adjDistance > minMax[1])) {
                minMax[1] = adjDistance;
            }
        }

        return minMax;
    }
}
