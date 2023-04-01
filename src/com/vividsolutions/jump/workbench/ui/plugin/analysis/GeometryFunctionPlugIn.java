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

import org.locationtech.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.plugin.util.LayerNameGenerator;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Provides basic functions for computation with {@link Geometry} objects.
 * <p>
 * Uses {@link GeometryFunction} objects obtained from the Registry
 * by the key GEOMETRY_FUNCTION_REG_KEY.
 * Other plug-ins can add further Geometry functions to the Registry.
 *
 * @see GeometryFunction
 */
public class GeometryFunctionPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

  public static final String GEOMETRY_FUNCTION_REG_KEY = "Geometry Function Registry Key";
  
  //-- [sstein 15.02.2006]
  private static String sErrorsFound = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.errors-found-while-executing-function");
  private static String sFunction = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.function");
  private static String sFeatures = I18N.getInstance().get("jump.features-processed");
  
  private static String SRC_LAYER = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Source");
  private static String MASK_LAYER = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Mask");
  private static String METHODS = sFunction;
  private static String PARAM = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Parameter");
  private static String SELECTED_ONLY = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Use-selected-features-only");
  private static String UPDATE_SRC = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Update-Source-features-with-result");
  private static String ADD_TO_SRC = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Add-result-to-Source-layer");
  private static String CREATE_LYR = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Create-new-layer-for-result");

  private Collection functions;
  private MultiInputDialog dialog;
  private Layer srcLayer, maskLayer;
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
  private boolean editSourceAllowed  = false;

  public void setAddToSourceAllowed(boolean value) {
    addToSourceAllowed = value;
  }

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    FeatureInstaller featureInstaller = context.getFeatureInstaller();
    featureInstaller.addMainMenuPlugin(this, new String[] { MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS },
        this.getName() + "...", false, null, createEnableCheck(context.getWorkbenchContext()));
    registerFunctions(context);
  }

  private void registerFunctions(PlugInContext context){
    // register standard functions
    GeometryFunction[] functions = GeometryFunction.getFunctions();
    context.getWorkbenchContext().getRegistry().createClassification(GEOMETRY_FUNCTION_REG_KEY,GeometryFunction.class);
    for (GeometryFunction function : functions) {
      context.getWorkbenchContext().getRegistry()
               .createEntry(GEOMETRY_FUNCTION_REG_KEY, function);
    }
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);

      return new MultiEnableCheck()
                      .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                      .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }
  
  public boolean execute(PlugInContext context) throws Exception {
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


  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

    monitor.allowCancellationRequests();

    // input-proofing
    if (functionToRun == null) return;
    if (srcLayer == null) return;
    if ((updateSource || addToSource)
          && !srcLayer.isEditable()) {
      context.getWorkbenchFrame().warnUser(srcLayer.getName() + " " +
              I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.is-not-editable"));
      return;
    }

    monitor.report(I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Executing-function") +
            " " + functionToRun.getName() + "...");

    Collection<Feature> resultFeatures;
    int nArgs = functionToRun.getGeometryArgumentCount();

    Collection<Feature> fc1 = getFeaturesToProcess(srcLayer, context);
    EditTransaction transaction = new EditTransaction(new LinkedHashSet<Feature>(),
            "Geometry function", srcLayer, true, true, context.getLayerViewPanel().getContext());

    if (nArgs == 2) {
      if (maskLayer == null) return;

      Collection<Feature> fc2 = getFeaturesToProcess(maskLayer, context);

      // check for valid size of input
      if (fc2.size() != 1) {
        context.getWorkbenchFrame().warnUser(I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Mask-must-contain-exactly-one-geometry"));
        return;
      }
      Geometry geomMask = fc2.iterator().next().getGeometry();
      resultFeatures = runGeometryMethodWithMask(monitor, fc1, geomMask, functionToRun, transaction);
    } else {
      resultFeatures = runGeometryMethod(monitor, fc1, functionToRun, transaction);
    }

    // this will happen if plugin was cancelled
    if (resultFeatures == null) return;

    if ((createLayer && resultFeatures.size() == 0) ||
        (updateSource && transaction.getFeatures().size() == 0) ||
        (addToSource && transaction.getFeatures().size() == 0)) {
      context.getWorkbenchFrame().warnUser(I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.No-geometries-were-processed"));
      return;
    }

    if (createLayer) {
      String outputLayerName = LayerNameGenerator.generateOperationOnLayerName(
          functionToRun.toString(), srcLayer.getName());
      FeatureCollection resultFC = new FeatureDataset(srcLayer.getFeatureCollectionWrapper().getFeatureSchema());
      resultFC.addAll(resultFeatures);
      context.getLayerManager().addCategory(categoryName);
      context.addLayer(categoryName, outputLayerName, resultFC);
    } else if (updateSource) {
        transaction.commit();
    } else if (addToSource) {
        transaction.commit();
    }

    if (exceptionThrown) {
      context.getWorkbenchFrame().warnUser(sErrorsFound);
    }
  }


  private Collection<Feature> getSelectedFeatures(Layer lyr, PlugInContext context){
    return context.getLayerViewPanel()
            .getSelectionManager().getFeaturesWithSelectedItems(lyr);
  }


  private Collection<Feature> getFeaturesToProcess(Layer lyr, PlugInContext context){
    if (useSelected && lyr.equals(srcLayer)) {
      return context.getLayerViewPanel()
                        .getSelectionManager().getFeaturesWithSelectedItems(lyr);
    }
    return lyr.getFeatureCollectionWrapper().getFeatures();
  }



  private Collection<Feature> runGeometryMethodWithMask(TaskMonitor monitor,
                                       Collection<Feature> fcA,
                                       Geometry geomB,
                                       GeometryFunction func,
                                       EditTransaction transaction) {
    exceptionThrown = false;
    Collection<Feature> resultColl = new ArrayList<>();
    int total = fcA.size();
    int count = 0;
    for (Feature fa : fcA) {

      monitor.report(count++, total, sFeatures);
      if (monitor.isCancelRequested()) return null;

      Geometry ga = fa.getGeometry();
      geoms[0] = ga;
      geoms[1] = geomB;
      Geometry result = execute(func, geoms, params);

      saveResult(fa, result, resultColl, transaction);
    }

    return resultColl;
  }


  private Collection<Feature> runGeometryMethod(TaskMonitor monitor,
                                       Collection<Feature> fc,
                                       GeometryFunction func,
                                       EditTransaction transaction){
    exceptionThrown = false;
    Collection<Feature> resultColl = new ArrayList<>();
    int total = fc.size();
    int count = 0;
    for (Feature fSrc : fc) {

      monitor.report(count++, total, sFeatures);
      if (monitor.isCancelRequested()) return null;

      Geometry gSrc = fSrc.getGeometry();
      if (gSrc == null) continue;

      geoms[0] = gSrc;
      Geometry result = execute(func, geoms, params);

      saveResult(fSrc, result, resultColl, transaction);
    }

    return resultColl;
  }

  private void saveResult(Feature srcFeat, Geometry resultGeom,
                          Collection<Feature> resultColl, EditTransaction transaction) {
      if (createLayer || addToSource) {
        // [mmichaud 2013-10-25] change deep parameter from true to false
        // as the geometry will be changed anyway
        // second argument prevent the copy of the external PK attribute
        Feature fNew = srcFeat.clone(false, false);
        fNew.setGeometry(resultGeom);
        if (resultGeom != null && !resultGeom.isEmpty()) {
            if (createLayer) {
                resultColl.add(fNew);
            }
            else if (addToSource) transaction.createFeature(fNew);
        }
      }
      else if (updateSource) {
          transaction.modifyFeatureGeometry(srcFeat, resultGeom);
      }
  }

  private Geometry execute(GeometryFunction func, Geometry[] geoms, double[] params){
    try {
      return func.execute(geoms, params);
    }
    catch (RuntimeException ex) {
      // simply eat exceptions and report them by returning null
      ex.printStackTrace();
      exceptionThrown = true;
    }
    return null;

  }

  private JComboBox layer2ComboBox;
  private JTextField paramField;


  private void setDialogValues(final MultiInputDialog dialog, PlugInContext context)
  {
    //dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffGeometry.png")));
    dialog.setSideBarDescription(
    		I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Computes-a-geometric-function-on-features-in-the-Source-layer") + "  "
        + I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Geometry-can-be-saved-to-a-new-layer,-updated-in-place,-or-added-to-the-Source-layer")+ "  "
        + I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Binary-geometric-functions-take-a-mask-feature-as-their-second-operand"));

    //Set initial layer values to the first and second layers in the layer list.
    //In #initialize we've already checked that the number of layers >= 1. [Jon Aquino]
    srcLayer = context.getCandidateLayer(0);
    editSourceAllowed = srcLayer.isEditable();
    final JComboBox srcLayerComboBox = dialog.addLayerComboBox(SRC_LAYER, srcLayer,
    		I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.The-Source-layer-features-provide-the-first-operand-for-the-chosen-function"),
                            context.getLayerManager());
    srcLayerComboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            srcLayer = dialog.getLayer(SRC_LAYER);
            editSourceAllowed = srcLayer.isEditable();
        }
    });

    JComboBox functionComboBox = dialog.addComboBox(METHODS, functionToRun,
        functions, null);
    functionComboBox.addItemListener(new MethodItemListener());

    paramField = dialog.addDoubleField(PARAM, params[0], 10);

    if (maskLayer == null) maskLayer = context.getLayerManager().size() > 1 ? context.getCandidateLayer(1) : srcLayer;
    layer2ComboBox = dialog.addLayerComboBox(MASK_LAYER, maskLayer,
    		I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.The-Mask-layer-must-contain-a-single-feature,-which-is-used-as-the-second-operand-for-binary-functions"),
        context.getLayerManager()  );
    final JCheckBox useSelectedCheckBox =  dialog.addCheckBox(SELECTED_ONLY, useSelected);
    boolean selectionEmpty = getSelectedFeatures((Layer)srcLayerComboBox.getSelectedItem(), context).isEmpty();
    useSelectedCheckBox.setSelected(useSelected && !selectionEmpty);
    useSelectedCheckBox.setEnabled(!selectionEmpty);

    final String OUTPUT_GROUP = I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Match-Type");
    JRadioButton createNewLayerRB = dialog.addRadioButton(CREATE_LYR, OUTPUT_GROUP, true,
    		I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Create-a-new-layer-for-the-results"));
    JRadioButton updateSourceRB = dialog.addRadioButton(UPDATE_SRC, OUTPUT_GROUP, false,
    		I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Replace-the-geometry-of-Source-features-with-the-result-geometry") + "  ");
    updateSourceRB.setEnabled(editSourceAllowed);

    //if ( addToSourceAllowed && editSourceAllowed) {
    JRadioButton addToSourceRB = dialog.addRadioButton(ADD_TO_SRC, OUTPUT_GROUP, false,
            I18N.getInstance().get("ui.plugin.analysis.GeometryFunctionPlugIn.Add-the-result-geometry-to-the-Source-layer")+"  ");
    addToSourceRB.setEnabled(addToSourceAllowed && editSourceAllowed);
    //}

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