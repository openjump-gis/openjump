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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;


public class Animations {
    private static void drawRings(Collection centers, int radius, int delay,
        Color color, LayerViewPanel panel, float[] dash) {
        GeneralPath path = new GeneralPath();                        
        for (Iterator i = centers.iterator(); i.hasNext(); ) {
            Point2D center = (Point2D) i.next();
            path.append(new Ellipse2D.Double(center.getX() - radius,
                center.getY() - radius, radius * 2, radius * 2), false);
        }
        panel.flash(path, color,
            new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10, dash, 0), delay);
    }

    /**
     * @param expanding true to expand; false to contract
     * @param dash null for no dashes
     */
    public static void drawExpandingRing(Point2D center, boolean expanding,
        Color color, LayerViewPanel panel, float[] dash) {
        drawExpandingRings(Arrays.asList(new Point2D[]{center}), expanding, color, panel, dash);
    }
    
    public static void drawExpandingRings(Collection centers, boolean expanding,
        Color color, LayerViewPanel panel, float[] dash) {
        int start = expanding ? 0 : 5;
        int end = 5 - start;
        int increment = expanding ? 1 : (-1);

        for (int i = start; i != end; i += increment) {
            drawRings(centers, i * 10, 30, color, panel, dash);
        }
    }    
}
