package com.vividsolutions.jump.workbench.ui.renderer.style;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.geom.Angle;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.warp.AffineTransform;
import com.vividsolutions.jump.workbench.ui.renderer.java2D.Java2DConverter;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


public class WKTFillPattern extends BasicFillPattern {
    private static final String LINE_WIDTH_KEY = "LINE WIDTH";
    private static final String EXTENT_KEY = "EXTENT";
    private static final String PATTERN_WKT_KEY = "PATTERN WKT";

    /**
     * Parameterless constructor for Java2XML
     */
    public WKTFillPattern() {
    }

    public WKTFillPattern(int lineWidth, int extent, String patternWKT) {
        super(new Blackboard().putAll(CollectionUtil.createMap(
                    new Object[] {
                        BasicFillPattern.COLOR_KEY, Color.black, LINE_WIDTH_KEY,
                        new Integer(lineWidth), EXTENT_KEY, new Integer(extent),
                        PATTERN_WKT_KEY, patternWKT
                    })));
    }

    public BufferedImage createImage(Blackboard properties) {
        BufferedImage image = new BufferedImage(properties.getInt(EXTENT_KEY),
                properties.getInt(EXTENT_KEY), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                ((Color) getProperties().get(COLOR_KEY)).getAlpha() / 255f));

        //Vertical flip so the y-axis points upwards [Jon Aquino]
        g.scale(1, -1);

        //Center [Jon Aquino]
        g.translate(properties.getInt(EXTENT_KEY) / 2d,
            -properties.getInt(EXTENT_KEY) / 2d);
        g.setColor((Color) properties.get(BasicFillPattern.COLOR_KEY));
        g.setStroke(new BasicStroke(properties.getInt(LINE_WIDTH_KEY)));

        try {
            g.draw(new Java2DConverter(new Java2DConverter.PointConverter() {
                    public Point2D toViewPoint(Coordinate modelCoordinate) {
                        return new Point2D.Double(modelCoordinate.x,
                                modelCoordinate.y);
                    }
                    public double getScale() { return 1;}
                }).toShape(new WKTReader().read(
                        (String) properties.get(PATTERN_WKT_KEY))));
        } catch (NoninvertibleTransformException e) {
            //Eat it [Jon Aquino]
        } catch (ParseException e) {
            Assert.shouldNeverReachHere((String) properties.get(PATTERN_WKT_KEY));
        }

        return image;
    }

    public static WKTFillPattern createDiagonalStripePattern(int lineWidth,
        double centerlineSeparationInLineWidths, boolean forward, boolean back) {
        double centerlineSeparation = centerlineSeparationInLineWidths * lineWidth;

        return new WKTFillPattern(lineWidth, (int) Math.rint(Math.sqrt(2) * centerlineSeparation),
            "GEOMETRYCOLLECTION(" +
            wktForThreeLines(centerlineSeparation, 45, forward) + ", " +
            wktForThreeLines(centerlineSeparation, -45, back) + ")");
    }

    public static WKTFillPattern createVerticalHorizontalStripePattern(
        int lineWidth, double centerlineSeparationInLineWidths,
        boolean vertical, boolean horizontal) {
        double centerlineSeparation = centerlineSeparationInLineWidths * lineWidth;

        return new WKTFillPattern(lineWidth, (int) Math.rint(2 * centerlineSeparation),
            "GEOMETRYCOLLECTION(" +
            wktForThreeLines(centerlineSeparation, 90, vertical) + ", " +
            wktForThreeLines(centerlineSeparation, 0, horizontal) + ")");
    }

    private static String wktForThreeLines(double centerlineSeparation,
        double angleInDegrees, boolean enabled) {
        return enabled
        ? wktForThreeLines(4 * centerlineSeparation, centerlineSeparation,
            angleInDegrees) : "POINT EMPTY";
    }

    private static String wktForThreeLines(double length,
        double centerlineSeparation, double angleInDegrees) {
        AffineTransform transform = new AffineTransform(new Coordinate(),
                new Coordinate(), new Coordinate(1, 0),
                new Coordinate(Math.cos(Angle.toRadians(angleInDegrees)),
                    Math.sin(Angle.toRadians(angleInDegrees))));

        try {
            return transform.transform(new WKTReader().read("MULTILINESTRING(" +
                    ("(" + (-length / 2) + " " + -centerlineSeparation + ", " +
                    (length / 2) + " " + -centerlineSeparation + "), ") +
                    ("(" + (-length / 2) + " " + 0 + ", " + (length / 2) + " " +
                    0 + "), ") +
                    ("(" + (-length / 2) + " " + centerlineSeparation + ", " +
                    (length / 2) + " " + centerlineSeparation + ") ") + ")"))
                            .toText();
        } catch (Exception e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }
}
