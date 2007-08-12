
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

package com.vividsolutions.jump.workbench.ui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.ui.renderer.java2D.Java2DConverter;

/**
 * Controls the area on the model being viewed by a LayerViewPanel.
 */

//<<TODO:NAMING>> Rename to Viewport [Jon Aquino]
public class Viewport implements Java2DConverter.PointConverter {
    static private final int INITIAL_VIEW_ORIGIN_X = 0;
    static private final int INITIAL_VIEW_ORIGIN_Y = 0;
    private ArrayList listeners = new ArrayList();
    private Java2DConverter java2DConverter;
    private LayerViewPanel panel;

    /**
     * Origin of view as perceived by model, that is, in model space
     */
    private Point2D viewOriginAsPerceivedByModel =
        new Point2D.Double(INITIAL_VIEW_ORIGIN_X, INITIAL_VIEW_ORIGIN_Y);
    private double scale = 1;
    private AffineTransform modelToViewTransform;
    private ZoomHistory zoomHistory;

    public Viewport(LayerViewPanel panel) {
        this.panel = panel;
        zoomHistory = new ZoomHistory(panel);
        java2DConverter = new Java2DConverter(this);
        panel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                fireZoomChanged(getEnvelopeInModelCoordinates());
            }
        });
    }

    public LayerViewPanel getPanel() {
        return panel;
    }

    public void addListener(ViewportListener l) {
        listeners.add(l);
    }

    public void removeListener(ViewportListener l) {
        listeners.remove(l);
    }

    public Java2DConverter getJava2DConverter() {
        return java2DConverter;
    }

    public void setJava2DConverter(Java2DConverter converter) {
        java2DConverter = converter;
    }

    public ZoomHistory getZoomHistory() {
        return zoomHistory;
    }

    public void update() throws NoninvertibleTransformException {
        modelToViewTransform =
            modelToViewTransform(scale, viewOriginAsPerceivedByModel, panel.getSize().height);
        panel.repaint();
    }

    public static AffineTransform modelToViewTransform(
        double scale,
        Point2D viewOriginAsPerceivedByModel,
        double panelHeight) {
        AffineTransform modelToViewTransform = new AffineTransform();
        modelToViewTransform.translate(0, panelHeight);
        modelToViewTransform.scale(1, -1);
        modelToViewTransform.scale(scale, scale);
        modelToViewTransform.translate(
            -viewOriginAsPerceivedByModel.getX(),
            -viewOriginAsPerceivedByModel.getY());
        return modelToViewTransform;
    }

    public double getScale() {
        return scale;
    }

    /**
     * Set both values but repaint once.
     */
    public void initialize(double newScale, Point2D newViewOriginAsPerceivedByModel) {
        setScale(newScale);
        viewOriginAsPerceivedByModel = newViewOriginAsPerceivedByModel;

        //Don't call #update here, because this method may be called before the
        //panel has been made visible, causing LayerViewPanel#createImage
        //to return null, causing a NullPointerException in
        //LayerViewPanel#updateImageBuffer. [Jon Aquino]
    }

    public Point2D getOriginInModelCoordinates() {
        return viewOriginAsPerceivedByModel;
    }

    /**
     * Of widthOfNewViewAsPerceivedByOldView and heightOfNewViewAsPerceivedByOldView,
     * this method will choose the one producing the least zoom.
     */
    public void zoom(
        Point2D centreOfNewViewAsPerceivedByOldView,
        double widthOfNewViewAsPerceivedByOldView,
        double heightOfNewViewAsPerceivedByOldView)
        throws NoninvertibleTransformException {
        double zoomFactor =
            Math.min(
                panel.getSize().width / widthOfNewViewAsPerceivedByOldView,
                panel.getSize().height / heightOfNewViewAsPerceivedByOldView);
        double realWidthOfNewViewAsPerceivedByOldView = panel.getSize().width / zoomFactor;
        double realHeightOfNewViewAsPerceivedByOldView = panel.getSize().height / zoomFactor;

        zoom(
            toModelEnvelope(
                centreOfNewViewAsPerceivedByOldView.getX()
                    - (0.5 * realWidthOfNewViewAsPerceivedByOldView),
                centreOfNewViewAsPerceivedByOldView.getX()
                    + (0.5 * realWidthOfNewViewAsPerceivedByOldView),
                centreOfNewViewAsPerceivedByOldView.getY()
                    - (0.5 * realHeightOfNewViewAsPerceivedByOldView),
                centreOfNewViewAsPerceivedByOldView.getY()
                    + (0.5 * realHeightOfNewViewAsPerceivedByOldView)));
    }

    public Point2D toModelPoint(Point2D viewPoint) throws NoninvertibleTransformException {
        return getModelToViewTransform().inverseTransform(toPoint2DDouble(viewPoint), null);
    }

    private Point2D.Double toPoint2DDouble(Point2D p) {
        //If you pass a non-Double Point2D to an AffineTransform, the AffineTransform
        //will be done using floats instead of doubles. [Jon Aquino]
        if (p instanceof Point2D.Double) {
            return (Point2D.Double) p;
        }
        return new Point2D.Double(p.getX(), p.getY());
    }

    public Coordinate toModelCoordinate(Point2D viewPoint) throws NoninvertibleTransformException {
        return CoordUtil.toCoordinate(toModelPoint(viewPoint));
    }

    public Point2D toViewPoint(Point2D modelPoint) throws NoninvertibleTransformException {
        return getModelToViewTransform().transform(toPoint2DDouble(modelPoint), null);
    }

    public Point2D toViewPoint(Coordinate modelCoordinate)
    throws NoninvertibleTransformException {
        //Optimization recommended by Todd Warnes [Jon Aquino 2004-02-06]
        Point2D.Double pt = new Point2D.Double(modelCoordinate.x, modelCoordinate.y);
        return getModelToViewTransform().transform(pt, pt);
    }

    public Envelope toModelEnvelope(double x1, double x2, double y1, double y2)
        throws NoninvertibleTransformException {
        Coordinate c1 = toModelCoordinate(new Point2D.Double(x1, y1));
        Coordinate c2 = toModelCoordinate(new Point2D.Double(x2, y2));

        return new Envelope(c1, c2);
    }

    public AffineTransform getModelToViewTransform() throws NoninvertibleTransformException {
        if (modelToViewTransform == null) {
            update();
        }

        return modelToViewTransform;
    }

    public Envelope getEnvelopeInModelCoordinates() {
        double widthAsPerceivedByModel = panel.getWidth() / scale;
        double heightAsPerceivedByModel = panel.getHeight() / scale;

        return new Envelope(
            viewOriginAsPerceivedByModel.getX(),
            viewOriginAsPerceivedByModel.getX() + widthAsPerceivedByModel,
            viewOriginAsPerceivedByModel.getY(),
            viewOriginAsPerceivedByModel.getY() + heightAsPerceivedByModel);
    }

    //<<TODO:IMPROVE>> Currently the zoomed image is aligned west in the viewport.
    //It should be centred. [Jon Aquino]
    public void zoom(Envelope modelEnvelope) throws NoninvertibleTransformException {
        if (modelEnvelope.isNull()) {
            return;
        }

        if (!zoomHistory.hasNext() && !zoomHistory.hasPrev()) {
            //When the first extent is added, first add the existing extent.
            //Must do this late because it's hard to tell when the panel is realized.
            //[Jon Aquino]
            zoomHistory.add(getEnvelopeInModelCoordinates());
        }

        setScale(
            Math.min(
                panel.getWidth() / modelEnvelope.getWidth(),
                panel.getHeight() / modelEnvelope.getHeight()));
        double xCenteringOffset = ((panel.getWidth() / scale) - modelEnvelope.getWidth()) / 2d; 
        double yCenteringOffset = ((panel.getHeight() / scale) - modelEnvelope.getHeight()) / 2d; 
        viewOriginAsPerceivedByModel =
            new Point2D.Double(modelEnvelope.getMinX() - xCenteringOffset, modelEnvelope.getMinY() - yCenteringOffset);
        update();
        zoomHistory.add(modelEnvelope);
        fireZoomChanged(modelEnvelope);
    }

    private void setScale(double scale) {
        this.scale = scale;
    }

    private void fireZoomChanged(Envelope modelEnvelope) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            ViewportListener l = (ViewportListener) i.next();
            l.zoomChanged(modelEnvelope);
        }
    }

    public void zoomToFullExtent() throws NoninvertibleTransformException {
        zoom(fullExtent());
    }

    public Envelope fullExtent() {
        return EnvelopeUtil.bufferByFraction(panel.getLayerManager().getEnvelopeOfAllLayers(true), 0.03);
    }

    public void zoomToViewPoint(Point2D centreOfNewViewAsPerceivedByOldView, double zoomFactor)
        throws NoninvertibleTransformException {
        double widthOfNewViewAsPerceivedByOldView = panel.getWidth() / zoomFactor;
        double heightOfNewViewAsPerceivedByOldView = panel.getHeight() / zoomFactor;
        zoom(
            centreOfNewViewAsPerceivedByOldView,
            widthOfNewViewAsPerceivedByOldView,
            heightOfNewViewAsPerceivedByOldView);
    }

    public Collection toViewPoints(Collection modelCoordinates)
        throws NoninvertibleTransformException {
        ArrayList viewPoints = new ArrayList();
        for (Iterator i = modelCoordinates.iterator(); i.hasNext();) {
            Coordinate modelCoordinate = (Coordinate) i.next();
            viewPoints.add(toViewPoint(modelCoordinate));
        }
        return viewPoints;
    }

    public Rectangle2D toViewRectangle(Envelope envelope) throws NoninvertibleTransformException {
        Point2D p1 = toViewPoint(new Coordinate(envelope.getMinX(), envelope.getMinY()));
        Point2D p2 = toViewPoint(new Coordinate(envelope.getMaxX(), envelope.getMaxY()));
        return new Rectangle2D.Double(
            Math.min(p1.getX(), p2.getX()),
            Math.min(p1.getY(), p2.getY()),
            Math.abs(p1.getX() - p2.getX()),
            Math.abs(p1.getY() - p2.getY()));
    }
}
