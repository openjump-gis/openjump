
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
  private static String ATTRIBUTE = GenericNames.ATTRIBUTE;
  private static String VALUE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value");
  private static String TYPE = "";
  private static String SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
	

  private Layer layer;
  private String attrName;
  private String value = "";
  private boolean useSelected = false;
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
    
    ATTRIBUTE = GenericNames.ATTRIBUTE;
    VALUE = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value");
    SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
	  
	dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
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
	    if (value == null) return;
	    
	    
	    List srcFeatures = layer.getFeatureCollectionWrapper().getFeatures();
			    
	  
	    if (useSelected){
	    	
	    Collection featureSelected = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer); ;
	    
	    //System.out.println("Feature selected");
	    monitor.report(I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Replacing-values"));
	    replaceValue(featureSelected, attrName, value);
	    
	    }
	    
	    else {
	    	
	    	//System.out.println("All features");
		    replaceValue(srcFeatures, attrName, value);
	    }
	    
	   
	    
   }

 

  

  private void setDialogValues(final MultiInputDialog dialog, PlugInContext context)
  {
	  
	  
    //Initial layer value is null
    
    layer = context.getSelectedLayer(0);
    
    //  combos sélection d'un champ
    List columns = DialogUtil.getFieldsFromLayerWithoutGeometry(layer);
    String column1 = null;
    
    if (columns.size()>0)
    	column1 = (String) columns.get(0);  // récupération du premier attribut s'il existe
     
    
    dialog.addComboBox(ATTRIBUTE,column1,columns , "Toto");
    
   
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
          
    
    dialog.addTextField(VALUE, value, 20, null, null);
    
    dialog.addCheckBox(SELECTED_ONLY, useSelected);
    
	
   
  }
  
  private void getDialogValues(MultiInputDialog dialog) {
	    
	    attrName = dialog.getText(ATTRIBUTE);	    
	    value = dialog.getText(VALUE);
	    useSelected = dialog.getBoolean(SELECTED_ONLY);
	
	  }
  
  private void  replaceValue(Collection selectedFC, String attrName,
	      String value){
	  
	  AttributeType type;
	  
	  for (Iterator i = selectedFC.iterator(); i.hasNext(); ) {
		  Feature f = (Feature) i.next();
		  		 
		  type = f.getSchema().getAttributeType(attrName);
		  
		  if (type == AttributeType.DOUBLE) {
		    
				  f.setAttribute(attrName, new Double (value));
		  
		  }
		  
		  else if (type == AttributeType.INTEGER)
		  
		  {
			  f.setAttribute(attrName, new Integer (value)); 
			  
		  }
		  else if (type == AttributeType.STRING) {
			  
			  f.setAttribute(attrName, new String (value)); 
			  
		  }
		  
		  else {
			  
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
