
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.font.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jts.geom.*;

/**
 * Displays line segment length and absolute angle.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class MetricsLineStringSegmentStyle extends LineStringSegmentStyle
{
  public final static int FONT_BASE_SIZE = 10;
  private Font font = new Font("Dialog", Font.PLAIN, FONT_BASE_SIZE);

  /**
   * @param finAngle degrees
   * @param finLength pixels
   */
  public MetricsLineStringSegmentStyle(String name, String iconFile) {
    super(name, IconLoader.icon(iconFile));
  }

  protected void paint(Coordinate p0, Coordinate p1, Viewport viewport,
                       Graphics2D graphics) throws Exception
  {
    String lenStr = Double.toString(p0.distance(p1));
    double ang = Math.toDegrees(
        Math.atan2(p1.y - p0.y,
        p1.x - p0.x));
    String angStr = Double.toString(ang);
    String text = lenStr + " / " + angStr;

    paint(text,
          viewport.toViewPoint(new Point2D.Double(p0.x, p0.y)),
          viewport.toViewPoint(new Point2D.Double(p1.x, p1.y)), viewport,
          graphics);
  }

  private void paint(String text, Point2D p0, Point2D p1, Viewport viewport,
                       Graphics2D g) throws NoninvertibleTransformException {
    if (p0.equals(p1)) {
      return;
    }

    Point2D mid = new Point2D.Float( (float) ((p0.getX() + p1.getX()) / 2),
                                     (float) ((p0.getY() + p1.getY()) / 2) );

    g.setColor(Color.BLACK);
    g.setStroke(stroke);

    TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
    layout.draw(g, (float) mid.getX(), (float) mid.getY());
  }

  protected void paint(Point2D p0, Point2D p1, Viewport viewport,
                       Graphics2D g) throws NoninvertibleTransformException {
    throw new UnsupportedOperationException("This method should never be called");
  }

  public static class LengthAngle extends MetricsLineStringSegmentStyle {
    public LengthAngle() {
      super(I18N.get("ui.renderer.style.MetricsLineStringSegmentStyle.Segment-Metrics"), "LengthAngleDecorator.gif");
    }
  }


}