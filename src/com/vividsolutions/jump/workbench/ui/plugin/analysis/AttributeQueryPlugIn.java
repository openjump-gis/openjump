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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
* Queries a layer by a spatial predicate.
*/
public class AttributeQueryPlugIn extends AbstractPlugIn 
                                  implements ThreadedPlugIn {
  
  private static String ATTR_GEOMETRY_AREA = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.Area");
  private static String ATTR_GEOMETRY_LENGTH = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.Length");
  private static String ATTR_GEOMETRY_NUMPOINTS = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.NumPoints");
  private static String ATTR_GEOMETRY_NUMCOMPONENTS = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.NumComponents");
  private static String ATTR_GEOMETRY_NUMHOLES = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.NumHoles");
  private static String ATTR_GEOMETRY_ISCLOSED = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.IsClosed");
  private static String ATTR_GEOMETRY_ISEMPTY = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.IsEmpty");
  private static String ATTR_GEOMETRY_ISSIMPLE = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.IsSimple");
  private static String ATTR_GEOMETRY_ISVALID = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.IsValid");
  private static String ATTR_GEOMETRY_TYPE = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.Type");
  private static String ATTR_GEOMETRY_DIMENSION = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Geometry.Dimension");

  // MD - could easily add this later
  //private final static String DIALOG_COMPLEMENT = "Complement Result";

  private Collection functionNames;
  private MultiInputDialog dialog;
  private Layer srcLayer;
  private String attrName;
  private String funcNameToRun;
  private String value = "";
  private boolean complementResult = false;
  private boolean caseInsensitive = false;
  private boolean exceptionThrown = false;
  private JRadioButton updateSourceRB;
  private JRadioButton createNewLayerRB;
  private boolean createLayer = true;

  public AttributeQueryPlugIn() {
    functionNames = AttributePredicate.getNames();
  }

  private String categoryName = StandardCategoryNames.RESULT;

  public void setCategoryName(String value) {
    categoryName = value;
  }

  public void initialize(PlugInContext context) throws Exception {
      	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
  		featureInstaller.addMainMenuItem(
  	        this,
            new String[] {MenuNames.TOOLS, MenuNames.TOOLS_QUERIES},
            new JMenuItem(this.getName() + "..."),
            createEnableCheck(context.getWorkbenchContext()));
  }
  
  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

      return new MultiEnableCheck()
                      .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                      .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }
  
  public String getName(){
    return I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Attribute-Query");
  }

  public ImageIcon getIcon(){
    return IconLoader.icon("fugue/regular-expression-search.png");
  }

  public boolean execute(PlugInContext context) throws Exception {

    dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);

    if (! dialog.wasOKPressed()) { return false; }

    getDialogValues(dialog);

    // input-proofing
    if (srcLayer == null) return false;
    if (StringUtil.isEmpty(value)) return false;
    if (StringUtil.isEmpty(attrName)) return false;
    if (StringUtil.isEmpty(funcNameToRun)) return false;

    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context)
      throws Exception {
    monitor.allowCancellationRequests();

    monitor.report(I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Executing-query")+"...");

    FeatureCollection sourceFC = srcLayer.getFeatureCollectionWrapper();

    if (monitor.isCancelRequested()) return;

    FeatureCollection resultFC = executeQuery(sourceFC, attrName, value);

    if (createLayer) {
      String outputLayerName = srcLayer.getName() + "_" + 
              attrName.replaceAll(".*\\.","") + "_" + // remove first part of geometry.**** 
              funcNameToRun + "_" + value;
      context.getLayerManager().addCategory(categoryName);
      context.addLayer(categoryName, outputLayerName, resultFC);
    } else {
      SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
      selectionManager.clear();
      selectionManager.getFeatureSelection().selectItems( srcLayer, resultFC.getFeatures() );
    }

    if (exceptionThrown) {
      context.getWorkbenchFrame().warnUser(I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Errors-found-while-executing-query"));
    }
  }

  private FeatureCollection executeQuery(
          FeatureCollection sourceFC,
          String attrName,
          String value){
    AttributePredicate pred = AttributePredicate.getPredicate(funcNameToRun,caseInsensitive);
    FeatureCollection resultFC = new FeatureDataset(sourceFC.getFeatureSchema());

    for (Iterator i = sourceFC.iterator(); i.hasNext(); ) {
      Feature f = (Feature) i.next();
      Object fVal = getValue(f, attrName);

      boolean predResult = pred.isTrue(fVal, value);

      if (complementResult)
        predResult = ! predResult;

      if (predResult) {
          if (createLayer) {
              resultFC.add(f.clone(true));
          } else {
              resultFC.add(f);
          }
      }
    }

    return resultFC;
  }


  private Object getValue(Feature f, String attrName) {
    if (attrName == ATTR_GEOMETRY_AREA) {
      Geometry g = f.getGeometry();
      double area = (g == null) ? 0.0 : g.getArea();
      return new Double(area);
    }
    if (attrName == ATTR_GEOMETRY_LENGTH) {
      Geometry g = f.getGeometry();
      double len = (g == null) ? 0.0 : g.getLength();
      return new Double(len);
    }
    if (attrName == ATTR_GEOMETRY_NUMPOINTS) {
      Geometry g = f.getGeometry();
      double len = (g == null) ? 0.0 : g.getNumPoints();
      return new Double(len);
    }
    if (attrName == ATTR_GEOMETRY_NUMCOMPONENTS) {
      Geometry g = f.getGeometry();
      double len = (g == null) ? 0.0 : g.getNumGeometries();
      return new Double(len);
    }
    if (attrName == ATTR_GEOMETRY_NUMHOLES) {
      Geometry g = f.getGeometry();
      int numHoles = 0;
      for (int i = 0 ; i < g.getNumGeometries() ; i++) {
          if (g.getGeometryN(i) instanceof Polygon) {
              numHoles += ((Polygon)g.getGeometryN(i)).getNumInteriorRing();
          }
      }
      return numHoles;
    }
    if (attrName == ATTR_GEOMETRY_ISCLOSED) {
      Geometry g = f.getGeometry();
      if (g instanceof LineString)
        return new Boolean( ((LineString) g).isClosed());
      if (g instanceof MultiLineString)
        return new Boolean( ((MultiLineString) g).isClosed());
      return new Boolean(false);
    }
    if (attrName == ATTR_GEOMETRY_ISEMPTY) {
      Geometry g = f.getGeometry();
      boolean bool = g.isEmpty();
      return new Boolean(bool);
    }
    if (attrName == ATTR_GEOMETRY_ISSIMPLE) {
      Geometry g = f.getGeometry();
      boolean bool = g.isSimple();
      return new Boolean(bool);
    }
    if (attrName == ATTR_GEOMETRY_ISVALID) {
      Geometry g = f.getGeometry();
      boolean bool = g.isValid();
      return new Boolean(bool);
    }
    if (attrName == ATTR_GEOMETRY_TYPE) {
      Geometry g = f.getGeometry();
      String type = StringUtil.classNameWithoutQualifiers(g.getClass().getName());
      return type;
    }
    if (attrName == ATTR_GEOMETRY_DIMENSION) {
        Geometry g = f.getGeometry();
        return g.getDimension();
    }
    return f.getAttribute(attrName);
  }

  private static String LAYER = GenericNames.SOURCE_LAYER;
  private static String ATTRIBUTE = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Attribute");
  private static String PREDICATE = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Condition");
  private static String VALUE = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Value");
  private static String DIALOG_CASE_INSENSITIVE = I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Case-Insensitive");
  private static String DIALOG_COMPLEMENT = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Complement-Result");

  private static String UPDATE_SRC = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Select-features-in-the-source-layer");
  private static String CREATE_LYR = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Create-a-new-layer-for-the-results");

  private JComboBox attrComboBox;

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context){
    dialog.setSideBarDescription(
    		I18N.get("ui.plugin.analysis.AttributeQueryPlugIn.Finds-the-Source-features-which-have-attribute-values-satisfying-a-given-condition"));

    //Set initial layer values to the first and second layers in the layer list.
    //In #initialize we've already checked that the number of layers >= 1. [Jon Aquino]
    Layer initLayer = (srcLayer == null)? context.getCandidateLayer(0) : srcLayer;

    JComboBox lyrCombo = dialog.addLayerComboBox(LAYER, initLayer, context.getLayerManager());
    lyrCombo.addItemListener(new LayerItemListener());
    attrComboBox = dialog.addComboBox(ATTRIBUTE, attrName, functionNames, null);
    final JComboBox dropBox = dialog.addComboBox(PREDICATE, funcNameToRun, functionNames, null);
    dialog.addTextField(VALUE, value, 20, null, null);

    final JCheckBox caseBox = dialog.addCheckBox(DIALOG_CASE_INSENSITIVE, caseInsensitive);
    caseBox.setEnabled(caseBoxEnabled(dropBox));

    // en/disable caseInsensitive box by user input
    dropBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean enabled = caseBoxEnabled(dropBox);
        caseBox.setEnabled(enabled);
      }
    });

    dialog.addCheckBox(DIALOG_COMPLEMENT, complementResult);
    
    final String OUTPUT_GROUP = "OUTPUT_GROUP";
    createNewLayerRB = dialog.addRadioButton(CREATE_LYR, OUTPUT_GROUP, createLayer,CREATE_LYR);
    updateSourceRB = dialog.addRadioButton(UPDATE_SRC, OUTPUT_GROUP, !createLayer, UPDATE_SRC);

    updateUI(initLayer);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    srcLayer = dialog.getLayer(LAYER);
    attrName = dialog.getText(ATTRIBUTE);
    funcNameToRun = dialog.getText(PREDICATE);
    value = dialog.getText(VALUE);
    caseInsensitive = dialog.getBoolean(DIALOG_CASE_INSENSITIVE);
    complementResult = dialog.getBoolean(DIALOG_COMPLEMENT);
    createLayer = dialog.getBoolean(CREATE_LYR);
  }

  private void updateUI(Layer lyr) {
    List attrNames = null;
    if (lyr != null) {
      FeatureCollection fc = lyr.getFeatureCollectionWrapper();
      FeatureSchema fs = fc.getFeatureSchema();

      attrNames = getAttributeNames(fs);
    } else {
      attrNames = new ArrayList();
    }
    attrComboBox.setModel(new DefaultComboBoxModel(new Vector(attrNames)));
    attrComboBox.setSelectedItem(attrName);
  }


  private static boolean caseBoxEnabled( JComboBox<String> dropBox ){
    Object selectedItem = dropBox.getSelectedItem();
    boolean enabled = false;
    for (String function : AttributePredicate.getNamesCI()) {
      if (selectedItem != null && selectedItem.equals(function)) {
        enabled = true;
        break;
      }
    }
    return enabled;
  }
  
  private static List getAttributeNames(FeatureSchema fs) {
    List names = new ArrayList();
    for (int i = 0; i < fs.getAttributeCount(); i++) {
      if (fs.getAttributeType(i) != AttributeType.GEOMETRY)
        names.add(fs.getAttributeName(i));
    }
    names.add(ATTR_GEOMETRY_AREA);
    names.add(ATTR_GEOMETRY_LENGTH);
    names.add(ATTR_GEOMETRY_NUMPOINTS);
    names.add(ATTR_GEOMETRY_NUMCOMPONENTS);
    names.add(ATTR_GEOMETRY_NUMHOLES);
    names.add(ATTR_GEOMETRY_ISCLOSED);
    names.add(ATTR_GEOMETRY_ISEMPTY);
    names.add(ATTR_GEOMETRY_ISSIMPLE);
    names.add(ATTR_GEOMETRY_ISVALID);
    names.add(ATTR_GEOMETRY_TYPE);
    names.add(ATTR_GEOMETRY_DIMENSION);

    return names;
  }


  private class LayerItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      updateUI((Layer) e.getItem());
    }
  }

}



