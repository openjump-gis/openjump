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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.TreeCellEditor;

/**
 * Implements a tree cell editor for the Layer tree.
 */

public class LayerTreeCellEditor implements TreeCellEditor {
    private JTextField textField = new JTextField();
    private DefaultCellEditor editor = new DefaultCellEditor(textField);
    private JTree tree;

    public LayerTreeCellEditor(JTree tree) {
        this.tree = tree;

        //A font that is reasonable for both categories and layers. [Jon Aquino]
        textField.setFont(new JLabel().getFont());
    }

    private void changeWidthUsing(JTree tree, int row) {
        //Make editor as wide as possible, so user input doesn't get chopped off. [Jon Aquino]
        int width =
            (int) (tree.getWidth()
                - tree
                    .getUI()
                    .getPathBounds(tree, tree.getUI().getPathForRow(tree, row))
                    .getLocation()
                    .getX());
        textField.setPreferredSize(
            new Dimension(width, (int) textField.getPreferredSize().getHeight()));
    }

    public Component getTreeCellEditorComponent(
        JTree tree,
        Object value,
        boolean isSelected,
        boolean expanded,
        boolean leaf,
        int row) {
        changeWidthUsing(tree, row);

        return editor.getTreeCellEditorComponent(
            tree,
            value,
            isSelected,
            expanded,
            leaf,
            row);
    }

    public Object getCellEditorValue() {
        return editor.getCellEditorValue();
    }

    public boolean isCellEditable(EventObject anEvent) {
        MouseEvent e = (MouseEvent) anEvent;
        if (SwingUtilities.isRightMouseButton(e)) {
            return false;
        }
        return editor.isCellEditable(anEvent);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    public boolean stopCellEditing() {
        return editor.stopCellEditing();
    }

    public void cancelCellEditing() {
        editor.cancelCellEditing();
    }

    public void addCellEditorListener(CellEditorListener l) {
        editor.addCellEditorListener(l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        editor.removeCellEditorListener(l);
    }
}
