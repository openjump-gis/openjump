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

package com.vividsolutions.jump.workbench.ui.style;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.*;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.addremove.AddRemovePanel;
import com.vividsolutions.jump.workbench.ui.addremove.DefaultAddRemoveList;
import com.vividsolutions.jump.workbench.ui.addremove.DefaultAddRemoveListModel;
import com.vividsolutions.jump.workbench.ui.renderer.style.ChoosableStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;


public class DecorationStylePanel extends JPanel implements StylePanel {
    private AddRemovePanel addRemovePanel = new AddRemovePanel(false);
    private BorderLayout borderLayout1 = new BorderLayout();
    private Layer layer;
    private Collection choosableStyleClasses;

	public String getTitle() {
		return I18N.get("ui.style.DecorationStylePanel.decorations");
	}

    public DecorationStylePanel(Layer layer, Collection choosableStyleClasses) {
        try {
            this.layer = layer;
            this.choosableStyleClasses = choosableStyleClasses;
            populateAddRemovePanel(layer, choosableStyleClasses);
            setUpRenderer(((DefaultAddRemoveList) addRemovePanel.getLeftList()).getList());
            setUpRenderer(((DefaultAddRemoveList) addRemovePanel.getRightList()).getList());
            ((DefaultAddRemoveListModel) addRemovePanel.getLeftList().getModel()).setSorted(true);
            ((DefaultAddRemoveListModel) addRemovePanel.getRightList().getModel()).setSorted(true);
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setUpRenderer(JList list) {
        list.setCellRenderer(new ListCellRenderer() {
                private DefaultListCellRenderer baseRenderer = new DefaultListCellRenderer();

                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                    ListItem item = (ListItem) value;
                    JLabel component = (JLabel) baseRenderer.getListCellRendererComponent(list,
                            value, index, isSelected, cellHasFocus);
                    component.setText(item.style.getName());
                    component.setIcon(item.style.getIcon());

                    return component;
                }
            });
    }

    private void clearStyles(Layer layer) {
        //Fundamental styles (BasicStyle, VertexStyle, LabelStyle) aren't among
        //choosableStyleClasses and are in no danger of being cleared. [Jon Aquino]
        for (Iterator i = choosableStyleClasses.iterator(); i.hasNext();) {
            Class choosableStyleClass = (Class) i.next();
            Style style = layer.getStyle(choosableStyleClass);

            if (style != null) {
                layer.removeStyle(style);
            }
        }
    }

    public void updateStyles() {
        boolean firingEvents = layer.getLayerManager().isFiringEvents();
        layer.getLayerManager().setFiringEvents(false);

        try {
            clearStyles(layer);

            for (Iterator i = addRemovePanel.getRightItems().iterator();
                    i.hasNext();) {
                ListItem item = (ListItem) i.next();
                layer.addStyle(item.style);
            }
        } finally {
            layer.getLayerManager().setFiringEvents(firingEvents);
        }

        layer.fireAppearanceChanged();
    }

    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        addRemovePanel.setLeftText(I18N.get("ui.style.DecorationStylePanel.available"));
        addRemovePanel.setRightText(I18N.get("ui.style.DecorationStylePanel.in-use"));
        this.add(addRemovePanel, BorderLayout.CENTER);
    }

    private void populateAddRemovePanel(Layer layer, Collection choosableStyleClasses) {
        ArrayList availableItems = new ArrayList();
        ArrayList inUseItems = new ArrayList();

        for (Iterator i = choosableStyleClasses.iterator(); i.hasNext();) {
            Class choosableStyleClass = (Class) i.next();
            ChoosableStyle choosableStyleInUse = (ChoosableStyle) layer.getStyle(choosableStyleClass);

            if (choosableStyleInUse != null) {
                inUseItems.add(new ListItem(choosableStyleInUse));
            } else {
                try {
                    availableItems.add(new ListItem(
                            (ChoosableStyle) choosableStyleClass.newInstance()));
                } catch (IllegalAccessException e) {
                    Assert.shouldNeverReachHere();
                } catch (InstantiationException e) {
                    Assert.shouldNeverReachHere();
                }
            }
        }

        Collections.sort(availableItems);
        Collections.sort(inUseItems);
        addRemovePanel.getLeftList().getModel().setItems(availableItems);
        addRemovePanel.getRightList().getModel().setItems(inUseItems);
    }

    private class ListItem implements Comparable {
        public ChoosableStyle style;

        public ListItem(ChoosableStyle style) {
            this.style = style;
        }

        public int compareTo(Object o) {
            return toString().compareTo(o.toString());
        }

        public String toString() {
            return style.getName();
        }
    }
    
	public String validateInput() {
		return null;
	}    
}
