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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.vividsolutions.jump.I18N;


/**
 *  Description panel to illustrate and explain a plugin throw its Dialog box 
 */
public class MultiInputDialogInfoPanel extends JPanel {
    
    static final int DEFAULT_WIDTH = 200;
    
    static final int DEFAULT_HEIGHT = 120;
    
    private int fixedWidth = DEFAULT_WIDTH;
    
    private int defaultHeight = DEFAULT_HEIGHT;
    
    // Label containing the Icon    
    final private JLabel imageLabel = new JLabel();
    
    // JTextArea containing the description
    final private JTextArea textArea = new JTextArea();
    
    // JScrolPane containing the JTextArea
    final private JScrollPane jsp = new JScrollPane(textArea);
    
    public MultiInputDialogInfoPanel() {
        this("", null, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public MultiInputDialogInfoPanel(String description, int width) {
        this(description, null, width, DEFAULT_HEIGHT);
    }
    
    public MultiInputDialogInfoPanel(String description, int width, int height) {
        this(description, null, width, height);
    }
    
    public MultiInputDialogInfoPanel(String description) {
        this(description, null, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public MultiInputDialogInfoPanel(Icon icon) {
        this("", icon, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public MultiInputDialogInfoPanel(Icon icon, int width) {
        this("", icon, width, DEFAULT_HEIGHT);
    }

    public MultiInputDialogInfoPanel(String description, Icon icon, int width, int height) {
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new GridBagLayout());
        
        imageLabel.setIcon(icon);
        add(imageLabel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.NORTH, GridBagConstraints.NONE,
            new Insets(4, 4, 4, 4), 0, 0));
        
        textArea.setEnabled(false);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(imageLabel.getFont());
        textArea.setDisabledTextColor(imageLabel.getForeground());
        textArea.setText(description);
        
        // weightx = 1.0 and fill=BOTH (or HORIZONAL) are both required to 
        // spread the textArea component over the entire panel width
        jsp.setBorder(null);
        add(jsp,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
            new Insets(2, 2, 2, 2), 0, 0));
        
        this.fixedWidth = width;
        this.defaultHeight = height;
                
        if (icon == null && (description == null || description.trim().length() == 0)) {
            setVisible(false);
        }
        else {
            textArea.setCaretPosition(0);
            setVisible(true);
        }
    }
    
    public String getDescription() {
        return textArea.getText();
    }
    
    public void setDescription(String description) {
        setIconAndDescription(getIcon(), description);
    }
    
    public Icon getIcon() {
        return imageLabel.getIcon();
    }
    
    public void setIcon(Icon icon) {
        setIconAndDescription(icon, getDescription());
    }
    
    private void setIconAndDescription(Icon icon, String description) {
        imageLabel.setIcon(icon);
        textArea.setText(description);
        if (icon == null && (description == null ||
                             description.trim().length() == 0)) {
            setVisible(false);
        }
        else {
            if (fixedWidth <= 0) fixedWidth = DEFAULT_WIDTH;
            textArea.setCaretPosition(0);
            setVisible(true);
        }
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(fixedWidth, defaultHeight);
    }
    
}
