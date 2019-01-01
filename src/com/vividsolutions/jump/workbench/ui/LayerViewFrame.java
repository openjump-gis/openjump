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
package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Task;

/**
 * Provides the internal Frame in which Layer views are rendered.
 */

public class LayerViewFrame extends JInternalFrame {

    protected BorderLayout borderLayout = new BorderLayout();

    protected JScrollPane scrollPane = new JScrollPane();

    protected JSplitPane splitPane = new JSplitPane();

    protected int cloneIndex;

    protected InfoFrame infoFrame = null;

    protected LayerViewPanel layerViewPanel;

    protected Task task;

    protected WorkbenchContext workbenchContext;

    protected LayerManager layerManager;

    public LayerViewPanel getLayerViewPanel() {
        return layerViewPanel;
    }

    public Task getTask() {
        return task;
    }

    public LayerManager getLayerManager() {
        return task.getLayerManager();
    }

    protected int nextCloneIndex() {
        String key = getClass().getName() + " - LAST_CLONE_INDEX";
        task.getLayerManager().getBlackboard().put(
            key,
            1 + task.getLayerManager().getBlackboard().get(key, 0));
    
        return task.getLayerManager().getBlackboard().getInt(key);
    }

    public SelectionManager getSelectionManager() {
        return getLayerViewPanel().getSelectionManager();
    }

    protected void updateTitle() {
        String title = task.getName();
    
        if (cloneIndex > 0) {
            title += (" (View " + (cloneIndex + 1) + ")");
        }
    
        setTitle(title);
    }

    public void taskNameChanged(String name) {
        updateTitle();
    }}
