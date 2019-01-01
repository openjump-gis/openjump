
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

package com.vividsolutions.jump.feature;

import java.util.*;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jump.I18N;


/**
 * An IndexedFeatureCollection creates a new collection which is backed by a
 * FeatureCollection, but which is indexed for query purposes. In this
 * implementation, Features cannot be added or removed (an Exception is thrown)
 * and Features' Geometries should not be modified (otherwise they will be out
 * of sync with the spatial index).
 */
public class IndexedFeatureCollection extends FeatureCollectionWrapper {
    private SpatialIndex spatialIndex;

    /**
     * Constructs an IndexedFeatureCollection wrapping the given FeatureCollection
     * and using the default spatial index.
     */
    public IndexedFeatureCollection(FeatureCollection fc) {
        //Based on tests on Victoria ICI data, 10 is an optimum node-capacity for
        //fast queries. [Jon Aquino]
        this(fc, new STRtree(10));
    }

    /**
     * Constructs an IndexedFeatureCollection wrapping the given FeatureCollection
     * and using the given empty spatial index.
     */
    public IndexedFeatureCollection(FeatureCollection fc,
        SpatialIndex spatialIndex) {
        super(fc);
        this.spatialIndex = spatialIndex;
        createIndex();
    }

    @Override
    public void add(Feature feature) {
        throw new UnsupportedOperationException(I18N.get("feature.IndexedFeatureCollection.index-cannot-be-modified"));
    }

    @Override
    public void remove(Feature feature) {
        throw new UnsupportedOperationException(I18N.get("feature.IndexedFeatureCollection.index-cannot-be-modified"));
    }

    @Override
    public List<Feature> query(Envelope env) {

        // index query returns list of *potential* overlaps (e.g. it is a primary filter)
        List<Feature> candidate = spatialIndex.query(env);

        // filter out only Features where envelope actually intersects
        List<Feature> result = new ArrayList<>();

        for (Feature feature : candidate) {
            Geometry g = feature.getGeometry();

            if (env.intersects(g.getEnvelopeInternal())) {
                result.add(feature);
            }
        }

        return result;
    }

    private void createIndex() {
        for (Iterator i = iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            spatialIndex.insert(f.getGeometry().getEnvelopeInternal(), f);
        }
    }

    @Override
    public void addAll(Collection<Feature> features) {
        throw new UnsupportedOperationException(I18N.get("feature.IndexedFeatureCollection.index-cannot-be-modified"));
    }

    @Override
    public Collection<Feature> remove(Envelope env) {
        throw new UnsupportedOperationException(I18N.get("feature.IndexedFeatureCollection.index-cannot-be-modified"));
    }

    @Override
    public void removeAll(Collection<Feature> features) {
        throw new UnsupportedOperationException(I18N.get("feature.IndexedFeatureCollection.index-cannot-be-modified"));
    }
}
