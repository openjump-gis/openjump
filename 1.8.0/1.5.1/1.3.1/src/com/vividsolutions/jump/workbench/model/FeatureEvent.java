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
import java.util.Collection;
import java.util.Collections;
import com.vividsolutions.jts.util.Assert;

/**
 * An addition, removal, or modification of a Feature.
 *
 * @see com.vividsolutions.jump.feature.Feature Feature
 * @see FeatureEventType
 */
public class FeatureEvent {
    private Layer layer;
    private FeatureEventType type;
    private Collection features;
    private Collection oldFeatureClones;
    
    //[UT] 25.08.2005 added 
    private Collection oldFeatureAttClones;
    
    /**
     * @param oldFeatureClones for GEOMETRY_MODIFIED events, clones of the Features before
     * they were modified; null for other events
     */
    public FeatureEvent(
        Collection features,
        FeatureEventType type,
        Layer layer,
        Collection oldFeatureClones) {
        Assert.isTrue(layer != null);
        Assert.isTrue(type != null);
        
        Assert.isTrue(
            (type == FeatureEventType.GEOMETRY_MODIFIED && oldFeatureClones != null)
                || (type != FeatureEventType.GEOMETRY_MODIFIED && oldFeatureClones == null)
                || (type == FeatureEventType.ATTRIBUTES_MODIFIED && oldFeatureAttClones == null ) );
        
        this.layer = layer;
        this.type = type;
        this.features = features;

        //      [UT] 25.08.2005 uncommented and...
        //      this.oldFeatureClones = oldFeatureClones;
        //[UT] 25.08.2005 did like this
        if( this.type == FeatureEventType.GEOMETRY_MODIFIED){
            this.oldFeatureClones = oldFeatureClones;
        } else if( this.type == FeatureEventType.ATTRIBUTES_MODIFIED){
            this.oldFeatureAttClones = oldFeatureClones;
        }
    }
    public Layer getLayer() {
        return layer;
    }
    public FeatureEventType getType() {
        return type;
    }
    public Collection getFeatures() {
        return Collections.unmodifiableCollection(features);
    }
    public Collection getOldFeatureClones() {
        return Collections.unmodifiableCollection(oldFeatureClones);
    }
    
    //[UT] 25.08.2005 added 
    public Collection getOldFeatureAttClones() {
        return Collections.unmodifiableCollection(oldFeatureAttClones);
    }
}
