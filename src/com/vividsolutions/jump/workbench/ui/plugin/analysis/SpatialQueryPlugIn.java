
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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;

/**
* Queries a layer by a spatial predicate.
*/
public class SpatialQueryPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

  private static String UPDATE_SRC = I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Select-features-in-the-source-layer");
  private static String CREATE_LYR = I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Create-a-new-layer-for-the-results");
  private static String MASK_LAYER = GenericNames.MASK_LAYER;
  private static String SRC_LAYER = GenericNames.SOURCE_LAYER;
  private static String PREDICATE = I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Relation");
  private static String PARAM = GenericNames.PARAMETER;
  private static String DIALOG_COMPLEMENT = I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Complement-Result");
  private static String ALLOW_DUPS = I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Allow-Duplicates-in-Result");

  private JTextField paramField;
  private Collection functionNames;
  private Layer maskLyr;
  private Layer srcLayer;
  private String funcNameToRun;
  private GeometryPredicate functionToRun = null;
  private boolean complementResult = false;
  private boolean allowDups = false;
  private boolean createLayer = true;

  private double[] params = new double[2];

  public SpatialQueryPlugIn() {
    functionNames = GeometryPredicate.getNames();
  }

  private String categoryName = StandardCategoryNames.RESULT;

  public void setCategoryName(String value) {
    categoryName = value;
  }

  public String getName(){
  	return I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Spatial-Query");
  }

  public ImageIcon getIcon(){
    return IconLoader.icon("spatial_query.png");
  }

  public void initialize(PlugInContext context) throws Exception {
    FeatureInstaller.getInstance().addMainMenuPlugin(this,
        new String[] { MenuNames.TOOLS, MenuNames.TOOLS_QUERIES });

  }
  
  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

      return new MultiEnableCheck()
                      .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                      .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }
  
  public boolean execute(PlugInContext context) throws Exception {
    MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) return false;
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context)
      throws Exception {
    monitor.allowCancellationRequests();

    // input-proofing
    if (functionToRun == null) return;
    if (maskLyr == null) return;
    if (srcLayer == null) return;

    monitor.report(I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Executing-query")+" " + functionToRun.getName() + "...");

    FeatureCollection maskFC = maskLyr.getFeatureCollectionWrapper();
    FeatureCollection sourceFC = srcLayer.getFeatureCollectionWrapper();

    SpatialQueryExecuter executer = new SpatialQueryExecuter(maskFC, sourceFC);
    executer.setAllowDuplicates(allowDups);
    executer.setComplementResult(complementResult);
    
    // Code added by the Sunburned Surveyor to allow
    // the creation of "normal" selections if a new
    // layer isn't being created for the features
    // selected as part of the spatial analysis.
    executer.setCreateNewLayer(createLayer);
    
    FeatureCollection resultFC = executer.getResultFC();
    executer.execute(monitor, functionToRun, params, resultFC);

    if (monitor.isCancelRequested()) return;

    if (createLayer) 
    {
      String outputLayerName = srcLayer.getName() + "_" + funcNameToRun + "_";
      if (functionToRun.getParameterCount()==1) {
          outputLayerName += (paramField.getText() + "_");
      }
      outputLayerName += maskLyr.getName();
      context.getLayerManager().addCategory(categoryName);
      context.addLayer(categoryName, outputLayerName, resultFC);
    } else {
      SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
      selectionManager.clear();
      selectionManager.getFeatureSelection().selectItems( srcLayer, resultFC.getFeatures() );
    }

    if (executer.isExceptionThrown()) {
      context.getWorkbenchFrame().warnUser(
              I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Errors-found-while-executing-query") +
                      ":" + executer.getException());
    }
  }



  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    //dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffGeometry.png")));
    dialog.setSideBarDescription(
    		I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.Finds-the-Source-features-which-have-a-given-spatial-relationship-to-some-feature-in-the-Mask-layer")
        + " (" + I18N.getInstance().get("ui.plugin.analysis.SpatialQueryPlugIn.ie-where-Source.Relationship(Mask)-is-true") + ")" );

    //Set initial layer values to the first and second layers in the layer list.
    //In #initialize we've already checked that the number of layers >= 1. [Jon Aquino]
    Layer initLayer = context.getCandidateLayer(0);

    dialog.addLayerComboBox(SRC_LAYER, initLayer, context.getLayerManager());
    JComboBox functionComboBox = dialog.addComboBox(PREDICATE, funcNameToRun, functionNames, null);
    functionComboBox.addItemListener(new MethodItemListener());

    maskLyr = context.getLayerManager().size() > 1 ? context.getCandidateLayer(1) : initLayer;
    dialog.addLayerComboBox(MASK_LAYER, maskLyr, context.getLayerManager());

    paramField = dialog.addDoubleField(PARAM, params[0], 10);
    dialog.addCheckBox(ALLOW_DUPS, allowDups);
    dialog.addCheckBox(DIALOG_COMPLEMENT, complementResult);

    final String OUTPUT_GROUP = "OUTPUT_GROUP";
    dialog.addRadioButton(CREATE_LYR, OUTPUT_GROUP, createLayer,CREATE_LYR);
    dialog.addRadioButton(UPDATE_SRC, OUTPUT_GROUP, !createLayer, UPDATE_SRC);

    updateUIForFunction(funcNameToRun);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    maskLyr = dialog.getLayer(MASK_LAYER);
    srcLayer = dialog.getLayer(SRC_LAYER);
    funcNameToRun = dialog.getText(PREDICATE);
    functionToRun = GeometryPredicate.getPredicate(funcNameToRun);
    params[0] = dialog.getDouble(PARAM);
    allowDups = dialog.getBoolean(ALLOW_DUPS);
    complementResult = dialog.getBoolean(DIALOG_COMPLEMENT);
    createLayer = dialog.getBoolean(CREATE_LYR);
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

  private class MethodItemListener
      implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      updateUIForFunction((String) e.getItem());
    }
  }

}
