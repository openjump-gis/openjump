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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.tree.TreeModel;


/**
 * A read-only tree appropriate for the left-hand panel of an AddRemovePanel.
 * Not for use as the right-hand panel because it is read-only -- it does not
 * have any logic for adding a node to the tree (or removing a node, for that
 * matter).
 */
public class TreeAddRemoveListModel implements AddRemoveListModel {
    private TreeModel treeModel;

    public TreeAddRemoveListModel(TreeModel treeModel) {
        this.treeModel = treeModel;
    }

    public TreeModel getTreeModel() {
        return treeModel;
    }

    public void add(Object item) {
        //Do nothing [Jon Aquino]
    }

    public void setItems(Collection items) {
        //Do nothing [Jon Aquino]
    }

    public List getItems() {
        return new ArrayList();
    }

    public void remove(Object item) {
        //Do nothing [Jon Aquino]
    }
}
