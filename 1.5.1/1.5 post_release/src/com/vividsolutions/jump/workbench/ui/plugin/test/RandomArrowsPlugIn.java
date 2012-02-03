
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

package com.vividsolutions.jump.workbench.ui.plugin.test;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringEndpointStyle;


public class RandomArrowsPlugIn extends AbstractPlugIn {
    private static final int FEATURE_COUNT = 20;
    private static final double LAYER_SIDE_LENGTH = 100;
    private static final int MAX_SEGMENT_COUNT = 3;
    private static final double MAX_SEGMENT_LENGTH = 20;
    private GeometryFactory geometryFactory = new GeometryFactory();

    public RandomArrowsPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addLayerViewMenuItem(this,
            new String[] { MenuNames.TOOLS, MenuNames.TOOLS_GENERATE }, getName());
    }

    public boolean execute(PlugInContext context) throws Exception {
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        FeatureDataset dataset = new FeatureDataset(schema);

        for (int i = 0; i < FEATURE_COUNT; i++) {
            dataset.add(createFeature(schema));
        }

        addLayer(dataset, context);

        return true;
    }

    private void addLayer(FeatureCollection featureCollection,
        PlugInContext context) {
        Layer layer = new Layer(I18N.get("ui.test.RandomArrowsPlugIn.random-arrows"),
                context.getLayerManager().generateLayerFillColor(),
                featureCollection, context.getLayerManager());
        //Can't fire events because this Layer hasn't been added to the
        //LayerManager yet. [Jon Aquino]    
        boolean firingEvents = context.getLayerManager().isFiringEvents();
        context.getLayerManager().setFiringEvents(false);
        try {
            layer.addStyle(new ArrowLineStringEndpointStyle.NarrowSolidEnd());
        } finally {
            context.getLayerManager().setFiringEvents(firingEvents);
        }

        context.getLayerManager().addLayer(StandardCategoryNames.WORKING, layer);
    }

    private Feature createFeature(FeatureSchema schema) {
        ArrayList coordinates = new ArrayList();
        coordinates.add(CoordUtil.add(
                new Coordinate(LAYER_SIDE_LENGTH / 2d, LAYER_SIDE_LENGTH / 2d),
                randomCoordinate(LAYER_SIDE_LENGTH / 2d)));

        int walkMax = (int) Math.ceil(Math.random() * MAX_SEGMENT_LENGTH);
        int segmentCount = (int) Math.ceil(Math.random() * MAX_SEGMENT_COUNT);

        for (int i = 0; i < segmentCount; i++) {
            Coordinate prevCoordinate = (Coordinate) coordinates.get(coordinates.size() -
                    1);
            coordinates.add(CoordUtil.add(prevCoordinate,
                    randomCoordinate(walkMax)));
        }

        LineString lineString = geometryFactory.createLineString((Coordinate[]) coordinates.toArray(
                    new Coordinate[] {  }));
        Feature feature = new BasicFeature(schema);
        feature.setGeometry(lineString);

        return feature;
    }

    private Coordinate randomCoordinate(double walkMax) {
        return CoordUtil.add(new Coordinate(-walkMax / 2d, -walkMax / 2d),
            new Coordinate(Math.random() * walkMax, Math.random() * walkMax));
    }
}
