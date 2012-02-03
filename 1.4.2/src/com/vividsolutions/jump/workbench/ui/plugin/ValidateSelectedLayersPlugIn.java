
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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.qa.ValidationError;
import com.vividsolutions.jump.qa.Validator;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.DualPaneInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.RingVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;


public class ValidateSelectedLayersPlugIn extends AbstractPlugIn
    implements ThreadedPlugIn {
    private static String CHECK_BASIC_TOPOLOGY = "";
    private final static String CHECK_POLYGON_ORIENTATION = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-polygon-orientation");
    private final static String CHECK_LINESTRINGS_SIMPLE = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-that-linestrings-are-simple");
    private final static String CHECK_POLYGONS_HAVE_NO_HOLES = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-polygons-and-multipolygons-with-holes");
    private final static String CHECK_NO_REPEATED_CONSECUTIVE_POINTS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-repeated-consective-points");
    private final static String CHECK_MIN_SEGMENT_LENGTH = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-minimum-segment-length");
    private final static String CHECK_MIN_ANGLE = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-minimum-angle");
    private final static String MIN_SEGMENT_LENGTH = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.minimum-segment-length");    
    private final static String MIN_ANGLE = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.minimum-angle-in-degrees");
    private final static String MIN_POLYGON_AREA = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.minimum-polygon-area");    
    private final static String CHECK_MIN_POLYGON_AREA = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-minimum-polygon-area");
    private final static String DISALLOW_POINTS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-points");
    private final static String DISALLOW_LINESTRINGS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-linestrings");
    private final static String DISALLOW_POLYGONS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-polygons");
    private final static String DISALLOW_MULTIPOINTS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-multipoints");
    private final static String DISALLOW_MULTILINESTRINGS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-multilinestrings");
    private final static String DISALLOW_MULTIPOLYGONS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-multipolygons");
    private final static String DISALLOW_GEOMETRYCOLLECTIONS = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.disallow-geometrycollections");
    private static final String ERROR = "ERROR";
    private static final String SOURCE_FID = "SOURCE_FID";
    private static final String GEOMETRY = "GEOMETRY";
    private DualPaneInputDialog dialog;
    private FeatureSchema schema;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private Color GOLD = new Color(255, 192, 0, 150);
    private Validator validator;

    public ValidateSelectedLayersPlugIn() {
        initFeatureSchema();
    }

    public boolean execute(PlugInContext context) throws Exception {
        validator = prompt(context);

        return validator != null;
    }

    public void initialize(PlugInContext context) throws Exception
    {
        	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    		featureInstaller.addMainMenuItem(
    	        this,								//exe
  				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_QA}, 	//menu path
                this.getName() + "...", //name methode .getName recieved by AbstractPlugIn 
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext())); //enable check  
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }
    
    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        //Call #getSelectedLayers before #clear, because #clear will surface
        //output window. [Jon Aquino]
        Layer[] selectedLayers = context.getSelectedLayers();
        context.getOutputFrame().createNewDocument();
        context.getOutputFrame().addHeader(1, I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.validation-errors"));

        for (int i = 0;
                (i < selectedLayers.length) && !monitor.isCancelRequested();
                i++) {
            validate(selectedLayers[i], validator, context, monitor);
        }
    }

    private void initFeatureSchema() {
        schema = new FeatureSchema();
        schema.addAttribute(ERROR, AttributeType.STRING);
        schema.addAttribute(SOURCE_FID, AttributeType.INTEGER);
        schema.addAttribute(GEOMETRY, AttributeType.GEOMETRY);
    }

    private Validator prompt(PlugInContext context) {
        if (dialog == null) {
            initDialog(context);
        }
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return null;
        }

        Validator validator = new Validator();
        validator.setCheckingBasicTopology(dialog.getBoolean(
        		CHECK_BASIC_TOPOLOGY));
        validator.setCheckingNoRepeatedConsecutivePoints(dialog.getBoolean(
                CHECK_NO_REPEATED_CONSECUTIVE_POINTS));
        validator.setCheckingLineStringsSimple(dialog.getBoolean(
                CHECK_LINESTRINGS_SIMPLE));
        validator.setCheckingPolygonOrientation(dialog.getBoolean(
                CHECK_POLYGON_ORIENTATION));
        validator.setCheckingNoHoles(dialog.getBoolean(
                CHECK_POLYGONS_HAVE_NO_HOLES));
        validator.setCheckingMinSegmentLength(dialog.getBoolean(
                CHECK_MIN_SEGMENT_LENGTH));
        validator.setCheckingMinAngle(dialog.getBoolean(CHECK_MIN_ANGLE));
        validator.setCheckingMinPolygonArea(dialog.getBoolean(
                CHECK_MIN_POLYGON_AREA));
        validator.setMinSegmentLength(dialog.getDouble(MIN_SEGMENT_LENGTH));
        validator.setMinAngle(dialog.getDouble(MIN_ANGLE));
        validator.setMinPolygonArea(dialog.getDouble(MIN_POLYGON_AREA));

        ArrayList disallowedGeometryClasses = new ArrayList();

        if (dialog.getBoolean(DISALLOW_POINTS)) {
            disallowedGeometryClasses.add(Point.class);
        }

        if (dialog.getBoolean(DISALLOW_LINESTRINGS)) {
            disallowedGeometryClasses.add(LineString.class);
        }

        if (dialog.getBoolean(DISALLOW_POLYGONS)) {
            disallowedGeometryClasses.add(Polygon.class);
        }

        if (dialog.getBoolean(DISALLOW_MULTIPOINTS)) {
            disallowedGeometryClasses.add(MultiPoint.class);
        }

        if (dialog.getBoolean(DISALLOW_MULTILINESTRINGS)) {
            disallowedGeometryClasses.add(MultiLineString.class);
        }

        if (dialog.getBoolean(DISALLOW_MULTIPOLYGONS)) {
            disallowedGeometryClasses.add(MultiPolygon.class);
        }

        if (dialog.getBoolean(DISALLOW_GEOMETRYCOLLECTIONS)) {
            disallowedGeometryClasses.add(GeometryCollection.class);
        }

        validator.setDisallowedGeometryClasses(disallowedGeometryClasses);

        return validator;
    }

    private void validate(final Layer layer, final Validator validator,
        PlugInContext context, TaskMonitor monitor) {
        List validationErrors = validator.validate(layer.getFeatureCollectionWrapper()
                                                        .getFeatures(), monitor);

        if (!validationErrors.isEmpty()) {
            addLayer(toLayer(I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.error-locations")+" - " + layer.getName(),
                    toLocationFeatures(validationErrors, layer), layer, true,
                    context), context);
            addLayer(toLayer(I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.bad-features")+" - " + layer.getName(),
                    toFeatures(validationErrors, layer), layer, false, context),
                context);
        }

        outputSummary(context, layer, validationErrors);
    }

    private void outputSummary(PlugInContext context, Layer layer,
        List validationErrors) {
        context.getOutputFrame().addHeader(2, I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.layer")+" " + layer.getName());

        if (validationErrors.isEmpty()) {
            context.getOutputFrame().addText(I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.no-validation-errors"));

            return;
        }

        CollectionMap descriptionToErrorMap = new CollectionMap();

        for (Iterator i = validationErrors.iterator(); i.hasNext();) {
            ValidationError error = (ValidationError) i.next();
            descriptionToErrorMap.addItem(error.getMessage(), error);
        }

        for (Iterator i = descriptionToErrorMap.keySet().iterator();
                i.hasNext();) {
            String message = (String) i.next();
            context.getOutputFrame().addField(message + ":",
                descriptionToErrorMap.getItems(message).size() + "");
        }
    }

    private List toFeatures(List validationErrors, Layer sourceLayer) {
        ArrayList features = new ArrayList();

        for (Iterator i = validationErrors.iterator(); i.hasNext();) {
            ValidationError error = (ValidationError) i.next();
            features.add(toFeature(error, sourceLayer,
                    (Geometry) error.getFeature().getGeometry().clone()));
        }

        return features;
    }

    private List toLocationFeatures(List validationErrors, Layer sourceLayer) {
        ArrayList features = new ArrayList();

        for (Iterator i = validationErrors.iterator(); i.hasNext();) {
            ValidationError error = (ValidationError) i.next();
            Geometry geometry = geometryFactory.createPoint(error.getLocation());
            features.add(toFeature(error, sourceLayer, geometry));
        }

        return features;
    }

    private Feature toFeature(ValidationError error, Layer sourceLayer,
        Geometry geometry) {
        Feature ringFeature = new BasicFeature(schema);
        ringFeature.setAttribute(SOURCE_FID,
            new Integer(error.getFeature().getID()));
        ringFeature.setAttribute(ERROR, error.getMessage());
        ringFeature.setGeometry(geometry);

        return ringFeature;
    }

    private void addLayer(Layer errorLayer, PlugInContext context) {
        context.getLayerManager().addLayer(StandardCategoryNames.QA, errorLayer);
    }

    private Layer toLayer(String name, List features, Layer sourceLayer,
        boolean ringVertices, PlugInContext context) {
        boolean firingEvents = context.getLayerManager().isFiringEvents();
        context.getLayerManager().setFiringEvents(false);

        try {
            FeatureDataset errorFeatureCollection = new FeatureDataset(features,
                    schema);
            Layer errorLayer = new Layer(name, GOLD, errorFeatureCollection,
                    context.getLayerManager());

            if (ringVertices) {
            	errorLayer.getBasicStyle().setEnabled(false);
                changeVertexToRing(errorLayer);
            }

            showVertices(errorLayer);

            return errorLayer;
        } finally {
            context.getLayerManager().setFiringEvents(firingEvents);
        }
    }

    private void changeVertexToRing(Layer errorLayer) {
        boolean firingEvents = errorLayer.getLayerManager().isFiringEvents();
        errorLayer.getLayerManager().setFiringEvents(false);

        try {
            //Many parties assume that a layer always has a VertexStyle. Therefore,
            //disable events while we make the switch. [Jon Aquino]
            errorLayer.removeStyle(errorLayer.getStyle(VertexStyle.class));
            errorLayer.addStyle(new RingVertexStyle());
            errorLayer.getBasicStyle().setLineWidth(5);
        } finally {
            errorLayer.getLayerManager().setFiringEvents(firingEvents);
        }

        errorLayer.fireAppearanceChanged();
    }

    private void showVertices(Layer errorLayer) {
        errorLayer.getVertexStyle().setEnabled(true);
        errorLayer.fireAppearanceChanged();
    }

    private void initDialog(PlugInContext context) {
    	
    	CHECK_BASIC_TOPOLOGY = I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-basic-topology");
        dialog = new DualPaneInputDialog(context.getWorkbenchFrame(),
        		I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.validate-selected-layers"), true);
        dialog.setSideBarImage(IconLoader.icon("Validate.gif"));
        dialog.setSideBarDescription(I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.tests-layers-against-various-criteria"));
        dialog.addLabel("<HTML><STRONG>"+I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.geometry-metrics-validation")+"</STRONG></HTML>");
        dialog.addSeparator();
        dialog.addCheckBox(CHECK_BASIC_TOPOLOGY, true, "Test");
        dialog.addCheckBox(CHECK_NO_REPEATED_CONSECUTIVE_POINTS,
            false);
        dialog.addCheckBox(CHECK_POLYGON_ORIENTATION,
            false, I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-that-polygon-shells-are-oriented-clockwise-and-holes-counterclockwise"));
        dialog.addCheckBox(CHECK_MIN_SEGMENT_LENGTH, false);
        dialog.addPositiveDoubleField(MIN_SEGMENT_LENGTH, 0.001,
            5);
        dialog.addCheckBox(CHECK_MIN_ANGLE, false);
        dialog.addPositiveDoubleField(MIN_ANGLE, 1, 5);
        dialog.addCheckBox(CHECK_MIN_POLYGON_AREA, false);
        dialog.addPositiveDoubleField(MIN_POLYGON_AREA, 0.001,
            5);
        dialog.addCheckBox(CHECK_LINESTRINGS_SIMPLE, false,
        		I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.check-that-linestrings-are-simple"));
        //dialog.startNewColumn();
        dialog.setRightPane();
        dialog.addLabel("<HTML><STRONG>"+I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.geometry-types-validation")+"</STRONG></HTML>");
        dialog.addSeparator();
        dialog.addCheckBox(DISALLOW_POINTS, false);
        dialog.addCheckBox(DISALLOW_LINESTRINGS, false);
        dialog.addCheckBox(DISALLOW_POLYGONS, false);
        dialog.addCheckBox(DISALLOW_MULTIPOINTS, false);
        dialog.addCheckBox(DISALLOW_MULTILINESTRINGS,
            false);
        dialog.addCheckBox(DISALLOW_MULTIPOLYGONS, false);
        dialog.addCheckBox(CHECK_POLYGONS_HAVE_NO_HOLES,
            false);
        dialog.addCheckBox(DISALLOW_GEOMETRYCOLLECTIONS,
            false, I18N.get("ui.plugin.ValidateSelectedLayersPlugIn.geometry-collection-subtypes-are-not-disallowed"));
    }
}
