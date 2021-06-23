
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

package org.openjump.core.ui.plugin.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

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
 * @version 25 juil. 06
 *
 * license Licence CeCILL http://www.cecill.info/
 * @deprecated this plugin is not undoable - moreover, it is redundant with AutoAssignAttribute
 */
@Deprecated
public class ReplaceValuePlugIn extends AbstractPlugIn implements ThreadedPlugIn {
 
  //-- replace later with correct language
  private static String ATTRIBUTE = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute");
  private static String VALUE = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value");
  private static String ATTRIBUTE_SRC = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-src");
  private static String BY_ATTRIBUTE = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value-by-copy");
  private static String SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
  private static String DESCRIPTION = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Description");

  private Layer layer;
  private String attrName;
  private String attrNameSrc;
  private String value = "";
  private boolean useSelected = true;
  private boolean byAttribute = false;
  public static final ImageIcon ICON = IconLoader.icon("Wrench.gif");


  public ReplaceValuePlugIn() {}
  
  public void initialize(PlugInContext context) {

	  context.getFeatureInstaller().addMainMenuPlugin(
	  		this,
			  new String[]{ MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES },
			  this.getName(), false, null,
			  createEnableCheck(context.getWorkbenchContext()));
  }

  public String getName(){
  	return I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Replace-Attribute-Value");
  }
  
  public boolean execute(PlugInContext context) throws Exception {
//	  lemesre: duplicate from private initialisation
	  //ATTRIBUTE = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute");
	  //VALUE = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value");
	  //ATTRIBUTE_SRC = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-src");
	  //BY_ATTRIBUTE = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.New-value-by-copy");
	  //SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
	  //DESCRIPTION = I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Description");

	  MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
	  setDialogValues(dialog, context);
	  if (layer.isEditable()){
		  GUIUtil.centreOnWindow(dialog);
		  dialog.setVisible(true);
		  if (! dialog.wasOKPressed()) { return false; }
		  getDialogValues(dialog);
		  return true;
	  }
	  else {
		  JOptionPane.showMessageDialog(dialog, I18N.getInstance().get("ui.SchemaPanel.layer-must-be-editable"));
	  }
	  return false;

  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

	  // input-proofing
	  if (layer == null) return;
	  if (attrName == null) return;
	  if (value == null && attrNameSrc == null) return;

	  List<Feature> srcFeatures = layer.getFeatureCollectionWrapper().getFeatures();

	  if (useSelected){

		  Collection<Feature> featureSelected =
				  context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer);
          if (featureSelected.size() == 0) {
              context.getWorkbenchFrame().warnUser(
              		I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Layer-has-no-feature-selected"));
              return;
          }
		  monitor.report(I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Replacing-values"));
		  if (byAttribute) {
			  replaceByAttributeValue(featureSelected, attrName,attrNameSrc);
		  } else {
			  replaceValue(featureSelected, attrName, value);
		  }

	  } else {

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
  

  private void setDialogValues(final MultiInputDialog dialog, PlugInContext context) {
	  
	dialog.setSideBarDescription(DESCRIPTION);  
    //Initial layer value is null
    
    layer = context.getSelectedLayer(0);
    
    //  combos field selection
    List columns = DialogUtil.getFieldsFromLayerWithoutGeometry(layer);
    String column1 = null;
    
    if (columns.size()>0)
    	column1 = (String) columns.get(0);  // get the first attribute if exists
     
    
    dialog.addComboBox(ATTRIBUTE,column1,columns , "Attribute to modify"); // TODO:I18N move to I18N [lemesre]
    
   
    final JTextField textfield = dialog.addTextField(
    		I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-type"), 
			(layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(column1)).toString(), 
			10, null, null);
    textfield.setEnabled(false);
    
    dialog.getComboBox(ATTRIBUTE).addActionListener(new ActionListener() { 
        public void actionPerformed(ActionEvent e) {
            // get the combo for the layer
            JComboBox cb = (JComboBox) e.getSource();
            // get the selected attribute
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
			  dialog.getComboBox(ATTRIBUTE_SRC).setEnabled(chk.isSelected());
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
  
  private void replaceValue(Collection<Feature> selectedFC, String attrName, String value){

	  AttributeType type =
			  selectedFC.iterator().next().getSchema().getAttributeType(attrName);

	  for (Feature f : selectedFC) {

		  if (!byAttribute) {
		  	try {
				// remplacement par la valeur saisie
				if (type != AttributeType.STRING && value == null || value.trim().isEmpty()) {
					f.setAttribute(attrName, null);
				} else if (type == AttributeType.DOUBLE) {
					f.setAttribute(attrName, new Double(value));
				} else if (type == AttributeType.INTEGER) {
					f.setAttribute(attrName, new Integer(value));
				} else if (type == AttributeType.LONG) {
					f.setAttribute(attrName, new Long(value));
				} else if (type == AttributeType.BOOLEAN) {
					f.setAttribute(attrName, Boolean.parseBoolean(value));
				} else if (type == AttributeType.STRING) {
					f.setAttribute(attrName, value);
				}
			} catch(NumberFormatException e) {
				f.setAttribute(attrName, null);
			}
		  }
	  }

  }
		  
  private void  replaceByAttributeValue(Collection<Feature> selectedFC, String attrNameDest,
		  String attrNameSrc){

	  AttributeType typeDest =
			  selectedFC.iterator().next().getSchema().getAttributeType(attrNameDest);

	  for (Feature f : selectedFC) {

		  String attrValue = f.getString(attrNameSrc);

		  if (byAttribute) {
			  // replace by the value of selected attribute
			  if (typeDest == AttributeType.DOUBLE) {		    
				  f.setAttribute(attrNameDest, new Double (attrValue));
			  } else if (typeDest == AttributeType.INTEGER)  {
				  f.setAttribute(attrNameDest, new Integer (attrValue));

			  } else if (typeDest == AttributeType.STRING) {
				  f.setAttribute(attrNameDest, attrValue);
			  } else if (typeDest == AttributeType.LONG) {
				  f.setAttribute(attrNameDest, new Long(attrValue));
			  } else if (typeDest == AttributeType.BOOLEAN) {
				  f.setAttribute(attrNameDest, Boolean.parseBoolean(attrValue));
			  }
		  }
	  }

  }

  public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
  	EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
  	return new MultiEnableCheck()
        .add(checkFactory.createExactlyOneSelectedLayerMustBeEditableCheck())
        .add(new EnableCheck(){
            public String check(JComponent component) {
                Layer[] layers = workbenchContext.getLayerableNamePanel().getSelectedLayers();
                Layer layer = null;
                for (Layer lyr : layers) {
                    if (lyr.isEditable()) {
                        layer = lyr;
                        break;
                    }
                }
                if (layer == null) return I18N.getInstance().get("com.vividsolutions.jump.workbench.plugin.Exactly-one-selected-layer-must-be-editable");
                if (layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount() < 2) {
                    return I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Layer-has-no-attribute");
                }
                if (layer.getFeatureCollectionWrapper().size() == 0) {
                    return I18N.getInstance().get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Layer-has-no-feature");
                }
                return null;
            }
        });
  }

}
