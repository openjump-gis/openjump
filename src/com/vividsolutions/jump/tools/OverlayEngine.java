
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

package com.vividsolutions.jump.tools;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.Logger;


/**
 * Takes two FeatureCollections and returns their overlay, which is a new
 * FeatureCollection containing the intersections of all pairs of input features.
 */
public class OverlayEngine {

    private boolean splittingGeometryCollections = true;
    private boolean allowingPolygonsOnly = true;

    /**
     * Creates a new OverlayEngine.
     */
    public OverlayEngine() {
    }

    /**
     * Creates the overlay of the two datasets. The attributes from both datasets
     * will be transferred to the overlay.
     *
     *@param a  the first dataset involved in the overlay
     *@param b  the second dataset involved in the overlay
     *@return   intersections of all pairs of input features
     */
    public FeatureCollection overlay(FeatureCollection a, FeatureCollection b,
        TaskMonitor monitor) {
        return overlay(a, b,
            new AttributeMapping(a.getFeatureSchema(), b.getFeatureSchema()),
            monitor);
    }

    /**
     * Creates the overlay of the two datasets. The attributes from the datasets
     * will be transferred as specified by the AttributeMapping.
     *
     *@param a the first dataset involved in the overlay
     *@param b the second dataset involved in the overlay
     *@param mapping specifies which attributes are transferred
     *@return intersections of all pairs of input features
     */
    public FeatureCollection overlay(FeatureCollection a, FeatureCollection b,
        AttributeMapping mapping, TaskMonitor monitor) {
        monitor.allowCancellationRequests();
        monitor.report(I18N.getInstance().get("tools.OverlayEngine.indexing-second-feature-collection"));

        IndexedFeatureCollection indexedB = new IndexedFeatureCollection(b);
        monitor.report(I18N.getInstance().get("tools.OverlayEngine.overlaying-feature-collections"));

        FeatureDataset overlay = new FeatureDataset(mapping.createSchema("GEOMETRY"));
        List<Feature> aFeatures = a.getFeatures();

        int count = 0;
        for (Feature aFeature : aFeatures) {
            if (monitor.isCancelRequested()) break;

            for (Feature bFeature : indexedB.query(aFeature.getGeometry().getEnvelopeInternal())) {
                if (monitor.isCancelRequested()) break;
                addIntersection(aFeature, bFeature, mapping, overlay, monitor);
            }

            monitor.report(count++, a.size(), "features");
        }

        return overlay;
    }

    private void addIntersection(Feature a, Feature b,
        AttributeMapping mapping, FeatureCollection overlay, TaskMonitor monitor) {
        if (!a.getGeometry().getEnvelope().intersects(b.getGeometry()
                                                           .getEnvelope())) {
            return;
        }

        Geometry intersection = null;

        try {
            //TODO check with MD if it is still relevant to use EnhancedPrecisionOp
            intersection = EnhancedPrecisionOp.intersection(a.getGeometry(),
                    b.getGeometry());
        } catch (Exception ex) {
            monitor.report(ex);
            Logger.error(a.getGeometry().toString());
            Logger.error(b.getGeometry().toString());
        }

        if ((intersection == null) || intersection.isEmpty()) {
            return;
        }

        addFeature(intersection, overlay, mapping, a, b);
    }

    private void addFeature(Geometry intersection, FeatureCollection overlay,
                AttributeMapping mapping, Feature a, Feature b) {
        if (splittingGeometryCollections && intersection instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) intersection;

            for (int i = 0; i < gc.getNumGeometries(); i++) {
                addFeature(gc.getGeometryN(i), overlay, mapping, a, b);
            }

            return;
        }

        if (allowingPolygonsOnly && !(intersection instanceof Polygon || intersection instanceof MultiPolygon)) {
            return;
        }

        Feature feature = new BasicFeature(overlay.getFeatureSchema());
        mapping.transferAttributes(a, b, feature);
        feature.setGeometry(intersection);
        overlay.add(feature);
    }

    public void setSplittingGeometryCollections(
        boolean splittingGeometryCollections) {
        this.splittingGeometryCollections = splittingGeometryCollections;
    }

    public void setAllowingPolygonsOnly(boolean allowingPolygonsOnly) {
        this.allowingPolygonsOnly = allowingPolygonsOnly;
    }
}
