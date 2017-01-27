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

package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.AbstractVectorLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.UndoableCommand;

public abstract class VectorTool extends NClickTool {

    public VectorTool() {
        super(2);
        setStroke(new BasicStroke(1));
        allowSnapping();
    }

    protected Feature feature(LineString lineString, Layer layer, UndoableCommand command) {
        Feature feature = new BasicFeature(layer.getFeatureCollectionWrapper().getFeatureSchema());
        feature.setGeometry(lineString);
        return feature;
    }

    protected LineString lineString(Coordinate source, Coordinate destination)
        throws NoninvertibleTransformException {
        return geometryFactory.createLineString(new Coordinate[] { source, destination });
    }

    protected GeometryFactory geometryFactory = new GeometryFactory();

    protected Shape getShape(Point2D source, Point2D destination) {
        return new Line2D.Double(source, destination);
    }

    protected abstract AbstractVectorLayerFinder createVectorLayerFinder(LayerManagerProxy layerManagerProxy);

    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();
        //Don't want viewport to change at this stage. [Jon Aquino]
        getPanel().setViewportInitialized(true);
        execute(createCommand());
    }

    protected UndoableCommand createCommand() throws NoninvertibleTransformException {
        final AbstractVectorLayerFinder vectorLayerFinder =
            createVectorLayerFinder(getPanel());
        final boolean vectorLayerExistedOriginally = vectorLayerFinder.getLayer() != null;
        final LineString lineString = lineString(getModelSource(), getModelDestination());
        return new UndoableCommand(getName()) {
            private Feature vector;
            private boolean vectorLayerVisibleOriginally;
            public void execute() {
                if (!vectorLayerExistedOriginally) {
                    vectorLayerFinder.createLayer();
                }
                if (vector == null) {
                    //Cache the vector because (1) we don't want to create a new feature
                    //when redo is pressed. [Jon Aquino]
                    vector = feature(lineString, vectorLayerFinder.getLayer(), this);
                }
                vectorLayerFinder.getLayer().getFeatureCollectionWrapper().add(vector);
                vectorLayerVisibleOriginally = vectorLayerFinder.getLayer().isVisible();
                vectorLayerFinder.getLayer().setVisible(true);
            }
            public void unexecute() {
                vectorLayerFinder.getLayer().setVisible(vectorLayerVisibleOriginally);
                vectorLayerFinder.getLayer().getFeatureCollectionWrapper().remove(vector);
                if (!vectorLayerExistedOriginally) {
                    getPanel().getLayerManager().remove(vectorLayerFinder.getLayer());
                }
            }
        };
    }

}
