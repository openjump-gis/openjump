
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.vividsolutions.jump.workbench.ui.images.IconLoader;


public class RecordPanel extends JPanel {
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JButton startButton = new JButton();
    private JButton prevButton = new JButton();
    private JTextField textField = new JTextField(4);
    private JButton nextButton = new JButton();
    private JButton endButton = new JButton();
    private RecordPanelModel model;

    public RecordPanel(RecordPanelModel model) {
        this.model = model;

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setIcon(startButton, "Start.gif");
        setIcon(prevButton, "Prev.gif");
        setIcon(nextButton, "Next.gif");
        setIcon(endButton, "End.gif");
        updateAppearance();
    }

    private void setIcon(JButton button, String filename) {
        button.setIcon(IconLoader.icon(filename));
        button.setText("");
    }

    public void updateAppearance() {
        startButton.setEnabled(model.getCurrentIndex() > 0);
        prevButton.setEnabled(model.getCurrentIndex() > 0);
        nextButton.setEnabled(model.getCurrentIndex() < (model.getRecordCount() -
            1));
        endButton.setEnabled(model.getCurrentIndex() < (model.getRecordCount() -
            1));
        textField.setText("" + (model.getCurrentIndex() + 1));
    }

    private void jbInit() throws Exception {
        startButton.setFocusPainted(false);
        startButton.setMargin(new Insets(0, 0, 0, 0));
        prevButton.setFocusPainted(false);
        prevButton.setMargin(new Insets(0, 0, 0, 0));
        nextButton.setFocusPainted(false);
        nextButton.setMargin(new Insets(0, 0, 0, 0));
        endButton.setFocusPainted(false);
        endButton.setMargin(new Insets(0, 0, 0, 0));
        startButton.setText("|<");
        prevButton.setText("<");
        startButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startButton_actionPerformed(e);
                }
            });
        this.setLayout(gridBagLayout1);
        prevButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    prevButton_actionPerformed(e);
                }
            });
        nextButton.setText(">");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nextButton_actionPerformed(e);
                }
            });
        endButton.setText(">|");
        endButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    endButton_actionPerformed(e);
                }
            });
        textField.setEditable(false);
        textField.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(startButton,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(prevButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(textField,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        this.add(nextButton,
            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(endButton,
            new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    void nextButton_actionPerformed(ActionEvent e) {
        model.setCurrentIndex(model.getCurrentIndex() + 1);
        updateAppearance();
    }

    void endButton_actionPerformed(ActionEvent e) {
        model.setCurrentIndex(model.getRecordCount() - 1);
        updateAppearance();
    }

    void prevButton_actionPerformed(ActionEvent e) {
        model.setCurrentIndex(model.getCurrentIndex() - 1);
        updateAppearance();
    }

    void startButton_actionPerformed(ActionEvent e) {
        model.setCurrentIndex(0);
        updateAppearance();
    }
}
