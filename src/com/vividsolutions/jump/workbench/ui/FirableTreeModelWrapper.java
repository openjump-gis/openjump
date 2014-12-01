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

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class FirableTreeModelWrapper implements TreeModel {
    private TreeModel model;
    private ArrayList listeners = new ArrayList();

    public FirableTreeModelWrapper(TreeModel model) {
        this.model = model;
    }

    public Object getRoot() {
        return model.getRoot();
    }

    public Object getChild(Object parent, int index) {
        return model.getChild(parent, index);
    }

    public int getChildCount(Object parent) {
        return model.getChildCount(parent);
    }

    public boolean isLeaf(Object node) {
        return model.isLeaf(node);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        model.valueForPathChanged(path, newValue);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return model.getIndexOfChild(parent, child);
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
        model.addTreeModelListener(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
        model.removeTreeModelListener(l);
    }

    public void fireTreeNodesChanged(TreeModelEvent e) {
        starting();

        try {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                TreeModelListener l = (TreeModelListener) i.next();
                l.treeNodesChanged(e);
            }
        } finally {
            finishing();
        }
    }

    public void fireTreeNodesInserted(TreeModelEvent e) {
        starting();

        try {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                TreeModelListener l = (TreeModelListener) i.next();
                l.treeNodesInserted(e);
            }
        } finally {
            finishing();
        }
    }

    public void fireTreeNodesRemoved(TreeModelEvent e) {
        starting();

        try {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                TreeModelListener l = (TreeModelListener) i.next();
                l.treeNodesRemoved(e);
            }
        } finally {
            finishing();
        }
    }

    public void fireTreeStructureChanged(TreeModelEvent e) {
        starting();

        try {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                TreeModelListener l = (TreeModelListener) i.next();
                l.treeStructureChanged(e);
            }
        } finally {
            finishing();
        }
    }

    protected void finishing() {}

    protected void starting() {}

    public TreeModel getModel() {
        return model;
    }
}
