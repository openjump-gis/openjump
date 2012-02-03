
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.I18N;

/**
 * A custom Colour Scheme Browser - custom choices based on JColorChooser.
 */

public class ColorChooserPanel extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JButton changeButton = new JButton();


    //{DEFECT-PREVENTION} In accessors, it's better to use "workbench = newWorkbench"
    //rather than "this.workbench = workbench", because otherwise if you misspell the
    //argument, Java won't complain! We should do this everywhere. [Jon Aquino]
    JPanel outerColorPanel = new JPanel();
    ColorPanel colorPanel = new ColorPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    private Color color = Color.black;
    private int alpha;
    private ArrayList actionListeners = new ArrayList();

    public ColorChooserPanel() {
        try {
            jbInit();
            colorPanel.setLineColor(null);
            changeButton.setToolTipText(I18N.get("ui.ColorChooserPanel.browse"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        changeButton.setMargin(new Insets(0, 0, 0, 0));
        changeButton.setText("   ...   ");
        changeButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeButton_actionPerformed(e);
                }
            });
        outerColorPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        outerColorPanel.setPreferredSize(new Dimension(60, 20));
        outerColorPanel.setBackground(Color.white);
        outerColorPanel.setLayout(gridBagLayout2);
        colorPanel.setMargin(0);
        colorPanel.setPreferredSize(new Dimension(45, 8));
        this.add(changeButton,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 2, 0, 0), 0, 0));
        this.add(outerColorPanel,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        outerColorPanel.add(colorPanel,
            new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    void changeButton_actionPerformed(ActionEvent e) {
        Color newColor = JColorChooser.showDialog(SwingUtilities.windowForComponent(this), I18N.get("ui.ColorChooserPanel.choose-color"), color);

        if (newColor == null) {
            return;
        }

        setColor(newColor);
        fireActionPerformed();
    }

    public void setColor(Color color) {
        this.color = color;
        updateColorPanel();
    }

    private void updateColorPanel() {
        colorPanel.setFillColor(GUIUtil.alphaColor(color, alpha));
        colorPanel.repaint();
    }

    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.remove(l);
    }

    protected void fireActionPerformed() {
        for (Iterator i = actionListeners.iterator(); i.hasNext();) {
            ActionListener l = (ActionListener) i.next();
            l.actionPerformed(new ActionEvent(this, 0, null));
        }
    }

    public Color getColor() {
        return color;
    }

    //{CLARITY} Might be clearer to say "alpha" rather than "alpha".
    //This occurs in other places too. [Jon Aquino]
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        updateColorPanel();
    }

    public void setEnabled(boolean newEnabled) {
        changeButton.setEnabled(newEnabled);
    }
}
