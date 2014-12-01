
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

package com.vividsolutions.jump.workbench.ui.plugin.generate;

import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.warp.Triangle;
import com.vividsolutions.jump.warp.Triangulator;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.warp.WarpingPanel;
import com.vividsolutions.jump.workbench.ui.warp.WarpingVectorLayerFinder;

/**
 * See White, Marvin S., Jr. and Griffin, Patricia. 1985. Piecewise linear
 * rubber-sheet map transformation. "The American Cartographer" 12:2,
 * 123-31.
 */
public class ShowTriangulationPlugIn extends AbstractPlugIn {
    private final static Color GOLD = new Color(255, 192, 0, 150);
    private Triangulator triangulator = new Triangulator();

    public ShowTriangulationPlugIn(WarpingPanel warpingPanel) {
        this.warpingPanel = warpingPanel;
    }
    
    private WarpingPanel warpingPanel;

    public void initialize(PlugInContext context) throws Exception {}

    public EnableCheck createEnableCheck(WorkbenchContext context) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(context);
        return new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
    }

    public final static String SOURCE_LAYER_NAME = I18N.get("ui.plugin.generate.ShowTriangulationPlugIn.initial-triangulation");
    public final static String DESTINATION_LAYER_NAME = I18N.get("ui.plugin.generate.ShowTriangulationPlugIn.final-triangulation");
    private Layer sourceLayer(LayerManagerProxy layerManagerProxy) {
        return layerManagerProxy.getLayerManager().getLayer(SOURCE_LAYER_NAME);
    }
    private Layer destinationLayer(LayerManagerProxy layerManagerProxy) {
        return layerManagerProxy.getLayerManager().getLayer(DESTINATION_LAYER_NAME);
    }

    private WarpingVectorLayerFinder warpingVectorLayerFinder(LayerManagerProxy proxy) {
        return new WarpingVectorLayerFinder(proxy);
    }
    private Envelope envelopeOfTails(Collection vectors) {
        Envelope envelope = new Envelope();
        for (Iterator i = vectors.iterator(); i.hasNext();) {
            LineString vector = (LineString) i.next();
            envelope.expandToInclude(vector.getCoordinateN(0));
        }
        return envelope;
    }
    public boolean execute(final PlugInContext context) throws Exception {
        context.getLayerManager().getUndoableEditReceiver().reportNothingToUndoYet();
        execute(createCommand(context.getWorkbenchContext(), true), context);
        return true;
    }

    private UndoableCommand createCommand(
        final WorkbenchContext context,
        final boolean createLayersIfNonExistent) {
        Envelope datasetEnvelope = new Envelope();
        if (warpingPanel.currentSourceLayer() != null) {
            datasetEnvelope =
                warpingPanel.currentSourceLayer().getFeatureCollectionWrapper().getEnvelope();
        }
        if (datasetEnvelope.isNull()) {
            datasetEnvelope = envelopeOfTails(warpingVectorLayerFinder(context).getVectors());
        }
        if (datasetEnvelope.isNull()) {
            return UndoableCommand.DUMMY;
        }
        if (datasetEnvelope.getWidth() == 0) {
            //Otherwise we could end up with zero-area quadrilaterals [Jon Aquino]
            datasetEnvelope.expandToInclude(
                new Coordinate(datasetEnvelope.getMinX() + 1, datasetEnvelope.getMinY()));
            datasetEnvelope.expandToInclude(
                new Coordinate(datasetEnvelope.getMinX() - 1, datasetEnvelope.getMinY()));
        }
        if (datasetEnvelope.getHeight() == 0) {
            datasetEnvelope.expandToInclude(
                new Coordinate(datasetEnvelope.getMinX(), datasetEnvelope.getMinY() + 1));
            datasetEnvelope.expandToInclude(
                new Coordinate(datasetEnvelope.getMinX(), datasetEnvelope.getMinY() - 1));
        }
        Map triangleMap =
            triangulator.triangleMap(
                datasetEnvelope,
                warpingVectorLayerFinder(context).getVectors(),
                new DummyTaskMonitor());
        List[] sourceAndDestinationTriangles =
            CollectionUtil.keysAndCorrespondingValues(triangleMap);
        final FeatureCollection sourceFeatureCollection =
            toFeatureCollection(sourceAndDestinationTriangles[0]);
        final FeatureCollection destinationFeatureCollection =
            toFeatureCollection(sourceAndDestinationTriangles[1]);
        return addUndo(new UndoableCommand(getName()) {
            public void execute() {
                if (sourceLayer(context) != null) {
                    sourceLayer(context).setFeatureCollection(sourceFeatureCollection);
                    sourceLayer(context).setVisible(true);
                }
                if (sourceLayer(context) == null && createLayersIfNonExistent) {
                    Layer sourceLayer =
                        context.getLayerManager().addLayer(
                            StandardCategoryNames.WORKING,
                            SOURCE_LAYER_NAME,
                            sourceFeatureCollection);
                    init(sourceLayer, Color.gray, 150, 1);
                }
                if (destinationLayer(context) != null) {
                    destinationLayer(context).setFeatureCollection(destinationFeatureCollection);
                    destinationLayer(context).setVisible(true);
                }
                if (destinationLayer(context) == null && createLayersIfNonExistent) {
                    Layer destinationLayer =
                        context.getLayerManager().addLayer(
                            StandardCategoryNames.WORKING,
                            DESTINATION_LAYER_NAME,
                            destinationFeatureCollection);
                    init(destinationLayer, GOLD, 255, 1);
                }
            }
            public void unexecute() {
                //Undo is handled by #addUndo. [Jon Aquino]
            }
        }, context);
    }

    public UndoableCommand addLayerGeneration(
        final UndoableCommand wrappeeCommand,
        final WorkbenchContext context,
        final boolean createLayersIfNonExistent) {
        return new UndoableCommand(wrappeeCommand.getName()) {
            private UndoableCommand layerGenerationCommand = null;
            private UndoableCommand layerGenerationCommand() {
                if (layerGenerationCommand == null) {
                    layerGenerationCommand = createCommand(context, createLayersIfNonExistent);
                }
                return layerGenerationCommand;
            }
            public void execute() {
                wrappeeCommand.execute();
                layerGenerationCommand().execute();
            }
            public void unexecute() {
                layerGenerationCommand().unexecute();
                wrappeeCommand.unexecute();
            }
        };
    }

    public static UndoableCommand addUndo(
        final UndoableCommand wrappeeCommand,
        final LayerManagerProxy proxy) {
        return Layer.addUndo(
            DESTINATION_LAYER_NAME,
            proxy,
            Layer.addUndo(SOURCE_LAYER_NAME, proxy, wrappeeCommand));
    }

    private final static String WARP_ID_NAME = "WARP_ID";

    private FeatureCollection toFeatureCollection(Collection triangles) {
        FeatureSchema featureSchema = new FeatureSchema();
        featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featureSchema.addAttribute(WARP_ID_NAME, AttributeType.INTEGER);

        FeatureCollection featureCollection = new FeatureDataset(featureSchema);

        int j = 0;
        for (Iterator i = triangles.iterator(); i.hasNext();) {
            Triangle t = (Triangle) i.next();
            j++;
            Feature feature = new BasicFeature(featureSchema);
            feature.setGeometry(factory.createPolygon(t.toLinearRing(), null));
            feature.setAttribute(WARP_ID_NAME, new Integer(j));
            featureCollection.add(feature);
        }

        return featureCollection;
    }

    private GeometryFactory factory = new GeometryFactory();

    private void init(Layer layer, Color color, int alpha, int lineWidth) {
        boolean firingEvents = layer.getLayerManager().isFiringEvents();
        layer.getLayerManager().setFiringEvents(false);
        try {
            layer.getBasicStyle().setLineColor(color);
            layer.getBasicStyle().setFillColor(color);
            layer.getBasicStyle().setAlpha(alpha);
            layer.getBasicStyle().setLineWidth(lineWidth);
            layer.getBasicStyle().setRenderingFill(false);
            layer.getVertexStyle().setEnabled(true);
            layer.getVertexStyle().setSize(4);
            layer.getLabelStyle().setEnabled(true);
            layer.getLabelStyle().setColor(color);
            layer.getLabelStyle().setFont(new Font("Dialog", Font.PLAIN, 12));
            layer.getLabelStyle().setAttribute(WARP_ID_NAME);
            layer.getLabelStyle().setHeight(12);
            layer.getLabelStyle().setScaling(false);
            layer.getLabelStyle().setHidingOverlappingLabels(false);
        } finally {
            layer.getLayerManager().setFiringEvents(firingEvents);
        }
        layer.fireAppearanceChanged();
    }

    public Icon getIcon() {
        return IconLoader.icon("Triangle.gif");
    }
}
