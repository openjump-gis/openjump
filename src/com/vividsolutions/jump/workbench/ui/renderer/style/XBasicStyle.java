package com.vividsolutions.jump.workbench.ui.renderer.style;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;

import java.awt.*;

/**
 * Convenience class extending BasicStyle to include a VertexStyle
 * and have both in one Style.
 */
public class XBasicStyle extends BasicStyle {

    VertexStyle vertexStyle;

    public XBasicStyle() {} // for java2xml

    //public XBasicStyle(BasicStyle fromBasicStyle) {
    //    this(fromBasicStyle, null);
    //}

    public XBasicStyle(BasicStyle fromBasicStyle, VertexStyle fromVertexStyle) {
        super();
        assert fromVertexStyle != null : "XBasicStyle must have a non null VertexStyle";

        setEnabled(fromBasicStyle.isEnabled());

        setRenderingFill(fromBasicStyle.isRenderingFill());
        setFillColor(fromBasicStyle.getFillColor());
        setAlpha(fromBasicStyle.getAlpha());
        setFillPattern(fromBasicStyle.getFillPattern());
        setRenderingFillPattern(fromBasicStyle.isRenderingFillPattern());

        setRenderingLine(fromBasicStyle.isRenderingLine());
        setLineColor(fromBasicStyle.getLineColor());
        setLineWidth(fromBasicStyle.getLineWidth());
        setLinePattern(fromBasicStyle.getLinePattern());
        setRenderingLinePattern(fromBasicStyle.isRenderingLinePattern());

        setRenderingVertices(fromBasicStyle.getRenderingVertices());

        if (fromVertexStyle == null) {
            vertexStyle = new SquareVertexStyle();
            vertexStyle.setFillColor(fromBasicStyle.getFillColor());
            vertexStyle.setLineColor(fromBasicStyle.getLineColor());
            vertexStyle.setAlpha(fromBasicStyle.getAlpha());
            vertexStyle.setEnabled(false);
        } else {
            vertexStyle = (VertexStyle)fromVertexStyle.clone();
        }
    }

    // fillColor is the color without alpha
    // here we want to return the color with alpha to paint it
    public Color getFillColor() {
        return GUIUtil.alphaColor(super.getFillColor(), getAlpha());
    }

    public VertexStyle getVertexStyle() {
        return vertexStyle;
    }

    public void setVertexStyle(VertexStyle vertexStyle) {
        this.vertexStyle = vertexStyle;
    }

    @Override public void paint(Feature f, Graphics2D g, Viewport viewport)
        throws Exception {

        if (!getRenderingVertices() && f.getGeometry() instanceof com.vividsolutions.jts.geom.Point) {
            return;
        }
        // render basic style
        StyleUtil.paint(f.getGeometry(), g, viewport, isRenderingFill(),
                getLineStroke(),
                (isRenderingFillPattern() && (getFillPattern() != null)) ? getFillPattern()
                        : getFillColor(),
                isRenderingLine(), getLineStroke(), getLineColor());
        // render vertex style
        if (vertexStyle.isEnabled()) {
            //@TODO don't know why some vertexStyles have a 0 alpha
            vertexStyle.setAlpha(getAlpha());
            vertexStyle.paint(f, g, viewport);
        }
    }

    @Override public XBasicStyle clone() {
        VertexStyle vs = (VertexStyle)vertexStyle.clone();
        return new XBasicStyle(this, vs);
    }

    @Override public Color getFeatureColor(Feature feature) {
        return null;
    }

    @Override public void initialize(Layer layer) {
        throw new UnsupportedOperationException("initialize(Layer) is nor implemented for XBasicStyle");
    }

}
