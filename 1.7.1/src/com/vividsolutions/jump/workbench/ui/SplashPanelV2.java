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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import org.openjump.core.CheckOS;

public class SplashPanelV2 extends JPanel {
  JPanel txt_panel;

  public SplashPanelV2(Icon image, String caption) {
    super(new BorderLayout());
    if (transparentSplash())
      setBackground(new Color(255, 255, 255, 0));
    JPanel img_panel = new JPanel(new BorderLayout());
    JLabel img_label = new JLabel(image /* IconLoader.icon("splash3.png") */);
    if (transparentSplash())
      img_panel.setBackground(new Color(255, 255, 255, 0));
    img_panel.add(img_label);

    txt_panel = new JPanel(new GridBagLayout());
    txt_panel.setBackground(Color.white);
    JLabel cap_label = new JLabel(caption /* "Version 1.0" */);
    cap_label
        .setFont(cap_label.getFont().deriveFont(java.awt.Font.BOLD, 20.0f));
    cap_label.setForeground(Color.lightGray);
    // make sure the version string is not wider than the logo img
    if (cap_label.getPreferredSize().width > img_label.getPreferredSize().width)
      cap_label.setPreferredSize(new Dimension(
          img_label.getPreferredSize().width - 20,
          cap_label.getPreferredSize().height));

    // replaced by progressmonitor below
    JLabel stat_label = new JLabel("some info");

    txt_panel.add(cap_label, new GridBagConstraints(0, 0, 1, 1, 1, 1,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0,
            10), 0, 0));
    // txt_panel.add(stat_label, new GridBagConstraints(0, 1, 1, 1, 1, 1,
    // GridBagConstraints.EAST, GridBagConstraints.NONE,
    // new Insets(0, 10, 10, 10), 0, 0));

    // getContentPane().setBackground(new Color(255, 0, 0));
    // setBackground(new Color(0, 255, 0,127));
    // contents.setBackground(new Color(0, 0, 255,0));
    add(img_panel, BorderLayout.NORTH);
    add(txt_panel, BorderLayout.SOUTH);

    int alpha = transparentSplash() ? 0 : 255;
    setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, new Color(
        255, 255, 255, alpha), new Color(255, 255, 255, alpha), new Color(103,
        101, 98, alpha), new Color(148, 145, 140, alpha)));
  }
  
  public void addProgressMonitor(JComponent pm) {
    txt_panel.add(pm, new GridBagConstraints(0, 1, 1, 1, 1, 1,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 4,
            0), 0, 0));
  }

  public static boolean transparentSplash() {
    return GUIUtil.isPerPixelTranslucencySupported() && !CheckOS.isLinux();
  }
}
