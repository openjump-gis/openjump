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
package com.vividsolutions.jump.workbench.ui.style;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.BorderFactory;

import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;

public class GridPalettePanel extends AbstractPalettePanel {
    private static final int TILE_EXTENT = 26;
    private static final int ROWS = 6;
    private static final int COLUMNS = 5;
    public GridPalettePanel() {
        setLayout(new GridLayout(ROWS, COLUMNS));
        Iterator k = basicStyles().iterator();
        for (int i = 0; i < ROWS && k.hasNext(); i++) {
            for (int j = 0; j < COLUMNS && k.hasNext(); j++) {
                add(colorPanel((BasicStyle)k.next()));
            }
        }
    }
    public void setAlpha(int alpha) {
        for (int i = 0; i < getComponentCount(); i++) {
            ColorPanel colorPanel = (ColorPanel) getComponent(i);
            colorPanel.setFillColor(GUIUtil.alphaColor(colorPanel.getFillColor(), alpha));
            colorPanel.setLineColor(GUIUtil.alphaColor(colorPanel.getLineColor(), alpha));
        }
        repaint();
    }
    private Component colorPanel(final BasicStyle basicStyle) {
        final ColorPanel colorPanel = new ColorPanel();
        colorPanel.setFillColor(basicStyle.getFillColor());
        colorPanel.setLineColor(basicStyle.getLineColor());
        colorPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        colorPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                fireBasicStyleChosen(basicStyle);
            }
        });
        Dimension size = new Dimension(TILE_EXTENT, TILE_EXTENT);
        colorPanel.setMaximumSize(size);
        colorPanel.setMinimumSize(size);
        colorPanel.setPreferredSize(size);
        return colorPanel;
    }
}
