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
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.JListTypeAheadKeyListener;


public class DefaultAddRemoveList extends JPanel implements AddRemoveList {

    private BorderLayout borderLayout1 = new BorderLayout();
    private JList list = new JList();
    private DefaultAddRemoveListModel model;
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();

    public DefaultAddRemoveList() {
        this(new DefaultListModel());
    }
    
    public void add(MouseListener listener) {
        list.addMouseListener(listener);
    }    

    public DefaultAddRemoveList(DefaultListModel listModel) {
        model = new DefaultAddRemoveListModel(listModel);
        list.setModel(listModel);
        list.addKeyListener(new JListTypeAheadKeyListener(list));
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
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
        List<Integer> indicesToSelect = new ArrayList<>();

        for (Object item : items) {
            int index = getModel().getItems().indexOf(item);

            if (index == -1) {
                continue;
            }

            indicesToSelect.add(index);
        }

        int[] indexArray = new int[indicesToSelect.size()];

        for (int i = 0; i < indicesToSelect.size(); i++) {
            Integer index = indicesToSelect.get(i);
            indexArray[i] = index;
        }

        list.setSelectedIndices(indexArray);
    }

    public AddRemoveListModel getModel() {
        return model;
    }

    public void add(InputChangedListener listener) {
        inputChangedFirer.add(listener);
    }

    public JList getList() {
        return list;
    }

    public List getSelectedItems() {
      return list.getSelectedValuesList();
    }

    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        this.add(list, BorderLayout.CENTER);
    }
}
