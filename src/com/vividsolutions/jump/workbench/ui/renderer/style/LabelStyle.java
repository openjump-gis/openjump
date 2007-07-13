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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.Angle;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.geom.InteriorPointFinder;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
public class LabelStyle implements Style {
    public final static int FONT_BASE_SIZE = 12;
    public final static String ABOVE_LINE = "ABOVE_LINE";
    public final static String ON_LINE = "ON_LINE";
    public final static String BELOW_LINE = "BELOW_LINE";
    // At the moment, niternationalization is of no use as the UI display
    // an image in the vertical alignment ComboBox used [mmichaud 2007-06-02]
    //public static String ABOVE_LINE = I18N.get("ui.renderer.style.LabelStyle.ABOVE_LINE");
    //public static String ON_LINE = I18N.get("ui.renderer.style.LabelStyle.ON_LINE");
    //public static String BELOW_LINE =I18N.get("ui.renderer.style.LabelStyle.BELOW_LINE");
    public static final String FID_COLUMN = "$FID";
    private GeometryFactory factory = new GeometryFactory();
    private Color originalColor;
    private AffineTransform originalTransform;
    private Layer layer;
    private Geometry viewportRectangle = null;
    private InteriorPointFinder interiorPointFinder = new InteriorPointFinder();
    private Quadtree labelsDrawn = null;
    private String attribute = LabelStyle.FID_COLUMN;
    private String angleAttribute = ""; //"" means no angle attribute [Jon Aquino]
    private String heightAttribute = ""; //"" means no height attribute [Jon Aquino]
    private boolean enabled = false;
    private Color color = Color.black;
    private Font font = new Font("Dialog", Font.PLAIN, FONT_BASE_SIZE);
    private boolean scaling = false;
    private double height = 12;
    private boolean hidingOverlappingLabels = true;
    public String verticalAlignment = ABOVE_LINE;
    public LabelStyle() {}
    public void initialize(Layer layer) {
        labelsDrawn = new Quadtree();
        viewportRectangle = null;
        //Set the vertices' fill colour to the layer's line colour
        this.layer = layer;
        //-- [sstein] added again to initialize correct language
        //-- [mmichaud] internationalization unused at the moment
        //ABOVE_LINE = I18N.get("ui.renderer.style.LabelStyle.ABOVE_LINE");
        //ON_LINE = I18N.get("ui.renderer.style.LabelStyle.ON_LINE");
        //BELOW_LINE =I18N.get("ui.renderer.style.LabelStyle.BELOW_LINE");
        //--
    }
    public void paint(Feature f, Graphics2D g, Viewport viewport)
        throws NoninvertibleTransformException {
        Object attribute = getAttributeValue(f);
        // added .trim() 2007-07-13 [mmichaud]
        if ((attribute == null) || (attribute.toString().trim().length() == 0)) {
            return;
        }
        Geometry viewportIntersection = intersection(f.getGeometry(), viewport);
        if (viewportIntersection == null) {
            return;
        }
        ModelSpaceLabelSpec spec = modelSpaceLabelSpec(viewportIntersection);
        Point2D labelCentreInViewSpace =
            viewport.toViewPoint(new Point2D.Double(spec.location.x, spec.location.y));
        paint(
            g,
            attribute.toString().trim(),    // added .trim() 2007-07-13 [mmichaud]
            viewport.getScale(),
            labelCentreInViewSpace,
            angle(f, getAngleAttribute(), spec.angle),
            height(f, getHeightAttribute(), getHeight()),
            spec.linear);
    }

    /**
     * Gets the appropriate attribute value, if one exists.
     * If for some reason the attribute column does not exist, return null
     *
     * @param f
     * @return the value of the attribute
     * @return null if the attribute column does not exist
     */
    private Object getAttributeValue(Feature f)
    {
      if (getAttribute().equals(LabelStyle.FID_COLUMN))
        return f.getID() + "";
      if (! f.getSchema().hasAttribute(getAttribute()))
          return null;
      return f.getAttribute(getAttribute());
    }

    public static double angle(
        Feature feature,
        String angleAttributeName,
        double defaultAngle) {
        if (angleAttributeName.equals("")) {
            return defaultAngle;
        }
        Object angleAttribute = feature.getAttribute(angleAttributeName);
        if (angleAttribute == null) {
            return defaultAngle;
        }
        try {
            return Angle.toRadians(Double.parseDouble(angleAttribute.toString().trim()));
        } catch (NumberFormatException e) {
            return defaultAngle;
        }
    }
    private ModelSpaceLabelSpec modelSpaceLabelSpec(Geometry geometry)
        throws NoninvertibleTransformException {
        if (geometry.getDimension() == 1) {
            return modelSpaceLabelSpec1D(geometry);
        }
        return new ModelSpaceLabelSpec(interiorPointFinder.findPoint(geometry), 0, false);
    }
    private ModelSpaceLabelSpec modelSpaceLabelSpec1D(Geometry geometry) {
        LineSegment longestSegment = longestSegment(geometry);
        return new ModelSpaceLabelSpec(
            CoordUtil.average(longestSegment.p0, longestSegment.p1),
            angle(longestSegment),
            true);
    }
    private double angle(LineSegment segment) {
        double angle = Angle.angle(segment.p0, segment.p1);
        //Don't want upside-down labels! [Jon Aquino]
        if (angle < (-Math.PI / 2d)) {
            angle += Math.PI;
        }
        if (angle > (Math.PI / 2d)) {
            angle -= Math.PI;
        }
        return angle;
    }
    private LineSegment longestSegment(Geometry geometry) {
        double maxSegmentLength = -1;
        Coordinate c0 = null;
        Coordinate c1 = null;
        List arrays = CoordinateArrays.toCoordinateArrays(geometry, false);
        for (Iterator i = arrays.iterator(); i.hasNext();) {
            Coordinate[] coordinates = (Coordinate[]) i.next();
            for (int j = 1; j < coordinates.length; j++) { //start 1
                if (coordinates[j - 1].distance(coordinates[j]) > maxSegmentLength) {
                    maxSegmentLength = coordinates[j - 1].distance(coordinates[j]);
                    c0 = coordinates[j - 1];
                    c1 = coordinates[j];
                }
            }
        }
        return new LineSegment(c0, c1);
    }
    public static double height(
        Feature feature,
        String heightAttributeName,
        double defaultHeight) {
        if (heightAttributeName.equals("")) {
            return defaultHeight;
        }
        Object heightAttribute = feature.getAttribute(heightAttributeName);
        if (heightAttribute == null) {
            return defaultHeight;
        }
        try {
            return Double.parseDouble(heightAttribute.toString().trim());
        } catch (NumberFormatException e) {
            return defaultHeight;
        }
    }
    public void paint(
        Graphics2D g,
        String text,
        double viewportScale,
        Point2D viewCentre,
        double angle,
        double height,
        boolean linear) {
        setup(g);
        try {
            double scale = height / getFont().getSize2D();
            if (isScaling()) {
                scale *= viewportScale;
            }
            g.setColor(getColor());
            TextLayout layout = new TextLayout(text, getFont(), g.getFontRenderContext());
            AffineTransform transform = g.getTransform();
            configureTransform(transform, viewCentre, scale, layout, angle, linear);
            g.setTransform(transform);
            if (isHidingOverlappingLabels()) {
                Area transformedLabelBounds =
                    new Area(layout.getBounds()).createTransformedArea(transform);
                Envelope transformedLabelBoundsEnvelope =
                    envelope(transformedLabelBounds);
                if (collidesWithExistingLabel(transformedLabelBounds,
                    transformedLabelBoundsEnvelope)) {
                    return;
                }
                labelsDrawn.insert(
                    transformedLabelBoundsEnvelope,
                    transformedLabelBounds);
            }
            layout.draw(g, 0, 0);
        } finally {
            cleanup(g);
        }
    }
    private Envelope envelope(Shape shape) {
        Rectangle2D bounds = shape.getBounds2D();
        return new Envelope(
            bounds.getMinX(),
            bounds.getMaxX(),
            bounds.getMinY(),
            bounds.getMaxY());
    }
    private boolean collidesWithExistingLabel(
        Area transformedLabelBounds,
        Envelope transformedLabelBoundsEnvelope) {
        List potentialCollisions = labelsDrawn.query(transformedLabelBoundsEnvelope);
        for (Iterator i = potentialCollisions.iterator(); i.hasNext();) {
            Area potentialCollision = (Area) i.next();
            Area intersection = new Area(potentialCollision);
            intersection.intersect(transformedLabelBounds);
            if (!intersection.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    private void setup(Graphics2D g) {
        originalTransform = g.getTransform();
        originalColor = g.getColor();
    }
    private void cleanup(Graphics2D g) {
        g.setTransform(originalTransform);
        g.setColor(originalColor);
    }
    /**
     * Even though a feature's envelope intersects the viewport, the feature
     * itself may not intersect the viewport. In this case, this method
     * returns null.
     */
    private Geometry intersection(Geometry geometry, Viewport viewport)
        throws NoninvertibleTransformException {
        return geometry.intersection(viewportRectangle(viewport));
    }
    private Geometry viewportRectangle(Viewport viewport)
        throws NoninvertibleTransformException {
        if (viewportRectangle == null) {
            Envelope e =
                viewport.toModelEnvelope(
                    0,
                    viewport.getPanel().getWidth(),
                    0,
                    viewport.getPanel().getHeight());
            viewportRectangle =
                factory.createPolygon(
                    factory.createLinearRing(
                        new Coordinate[] {
                            new Coordinate(e.getMinX(), e.getMinY()),
                            new Coordinate(e.getMinX(), e.getMaxY()),
                            new Coordinate(e.getMaxX(), e.getMaxY()),
                            new Coordinate(e.getMaxX(), e.getMinY()),
                            new Coordinate(e.getMinX(), e.getMinY())}),
                    null);
        }
        return viewportRectangle;
    }
    private void configureTransform(
        AffineTransform transform,
        Point2D viewCentre,
        double scale,
        TextLayout layout,
        double angle,
        boolean linear) {
        double xTranslation =
            viewCentre.getX() - ((scale * layout.getBounds().getWidth()) / 2d);
        double yTranslation =
            viewCentre.getY() + ((scale * GUIUtil.trueAscent(layout)) / 2d);
        if (linear) {
            yTranslation
                -= verticalAlignmentOffset(scale * layout.getBounds().getHeight());
        }
        //Negate the angle because the positive y-axis points downwards.
        //See the #rotate JavaDoc. [Jon Aquino]
        transform.rotate(-angle, viewCentre.getX(), viewCentre.getY());
        transform.translate(xTranslation, yTranslation);
        transform.scale(scale, scale);
    }
    private double verticalAlignmentOffset(double scaledLabelHeight) {
        if (getVerticalAlignment().equals(ON_LINE)) {
            return 0;
        }
        double buffer = 3;
        double offset =
            buffer
                + (layer.getBasicStyle().getLineWidth() / 2d)
                + (scaledLabelHeight / 2d);
        if (getVerticalAlignment().equals(ABOVE_LINE)) {
            return offset;
        }
        if (getVerticalAlignment().equals(BELOW_LINE)) {
            return -offset;
        }
        Assert.shouldNeverReachHere();
        return 0;
    }
    public String getAttribute() {
        return attribute;
    }
    public String getAngleAttribute() {
        return angleAttribute;
    }
    public String getHeightAttribute() {
        return heightAttribute;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public Color getColor() {
        return color;
    }
    public Font getFont() {
        return font;
    }
    public boolean isScaling() {
        return scaling;
    }
    public double getHeight() {
        return height;
    }
    public boolean isHidingOverlappingLabels() {
        return hidingOverlappingLabels;
    }
    public String getVerticalAlignment() {
        return verticalAlignment;
    }
    public void setVerticalAlignment(String verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
    public void setAngleAttribute(String angleAttribute) {
        this.angleAttribute = angleAttribute;
    }
    public void setHeightAttribute(String heightAttribute) {
        this.heightAttribute = heightAttribute;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public void setColor(Color color) {
        this.color = color;
    }
    public void setFont(Font font) {
        this.font = font;
    }
    public void setScaling(boolean scaling) {
        this.scaling = scaling;
    }
    public void setHeight(double height) {
        this.height = height;
    }
    public void setHidingOverlappingLabels(boolean hidingOverlappingLabels) {
        this.hidingOverlappingLabels = hidingOverlappingLabels;
    }
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();
            return null;
        }
    }
    private class ModelSpaceLabelSpec {
        public double angle;
        public Coordinate location;
        public boolean linear;
        public ModelSpaceLabelSpec(Coordinate location, double angle, boolean linear) {
            this.location = location;
            this.angle = angle;
            this.linear = linear;
        }
    }
}
