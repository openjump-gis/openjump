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
package com.vividsolutions.jump.plugin.edit;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.feature.Feature;

/** 
 * Data attached to a SegmentString to remember which Feature, which Geometry
 * component and which Linear element it comes from.
 * This data is used to re-build the noded Geometry after a noding process.
 */
public class SegmentStringData {

    private Feature feature;
    private int component;
    private int linearElement;
        
    public SegmentStringData(Feature feature, int component, int linearElement) {
        this.feature = feature;
        this.component = component;
        this.linearElement = linearElement;
    }
        
    public Feature getFeature() {
        return feature;
    }
    
    public int getComponent() {
        return component;
    }
    
    public int getLinearElement() {
        return linearElement;
    }
            
    public LineString getSourceLineString() {
        Geometry source = feature.getGeometry();
        Geometry sourceComponent = source.getGeometryN(component);
        if (sourceComponent instanceof Polygon) {
            if (linearElement == 0) {
                return ((Polygon)sourceComponent).getExteriorRing();
            } else {
                return ((Polygon)sourceComponent).getInteriorRingN(linearElement-1);
            }
        }
        else return (LineString)sourceComponent;
    }
    
    public String toString() {
        return "FID="+feature.getID() + 
               " Component=" + component + 
               " LinearElement="+linearElement;
    }
        
}
