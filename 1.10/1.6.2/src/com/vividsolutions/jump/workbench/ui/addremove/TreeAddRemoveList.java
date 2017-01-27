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

package com.vividsolutions.jump.workbench.ui.addremove;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;


public class TreeAddRemoveList extends JPanel implements AddRemoveList {
    private BorderLayout borderLayout1 = new BorderLayout();
    private TreeAddRemoveListModel model = new TreeAddRemoveListModel(new JTree().getModel());
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private JTree tree = new JTree();
    private Border border1;

    public void add(MouseListener listener) {
        tree.addMouseListener(listener);
    }    

    public TreeAddRemoveList() {
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    inputChangedFirer.fire();
                }
            });

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setSelectedItems(Collection items) {
        throw new UnsupportedOperationException();
    }

    public void setModel(TreeAddRemoveListModel model) {
        this.model = model;
        tree.setModel(model.getTreeModel());
        inputChangedFirer.fire();
    }

    public JTree getTree() {
        return tree;
    }

    public void add(InputChangedListener listener) {
        inputChangedFirer.add(listener);
    }

    void jbInit() throws Exception {
        border1 = new EtchedBorder(EtchedBorder.RAISED, new Color(0, 0, 51),
                new Color(0, 0, 25));
        this.setLayout(borderLayout1);
        this.add(tree, BorderLayout.CENTER);
    }

    public AddRemoveListModel getModel() {
        return model;
    }

    public List getSelectedItems() {
        ArrayList selectedNodes = new ArrayList();
        TreePath[] selectionPaths = tree.getSelectionPaths();

        if (selectionPaths == null) {
            return selectedNodes;
        }

        for (int i = 0; i < selectionPaths.length; i++) {
            selectedNodes.add(selectionPaths[i].getLastPathComponent());
        }

        return selectedNodes;
    }
}
