
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Implements an animated clock.
 */

public class AnimatedClockPanel extends JPanel {
    private ArrayList queue = new ArrayList();
    private Timer timer = new Timer(250,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nextImage();
                }
            });
    private JLabel label = new JLabel();
    private BorderLayout borderLayout1 = new BorderLayout();

    public AnimatedClockPanel() {
        add("ClockN.gif");
        add("ClockNE.gif");
        add("ClockE.gif");
        add("ClockSE.gif");
        add("ClockS.gif");
        add("ClockSW.gif");
        add("ClockW.gif");
        add("ClockNW.gif");

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void add(String icon) {
        queue.add(IconLoader.icon(icon));
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void nextImage() {
        ImageIcon icon = (ImageIcon) queue.remove(0);
        queue.add(icon);
        label.setIcon(icon);
    }

    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        this.add(label, BorderLayout.CENTER);
        label.setIcon(IconLoader.icon("ClockN.gif"));
    }

    public static void main(String[] args) {
        AnimatedClockPanel p = new AnimatedClockPanel();
        p.start();

        JFrame f = new JFrame();
        f.getContentPane().add(p);
        f.setVisible(true);
    }
}
