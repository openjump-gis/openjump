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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * UnionByAttribute plugin is used to perform selective unions based on attribute values.
 * All the features having a same attribute value are unioned together.<br>
 * An option can be used to eliminate null or empty attribute values. Useful to merge,
 * for example, all named rivers, but not unnamed one, even if they are in the same layer.<br>
 * An other option makes it possible to total numeric fields of unioned features.<br>
 */
public class UnionByAttributePlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    
    private final static String LAYER = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.layer");
    private final static String ATTRIBUTE = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.attribute");
    private final static String IGNORE_EMPTY = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.ignore-empty");
    private final static String MERGE_LINES = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.merge-lines");
    private final static String TOTAL_NUMERIC_FIELDS = I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.total-numeric-fields");
    
    private MultiInputDialog dialog;
    
    private GeometryFactory fact;
    
    public UnionByAttributePlugIn() {}
    
    public String getName() {return I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.union-by-attribute");}
    
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
            this,
            new String[] { MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS, MenuNames.ONELAYER },
            this.getName(),
            false,
            null,
            new MultiEnableCheck()
                .add(new EnableCheckFactory(context.getWorkbenchContext())
                    .createTaskWindowMustBeActiveCheck())
                .add(new EnableCheckFactory(context.getWorkbenchContext())
                    .createAtLeastNLayersMustExistCheck(1)));
    }
    
    
    public boolean execute(PlugInContext context) throws Exception {
    
        initDialog(context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        return true;
    }
    
    private void initDialog(PlugInContext context) {
        
        dialog = new MultiInputDialog(context.getWorkbenchFrame(),
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.union-by-attribute"), true);
        
        dialog.setSideBarImage(IconLoader.icon("UnionByAttribute.gif"));
        
        dialog.setSideBarDescription(
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.creates-a-new-layer-containing-the-unions-of-features-having-a-common-attribute-value"));
        
        dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
        
        List list = getFieldsFromLayerWithoutGeometry(context.getCandidateLayer(0));
        Object val = list.size()>0?list.iterator().next():null;
        final JComboBox jcb_attribute = dialog.addComboBox(ATTRIBUTE, val, list,
            I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.select-attribute"));
        if (list.size() == 0) jcb_attribute.setEnabled(false);
        
        final JCheckBox jcb_ignore_empty = dialog.addCheckBox(IGNORE_EMPTY, true);
        if (list.size() == 0) jcb_ignore_empty.setEnabled(false);
        
        dialog.addSeparator();
        final JCheckBox jcb_merge_lines = dialog.addCheckBox(MERGE_LINES, true);
        final JCheckBox jcb_total_numeric_fields = dialog.addCheckBox(TOTAL_NUMERIC_FIELDS, true);
        
        dialog.getComboBox(LAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List list = getFieldsFromLayerWithoutGeometry();
                if (list.size() == 0) {
                    jcb_attribute.setModel(new DefaultComboBoxModel(new String[0]));
                    jcb_attribute.setEnabled(false);
                    jcb_ignore_empty.setEnabled(false);
                }
                jcb_attribute.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
            }
        });
        
        GUIUtil.centreOnWindow(dialog);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        
        monitor.allowCancellationRequests();
        
        // Get options from the dialog
        Layer layer = dialog.getLayer(LAYER);
        FeatureCollection fc = layer.getFeatureCollectionWrapper();
        FeatureSchema schema = fc.getFeatureSchema();
        String att = dialog.getText(ATTRIBUTE);
        boolean ignore_empty = dialog.getBoolean(IGNORE_EMPTY);
        boolean merge_lines = dialog.getBoolean(MERGE_LINES);
        boolean total_numeric_fields = dialog.getBoolean(TOTAL_NUMERIC_FIELDS);
        
        if (fc.getFeatures().size() > 0 &&
            ((Feature)fc.getFeatures().get(0)).getGeometry()!=null) {
            fact = ((Feature)fc.getFeatures().get(0)).getGeometry().getFactory();
        }
        else {
            context.getWorkbenchFrame().warnUser(I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.no-data-to-be-unioned"));
            return;
        }
        
        // Create the schema for the output dataset
        FeatureSchema newSchema = new FeatureSchema();
        //fix bug on 2007-09-17 : must take the geometry name of the source layer
        //newSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        newSchema.addAttribute(schema.getAttributeName(schema.getGeometryIndex()), AttributeType.GEOMETRY);
        newSchema.addAttribute(att, schema.getAttributeType(att));
        if (total_numeric_fields) {
            for (int i = 0, max = schema.getAttributeCount() ; i < max ; i++) {
                if (schema.getAttributeType(i) == AttributeType.INTEGER ||
                    schema.getAttributeType(i) == AttributeType.DOUBLE)
                    newSchema.addAttribute(schema.getAttributeName(i),
                                           schema.getAttributeType(i));
            }
        }
        
        // Order features by attribute value in a map
        Map map = new HashMap();
        monitor.report(I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.union-by-attribute"));
        //monitor.report(-1, -1, I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.sorting"));
        for (Iterator i = fc.iterator() ; i.hasNext() ; ) {
            Feature f = (Feature)i.next();
            Object key = f.getAttribute(att);
            if (ignore_empty && (key == null || key.toString().trim().length() == 0)) {
                continue;
            }
            else if (!map.containsKey(key)) {
                FeatureCollection fd = new FeatureDataset(fc.getFeatureSchema());
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
            monitor.report(I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.union-by-attribute") + " (" + count++ + "/" + map.size() + ")");
            Object key = i.next();
            FeatureCollection fca = (FeatureCollection)map.get(key);
            if (fca.size() > 0) {
                Feature feature = union(context, monitor, fca, merge_lines, total_numeric_fields);
                feature.setAttribute(att, key);
                Feature newFeature = new BasicFeature(newSchema);
                // Copy feature attributes in newFeature
                for (int j = 0, max = newSchema.getAttributeCount() ; j < max ; j++) {
                    newFeature.setAttribute(j, feature.getAttribute(newSchema.getAttributeName(j)));
                }
                resultfc.add(newFeature);
            }
        }
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        context.addLayer(StandardCategoryNames.RESULT, layer.getName() + "-" + att + " (union)", resultfc);
    }
    
   /**
    * New method for union. Instead of the naive algorithm looping over the features and
    * unioning each time, this one union small groups of features which are closed to each
    * other, then iterates over the result.
    * The difference is not so important for small datasets, but for large datasets, the
    * difference may of 5 minutes versus 5 hours.
    */
    private Feature union(PlugInContext context, TaskMonitor monitor,
                    FeatureCollection fc, boolean merge_lines, boolean total) {
        Collection points      = new ArrayList();
        Collection lineStrings = new ArrayList();
        Collection polygons    = new ArrayList();
        Collection geoms       = new ArrayList();
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Geometry g = (Geometry)((Feature) it.next()).getGeometry();
            if (g instanceof Point) points.add(g);
            else if (g instanceof LineString) lineStrings.add(g);
            else if (g instanceof Polygon) polygons.add(g);
            else if (g instanceof GeometryCollection) {
                Geometry gc = (GeometryCollection)g;
                for (int j = 0 ; j < gc.getNumGeometries() ; j++) {
                    Geometry gp = gc.getGeometryN(j);
                    if (gp instanceof Point) points.add(gp);
                    else if (gp instanceof LineString) lineStrings.add(gp);
                    else if (gp instanceof Polygon) polygons.add(gp);
                    else;
                }
            }
        }
        Geometry gp;
        if (points.size()>0 && null != (gp = UnaryUnionOp.union(points))) {
            geoms.add(gp);
        }
        if (merge_lines && lineStrings.size()>0) {
            LineMerger merger = new LineMerger();
            merger.add(lineStrings);
            geoms.addAll(merger.getMergedLineStrings());
        }
        else if (lineStrings.size()>0) {
            context.getOutputFrame().addText("merge_lines = true");
            gp = UnaryUnionOp.union(lineStrings);
            if (gp != null) geoms.add(gp);
        }
        if (polygons.size()>0 && null != (gp = UnaryUnionOp.union(polygons))) {
            geoms.add(gp);
        }
        
        FeatureSchema schema = fc.getFeatureSchema();
        Feature feature = new BasicFeature(schema);
        if (geoms.size()==0) {
            feature.setGeometry(fact.createGeometryCollection(new Geometry[]{}));
        }
        else {
            feature.setGeometry(UnaryUnionOp.union(geoms));
        }
        if (total) {
            feature = totalNumericValues(fc, feature);
        }
        return feature;
    }
    
    // Set the sum of fc collection numeric attribute values into feature numeric attributes.
    private Feature totalNumericValues(FeatureCollection fc, Feature feature) {
        FeatureSchema schema = fc.getFeatureSchema();
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            for (int i = 0, max = schema.getAttributeCount() ; i < max ; i++) {
                if (schema.getAttributeType(i) == AttributeType.INTEGER) {
                    Object val = feature.getAttribute(i);
                    int val1 = (val ==  null)? 0 : ((Integer)val).intValue();
                    val = f.getAttribute(i);
                    val1 = (val == null)? val1 : val1 + ((Integer)val).intValue();
                    feature.setAttribute(i, new Integer(val1));
                }
                else if (schema.getAttributeType(i) == AttributeType.DOUBLE) {
                    Object val = feature.getAttribute(i);
                    double val1 = (val ==  null)? 0 : ((Double)val).doubleValue();
                    val = f.getAttribute(i);
                    val1 = (val == null)? val1 : val1 + ((Double)val).doubleValue();
                    feature.setAttribute(i, new Double(val1));
                }
            }
        }
        return feature;
    }
    
    private Geometry mergeLines(Geometry g) {
        List linesList = new ArrayList();
        LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
        g.apply(lineFilter);
        LineMerger merger = new LineMerger();
        merger.add(linesList);
        return fact.buildGeometry(merger.getMergedLineStrings());
    }
    
    private List getFieldsFromLayerWithoutGeometry(Layer lyr) {
        List fields = new ArrayList();
        FeatureSchema schema = lyr.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
        	if (schema.getAttributeType(i) != AttributeType.GEOMETRY) {
        	    fields.add(schema.getAttributeName(i));  
           }
        }
        return fields;
    }
    
    private List getFieldsFromLayerWithoutGeometry() {
        return getFieldsFromLayerWithoutGeometry(dialog.getLayer(LAYER));
    }
    
}
