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
import javax.swing.JMenuItem;

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
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Calculates areas and lengths from information obtained
 * from the user via a {@link MultiInputDialog}.
 */
public class CalculateAreasAndLengthsPlugIn extends AbstractPlugIn {
	
    private static String LAYER_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.layer");
    private static String AREA_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.area-attribute-name");
    private static String LENGTH_COMBO_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.length-attribute-name");
    private static String LENGTH_CHECK_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.calculate-length");
    private static String AREA_CHECK_BOX = I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.calculate-area");

    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(
            this,
            new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES},
            this.getName() + "...", false, null,
            createEnableCheck(context.getWorkbenchContext())); 
    }

    private Layer layer;
    private boolean setLength, setArea;
    private String lengthAttribute, areaAttribute;
    
    public boolean execute(PlugInContext context) throws Exception {

        //<<TODO>> Undo? [Jon Aquino]
        //<<TODO>> Two-phase commit? [Jon Aquino]
        MultiInputDialog dialog = prompt(context);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        if (setArea) {
            updateAreas(layer, areaAttribute);
        }
        if (setLength) {
            updateLengths(layer, lengthAttribute);
        }
        context.getLayerManager().fireFeaturesChanged(
            layer.getFeatureCollectionWrapper().getFeatures(),
            FeatureEventType.ATTRIBUTES_MODIFIED, layer);
        return true;
    }

    private MultiInputDialog prompt(PlugInContext context) {

        final MultiInputDialog dialog =
            new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);

        dialog.addLayerComboBox(LAYER_COMBO_BOX,
                context.getLayerableNamePanel().chooseEditableLayer(), "",
                context.getLayerManager().getEditableLayers());
        dialog.addCheckBox(AREA_CHECK_BOX, setArea);
        dialog.addAttributeComboBox(AREA_COMBO_BOX, LAYER_COMBO_BOX, AttributeTypeFilter.NUMSTRING_FILTER,"");
        dialog.addCheckBox(LENGTH_CHECK_BOX, setLength);
        dialog.addAttributeComboBox(LENGTH_COMBO_BOX, LAYER_COMBO_BOX, AttributeTypeFilter.NUMSTRING_FILTER,"");

        initEnableChecks(dialog);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        return dialog;
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer = dialog.getLayer(LAYER_COMBO_BOX);
        setLength = dialog.getBoolean(LENGTH_CHECK_BOX);
        lengthAttribute = dialog.getText(LENGTH_COMBO_BOX);
        setArea = dialog.getBoolean(AREA_CHECK_BOX);
        areaAttribute = dialog.getText(AREA_COMBO_BOX);
    }

    private void initEnableChecks(final MultiInputDialog dialog) {
        dialog.addEnableChecks(
                LENGTH_COMBO_BOX, new EnableCheck() {
                    public String check(JComponent component) {
                        return dialog.getBoolean(AREA_CHECK_BOX)
                                && dialog.getBoolean(LENGTH_CHECK_BOX)
                                && dialog.getText(AREA_COMBO_BOX).equals(
                                    dialog.getText(LENGTH_COMBO_BOX))
                                        ? I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.area-and-length-attribute-names-must-be-different")
                                        : null;
                }
            }
        );
    }

    private interface Converter {
        Object convert(double d);
    }

    private Map<AttributeType,Converter> typeToConverterMap =
            new HashMap<AttributeType,Converter>() {
        {
            put(AttributeType.STRING, new Converter() {
                public Object convert(double d) {
                    return "" + d;
                }
            });
            put(AttributeType.INTEGER, new Converter() {
                public Object convert(double d) {
                    return (int)d;
                }
            });
            put(AttributeType.LONG, new Converter() {
                public Object convert(double d) {
                    return (long)d;
                }
            });
            put(AttributeType.DOUBLE, new Converter() {
                public Object convert(double d) {
                    return d;
                }
            });
        }
    };

    private interface Op {
        double compute(Geometry g);
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
        for (Feature feature : layer.getFeatureCollectionWrapper().getFeatures()) {
            feature.setAttribute(
                attributeIndex,
                convert(op.compute(feature.getGeometry()), attributeType));
        }
    }
    private Object convert(double d, AttributeType attributeType) {
        return typeToConverterMap.get(attributeType).convert(d);
    }

    private void updateAreas(Layer layer, String attributeName) {
        update(layer, attributeName, new Op() {
            public double compute(Geometry g) {
                return g.getArea();
            }
        });
    }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
            .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))
            .add(new EnableCheck(){
                public String check(JComponent component) {
                    Collection<Layer> layers = AttributeTypeFilter.NUMSTRING_FILTER.filter(workbenchContext.getLayerManager());
                    boolean candidateLayerFound = false;
                    for (Layer layer : layers) {
                        if (layer.isEditable()) {
                            candidateLayerFound = true;
                            break;
                        }
                    }
                    if (candidateLayerFound) return null;
                    return I18N.get("ui.plugin.analysis.CalculateAreasAndLengthsPlugIn.no-editable-layer-with-required-attributes");
                }
            });
    }
}
