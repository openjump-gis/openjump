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
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.renderer.PartSelectionRenderer;

/**
* A collection of selected {@link Geometry} objects (parts of larger
* selections).
*/

public class PartSelection extends AbstractSelection {

    /**
     * Returns a list containing geometry components of this geometry.
     */
    public List<Geometry> items(Geometry geometry) {
        int partNumber = geometry.getNumGeometries();
        List<Geometry> items = new ArrayList<Geometry>(partNumber);
        if (partNumber > 1) {
            for (int i = 0; i < partNumber; i++) {
                items.addAll(items(geometry.getGeometryN(i)));
            }
        }
        else {
            items.add(geometry);
        }
        return items;
    }    
    
    public PartSelection(SelectionManager selectionManager) {
        super(selectionManager);
    }    

    public String getRendererContentID() {
        return PartSelectionRenderer.CONTENT_ID;
    }


    protected boolean selectedInAncestors(Layer layer, Feature feature, Geometry item) {
        Assert.isTrue(getParent() instanceof FeatureSelection);
        return getParent().getFeaturesWithSelectedItems().contains(feature);
    }

    protected void unselectInDescendants(Layer layer, Feature feature, Collection items) {
        Assert.isTrue(getChild() instanceof LineStringSelection);
        for (Iterator i = items.iterator(); i.hasNext();) {
            Geometry part = (Geometry) i.next();
            List partLineStrings = getChild().items(part);
            for (Iterator j = getChild().getSelectedItems(layer, feature).iterator();
                j.hasNext();
                ) {
                LineString selectedLineString = (LineString) j.next();
                if (partLineStrings.contains(selectedLineString)) {
                    getChild().unselectItem(
                        layer,
                        feature,
                        partLineStrings.indexOf(selectedLineString));
                }
            }
        }
    }

}
