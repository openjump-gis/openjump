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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class BasicStyleListCellRenderer implements ListCellRenderer {

    protected ColorPanel colorPanel = new ColorPanel();
    private JPanel panel = new JPanel();
    protected DefaultListCellRenderer defaultListCellRenderer =
        new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            JLabel label =
                (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);
            label.setFont(label.getFont().deriveFont(0, 10));
            return label;
        }
    };
    {
        panel.setLayout(new GridBagLayout());
        panel.add(
            colorPanel,
            new GridBagConstraints(
                0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0,
                0));
        setColorPanelSize(new Dimension(45, 8));
    }
    protected void setColorPanelSize(Dimension d) {
        colorPanel.setMinimumSize(d);
        colorPanel.setMaximumSize(d);
        colorPanel.setPreferredSize(d);
    }
    private int alpha = 255;
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
        if (value instanceof String) {
            //Get here for "Custom..." [Jon Aquino]
            return defaultListCellRenderer.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus);
        }
        BasicStyle basicStyle = (BasicStyle) value;
        //colorPanel.setLineWidth(Math.min(3, basicStyle.getLineWidth()));
        //colorPanel.setLineStroke(basicStyle.getLineStroke());
        colorPanel.setStyle(basicStyle);
        colorPanel.setLineColor(
            basicStyle instanceof BasicStyle
                && ((BasicStyle) basicStyle).isRenderingLine()
                    ? GUIUtil.alphaColor(
                        ((BasicStyle) basicStyle).getLineColor(),
                        alpha)
                    : (isSelected
                        ? list.getSelectionBackground()
                        : list.getBackground()));
        colorPanel.setFillColor(
            basicStyle instanceof BasicStyle
                && ((BasicStyle) basicStyle).isRenderingFill()
                    ? GUIUtil.alphaColor(
                        ((BasicStyle) basicStyle).getFillColor(),
                        alpha)
                    : (isSelected
                        ? list.getSelectionBackground()
                        : list.getBackground()));
        if (isSelected) {
            colorPanel.setForeground(list.getSelectionForeground());
            colorPanel.setBackground(list.getSelectionBackground());
            panel.setForeground(list.getSelectionForeground());
            panel.setBackground(list.getSelectionBackground());
        } else {
            colorPanel.setForeground(list.getForeground());
            colorPanel.setBackground(list.getBackground());
            panel.setForeground(list.getForeground());
            panel.setBackground(list.getBackground());
        }
        return panel;
    }

}
