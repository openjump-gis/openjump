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

import java.util.*;

import java.awt.event.*;
import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.plugin.util.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
* Queries a layer by a spatial predicate.
*/
public class SpatialJoinPlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    private Layer srcLayerA;
    private Layer srcLayerB;
    private JTextField paramField;
    private Collection functionNames;
    private MultiInputDialog dialog;
    private String funcNameToRun;
    private GeometryPredicate functionToRun = null;
    private boolean exceptionThrown = false;

    private double[] params = new double[2];

    public SpatialJoinPlugIn() {
        functionNames = GeometryPredicate.getNames();
    }

    private String categoryName = StandardCategoryNames.RESULT;

    public String getName(){
        //exchanged plugin with SIGLE plugin
        return I18N.get("ui.plugin.analysis.SpatialJoinPlugIn.Spatial-Join");
        //return I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Transfer-Attributes");
    }
  
    public void setCategoryName(String value) {
        categoryName = value;
    }

    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuItem(
            this,
            new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
            this.getName() + "...",
            false,			//checkbox
            null,			//icon
            createEnableCheck(context.getWorkbenchContext())); 
    }
  
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(2));
    }
  
    public boolean execute(PlugInContext context) throws Exception {
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

        // input-proofing
        if (functionToRun == null) return;
        if (srcLayerA == null) return;
        if (srcLayerB == null) return;

        monitor.report(
            I18N.get("ui.plugin.analysis.SpatialJoinPlugIn.Executing-join") +
                     " " + functionToRun.getName() + "...");

        FeatureCollection srcAFC = srcLayerA.getFeatureCollectionWrapper();
        FeatureCollection srcBFC = srcLayerB.getFeatureCollectionWrapper();
        //[sstein 28.Mar.2008] reversed order of input
        //(to be able to read from top to down the spatial relations) 
        SpatialJoinExecuter executer = new SpatialJoinExecuter(srcBFC, srcAFC);
        FeatureCollection resultFC = executer.getResultFC();
        executer.execute(monitor, functionToRun, params, resultFC);

        if (monitor.isCancelRequested()) return;

        String outputLayerName = I18N.get("ui.plugin.analysis.SpatialJoinPlugIn.Join")+ "-" + funcNameToRun;
        context.getLayerManager().addCategory(categoryName);
        context.addLayer(categoryName, outputLayerName, resultFC);
    
        if (exceptionThrown) {
            context.getWorkbenchFrame()
                   .warnUser("Errors found while executing query");
        }
    }

    private final static String LAYER_A = GenericNames.LAYER_A + " (" + GenericNames.TARGET_LAYER + ")";
    private final static String LAYER_B = GenericNames.LAYER_B + " (" + GenericNames.SOURCE_LAYER + ")";
    private final static String PREDICATE = GenericNames.RELATION;
    private final static String PARAM = GenericNames.PARAMETER;

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    
        //dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffSegments.png")));
        //[sstein 31March2008] replaced sidebar description by better description use in SIGLE plugin  
        /*
        dialog.setSideBarDescription(
            I18N.get("ui.plugin.analysis.SpatialJoinPlugIn.Joins-two-layers-on-a-given-spatial-relationship")
            + " (" + I18N.get("ui.plugin.analysis.SpatialJoinPlugIn.example") +")");
        */
        dialog.setSideBarDescription(
            I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Transfers-the-attributes-of-Layer-B-to-Layer-A-using-a-spatial-criterion")
        );

        //Set initial layer values to the first and second layers in the layer list.
        //In #initialize we've already checked that the number of layers >= 1. [Jon Aquino]
        Layer initLayer1 = (srcLayerA == null)? context.getCandidateLayer(0) : srcLayerA;
        Layer initLayer2 = (srcLayerB == null)? context.getCandidateLayer(1) : srcLayerB;

        dialog.addLayerComboBox(LAYER_A, initLayer1, context.getLayerManager());
    
        JComboBox functionComboBox = dialog.addComboBox(PREDICATE, funcNameToRun, functionNames, null);
        functionComboBox.addItemListener(new MethodItemListener());
        paramField = dialog.addDoubleField(PARAM, params[0], 10);
    
        dialog.addLayerComboBox(LAYER_B, initLayer2, context.getLayerManager());

        updateUIForFunction(funcNameToRun);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        srcLayerA = dialog.getLayer(LAYER_A);
        srcLayerB = dialog.getLayer(LAYER_B);
        funcNameToRun = dialog.getText(PREDICATE);
        functionToRun = GeometryPredicate.getPredicate(funcNameToRun);
        params[0] = dialog.getDouble(PARAM);
    }

    private void updateUIForFunction(String funcName) {
        boolean paramUsed = false;
        GeometryPredicate func = GeometryPredicate.getPredicate(funcName);
        if (func != null) {
            paramUsed = func.getParameterCount() > 0;
        }
        paramField.setEnabled(paramUsed);
        // this has the effect of making the background gray (disabled)
        paramField.setOpaque(paramUsed);
    }

    private class MethodItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            updateUIForFunction((String) e.getItem());
        }
    }

}
