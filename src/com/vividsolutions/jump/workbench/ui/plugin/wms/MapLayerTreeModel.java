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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.vividsolutions.wms.MapLayer;

/** Hierarchical collection of MapLayers */
public class MapLayerTreeModel extends DefaultTreeModel {

    private boolean sorted = false;

    public MapLayerTreeModel(MapLayer topLayer) {
        super(new LayerNode(topLayer, null));
        ((LayerNode) getRoot()).mapLayerTreeModel = this;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
        reload();
    }

    public static class LayerNode implements TreeNode, Comparable<LayerNode> {
        private final MapLayer layer;
        private MapLayerTreeModel mapLayerTreeModel;

        public LayerNode(MapLayer layer, MapLayerTreeModel mapLayerTreeModel) {
            this.layer = layer;
            this.mapLayerTreeModel = mapLayerTreeModel;
        }

        public boolean isContainer() {
            return layer.getName() == null;
        }

        public MapLayer getLayer() {
            return layer;
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return childList().get(childIndex);
        }

        @Override
        public int getChildCount() {
            return childList().size();
        }

        @Override
        public TreeNode getParent() {
            return new LayerNode(layer.getParent(), mapLayerTreeModel);
        }

        @Override
        public int getIndex(TreeNode node) {
            LayerNode layer = (LayerNode)node;
            return childList().indexOf(layer);
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return getChildCount() == 0;
        }

        @Override
        public Enumeration<LayerNode> children() {
            return new Vector<>(childList()).elements();
        }

        private ArrayList<LayerNode> childList() {
            ArrayList<LayerNode> children = new ArrayList<>();

            for (MapLayer mapLayer : layer.getSubLayerList()) {
                children.add(new LayerNode(mapLayer, mapLayerTreeModel));
            }

            if (mapLayerTreeModel.sorted) {
                Collections.sort(children);
            }

            return children;
        }

        @Override
        public boolean equals(Object o) {
            //Needed for the #contains check in MapLayerPanel, as well as #getIndex. [Jon Aquino]
            return (o instanceof LayerNode
                    && layer == ((LayerNode) o).layer);
        }

        @Override
        public int compareTo(LayerNode other) {
            //LayerNode other = (LayerNode) o;
            // [mmichaud 2013-01-15] avoid NPE in case layer has no title
            if (layer.getTitle() != null) {
                return layer.getTitle().compareTo(other.layer.getTitle());
            } else if (layer.getName() != null) {
                return layer.getName().compareTo(other.layer.getName());
            } else if (other.layer.getTitle() == null) {
                return 0;
            } else return -1;
        }
    }
}
