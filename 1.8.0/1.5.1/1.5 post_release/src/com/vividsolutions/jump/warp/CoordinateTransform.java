
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

package com.vividsolutions.jump.warp;

import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;


/**
 * A function that maps one Coordinate to another.
 */
public abstract class CoordinateTransform {
    private CoordinateFilter coordinateFilter = new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                coordinate.setCoordinate(transform(coordinate));
            }
        };

    /**
     * Maps one Coordinate to another.
     * @param c the Coordinate to map
     * @return a new Coordinate
     */
    public abstract Coordinate transform(Coordinate c);

    public FeatureCollection transform(FeatureCollection featureCollection)
        throws JUMPException {
        FeatureCollection newCollection = new FeatureDataset(featureCollection.getFeatureSchema());

        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            Geometry newGeometry = transform(feature.getGeometry());
            Feature newFeature = (Feature) feature.clone(false);
            newFeature.setGeometry(newGeometry);
            newCollection.add(newFeature);
        }

        return newCollection;
    }

    public Geometry transform(Geometry oldGeometry) {
        Geometry newGeometry = (Geometry) oldGeometry.clone();
        newGeometry.apply(coordinateFilter);
        newGeometry.geometryChanged();

        return newGeometry;
    }
}
