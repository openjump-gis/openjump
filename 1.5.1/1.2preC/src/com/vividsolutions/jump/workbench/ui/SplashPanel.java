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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

public class SplashPanel extends JPanel {
    private GridBagLayout gridBagLayout = new GridBagLayout();
    private JLabel captionLabel = new JLabel();
    private JLabel imageLabel = new JLabel();
    private Border border1;
    private Border border2;

    public SplashPanel(Icon image, String caption) {
        try {
            jbInit();
            imageLabel.setIcon(image);
            captionLabel.setText(caption);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        border2 =
            BorderFactory.createBevelBorder(
                BevelBorder.RAISED,
                Color.white,
                Color.white,
                new Color(103, 101, 98),
                new Color(148, 145, 140));

        CompoundBorder compoundBorder =
            new CompoundBorder(BorderFactory.createLineBorder(Color.black), border2);
        this.setLayout(gridBagLayout);
        captionLabel.setFont(new java.awt.Font("Dialog", 1, 20));
        this.setBackground(Color.white);
        captionLabel.setForeground(Color.lightGray);
        captionLabel.setBorder(border1);
        captionLabel.setText("Version 1.0");
        this.setBorder(compoundBorder);
        this.add(
            imageLabel,
            new GridBagConstraints(
                0,
                0,
                1,
                1,
                1,
                1,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            captionLabel,
            new GridBagConstraints(
                0,
                1,
                1,
                1,
                0,
                0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 10),
                0,
                0));                
    }
}
