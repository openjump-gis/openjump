
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.tools;

import java.util.*;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jts.util.Assert;

public class MultiRingBufferSelectedPlugIn
    extends AbstractPlugIn
    implements ThreadedPlugIn
{
  private static String BUFFER;
  private static String LAYER;
  private static String NEWLAYERNAME;
  private static String LABEL;
  private static String THEMING;
  private static String LAYERNAME;
  private static String ATTRIBUTENAME;
  private static String ATTRIBUTEVALUE;
  private static String BUFFERDISTANCE;
  private static String BUFFERNUMBER;
  private static String DISTANCEATTRIBUTE;
  private static String LAYEROPTIONS;
  private static String BUFFEROPTIONS;
  private static String SELECTED_ONLY;
  private static String MULTIPLE_RING_BUFFER;
  private static String sRESET;
  
  private static String layerName;
  private static String attributeName = "Label";
  private static boolean enableTheming = true;
  private static boolean enableRanging = false; //if this is set true need to add sorting routine to attributes
  private static boolean enableLabeling = true;
  
  
  private JSpinner bufferSpinner;
  private boolean exceptionThrown = false;
  private static String[] bufferDistances = {"10","50","100","250","0","0","0","0","0","0"};
  private static String[] bufferAttributeValues = {"A","B","C","D","","","","","",""};
  private int currBufferNumber = 1;
  private JTextField currBufferDistance;
  private JTextField currAttributeValue;
  
  public MultiRingBufferSelectedPlugIn()
  {
  }

  public void initialize(PlugInContext context) throws Exception 
  {
	  MULTIPLE_RING_BUFFER = I18N.get("org.openjump.core.ui.plugin.tools.MultiRingBufferSelectedPlugIn.Multiple-Ring-Buffer");
      context.getFeatureInstaller().addMainMenuItem(this,
      new String[] { MenuNames.TOOLS , MenuNames.TOOLS_GENERATE }, MULTIPLE_RING_BUFFER + "...", false, null, this.createEnableCheck(context.getWorkbenchContext()));
  }
  
  public boolean execute(PlugInContext context) throws Exception {
	  MULTIPLE_RING_BUFFER = I18N.get("org.openjump.core.ui.plugin.tools.MultiRingBufferSelectedPlugIn.Multiple-Ring-Buffer");
	  sRESET = I18N.get("org.openjump.core.ui.plugin.tools.MultiRingBufferSelectedPlugIn.Reset-all-buffer-options");
	  SELECTED_ONLY = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.Use-selected-features-only");
	  BUFFERDISTANCE = I18N.get("ui.plugin.analysis.BufferPlugIn.buffer-distance");
	  BUFFER = I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.BufferPlugIn");
	  NEWLAYERNAME = BUFFER + "-" + I18N.get("ui.MenuNames.SELECTION");
	  LAYER = MenuNames.LAYER;
	  layerName = NEWLAYERNAME;
	  String options =  I18N.get("com.vividsolutions.jump.workbench.ui.plugin.OptionsPlugIn");
	  LAYEROPTIONS = LAYER+" "+options+":";
	  BUFFEROPTIONS = BUFFER + " "+options+":";
	  LABEL = I18N.get("ui.style.LabelStylePanel.labels");
	  THEMING = I18N.get("ui.renderer.style.ColorThemingPanel.colour-theming");
	  String name = I18N.get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Name");
	  LAYERNAME = LAYER + " " + name;
	  String attribute = I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.attribute");
	  ATTRIBUTENAME = attribute+ " "+ name;
	  ATTRIBUTEVALUE =BUFFER +" "+ LABEL;
	  BUFFERNUMBER = BUFFER +" Number";
	  DISTANCEATTRIBUTE =  I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.Distance");

	  MultiInputDialog dialog = new MultiInputDialog(
			  context.getWorkbenchFrame(), getName(), true);
	  setDialogValues(dialog, context);
	  GUIUtil.centreOnWindow(dialog);
	  boolean goodEntry = false;
	  while (!goodEntry)
	  {
		  dialog.setVisible(true);
		  if (! dialog.wasOKPressed()) 
		  { 
			  return false; 
		  }
		  goodEntry = getDialogValues(dialog);
	  }
	  return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context)
      throws Exception
  {
	  FeatureSchema featureSchema = new FeatureSchema();
	  featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
	  if (! DISTANCEATTRIBUTE.equalsIgnoreCase(attributeName))
		  featureSchema.addAttribute(DISTANCEATTRIBUTE, AttributeType.DOUBLE);
	  featureSchema.addAttribute(attributeName, AttributeType.STRING);
	  FeatureDataset featureDataset = new FeatureDataset(featureSchema);
	
	  Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
	  Layer layer = context.addLayer(selectedCategories.isEmpty()
			? StandardCategoryNames.WORKING
					: selectedCategories.iterator().next().toString(), layerName,
					featureDataset);
	
	  layer.setFeatureCollectionModified(true).setEditable(true);
	  Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
	  Collection bufferCollection = getBuffers(featureSchema, selectedFeatures);
	  
	  for (Iterator i = bufferCollection.iterator(); i.hasNext();)
		  layer.getFeatureCollectionWrapper().add((Feature)i.next());
	  
	  if (enableLabeling)
	  {
		  LabelStyle labelStyle = layer.getLabelStyle();
		  labelStyle.setAttribute(attributeName);	
		  labelStyle.setEnabled(true);
	  }
	  
	  //ColorThemingStyle.get(layer).setEnabled(enableTheming);
	  ColorScheme colorScheme = ColorScheme.create("spectral (ColorBrewer)");		  
	  Map attributeToStyleMap = new HashMap();
		
	  if (enableTheming)
	  {
		  for (Iterator i = bufferCollection.iterator(); i.hasNext();) 
		  {
			  Feature feature = (Feature) i.next();
			  attributeToStyleMap.put((String)feature.getAttribute(attributeName), new BasicStyle(colorScheme.next()));
		  }
	  }
	  else if (enableRanging)
	  {
		  colorScheme = ColorScheme.create("Reds (ColorBrewer)");
		  attributeToStyleMap = new Range.RangeTreeMap();
		  Object previousValue = null;
		  
		  for (Iterator i = bufferCollection.iterator(); i.hasNext();) 
		  {
			  Object value = ((Feature) i.next()).getAttribute(attributeName);
			  try
			  {
				  if (previousValue == null) continue;
				  attributeToStyleMap.put(new Range(previousValue, true, value, false), new BasicStyle(colorScheme.next()));
			  }
			  finally
			  {
				  previousValue = value;
			  }
		  }
		  attributeToStyleMap.put(new Range(previousValue, true, new Range.PositiveInfinity(), false), new BasicStyle(colorScheme.next()));
	  }
	  
	  if (enableTheming)
	  {
		  layer.getBasicStyle().setEnabled(false);
		  ColorThemingStyle themeStyle = new ColorThemingStyle(attributeName, attributeToStyleMap, new BasicStyle(Color.gray));
		  themeStyle.setEnabled(true);
		  layer.addStyle(themeStyle);
		  ColorThemingStyle.get(layer).setEnabled(enableTheming);
		  layer.removeStyle(ColorThemingStyle.get(layer));
		  ColorThemingStyle.get(layer).setEnabled(true);
		  layer.getBasicStyle().setEnabled(false);
	  }
				
	  if (exceptionThrown)
		  context.getWorkbenchFrame().warnUser("Errors found while executing buffer");
  }

  private Collection getBuffers(FeatureSchema featureSchema, Collection selectedFeatures)
  {
	  exceptionThrown = false;
	  Collection bufferFeatureCollection = new ArrayList();
	  Geometry prevGeo = null;
	    
	  Iterator ia = selectedFeatures.iterator();
	  prevGeo = ((Feature) selectedFeatures.iterator().next()).getGeometry();
	  while (ia.hasNext()) prevGeo = prevGeo.union(((Feature) ia.next()).getGeometry());
	        
	  for (int bufferNum = 0; bufferNum < bufferDistances.length; bufferNum++)
	  {
		  Geometry bufferGeo = null;
		  //no need for error checking for valid doubles
		  //it was done in getDialogValues
		  double bufferDistance = Double.parseDouble(bufferDistances[bufferNum]);

		  if (bufferDistance > 0)
		  {
			  ia = selectedFeatures.iterator();
			  Geometry featureGeo = ((Feature) selectedFeatures.iterator().next()).getGeometry();
			  bufferGeo = getBuffer(featureGeo, bufferDistance);
		    
			  while (ia.hasNext()) 
			  {
				  featureGeo = ((Feature) ia.next()).getGeometry();
				  Geometry result = getBuffer(featureGeo, bufferDistance);
		    	
				  if (result != null)
					  bufferGeo = bufferGeo.union(result);
			  }
		   	
			  Feature bufferFeature = new BasicFeature(featureSchema);
		    
//			  if (constructDonuts) 
				  bufferFeature.setGeometry(bufferGeo.difference(prevGeo));
//			  else
//				  bufferFeature.setGeometry(bufferGeo);
		    
			  bufferFeature.setAttribute(DISTANCEATTRIBUTE, new Double(bufferDistances[bufferNum]));
			  bufferFeature.setAttribute(attributeName, bufferAttributeValues[bufferNum]);
			  bufferFeatureCollection.add(bufferFeature);
			  prevGeo = bufferGeo;
		  }
	  }
	  return bufferFeatureCollection;
  }
  
  private Geometry getBuffer(Geometry a, double distance)
  {
    Geometry result = null;
    try {
      result = a.buffer(distance);// * conversionFactor);
      return result;
    }
    catch (RuntimeException ex) {
      // simply eat exceptions and report them by returning null
      exceptionThrown = true;
    }
    return null;
  }

  private void setDialogValues(final MultiInputDialog dialog, PlugInContext context)
  {
    dialog.setSideBarDescription( SELECTED_ONLY);
    dialog.addSeparator();
    dialog.addLabel(LAYEROPTIONS);
    dialog.addSeparator();
    dialog.addTextField(LAYERNAME, layerName, 30, null, "");
    dialog.addTextField(ATTRIBUTENAME, attributeName, 30, null, "");
    dialog.addCheckBox(LABEL, enableLabeling, "");
    dialog.addCheckBox(THEMING, enableTheming, "");
    dialog.addSeparator();
    dialog.addLabel(BUFFEROPTIONS);
    dialog.addSeparator();
    
    int maxVal = bufferDistances.length;
    SpinnerModel bufferSpinnerModel = new SpinnerNumberModel(1, 1, maxVal, 1);
	bufferSpinner = new JSpinner(bufferSpinnerModel);
	bufferSpinner.addChangeListener(new ChangeListener()
    {
        public void stateChanged(ChangeEvent e)
        {
            JSpinner spinner = (JSpinner)e.getSource();
            int spinVal = ((Integer)spinner.getValue()).intValue();
            bufferDistances[currBufferNumber-1] = currBufferDistance.getText().trim();
            bufferAttributeValues[currBufferNumber-1] = currAttributeValue.getText().trim();
            currBufferNumber = spinVal;
            currBufferDistance.setText("" + bufferDistances[spinVal-1]);
            currAttributeValue.setText(bufferAttributeValues[spinVal-1]);
        }
    });
	
	dialog.addRow(BUFFERNUMBER, new JLabel(BUFFERNUMBER), bufferSpinner, null, "");

	currBufferDistance = dialog.addTextField(BUFFERDISTANCE, bufferDistances[0], 10, null, "");
    currAttributeValue = dialog.addTextField(ATTRIBUTEVALUE, bufferAttributeValues[0], 30, null, "");
    JButton resetButton = dialog.addButton(sRESET);
    resetButton.addActionListener(new ResetButtonListener());
    currBufferNumber = 1;
    currBufferDistance.setText("" + bufferDistances[currBufferNumber - 1]);
    currAttributeValue.setText(bufferAttributeValues[currBufferNumber - 1]);
  }

  private boolean getDialogValues(MultiInputDialog dialog) {
      int spinVal = ((Integer)bufferSpinner.getValue()).intValue();
      bufferDistances[spinVal-1] = currBufferDistance.getText().trim();
      bufferAttributeValues[spinVal-1] = currAttributeValue.getText().trim();
	  layerName = dialog.getText(LAYERNAME);
	  attributeName = dialog.getText(ATTRIBUTENAME);
	  enableLabeling = dialog.getCheckBox(LABEL).isSelected();
	  enableTheming = dialog.getCheckBox(THEMING).isSelected();
	  
	  int bufNum = 0;
	  try
	  {
		  //check out the values before trying to use them
		  for (bufNum = 0; bufNum < bufferDistances.length; bufNum++)
		  {
			  double bufDist = Double.parseDouble(bufferDistances[bufNum]);	
			  if (bufDist < 0)
			  {
		          reportValidationError(dialog, BUFFER+" #" + (bufNum+1) + " < 0.");
		          return false;
			  }
		  }
	  }
      catch (NumberFormatException e)
      {
          reportValidationError(dialog, "\"" + bufferDistances[bufNum]
                  + "\" is an invalid double for buffer distance #" + (bufNum+1) + ".");
          return false;
      }
      return true;
  }
  
  public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(1));
  } 

  private class ResetButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e) 
    {
    	currBufferNumber = 1;
    	bufferSpinner.setValue(new Integer(currBufferNumber));    	
    	
    	for (int i = 0; i < bufferDistances.length; i++)
    	{
    	  bufferDistances[i] = "0";
    	  bufferAttributeValues[i] = "";
    	}
        currBufferDistance.setText("" + bufferDistances[currBufferNumber - 1]);
        currAttributeValue.setText(bufferAttributeValues[currBufferNumber - 1]);
    }
  }

  private void reportValidationError(MultiInputDialog dialog, String errorMessage)
  {
      JOptionPane.showMessageDialog(
      dialog,
      errorMessage,
      "JUMP",
      JOptionPane.ERROR_MESSAGE);
  }
  

}

