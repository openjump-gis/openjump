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
package com.vividsolutions.jump.workbench.ui.snap;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.ViewportListener;

import java.util.ArrayList;
import java.util.Iterator;


public class VisiblePointsAndLinesCache {
    private static final String PANEL_PROPERTY_KEY = "VISIBLE_POINTS_AND_LINES_CACHE";
    private LayerListener layerListener = new LayerListener() {
            public void layerChanged(LayerEvent e) {
                invalidate();
            }

            public void featuresChanged(FeatureEvent e) {
            }

            public void categoryChanged(CategoryEvent e) {
            }
        };

    private ViewportListener viewportListener = new ViewportListener() {
            public void zoomChanged(Envelope modelEnvelope) {
                invalidate();
            }
        };

    private LayerViewPanel panel;
    private GeometryFactory factory = new GeometryFactory();
    private STRtree tree = null;

    private VisiblePointsAndLinesCache(LayerViewPanel panel) {
        this.panel = panel;
        panel.getViewport().addListener(viewportListener);
        panel.getLayerManager().addLayerListener(layerListener);
    }

    private void invalidate() {
        tree = null;
    }

    public STRtree getTree() {
        if (tree == null) {
            Envelope viewportEnvelope = panel.getViewport()
                                             .getEnvelopeInModelCoordinates();
            tree = new STRtree();
            for (Iterator i = panel.getLayerManager().iterator(); i.hasNext();) {
                Layer layer = (Layer) i.next();
                if (!layer.isVisible()) {
                    continue;
                }
                for (Iterator j = layer.getFeatureCollectionWrapper()
                                       .query(viewportEnvelope).iterator();
                        j.hasNext();) {
                    Feature feature = (Feature) j.next();
                    Geometry geometry = feature.getGeometry();
                    tree.insert(geometry.getEnvelopeInternal(),
                        toPointsAndLines(geometry));
                }
            }
        }

        return tree;
    }

    private Geometry toPointsAndLines(Geometry g) {
        if (g.getDimension() <= 1) {
            return g;
        }
        if (g instanceof GeometryCollection) {
            GeometryCollection oldCollection = (GeometryCollection) g;
            ArrayList newCollection = new ArrayList();
            for (int i = 0; i < oldCollection.getNumGeometries(); i++) {
                newCollection.add(toPointsAndLines(oldCollection.getGeometryN(i)));
            }

            return factory.createGeometryCollection((Geometry[]) newCollection.toArray(
                    new Geometry[] {  }));
        }
        Assert.isTrue(g instanceof Polygon);

        return ((Polygon) g).getBoundary();
    }

    public static VisiblePointsAndLinesCache instance(LayerViewPanel panel) {
        if (panel.getBlackboard().get(PANEL_PROPERTY_KEY) == null) {
            return (VisiblePointsAndLinesCache) panel.getBlackboard().get(PANEL_PROPERTY_KEY,
                new VisiblePointsAndLinesCache(panel));
        }

        return (VisiblePointsAndLinesCache) panel.getBlackboard().get(PANEL_PROPERTY_KEY);
    }
}
