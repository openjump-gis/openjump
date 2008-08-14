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
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;


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
    
    public void initialize(PlugInContext context) throws Exception
    {
        	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    		featureInstaller.addMainMenuItem(
    	        this,								//exe
  				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS, MenuNames.ONELAYER}, 	//menu path
                this.getName() + "...", //name methode .getName recieved by AbstractPlugIn 
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext())); //enable check  
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
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
        
        Collection geoms = FeatureUtil.toGeometries(a.getFeatures());
        Geometry g = UnaryUnionOp.union(geoms);
        geoms.clear();
        geoms.add(g);
        FeatureCollection fc = FeatureDatasetFactory.createFromGeometry(geoms);
        
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        context.addLayer(StandardCategoryNames.RESULT, I18N.get("ui.plugin.analysis.UnionPlugIn.union"), fc);
    }
    
}
