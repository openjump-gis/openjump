
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Whatever the user needs to do a particular task. Currently a thin wrapper
 * around a LayerManager.
 */
//I wonder if this class should be named "Project" instead. [Jon Aquino]
public class Task implements LayerManagerProxy {
    private String name = "";
    private ArrayList nameListeners = new ArrayList();
    private File projectFile = null;

    //No parameters so it can be created by Java2XML.
    public Task() {
        layerManager = new LayerManager();
    }

    private LayerManager layerManager;

    public void add(NameListener nameListener) {
        nameListeners.add(nameListener);
    }

    public void setName(String name) {
        this.name = name;
        fireNameChanged(name);
    }

    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public File getProjectFile() {
        return projectFile;
    }

    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    private void fireNameChanged(String name) {
        for (Iterator i = nameListeners.iterator(); i.hasNext();) {
            NameListener nameListener = (NameListener) i.next();
            nameListener.taskNameChanged(name);
        }
    }

    public Collection getCategories() {
        return getLayerManager().getCategories();
    }

    /**
     * Called by Java2XML
     */
    public void addCategory(Category category) {
        getLayerManager().addCategory(category.getName());

        Category actual = getLayerManager().getCategory(category.getName());

        for (Iterator i = category.getLayerables().iterator(); i.hasNext();) {
            Layerable layerable = (Layerable) i.next();
            actual.addPersistentLayerable(layerable);
        }
    }

    /**
     * Interface: NameListener must respond to task name changing.
     */
    public static interface NameListener {
        public void taskNameChanged(String name);
    }

}
