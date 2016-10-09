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

package com.vividsolutions.jump.workbench;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.driver.DriverManager;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;

/**
 * Convenience methods for accessing the various elements in the Workbench
 * structure. Some getters return null -- subclasses may choose to override them
 * or leave them unimplemented, depending on their needs.
 */
public abstract class WorkbenchContext implements LayerViewPanelProxy,
        LayerNamePanelProxy, LayerableNamePanelProxy, LayerManagerProxy {
    public DriverManager getDriverManager() {
        return null;
    }

    abstract public JUMPWorkbench getWorkbench();

    abstract public ErrorHandlerV2 getErrorHandler();

    abstract public Blackboard getBlackboard();

    /**
     * @Deprecated in favor of getLayerableNamePanel()
     */
    @Deprecated
    abstract public LayerNamePanel getLayerNamePanel();

    abstract public LayerableNamePanel getLayerableNamePanel();

    public LayerViewPanel getLayerViewPanel() {
        return null;
    }

    //Sometimes you can have a layer manager but no layer view panel
    //e.g. when the attribute window is at the forefront. [Jon Aquino]
    public LayerManager getLayerManager() {
        return null;
    }

    public Task getTask() {
        return null;
    }

    /**
     * Creates a snapshot of the system for use by plug-ins.
     */
    public PlugInContext createPlugInContext() {
        return new PlugInContext(this, getTask(), this, getLayerNamePanel(),
                getLayerViewPanel());
    }

    public FeatureTextWriterRegistry getFeatureTextWriterRegistry() {
        return featureTextWriterRegistry;
    }

    private Registry registry = new Registry();

    private FeatureTextWriterRegistry featureTextWriterRegistry = new FeatureTextWriterRegistry();

    public Registry getRegistry() {
        return registry;
    }
}