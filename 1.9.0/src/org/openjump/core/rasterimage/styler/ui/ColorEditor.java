/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package org.openjump.core.rasterimage.styler.ui;

/* 
 * ColorEditor.java (compiles with releases 1.3 and 1.4) is used by 
 * TableDialogEditDemo.java.
 */

import java.awt.AWTError;
import java.awt.BorderLayout;
import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            ActionListener {
    
    private Component parent;
    private Color currentColor;
    private JButton button;
    private JColorChooser colorChooser;
    private JDialog jDialog_ColorPicker;
    private JCheckBox jCheckBox_Transparent;
    protected static final String EDIT = java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.ui.ColorEditor.edit");

    public ColorEditor(Component parent) {
        this.parent = parent;
        //Set up the editor (from the table's point of view),
        //which is a button.
        //This button brings up the color chooser jDialog_ColorPicker,
        //which is the editor from the user's point of view.
        fixComponents();
    }

    private void fixComponents() {
        button = new JButton();
        button.setActionCommand(EDIT);
        button.addActionListener(this);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        
        //Set up the jDialog_ColorPicker that the button brings up.
        colorChooser = new JColorChooser();
//        jDialog_ColorPicker = JColorChooser.createDialog(button,
//                                        "Pick a Color",
//                                        true,  //modal
//                                        colorChooser,
//                                        this,  //OK button handler
//                                        null); //no CANCEL button handler
        
        if(parent == null) {
            throw new AWTError("No suitable parent found for Component."); //NOI18N
        } else if (parent instanceof Dialog) {
            jDialog_ColorPicker = new JDialog((Dialog) parent, java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.ui.ColorEditor.PickAColor"));
        } else if (parent instanceof Frame) {
            jDialog_ColorPicker = new JDialog((Frame) parent, java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.ui.ColorEditor.PickAColor"));
        }
        jDialog_ColorPicker.setLocationRelativeTo(parent);
        jDialog_ColorPicker.setSize(500, 500);
        jDialog_ColorPicker.setModal(true);
        
        jDialog_ColorPicker.getContentPane().setLayout(new BorderLayout());
        colorChooser = new JColorChooser();
        jDialog_ColorPicker.getContentPane().add(colorChooser, BorderLayout.NORTH);
        
        jCheckBox_Transparent = new JCheckBox(java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.ui.ColorEditor.Transparent"));
        jDialog_ColorPicker.getContentPane().add(jCheckBox_Transparent, BorderLayout.CENTER);
        
        JButton jButton_Cancel = new JButton(java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.ui.ColorEditor.Cancel"));
        jButton_Cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                jDialog_ColorPicker.setVisible(false);
            }
        });
        
        JButton jButton_Ok = new JButton(java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle").getString("org.openjump.core.rasterimage.styler.ui.ColorEditor.Ok"));
        jButton_Ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                currentColor = colorChooser.getColor();
                if(jCheckBox_Transparent.isSelected()) {
                    currentColor = new Color(
                            currentColor.getRed(),
                            currentColor.getGreen(),
                            currentColor.getBlue(),
                            0);
                }
                
                jDialog_ColorPicker.setVisible(false);
            }
        });
        
        JPanel jPanel_Buttons = new JPanel(new FlowLayout());
        jPanel_Buttons.add(jButton_Cancel);
        jPanel_Buttons.add(jButton_Ok);
        
        jDialog_ColorPicker.getContentPane().add(jPanel_Buttons, BorderLayout.SOUTH);
        
    }
    
    /**
     * Handles events from the editor button and from
     * the jDialog_ColorPicker's OK button.
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (EDIT.equals(e.getActionCommand())) {
            //The user has clicked the cell, so
            //bring up the jDialog_ColorPicker.
            button.setBackground(currentColor);
            colorChooser.setColor(currentColor);
            jDialog_ColorPicker.setVisible(true);

            //Make the renderer reappear.
            fireEditingStopped();
            
        } else { //User pressed jDialog_ColorPicker's "OK" button.
            currentColor = colorChooser.getColor();
        }
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    @Override
    public Object getCellEditorValue() {
        return currentColor;
    }

    //Implement the one method defined by TableCellEditor.
    @Override
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        currentColor = (Color)value;
        return button;
    }
    
    
}
