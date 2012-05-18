
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import org.openjump.core.ui.SelectionStyllingOptionsPanel;

//<<TODO:REFACTORING>> Refactor code common to SelectionHandleRenderer and
//VertexRenderer [Jon Aquino]
public class FeatureSelectionRenderer extends AbstractSelectionRenderer {
    
    public final static String CONTENT_ID = "SELECTED_FEATURES";

    public FeatureSelectionRenderer(LayerViewPanel panel) {
        super(CONTENT_ID, panel, Color.yellow, true, true);
		// get the persistent Blackboard for set the selectionstyle values [Matthias Scholz 3. Sept. 2010]
		if (panel.getContext() instanceof WorkbenchFrame) {
			Blackboard blackboard = ((WorkbenchFrame)panel.getContext()).getContext().getBlackboard();
			blackboard = PersistentBlackboardPlugIn.get(blackboard);
			// the Color
			Object color = blackboard.get(SelectionStyllingOptionsPanel.BB_SELECTION_STYLE_COLOR, SelectionStyllingOptionsPanel.DEFAULT_SELECTION_STYLE_COLOR);
			if (color instanceof Color) setSelectionLineColor((Color) color);
			// the size
			Object size = blackboard.get(SelectionStyllingOptionsPanel.BB_SELECTION_STYLE_POINT_SIZE, SelectionStyllingOptionsPanel.DEFAULT_SELECTION_STYLE_POINT_SIZE);
			if (size instanceof Integer) setSelectionPointSize(((Integer) blackboard.get(SelectionStyllingOptionsPanel.BB_SELECTION_STYLE_POINT_SIZE, SelectionStyllingOptionsPanel.DEFAULT_SELECTION_STYLE_POINT_SIZE)).intValue());
			// and the form
			setSelectionPointForm((String) blackboard.get(SelectionStyllingOptionsPanel.BB_SELECTION_STYLE_POINT_FORM, SelectionStyllingOptionsPanel.DEFAULT_SELECTION_STYLE_POINT_FORM));
		}
    }
    
    protected Map<Feature,List<Geometry>> featureToSelectedItemsMap(Layer layer) {
        return panel.getSelectionManager()
                    .getFeatureSelection()
                    .getFeatureToSelectedItemCollectionMap(layer);
    }    

}
