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

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.workbench.ui.Viewport;


public abstract class LineStringEndpointStyle extends LineStringStyle implements ChoosableStyle {
    private boolean start;

    protected String name;

    protected Icon icon;

    public LineStringEndpointStyle(String name, Icon icon, boolean start) {
        super(name, icon);
        this.name = name;
        this.icon = icon;                
        this.start = start;
    }    

    protected void paintLineString(LineString lineString, Viewport viewport,
        Graphics2D graphics) throws Exception {
        if (lineString.isEmpty()) {
            return;
        }

        paint(start ? lineString.getCoordinateN(0)
                    : lineString.getCoordinateN(lineString.getNumPoints() - 1),
            start ? lineString.getCoordinateN(1)
                  : lineString.getCoordinateN(lineString.getNumPoints() - 2),
            viewport, graphics);
    }

    private void paint(Coordinate terminal, Coordinate next, Viewport viewport,
        Graphics2D graphics) throws Exception {
        paint(viewport.toViewPoint(new Point2D.Double(terminal.x, terminal.y)),
            viewport.toViewPoint(new Point2D.Double(next.x, next.y)), viewport,
            graphics);
    }

    protected abstract void paint(Point2D terminal, Point2D next,
        Viewport viewport, Graphics2D graphics) throws Exception;

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }
}
