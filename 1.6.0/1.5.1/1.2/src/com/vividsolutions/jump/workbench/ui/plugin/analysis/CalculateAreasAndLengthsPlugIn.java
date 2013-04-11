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
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
* Calculates areas and lengths from information obtained
* from the user via a {@link MultiInputDialog}.
*
*/
public class CalculateAreasAndLengthsPlugIn extends AbstractPlugIn {
	
    private String LAYER_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.layer");
    private String AREA_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.area-attribute-name");
    private String LENGTH_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.length-attribute-name");
    private String LENGTH_CHECK_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.calculate-length");
    private String AREA_CHECK_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.calculate-area");

	
    public boolean execute(PlugInContext context) throws Exception {
    	//[sstein, 16.07.2006] set again to obtain correct language
        LAYER_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.layer");
        AREA_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.area-attribute-name");
        LENGTH_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.length-attribute-name");
        LENGTH_CHECK_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.calculate-length");
        AREA_CHECK_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.calculate-area");

        //<<TODO>> Undo? [Jon Aquino]
        //<<TODO>> Two-phase commit? [Jon Aquino]
        MultiInputDialog dialog = prompt(context);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        if (dialog.getBoolean(AREA_CHECK_BOX)) {
            updateAreas(dialog.getLayer(LAYER_COMBO_BOX), dialog.getText(AREA_COMBO_BOX));
        }
        if (dialog.getBoolean(LENGTH_CHECK_BOX)) {
            updateLengths(
                dialog.getLayer(LAYER_COMBO_BOX),
                dialog.getText(LENGTH_COMBO_BOX));
        }
        context.getLayerManager().fireFeaturesChanged(
            dialog.getLayer(LAYER_COMBO_BOX).getFeatureCollectionWrapper().getFeatures(),
            FeatureEventType.ATTRIBUTES_MODIFIED,
            dialog.getLayer(LAYER_COMBO_BOX));
        return true;
    }
    private MultiInputDialog prompt(PlugInContext context) {
        final MultiInputDialog dialog =
            new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        dialog.addEditableLayerComboBox(
            LAYER_COMBO_BOX,
            null,
            null,
            context.getLayerManager());
        initFields(dialog, AREA_CHECK_BOX, AREA_COMBO_BOX, 0);
        initFields(dialog, LENGTH_CHECK_BOX, LENGTH_COMBO_BOX, 1);
        initEnableChecks(dialog);
        loadValues(dialog, context);
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {
            saveValues(dialog, context);
        }
        return dialog;
    }
    private void saveValues(MultiInputDialog dialog, PlugInContext context) {
        Blackboard blackboard = context.getLayerManager().getBlackboard();
        blackboard.put(namespace() + LAYER_COMBO_BOX, dialog.getLayer(LAYER_COMBO_BOX));
        blackboard.put(
            namespace() + AREA_CHECK_BOX,
            dialog.getCheckBox(AREA_CHECK_BOX).isSelected());
        blackboard.put(
            namespace() + LENGTH_CHECK_BOX,
            dialog.getCheckBox(LENGTH_CHECK_BOX).isSelected());
        blackboard.put(
            namespace() + AREA_COMBO_BOX,
            dialog.getComboBox(AREA_COMBO_BOX).getSelectedItem());
        blackboard.put(
            namespace() + LENGTH_COMBO_BOX,
            dialog.getComboBox(LENGTH_COMBO_BOX).getSelectedItem());
    }
    private void loadValues(MultiInputDialog dialog, PlugInContext context) {
        Blackboard blackboard = context.getLayerManager().getBlackboard();
        dialog.getComboBox(LAYER_COMBO_BOX).setSelectedItem(
            CollectionUtil.ifNotIn(
                blackboard.get(namespace() + LAYER_COMBO_BOX),
                GUIUtil.items(dialog.getComboBox(LAYER_COMBO_BOX)),
                candidateLayer(context)));
        GUIUtil.setSelectedWithClick(
            dialog.getCheckBox(AREA_CHECK_BOX),
            blackboard.get(namespace() + AREA_CHECK_BOX, true));
        GUIUtil.setSelectedWithClick(
            dialog.getCheckBox(LENGTH_CHECK_BOX),
            blackboard.get(namespace() + LENGTH_CHECK_BOX, true));
        dialog.getComboBox(AREA_COMBO_BOX).setSelectedItem(
            CollectionUtil.ifNotIn(
                blackboard.get(namespace() + AREA_COMBO_BOX),
                GUIUtil.items(dialog.getComboBox(AREA_COMBO_BOX)),
                dialog.getComboBox(AREA_COMBO_BOX).getSelectedItem()));
        dialog.getComboBox(LENGTH_COMBO_BOX).setSelectedItem(
            CollectionUtil.ifNotIn(
                blackboard.get(namespace() + LENGTH_COMBO_BOX),
                GUIUtil.items(dialog.getComboBox(LENGTH_COMBO_BOX)),
                dialog.getComboBox(LENGTH_COMBO_BOX).getSelectedItem()));
    }
    private String namespace() {
        return getClass().getName() + " - ";
    }
    private void initEnableChecks(final MultiInputDialog dialog) {
        dialog
            .addEnableChecks(
                LENGTH_COMBO_BOX,
                Arrays
                    .asList(
                        new Object[] {
                            new EnableCheck() {
                                public String check(JComponent component) {
                                return dialog.getBoolean(AREA_CHECK_BOX)
                                    && dialog.getBoolean(LENGTH_CHECK_BOX)
                                    && dialog.getText(AREA_COMBO_BOX).equals(
                                        dialog.getText(LENGTH_COMBO_BOX))
                                        ? I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.area-and-length-attribute-names-must-be-different")
                                        : null;
                }
            }
        }));
    }
    private String attributeName(List attributeNames, int preferredIndex) {
        return (String) attributeNames.get(
            attributeNames.size() > preferredIndex ? preferredIndex : 0);
    }
    private void initFields(
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
        dialog.getCheckBox(checkBoxFieldName).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.getComboBox(comboBoxFieldName).setEnabled(
                    dialog.getCheckBox(checkBoxFieldName).isSelected());
                dialog.getLabel(comboBoxFieldName).setEnabled(
                    dialog.getCheckBox(checkBoxFieldName).isSelected());
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
        public Object convert(double d);
    }
    private Map typeToConverterMap = new HashMap() {
        {
            put(AttributeType.STRING, new Converter() {
                public Object convert(double d) {
                    return "" + d;
                }
            });
            put(AttributeType.INTEGER, new Converter() {
                public Object convert(double d) {
                    return new Integer((int) d);
                }
            });
            put(AttributeType.DOUBLE, new Converter() {
                public Object convert(double d) {
                    return new Double(d);
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
    private static interface Op {
        public double compute(Geometry g);
    }
    private void updateLengths(Layer layer, String attributeName) {
        update(layer, attributeName, new Op() {
            public double compute(Geometry g) {
                return g.getLength();
            }
        });
    }
    private void update(Layer layer, String attributeName, Op op) {
        int attributeIndex =
            layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeIndex(
                attributeName);
        AttributeType attributeType =
            layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(
                attributeIndex);
        for (Iterator i = layer.getFeatureCollectionWrapper().getFeatures().iterator();
            i.hasNext();
            ) {
            Feature feature = (Feature) i.next();
            feature.setAttribute(
                attributeIndex,
                convert(op.compute(feature.getGeometry()), attributeType));
        }
    }
    private Object convert(double d, AttributeType attributeType) {
        return ((Converter) typeToConverterMap.get(attributeType)).convert(d);
    }
    private void updateAreas(Layer layer, String attributeName) {
        update(layer, attributeName, new Op() {
            public double compute(Geometry g) {
                return g.getArea();
            }
        });
    }
    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
            .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1));
    }
}
