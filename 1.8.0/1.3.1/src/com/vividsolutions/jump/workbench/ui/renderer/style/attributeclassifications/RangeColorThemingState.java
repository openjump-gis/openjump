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
package com.vividsolutions.jump.workbench.ui.renderer.style.attributeclassifications;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStylePanel;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingTableModel;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStylePanel.State;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingTableModel.AttributeValueTableModelEvent;

public class RangeColorThemingState implements ColorThemingStylePanel.State {
    private ColorThemingStylePanel stylePanel;
    private static final String RANGE_COUNT_KEY =
        RangeColorThemingState.class.getName() + " - RANGE COUNT";
    public String getAllOtherValuesDescription() {
        return I18N.get("ui.renderer.style.RangeColorThemingState.values-below-these-values");
    }
    public String getAttributeValueColumnTitle() {
        return I18N.get("ui.renderer.style.RangeColorThemingState.minimum-attribute-values");
    }
    private int getRangeCount() {
        return ((Integer) comboBox.getSelectedItem()).intValue();
    }
    public Collection filterAttributeValues(SortedSet attributeValues) {
        //-1 because one row in the table is reserved for "all other values". [Jon Aquino]
        int maxFilteredSize = getRangeCount() - 1;
        //Obtain even distribution. [Jon Aquino]
        ArrayList attributeValueList = new ArrayList(attributeValues);
        Set filteredValues = new TreeSet();
        CollectionUtil.stretch(
            attributeValueList,
            filteredValues,
            maxFilteredSize);
        return filteredValues;
    }
    private JPanel panel = new JPanel(new GridBagLayout()) {
        public void setEnabled(boolean enabled) {
            comboBox.setEnabled(enabled);
            label.setEnabled(enabled);
            reverseButton.setEnabled(enabled);
            super.setEnabled(enabled);
        }
    };

    public RangeColorThemingState(final ColorThemingStylePanel stylePanel) {
        this.stylePanel = stylePanel;
        addComboBoxItems();
        comboBox.setSelectedItem(
            stylePanel.getLayer().getLayerManager().getBlackboard().get(
                RANGE_COUNT_KEY,
                new Integer(5)));
        //Don't add action listeners until items have been added to the
        //combo box. [Jon Aquino]                
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stylePanel.populateTable();
                stylePanel.getLayer().getLayerManager().getBlackboard().put(
                    RANGE_COUNT_KEY,
                    comboBox.getSelectedItem());
            }
        });
        panel.add(
            label,
            new GridBagConstraints(
                1,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0,
                0));
        panel.add(
            comboBox,
            new GridBagConstraints(
                2,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0,
                0));
        panel.add(
            reverseButton,
            new GridBagConstraints(
                3,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0,
                0));
        reverseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reversingColorScheme = !reversingColorScheme;
                stylePanel.applyColorScheme();
            }
        });
    }
    private JButton reverseButton = new JButton(I18N.get("ui.renderer.style.RangeColorThemingState.reverse-colors"));
    private void addComboBoxItems() {
        int maxColorSchemeSize = -1;
        for (Iterator i = ColorScheme.rangeColorSchemeNames().iterator();
            i.hasNext();
            ) {
            String rangeColorSchemeName = (String) i.next();
            maxColorSchemeSize =
                Math.max(
                    maxColorSchemeSize,
                    ColorScheme
                        .create(rangeColorSchemeName)
                        .getColors()
                        .size());
        }
        for (int i = 3; i <= maxColorSchemeSize; i++) {
            comboBoxModel.addElement(new Integer(i));
        }
    }
    private DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private JComboBox comboBox = new JComboBox(comboBoxModel);
    private JLabel label = new JLabel(I18N.get("ui.renderer.style.RangeColorThemingState.range-count"));
    public JComponent getPanel() {
        return panel;
    }
    public Map fromExternalFormat(Map attributeValueToObjectMap) {
        //Table takes values, not ranges. [Jon Aquino]
        TreeMap newMap = new TreeMap();
        for (Iterator i = attributeValueToObjectMap.keySet().iterator();
            i.hasNext();
            ) {
            Range range = (Range) i.next();
            newMap.put(
                range.getMin(),
                attributeValueToObjectMap.get(range));
        }
        return newMap;
    }
    public Map toExternalFormat(Map attributeValueToObjectMap) {
        if (attributeValueToObjectMap.isEmpty()) {
            return attributeValueToObjectMap;
        }
        //Turn the values into ranges. Validations have already ensured that
        //the values are unique and contain no nulls. [Jon Aquino]
        Assert.isTrue(attributeValueToObjectMap instanceof SortedMap);
        TreeMap newMap = new Range.RangeTreeMap();
        Object previousValue = null;
        for (Iterator i = attributeValueToObjectMap.keySet().iterator();
            i.hasNext();
            ) {
            Object value = i.next();
            try {
                if (previousValue == null) {
                    //Let the default style handle values from negative infinity to
                    //the first value. [Jon Aquino]
                    continue;
                }
                //Make one side inclusive and the other exclusive to ensure no
                //overlaps. [Jon Aquino]
                newMap.put(
                    new Range(previousValue, true, value, false),
                    attributeValueToObjectMap.get(previousValue));
            } finally {
                previousValue = value;
            }
        }
        newMap.put(
            new Range(previousValue, true, new Range.PositiveInfinity(), false),
            attributeValueToObjectMap.get(previousValue));
        return newMap;
    }
    public void applyColorScheme(ColorScheme colorScheme) {
        stylePanel.tableModel().apply(
            new ColorScheme(
                null,
                CollectionUtil.stretch(
                    colorScheme.getColors(),
                    new ArrayList(),
                    stylePanel.tableModel().getRowCount())),
            false);
    }
    public Collection getColorSchemeNames() {
        return ColorScheme.rangeColorSchemeNames();
    }
    private TableModelListener tableModelListener = new TableModelListener() {
        public void tableChanged(TableModelEvent e) {
            if (e
                instanceof ColorThemingTableModel.AttributeValueTableModelEvent) {
                stylePanel.tableModel().sort(stylePanel.tableModel().wasLastSortAscending());
                //I'd like to scroll to the row at this point, but the user probably
                //finished the edit by clicking on another cell, so even if I scroll
                //to the row, it scrolls back to where the user clicked. [Jon Aquino]
            }
        }
    };
    private int row(Object attributeValue) {
        for (int i = 0; i < stylePanel.tableModel().getRowCount(); i++) {
            Object otherAttributeValue =
                stylePanel.tableModel().getValueAt(
                    i,
                    ColorThemingTableModel.ATTRIBUTE_COLUMN);
            if (attributeValue == null && otherAttributeValue == null) {
                return i;
            }
            if (attributeValue != null
                && attributeValue.equals(otherAttributeValue)) {
                return i;
            }
        }
        Assert.shouldNeverReachHere();
        return -1;
    }
    public void activate() {
        stylePanel.tableModel().addTableModelListener(tableModelListener);
    }
    public void deactivate() {
        stylePanel.tableModel().removeTableModelListener(tableModelListener);
    }

    private boolean reversingColorScheme = false;
    public ColorScheme filterColorScheme(ColorScheme colorScheme) {
        if (!reversingColorScheme) {
            return colorScheme;
        }
        List colors = new ArrayList(colorScheme.getColors());
        Collections.reverse(colors);
        return new ColorScheme(colorScheme.getName(), colors);
    }

}
