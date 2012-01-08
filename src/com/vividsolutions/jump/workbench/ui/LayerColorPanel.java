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
package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.JPanel;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.DummyStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

public class LayerColorPanel extends JPanel {
    public LayerColorPanel() {
        setLayout(new BorderLayout());
        add(oneColorPanel);
        setMaximumSize(new Dimension(10, 10));
        setMinimumSize(new Dimension(10, 10));
        setPreferredSize(new Dimension(10, 10));
    }
    private ColorPanel oneColorPanel = new ColorPanel();
    private FourColorPanel fourColorPanel = new LayerColorPanel.FourColorPanel();
    public LayerColorPanel init(
        Layer layer,
        boolean selected,
        Color backgroundColor,
        Color selectionBackgroundColor) {
        if (getComponents()[0] == fourColorPanel) {
            remove(fourColorPanel);
            add(oneColorPanel);
        }
        color(
            oneColorPanel,
            layer.getBasicStyle(),
            selected,
            backgroundColor,
            selectionBackgroundColor);
        ColorThemingStyle colorThemingStyle =
            (ColorThemingStyle) layer.getStyle(ColorThemingStyle.class);
        if (colorThemingStyle != null && colorThemingStyle.isEnabled()) {
            if (getComponents()[0] == oneColorPanel) {
                remove(oneColorPanel);
                add(fourColorPanel);
            }
            Iterator styles =
                colorThemingStyle.getAttributeValueToBasicStyleMap().values().iterator();
            color(
                fourColorPanel.panel1,
                colorThemingStyle.getDefaultStyle(),
                selected,
                backgroundColor,
                selectionBackgroundColor);
            color(
                fourColorPanel.panel2,
                styles.hasNext() ? (Style) styles.next() : DummyStyle.instance(),
                selected,
                backgroundColor,
                selectionBackgroundColor);
            color(
                fourColorPanel.panel3,
                styles.hasNext() ? (Style) styles.next() : DummyStyle.instance(),
                selected,
                backgroundColor,
                selectionBackgroundColor);
            color(
                fourColorPanel.panel4,
                styles.hasNext() ? (Style) styles.next() : DummyStyle.instance(),
                selected,
                backgroundColor,
                selectionBackgroundColor);
        }
        return this;
    }

    private void color(
        ColorPanel colorPanel,
        Style style,
        boolean selected,
        Color backgroundColor,
        Color selectionBackgroundColor) {
        colorPanel.setLineColor(
            style instanceof BasicStyle
                && ((BasicStyle) style).isRenderingLine()
                    ? GUIUtil.alphaColor(
                        ((BasicStyle) style).getLineColor(),
                        ((BasicStyle) style).getAlpha())
                    : (selected ? selectionBackgroundColor : backgroundColor));
        colorPanel.setFillColor(
            style instanceof BasicStyle
                && ((BasicStyle) style).isRenderingFill()
                    ? GUIUtil.alphaColor(
                        ((BasicStyle) style).getFillColor(),
                        ((BasicStyle) style).getAlpha())
                    : (selected ? selectionBackgroundColor : backgroundColor));
    }
    public static class FourColorPanel extends JPanel {
        public ColorPanel panel1 = new ColorPanel();
        public ColorPanel panel2 = new ColorPanel();
        public ColorPanel panel3 = new ColorPanel();
        public ColorPanel panel4 = new ColorPanel();
        public FourColorPanel() {
            GridLayout gridLayout = new GridLayout(2, 2);
            setLayout(gridLayout);
            add(panel1);
            add(panel2);
            add(panel3);
            add(panel4);
        }
    }
}