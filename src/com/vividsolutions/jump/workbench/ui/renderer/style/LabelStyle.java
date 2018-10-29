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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
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
    public static final String FID_COLUMN = "$FID";

    public final static String ABOVE_LINE = "ABOVE_LINE"; // LDB: keep these for
                                                          // Project file
    public final static String ON_LINE = "ON_LINE";
    public final static String BELOW_LINE = "BELOW_LINE";
    public final static String DEFAULT = "DEFAULT";
    public final static String[] verticalAlignmentLookup = { ABOVE_LINE,
            ON_LINE, BELOW_LINE, DEFAULT };

    public final static String LEFT_SIDE = "LEFT_SIDE";
    public final static String CENTER = "CENTER";
    public final static String RIGHT_SIDE = "RIGHT_SIDE";
    public final static String[] horizontalPositionLookup = { LEFT_SIDE,
            CENTER, RIGHT_SIDE };

    // At the moment, internationalization is of no use as the UI display
    // an image in the vertical alignment ComboBox used [mmichaud 2007-06-02]
    // Disabled image in ComboBox and replaced with existing I18N text [LDB
    // 2007-08-27]
    public static String DEFAULT_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.default");
    public static String ABOVE_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.above");
    public static String MIDDLE_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.middle");
    public static String BELOW_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.below");

    public final static String LEFT_SIDE_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.left-side");
    public final static String CENTER_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.center");
    public final static String RIGHT_SIDE_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.right-side");

    public final static String JUSTIFY_CENTER_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.centered");
    public final static String JUSTIFY_LEFT_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.left-alignment");
    public final static String JUSTIFY_RIGHT_TEXT = I18N
            .get("ui.renderer.style.LabelStyle.right-alignment");
    public final static int JUSTIFY_CENTER = 0; // LDB: in retrospect, should
                                                // have used text lookup as
                                                // above
    public final static int JUSTIFY_LEFT = 1; // for readabilty of Project XML
                                              // file
    public final static int JUSTIFY_RIGHT = 2;

    private final GeometryFactory factory = new GeometryFactory();
    private Color originalColor;
    private AffineTransform originalTransform;
    private Layer layer;
    private Geometry viewportRectangle = null;
    private final InteriorPointFinder interiorPointFinder = new InteriorPointFinder();
    private Quadtree labelsDrawn = null;
    private String attribute = LabelStyle.FID_COLUMN;
    private String angleAttribute = ""; // "" means no angle attribute [Jon
                                        // Aquino]
    private String heightAttribute = ""; // "" means no height attribute [Jon
                                         // Aquino]
    private boolean enabled = false;
    private Color color = Color.black;
    private Font font = new Font("Dialog", Font.PLAIN, FONT_BASE_SIZE);
    private boolean scaling = false;
    private double height = 12;
    private boolean hidingOverlappingLabels = true;
    public String verticalAlignment = DEFAULT;
    public String horizontalPosition = CENTER;
    private int horizontalAlignment = JUSTIFY_CENTER;
    private boolean outlineShowing = false;
    private Color outlineColor = new Color(230, 230, 230, 192);
    private double outlineWidth = 4d;
    private Stroke outlineStroke = new BasicStroke(4f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL);
    private boolean hideAtScale = false;
    private double scaleToHideAt = 20000d;

    public LabelStyle() {
    }

    @Override
    public void initialize(Layer layer) {
        labelsDrawn = new Quadtree();
        viewportRectangle = null;
        this.layer = layer;
    }

    @Override
    public void paint(Feature f, Graphics2D g, Viewport viewport)
            throws NoninvertibleTransformException {
        // Test scale first to return faster if realScale > scaleToHideAt
        if (isHidingAtScale()) {
            double scale = height / getFont().getSize2D();
            if (isScaling()) {
                scale *= viewport.getScale();
            }
            final double realScale = ScreenScale
                    .getHorizontalMapScale(viewport);
            if (realScale > scaleToHideAt) {
                return;
            }
        }

        final Object attributeValue = getAttributeValue(f);
        String attributeStringValue;
        if ((attributeValue == null)) {
            return;
        } else if (attributeValue instanceof String) {
            // added .trim() 2007-07-13 [mmichaud]
            attributeStringValue = ((String) attributeValue).trim();
            if (attributeStringValue.length() == 0) {
                return;
            }
        } else if (attributeValue instanceof Date) {
            final DateFormat dateFormat = DateFormat
                    .getDateInstance(DateFormat.DEFAULT);
            attributeStringValue = dateFormat.format((Date) attributeValue);
        } else if (attributeValue instanceof Double) {
            final NumberFormat numberFormat = NumberFormat.getNumberInstance();
            attributeStringValue = numberFormat
                    .format(((Double) attributeValue).doubleValue());
        } else if (attributeValue instanceof Integer) {
            final NumberFormat numberFormat = NumberFormat.getIntegerInstance();
            attributeStringValue = numberFormat
                    .format(((Integer) attributeValue).intValue());
        } else {
            attributeStringValue = attributeValue.toString();
        }

        // [mmichaud 2012-09-22] Simplify the geometry used to draw the label
        // makes sense for very complex polygons
        // ex. finnish lake of 282000 pts takes 0.3 s instead of 8 s
        // TODO : investigate another problem : paint is called 4 times after a
        // pan !
        Geometry geom = f.getGeometry();
        final double pixelSize = viewport.getEnvelopeInModelCoordinates()
                .getWidth() / viewport.getPanel().getSize().getWidth();
        if (geom.getNumPoints() > 64) {
            geom = DouglasPeuckerSimplifier
                    .simplify(f.getGeometry(), pixelSize);
            // revert if geometry is empty or invalid
            if (geom.isEmpty() || !geom.isValid()) {
                geom = f.getGeometry();
            }
        }

        final Geometry viewportIntersection = intersection(geom, viewport);
        if (viewportIntersection == null || viewportIntersection.isEmpty()) {
            return;
        }
        final ModelSpaceLabelSpec spec = modelSpaceLabelSpec(viewportIntersection);
        final Point2D labelCentreInViewSpace = viewport
                .toViewPoint(new Point2D.Double(spec.location.x,
                        spec.location.y));
        paint(g,
                attributeStringValue,
                viewport,// .getScale(),
                labelCentreInViewSpace,
                angle(f, getAngleAttribute(), spec.angle),
                height(f, getHeightAttribute(), getHeight()), spec.dim);
    }

    /**
     * Gets the appropriate attribute value, if one exists. If for some reason
     * the attribute column does not exist, return null
     *
     * @param f
     * @return the value of the attribute
     * @return null if the attribute column does not exist
     */
    private Object getAttributeValue(Feature f) {
        if (getAttribute().equals(LabelStyle.FID_COLUMN)) {
            return f.getID() + "";
        }
        if (!f.getSchema().hasAttribute(getAttribute())) {
            return null;
        }
        return f.getAttribute(getAttribute());
    }

    public static double angle(Feature feature, String angleAttributeName,
            double defaultAngle) {
        if (angleAttributeName.equals("")) {
            return defaultAngle;
        }
        final Object angleAttribute = feature.getAttribute(angleAttributeName);
        if (angleAttribute == null) {
            return defaultAngle;
        }
        try {
            return Angle.toRadians(Double.parseDouble(angleAttribute.toString()
                    .trim()));
        } catch (final NumberFormatException e) {
            return defaultAngle;
        }
    }

    private ModelSpaceLabelSpec modelSpaceLabelSpec(Geometry geometry)
            throws NoninvertibleTransformException {
        if (geometry.getDimension() == 1) {
            return modelSpaceLabelSpec1D(geometry);
        }
        if (geometry.getDimension() == 0) { // LDB: treat points as linear to
                                            // justify them
            // [Giuseppe Aruta 2018-10-29] workaround to keep the label outside
            // the vertex symbol object
            if (layer.getVertexStyle().isEnabled()) {
                final int size = layer.getVertexStyle().size;
                return new ModelSpaceLabelSpec(findPointForVertexSymbology(
                        geometry, size), 0, 0);
            } else {
                return new ModelSpaceLabelSpec(geometry.getCoordinate(), 0d, 0);
            }
        }
        if (verticalAlignment.equals(ON_LINE)
                || verticalAlignment.equals(DEFAULT)) {
            return new ModelSpaceLabelSpec(
                    interiorPointFinder.findPoint(geometry), 0, 2);
        }
        return new ModelSpaceLabelSpec(findPoint(geometry), 0, 2);
    }

    /**
     * Find a point at upper-left, upper-center, upper-right, center-left,
     * center, center-right, lower-left, lower-center or lower-right of the
     * geometry envelope.
     */
    public Coordinate findPoint(Geometry geometry) {
        if (geometry.isEmpty()) {
            return new Coordinate(0, 0);
        }
        final Envelope envelope = geometry.getEnvelopeInternal();
        double x = (envelope.getMinX() + envelope.getMaxX()) / 2d;
        double y = (envelope.getMinY() + envelope.getMaxY()) / 2d;
        if (verticalAlignment.equals(DEFAULT) && geometry.getDimension() != 2) {
            y = envelope.getMaxY();
        } else if (verticalAlignment.equals(ABOVE_LINE)) {
            y = envelope.getMaxY();
        } else if (verticalAlignment.equals(BELOW_LINE)) {
            y = envelope.getMinY();
        }
        if (horizontalPosition.equals(LEFT_SIDE)) {
            x = envelope.getMinX();
        } else if (horizontalPosition.equals(RIGHT_SIDE)) {
            x = envelope.getMaxX();
        }
        return new Coordinate(x, y);
    }

    /**
     * [Giuseppe Aruta 2018-10-29] Find a point at upper-left, upper-center,
     * upper-right, center-left, center, center-right, lower-left, lower-center
     * or lower-right of the geometry envelope. Find right distance to symbol
     * size in order to write the label outside the symbol
     * 
     * @param geometry
     * @param int value
     * @return
     */
    public Coordinate findPointForVertexSymbology(Geometry geometry, int value) {
        if (geometry.isEmpty()) {
            return new Coordinate(0, 0);
        }
        final Envelope envelope = new Envelope(geometry.getCoordinate().x
                - value / 2, geometry.getCoordinate().x + value / 2,
                geometry.getCoordinate().y - value / 2,
                geometry.getCoordinate().y + value / 2);
        double x = (envelope.getMinX() + envelope.getMaxX()) / 2d;
        double y = (envelope.getMinY() + envelope.getMaxY()) / 2d;
        if (verticalAlignment.equals(DEFAULT) && geometry.getDimension() != 2) {
            y = envelope.getMaxY();
        } else if (verticalAlignment.equals(ABOVE_LINE)) {
            y = envelope.getMaxY();
        } else if (verticalAlignment.equals(BELOW_LINE)) {
            y = envelope.getMinY();
        }
        if (horizontalPosition.equals(LEFT_SIDE)) {
            x = envelope.getMinX();
        } else if (horizontalPosition.equals(RIGHT_SIDE)) {
            x = envelope.getMaxX();
        }
        return new Coordinate(x, y);
    }

    private ModelSpaceLabelSpec modelSpaceLabelSpec1D(Geometry geometry) {
        LineSegment segment;
        if (horizontalPosition.equals(LEFT_SIDE)) {
            segment = endSegment(geometry);
            return new ModelSpaceLabelSpec(factory.createPoint(segment.p0)
                    .getCoordinate(), angle(segment), 1);
        } else if (horizontalPosition.equals(RIGHT_SIDE)) {
            segment = endSegment(geometry);
            return new ModelSpaceLabelSpec(factory.createPoint(segment.p1)
                    .getCoordinate(), angle(segment), 1);
        } else {
            segment = longestSegment(geometry);
        }
        // LineSegment longestSegment = longestSegment(geometry);
        return new ModelSpaceLabelSpec(
                (horizontalAlignment == JUSTIFY_CENTER) ? CoordUtil.average(
                        segment.p0, segment.p1)
                        : (horizontalAlignment == JUSTIFY_LEFT) ? segment.p0
                                : segment.p1, angle(segment), 1);
    }

    private double angle(LineSegment segment) {
        double angle = Angle.angle(segment.p0, segment.p1);
        // Don't want upside-down labels! [Jon Aquino]
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
        final List arrays = CoordinateArrays
                .toCoordinateArrays(geometry, false);
        for (final Iterator i = arrays.iterator(); i.hasNext();) {
            final Coordinate[] coordinates = (Coordinate[]) i.next();
            for (int j = 1; j < coordinates.length; j++) { // start 1
                final double length = coordinates[j - 1]
                        .distance(coordinates[j]);
                if (length > maxSegmentLength) {
                    maxSegmentLength = length;
                    c0 = coordinates[j - 1];
                    c1 = coordinates[j];
                }
            }
        }
        return new LineSegment(c0, c1);
    }

    private LineSegment endSegment(Geometry geometry) {
        final Coordinate c0 = geometry.getCoordinates()[0];
        final Coordinate c1 = geometry.getCoordinates()[geometry.getNumPoints() - 1];
        // if (geometry.getNumPoints() < 3) return new LineSegment(c0, c1);
        if (c0.x <= c1.x && horizontalPosition.equals(LEFT_SIDE)) {
            if (horizontalAlignment == JUSTIFY_RIGHT) {
                return new LineSegment(c0, new Coordinate(c1.x, c0.y));
            } else {
                return new LineSegment(c0, geometry.getCoordinates()[1]);
            }
        } else if (c0.x <= c1.x && horizontalPosition.equals(RIGHT_SIDE)) {
            if (horizontalAlignment == JUSTIFY_LEFT) {
                return new LineSegment(c1, new Coordinate(c1.x + 1.0, c1.y));
            } else {
                return new LineSegment(
                        geometry.getCoordinates()[geometry.getNumPoints() - 2],
                        c1);
            }
        } else if (c0.x > c1.x && horizontalPosition.equals(LEFT_SIDE)) {
            if (horizontalAlignment == JUSTIFY_RIGHT) {
                return new LineSegment(new Coordinate(c1.x - 1.0, c1.y), c1);
            } else {
                return new LineSegment(c1,
                        geometry.getCoordinates()[geometry.getNumPoints() - 2]);
            }
        } else if (c0.x > c1.x && horizontalPosition.equals(RIGHT_SIDE)) {
            if (horizontalAlignment == JUSTIFY_LEFT) {
                return new LineSegment(new Coordinate(c0.x - 1, c0.y), c0);
            } else {
                return new LineSegment(geometry.getCoordinates()[1], c0);
            }
        } else {
            return longestSegment(geometry);
        }
    }

    public static double height(Feature feature, String heightAttributeName,
            double defaultHeight) {
        if (heightAttributeName.equals("")) {
            return defaultHeight;
        }
        final Object heightAttribute = feature
                .getAttribute(heightAttributeName);
        if (heightAttribute == null) {
            return defaultHeight;
        }
        try {
            return Double.parseDouble(heightAttribute.toString().trim());
        } catch (final NumberFormatException e) {
            return defaultHeight;
        }
    }

    public void paint(Graphics2D g, String text, Viewport viewport,
            Point2D viewCentre, double angle, double height, int dim) {
        setup(g);
        try {
            final double viewportScale = viewport.getScale();
            double scale = height / getFont().getSize2D();
            if (isScaling()) {
                scale *= viewportScale;
            }
            final TextLayout layout = new TextLayout(text, getFont(),
                    g.getFontRenderContext());
            final AffineTransform transform = g.getTransform();
            configureTransform(transform, viewCentre, scale, layout, angle, dim);
            g.setTransform(transform);
            if (isHidingOverlappingLabels()) {
                final Area transformedLabelBounds = new Area(layout.getBounds())
                        .createTransformedArea(transform);
                final Envelope transformedLabelBoundsEnvelope = envelope(transformedLabelBounds);
                if (collidesWithExistingLabel(transformedLabelBounds,
                        transformedLabelBoundsEnvelope)) {
                    return;
                }
                labelsDrawn.insert(transformedLabelBoundsEnvelope,
                        transformedLabelBounds);
            }
            if (outlineShowing) {
                g.setColor(outlineColor);
                g.setStroke(outlineStroke);
                g.draw(layout.getOutline(null));
            }
            g.setColor(getColor());
            layout.draw(g, 0, 0);
        } finally {
            cleanup(g);
        }
    }

    private Envelope envelope(Shape shape) {
        final Rectangle2D bounds = shape.getBounds2D();
        return new Envelope(bounds.getMinX(), bounds.getMaxX(),
                bounds.getMinY(), bounds.getMaxY());
    }

    private boolean collidesWithExistingLabel(Area transformedLabelBounds,
            Envelope transformedLabelBoundsEnvelope) {
        final List potentialCollisions = labelsDrawn
                .query(transformedLabelBoundsEnvelope);
        for (final Iterator i = potentialCollisions.iterator(); i.hasNext();) {
            final Area potentialCollision = (Area) i.next();
            final Area intersection = new Area(potentialCollision);
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
     * itself may not intersect the viewport. In this case, this method returns
     * null.
     */
    private Geometry intersection(Geometry geometry, Viewport viewport) {
        Geometry geo;
        try {
            // LDB: need to catch the NoninvertibleTransformException instead of
            // just throwing it
            geo = geometry.intersection(viewportRectangle(viewport));
        } catch (final NoninvertibleTransformException e) {
            return null;
        }
        if (geo.getNumGeometries() == 0) {
            return null;
        }
        return geo;
    }

    private Geometry viewportRectangle(Viewport viewport)
            throws NoninvertibleTransformException {
        if (viewportRectangle == null) {
            final Envelope e = viewport.toModelEnvelope(0, viewport.getPanel()
                    .getWidth(), 0, viewport.getPanel().getHeight());
            viewportRectangle = factory.createPolygon(
                    factory.createLinearRing(new Coordinate[] {
                            new Coordinate(e.getMinX(), e.getMinY()),
                            new Coordinate(e.getMinX(), e.getMaxY()),
                            new Coordinate(e.getMaxX(), e.getMaxY()),
                            new Coordinate(e.getMaxX(), e.getMinY()),
                            new Coordinate(e.getMinX(), e.getMinY()) }), null);
        }
        return viewportRectangle;
    }

    private void configureTransform(AffineTransform transform,
            Point2D viewCentre, double scale, TextLayout layout, double angle,
            int dim) {

        double xTranslation = viewCentre.getX();
        double yTranslation = viewCentre.getY()
                + ((scale * GUIUtil.trueAscent(layout)) / 2d);
        if (dim == 1) {
            xTranslation -= horizontalAlignmentOffset(scale
                    * layout.getBounds().getWidth());
            yTranslation -= verticalAlignmentOffset(scale
                    * layout.getBounds().getHeight(), dim);
        } else if (dim == 0) {
            xTranslation -= horizontalPositionOffset(scale
                    * layout.getBounds().getWidth());
            yTranslation -= verticalAlignmentOffset(scale
                    * layout.getBounds().getHeight(), dim);
        } else {
            xTranslation -= horizontalAlignmentOffset(scale
                    * layout.getBounds().getWidth());
            yTranslation -= verticalAlignmentOffset(scale
                    * layout.getBounds().getHeight(), dim);
        }
        // Negate the angle because the positive y-axis points downwards.
        // See the #rotate JavaDoc. [Jon Aquino]
        transform.rotate(-angle, viewCentre.getX(), viewCentre.getY());
        transform.translate(xTranslation, yTranslation);
        transform.scale(scale, scale);
    }

    private double verticalAlignmentOffset(double scaledLabelHeight, int dim) {
        if (getVerticalAlignment().equals(ON_LINE)
                || (getVerticalAlignment().equals(DEFAULT) && dim == 2)) {
            return 0;
        }
        final double buffer = 3;
        final double offset = buffer
                + (layer.getBasicStyle().getLineWidth() / 2d)
                + (scaledLabelHeight / 2d);
        if (getVerticalAlignment().equals(ABOVE_LINE)
                || (getVerticalAlignment().equals(DEFAULT) && dim != 2)) {
            return offset;
        }
        if (getVerticalAlignment().equals(BELOW_LINE)) {
            return -offset;
        }
        Assert.shouldNeverReachHere();
        return 0;
    }

    private double horizontalPositionOffset(double width) {
        if (getHorizontalPosition().equals(LEFT_SIDE)) {
            return width;
        }
        if (getHorizontalPosition().equals(CENTER)) {
            return width / 2d;
        }
        if (getHorizontalPosition().equals(RIGHT_SIDE)) {
            return 0;
        }
        Assert.shouldNeverReachHere();
        return 0;
    }

    private double horizontalAlignmentOffset(double width) {
        if (getHorizontalAlignment() == JUSTIFY_CENTER) {
            return width / 2d;
        }
        if (getHorizontalAlignment() == JUSTIFY_LEFT) {
            return 0;
        }
        if (getHorizontalAlignment() == JUSTIFY_RIGHT) {
            return width; // LDB: see hack in modelSpaceLabelSpec1D
        }
        Assert.shouldNeverReachHere();
        return 0;
    }

    /**
     * @return approximate alignment offset for estimation
     */
    public double getVerticalAlignmentOffset(int dim) {
        return verticalAlignmentOffset(getHeight(), dim) - getHeight() / 2;
    }

    /**
     * @return approximate alignment offset for estimation
     */
    public double getHorizontalAlignmentOffset(String text) {
        return horizontalAlignmentOffset(text.length() * getHeight() * 0.6);
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

    @Override
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

    public boolean isHidingAtScale() {
        return hideAtScale;
    }

    public boolean getHideAtScale() {
        return hideAtScale;
    }

    public String getVerticalAlignment() {
        return verticalAlignment;
    }

    public String getHorizontalPosition() {
        return horizontalPosition;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public boolean getHidingOverlappingLabels() {
        return hidingOverlappingLabels;
    }

    public boolean getOutlineShowing() {
        return outlineShowing;
    }

    public double getOutlineWidth() {
        return outlineWidth;
    }

    public double getScaleToHideAt() {
        return scaleToHideAt;
    }

    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setVerticalAlignment(String verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public void setHorizontalPosition(String horizontalPosition) {
        this.horizontalPosition = horizontalPosition;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
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

    @Override
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

    public void setOutlineShowing(boolean outlineShowing) {
        this.outlineShowing = outlineShowing;
    }

    public void setOutlineWidth(double outlineWidth) {
        this.outlineWidth = outlineWidth;
        outlineStroke = new BasicStroke((float) outlineWidth,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    }

    public void setScaleToHideAt(double scaleToHideAt) {
        this.scaleToHideAt = scaleToHideAt;
    }

    public void setOutlineColor(Color outlineColor, int alpha) {
        this.outlineColor = new Color(outlineColor.getRed(),
                outlineColor.getGreen(), outlineColor.getBlue(), alpha);
    }

    public void setOutlineColor(Color outlineColor) {
        if (outlineColor != null) {
            final int alpha = this.outlineColor.getAlpha();
            setOutlineColor(outlineColor, alpha);
        }
    }

    public void setHideAtScale(boolean hideAtScale) {
        this.hideAtScale = hideAtScale;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();
            return null;
        }
    }

    private class ModelSpaceLabelSpec {
        public double angle;
        public Coordinate location;
        public int dim;

        public ModelSpaceLabelSpec(Coordinate location, double angle, int dim) {
            this.location = location;
            this.angle = angle;
            this.dim = dim;
        }
    }
}
