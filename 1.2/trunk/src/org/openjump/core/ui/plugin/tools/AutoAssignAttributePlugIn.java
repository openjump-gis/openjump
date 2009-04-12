/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2008 Integrated Systems Analysts, Inc.
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
 */
package org.openjump.core.ui.plugin.tools;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
* Based on CalculateAreasAndLengthsPlugIn.
*
*/
public class AutoAssignAttributePlugIn extends AbstractPlugIn {
		//TODO: translation
	    private static String LAYER_COMBO_BOX = GenericNames.LAYER;
	    private static String DEST_COMBO_BOX = "Destination attribute";
	    private static String SOURCE_COMBO_BOX = "Source attribute";
	    private static String FROM_SOURCE_CHECK_BOX = "Assign from other attribute";
	    private static final String A_CHECK_BOX = "invisible checkbox";
	    private static String AUTOINC_CHECK_BOX = "Auto-increment";
	    private static String INC_VALUE_EDIT_BOX = "Increment by";
	    private static String SELECTED_CHECK_BOX = "Selected features only";
	    private static String ASSIGN_VALUE_TEXT_BOX = "Assign this value";
	    private static String SELECTONLYONONELAYER = "Select features on only one layer";
        private static String DESCRIPTION = "Assign a value from another attribute, " +
                       "a value, an auto-increment number or a combination of a value " +
                       "and an auto-increment number";
	    //private static String DESCRIPTION = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Description");
	    private int autoInc;
	    private Layer destinationLayer;
	    private FeatureSchema schema;
	    private String attributeName;
	    private String sourceAttributeName;
	    private int destinationAttributeIndex;
	    private int sourceAttributeIndex;
	    private AttributeType attributeType;
	    private boolean autoIncrement;
	    private int incValue;
	    private boolean assignFromSource;
	    private boolean selectedFeaturesOnly;
	    private String textToAssign;
	    private String numeric;
        private JTextField incfield = null;

	public void initialize(PlugInContext context) throws Exception
	{     
	      context.getFeatureInstaller().addMainMenuItem(this,
	    	      new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES}, 
	    	      this.getName() + "...", false, null, 
	    	      this.createEnableCheck(context.getWorkbenchContext()));
	      
		    DEST_COMBO_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Destination-attribute");
		    SOURCE_COMBO_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Source-attribute");
		    FROM_SOURCE_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-from-other-attribute");
		    //A_CHECK_BOX = "invisible checkbox";
		    AUTOINC_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment");
		    INC_VALUE_EDIT_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Increment-by");
		    SELECTED_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Selected-features-only");
		    ASSIGN_VALUE_TEXT_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-this-value");
		    SELECTONLYONONELAYER = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Select-features-on-only-one-layer");
			DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Description");
            //DESCRIPTION = I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Description");
	}

	public String getName(){
		return I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-Assign-Attribute");
	}
	
	public boolean execute(PlugInContext context) throws Exception {
		MultiInputDialog dialog = prompt(context);
		if (!dialog.wasOKPressed()) {
			return false;
		}
		getDialogValues(dialog);
		assignValues(context);
		return true;
	}
	
    private MultiInputDialog prompt(PlugInContext context) {
        final MultiInputDialog dialog =
            new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    	dialog.setSideBarDescription(DESCRIPTION);  
        dialog.addEditableLayerComboBox(
            LAYER_COMBO_BOX,
            null,
            null,
            context.getLayerManager());
        initComboFields(dialog, A_CHECK_BOX, DEST_COMBO_BOX, 0);
        dialog.addCheckBox(AUTOINC_CHECK_BOX, true);
        incfield = dialog.addIntegerField(INC_VALUE_EDIT_BOX, 1, 4, "Auto-increment number by this value");
        dialog.indentLabel(INC_VALUE_EDIT_BOX);
        initComboFields(dialog, FROM_SOURCE_CHECK_BOX, SOURCE_COMBO_BOX, 1);
        dialog.getCheckBox(FROM_SOURCE_CHECK_BOX).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.getComboBox(SOURCE_COMBO_BOX).setEnabled(
                    dialog.getCheckBox(FROM_SOURCE_CHECK_BOX).isSelected());
                boolean fromSelected = dialog.getCheckBox(FROM_SOURCE_CHECK_BOX).isSelected();
                dialog.getLabel(SOURCE_COMBO_BOX).setEnabled(fromSelected);
                JCheckBox checkbox = dialog.getCheckBox(AUTOINC_CHECK_BOX);
                checkbox.setEnabled(!fromSelected);
                incfield.setEnabled(!fromSelected);
                if (fromSelected)
                	checkbox.setSelected(false);
            }
        });

        initEnableChecks(dialog);
        boolean selectionExists = context.getLayerViewPanel().getSelectionManager()
        	.getFeatureSelection().getSelectedItems().size() > 0;
        dialog.addCheckBox(SELECTED_CHECK_BOX, selectionExists);
        dialog.addTextField(ASSIGN_VALUE_TEXT_BOX, "", 15, null, 
        		"Enter value to assign. Numeric portion used as start value.");
        loadValues(dialog, context);
        dialog.getCheckBox(A_CHECK_BOX).setVisible(false); //*LDB
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {
            saveValues(dialog, context);
        }
        return dialog;
    }
    
    private void getDialogValues(MultiInputDialog dialog) {
		destinationLayer = dialog.getLayer(LAYER_COMBO_BOX);
		attributeName = dialog.getText(DEST_COMBO_BOX);
		sourceAttributeName = dialog.getText(SOURCE_COMBO_BOX);
		schema = destinationLayer.getFeatureCollectionWrapper().getFeatureSchema();
		destinationAttributeIndex = schema.getAttributeIndex(attributeName);
		sourceAttributeIndex = schema.getAttributeIndex(sourceAttributeName);
        attributeType = schema.getAttributeType(destinationAttributeIndex);
	    autoIncrement = dialog.getBoolean(AUTOINC_CHECK_BOX);
	    incValue = dialog.getInteger(INC_VALUE_EDIT_BOX);
	    selectedFeaturesOnly = dialog.getBoolean(SELECTED_CHECK_BOX);
		textToAssign = dialog.getText(ASSIGN_VALUE_TEXT_BOX);
		numeric = parseNumber(textToAssign);
		if (autoIncrement) {
			if (numeric.length() == 0)
				autoInc = 0;
			else
				autoInc = new Integer(numeric).intValue();
		} else
	    	autoInc = 0;
		assignFromSource = dialog.getBoolean(FROM_SOURCE_CHECK_BOX);   	
    }
    
    private void saveValues(MultiInputDialog dialog, PlugInContext context) {
        Blackboard blackboard = context.getLayerManager().getBlackboard();
        blackboard.put(namespace() + LAYER_COMBO_BOX, dialog.getLayer(LAYER_COMBO_BOX));
        blackboard.put(
            namespace() + FROM_SOURCE_CHECK_BOX,
            dialog.getCheckBox(FROM_SOURCE_CHECK_BOX).isSelected());
        blackboard.put(
            namespace() + DEST_COMBO_BOX,
            dialog.getComboBox(DEST_COMBO_BOX).getSelectedItem());
        blackboard.put(
            namespace() + SOURCE_COMBO_BOX,
            dialog.getComboBox(SOURCE_COMBO_BOX).getSelectedItem());
    }
    
    private void loadValues(MultiInputDialog dialog, PlugInContext context) {
        Blackboard blackboard = context.getLayerManager().getBlackboard();
        dialog.getComboBox(LAYER_COMBO_BOX).setSelectedItem(
            CollectionUtil.ifNotIn(
                blackboard.get(namespace() + LAYER_COMBO_BOX),
                GUIUtil.items(dialog.getComboBox(LAYER_COMBO_BOX)),
                candidateLayer(context)));
        GUIUtil.setSelectedWithClick(
            dialog.getCheckBox(FROM_SOURCE_CHECK_BOX),
            blackboard.get(namespace() + FROM_SOURCE_CHECK_BOX, false));
        dialog.getComboBox(DEST_COMBO_BOX).setSelectedItem(
            CollectionUtil.ifNotIn(
                blackboard.get(namespace() + DEST_COMBO_BOX),
                GUIUtil.items(dialog.getComboBox(DEST_COMBO_BOX)),
                dialog.getComboBox(DEST_COMBO_BOX).getSelectedItem()));
        dialog.getComboBox(SOURCE_COMBO_BOX).setSelectedItem(
            CollectionUtil.ifNotIn(
                blackboard.get(namespace() + SOURCE_COMBO_BOX),
                GUIUtil.items(dialog.getComboBox(SOURCE_COMBO_BOX)),
                dialog.getComboBox(SOURCE_COMBO_BOX).getSelectedItem()));
    }
    
    private String namespace() {
        return getClass().getName() + " - ";
    }
    
    private void initEnableChecks(final MultiInputDialog dialog) {
        dialog
            .addEnableChecks(
                SOURCE_COMBO_BOX,
                Arrays
                    .asList(
                        new Object[] {
                            new EnableCheck() {
                                public String check(JComponent component) {
                                return dialog.getBoolean(FROM_SOURCE_CHECK_BOX)
                                    && dialog.getText(DEST_COMBO_BOX).equals(
                                        dialog.getText(SOURCE_COMBO_BOX))
                                        ? "Source and destination attributes must be different"
                                        : null;
                }
            }
        }));
    }
    
    private String attributeName(List attributeNames, int preferredIndex) {
        return (String) attributeNames.get(
            attributeNames.size() > preferredIndex ? preferredIndex : 0);
    }
    
    private void initComboFields(
        final MultiInputDialog dialog,
        final String checkBoxFieldName,
        final String comboBoxFieldName,
        final int preferredCandidateAttributeIndex) {
        dialog.addCheckBox(checkBoxFieldName, true);
        dialog.addComboBox(comboBoxFieldName, null, new ArrayList(), null);
        dialog.getComboBox(LAYER_COMBO_BOX).addActionListener(new ActionListener() {
            private Layer lastLayer = null;
            public void actionPerformed(ActionEvent e) {
                Layer newLayer =
                    (Layer) dialog.getComboBox(LAYER_COMBO_BOX).getSelectedItem();
                if (lastLayer == newLayer) {
                    return;
                }
                lastLayer = newLayer;
                dialog.getComboBox(comboBoxFieldName).setModel(
                    new DefaultComboBoxModel(
                        new Vector(candidateAttributeNames(newLayer))));
                if (!candidateAttributeNames(newLayer).isEmpty()) {
                    dialog.getComboBox(comboBoxFieldName).setSelectedItem(
                        attributeName(
                            candidateAttributeNames(newLayer),
                            preferredCandidateAttributeIndex));
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
                                        == 0
                                        ? "Layer has no string, integer, or double attributes"
                                        : null;
                }
            }
        }));
        dialog.indentLabel(comboBoxFieldName);
    }
    
    private Layer candidateLayer(PlugInContext context) {
        if (context.getActiveInternalFrame() instanceof LayerNamePanelProxy) {
            Layer[] selectedLayers = context.getSelectedLayers();
            for (int i = 0; i < selectedLayers.length; i++) {
                if (selectedLayers[i].isEditable()) {
                    return selectedLayers[i];
                }
            }
        }
        return (Layer) context.getLayerManager().getEditableLayers().iterator().next();
    }

    private static interface Converter {
        public Object convert(String d);
    }
    
    private Map typeToConverterMap = new HashMap() {
        {
            put(AttributeType.STRING, new Converter() {
                public Object convert(String d) {
                    return d;
                }
            });
            put(AttributeType.INTEGER, new Converter() {
                public Object convert(String d) {
                	String s = parseNumber(d);
                	if (s.length() == 0) 
                		return new Integer(0);
                    return new Integer(s);
                }
            });
            put(AttributeType.DOUBLE, new Converter() {
                public Object convert(String d) {
                	String s = parseNumber(d);
                	if (s.length() == 0) 
                		return new Double(0);
                    return new Double(parseNumber(d));
                }
            });
        }
    };
    
    private List candidateAttributeNames(Layer layer) {
        ArrayList candidateAttributeNames = new ArrayList();
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            if (typeToConverterMap.keySet().contains(schema.getAttributeType(i))) {
                candidateAttributeNames.add(schema.getAttributeName(i));
            }
        }
        return candidateAttributeNames;
    }
 
    private String parseNumber(String assignText) {
        int b=0; int e=0;
    	for (int i=0; i<assignText.length(); i++) {
    		if ( Character.isDigit(assignText.charAt(i)) ) {
		        b=i; e=i;
		   		while ( e < assignText.length() && Character.isDigit(assignText.charAt(e)) ) 
		   			e++;
		   		return assignText.substring(b, e);
    		}
    	}
    	return "";
    }
     
    private void assignValues(PlugInContext context) {
    	Iterator iterator;
    	if (selectedFeaturesOnly) {
    		Collection layers = context.getLayerViewPanel().getSelectionManager()
    		.getLayersWithSelectedItems();
    		if (layers.size() > 1) {
    			context.getWorkbenchFrame().warnUser(SELECTONLYONONELAYER);
    		}
    		iterator = context.getLayerViewPanel().getSelectionManager()
    		.getFeaturesWithSelectedItems().iterator();
    	} else {
    		iterator = destinationLayer.getFeatureCollectionWrapper().getFeatures().iterator();
    	}
    	for (Iterator i = iterator; i.hasNext(); ) {
    		Feature feature = (Feature) i.next();
    		String s = textToAssign;
    		if (autoIncrement) {
    			String value = "" + autoInc;
    			autoInc += incValue;
    			if (numeric.length() == 0)
    				s = value;
    			else
    				s = textToAssign.replaceFirst(numeric, value);
    		} else {
    			if (assignFromSource) {
    				s = feature.getAttribute(sourceAttributeIndex).toString();
    	   			if (numeric.length() > 0)
         				s = textToAssign.replaceFirst(numeric, s);
        			}
    		}
    		Object object = ((Converter) typeToConverterMap.get(attributeType)).convert(s);
    		feature.setAttribute( destinationAttributeIndex, object);

    	}
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
            .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1));
    }
}
