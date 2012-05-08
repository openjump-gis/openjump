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

package com.vividsolutions.jump.workbench.ui.plugin.wms;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.addremove.AddRemoveListModel;
import com.vividsolutions.jump.workbench.ui.addremove.AddRemovePanel;
import com.vividsolutions.jump.workbench.ui.addremove.DefaultAddRemoveList;
import com.vividsolutions.jump.workbench.ui.addremove.TreeAddRemoveList;
import com.vividsolutions.jump.workbench.ui.addremove.TreeAddRemoveListModel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;


public class MapLayerPanel extends JPanel {
    public final static ImageIcon ICON = GUIUtil.resize(IconLoader.icon(
                "country.gif"), 13);
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private AddRemovePanel addRemovePanel = new AddRemovePanel(true);
    private JCheckBox checkBox = new JCheckBox(I18N.get("ui.plugin.wms.MapLayerPanel.sort"), true);
    // [mmichaud 2012-05-08] cache the fullSrs list associated to each MapLayer
    private Map<String,String> fullSrsMap = new HashMap<String,String>();

    public MapLayerPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initAddRemovePanel();
    }

    public List getChosenMapLayers() {
        ArrayList mapLayers = new ArrayList();

        for (Iterator i = addRemovePanel.getRightItems().iterator();
                i.hasNext();) {
            MapLayerTreeModel.LayerNode node = (MapLayerTreeModel.LayerNode) i.next();
            Assert.isTrue(node.getLayer().getName() != null);
            mapLayers.add(node.getLayer());
        }

        return mapLayers;
    }

    private void setRendererText(JLabel renderer, MapLayer layer) {
        String label = fullSrsMap.get(layer.getTitle());
        if (label == null) {
            label = layer.getTitle() + " [" +
                StringUtil.toCommaDelimitedString(layer.getFullSRSList()) + "]";
            fullSrsMap.put(layer.getTitle(), label);
        }
        renderer.setText(label);
    }

    void jbInit() throws Exception {
        addRemovePanel.setRightText(I18N.get("ui.plugin.wms.MapLayerPanel.chosen-layers"));
        this.setLayout(gridBagLayout1);
        this.add(addRemovePanel,
            new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    public void add(InputChangedListener listener) {
        inputChangedFirer.add(listener);
    }

    public void remove(InputChangedListener listener) {
        inputChangedFirer.remove(listener);
    }

    private void addIfOnList(MapLayer layer, AddRemoveListModel model,
        Collection names) {
        if (names.contains(layer.getName())) {
            //Just leave the second argument as null, because it's only needed for sorting the left list, and
            //here we're dealing with the right list. [Jon Aquino]
            model.add(new MapLayerTreeModel.LayerNode(layer, null));
        }

        for (Iterator i = layer.getSubLayerList().iterator(); i.hasNext();) {
            MapLayer child = (MapLayer) i.next();
            addIfOnList(child, model, names);
        }
    }

    public List commonSRSList() {
        List mapLayers = getChosenMapLayers();

        if (mapLayers.isEmpty()) {
            return new ArrayList();
        }

        ArrayList commonSRSList = new ArrayList(((MapLayer) mapLayers.get(0)).getFullSRSList());

        for (Iterator i = mapLayers.iterator(); i.hasNext();) {
            MapLayer layer = (MapLayer) i.next();
            commonSRSList.retainAll(layer.getFullSRSList());
        }

        return commonSRSList;
    }

    private void initAddRemovePanel() {
        TreeAddRemoveList leftList = new TreeAddRemoveList() {
                public List getSelectedItems() {
                    List selectedItems = new ArrayList(super.getSelectedItems());

                    for (Iterator i = selectedItems.iterator(); i.hasNext();) {
                        MapLayerTreeModel.LayerNode node = (MapLayerTreeModel.LayerNode) i.next();

                        //Don't want to add containers to the right-hand list. [Jon Aquino]
                        if (node.isContainer()) {
                            i.remove();
                        }
                    }

                    return selectedItems;
                }
            };

        addRemovePanel.setLeftList(leftList);
        leftList.getTree().setCellRenderer(new DefaultTreeCellRenderer() {
                public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {
                    JLabel component = (JLabel) super.getTreeCellRendererComponent(tree,
                            value, sel, expanded, leaf, row, hasFocus);

                    if (!(value instanceof MapLayerTreeModel.LayerNode)) {
                        //This happens during initialization. [Jon Aquino]
                        return component;
                    }

                    MapLayer layer = ((MapLayerTreeModel.LayerNode) value).getLayer();
                    setRendererText(component, layer);

                    if (layer.getName() == null) {
                        //Node is just a container. [Jon Aquino]
                        if (expanded) {
                            component.setIcon(UIManager.getIcon("Tree.openIcon"));
                        } else {
                            component.setIcon(UIManager.getIcon(
                                    "Tree.closedIcon"));
                        }
                    } else {
                        component.setIcon(ICON);
                    }

                    return component;
                }
            });

        DefaultAddRemoveList rightList = new DefaultAddRemoveList(new DefaultListModel() {
                    public void addElement(Object obj) {
                        if (contains(obj)) {
                            //Possible because items are never removed from the tree on the left. [Jon Aquino]
                            return;
                        }

                        super.addElement(obj);
                    }
                });
        addRemovePanel.setRightList(rightList);
        rightList.getList().setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                    JLabel component = (JLabel) super.getListCellRendererComponent(list,
                            value, index, isSelected, cellHasFocus);
                    MapLayer layer = ((MapLayerTreeModel.LayerNode) value).getLayer();
                    setRendererText(component, layer);
                    component.setIcon(ICON);

                    return component;
                }
            });
        leftList.add(new InputChangedListener() {
                public void inputChanged() {
                    inputChangedFirer.fire();
                }
            });
        rightList.add(new InputChangedListener() {
                public void inputChanged() {
                    inputChangedFirer.fire();
                }
            });
        rightList.getList().getModel().addListDataListener(new ListDataListener() {
                public void intervalAdded(ListDataEvent e) {
                    inputChangedFirer.fire();
                }

                public void intervalRemoved(ListDataEvent e) {
                    inputChangedFirer.fire();
                }

                public void contentsChanged(ListDataEvent e) {
                    inputChangedFirer.fire();
                }
            });

        JPanel leftLabelPanel = new JPanel();
        leftLabelPanel.setLayout(new GridBagLayout());
        leftLabelPanel.add(new JLabel(I18N.get("ui.plugin.wms.MapLayerPanel.available-layers")),
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        leftLabelPanel.add(new JPanel(),
            new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setSorted( checkBox.isSelected() );
                }
            });
        leftLabelPanel.add(checkBox,
            new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        addRemovePanel.setLeftLabel(leftLabelPanel);
    }


    private void setSorted( boolean isSorted ) {
        TreeAddRemoveList tarl = (TreeAddRemoveList) addRemovePanel.getLeftList();
        TreeAddRemoveListModel tarlm = (TreeAddRemoveListModel) tarl.getModel();
        MapLayerTreeModel mltm = (MapLayerTreeModel) tarlm.getTreeModel();

        mltm.setSorted( isSorted );
    }

    /**
     * @param initialChosenMapLayers null to leave unspecified
     */
    public void init(WMService service, Collection initialChosenMapLayers) {
        final MapLayerTreeModel treeModel = new MapLayerTreeModel(service.getCapabilities()
                                                                         .getTopLayer());

        treeModel.setSorted( checkBox.isSelected() );

        TreeAddRemoveListModel treeAddRemoveListModel = new TreeAddRemoveListModel(treeModel) {
                public List getItems() {
                    ArrayList items = new ArrayList(items((MapLayerTreeModel.LayerNode) treeModel.getRoot()));

                    for (Iterator i = items.iterator(); i.hasNext();) {
                        MapLayerTreeModel.LayerNode node = (MapLayerTreeModel.LayerNode) i.next();

                        //Don't want to add containers to the right-hand list. [Jon Aquino]
                        if (node.isContainer()) {
                            i.remove();
                        }
                    }

                    return items;
                }
            };

        ((TreeAddRemoveList) addRemovePanel.getLeftList()).setModel(treeAddRemoveListModel);

        if (initialChosenMapLayers != null) {
            addIfOnList(service.getCapabilities().getTopLayer(),
                addRemovePanel.getRightList().getModel(), initialChosenMapLayers);
        }

        addRemovePanel.updateEnabled();
    }

    private List items(MapLayerTreeModel.LayerNode node) {
        ArrayList items = new ArrayList();
        items.add(node);

        for (Enumeration e = node.children(); e.hasMoreElements();) {
            MapLayerTreeModel.LayerNode child = (MapLayerTreeModel.LayerNode) e.nextElement();
            items.addAll(items(child));
        }

        //Remove any items already on the right-hand list, so that the ">>" button enables/disables properly. [Jon Aquino]
        items.removeAll(addRemovePanel.getRightItems());

        return items;
    }
}
