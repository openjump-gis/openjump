package com.vividsolutions.jump.workbench.ui.style;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.openjump.core.ui.util.LayerableUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LineStringStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

public class LegendPanel extends JPanel {

    /**
     * Panel that uses LayerView.class capability to show applied style
     * according to feature type. See also
     * com.vividsolutions.jump.workbench.ui.style.LegendPlugIn class
     * 
     * @author Giuseppe Aruta
     */
    private final Layer layer;
    private final BasicStyle style;
    private final FeatureCollection featureCollection;

    public LegendPanel(Layer layer, BasicStyle style,
            FeatureCollection featureCollection) {

        this.layer = layer;
        this.style = style;
        this.featureCollection = featureCollection;
    }

    private static final long serialVersionUID = 1L;

    {
        setBackground(new Color(0, 0, 0, 0));
        setBorder(BorderFactory.createEmptyBorder());
        setMaximumSize(new Dimension(120, 40));
        setMinimumSize(new Dimension(120, 40));
        setPreferredSize(new Dimension(120, 40));

    }

    private final LayerViewPanel dummyLayerViewPanel = new LayerViewPanel(
            new LayerManager(), new LayerViewPanelContext() {
                @Override
                public void setStatusMessage(String message) {
                }

                @Override
                public void warnUser(String warning) {
                }

                @Override
                public void handleThrowable(Throwable t) {
                }

            });

    private final Viewport viewport = new Viewport(dummyLayerViewPanel) {

        private final AffineTransform transform = new AffineTransform();

        @Override
        public Envelope getEnvelopeInModelCoordinates() {
            return new Envelope(0, 120, 0, 40);
        }

        @Override
        public AffineTransform getModelToViewTransform() {
            return transform;
        }

        @Override
        public Point2D toViewPoint(Coordinate modelCoordinate) {
            return new Point2D.Double(modelCoordinate.x, modelCoordinate.y);
        }

    };

    private void paint(Style style, Graphics2D g) {
        final Stroke originalStroke = g.getStroke();

        if (layer.getVertexStyle().getSize() > 40) {
            setPreferredSize(new Dimension(120, layer.getVertexStyle()
                    .getSize()));
        }

        try {

            if (LayerableUtil.isLinealLayer(featureCollection)) {
                style.paint(lineFeature(), g, viewport);
            } else if (LayerableUtil.isPointLayer(featureCollection)) {
                style.paint(pointFeature(), g, viewport);
            } else if (LayerableUtil.isPolygonalLayer(featureCollection)) {
                style.paint(polygonFeature(), g, viewport);
            } else {
                style.paint(multiGeometriesFeature(), g, viewport);
            }

        } catch (final Exception e) {
            try {
                style.paint(multiGeometriesFeature(), g, viewport);
            } catch (final Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } finally {
            g.setStroke(originalStroke);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        paint(style, (Graphics2D) g);

        for (final Object element : layer.getStyles()) {
            final Style item = (Style) element;
            if (item instanceof LineStringStyle) {
                final LineStringStyle lineStyle = (LineStringStyle) item
                        .clone();
                paint(lineStyle, (Graphics2D) g);
            } else if (item instanceof VertexStyle
                    & layer.getVertexStyle().isEnabled()) {

                final VertexStyle vertexStyle = (VertexStyle) item;
                // [Giuseppe Aruta 2018_11_9] if the object is
                // ExternalSymbolsType.class CadPlan
                // otype, symbol is correctly displayed but is
                // changed/modified/resized even on LegendPanel
                // whenever the user changes it. Using VertexStyle.clone()
                // method
                // ExternalSymbolsType object is removed
                // TODO find a valid clone() method
                paint(vertexStyle, (Graphics2D) g);
            }
        }

    }

    private Feature polygonFeature() {
        try {
            Feature feat;

            feat = FeatureUtil.toFeature(new WKTReader()
                    .read("POLYGON ((10 10, 110 10, 110 30, 10 30, 10 10))"),
                    new FeatureSchema() {
                        private static final long serialVersionUID = -8627306219650589202L;
                        {
                            addAttribute("GEOMETRY", AttributeType.GEOMETRY);
                        }
                    });

            return feat;
        } catch (final ParseException e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }

    private Feature lineFeature() {
        try {
            Feature feat;

            feat = FeatureUtil.toFeature(
                    new WKTReader().read("LINESTRING (10 20.05, 110 19.95)"),
                    new FeatureSchema() {
                        private static final long serialVersionUID = -8627306219650589202L;
                        {
                            addAttribute("GEOMETRY", AttributeType.GEOMETRY);
                        }
                    });

            return feat;
        } catch (final ParseException e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }

    private Feature pointFeature() {
        try {
            Feature feat;

            feat = FeatureUtil.toFeature(new WKTReader().read("POINT (60 20)"),
                    new FeatureSchema() {
                        private static final long serialVersionUID = -8627306219650589202L;
                        {
                            addAttribute("GEOMETRY", AttributeType.GEOMETRY);
                        }
                    });

            return feat;
        } catch (final ParseException e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }

    private Feature multiGeometriesFeature() {
        try {
            Feature feat;

            feat = FeatureUtil
                    .toFeature(
                            new WKTReader()
                                    .read("GEOMETRYCOLLECTION (POLYGON ((10 10, 55 10, 55 30, 10 30, 10 10)), POINT (85 15), LINESTRING (65 25.05, 110 24.95))"),
                            new FeatureSchema() {
                                private static final long serialVersionUID = -8627306219650589202L;
                                {
                                    addAttribute("GEOMETRY",
                                            AttributeType.GEOMETRY);
                                }
                            });

            return feat;
        } catch (final ParseException e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }

}
