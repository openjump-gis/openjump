/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import com.vividsolutions.jump.datastore.DataStoreLayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.tree.TreeModel;

public class DataStoreLayerTreeModel implements TreeModel {

    private LinkedHashMap<String, ArrayList<DataStoreLayer>> root;

    public DataStoreLayerTreeModel(LinkedHashMap<String, ArrayList<DataStoreLayer>> root) {
        this.root = root;
    }
    
    /**
     * Refreshes the treeModelData
     */
    public void setData(LinkedHashMap<String, ArrayList<DataStoreLayer>> root) {
        this.root = root;
    }

    @Override
    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        //this.
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof LinkedHashMap) {
            return root.keySet().toArray()[index];
        } else if (parent instanceof String) {
            String l = (String) parent;
            List<DataStoreLayer> layers = root.get(l);
            return layers.get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof LinkedHashMap) {
            // root node containing all schemas
            return root.size();
        } else if (parent instanceof String) {
            String l = (String) parent;
            List<DataStoreLayer> layers = root.get(l);
            return layers.size();

        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof LinkedHashMap) {
                // root node containing all schemas
            // parent = root
            String ch = (String) child;
            String[] schemas = root.keySet().toArray(new String[root.keySet().size()]);
            for (int i = 0; i < schemas.length; i++) {
                if (ch.equals(schemas[i])) {
                    return i;
                }
            }
        } else if (parent instanceof String) {
            List<DataStoreLayer> layers = root.get(parent);
            DataStoreLayer ch = (DataStoreLayer) child;
            return layers.indexOf(ch);
        }
        // no other case ?
        return 0;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
        // TODO: clean
        if (node instanceof HashMap || node instanceof String) {
            // root node or schema node
            return false;
        }
        // a leaf: 
        return true;
    }

    @Override
    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        //do nothing
    }

    @Override
    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
        //do nothing
    }

}
