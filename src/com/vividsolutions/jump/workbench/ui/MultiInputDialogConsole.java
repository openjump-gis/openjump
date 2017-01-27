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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

/**
 * Console panel for the MultiInputDialog.
 * This console is used to flash warning messages to the user in a
 * MultiInputDialog. For example, it can be used when the user pressed the
 * OK button to explain why input values are inconsistant.
 */
public class MultiInputDialogConsole extends JPanel {
    
    final private JTextArea textArea = new JTextArea(2,32);
    
    final private Color BACKGROUND;
    
    private Color flashColor = Color.YELLOW;

    public MultiInputDialogConsole() {
        setLayout(new GridBagLayout());
        setBorder(javax.swing.BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setForeground(Color.BLACK);
        BACKGROUND = textArea.getBackground();
        add(new JScrollPane(textArea), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }
    
    /**
     * @return the message displayed by this console.
     */
    public String getMessage() {
        return textArea.getText();
    }
    
    /**
     * @param message the message to add to the console.
     */
    public void addMessage(String message) {
        textArea.append("\n" + message);
    }
    
    /**
     * @param message the message to set in this console.
     */
    public void setMessage(String message) {
        textArea.setText(message);
    }
    
    /**
     * @param flashColor the color of the flashed message.
     */
    public void setFlashColor(Color flashColor) {
        this.flashColor = flashColor;
    }
    
    /**
     * Flash a message in the console.
     */
    public void flashMessage(String message) {
        textArea.append("\n" + message);
        textArea.setDoubleBuffered(true);
        Runnable flash = new Runnable() {
            public void run() {
                Color gradient = flashColor;
                try {
                    flash(gradient, 500);
                    flash(BACKGROUND, 300);
                    flash(gradient, 500);
                    flash(BACKGROUND, 300);
                    flash(gradient, 600);
                    while (gradient.getRGB() != BACKGROUND.getRGB()) {
                        gradient = soften(gradient, BACKGROUND, 0.05f);
                        flash(gradient, 50);
                    }
                } catch(InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        };
        new Thread(flash).start();        
    }
    
    private void flash(Color background, int sleepMillis) throws InterruptedException {
        textArea.setBackground(background);
        Thread.sleep(sleepMillis);
    }
    
    private Color soften(Color src, Color tgt, float c) {
        int red   = src.getRed();
        int green = src.getGreen();
        int blue  = src.getBlue();
        red       = tgt.getRed()   + (int)((1f-c)*(src.getRed()  -tgt.getRed()));
        green     = tgt.getGreen() + (int)((1f-c)*(src.getGreen()-tgt.getGreen()));
        blue      = tgt.getBlue()  + (int)((1f-c)*(src.getBlue() -tgt.getBlue()));
        return new Color(red, green, blue);
    } 
    
}
