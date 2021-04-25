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
package com.vividsolutions.jump.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.locationtech.jts.util.Assert;

/**
 * Simple implementation of TreeModel.
 * SimpleTreeModel is the equivalent of DefaultTreeModel with a firingEvent attribute
 * to determine if model events must be propagated to the interface.
 */
public abstract class SimpleTreeModel implements TreeModel {

    private final Object root;
    private final List<TreeModelListener> listeners = new ArrayList<>();
    private boolean firingEvents = true;

    // [mmichaud] what is it for ? unused in OpenJUMP codebase
    //public static abstract class Folder {
    //    private final Class<?> childrenClass;
    //    private final String name;
    //    private final Object parent;
    //    public Folder(String name, Object parent, Class<?> childrenClass) {
    //        this.name = name;
    //        this.parent = parent;
    //        this.childrenClass = childrenClass;
    //    }
    //    public abstract List<?> getChildren();
    //    public String toString() {
    //        return name;
    //    }
    //    public int hashCode() {
    //        //JTree puts nodes in a Hashtable. To keep things simple, just return 0,
    //        //which will cause linear searches (fine for small trees). [Jon Aquino]
    //        return name.hashCode();
    //    }
    //    public boolean equals(Object other) {
    //        //Folders are value objects. [Jon Aquino]
    //        if (! (other instanceof Folder)) {
    //            return false;
    //        }
    //        Folder otherFolder = (Folder) other;
    //        return parent == otherFolder.parent && name.equals(otherFolder.name);
    //    }
    //
    //    public Class<?> getChildrenClass() {
    //        return childrenClass;
    //    }
    //
    //    public Object getParent() {
    //        return parent;
    //    }
    //
    //}


    public SimpleTreeModel(Object root) {
        this.root = root;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
        return /*! (node instanceof Folder) &&*/ getChildCount(node) == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        for (int i = 0; i < getChildCount(parent); i++) {
            //Folders are value objects. [Jon Aquino]
            if (/*child instanceof Folder
                && getChild(parent, i) instanceof Folder
                &&*/ getChild(parent, i).toString().equals(child.toString())) {
                return i;
            }
            if (getChild(parent, i) == child) {
                return i;
            }
        }
        Assert.shouldNeverReachHere(parent + ", " + child);
        return -1;
    }



    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * No need to handle Folders
     * @param parent not a Folder
     */
    public abstract List<?> getChildren(Object parent);

    public Object getChild(Object parent, int index) {
        return children(parent).get(index);
    }

    private List<?> children(Object parent) {
        //return parent instanceof Folder
        //    ? ((Folder) parent).getChildren()
        //    : getChildren(parent);
        return getChildren(parent);
    }

    public int getChildCount(Object parent) {
        return children(parent).size();
    }
    

    public void fireTreeNodesChanged(TreeModelEvent e) {
        if (!firingEvents) { return; }
        for (TreeModelListener listener : listeners) {
            listener.treeNodesChanged(e);
        }
    }
    
    public void fireTreeNodesInserted(TreeModelEvent e) {
        if (!firingEvents) { return; }
        for (TreeModelListener listener : listeners) {
            listener.treeNodesInserted(e);
        }
    }
    
    public void fireTreeNodesRemoved(TreeModelEvent e) {
        if (!firingEvents) { return; }
        for (TreeModelListener listener : listeners) {
            listener.treeNodesRemoved(e);
        }
    }
    
    public void fireTreeStructureChanged(TreeModelEvent e) {
        if (!firingEvents) { return; }
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(e);
        }
    }    

    public void setFiringEvents(boolean firingEvents) {
        this.firingEvents = firingEvents;
    }

}
