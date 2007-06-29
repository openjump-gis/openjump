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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComboBox;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;


public class UnionPlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    private String LAYER = I18N.get("ui.plugin.analysis.UnionPlugIn.layer");
    private String SELECTED_ONLY = I18N.get("ui.plugin.analysis.UnionPlugIn.selected-features-only");
    private boolean useSelected = false;
    private MultiInputDialog dialog;
    private JComboBox addLayerComboBox;
    
    public UnionPlugIn() {
    }

    /*
      public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
            this, "Tools", "Find Unaligned Segments...", null, new MultiEnableCheck()
          .add(context.getCheckFactory().createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
      }
    */
    public boolean execute(PlugInContext context) throws Exception {
    	//[sstein, 16.07.2006] put here again to load correct language
        //[mmichaud 2007-05-20] move to UnionPlugIn constructor to load the string only once
        //LAYER = I18N.get("ui.plugin.analysis.UnionPlugIn.layer");
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes. [Jon Aquino]
        int n = context.getLayerViewPanel().getSelectionManager()
		  .getFeaturesWithSelectedItems().size();
        useSelected = (n > 0);
        initDialog(context);
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return false;
        }

        return true;
    }

    private void initDialog(PlugInContext context) {
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), I18N.get("ui.plugin.analysis.UnionPlugIn.union"), true);

        //dialog.setSideBarImage(IconLoader.icon("Overlay.gif"));
        if (useSelected) {
            dialog.setSideBarDescription(
                I18N.get("ui.plugin.analysis.UnionPlugIn.creates-a-new-layer-containing-the-union-of-selected-features-in-the-input-layer"));
        }
        else {
            dialog.setSideBarDescription(
                I18N.get("ui.plugin.analysis.UnionPlugIn.creates-a-new-layer-containing-the-union-of-all-the-features-in-the-input-layer"));
        }
        String fieldName = LAYER;
        if (useSelected) {
            dialog.addLabel(SELECTED_ONLY);
        }
        else {
            addLayerComboBox = dialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());
        }
        GUIUtil.centreOnWindow(dialog);
    }

    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        FeatureCollection a;
        Collection inputC;
        if (useSelected) {
            inputC = context.getLayerViewPanel()
                            .getSelectionManager()
                            .getFeaturesWithSelectedItems();
            FeatureSchema featureSchema = new FeatureSchema();
            featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
		 	a = new FeatureDataset(inputC, featureSchema);
        }
        else {
            a = dialog.getLayer(LAYER).getFeatureCollectionWrapper();
        }
        
        FeatureCollection union = progressiveUnion(monitor, a);
        
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        context.addLayer(StandardCategoryNames.RESULT, I18N.get("ui.plugin.analysis.UnionPlugIn.union"), union);
        
    }

    // The naive algorithm is not efficient for dataset containing more than one thousand
    // features. See a replacement in progressiveUnion [mmichaud 2007-06-10]
    private FeatureCollection union(TaskMonitor monitor, FeatureCollection fc) {
        monitor.allowCancellationRequests();
        monitor.report(I18N.get("ui.plugin.analysis.UnionPlugIn.computing-union"));

        List unionGeometryList = new ArrayList();

        Geometry currUnion = null;
        int size = fc.size();
        int count = 1;

        for (Iterator i = fc.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            Geometry geom = f.getGeometry();

            if (currUnion == null) {
                currUnion = geom;
            } else {
                currUnion = currUnion.union(geom);
            }

            monitor.report(count++, size, "features");
        }

        unionGeometryList.add(currUnion);

        return FeatureDatasetFactory.createFromGeometry(unionGeometryList);
    }
    
   /**
    * New method for union. Instead of the naive algorithm looping over the features and
    * unioning each time, this one union small groups of features which are closed to each
    * other, then iterates over the result.
    * The difference is not so important for small datasets, but for large datasets, the
    * difference may of 5 minutes versus 5 hours.
    */
    private FeatureCollection progressiveUnion(TaskMonitor monitor, FeatureCollection fc) {
        monitor.allowCancellationRequests();
        //monitor.report(I18N.get("ui.plugin.analysis.UnionPlugIn.computing-union"));
        
        List unionGeometryList = new ArrayList();
        for (Iterator i = fc.iterator(); i.hasNext();) {
            unionGeometryList.add(((Feature) i.next()).getGeometry());
        }
        int iteration = 1;
        int nbIteration = 1 + (int)(Math.log(unionGeometryList.size())/Math.log(4));
        while (unionGeometryList.size() > 1) {
            monitor.report(I18N.get("ui.plugin.analysis.UnionPlugIn.computing-union") + " (" + iteration++ + "/" + nbIteration + ")");
            final int cellSize = 1 + (int)Math.sqrt(unionGeometryList.size());
            java.util.Comparator comparator =  new java.util.Comparator(){
                public int compare(Object o1, Object o2) {
                    if (o1==null || o2==null) return 0;
                    Envelope env1 = ((Geometry)o1).getEnvelopeInternal();
                    Envelope env2 = ((Geometry)o2).getEnvelopeInternal();
                    double indice1 = env1.getMinX()/cellSize + cellSize*((int)env1.getMinY()/cellSize);
                    double indice2 = env2.getMinX()/cellSize + cellSize*((int)env2.getMinY()/cellSize);
                    return indice1>=indice2?1:indice1<indice2?-1:0;
                }
                public boolean equals(Object obj) {return this.equals(obj);}
            };
            java.util.TreeSet treeSet = new java.util.TreeSet(comparator);
            treeSet.addAll(unionGeometryList);
            // Testes with groups of 4, 8 and 16 (4 is better than 8 which is better than 16
            // for large datasets).
            unionGeometryList = union(monitor, treeSet, 4);
        }
        
        return FeatureDatasetFactory.createFromGeometry(unionGeometryList);
    }
    
   /**
    * Method unioning an ordered set of geometries by small groups.
    * 
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
            monitor.report(++count, size, "features");
        }
        if (groupSize-count%groupSize!=0) {
            unionGeometryList.add(currUnion);
        }
        return unionGeometryList;
    }
    
}
