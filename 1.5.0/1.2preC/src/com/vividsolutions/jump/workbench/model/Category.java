
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

package com.vividsolutions.jump.workbench.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.util.Assert;

/**
 * A folder containing Layers.
 */
public class Category {
    private ArrayList layerables = new ArrayList();
    private String name;
    private LayerManager layerManager;

    public Category() {
    }

    public void setName(String name) {
        this.name = name;
        fireCategoryChanged(CategoryEventType.METADATA_CHANGED);
    }

    public void setLayerManager(LayerManager layerManager) {
        this.layerManager = layerManager;
    }

    public void fireCategoryChanged(CategoryEventType type) {
        if (getLayerManager() == null) {
            //layerManager is null when Java2XML creates the object. [Jon Aquino]
            return;
        }
        
        getLayerManager().fireCategoryChanged(this, type);
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    /**
     * Called by Java2XML
     * @return Layerables with enough information to be saved to a project file
     */
    public List getPersistentLayerables() {
        ArrayList persistentLayerables = new ArrayList();

        for (Iterator i = layerables.iterator(); i.hasNext();) {
            Layerable layerable = (Layerable) i.next();

            if (layerable instanceof Layer &&
                    !((Layer) layerable).hasReadableDataSource()) {
                continue;
            }

            persistentLayerables.add(layerable);
        }

        return persistentLayerables;
    }

    public List getLayerables() {
        return Collections.unmodifiableList(layerables);
    }

    public void remove(Layerable layerable) {
        Assert.isTrue(contains(layerable));
        layerables.remove(layerable);
    }

    /**
     * @return -1 if the category does not contain the layerable
     */
    public int indexOf(Layerable layerable) {
        return layerables.indexOf(layerable);
    }

    public boolean contains(Layerable layerable) {
        return layerables.contains(layerable);
    }

    /**
     * @param index 0 to add to the top
     */
    public void add(int index, Layerable layerable) {
        layerables.add(index, layerable);
        if (getLayerManager() != null) {
            //layerManager is null when Java2XML creates the object. [Jon Aquino]
            getLayerManager().fireLayerChanged(layerable, LayerEventType.ADDED);        
        }                
    }

    /**
     * Called by Java2XML
     */
    public void addPersistentLayerable(Layerable layerable) {
        add(layerables.size(), layerable);
    }

    public boolean isEmpty() {
        return layerables.isEmpty();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }
}
