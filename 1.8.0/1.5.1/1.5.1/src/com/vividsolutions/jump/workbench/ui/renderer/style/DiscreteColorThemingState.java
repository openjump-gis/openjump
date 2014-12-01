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
package com.vividsolutions.jump.workbench.ui.renderer.style;

import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.vividsolutions.jump.I18N;

public class DiscreteColorThemingState
    implements ColorThemingStylePanel.State {
    public String getAllOtherValuesDescription() {
        return I18N.get("ui.renderer.style.DiscreteColorThemingState.all-other-values");
    }
    public String getAttributeValueColumnTitle() {
        return I18N.get("ui.renderer.style.DiscreteColorThemingState.attribute-values");
    }
    public ColorScheme filterColorScheme(ColorScheme colorScheme) {
        return colorScheme;
    }
    public Collection filterAttributeValues(SortedSet attributeValues) {
        return attributeValues;
    }
    private JTable table;
    private JPanel panel = new JPanel(new GridBagLayout());
    public JComponent getPanel() {
        return panel;
    }
    public Map toExternalFormat(Map attributeValueToObjectMap) {
        return attributeValueToObjectMap;
    }
    public Map fromExternalFormat(Map attributeValueToObjectMap) {
        return attributeValueToObjectMap;
    }
    public void applyColorScheme(ColorScheme colorScheme) {
        ((ColorThemingTableModel) table.getModel()).apply(colorScheme, false);
    }
    public Collection getColorSchemeNames() {
        return ColorScheme.discreteColorSchemeNames();
    }
    public void activate() {
    }
    public void deactivate() {
    }
    public DiscreteColorThemingState(JTable table) {
        this.table = table;
    }
}
