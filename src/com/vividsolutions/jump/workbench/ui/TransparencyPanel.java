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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JSlider;


public class TransparencyPanel extends JPanel {
    JPanel opaquePanel = new JPanel();
    JPanel transparentPanel = new JPanel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JSlider transparencySlider = new JSlider();

    public TransparencyPanel() {
        transparencySlider.setMaximum(255);
        transparencySlider.setPreferredSize(new Dimension(100, 24));
        setLayout(gridBagLayout3);
        add(opaquePanel,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        add(transparencySlider,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        add(transparentPanel,
            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        opaquePanel.setBackground(Color.black);
        opaquePanel.setMinimumSize(new Dimension(11, 11));
        opaquePanel.setMaximumSize(new Dimension(11, 11));
        opaquePanel.setPreferredSize(new Dimension(11, 11));
        transparentPanel.setBackground(Color.white);
        transparentPanel.setForeground(Color.white);
        transparentPanel.setMaximumSize(new Dimension(11, 11));
        transparentPanel.setMinimumSize(new Dimension(11, 11));
        transparentPanel.setPreferredSize(new Dimension(11, 11));
    }

    public void setColor(Color color) {
        opaquePanel.setBackground(color);
    }

    public JSlider getSlider() {
        return transparencySlider;
    }
}
