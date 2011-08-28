
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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.font.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.LineStringVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexIndexLineSegmentStyle;
import com.vividsolutions.jts.geom.*;

import org.openjump.core.ui.images.IconLoader;

/**
 * Displays the vertex z value for each vertex of a line string.
 *
 * @author Michael Michaud
 * @version 1.0
 */
//public class VertexZValueStyle extends LineStringVertexStyle {
public class VertexZValueStyle extends TextBasedVertexStyle {
    
    public final static int FONT_BASE_SIZE = 10;
    private Font font = new Font("Dialog", Font.PLAIN, FONT_BASE_SIZE);
    private Font font2 = new Font("Dialog", Font.BOLD, FONT_BASE_SIZE+2);
    protected Stroke stroke;
    protected Color lineColorWithAlpha;
    protected Color fillColorWithAlpha;

    public VertexZValueStyle(String name, String iconFile) {
        super(name, IconLoader.icon(iconFile));
    }

    protected void paint(Point2D p, LineString line, int index,
                             Viewport viewport, Graphics2D g) throws Exception {
        String text = Double.toString(line.getCoordinates()[index].z);
        g.setColor(Color.BLACK);
        g.setStroke(stroke);
        
        // draw a small dot on each vertex
        g.drawLine((int)p.getX(), (int)p.getY(), 1+(int)p.getX(), 1+(int)p.getY());
        // draw a small circle around the dot on first and last point
        if (index == 0 || index == line.getNumPoints()-1) {
            g.drawOval((int)p.getX()-2, (int)p.getY()-2, 5, 5);
        }
        
        // For first and last point, write the label along the first (last) segment
        // so that the altitude is more readable
        // First and last altitude are also written in a bold font of size 12
        if (index == 0) {
            TextLayout layout = new TextLayout(text, font2, g.getFontRenderContext());
            Coordinate c0 = line.getCoordinates()[0];
            Coordinate c1 = line.getCoordinates()[1];
            float vx = (float)(c1.x-c0.x);
            float vy = (float)(c1.y-c0.y);
            float d = (float)Math.sqrt(vx*vx+vy*vy);
            float dx = vx >= 0 ? 10f*vx/d : 52f*vx/d;
            float dy = vy >= 0 ? -10f*vy/d : -20f*vy/d;
            layout.draw(g, dx + (float)p.getX(), dy + (float)p.getY());
        }
        else if (index == line.getNumPoints()-1) {
            TextLayout layout = new TextLayout(text, font2, g.getFontRenderContext());
            Coordinate c0 = line.getCoordinates()[line.getNumPoints()-1];
            Coordinate c1 = line.getCoordinates()[line.getNumPoints()-2];
            float vx = (float)(c1.x-c0.x);
            float vy = (float)(c1.y-c0.y);
            float d = (float)Math.sqrt(vx*vx+vy*vy);
            float dx = vx >= 0 ? 10f*vx/d : 52f*vx/d;
            float dy = vy >= 0 ? -10f*vy/d : -20f*vy/d;
            layout.draw(g, dx + (float)p.getX(), dy + (float)p.getY());
        }
        else {
            TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
            layout.draw(g, 1f+(float)p.getX(), -1f+(float)p.getY());
        }
    }
  
    protected void paintPoint(Point point,
                             Viewport viewport, Graphics2D g) throws Exception {
          Coordinate c = point.getCoordinate();
          String text = Double.toString(c.z);
          g.setColor(Color.BLACK);
          g.setStroke(stroke);
          TextLayout layout = new TextLayout(text, font2, g.getFontRenderContext());
          Point2D p = viewport.toViewPoint(new Point2D.Double(c.x, c.y));
          layout.draw(g, 1f + (float)p.getX(), -1f + (float)p.getY());
    }
  
    public void initialize(Layer layer) {
        stroke = new BasicStroke(layer.getBasicStyle().getLineWidth(),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        lineColorWithAlpha = GUIUtil.alphaColor(layer.getBasicStyle()
                                                     .getLineColor(),
                layer.getBasicStyle().getAlpha());
        fillColorWithAlpha = GUIUtil.alphaColor(layer.getBasicStyle()
                                                     .getFillColor(),
                layer.getBasicStyle().getAlpha());
    }

    public static class VertexZValue extends VertexZValueStyle {
        public VertexZValue() {
            super(I18N.get("ui.renderer.style.VertexZValueStyle.Vertex-Z"), "ZValueDecorator.gif");
        }
    }

}