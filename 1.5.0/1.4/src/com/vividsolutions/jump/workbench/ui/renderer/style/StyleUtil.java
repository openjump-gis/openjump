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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.util.AssertionFailedException;

import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;


public class StyleUtil {
    /**
     * Smart enough to not fill LineStrings.
     */
    public static void paint(Geometry geometry, Graphics2D g,
        Viewport viewport, boolean renderingFill, Stroke fillStroke,
        Paint fillPaint, boolean renderingLine, Stroke lineStroke,
        Color lineColor) throws NoninvertibleTransformException {
        if (geometry instanceof GeometryCollection) {
            paintGeometryCollection((GeometryCollection) geometry, g, viewport,
                renderingFill, fillStroke, fillPaint, renderingLine,
                lineStroke, lineColor);

            return;
        }

        Shape shape = toShape(geometry, viewport);
        if (!(shape instanceof GeneralPath) && renderingFill) {
            g.setStroke(fillStroke);
            g.setPaint(fillPaint);
            g.fill(shape);
        }
        if (renderingLine) {
            g.setStroke(lineStroke);
            g.setColor(lineColor);
            g.draw(shape);
        }
    }

    private static void paintGeometryCollection(GeometryCollection collection,
        Graphics2D g, Viewport viewport, boolean renderingFill,
        Stroke fillStroke, Paint fillPaint, boolean renderingLine,
        Stroke lineStroke, Color lineColor)
        throws NoninvertibleTransformException {
        //For GeometryCollections, render each element separately. Otherwise,
        //for example, if you pass in a GeometryCollection containing a ring and a
        // disk, you cannot render them as such: if you use Graphics.fill, you'll get
        //two disks, and if you use Graphics.draw, you'll get two rings. [Jon Aquino]
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            paint(collection.getGeometryN(i), g, viewport, renderingFill,
                fillStroke, fillPaint, renderingLine, lineStroke, lineColor);
        }
    }

    private static Shape toShape(Geometry geometry, Viewport viewport)
        throws NoninvertibleTransformException {
        //At high magnifications, Java rendering can be sped up by clipping
        //the Geometry to only that portion visible inside the viewport.
        //Hence the code below. [Jon Aquino]
        Envelope bufferedEnvelope = EnvelopeUtil.bufferByFraction(viewport.getEnvelopeInModelCoordinates(),
                0.05);
        Geometry actualGeometry = geometry;
        Envelope geomEnv = actualGeometry.getEnvelopeInternal();
        if (! bufferedEnvelope.contains(geomEnv)) {
          /**
           * MD - letting Java2D do more clipping actually seems to be slower!
           * So don't use following "optimization"
           */
          //if (isRatioLarge(bufferedEnvelope, geomEnv, 2)) {
        	if (!((geometry instanceof LineString) || (geometry instanceof MultiLineString)))
        		actualGeometry = clipGeometry(geometry, bufferedEnvelope);
            //System.out.println("cl");
          //}
        }
        return viewport.getJava2DConverter().toShape(actualGeometry);
    }

    /**
     * Clipping a geometry using JTS produces higher quality results than letting Java2D do it.
     * It may also be faster!
     *
     * @param geom
     * @param env
     * @return
     */
    private static Geometry clipGeometry(Geometry geom, Envelope env)
    {
      try {
          Geometry clipGeom = EnvelopeUtil.toGeometry(env)
                                       .intersection(geom);
          return clipGeom;
      } catch (Exception e) {
          //Can get a TopologyException if the Geometry is invalid. Eat it. [Jon Aquino]
          //Can get an AssertionFailedException (unable to assign hole to a shell)
          //at high magnifications. Eat it. [Jon Aquino]

          //Alvaro Zabala reports that we can get here with an
          //IllegalArgumentException (points must form a closed linestring)
          //for bad geometries. Eat it. [Jon Aquino]
      }
      return geom;
    }

    private static boolean isRatioLarge(Envelope viewEnv,
        Envelope geomEnv,
        double factor)
    {
      if (isRatioLarge(viewEnv.getHeight(), geomEnv.getHeight(), factor)) return true;
      if (isRatioLarge(viewEnv.getWidth(), geomEnv.getWidth(), factor)) return true;
      return false;
    }

    private static boolean isRatioLarge(double winDim, double geomDim, double factor)
    {
      return (geomDim / winDim) < factor;
    }
}
