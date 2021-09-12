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

package org.openjump.core.ui.plugin.tools;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.operation.overlayng.UnaryUnionNG;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * UnionByAttribute plugin is used to union features in a Layer or to Dissolve
 * features using an attribute. It can optionnaly merge unioned LineStrings
 * (union just create MultiLineStrings by default).
 * <br>
 * There are three options available :
 * <ul>
 * <li>Features with empty values can be discarded</li>
 * <li>Attribute values can be added up (numeric) or concatened (strings)</li>
 * </ul>
 */
public class UnionByAttributePlugIn extends AbstractThreadedUiPlugIn {
    
	  private final static String LAYER             = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.layer");
	  private final static String SELECTION         = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.selection");
	  private final static String SELECTION_HELP    = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.selection-help");
    
    private final static String USE_ATTRIBUTE     = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.use-attribute");
    private final static String ATTRIBUTE         = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.attribute");
    private final static String IGNORE_EMPTY      = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.ignore-empty");
    private final static String MERGE_LINESTRINGS = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.merge-linestrings");
    private final static String AGG_UNUSED_FIELDS = I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.aggregate-unused-fields");

    private final String FLOATING_PRECISION_MODEL = I18N.JUMP.get("jts.use-floating-point-precision-model");
    private final String FLOATING_PRECISION_MODEL_TT = I18N.JUMP.get("jts.use-floating-point-precision-model-tt");
    private final String FIXED_PRECISION_MODEL    = I18N.JUMP.get("jts.use-fixed-precision-model");
    private final String FIXED_PRECISION_MODEL_TT = I18N.JUMP.get("jts.use-fixed-precision-model-tt");
    private final String PRECISION                = I18N.JUMP.get("jts.fixed-precision");
    private final String PRECISION_TT             = I18N.JUMP.get("jts.fixed-precision-tt");

    private Layer layer;
    private boolean use_selection           = false;
    private boolean use_attribute           = false;
    private String attribute;
    private boolean ignore_empty            = false;
    private boolean merge_linestrings       = true;
    private boolean aggregate_unused_fields = false;

    private boolean floatingPrecision = true;
    private boolean fixedPrecision = false;
    private double precision = 0.001;
    
    private GeometryFactory factory;
    
    public UnionByAttributePlugIn() {
    }
    
    public String getName() {
        return I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn");
    }
    
    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
                getName() + "...", false, IconLoader.icon("union_layer_icon.gif"),
                createEnableCheck(context.getWorkbenchContext()), -1);
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    
    @Override
    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
            context.getWorkbenchFrame(), getName(), true);
        int n = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
	      use_selection = (n > 0);
        initDialog(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }
    
    private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
        
        dialog.addSubTitle(
            I18N.JUMP.get("ui.plugin.analysis.UnionByAttributePlugIn.processed-data"));
        dialog.addLabel(SELECTION);
        dialog.addLabel(SELECTION_HELP);
        final JComboBox<Layer> layerComboBox =
            dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
        final JCheckBox useAttributeCheckBox =
            dialog.addCheckBox(USE_ATTRIBUTE, false, "");
        dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NO_GEOMETRY_FILTER, null);
        dialog.addCheckBox(IGNORE_EMPTY, true);
        
        dialog.addSeparator();

        JRadioButton floatingPrecisionRB = dialog
            .addRadioButton(FLOATING_PRECISION_MODEL,"MODEL", floatingPrecision, FLOATING_PRECISION_MODEL_TT);
        JRadioButton fixedPrecisionRB = dialog
            .addRadioButton(FIXED_PRECISION_MODEL,"MODEL", fixedPrecision, FIXED_PRECISION_MODEL_TT);
        dialog.addDoubleField(PRECISION,precision, 12, PRECISION_TT);
        floatingPrecisionRB.addActionListener(e -> updateControls(dialog));
        fixedPrecisionRB.addActionListener(e -> updateControls(dialog));

        dialog.addSeparator();
        
        final JCheckBox mergeLineStringsCheckBox = dialog.addCheckBox(MERGE_LINESTRINGS, merge_linestrings, 
            I18N.JUMP.get("ui.plugin.analysis.UnionByAttributePlugIn.merge-linestrings-tooltip"));
        dialog.addCheckBox(AGG_UNUSED_FIELDS, aggregate_unused_fields,
            I18N.JUMP.get("ui.plugin.analysis.UnionByAttributePlugIn.aggregation-tooltip"));
        
        updateControls(dialog);
        
        useAttributeCheckBox.addActionListener(e -> updateControls(dialog));
        layerComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (ActionListener listener : layerComboBox.getActionListeners()) {
                    // execute other ActionListener methods before this one
                    if (listener != this) listener.actionPerformed(e);
                }
                updateControls(dialog);
            }
        });
        mergeLineStringsCheckBox.addActionListener(e -> updateControls(dialog));
    }
    
    private void getDialogValues(MultiInputDialog dialog) {
		    layer = dialog.getLayer(LAYER);
		    use_attribute = dialog.getBoolean(USE_ATTRIBUTE);
        attribute = dialog.getText(ATTRIBUTE);
        ignore_empty = dialog.getBoolean(IGNORE_EMPTY) && use_attribute;
        merge_linestrings = dialog.getBoolean(MERGE_LINESTRINGS);
        aggregate_unused_fields = dialog.getBoolean(AGG_UNUSED_FIELDS);
        floatingPrecision = dialog.getBoolean(FLOATING_PRECISION_MODEL);
        fixedPrecision = dialog.getBoolean(FIXED_PRECISION_MODEL);
        precision = dialog.getDouble(PRECISION);
    }
    
    private void updateControls(MultiInputDialog dialog) {
        getDialogValues(dialog);
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        boolean has_attributes = 
            !AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(schema).isEmpty();
        int other_fields =
            AttributeTypeFilter.NUMSTRING_FILTER.filter(schema).size();
        if (use_attribute) other_fields--; 
        
        dialog.setFieldVisible(SELECTION, use_selection);
        dialog.setFieldVisible(SELECTION_HELP, use_selection);
        dialog.setFieldVisible(LAYER, !use_selection);
        dialog.setFieldEnabled(USE_ATTRIBUTE, has_attributes && !use_selection);
	      dialog.setFieldEnabled(ATTRIBUTE, has_attributes && !use_selection);
	      dialog.setFieldEnabled(IGNORE_EMPTY, has_attributes && !use_selection);
	      dialog.setFieldEnabled(AGG_UNUSED_FIELDS, has_attributes && !use_selection);
	    
        dialog.setFieldEnabled(USE_ATTRIBUTE, has_attributes);
        dialog.setFieldEnabled(ATTRIBUTE, has_attributes && use_attribute);
        dialog.setFieldEnabled(IGNORE_EMPTY, has_attributes && use_attribute);
        dialog.setFieldEnabled(AGG_UNUSED_FIELDS, other_fields>0);
        
        if (use_selection) {
            dialog.setSideBarDescription(
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.union-selection-description"));
            if (merge_linestrings) dialog.setSideBarImage(IconLoader.icon("union_selection_merge.png"));
            else dialog.setSideBarImage(IconLoader.icon("union_selection_no_merge.png"));
        }
        else if (!use_attribute || !has_attributes) {
            dialog.setSideBarDescription(I18N.getInstance().get(
                "ui.plugin.analysis.UnionByAttributePlugIn.union-layer-description",
                layer.getName()));
            if (merge_linestrings) dialog.setSideBarImage(IconLoader.icon("union_layer_merge.png"));
            else dialog.setSideBarImage(IconLoader.icon("union_layer_no_merge.png"));
        }
        else {
            dialog.setSideBarDescription(I18N.getInstance().get(
                "ui.plugin.analysis.UnionByAttributePlugIn.union-layer-by-attribute-description",
                layer.getName(), attribute));
            if (merge_linestrings) dialog.setSideBarImage(IconLoader.icon("dissolve_layer_merge.png"));
            else dialog.setSideBarImage(IconLoader.icon("dissolve_layer_no_merge.png"));
        }

        dialog.setFieldEnabled(PRECISION, dialog.getBoolean(FIXED_PRECISION_MODEL));
        dialog.pack();
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        
        monitor.allowCancellationRequests();
        
        Collection<Feature> inputC;
        FeatureSchema schema;
        if (use_selection) {
        	inputC = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
        	Feature feature = inputC.iterator().next();
        	schema = feature.getSchema();
        	inputC = PasteItemsPlugIn.conform(inputC, schema);
        } else {
        	inputC = layer.getFeatureCollectionWrapper().getFeatures();
        	schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        }
        FeatureDataset inputFC = new FeatureDataset(inputC, schema);
        
        if (inputFC.getFeatures().size() > 1 &&
            inputFC.getFeatures().get(0).getGeometry() != null) {
            factory = inputFC.getFeatures().get(0).getGeometry().getFactory();
            writeReport(context);
        }
        else {
            context.getWorkbenchFrame().warnUser(
                I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.needs-two-features-or-more"));
            return;
        }
        
        // Create the schema for the output dataset
        FeatureSchema newSchema = new FeatureSchema();
        //fix bug on 2007-09-17 : must take the geometry name of the source layer
        //newSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        newSchema.addAttribute(schema.getAttributeName(schema.getGeometryIndex()), AttributeType.GEOMETRY);
        if (use_attribute && !attribute.equals(MultiInputDialog.NO_VALID_ATTRIBUTE)) {
            newSchema.addAttribute(attribute, schema.getAttributeType(attribute));
        }
        // if total_numeric_fields is true, add numeric fields to the result layer
        if (aggregate_unused_fields) {
            for (int i = 0, max = schema.getAttributeCount() ; i < max ; i++) {
                if (schema.getAttributeType(i) == AttributeType.INTEGER ||
                    schema.getAttributeType(i) == AttributeType.DOUBLE ||
                    schema.getAttributeType(i) == AttributeType.STRING) {
                    if (use_attribute && 
                        !attribute.equals(MultiInputDialog.NO_VALID_ATTRIBUTE) &&
                        attribute.equals(schema.getAttributeName(i))) continue;
                    newSchema.addAttribute(schema.getAttributeName(i),
                                           schema.getAttributeType(i));
                }
            }
        }
        
        // Order features by attribute value in a map
        Map<Object,FeatureCollection> map = new HashMap<>();
        monitor.report(I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn"));
        for (Feature f : inputFC.getFeatures()) {
            Object key = use_attribute ? f.getAttribute(attribute) : null;
            if (ignore_empty && (key == null || key.toString().trim().length() == 0)) {
                continue;
            }
            else if (!map.containsKey(key)) {
                FeatureCollection fd = new FeatureDataset(inputFC.getFeatureSchema());
                fd.add(f);
                map.put(key, fd);
            }
            else {
                map.get(key).add(f);
            }
        }
        
        // Computing the result
        int count = 1;
        FeatureCollection resultfc = new FeatureDataset(newSchema);
        for (Object key : map.keySet()) {
            monitor.report(I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.computing-union") + " (" + count++ + "/" + map.size() + ")");
            FeatureCollection fca = map.get(key);
            if (fca.size() > 0) {
                Feature feature = union(context, monitor, fca);
                if (use_attribute) feature.setAttribute(attribute, key);
                Feature newFeature = new BasicFeature(newSchema);
                // Copy feature attributes in newFeature
                for (int j = 0, max = newSchema.getAttributeCount() ; j < max ; j++) {
                    newFeature.setAttribute(j, feature.getAttribute(newSchema.getAttributeName(j)));
                }
                resultfc.add(newFeature);
            }
        }
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        String newLayerName = layer.getName() +
            (use_attribute ? ("-" + attribute + " (dissolve)") : " (union)");
        context.addLayer(StandardCategoryNames.RESULT, newLayerName, resultfc);
        context.getOutputFrame().append("<h3>"+
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.end-of-process") + " " +
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn") + "</h3>");
    }
    
   /**
    * New method for union. Uses new UnaryUnionOp which is much more
    * efficient than Geometry.union() for large datasets.
    */
    private Feature union(PlugInContext context, TaskMonitor monitor, FeatureCollection fc) {
        // Eliminate invalid geomeries and log their fid
        Collection<Geometry> geometries  = new ArrayList<>();
        for (Feature f : fc.getFeatures()) {
            Geometry g = f.getGeometry();
            if (g.isValid()) {
                geometries.add(g);
            }
            else {
                context.getWorkbenchFrame().warnUser(
                    I18N.JUMP.get("ui.plugin.analysis.UnionByAttributePlugIn.invalid-geometry-excluded"));
                context.getOutputFrame().addText(
                    I18N.JUMP.get("ui.plugin.analysis.UnionByAttributePlugIn.exclusion", f.getID()));
            }
        }
        Geometry unioned;
        //unioned = UnaryUnionOp.union(geometries); // old algorithm (not as robust)
        if (floatingPrecision) {
            unioned = OverlayNGRobust.union(geometries);
        } else {
            unioned = UnaryUnionNG.union(geometries, new PrecisionModel(precision));
        }
        // Post process linestring if merged is wanted
        if (merge_linestrings) {
            geometries.clear();
            List<Geometry> points      = new ArrayList<>();
            List<Geometry> lineStrings = new ArrayList<>();
            List<Geometry> polygons    = new ArrayList<>();
            decompose(unioned, points, lineStrings, polygons);
            LineMerger merger = new LineMerger();
            merger.add(lineStrings);
            geometries.addAll(points);
            geometries.addAll(merger.getMergedLineStrings());
            geometries.addAll(polygons);
            unioned = unioned.getFactory().buildGeometry(geometries);
        }
        FeatureSchema schema = fc.getFeatureSchema();
        Feature feature = new BasicFeature(schema);
        if (geometries.size()==0) {
            feature.setGeometry(factory.createGeometryCollection(new Geometry[]{}));
        }
        else {
            feature.setGeometry(unioned);
        }
        if (aggregate_unused_fields) {
            feature = aggregateValues(context, fc, feature);
        }
        return feature;
    }

    private void decompose(Geometry geometry, List<Geometry> dim0, List<Geometry> dim1, List<Geometry> dim2) {
        if (geometry instanceof GeometryCollection) {
            for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
                decompose(geometry.getGeometryN(i), dim0, dim1, dim2);
            }
        }
        else if (geometry.getDimension() == 2) dim2.add(geometry);
        else if (geometry.getDimension() == 1) dim1.add(geometry);
        else if (geometry.getDimension() == 0) dim0.add(geometry);
        else {
            assert false : "Should never reach here";
        }
    }
    
    private Feature aggregateValues(PlugInContext context, FeatureCollection fc, Feature feature) {
        FeatureSchema schema = fc.getFeatureSchema();
        for (int i = 0, max = schema.getAttributeCount() ; i < max ; i++) {
            if (schema.getAttributeType(i) == AttributeType.INTEGER) {
                int total = 0;
                for (Feature f : fc.getFeatures()) {
                    Object val = f.getAttribute(i);
                    if (val != null) total += (Integer) val;
                }
                feature.setAttribute(i, total);
            }
            if (schema.getAttributeType(i) == AttributeType.LONG) {
                long total = 0;
                for (Feature f : fc.getFeatures()) {
                    Object val = f.getAttribute(i);
                    if (val != null) total += (Long) val;
                }
                feature.setAttribute(i, total);
            }
            else if (schema.getAttributeType(i) == AttributeType.DOUBLE) {
                double total = 0;
                for (Feature f : fc.getFeatures()) {
                    Object val = f.getAttribute(i);
                    if (val != null) total += (Double) val;
                }
                feature.setAttribute(i, total);
            }
            else if (schema.getAttributeType(i) == AttributeType.STRING) {
                Set<String> set = new TreeSet<>();
                for (Feature f : fc.getFeatures()) {
                    String val = f.getString(i);
                    if (val != null) set.add(val);
                }
                feature.setAttribute(i, java.util.Arrays.toString(set.toArray()));
            }
            else if (schema.getAttributeType(i) != AttributeType.GEOMETRY) {
                context.getOutputFrame().addText(
                    I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.cannot-be-aggregated",
                        schema.getAttributeName(i), schema.getAttributeType(i)));
            }
        }
        return feature;
    }
    
    private void writeReport(PlugInContext context) {
        context.getOutputFrame().createNewDocument();
        context.getOutputFrame().append(
            "<h1>" + I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn") + "</h1>");
        context.getOutputFrame().addText(
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.processed-data") + " : " +
            (use_selection ? I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.selection") : layer.getName()));
        context.getOutputFrame().addText(
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.use-attribute") + " : " +
            (use_attribute ? attribute : I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.no-attribute-used")));
        context.getOutputFrame().addText(
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.empty-values") + " : " +
            (ignore_empty ? I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.ignore") : 
                            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.process")));
        context.getOutputFrame().addText(
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.merge-linestrings") + " : " +
            (merge_linestrings ? I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.yes") : 
                                 I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.no")));
        context.getOutputFrame().addText(
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.aggregate-unused-fields") + " : " +
            (aggregate_unused_fields ? I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.yes") : 
                                 I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.no")));
        context.getOutputFrame().append("<h3>" + 
            I18N.getInstance().get("ui.plugin.analysis.UnionByAttributePlugIn.warnings") + "</h3>");
    }
    
}
