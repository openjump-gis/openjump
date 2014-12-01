
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

package com.vividsolutions.jump.workbench.ui.cursortool.editing;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GeometryEditor;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DelegatingTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;
public class FeatureDrawingUtil {
    private Collection selectedFeaturesContaining(Polygon polygon, LayerViewPanel panel) {
        if (layerNamePanelProxy.getLayerNamePanel().chooseEditableLayer() == null) {
            return new ArrayList();
        }
        ArrayList selectedFeaturesContainingPolygon = new ArrayList();
        for (Iterator i = panel.getSelectionManager().getFeaturesWithSelectedItems(layerNamePanelProxy.getLayerNamePanel().chooseEditableLayer()).iterator(); i.hasNext(); ) {
            Feature feature = (Feature) i.next();
            //Unfortunately, GeometryCollection does not yet support either
            //#contains or (more importantly) #difference. [Jon Aquino]
            //Use == rather than instanceof because MultiPoint, MultiLineString and
            //MultiPolygon do not have this problem. [Jon Aquino]
            if (feature.getGeometry().getClass() == GeometryCollection.class) {
                continue;
            }
            if (!feature.getGeometry().getEnvelopeInternal().contains(polygon.getEnvelopeInternal())) { continue; }
            if (feature.getGeometry().contains(polygon)) {
                selectedFeaturesContainingPolygon.add(feature);
            }
        }
        return selectedFeaturesContainingPolygon;
    }
    private void createHole(
        Polygon hole,
        Collection features,
        Layer layer,
        LayerViewPanel panel,
        boolean rollingBackInvalidEdits,
        String transactionName) {
        Assert.isTrue(hole.getNumInteriorRing() == 0);
        EditTransaction transaction =
            new EditTransaction(features, transactionName, layer, rollingBackInvalidEdits, false, panel);
        for (int i = 0; i < transaction.size(); i++) {
            transaction.setGeometry(i, transaction.getGeometry(i).difference(hole));
        }
        transaction.commit();
    }
    private LayerNamePanelProxy layerNamePanelProxy;
    public FeatureDrawingUtil(LayerNamePanelProxy layerNamePanelProxy) {
        this.layerNamePanelProxy = layerNamePanelProxy;
    }
    private Layer layer(LayerViewPanel layerViewPanel) {
        if (layerNamePanelProxy.getLayerNamePanel().chooseEditableLayer() == null) {
            Layer layer =
                layerViewPanel.getLayerManager().addLayer(
                    StandardCategoryNames.WORKING,
                    I18N.get("ui.cursortool.editing.FeatureDrawingUtil.new"),
                    AddNewLayerPlugIn.createBlankFeatureCollection());
            layer.setEditable(true);
            layerViewPanel.getContext().warnUser(
            		I18N.get("ui.cursortool.editing.FeatureDrawingUtil.no-layer-is-editable-creating-new-editable-layer"));
        }
        return layerNamePanelProxy.getLayerNamePanel().chooseEditableLayer();
    }
    /**
     * The calling CursorTool should call #preserveUndoHistory; otherwise the
     * undo history will be (unnecessarily) truncated if a problem occurs.
     * @return null if the geometry is invalid
     */
    public UndoableCommand createAddCommand(
        Geometry geometry,
        boolean rollingBackInvalidEdits,
        LayerViewPanel layerViewPanel,
        AbstractCursorTool tool) {
        if (rollingBackInvalidEdits && !geometry.isValid()) {
            layerViewPanel.getContext().warnUser(I18N.get("ui.cursortool.editing.FeatureDrawingUtil.draw-feature-tool-topology-error"));
            return null;
        }
        //Don't want viewport to change at this stage. [Jon Aquino]
        layerViewPanel.setViewportInitialized(true);
        final Layer layer = layer(layerViewPanel);
        final Feature feature =
            FeatureUtil.toFeature(editor.removeRepeatedPoints(geometry), layer.getFeatureCollectionWrapper().getFeatureSchema());
        return new UndoableCommand(tool.getName()) {
            public void execute() {
                layer.getFeatureCollectionWrapper().add(feature);
            }
            public void unexecute() {
                layer.getFeatureCollectionWrapper().remove(feature);
            }
        };
    }
    private GeometryEditor editor = new GeometryEditor();
    /**
     * Apply settings common to all feature-drawing tools.
     */
    public CursorTool prepare(final AbstractCursorTool drawFeatureTool, boolean allowSnapping) {
        drawFeatureTool.setColor(Color.red);
        if (allowSnapping) { drawFeatureTool.allowSnapping(); } 
        return new DelegatingTool(drawFeatureTool) {
            public String getName() {
                return drawFeatureTool.getName();
            }
            public Cursor getCursor() {
                if (Toolkit
                    .getDefaultToolkit()
                    .getBestCursorSize(32, 32)
                    .equals(new Dimension(0, 0))) {
                    return Cursor.getDefaultCursor();
                }
                return Toolkit.getDefaultToolkit().createCustomCursor(
                    IconLoader.icon("Pen.gif").getImage(),
                    new java.awt.Point(1, 31),
                    drawFeatureTool.getName());
            }
        };
    }
    public void drawRing(
        Polygon polygon,
        boolean rollingBackInvalidEdits,
        AbstractCursorTool tool,
        LayerViewPanel panel) {
        Collection selectedFeaturesContainingPolygon = selectedFeaturesContaining(polygon, panel);
        if (selectedFeaturesContainingPolygon.isEmpty()) {
            AbstractPlugIn.execute(createAddCommand(polygon, rollingBackInvalidEdits, panel, tool), panel);
        } else {
            createHole(
                polygon,
                selectedFeaturesContainingPolygon,
                layer(panel),
                panel,
                rollingBackInvalidEdits,
                tool.getName());
        }
    }
}
