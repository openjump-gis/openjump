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
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Provides proxied (non-spatial) views of a Layer.
 */
public class InfoFrame
    extends JInternalFrame
    implements
        LayerManagerProxy,
        SelectionManagerProxy,
        LayerNamePanelProxy,
        TaskFrameProxy,
        LayerViewPanelProxy {
    public LayerManager getLayerManager() {
        return layerManager;
    }
    public TaskFrame getTaskFrame() {
        return attributeTab.getTaskFrame();
    }
    private LayerManager layerManager;
    private BorderLayout borderLayout1 = new BorderLayout();
    private AttributeTab attributeTab;
    private InfoModel model = new InfoModel();
    private GeometryInfoTab geometryInfoTab;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private WorkbenchFrame workbenchFrame;
    public InfoFrame(
        WorkbenchContext workbenchContext,
        LayerManagerProxy layerManagerProxy,
        final TaskFrame taskFrame) {
        geometryInfoTab = new GeometryInfoTab(model, workbenchContext);
        //Keep my own copy of LayerManager, because it will be nulled in TaskFrame
        //when TaskFrame closes (it may in fact already be closed, which is why
        //a LayerManagerProxy must be passed in too). But I have to 
        //remember to null it when I close. [Jon Aquino]
        Assert.isTrue(layerManagerProxy.getLayerManager() != null);
        layerManager = layerManagerProxy.getLayerManager();
        // I cannot see any reason to add this listener [mmichaud 2007-06-03]
        // See also WorkbenchFrame
        /*addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                layerManager = new LayerManager();
            }
        });*/
        attributeTab = new AttributeTab(model, workbenchContext, taskFrame, this, false);
        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameOpened(InternalFrameEvent e) {
                attributeTab.getToolBar().updateEnabledState();
            }
        });
        workbenchFrame = workbenchContext.getWorkbench().getFrame();
        this.setResizable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        //This size is chosen so that when the user hits the Info tool, the window
        //fits between the lower edge of the TaskFrame and the lower edge of the
        //WorkbenchFrame. See the call to #setSize in WorkbenchFrame. [Jon Aquino]
        //Make sure there's a little space for a custom FeatureTextWriter 
        //[Jon Aquino 12/31/2003]
        this.setSize(550, 185);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        tabbedPane.addTab("", IconLoader.icon("Table.gif"), attributeTab, "Table View");
        tabbedPane.addTab("", IconLoader.icon("Paper.gif"), geometryInfoTab, "HTML View");
        updateTitle(taskFrame.getTask().getName());
        taskFrame.getTask().add(new Task.NameListener() {
            public void taskNameChanged(String name) {
                updateTitle(taskFrame.getTask().getName());
            }
        });
        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                //Assume that there are no other views on the model
                model.dispose();
            }
        });
        layerManagerProxy.getLayerManager().addLayerListener(new LayerListener() {
            public void featuresChanged(FeatureEvent e) {}

            public void layerChanged(LayerEvent e) {
                // Layer REMOVE [mmichaud 2012-01-05]
                if (e.getType() == LayerEventType.REMOVED) {
                    if (getModel().getLayers().contains(e.getLayerable())) {
                        getModel().remove((Layer)e.getLayerable());
                    }
                }
            }

            public void categoryChanged(CategoryEvent e) {}
        });
    }
    public JPanel getAttributeTab() {
        return attributeTab;
    }
    public JPanel getGeometryTab() {
        return geometryInfoTab;
    }
    public void setSelectedTab(JPanel tab) {
        tabbedPane.setSelectedComponent(tab);
    }
    public static String title(String taskName) {
        return I18N.get("ui.InfoFrame.feature-info")+": " + taskName;
    }
    private void updateTitle(String taskName) {
        setTitle(title(taskName));
    }
    public InfoModel getModel() {
        return model;
    }
    private void jbInit() throws Exception {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // With the DefaultInternalFrameCloser of WorkbenchFrame,
        // I think this code is no more necessary [mmichaud 2007-06-03]
        /*addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent e) {
                //Regardless of the defaultCloseOperation, this InfoFrame should be
                //removed from the WorkbenchFrame when the user hits X so it won't
                //appear on the Window list. [Jon Aquino]
                try {
                    workbenchFrame.removeInternalFrame(InfoFrame.this);
                } catch (Exception x) {
                    workbenchFrame.handleThrowable(x);
                }
            }
        });*/
        this.setTitle(I18N.get("ui.InfoFrame.feature-info"));
        this.getContentPane().setLayout(borderLayout1);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        this.getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }
    public void surface() {
        JInternalFrame activeFrame = workbenchFrame.getActiveInternalFrame();
        if (!workbenchFrame.hasInternalFrame(this)) {
            workbenchFrame.addInternalFrame(this, false, true);
        }
        if (activeFrame != null) {
            workbenchFrame.activateFrame(activeFrame);
        }
        moveToFront();
        //Move this frame to the front, but don't activate it if the TaskFrame is
        //active. Otherwise the user would need to re-activate the TaskFrame before
        //making another Info gesture. [Jon Aquino]
    }
    public SelectionManager getSelectionManager() {
        return attributeTab.getPanel().getSelectionManager();
    }
    public LayerNamePanel getLayerNamePanel() {
        return attributeTab;
    }
    public LayerViewPanel getLayerViewPanel() {
        return getTaskFrame().getLayerViewPanel();
    }
}
