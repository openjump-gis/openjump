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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.renderer.LineStringSelectionRenderer;

/**
* A collection of selected {@link LineString LineStrings}.
*/

public class LineStringSelection extends AbstractSelection {
    public List items(Geometry geometry) {
        ArrayList items = new ArrayList();

        if (geometry instanceof LineString) {
            items.add(geometry);
        }

        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            items.add(polygon.getExteriorRing());

            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                items.add(polygon.getInteriorRingN(i));
            }
        }

        if (geometry instanceof GeometryCollection) {
            GeometryCollection geometryCollection = (GeometryCollection) geometry;

            for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
                items.addAll(items(geometryCollection.getGeometryN(
                            i)));
            }
        }

        return items;
    }    
    
    public LineStringSelection(SelectionManager selectionManager) {
        super(selectionManager);
    }    

    public String getRendererContentID() {
        return LineStringSelectionRenderer.CONTENT_ID;
    }

    protected boolean selectedInAncestors(Layer layer, Feature feature, Geometry item) {
        Assert.isTrue(getParent().getParent() instanceof FeatureSelection);
        Assert.isTrue(getParent() instanceof PartSelection);        
        if (getParent().getParent().getFeaturesWithSelectedItems().contains(feature)) { return true; }
        for (Iterator i = getParent().getSelectedItems(layer, feature).iterator(); i.hasNext(); ) {
            Geometry selectedPart = (Geometry) i.next();
            if (items(selectedPart).contains(item)) { return true; }
        }
        return false;
    }

    protected void unselectInDescendants(Layer layer, Feature feature, Collection items) {
        Assert.isTrue(getChild() == null);        
    }

}
