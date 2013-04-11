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

package com.vividsolutions.jump.workbench.model;

import java.awt.Color;


/**
 * Convenience functions for setting and working with Layer Styles.
 * The convention for methods which change a layer style is that
 * the layerChanged event is <i>not</i> fired.  This allows further
 * modification of the style by the caller.
 */
public class LayerStyleUtil {
    public LayerStyleUtil() {
    }

    /**
     * Sets the style for a linear layer.
     * @param vertexSize 0 if vertices are not to be shown
     */
    public static void setLinearStyle(Layer lyr, Color lineColor,
        int lineWidth, int vertexSize) {
        lyr.getBasicStyle().setLineColor(lineColor);
        lyr.getBasicStyle().setRenderingFill(false);
        lyr.getBasicStyle().setAlpha(255);
        lyr.getBasicStyle().setLineWidth(lineWidth);
        lyr.setSynchronizingLineColor(false);
        lyr.getVertexStyle().setSize(vertexSize);
        lyr.getVertexStyle().setEnabled(vertexSize > 0);
    }
}
