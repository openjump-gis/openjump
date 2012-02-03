
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

package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import javax.swing.JComboBox;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.tools.OverlayEngine;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 *
 * Creates a new layer containing intersections of all pairs of
 * features from two input layers.  Splits {@link
 * com.vividsolutions.jts.geom.MultiPolygon Multipolygons} and {@link
 * com.vividsolutions.jts.geom.GeometryCollection
 * GeometryCollections}, and filters out non-Polygons.
 */

public class OverlayPlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    private String POLYGON_OUTPUT = I18N.get("ui.plugin.analysis.OverlayPlugIn.limit-output-to-polygons-only");
    private String FIRST_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.first-layer");
    private String SECOND_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.second-layer");
    private String TRANSFER_ATTRIBUTES_FROM_FIRST_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.transfer-attributes-from-first-layer");
    private String TRANSFER_ATTRIBUTES_FROM_SECOND_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.transfer-attributes-from-second-layer");
    private MultiInputDialog dialog;
    private OverlayEngine overlayEngine;



    public OverlayPlugIn() {
    }

    private String categoryName = StandardCategoryNames.RESULT;

    public void setCategoryName(String value) {
        categoryName = value;
    }
    
    public boolean execute(PlugInContext context) throws Exception {
    	//[sstein, 15.07.2006] placed here again otherwise language settings wont work for i18n 
		POLYGON_OUTPUT = I18N.get("ui.plugin.analysis.OverlayPlugIn.limit-output-to-polygons-only");
		FIRST_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.first-layer");
		SECOND_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.second-layer");
		TRANSFER_ATTRIBUTES_FROM_FIRST_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.transfer-attributes-from-first-layer");
		TRANSFER_ATTRIBUTES_FROM_SECOND_LAYER = I18N.get("ui.plugin.analysis.OverlayPlugIn.transfer-attributes-from-second-layer");

        overlayEngine = prompt(context);

        return overlayEngine != null;
    }

    private OverlayEngine prompt(PlugInContext context) {
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes. [Jon Aquino]
        initDialog(context);
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return null;
        }

        OverlayEngine e = new OverlayEngine();
        e.setAllowingPolygonsOnly(dialog.getBoolean(POLYGON_OUTPUT));
        e.setSplittingGeometryCollections(dialog.getBoolean(POLYGON_OUTPUT));

        return e;
    }

    private void initDialog(PlugInContext context) {
        dialog = new MultiInputDialog(context.getWorkbenchFrame(),
                getName(), true);
        dialog.setSideBarImage(IconLoader.icon("Overlay.gif"));
        dialog.setSideBarDescription(I18N.get("ui.plugin.analysis.OverlayPlugIn.create-new-layer-containing-intersections-of-all-pairs-of-input-features"));
        String fieldName = FIRST_LAYER;
        JComboBox addLayerComboBox = dialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());
        String fieldName1 = SECOND_LAYER;
        JComboBox addLayerComboBox1 = dialog.addLayerComboBox(fieldName1, context.getCandidateLayer(1), null, context.getLayerManager());
        dialog.addCheckBox(POLYGON_OUTPUT, true, I18N.get("ui.plugin.analysis.OverlayPlugIn.splits-multipolygons-and-geometry-and-filters-out-non-polygons"));
        dialog.addCheckBox(TRANSFER_ATTRIBUTES_FROM_FIRST_LAYER,
            true);
        dialog.addCheckBox(TRANSFER_ATTRIBUTES_FROM_SECOND_LAYER,
            true);
        GUIUtil.centreOnWindow(dialog);
    }

    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        FeatureCollection a = dialog.getLayer(FIRST_LAYER).getFeatureCollectionWrapper();
        FeatureCollection b = dialog.getLayer(SECOND_LAYER)
                                    .getFeatureCollectionWrapper();
        FeatureCollection overlay = overlayEngine.overlay(a, b, mapping(a, b),
                monitor);
        context.getLayerManager().addCategory(categoryName);
        context.addLayer(categoryName, I18N.get("ui.plugin.analysis.OverlayPlugIn.overlay"), overlay);
    }

    private AttributeMapping mapping(FeatureCollection a, FeatureCollection b) {
        return new AttributeMapping(dialog.getBoolean(
                TRANSFER_ATTRIBUTES_FROM_FIRST_LAYER) ? a.getFeatureSchema()
                                                      : new FeatureSchema(),
            dialog.getBoolean(TRANSFER_ATTRIBUTES_FROM_SECOND_LAYER)
            ? b.getFeatureSchema() : new FeatureSchema());
    }
}
