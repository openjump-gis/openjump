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

package com.vividsolutions.jump.workbench.ui.renderer.style;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


public abstract class CircleLineStringEndpointStyle extends LineStringEndpointStyle {
    private final static int DIAMETER = 8;
    private Stroke circleStroke = new BasicStroke(2);

    private CircleLineStringEndpointStyle(String name, boolean start, String iconFile) {
        super(name, IconLoader.icon(iconFile), start);
    }

    protected void paint(Point2D terminal, Point2D next, Viewport viewport,
        Graphics2D graphics) throws NoninvertibleTransformException {
        graphics.setColor(lineColorWithAlpha);
        graphics.setStroke(circleStroke);
        graphics.draw(toShape(terminal));
    }

    private Shape toShape(Point2D viewPoint) {
        return new Ellipse2D.Double(viewPoint.getX() - (DIAMETER / 2d),
            viewPoint.getY() - (DIAMETER / 2d), DIAMETER, DIAMETER);
    }

    public static class Start extends CircleLineStringEndpointStyle {
        public Start() {
            super(I18N.get("ui.renderer.style.CircleLineStringEndpointStyle.Start-Circle"), true, "CircleStart.gif");
        }
    }

    public static class End extends CircleLineStringEndpointStyle {
        public End() {
            super(I18N.get("ui.renderer.style.CircleLineStringEndpointStyle.End-Circle"), false, "CircleEnd.gif");
        }
    }
}
