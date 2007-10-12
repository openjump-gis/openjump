
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

package org.openjump.sigle.plugin.replace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;



import org.openjump.sigle.utilities.gui.DialogUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


/**
 * 
* @author Erwan Bocher Laboratoire RESO UMR CNRS 6590
* @url www.projet-sigle.org
* @curentdate 25 juil. 06
* @package name org.openjump.sigle.plugin.replace
* @license Licence CeCILL http://www.cecill.info/
* @todo TODO
*
 */



public class ReplaceValuePlugIn
    extends AbstractPlugIn
    implements ThreadedPlugIn
{

 
  private MultiInputDialog dialog;
  //-- replace later with correct language
  private static String ATTRIBUTE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute");
  private static String VALUE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value");
  private static String ATTRIBUTE_SRC = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-src");
  private static String BY_ATTRIBUTE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value-by-copy");
  private static String TYPE = "";
  private static String SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
  private static String DESCRIPTION = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Description");

  private Layer layer;
  private String attrName;
  private String attrNameSrc;
  private String value = "";
  private boolean useSelected = true;
  private boolean byAttribute = false;
  public static final ImageIcon ICON = IconLoader.icon("Wrench.gif");


  public ReplaceValuePlugIn()
  {
   
  }
  
  public void initialize(PlugInContext context) {
		
	  context.getFeatureInstaller().addMainMenuItem(this,new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES }, 
  			this.getName(), false, null, 
  			createEnableCheck(context.getWorkbenchContext())
				); 
	   	
	}

  public String getName(){
  	return I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Replace-Attribute-Value");
  }
  
  public boolean execute(PlugInContext context) throws Exception {
//	  lemesre: duplicate from private initialisation
	  ATTRIBUTE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute");
	  VALUE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value");
	  ATTRIBUTE_SRC = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-src");
	  BY_ATTRIBUTE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value-by-copy");
	  SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
	  DESCRIPTION = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Description");

	  dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
	  setDialogValues(dialog, context);
	  if (layer.isEditable()){
		  GUIUtil.centreOnWindow(dialog);
		  dialog.setVisible(true);
		  if (! dialog.wasOKPressed()) { return false; }
		  getDialogValues(dialog);
		  return true;
	  }
	  else {
		  JOptionPane.showMessageDialog(dialog, I18N.get("ui.SchemaPanel.layer-must-be-editable"));
	  }
	  return false;

  }

  public void run(TaskMonitor monitor, PlugInContext context)
  throws Exception
  {

	  // input-proofing
	  if (layer == null) return;
	  if (attrName == null) return;
	  if (value == null && attrNameSrc == null) return;


	  List srcFeatures = layer.getFeatureCollectionWrapper().getFeatures();


	  if (useSelected){

		  Collection featureSelected = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer); ;

		  //System.out.println("Feature selected");
		  monitor.report(I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Replacing-values"));
		  if (byAttribute) {
			  replaceByAttributeValue(featureSelected, attrName,attrNameSrc);
		  } else {
			  replaceValue(featureSelected, attrName, value);
		  }

	  } else {

		  //System.out.println("All features");
		  if (byAttribute) {
			  replaceByAttributeValue(srcFeatures, attrName,attrNameSrc);
		  } else {
			  replaceValue(srcFeatures, attrName, value);
		  }

	  }
	  // TODO: look for FeatureEventType.ATTRIBUTE_MODIFIED 
	  //  it is probably better than Layer changed
	  // [eric lemesre]
	  layer.fireAppearanceChanged();
  }
  

  private void setDialogValues(final MultiInputDialog dialog, PlugInContext context)
  {
	  
	dialog.setSideBarDescription(DESCRIPTION);  
    //Initial layer value is null
    
    layer = context.getSelectedLayer(0);
    
    //  combos sélection d'un champ
    List columns = DialogUtil.getFieldsFromLayerWithoutGeometry(layer);
    String column1 = null;
    
    if (columns.size()>0)
    	column1 = (String) columns.get(0);  // récupération du premier attribut s'il existe
     
    
    dialog.addComboBox(ATTRIBUTE,column1,columns , "Attribut à modifier"); // TODO:I18N move to I18N [lemesre]
    
   
    final JTextField textfield = dialog.addTextField(
    		I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-type"), 
			(layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(column1)).toString(), 
			10, null, null);
    textfield.setEnabled(false);
    
    dialog.getComboBox(ATTRIBUTE).addActionListener(new ActionListener() { 
        public void actionPerformed(ActionEvent e) {
            // recuperation de la combo couche 
            JComboBox cb = (JComboBox) e.getSource();
            // recuperation de l'attribut selectionné
           String attributeName = (String) cb.getSelectedItem();
            
           textfield.setText((layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(attributeName)).toString());
                        
         }
		});
          
    
	  dialog.addCheckBox(BY_ATTRIBUTE, byAttribute);
	  dialog.addComboBox(ATTRIBUTE_SRC,column1,columns , "Nom du champ source de la valeur"); //TODO:I18N move to I18N [eric lemesre]
	  dialog.getComboBox(ATTRIBUTE_SRC).setEnabled(byAttribute);
    
	  final JTextField valuetextfield = dialog.addTextField(VALUE,value,20, null, null);
	  valuetextfield.setEnabled(!byAttribute);
    
	  dialog.getCheckBox(BY_ATTRIBUTE).addActionListener(new ActionListener() { 
		  public void actionPerformed(ActionEvent e) {
			  JCheckBox chk = (JCheckBox) e.getSource();
			  valuetextfield.setEnabled(!chk.isSelected());
			  // valuetextfield.setVisible(!chk.isSelected());
			  dialog.getComboBox(ATTRIBUTE_SRC).setEnabled(chk.isSelected()); 
			  // dialog.getComboBox(ATTRIBUTE_SRC).setVisible(chk.isSelected()); 
		  }
	  });
	
	  dialog.addCheckBox(SELECTED_ONLY, true);
   
  }
  
  private void getDialogValues(MultiInputDialog dialog) {

	  attrName = dialog.getText(ATTRIBUTE);	    
	  value = dialog.getText(VALUE);
	  useSelected = dialog.getBoolean(SELECTED_ONLY);
	  attrNameSrc = dialog.getText(ATTRIBUTE_SRC);
	  byAttribute = dialog.getBoolean(BY_ATTRIBUTE);

  }
  
  private void  replaceValue(Collection selectedFC, String attrName, String value){

	  AttributeType type;
	  type = ((Feature) selectedFC.iterator().next()).getSchema().getAttributeType(attrName);

	  for (Iterator i = selectedFC.iterator(); i.hasNext(); ) {
		  Feature f = (Feature) i.next();

		  if (byAttribute) {
			  // remplacement par la valeur de l'attribut selectionné

		  }else {
			  // remplacement par la valeur saisie
			  if (type == AttributeType.DOUBLE) {
				  f.setAttribute(attrName, new Double (value));

			  } else if (type == AttributeType.INTEGER)  {
				  f.setAttribute(attrName, new Integer (value)); 

			  } else if (type == AttributeType.STRING) {
				  f.setAttribute(attrName, new String (value)); 

			  } else {

			  }
		  }
	  }

  }
		  
  private void  replaceByAttributeValue(Collection selectedFC, String attrNameDest,
		  String attrNameSrc){

	  //AttributeType typeSrc;
	  AttributeType typeDest;
	  String AttrValue;
	  typeDest = ((Feature) selectedFC.iterator().next()).getSchema().getAttributeType(attrNameDest); 

	  for (Iterator i = selectedFC.iterator(); i.hasNext(); ) {
		  Feature f = (Feature) i.next();

		  AttrValue = (String) f.getAttribute(attrNameSrc);

		  if (byAttribute) {
			  // remplacement par la valeur de l'attribut selectionné
			  if (typeDest == AttributeType.DOUBLE) {		    
				  f.setAttribute(attrNameDest, new Double (AttrValue));

			  } else if (typeDest == AttributeType.INTEGER)  {
				  f.setAttribute(attrNameDest, new Integer (AttrValue)); 

			  } else if (typeDest == AttributeType.STRING) {
				  f.setAttribute(attrNameDest, new String (AttrValue)); 

			  } else {

			  }

		  }else {
			  
		  }
	  }

  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
  	EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
  	
  	return new MultiEnableCheck()
		.add(checkFactory.createAtLeastNLayersMustExistCheck(1))
		.add(checkFactory.createSelectedLayersMustBeEditableCheck());
  }

}
