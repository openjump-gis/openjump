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
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
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

    /*
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
            this, "Tools", "Find Unaligned Segments...", null, new MultiEnableCheck()
            .add(context.getCheckFactory().createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
    }
    */
    
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
        newSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
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
                Feature feature = union(monitor, fca, merge_lines, total_numeric_fields);
                feature.setAttribute(att, key);
                Feature newFeature = new BasicFeature(newSchema);
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
    private Feature union(TaskMonitor monitor, FeatureCollection fc,
                                               boolean merge_lines, boolean total) {
        List[] geometries = getGeometries(fc.iterator());
        List polygons   = geometries[2];
        List lines      = geometries[1];
        List points     = geometries[0];
        
        Geometry pointsUnion      = fact.buildGeometry(new ArrayList());
        Geometry lineStringsUnion = fact.buildGeometry(new ArrayList());
        Geometry polygonsUnion    = fact.buildGeometry(new ArrayList());
        
        // Union Points
        if (points.size() > 0) {
            pointsUnion = fact.createMultiPoint((Point[])points.toArray(new Point[0]));
        }
        
        // Union LineString
        if (lines.size() > 0) {
            Geometry multiLineGeom = fact.createMultiLineString(fact.toLineStringArray(lines));
            Geometry unionInput    = fact.createMultiLineString(null);
            Geometry minLine       = extractPoint(lines);
            if (minLine != null) unionInput = minLine;
            lineStringsUnion = multiLineGeom.union(unionInput);
            if (merge_lines) {
                lineStringsUnion = mergeLines(lineStringsUnion);
            }
        }
        
        // Union Polygons
        if (polygons.size() > 0) {
            int iteration = 1;
            int nbIteration = 1 + (int)(Math.log(polygons.size())/Math.log(4));
            while (polygons.size() > 1) {
                monitor.report(iteration++, nbIteration, I18N.get("ui.plugin.analysis.UnionByAttributePlugIn.union-by-attribute"));
                final int cellSize = 1 + (int)Math.sqrt(polygons.size());
                java.util.Comparator comparator =  new java.util.Comparator(){
                    public int compare(Object o1, Object o2) {
                        if (o1==null || o2==null) return 0;
                        Envelope env1 = ((Geometry)o1).getEnvelopeInternal();
                        Envelope env2 = ((Geometry)o2).getEnvelopeInternal();
                        double indice1 = env1.getMinX()/cellSize + cellSize*((int)env1.getMinY()/cellSize);
                        double indice2 = env2.getMinX()/cellSize + cellSize*((int)env2.getMinY()/cellSize);
                        // Bug fixed on 2007-06-27 : must never return 0
                        return indice1>=indice2?1:indice1<indice2?-1:0;
                    }
                    public boolean equals(Object obj) {return this.equals(obj);}
                };
                java.util.TreeSet treeSet = new java.util.TreeSet(comparator);
                treeSet.addAll(polygons);
                // Testes with groups of 4, 8 and 16 (4 is better than 8 which is better than 16
                // for large datasets).
                polygons = union(monitor, treeSet, 4);
            }
        }
        if (polygons.size() > 0) polygonsUnion = (Geometry)polygons.get(0);
        Geometry union;
        if (polygonsUnion.isEmpty()) {
            if (lineStringsUnion.isEmpty()) {
                if (pointsUnion.isEmpty()) union = fact.buildGeometry(new ArrayList());
                else union = pointsUnion;
            }
            else if (pointsUnion.isEmpty()) union = lineStringsUnion;
            else union = lineStringsUnion.union(pointsUnion);
        }
        else if (lineStringsUnion.isEmpty()) {
            if (pointsUnion.isEmpty()) union = polygonsUnion;
            else union = polygonsUnion.union(pointsUnion);
        }
        else {
            if (pointsUnion.isEmpty()) union = polygonsUnion.union(lineStringsUnion);
            // Can't union poly + linestring + points, because the intermediate result
            // is GeometryCollection which is non unionable
            //else union = polygonsUnion.union(lineStringsUnion).union(pointsUnion);
            else union = polygonsUnion.union(lineStringsUnion);
        }
        
        FeatureSchema schema = fc.getFeatureSchema();
        Feature feature = new BasicFeature(schema);
        feature.setGeometry(union);
        if (total) {
            feature = totalNumericValues(fc, feature);
        }
        return feature;
    }
    
   /**
    * Method unioning an ordered set of geometries by small groups.
    */
    private List union(TaskMonitor monitor, Set set, int groupSize) {
        List unionGeometryList = new ArrayList();
        Geometry currUnion = null;
        int size = set.size();
        int count = 0;
        for (Iterator i = set.iterator(); i.hasNext();) {
            Geometry geom = (Geometry)i.next();
            if (count%groupSize==0) currUnion = geom;
            else {
                currUnion = currUnion.union(geom);
                if (groupSize-count%groupSize==1) unionGeometryList.add(currUnion);
            }
            count++;
        }
        if (groupSize-count%groupSize!=0) {
            unionGeometryList.add(currUnion);
        }
        return unionGeometryList;
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
    
    
    private List[] getGeometries(Iterator featureIterator) {
        List[] lists = new ArrayList[]{new ArrayList(), new ArrayList(), new ArrayList()};
        for (Iterator i = featureIterator; i.hasNext();) {
            Geometry g = (Geometry)((Feature) i.next()).getGeometry();
            if (g instanceof Point) lists[0].add(g);
            else if (g instanceof LineString) lists[1].add(g);
            else if (g instanceof Polygon) lists[2].add(g);
            else if (g instanceof GeometryCollection) {
                Geometry gc = (GeometryCollection)g;
                for (int j = 0 ; j < gc.getNumGeometries() ; j++) {
                    Geometry gp = gc.getGeometryN(j);
                    if (gp instanceof Point) lists[0].add(gp);
                    else if (gp instanceof LineString) lists[1].add(gp);
                    else if (gp instanceof Polygon) lists[2].add(gp);
                    else;
                }
            }
        }
        return lists;
    }
    
    private Geometry mergeLines(Geometry g) {
        List linesList = new ArrayList();
        LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
        g.apply(lineFilter);
        LineMerger merger = new LineMerger();
        merger.add(linesList);
        return fact.buildGeometry(merger.getMergedLineStrings());
    }
    
    private Geometry extractPoint(Collection lines) {
        int minPts = Integer.MAX_VALUE;
        Geometry point = null;
        // extract first point from first non-empty geometry
        for (Iterator i = lines.iterator(); i.hasNext(); ) {
            Geometry g = (Geometry) i.next();
            if (! g.isEmpty()) {
                Coordinate p = g.getCoordinate();
                point = g.getFactory().createPoint(p);
            }
        }
        return point;
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
