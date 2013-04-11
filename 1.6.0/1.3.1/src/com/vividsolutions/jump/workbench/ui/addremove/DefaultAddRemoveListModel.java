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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;


public class DefaultAddRemoveListModel implements AddRemoveListModel {
    private DefaultListModel listModel;
    private boolean sorted = false;

    public DefaultAddRemoveListModel(DefaultListModel listModel) {
        this.listModel = listModel;
    }

    public ListModel getListModel() {
        return listModel;
    }

    public void add(Object item) {
        listModel.addElement(item);

        if (sorted) {
            sort();
        }
    }

    private void setItemsWithoutSorting(Collection items) {
        listModel.clear();

        for (Iterator i = items.iterator(); i.hasNext();) {
            listModel.addElement(i.next());
        }
    }

    public void setItems(Collection items) {
        setItemsWithoutSorting(items);

        if (sorted) {
            sort();
        }
    }

    private void sort() {
        ArrayList items = new ArrayList(getItems());
        Collections.sort(items);
        setItemsWithoutSorting(items);
    }

    public List getItems() {
        return Arrays.asList(listModel.toArray());
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public void remove(Object item) {
        listModel.removeElement(item);
    }
}
