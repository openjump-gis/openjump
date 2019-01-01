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

package org.openjump.core.ui.style.decoration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.ChoosableStyle;


public abstract class TextBasedVertexStyle implements ChoosableStyle {
    
    protected boolean enabled = true;
    
    protected String name;

    protected Icon icon;

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();
            return null;
        }
    }

    //protected Color lineColorWithAlpha;

    //protected Color fillColorWithAlpha;

    public TextBasedVertexStyle() {
    }
    
    public TextBasedVertexStyle(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

    protected void paintGeometry(Geometry geometry, Graphics2D graphics,
        Viewport viewport) throws Exception {
    
        //if (geometry instanceof MultiPoint) {
        //    return;
        //}

        if (geometry instanceof GeometryCollection) {
            paintGeometryCollection((GeometryCollection) geometry, graphics,
                viewport);
            return;
        }
        if (geometry instanceof Polygon) {
            paintPolygon((Polygon)geometry, graphics, viewport);
            return;
        }
        if (geometry instanceof Point) {
            paintPoint((Point)geometry, viewport, graphics);
            return;
        }
        if (!(geometry instanceof LineString)) {
            return;
        }
        LineString lineString = (LineString) geometry;
        if (lineString.getNumPoints() < 2) {
            return;
        }
        paintLineString(lineString, viewport, graphics);
    }

    /**
     * @param lineString has 2 or more points
     */
    protected void paintLineString(LineString lineString,
                      Viewport viewport, Graphics2D graphics) throws Exception {
        int numPtsToRender = lineString.getNumPoints();
        // don't paint end vertex twice
        if (lineString.isClosed()) numPtsToRender--;
        for (int i = 0; i < numPtsToRender; i++) {
            Coordinate p = lineString.getCoordinateN(i);
            paint(viewport.toViewPoint(new Point2D.Double(p.x, p.y)),
                  lineString, i, viewport, graphics);
        }
    }
    
    protected abstract void paint(Point2D p0, LineString line, int index,
        Viewport viewport, Graphics2D graphics) throws Exception;
    
    protected abstract void paintPoint(Point point,
        Viewport viewport, Graphics2D graphics) throws Exception;

    private void paintGeometryCollection(GeometryCollection gc,
        Graphics2D graphics, Viewport viewport) throws Exception {
        for (int i = 0; i < gc.getNumGeometries(); i++) {
            paintGeometry(gc.getGeometryN(i), graphics, viewport);
        }
    }

    private void paintPolygon(Polygon polygon, Graphics2D graphics,
        Viewport viewport) throws Exception {
        paintGeometry(polygon.getExteriorRing(), graphics, viewport);

        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            paintGeometry(polygon.getInteriorRingN(i), graphics, viewport);
        }
    }

    public void initialize(Layer layer) {
        //stroke = new BasicStroke(layer.getBasicStyle().getLineWidth(),
        //        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        //lineColorWithAlpha = GUIUtil.alphaColor(layer.getBasicStyle()
        //                                             .getLineColor(),
        //        layer.getBasicStyle().getAlpha());
        //fillColorWithAlpha = GUIUtil.alphaColor(layer.getBasicStyle()
        //                                             .getFillColor(),
        //        layer.getBasicStyle().getAlpha());
    }

    public void paint(Feature f, Graphics2D g, Viewport viewport) throws Exception {
        paintGeometry(f.getGeometry(), g, viewport);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }
    
}
