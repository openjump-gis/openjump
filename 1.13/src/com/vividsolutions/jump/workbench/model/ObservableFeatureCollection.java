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

package com.vividsolutions.jump.workbench.model;

import java.util.*;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;


/**
 * Notifies listeners when features are added to or removed from a 
 * FeatureCollection.
 * <p> 
 * Prefer #addAll and #removeAll to #add and #remove, so that fewer events
 * will be fired.</p>
 */
public class ObservableFeatureCollection extends FeatureCollectionWrapper {

    private ArrayList<ObservableFeatureCollection.Listener> listeners =
            new ArrayList<>();

    public ObservableFeatureCollection(FeatureCollection fc) {
        super(fc);
    }


    public void add(Listener listener) {
        listeners.add(listener);
    }

    public void add(Feature feature) {
        super.add(feature);
        fireFeaturesAdded(Collections.singletonList(feature));
    }

    public void remove(Feature feature) {
        super.remove(feature);
        fireFeaturesRemoved(Collections.singletonList(feature));
    }

    private void fireFeaturesAdded(Collection<Feature> features) {
        for (Listener listener : listeners) {
            listener.featuresAdded(features);
        }
    }

    private void fireFeaturesRemoved(Collection<Feature> features) {
        for (Listener listener : listeners) {
            listener.featuresRemoved(features);
        }
    }

    public void addAll(Collection<Feature> features) {
        super.addAll(features);
        fireFeaturesAdded(features);
    }

    public void removeAll(Collection<Feature> features) {
        super.removeAll(features);
        fireFeaturesRemoved(features);
    }

    public Collection<Feature> remove(Envelope env) {
        Collection<Feature> features = super.remove(env);
        fireFeaturesRemoved(features);

        return features;
    }

    /**
     * Listens for features being added to or removed from a 
     * FeatureCollection.
     */
    public interface Listener {
        void featuresAdded(Collection features);

        void featuresRemoved(Collection features);
    }

}
