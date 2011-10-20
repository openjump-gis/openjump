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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JMenuItem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.plugin.util.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.I18N;

/**
 * Provides basic functions for computation with {@link Geometry} objects.
 * <p>
 * Uses {@link GeometryFunction} objects obtained from the Registry
 * by the key GEOMETRY_FUNCTION_REG_KEY.
 * Other plug-ins can add further Geometry functions to the Registry.
 *
 * @see GeometryFunction
 */
public class GeometryFunctionPlugIn extends AbstractPlugIn
                                    implements ThreadedPlugIn {
  public static final String GEOMETRY_FUNCTION_REG_KEY = "Geometry Function Registry Key";
  
  //-- [sstein 15.02.2006]
  private String sErrorsFound = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.errors-found-while-executing-function");
  private String sFunction = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.function");
  private String sFeatures = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.features");
  
  private String SRC_LAYER = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Source");
  private String MASK_LAYER = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Mask");
  private String METHODS = sFunction;
  private String PARAM = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Parameter");
  private String SELECTED_ONLY = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Use-selected-features-only");
  private String UPDATE_SRC = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Update-Source-features-with-result");
  private String ADD_TO_SRC = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Add-result-to-Source-layer");
  private String CREATE_LYR = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Create-new-layer-for-result");

  private Collection functions;
  private MultiInputDialog dialog;
  private Layer srcLayer, maskLayer;
  //private String funcNameToRun;
  private GeometryFunction functionToRun = null;
  private boolean exceptionThrown = false;

  private boolean createLayer = false;
  private boolean updateSource = false;
  private boolean addToSource = false;

  private boolean useSelected = false;

  private Geometry geoms[] = new Geometry[2];
  private double[] params = new double[2];

  public GeometryFunctionPlugIn() {}

  private String categoryName = StandardCategoryNames.RESULT;

  public void setCategoryName(String value) {
    categoryName = value;
  }

  private boolean addToSourceAllowed = true;

  public void setAddToSourceAllowed(boolean value) {
    addToSourceAllowed = value;
  }

  public void initialize(PlugInContext context) throws Exception {
    	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
  		featureInstaller.addMainMenuItem(
  		    this,
  		    new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
  		    new JMenuItem(this.getName() + "..."),
  		    createEnableCheck(context.getWorkbenchContext()));
        registerFunctions(context);
  }

  private void registerFunctions(PlugInContext context){
    // register standard functions
    GeometryFunction[] functions = GeometryFunction.getFunctions();
    context.getWorkbenchContext().getRegistry().createClassification(GEOMETRY_FUNCTION_REG_KEY,GeometryFunction.class);
    for (int i = 0; i < functions.length; i++) {
      context.getWorkbenchContext().getRegistry()
               .createEntry(GEOMETRY_FUNCTION_REG_KEY, functions[i]);
    }
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

      return new MultiEnableCheck()
                      .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                      .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }
  
  public boolean execute(PlugInContext context) throws Exception {
    //-- [sstein 16.07.2006] put here again for langugae settings
    sErrorsFound = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.errors-found-while-executing-function");
    sFunction = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.function");
    sFeatures = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.features");
    
    SRC_LAYER = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Source");
    MASK_LAYER = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Mask");
    METHODS = sFunction;
    PARAM = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Parameter");
    SELECTED_ONLY = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Use-selected-features-only");
    UPDATE_SRC = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Update-Source-features-with-result");
    ADD_TO_SRC = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Add-result-to-Source-layer");
    CREATE_LYR = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Create-new-layer-for-result");

    functions = context.getWorkbenchContext().getRegistry()
              .getEntries(GEOMETRY_FUNCTION_REG_KEY);
    dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (! dialog.wasOKPressed()) { return false; }
    getDialogValues(dialog);
    return true;
  }


  public void run(TaskMonitor monitor, PlugInContext context)
      throws Exception {
    monitor.allowCancellationRequests();

    // input-proofing
    if (functionToRun == null) return;
    if (srcLayer == null) return;
    if ( (updateSource || addToSource)
          && ! srcLayer.isEditable()) {
      context.getWorkbenchFrame().warnUser(srcLayer.getName() + " " + I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.is-not-editable"));
      return;
    }

    monitor.report(I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Executing-function") + " " + functionToRun.getName() + "...");

    ArrayList modifiedFeatures = new ArrayList();
    Collection resultFeatures = null;
    int nArgs = functionToRun.getGeometryArgumentCount();

    if (nArgs == 2) {
      if (maskLayer == null) return;

      Collection fc1 = getFeaturesToProcess(srcLayer, context);
      Collection fc2 = getFeaturesToProcess(maskLayer, context);

      // check for valid size of input
      if (fc2.size() != 1) {
        context.getWorkbenchFrame().warnUser(I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Mask-must-contain-exactly-one-geometry"));
        return;
      }
      Geometry geomMask = ((Feature) fc2.iterator().next()).getGeometry();

      resultFeatures = runGeometryMethodWithMask(monitor, fc1, geomMask, functionToRun, modifiedFeatures);
    } else {
      Collection fc1 = getFeaturesToProcess(srcLayer, context);
      resultFeatures = runGeometryMethod(monitor, fc1, functionToRun, modifiedFeatures);
    }

    // this will happen if plugin was cancelled
    if (resultFeatures == null) return;

    if (modifiedFeatures.size() == 0) {
      context.getWorkbenchFrame().warnUser(I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.No-geometries-were-processed"));
      return;
    }

    if (createLayer) {
      String outputLayerName = LayerNameGenerator.generateOperationOnLayerName(
          functionToRun.toString(),
          srcLayer.getName());
      FeatureCollection resultFC = new FeatureDataset(srcLayer.getFeatureCollectionWrapper().getFeatureSchema());
      resultFC.addAll(resultFeatures);
      context.getLayerManager().addCategory(categoryName);
      context.addLayer(categoryName, outputLayerName, resultFC);
    } else if (updateSource) {
        final Collection undoableNewFeatures = resultFeatures;
        final Collection undoableModifiedFeatures = modifiedFeatures;

        UndoableCommand cmd = new UndoableCommand( getName() ) {
            public void execute() {
                srcLayer.getFeatureCollectionWrapper().removeAll( undoableModifiedFeatures );
                srcLayer.getFeatureCollectionWrapper().addAll( undoableNewFeatures );
            }

            public void unexecute() {
                srcLayer.getFeatureCollectionWrapper().removeAll( undoableNewFeatures );
                srcLayer.getFeatureCollectionWrapper().addAll( undoableModifiedFeatures );
            }
        };

        execute( cmd, context );
    } else if (addToSource) {
        final Collection undoableFeatures = resultFeatures;

        UndoableCommand cmd = new UndoableCommand( getName() ) {
            public void execute() {
                srcLayer.getFeatureCollectionWrapper().addAll( undoableFeatures );
            }

            public void unexecute() {
                srcLayer.getFeatureCollectionWrapper().removeAll( undoableFeatures );
            }
        };

        execute( cmd, context );
    }

    if (exceptionThrown) {
      context.getWorkbenchFrame().warnUser(sErrorsFound);
    }
  }


  private Feature getSelectedFeature(Layer lyr, PlugInContext context){
    Collection selected = context
                        .getLayerViewPanel()
                        .getSelectionManager().getFeaturesWithSelectedItems(lyr);
    if (selected.size() != 1)
      return null;
    return (Feature) selected.iterator().next();
  }


  private Collection getSelectedFeatures(Layer lyr, PlugInContext context){
    Collection selected = context
                        .getLayerViewPanel()
                        .getSelectionManager().getFeaturesWithSelectedItems(lyr);
    return selected;
  }


  private Collection getFeaturesToProcess(Layer lyr, PlugInContext context){
    if (useSelected)
      return context.getLayerViewPanel()
                        .getSelectionManager().getFeaturesWithSelectedItems(lyr);
    return lyr.getFeatureCollectionWrapper().getFeatures();
  }



  private Collection runGeometryMethodWithMask(TaskMonitor monitor,
                                       Collection fcA,
                                       Geometry geomB,
                                       GeometryFunction func,
                                       Collection modifiedFeatures
                                       ) {
    exceptionThrown = false;
    Collection resultColl = new ArrayList();
    int total = fcA.size();
    int count = 0;
    for (Iterator ia = fcA.iterator(); ia.hasNext(); ) {

      monitor.report(count++, total, sFeatures);
      if (monitor.isCancelRequested()) return null;

      Feature fa = (Feature) ia.next();
      Geometry ga = fa.getGeometry();
      geoms[0] = ga;
      geoms[1] = geomB;
      Geometry result = execute(func, geoms, params);

      saveResult(fa, result, resultColl, modifiedFeatures);

    }
    return resultColl;
  }


  private Collection runGeometryMethod(TaskMonitor monitor,
                                       Collection fc,
                                       GeometryFunction func,
                                       Collection modifiedFeatures
                                       ){
    exceptionThrown = false;
    Collection resultColl = new ArrayList();
    int total = fc.size();
    int count = 0;
    for (Iterator iSrc = fc.iterator(); iSrc.hasNext(); ) {

      monitor.report(count++, total, sFeatures);
      if (monitor.isCancelRequested()) return null;

      Feature fSrc = (Feature) iSrc.next();
      Geometry gSrc = fSrc.getGeometry();
      if (gSrc == null) continue;

      geoms[0] = gSrc;
      Geometry result = execute(func, geoms, params);

      saveResult(fSrc, result, resultColl, modifiedFeatures);
    }

    return resultColl;
  }

  private void saveResult(Feature srcFeat, Geometry resultGeom, Collection resultColl, Collection modifiedFeatures) {
    if (resultGeom == null || resultGeom.isEmpty()) {
        // do nothing
    } else {
      Feature fNew = srcFeat.clone(true);
      fNew.setGeometry(resultGeom);
      resultColl.add(fNew);
      modifiedFeatures.add(srcFeat);
    }
  }

  private Geometry execute(GeometryFunction func, Geometry[] geoms, double[] params){
    try {
      return func.execute(geoms, params);
    }
    catch (RuntimeException ex) {
      // simply eat exceptions and report them by returning null
      exceptionThrown = true;
    }
    return null;

  }

  private JComboBox layer2ComboBox;
  private JTextField paramField;
  private JLabel labelField;
//  private JCheckBox replaceSrcChkBox;
  private JRadioButton updateSourceRB;
  private JRadioButton createNewLayerRB;
  private JRadioButton addToSourceRB;


  private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
  {
    //dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffSegments.png")));
    dialog.setSideBarDescription(
    		I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Computes-a-geometric-function-on-features-in-the-Source-layer") + "  "
        + I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Geometry-can-be-saved-to-a-new-layer,-updated-in-place,-or-added-to-the-Source-layer")+ "  "
        + I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Binary-geometric-functions-take-a-mask-feature-as-their-second-operand"));

    //Set initial layer values to the first and second layers in the layer list.
    //In #initialize we've already checked that the number of layers >= 1. [Jon Aquino]
    if (srcLayer == null) srcLayer = context.getCandidateLayer(0);
    dialog.addLayerComboBox(SRC_LAYER, srcLayer,
    		I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.The-Source-layer-features-provide-the-first-operand-for-the-chosen-function"),
                            context.getLayerManager());

    JComboBox functionComboBox = dialog.addComboBox(METHODS, functionToRun,
        functions, null);
    functionComboBox.addItemListener(new MethodItemListener());

    paramField = dialog.addDoubleField(PARAM, params[0], 10);

    layer2ComboBox = dialog.addLayerComboBox(MASK_LAYER, maskLayer,
    		I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.The-Mask-layer-must-contain-a-single-feature,-which-is-used-as-the-second-operand-for-binary-functions"),
        context.getLayerManager()  );
    dialog.addCheckBox(SELECTED_ONLY, useSelected);

    final String OUTPUT_GROUP = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Match-Type");
    createNewLayerRB = dialog.addRadioButton(CREATE_LYR, OUTPUT_GROUP, true,
    		I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Create-a-new-layer-for-the-results"));
    updateSourceRB = dialog.addRadioButton(UPDATE_SRC, OUTPUT_GROUP, false,
    		I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Replace-the-geometry-of-Source-features-with-the-result-geometry") + "  ");

    if ( addToSourceAllowed ) {
        addToSourceRB = dialog.addRadioButton(ADD_TO_SRC, OUTPUT_GROUP, false,
                I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Add-the-result-geometry-to-the-Source-layer")+"  ");
    }

    updateUIForMethod(functionToRun);
  }



  private void getDialogValues(MultiInputDialog dialog) {
    srcLayer = dialog.getLayer(SRC_LAYER);
    maskLayer = dialog.getLayer(MASK_LAYER);
    functionToRun = (GeometryFunction) dialog.getComboBox(METHODS).getSelectedItem();
    params[0] = dialog.getDouble(PARAM);
    useSelected = dialog.getBoolean(SELECTED_ONLY);
    createLayer = dialog.getBoolean(CREATE_LYR);
    updateSource = dialog.getBoolean(UPDATE_SRC);

    if ( addToSourceAllowed ) {
        addToSource = dialog.getBoolean(ADD_TO_SRC);
    }
  }

  private void updateUIForMethod(GeometryFunction func){
    boolean layer2Used = false;
    boolean paramUsed = false;
    if (func != null) {
      layer2Used = func.getGeometryArgumentCount() > 1;
      paramUsed = func.getParameterCount() > 0;
    }
    layer2ComboBox.setEnabled(layer2Used);
    paramField.setEnabled(paramUsed);
    // this has the effect of making the background gray (disabled)
    paramField.setOpaque(paramUsed);

    dialog.validate();
  }

  private class MethodItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      updateUIForMethod((GeometryFunction) e.getItem());
    }
  }

}