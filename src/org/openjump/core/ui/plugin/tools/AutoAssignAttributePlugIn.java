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
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import org.openjump.core.ui.plugin.AbstractUiPlugIn;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.*;

/**
* Assign a value to an attribute on a set of features.
* Value may be computed (auto-increment), copied from another
* attribute or set by the user
*/
public class AutoAssignAttributePlugIn extends AbstractUiPlugIn {
    
    private static String LAYER_COMBO_BOX = GenericNames.LAYER;
    private static String SELECTED_CHECK_BOX;
    private static String SELECT_ONLY_ON_ONE_LAYER;
    private static String TARGET_ATTRIBUTE_COMBO_BOX;
    
    private static String AUTOINC_CHECK_BOX;
    private static String INC_VALUE_EDIT_BOX;
    private static String AUTOINC_PATTERN_BOX;
    private static String AUTOINC_DESCRIPTION_1;
    private static String AUTOINC_DESCRIPTION_2;
    
    private static String FROM_SOURCE_CHECK_BOX;
    private static String SOURCE_COMBO_BOX;
    private static String FROM_SOURCE_DESCRIPTION;
    private static String SOURCE_DIFF_DESTINATION;
    
    private static String ASSIGN_VALUE_CHECK_BOX;
    private static String ASSIGN_VALUE_TEXT_BOX;
    private static String ASSIGN_VALUE_DESCRIPTION;
    
    private static String DESCRIPTION;
    
    private Layer layer;
    private boolean selectedFeaturesOnly = true;
    private String targetAttribute;
    private AttributeType destinationAttributeType;
    
    private boolean autoIncrement = false;
    private String pattern = "0";
    private int autoInc = 1;
    private int incValue;
    
    private String numeric;
    
    private boolean assignFromSource = false;
    private String sourceAttribute;
    
    private boolean assignValue = true;
    private String textToAssign;
	    
	public void initialize(PlugInContext context) throws Exception {
	    
	    context.getFeatureInstaller().addMainMenuPlugin(this,
	    	  new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES}, 
	    	  this.getName(), false, null,
	    	  createEnableCheck(context.getWorkbenchContext()));
	      
        SELECTED_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Selected-features-only");
        TARGET_ATTRIBUTE_COMBO_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Target-attribute");
        
        SOURCE_COMBO_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Source-attribute");
        FROM_SOURCE_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-from-other-attribute");
        FROM_SOURCE_DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.From-source-description");
        SOURCE_DIFF_DESTINATION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Source-and-destination-atributes-must-be-different");
        
        AUTOINC_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment");
        AUTOINC_PATTERN_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment-pattern");
        INC_VALUE_EDIT_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Increment-by");
        AUTOINC_DESCRIPTION_1 = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment-description-1");
        AUTOINC_DESCRIPTION_2 = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment-description-2");
        
        ASSIGN_VALUE_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-fixed-value");
        ASSIGN_VALUE_TEXT_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-value");
        ASSIGN_VALUE_DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-value-description");
        
        SELECT_ONLY_ON_ONE_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Select-features-on-only-one-layer");
        DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Description");
	}
	
	public boolean execute(PlugInContext context) throws Exception {
		MultiInputDialog dialog = prompt(context);
		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (!dialog.wasOKPressed()) {
			return false;
		}
		getDialogValues(dialog);
        reportNothingToUndoYet(context);
		assignValues(context);
		return true;
	}
	
    private MultiInputDialog prompt(PlugInContext context) {
        
        final MultiInputDialog dialog =
            new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    	dialog.setSideBarDescription(DESCRIPTION);
        
    	// Source layer and target attribute
        if (layer == null || !context.getLayerManager().getLayers().contains(layer) ||
                context.getLayerManager().getEditableLayers().contains(layer)) {
            layer = context.getLayerableNamePanel().chooseEditableLayer();
        }
        dialog.addLayerComboBox(LAYER_COMBO_BOX, layer, null,
                context.getLayerManager().getEditableLayers());

        boolean selectionExists = context.getLayerViewPanel()
                                         .getSelectionManager()
                                         .getFeaturesWithSelectedItems()
                                         .size() > 0;

        if (!selectionExists) selectedFeaturesOnly = false;
        dialog.addCheckBox(SELECTED_CHECK_BOX, selectedFeaturesOnly);
        dialog.setFieldVisible(SELECTED_CHECK_BOX, selectionExists);

        final JComboBox targetAttributeComboBox = 
            dialog.addAttributeComboBox(TARGET_ATTRIBUTE_COMBO_BOX, LAYER_COMBO_BOX,
                    new AttributeTypeFilter(
                            AttributeTypeFilter.DATE |
                                    AttributeTypeFilter.STRING |
                                    AttributeTypeFilter.DOUBLE |
                                    AttributeTypeFilter.INTEGER |
                                    AttributeTypeFilter.LONG |
                                    AttributeTypeFilter.BOOLEAN), "");

        for (int i = 0 ; i < targetAttributeComboBox.getModel().getSize() ; i++) {
            Object item = targetAttributeComboBox.getModel().getElementAt(i);
            if (item.equals(targetAttribute)) targetAttributeComboBox.setSelectedIndex(i);
        }

        // Set new value
        dialog.addSeparator();
        final JRadioButton assignValueRB = dialog.addRadioButton(ASSIGN_VALUE_CHECK_BOX, "MODE", assignValue, null);
        dialog.addTextField(ASSIGN_VALUE_TEXT_BOX, "", 15, null, "");

        // Set value from another attribute
        dialog.addSeparator();
        final JRadioButton fromSourceRB = dialog.addRadioButton(FROM_SOURCE_CHECK_BOX, "MODE", assignFromSource, null);
        final JComboBox sourceAttributeComboBox = 
            dialog.addAttributeComboBox(SOURCE_COMBO_BOX, LAYER_COMBO_BOX,
                                        AttributeTypeFilter.ALL_FILTER, 
                                        "");
        for (int i = 0 ; i < sourceAttributeComboBox.getModel().getSize() ; i++) {
            Object item = sourceAttributeComboBox.getModel().getElementAt(i);
            if (item.equals(sourceAttribute)) sourceAttributeComboBox.setSelectedIndex(i);
        }
        initEnableChecks(dialog);


        // Auto-incremented value
        dialog.addSeparator();
        final JRadioButton autoIncRB = dialog.addRadioButton(AUTOINC_CHECK_BOX, "MODE", autoIncrement, null);
        dialog.addTextField(AUTOINC_PATTERN_BOX, pattern, 4, null, AUTOINC_DESCRIPTION_2);
        dialog.addIntegerField(INC_VALUE_EDIT_BOX, 1, 4, "");

        // Update controls

        updateControls(dialog);

        targetAttributeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Layer layer = dialog.getLayer(LAYER_COMBO_BOX);
                FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
                String attribute = dialog.getText(TARGET_ATTRIBUTE_COMBO_BOX);
                if (schema.getAttributeType(attribute) == AttributeType.BOOLEAN && autoIncrement) {
                    assignValueRB.setSelected(true);
                }
                if (schema.getAttributeType(attribute) == AttributeType.DATE && autoIncrement) {
                    assignValueRB.setSelected(true);
                }
                updateControls(dialog);
            }
        });

        autoIncRB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        fromSourceRB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        assignValueRB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });

        return dialog;
    }
    
    private void updateControls(MultiInputDialog dialog) {
        layer = dialog.getLayer(LAYER_COMBO_BOX);
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();

        targetAttribute = dialog.getText(TARGET_ATTRIBUTE_COMBO_BOX);
        destinationAttributeType = schema.getAttributeType(schema.getAttributeIndex(targetAttribute));

        assignFromSource = dialog.getBoolean(FROM_SOURCE_CHECK_BOX);
        autoIncrement = dialog.getBoolean(AUTOINC_CHECK_BOX);
        assignValue = dialog.getBoolean(ASSIGN_VALUE_CHECK_BOX);

        //boolean fromAttributeValid = schema.getAttributeCount() > 1;
        
        //dialog.setFieldEnabled(AUTOINC_CHECK_BOX, !assignFromSource && !assignValue);
        dialog.setFieldEnabled(AUTOINC_PATTERN_BOX, autoIncrement);
        dialog.setFieldEnabled(INC_VALUE_EDIT_BOX, autoIncrement);
        
        //dialog.setFieldEnabled(FROM_SOURCE_CHECK_BOX, !autoIncrement && !assignValue && fromAttributeValid);
        dialog.setFieldEnabled(SOURCE_COMBO_BOX, assignFromSource);
        
        //dialog.setFieldEnabled(ASSIGN_VALUE_CHECK_BOX, !assignFromSource && !autoIncrement);
        dialog.setFieldEnabled(ASSIGN_VALUE_TEXT_BOX, assignValue);
        
        if (assignValue) dialog.setSideBarDescription(ASSIGN_VALUE_DESCRIPTION);
        else if (autoIncrement) dialog.setSideBarDescription(AUTOINC_DESCRIPTION_1 + "\n\n" + AUTOINC_DESCRIPTION_2);
        else if (assignFromSource) dialog.setSideBarDescription(FROM_SOURCE_DESCRIPTION);
        else dialog.setSideBarDescription(DESCRIPTION);
    }
    
    private void getDialogValues(MultiInputDialog dialog) {
		layer = dialog.getLayer(LAYER_COMBO_BOX);
		selectedFeaturesOnly = dialog.getBoolean(SELECTED_CHECK_BOX);
		FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
		targetAttribute = dialog.getText(TARGET_ATTRIBUTE_COMBO_BOX);
		destinationAttributeType = schema.getAttributeType(schema.getAttributeIndex(targetAttribute));
		
		autoIncrement = dialog.getBoolean(AUTOINC_CHECK_BOX);
		incValue = dialog.getInteger(INC_VALUE_EDIT_BOX);
	    pattern = dialog.getText(AUTOINC_PATTERN_BOX);
	    numeric = parseNumber(pattern);
	    
	    assignFromSource = dialog.getBoolean(FROM_SOURCE_CHECK_BOX);
		sourceAttribute = dialog.getText(SOURCE_COMBO_BOX);
        
	    assignValue = dialog.getBoolean(ASSIGN_VALUE_CHECK_BOX);
		textToAssign = dialog.getText(ASSIGN_VALUE_TEXT_BOX);
		
		if (autoIncrement) {
			if (numeric.length() == 0)
				autoInc = 0;
			else
				autoInc = Integer.parseInt(numeric);
		} else
	    	autoInc = 0;
    }
    
    private void initEnableChecks(final MultiInputDialog dialog) {
        dialog.addEnableChecks(SOURCE_COMBO_BOX,
            Collections.singletonList(new EnableCheck() {
                public String check(JComponent component) {
                    return assignFromSource && 
                           dialog.getText(TARGET_ATTRIBUTE_COMBO_BOX)
                                 .equals(dialog.getText(SOURCE_COMBO_BOX)) ? 
                                 SOURCE_DIFF_DESTINATION : null;
                }
        }));
    }

    private interface Converter {
        Object convert(String d);
    }

    private Map<AttributeType,Converter> typeToConverterMap = new HashMap<AttributeType,Converter>() {
        {
            put(AttributeType.STRING, new Converter() {
                public Object convert(String d) {
                    return d;
                }
            });
            put(AttributeType.INTEGER, new Converter() {
                public Object convert(String d) {
                    if (d==null) return null;
                    try {
                        return Integer.parseInt(d);
                    } catch(NumberFormatException nfe) {
                        return null;
                    }
                }
            });
            put(AttributeType.LONG, new Converter() {
                public Object convert(String d) {
                    if (d==null) return null;
                    try {
                        return Long.parseLong(d);
                    } catch(NumberFormatException nfe) {
                        return null;
                    }
                }
            });
            put(AttributeType.BOOLEAN, new Converter() {
                public Object convert(String d) {
                    if (d==null || d.isEmpty()) return null;
                    try {
                        return Boolean.parseBoolean(d);
                    } catch(NumberFormatException nfe) {
                        return null;
                    }
                }
            });
            put(AttributeType.DOUBLE, new Converter() {
                public Object convert(String d) {
                    if (d==null) return null;
                    try {
                        return Double.parseDouble(d);
                    } catch(NumberFormatException nfe) {
                        return null;
                    }
                }
            });
            put(AttributeType.DATE, new Converter() {
                final FlexibleDateParser parser = new FlexibleDateParser();
                public Object convert(String d) {
                    if (d==null) return null;
                    try {
                        return parser.parse(d, true);
                    } catch(ParseException nfe) {
                        return null;
                    }
                }
            });
        }
    };
 
    private String parseNumber(String text) {
        int b; int e;
    	for (int i=0; i<text.length(); i++) {
    		if (Character.isDigit(text.charAt(i))) {
		        b=i; e=i;
		   		while ( e < text.length() && Character.isDigit(text.charAt(e))) 
		   			e++;
		   		return text.substring(b, e);
    		}
    	}
    	return "";
    }

    private void assignValues(PlugInContext context) {
        //Iterator iterator;
        final Collection<Feature> newFeatures = new ArrayList<>();
        final Collection<Feature> oldFeatures = new ArrayList<>();
        final Collection<Feature> features;
        if (selectedFeaturesOnly) {
            Collection layers = context.getLayerViewPanel().getSelectionManager()
                    .getLayersWithSelectedItems();
            if (layers.size() > 1) {
                context.getWorkbenchFrame().warnUser(SELECT_ONLY_ON_ONE_LAYER);
            }
            features = context.getLayerViewPanel().getSelectionManager()
                    .getFeaturesWithSelectedItems();
        } else {
            features = layer.getFeatureCollectionWrapper().getFeatures();
        }
        context.getLayerManager().getUndoableEditReceiver().startReceiving();
        for (Feature feature : features) {
            String s;
            if (autoIncrement) {
                String value = "" + autoInc;
                autoInc += incValue;
                if (numeric.length() == 0)
                    s = value;
                else
                    s = pattern.replaceFirst(numeric, value);
            } else if (assignFromSource) {
                Object v = feature.getAttribute(sourceAttribute);
                s = v == null ? null : v.toString();
            } else {
                s = textToAssign;
            }
            Object object = typeToConverterMap.get(destinationAttributeType).convert(s);
            oldFeatures.add(feature.clone(false));
            Feature newFeature = feature.clone(false);
            newFeature.setAttribute(targetAttribute, object);
            newFeatures.add(newFeature);
        }
        layer.getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            UndoableCommand command =
                    new UndoableCommand(I18N.get(AutoAssignAttributePlugIn.class.getName())) {
                        public void execute() {
                            Iterator i = newFeatures.iterator();
                            for (Feature f : features) {
                                f.setAttribute(targetAttribute, ((Feature)i.next()).getAttribute(targetAttribute));
                            }
                            layer.getLayerManager().fireFeaturesAttChanged(features,
                                    FeatureEventType.ATTRIBUTES_MODIFIED, layer, oldFeatures);
                        }
                        public void unexecute() {
                            Iterator i = oldFeatures.iterator();
                            for (Feature f : features) {
                                f.setAttribute(targetAttribute, ((Feature)i.next()).getAttribute(targetAttribute));
                            }
                            layer.getLayerManager().fireFeaturesAttChanged(features,
                                    FeatureEventType.ATTRIBUTES_MODIFIED, layer, newFeatures);
                        }
                    };
            command.execute();
            layer.getLayerManager().getUndoableEditReceiver().receive(command.toUndoableEdit());
        } finally {
            layer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
    }


    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
            .add(checkFactory.createAtLeastOneVisibleLayersMustBeEditableCheck());
    }
}
