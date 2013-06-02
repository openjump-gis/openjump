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

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
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
    
	private final static String LAYER             = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.layer");
	private final static String SELECTION         = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.selection");
	private final static String SELECTION_HELP    = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.selection-help");
    
    private final static String USE_ATTRIBUTE     = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.use-attribute");
    private final static String ATTRIBUTE         = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.attribute");
    private final static String IGNORE_EMPTY      = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.ignore-empty");
    private final static String MERGE_LINESTRINGS = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.merge-linestrings");
    private final static String AGG_UNUSED_FIELDS = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.aggregate-unused-fields");
    
    private Layer layer;
    private boolean use_selection           = false;
    private boolean use_attribute           = false;
    private String attribute;
    private boolean ignore_empty            = false;
    private boolean merge_linestrings       = true;
    private boolean aggregate_unused_fields = false;
    
    private GeometryFactory factory;
    
    public UnionByAttributePlugIn() {
    }
    
    public String getName() {
        return I18N.get("ui.plugin.analysis.UnionByAttributePlugIn");
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
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
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
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }
    
    private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
        
        //dialog.setSideBarImage(IconLoader.icon("union_layer.png"));
        //dialog.setSideBarDescription("");
        
        final JLabel processedDataLabel = dialog.addSubTitle(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.processed-data"));
        final JLabel selectionLabel = dialog.addLabel(SELECTION);
        final JLabel selectionHelpLabel = dialog.addLabel(SELECTION_HELP);
        final JComboBox layerComboBox = dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
        final JCheckBox useAttributeCheckBox = dialog.addCheckBox(USE_ATTRIBUTE, false, "");
        final JComboBox attributeComboBox = dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NO_GEOMETRY_FILTER, null);
        final JCheckBox ignoreEmptyCheckBox = dialog.addCheckBox(IGNORE_EMPTY, true);
        
        dialog.addSeparator();
        
        final JCheckBox mergeLineStringsCheckBox = dialog.addCheckBox(MERGE_LINESTRINGS, merge_linestrings, 
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.merge-linestrings-tooltip"));
        final JCheckBox aggUnusedFieldsCheckBox = dialog.addCheckBox(AGG_UNUSED_FIELDS, aggregate_unused_fields,
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.aggregation-tooltip"));
        
        updateControls(dialog);
        
        useAttributeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        layerComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (ActionListener listener : layerComboBox.getActionListeners()) {
                    // execute other ActionListener methods before this one
                    if (listener != this) listener.actionPerformed(e);
                }
                updateControls(dialog);
            }
        });
        mergeLineStringsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        
        // Give the dialog box a minimum height of 250, so that the description
        // of the infoPanel is always visible.
        dialog.setPreferredSize(new java.awt.Dimension(
            (int)dialog.getPreferredSize().getWidth(), 280));
        GUIUtil.centreOnWindow(dialog);
    }
    
    private void getDialogValues(MultiInputDialog dialog) {
		layer = dialog.getLayer(LAYER);
		use_attribute = dialog.getBoolean(USE_ATTRIBUTE);
        attribute = dialog.getText(ATTRIBUTE);
        ignore_empty = dialog.getBoolean(IGNORE_EMPTY) && use_attribute;
        merge_linestrings = dialog.getBoolean(MERGE_LINESTRINGS);
        aggregate_unused_fields = dialog.getBoolean(AGG_UNUSED_FIELDS);
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
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.union-selection-description"));
            if (merge_linestrings) dialog.setSideBarImage(IconLoader.icon("union_selection_merge.png"));
            else dialog.setSideBarImage(IconLoader.icon("union_selection_no_merge.png"));
        }
        else if (!use_attribute || !has_attributes || use_selection) {
            dialog.setSideBarDescription(I18N.getMessage(
                "ui.plugin.analysis.UnionByAttributePlugIn.union-layer-description", 
                new Object[]{layer.getName()}
            ));
            if (merge_linestrings) dialog.setSideBarImage(IconLoader.icon("union_layer_merge.png"));
            else dialog.setSideBarImage(IconLoader.icon("union_layer_no_merge.png"));
        }
        else {
            dialog.setSideBarDescription(I18N.getMessage(
                "ui.plugin.analysis.UnionByAttributePlugIn.union-layer-by-attribute-description",
                new Object[]{layer.getName(), attribute}
            ));
            if (merge_linestrings) dialog.setSideBarImage(IconLoader.icon("dissolve_layer_merge.png"));
            else dialog.setSideBarImage(IconLoader.icon("dissolve_layer_no_merge.png"));
        }
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        
        monitor.allowCancellationRequests();
        
        Collection inputC = null;
        FeatureSchema schema = null;
        if (use_selection) {
        	inputC = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
        	Feature feature = (Feature) inputC.iterator().next();
        	schema = feature.getSchema();
        	inputC = PasteItemsPlugIn.conform(inputC, schema);
        } else {
        	inputC = layer.getFeatureCollectionWrapper().getFeatures();
        	schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        }
        FeatureDataset inputFC = new FeatureDataset(inputC, schema);
        
        if (inputFC.getFeatures().size() > 1 &&
            ((Feature)inputFC.getFeatures().get(0)).getGeometry() != null) {
            factory = ((Feature)inputFC.getFeatures().get(0)).getGeometry().getFactory();
            writeReport(context);
        }
        else {
            context.getWorkbenchFrame().warnUser(
                I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.needs-two-features-or-more"));
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
        Map map = new HashMap();
        monitor.report(I18N.get("ui.plugin.analysis.UnionByAttributePlugIn"));
        for (Iterator i = inputFC.iterator() ; i.hasNext() ; ) {
            Feature f = (Feature)i.next();
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
                ((FeatureCollection)map.get(key)).add(f);
            }
        }
        
        // Computing the result
        int count = 1;
        FeatureCollection resultfc = new FeatureDataset(newSchema);
        for (Iterator i = map.keySet().iterator() ; i.hasNext() ; ) {
            monitor.report(I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.computing-union") + " (" + count++ + "/" + map.size() + ")");
            Object key = i.next();
            FeatureCollection fca = (FeatureCollection)map.get(key);
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
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.end-of-process") + " " +
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn") + "</h3>");
    }
    
   /**
    * New method for union. Uses new UnaryUnionOp which is much more
    * efficient than Geometry.union() for large datasets.
    */
    private Feature union(PlugInContext context, TaskMonitor monitor, FeatureCollection fc) {
        // Eliminate invalid geomeries and log their fid
        Collection geometries  = new ArrayList();
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature) it.next();
            Geometry g = f.getGeometry();
            if (!g.isValid()) {
                context.getWorkbenchFrame().warnUser(
                    I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.invalid-geometry-excluded"));
                context.getOutputFrame().addText(
                    I18N.getMessage("ui.plugin.analysis.UnionByAttributePlugIn.exclusion", new Object[]{f.getID()}));
                continue;
            }
            else geometries.add(g);
        }
        Geometry unioned = UnaryUnionOp.union(geometries);
        // Post process linestring if merged is wanted
        if (merge_linestrings) {
            geometries.clear();
            List points      = new ArrayList();
            List lineStrings = new ArrayList();
            List polygons    = new ArrayList();
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

    private void decompose(Geometry geometry, List dim0, List dim1, List dim2) {
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
        };
    }
    
    private Feature aggregateValues(PlugInContext context, FeatureCollection fc, Feature feature) {
        FeatureSchema schema = fc.getFeatureSchema();
        for (int i = 0, max = schema.getAttributeCount() ; i < max ; i++) {
            if (schema.getAttributeType(i) == AttributeType.INTEGER) {
                int total = 0;
                for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
                    Object val = ((Feature)it.next()).getAttribute(i);
                    if (val != null) total += ((Integer)val).intValue();
                }
                feature.setAttribute(i, new Integer(total));
            }
            else if (schema.getAttributeType(i) == AttributeType.DOUBLE) {
                double total = 0;
                for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
                    Object val = ((Feature)it.next()).getAttribute(i);
                    if (val != null) total += ((Double)val).doubleValue();
                }
                feature.setAttribute(i, new Double(total));
            }
            else if (schema.getAttributeType(i) == AttributeType.STRING) {
                java.util.Set set = new java.util.TreeSet();
                for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
                    Object val = ((Feature)it.next()).getAttribute(i);
                    if (val != null) set.add(val);
                }
                feature.setAttribute(i, java.util.Arrays.toString(set.toArray()));
            }
            else if (schema.getAttributeType(i) != AttributeType.GEOMETRY) {
                context.getOutputFrame().addText(
                    I18N.getMessage("ui.plugin.analysis.UnionByAttributePlugIn.cannot-be-aggregated", 
                    new Object[]{schema.getAttributeName(i), schema.getAttributeType(i)}));
            }
        }
        return feature;
    }
    
    private void writeReport(PlugInContext context) {
        context.getOutputFrame().createNewDocument();
        context.getOutputFrame().append(
            "<h1>" + I18N.get("ui.plugin.analysis.UnionByAttributePlugIn") + "</h1>");
        context.getOutputFrame().addText(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.processed-data") + " : " +
            (use_selection ? I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.selection") : layer.getName()));
        context.getOutputFrame().addText(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.use-attribute") + " : " +
            (use_attribute ? attribute : I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.no-attribute-used")));
        context.getOutputFrame().addText(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.empty-values") + " : " +
            (ignore_empty ? I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.ignore") : 
                            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.process")));
        context.getOutputFrame().addText(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.merge-linestrings") + " : " +
            (merge_linestrings ? I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.yes") : 
                                 I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.no")));
        context.getOutputFrame().addText(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.aggregate-unused-fields") + " : " +
            (aggregate_unused_fields ? I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.yes") : 
                                 I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.no")));
        context.getOutputFrame().append("<h3>" + 
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.warnings") + "</h3>");
    }
    
}
