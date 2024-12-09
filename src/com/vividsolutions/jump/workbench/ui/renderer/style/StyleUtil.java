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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Set;
import java.util.TreeSet;

import org.locationtech.jts.geom.*;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.ui.Viewport;

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
        //long t = System.nanoTime();
        final Shape shape = toShape(geometry, viewport);
        //System.out.println("toshape " + (System.nanoTime()-t)/1000000.0 + "ms"); t = System.nanoTime();
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
        //System.out.println("render " + (System.nanoTime()-t)/1000000.0 + "ms"); t = System.nanoTime();
    }

    private static void paintGeometryCollection(GeometryCollection collection,
            Graphics2D g, Viewport viewport, boolean renderingFill,
            Stroke fillStroke, Paint fillPaint, boolean renderingLine,
            Stroke lineStroke, Color lineColor)
            throws NoninvertibleTransformException {
        // For GeometryCollections, render each element separately. Otherwise,
        // for example, if you pass in a GeometryCollection containing a ring
        // and a
        // disk, you cannot render them as such: if you use Graphics.fill,
        // you'll get
        // two disks, and if you use Graphics.draw, you'll get two rings. [Jon
        // Aquino]
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            paint(collection.getGeometryN(i), g, viewport, renderingFill,
                    fillStroke, fillPaint, renderingLine, lineStroke, lineColor);
        }
    }

    private static Shape toShape(Geometry geometry, Viewport viewport)
            throws NoninvertibleTransformException {
        // [Jon Aquino] At high magnifications, Java rendering can be sped up by clipping
        // the Geometry to only that portion visible inside the viewport.
        // [MD] letting Java2D do more clipping actually seems to be slower!
        // So don't use following "optimization"
        // if (isRatioLarge(bufferedEnvelope, geomEnv, 2)) {
        // [mmichaud 2024-11-16] remove clipping : simpler and faster (maybe related
        // to decimation and other optimizations done in Java2DConverter)
        // Envelope viewInModelCoordinates = viewport.getEnvelopeInModelCoordinates();
        // if (geometry.getEnvelopeInternal().intersection(viewInModelCoordinates).getArea()/
        //     geometry.getEnvelopeInternal().getArea() < 0.25) {
        //     Geometry diagonal = geometry.getFactory().createLineString(new Coordinate[]{
        //         new Coordinate(viewInModelCoordinates.getMinX() - viewInModelCoordinates.getWidth() / 50,
        //                viewInModelCoordinates.getMinY() - viewInModelCoordinates.getHeight() / 50),
        //         new Coordinate(viewInModelCoordinates.getMaxX() + viewInModelCoordinates.getWidth() / 50,
        //                viewInModelCoordinates.getMaxY() + viewInModelCoordinates.getHeight() / 50)
        //     });
        //     geometry = RectangleClipPolygon.clip(geometry, diagonal.getEnvelope());
        // }
        return viewport.getJava2DConverter().toShape(geometry);
    }

    /**
     * [Giuseppe Aruta 2018_10-30] Gets available values of styles from feature
     * collection
     * 
     * @param style a ColorTheming style
     * @param fc a FeatureCollection
     * @return a list of style values actually used in the FeatureCollection
     */
    public static Set<String> getAvailableValues(ColorThemingStyle style,
            FeatureCollection fc) {
        final Set<String> set = new TreeSet<>();
        set.add("");
        if (style.isEnabled()) {
            String name = style.getAttributeName();
            for (Feature feature : fc) {
                set.add(feature.getAttribute(name).toString());
            }
        }
        return set;
    }
}
