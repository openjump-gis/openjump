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

package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class SelectionBackgroundRenderer extends AbstractSelectionRenderer {

    public final static String CONTENT_ID = "SELECTION_BACKGROUND";

    public SelectionBackgroundRenderer(LayerViewPanel panel) {
        super(CONTENT_ID, panel, Color.yellow, false, true);
    }

    protected CollectionMap featureToSelectedItemsMap(Layer layer) {
        //Use Set because PartSelection and LineStringSelection may share features. [Jon Aquino]
        HashSet featuresNeedingBackground = new HashSet();
        featuresNeedingBackground.addAll(panel.getSelectionManager().getPartSelection().getFeaturesWithSelectedItems(layer));
        featuresNeedingBackground.addAll(panel.getSelectionManager().getLineStringSelection().getFeaturesWithSelectedItems(layer));                
        //Don't need to remove FeatureSelection features, because if a feature were
        //selected, its parts and linestrings would not be selected. [Jon Aquino]
        CollectionMap map = new CollectionMap();
        for (Iterator i = featuresNeedingBackground.iterator(); i.hasNext(); ) {
            Feature feature = (Feature) i.next();
            map.addItem(feature, feature.getGeometry());
        }
        return map;
    }

}
