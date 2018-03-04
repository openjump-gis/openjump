
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

package com.vividsolutions.jump.workbench.plugin;

import java.util.Collection;

import javax.swing.JInternalFrame;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.driver.DriverManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerableNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerableNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Passed to PlugIns to enable them to access the rest of the JUMP Workbench.
 * contains preselected task, 
 * @see PlugIn
 */
public class PlugInContext implements LayerViewPanelProxy,
        LayerNamePanelProxy, LayerableNamePanelProxy, LayerManagerProxy {
    private Task task;
    private LayerNamePanel layerNamePanel;
    private LayerViewPanel layerViewPanel;
    private WorkbenchContext workbenchContext;
    private LayerManagerProxy layerManagerProxy;

    public PlugInContext(
        WorkbenchContext workbenchContext,
        Task task,
        LayerManagerProxy layerManagerProxy,
        LayerNamePanel layerNamePanel,
        LayerViewPanel layerViewPanel) {
        this.workbenchContext = workbenchContext;
        this.task = task;
        this.layerManagerProxy = layerManagerProxy;
        this.layerNamePanel = layerNamePanel;
        this.layerViewPanel = layerViewPanel;
    }

    public DriverManager getDriverManager() {
        return workbenchContext.getDriverManager();
    }

    public ErrorHandler getErrorHandler() {
        return workbenchContext.getErrorHandler();
    }

    public WorkbenchContext getWorkbenchContext() {
        return workbenchContext;
    }

    /**
     *@return the ith layer clicked on the layer-list panel,
     * or null if the user hasn't clicked an ith layer
     */
    public Layer getSelectedLayer(int i) {
        Layer[] selectedLayers = getSelectedLayers();

        if (selectedLayers.length > i) {
            return selectedLayers[i];
        }

        return null;
    }

    /**
     * @return the ith selected layer, or if there is none, the ith layer
     */
    public Layer getCandidateLayer(int i) {
        Layer lyr = getSelectedLayer(i);

        if (lyr != null) {
            return lyr;
        }

        return getLayerManager().getLayer(i);
    }

    //<<TODO:DESIGN>> Return List instead of array [Jon Aquino]
    public Layer[] getSelectedLayers() {
        return getLayerNamePanel().getSelectedLayers();
    }

    /**
     * this extends getSelectedLayers() as it returns all selected Layerables
     * e.g. WMSLayers
     * @return null or a Collection of Layerables
     */
    public Collection<Layerable> getSelectedLayerables() {
      if ( getLayerNamePanel() instanceof LayerableNamePanel )
        return ((LayerableNamePanel)getLayerNamePanel()).getSelectedLayerables();
      return null;
    }

    public Envelope getSelectedLayerEnvelope() {
        return getSelectedLayer(0).getFeatureCollectionWrapper().getEnvelope();
    }

    public Task getTask() {
        return task;
    }

    public LayerNamePanel getLayerNamePanel() {
     return layerNamePanel;
    }

    public LayerableNamePanel getLayerableNamePanel() {
      if ( layerNamePanel instanceof LayerableNamePanel )
        return (LayerableNamePanel) layerNamePanel;
      return null;
    }

    public LayerManager getLayerManager() {
        return layerManagerProxy.getLayerManager();
    }

    public LayerViewPanel getLayerViewPanel() {
        return layerViewPanel;
    }

    public WorkbenchFrame getWorkbenchFrame() {
        return workbenchContext.getWorkbench().getFrame();
    }

    public Layer addLayer(
        String categoryName,
        String layerName,
        FeatureCollection featureCollection) {
        return getLayerManager().addLayer(categoryName, layerName, featureCollection);
    }

    public void addLayerSextanteRaster(
            String categoryName,
            RasterImageLayer raster) {
            getLayerManager().addLayerable(categoryName, raster);
        }
    
    public HTMLFrame getOutputFrame() {
        return workbenchContext.getWorkbench().getFrame().getOutputFrame();
    }

    public JInternalFrame getActiveInternalFrame() {
        return workbenchContext.getWorkbench().getFrame().getActiveInternalFrame();
    }

    public EnableCheckFactory getCheckFactory() {
        return EnableCheckFactory.getInstance();
    }

    public FeatureInstaller getFeatureInstaller() {
        return FeatureInstaller.getInstance();
    }

}
