
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
import javax.swing.JComponent;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;


public class BufferPlugIn
    extends AbstractPlugIn
    implements ThreadedPlugIn
{
	
  public static final ImageIcon ICON = IconLoader.icon("buffer.gif");

  private  String LAYER ;
  private  String DISTANCE;
  private  String END_CAP_STYLE;
  private  String QUADRANT_SEGMENTS;
  private  String SELECTED_ONLY;
  private  String UNION_RESULT;
  private  String COPY_ATTRIUBTES;
  private  String CAP_STYLE_ROUND;
  private  String CAP_STYLE_SQUARE;
  private  String CAP_STYLE_BUTT;
  private  String ATTRIBUTE;
  private  String FROMATTRIBUTE;

  private List endCapStyles;

  private MultiInputDialog dialog;
  private Layer layer;
  private double bufferDistance = 1.0;
  private String endCapStyle;
  private boolean exceptionThrown = false;
  private boolean useSelected = false;
  private int quadrantSegments = 16;
  private boolean unionResult = false;
  private String sideBarText = "";
  private boolean copyAttributes = true;
  private boolean fromAttribute = false;
  private int attributeIndex = 0;

  public BufferPlugIn() {
  }

  private final String ROUND = "Round";   //Don't translate
  private final String SQUARE = "Square"; //Don't translate
  private final String BUTT = "Butt";     //Don't translate

  private String categoryName = StandardCategoryNames.RESULT;

  public void setCategoryName(String value) {
    categoryName = value;
  }
  
  public void initialize(PlugInContext context) throws Exception
  {
      	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
  		featureInstaller.addMainMenuItem(
  	        this,								//exe
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS}, 	//menu path
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
  	//[sstein, 16.07.2006] set again to obtain correct language
	//[LDB: 31.08.2007] moved all initialization of strings here
	  LAYER = I18N.get("ui.plugin.analysis.BufferPlugIn.layer");
	  DISTANCE = I18N.get("ui.plugin.analysis.BufferPlugIn.buffer-distance");
	  END_CAP_STYLE = I18N.get("ui.plugin.analysis.BufferPlugIn.End-Cap-Style");
	  QUADRANT_SEGMENTS = I18N.get(
"org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Number-of-segments-per-circle-quarter");
	  SELECTED_ONLY = 
		  I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Use-selected-features-only");
	  UNION_RESULT = I18N.get("ui.plugin.analysis.UnionPlugIn.union");
	  COPY_ATTRIUBTES = 
		  I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Keep-attributes");
	  CAP_STYLE_ROUND = I18N.get("ui.plugin.analysis.BufferPlugIn.Round");
	  CAP_STYLE_SQUARE = I18N.get("ui.plugin.analysis.BufferPlugIn.Square");
	  CAP_STYLE_BUTT = I18N.get("ui.plugin.analysis.BufferPlugIn.Butt");
	  endCapStyles = new ArrayList();
	  endCapStyles.add(CAP_STYLE_ROUND);
	  endCapStyles.add(CAP_STYLE_SQUARE);
	  endCapStyles.add(CAP_STYLE_BUTT);
	  endCapStyle = CAP_STYLE_ROUND;
	  ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use");
	  FROMATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.Get-distance-from-attribute-value");
	  
	  dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
	  int n = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
	  useSelected = (n > 0);
	  if (useSelected)
		  sideBarText = SELECTED_ONLY;
	  else
		  sideBarText = I18N.get("ui.plugin.analysis.BufferPlugIn.buffers-all-geometries-in-the-input-layer");
	  setDialogValues(dialog, context);
	  updateControls();
	  GUIUtil.centreOnWindow(dialog);
	  dialog.setVisible(true);
	  if (! dialog.wasOKPressed()) { return false; }
	  getDialogValues(dialog);
	  return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context)
      throws Exception{
	monitor.allowCancellationRequests();
    FeatureSchema featureSchema = new FeatureSchema();
    featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    FeatureCollection resultFC = new FeatureDataset(featureSchema);
    Collection inputC;
    if (useSelected) {
    	inputC = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
    	Feature feature = (Feature) inputC.iterator().next();
    	featureSchema = feature.getSchema();
    	inputC = PasteItemsPlugIn.conform(inputC,featureSchema );
    } else {
    	inputC = layer.getFeatureCollectionWrapper().getFeatures();
    	featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
    	resultFC = new FeatureDataset(featureSchema);
    }
    FeatureDataset inputFD = new FeatureDataset(inputC, featureSchema);
    Collection resultGeomColl = runBuffer(monitor, inputFD);
    if (copyAttributes) {
    	FeatureCollection resultFeatureColl = new FeatureDataset(featureSchema);
    	Iterator iResult = resultGeomColl.iterator();
    	for (Iterator iSource = inputFD.iterator(); iSource.hasNext(); ) {   			
    		Feature sourceFeature = (Feature) iSource.next();
    		Geometry gResult = (Geometry) iResult.next();
    		if (!(gResult == null || gResult.isEmpty())) {
    			Feature newFeature = sourceFeature.clone(true);
    			newFeature.setGeometry(gResult);
    			resultFeatureColl.add(newFeature);
    		}
    		if (monitor.isCancelRequested()) break;
    	}
    	resultFC = resultFeatureColl;
    } else {
    	resultFC = FeatureDatasetFactory.createFromGeometry(resultGeomColl);
    }
	if (unionResult) {
		Collection geoms = FeatureUtil.toGeometries(resultFC.getFeatures());
		Geometry g = UnaryUnionOp.union(geoms);
		geoms.clear();
		geoms.add(g);
		resultFC = FeatureDatasetFactory.createFromGeometry(geoms);
	}
    context.getLayerManager().addCategory(categoryName);
    String name;
    if (!useSelected)
    	name = layer.getName();
    else
    	name = I18N.get("ui.MenuNames.SELECTION");
    name = I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.BufferPlugIn") + "-" + name;
    if (endCapStyle != CAP_STYLE_ROUND)
    	name = name + "-" + endCapStyle;
    context.addLayer(categoryName, name, resultFC);
    if (exceptionThrown)
      context.getWorkbenchFrame().warnUser(I18N.get("ui.plugin.analysis.BufferPlugIn.errors-found-while-executing-buffer"));
  }

  private Collection runBuffer(TaskMonitor monitor, FeatureCollection fcA)
  {
    exceptionThrown = false;
    int total = fcA.size();
    int count = 0;
    Collection resultColl = new ArrayList();
    for (Iterator ia = fcA.iterator(); ia.hasNext(); ) {
      monitor.report(count++, total, I18N.get("com.vividsolutions.jump.qa.diff.DiffGeometry.features"));
      if (monitor.isCancelRequested()) break;
      Feature fa = (Feature) ia.next();
      Geometry ga = fa.getGeometry();
      if (fromAttribute) {
    	  Object o = fa.getAttribute(attributeIndex);
    	  if (o instanceof Double)     		  
    		  bufferDistance = ((Double) o).doubleValue();
    	  else if (o instanceof Integer)
       		  bufferDistance = ((Integer) o).doubleValue();
     }
      Geometry result = runBuffer(ga);
      if (result != null)
        resultColl.add(result);
    }
    return resultColl;
  }

  private Geometry runBuffer(Geometry a)
  {
    Geometry result = null;
    try {
      BufferOp bufOp = new BufferOp(a);
      bufOp.setQuadrantSegments(quadrantSegments);
      bufOp.setEndCapStyle(endCapStyleCode(endCapStyle));
      result = bufOp.getResultGeometry(bufferDistance);
       return result;
    }
    catch (RuntimeException ex) {
      // simply eat exceptions and report them by returning null
      exceptionThrown = true;
    }
    return null;
  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
  {
	dialog.setSideBarDescription(sideBarText);
	try{
		dialog.setSideBarImage(IconLoader.icon("Round.gif"));}catch (Exception ex){}
//	dialog.addCheckBox(SELECTED_ONLY, useSelected);
    if (useSelected)
    	dialog.addLabel(SELECTED_ONLY);
    else {
		dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());  	
   
		initComboFields(dialog, FROMATTRIBUTE, ATTRIBUTE);
		final MultiInputDialog fdialog = dialog;
        dialog.getCheckBox(FROMATTRIBUTE).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	fromAttribute = fdialog.getCheckBox(FROMATTRIBUTE).isSelected();
            	fdialog.getComboBox(ATTRIBUTE).setEnabled(fromAttribute);
            	//fdialog.getCheckBox(UNION_RESULT).setEnabled(!fromAttribute);
            }
        });
    }
    dialog.addDoubleField(DISTANCE, bufferDistance, 10, null);
    JComboBox endCapComboBox = dialog.addComboBox(END_CAP_STYLE, endCapStyle, endCapStyles, null);
    endCapComboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateControls();
        }
    });
    dialog.addIntegerField(QUADRANT_SEGMENTS,quadrantSegments,3,null);
    JCheckBox unionCheckBox = dialog.addCheckBox(UNION_RESULT, unionResult);
    unionCheckBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateControls();
        }
    });
    JCheckBox copyAttributesCheckBox = dialog.addCheckBox(COPY_ATTRIUBTES, copyAttributes);
    }

  private void getDialogValues(MultiInputDialog dialog) {
	  if (!useSelected)
		  layer = dialog.getLayer(LAYER);
	  bufferDistance = dialog.getDouble(DISTANCE);
	  endCapStyle = dialog.getText(END_CAP_STYLE);
	  quadrantSegments = dialog.getInteger(QUADRANT_SEGMENTS);
//	  if (useSelectedAvailable)
//	  useSelected = dialog.getBoolean(SELECTED_ONLY);
	  unionResult = dialog.getBoolean(UNION_RESULT);
	  copyAttributes = dialog.getBoolean(COPY_ATTRIUBTES);
	  if (fromAttribute) {
		  if (dialog.getCheckBox(FROMATTRIBUTE).isEnabled()) {
			  Layer destinationLayer = dialog.getLayer(LAYER);
			  FeatureSchema schema = destinationLayer.getFeatureCollectionWrapper().getFeatureSchema();
			  String attributeName = dialog.getText(ATTRIBUTE);
			  attributeIndex = schema.getAttributeIndex(attributeName);
		  } else {
			  fromAttribute = false;
		  }
	  }
  }

  private void initComboFields(
		  final MultiInputDialog dialog,
		  final String checkBoxFieldName,
		  final String comboBoxFieldName) {
	  JCheckBox checkBox = dialog.addCheckBox(checkBoxFieldName, false);
	  JComboBox comboBox = dialog.addComboBox(comboBoxFieldName, null, new ArrayList(), null);
	  Layer newLayer = (Layer) dialog.getComboBox(LAYER).getSelectedItem();
	  if (newLayer != null)
		  dialog.getComboBox(comboBoxFieldName).setModel(
				  new DefaultComboBoxModel(
						  new Vector(candidateAttributeNames(newLayer))));
	  boolean numericAttributesPresent = !(candidateAttributeNames(newLayer).size() == 0);
	  checkBox.setEnabled(numericAttributesPresent);
	  //comboBox.setEnabled(numericAttributesPresent);
      comboBox.setEnabled(false);
	  dialog.getComboBox(LAYER).addActionListener(new ActionListener() {
          private Layer lastLayer = null;
		  public void actionPerformed(ActionEvent e) {
			  Layer newLayer = (Layer) dialog.getComboBox(LAYER).getSelectedItem();
              if (lastLayer == newLayer) {
                  return;
              }
              lastLayer = newLayer;
			  dialog.getComboBox(comboBoxFieldName).setModel(
					  new DefaultComboBoxModel(
							  new Vector(candidateAttributeNames(newLayer))));
			  boolean notEmpty = !(candidateAttributeNames(newLayer).size() == 0);
			  dialog.getCheckBox(checkBoxFieldName).setEnabled(notEmpty);
			  if (notEmpty) {
				  dialog.getComboBox(comboBoxFieldName).setSelectedItem(
						  candidateAttributeNames(newLayer).get(0));
			  }
		  }
	  });
	  dialog
	  .addEnableChecks(
			  comboBoxFieldName,
			  Arrays
			  .asList(
					  new Object[] {
							  new EnableCheck() {
								  public String check(JComponent component) {
									  return dialog.getBoolean(checkBoxFieldName)
									  && dialog.getComboBox(comboBoxFieldName).getItemCount()
									  == 0 ? " " : null;
								  }
							  }
					  }));
	  dialog.indentLabel(comboBoxFieldName);
  }
	    
  private List candidateAttributeNames(Layer layer) {
      ArrayList candidateAttributeNames = new ArrayList();
      FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
      for (int i = 0; i < schema.getAttributeCount(); i++) {
          if ((schema.getAttributeType(i) == AttributeType.DOUBLE) ||
                  (schema.getAttributeType(i) == AttributeType.INTEGER)) {
               candidateAttributeNames.add(schema.getAttributeName(i));
          }
      }
      return candidateAttributeNames;
  }

  private int endCapStyleCode(String capStyle)
  {
    if (capStyle == CAP_STYLE_BUTT) return BufferOp.CAP_BUTT;
    if (capStyle == CAP_STYLE_SQUARE) return BufferOp.CAP_SQUARE;
    return BufferOp.CAP_ROUND;
  }
  private Feature combine(Collection originalFeatures) {
      GeometryFactory factory = new GeometryFactory();
      Feature feature = (Feature) ((Feature) originalFeatures.iterator().next()).clone();
       feature.setGeometry(
          factory.createGeometryCollection(
              (Geometry[]) FeatureUtil.toGeometries(originalFeatures).toArray(
                  new Geometry[originalFeatures.size()])));

      return feature;
  }

  protected void updateControls() {
	    getDialogValues(dialog);
	    dialog.getCheckBox(COPY_ATTRIUBTES).setEnabled(!unionResult);
	    int capCode = endCapStyleCode(endCapStyle);
        if (capCode == BufferOp.CAP_BUTT) updateUIForFunction( BUTT);
        else if (capCode == BufferOp.CAP_SQUARE) updateUIForFunction( SQUARE);
        else updateUIForFunction(ROUND);
}

  private void updateUIForFunction(String capName) 
  {
      if ( unionResult)
      {
    	  try{dialog.setSideBarImage(IconLoader.icon(capName + "-union.gif"));}catch (Exception ex){}
    	  //dialog.setSideBarDescription();
      }
      else
      {
    	  try{dialog.setSideBarImage(IconLoader.icon(capName + ".gif"));}catch (Exception ex){}
    	  //dialog.setSideBarDescription();
      }
  }

}
