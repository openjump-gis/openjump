/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.openjump.core.ui.swing.DetachableInternalFrame;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.CloneableInternalFrame;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.OneLayerAttributeTab;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.TaskFrameProxy;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ViewAttributesPlugIn extends AbstractPlugIn {

  // Blackboard keys
  public static final String BB_ATTRIBUTES_WINDOW_SIZE_WIDTH = ViewAttributesPlugIn.class
      .getName() + " - ATTRIBUTES_WINDOW_SIZE_WIDTH";
  public static final String BB_ATTRIBUTES_WINDOW_SIZE_HEIGHT = ViewAttributesPlugIn.class
      .getName() + " - ATTRIBUTES_WINDOW_SIZE_HEIGHT";
  public static final String BB_ATTRIBUTES_WINDOW_POSITION_X = ViewAttributesPlugIn.class
      .getName() + " - ATTRIBUTES_WINDOW_POSITION_X";
  public static final String BB_ATTRIBUTES_WINDOW_POSITION_Y = ViewAttributesPlugIn.class
      .getName() + " - ATTRIBUTES_WINDOW_POSITION_Y";

  private static Blackboard blackboard = null;

  public ViewAttributesPlugIn() {
  }

  @Override
  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    blackboard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());
  }

  @Override
  public String getName() {
    return I18N.get("ui.plugin.ViewAttributesPlugIn.view-edit-attributes");
  }

  @Override
  public boolean execute(final PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    // If the AttributeTable for the currently selected layer is already open,
    // don't create a new ViewAttributesFrame
    for (JInternalFrame iFrame : context.getWorkbenchFrame().getInternalFrames()) {
        if (iFrame instanceof ViewAttributesFrame) {
            if (((ViewAttributesFrame)iFrame)
                    .getOneLayerAttributeTab()
                    .getLayer()
                    .equals(context.getSelectedLayer(0))) {
                iFrame.toFront();
                return true;
            }
        }
    }
    // Don't add GeometryInfoFrame because the HTML will probably be too
    // much for the editor pane (too many features). [Jon Aquino]
    final ViewAttributesFrame frame = new ViewAttributesFrame(
        context.getSelectedLayer(0), context);
    frame.setSize(500, 300);

    context.getWorkbenchFrame().addInternalFrame(frame);
    // restore window position and size from Blackboard. We make this after
    // addInternalFrame, because addInternalFrame calls setLocation...,
    // so we cannot set location in the ViewAttributesFrame constructor :-(
    if (blackboard.get(BB_ATTRIBUTES_WINDOW_POSITION_X) != null) {
      int x = blackboard.getInt(BB_ATTRIBUTES_WINDOW_POSITION_X);
      int y = blackboard.getInt(BB_ATTRIBUTES_WINDOW_POSITION_Y);
      int width = blackboard.getInt(BB_ATTRIBUTES_WINDOW_SIZE_WIDTH);
      int height = blackboard.getInt(BB_ATTRIBUTES_WINDOW_SIZE_HEIGHT);
      frame.setBounds(x, y, width, height);
    }

    return true;
  }

  public MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(
        checkFactory.createTaskWindowMustBeActiveCheck()).add(
        checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
  }

  public static ImageIcon getIcon() {
    // return IconLoaderFamFam.icon("table.png");
    return IconLoader.icon("Row.gif");
  }

  public static class ViewAttributesFrame extends DetachableInternalFrame
      implements LayerManagerProxy, SelectionManagerProxy, LayerNamePanelProxy,
      TaskFrameProxy, LayerViewPanelProxy {
    private LayerManager layerManager;
    private OneLayerAttributeTab attributeTab;
    private static ImageIcon ICON12 = IconLoader.icon("Row_14.gif");
    private static ImageIcon ICON16 = IconLoader.icon("Row_16.gif");

    public ViewAttributesFrame(final Layer layer, final PlugInContext context) {
      this.layerManager = context.getLayerManager();
      addInternalFrameListener(new InternalFrameAdapter() {
        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
          // Assume that there are no other views on the model [Jon
          // Aquino]
          attributeTab.getModel().dispose();

          // save window size and position
          blackboard.put(BB_ATTRIBUTES_WINDOW_SIZE_WIDTH, getSize().width);
          blackboard.put(BB_ATTRIBUTES_WINDOW_SIZE_HEIGHT, getSize().height);
          blackboard.put(BB_ATTRIBUTES_WINDOW_POSITION_X, getLocation().x);
          blackboard.put(BB_ATTRIBUTES_WINDOW_POSITION_Y, getLocation().y);
        }
      });
      setResizable(true);
      setClosable(true);
      setMaximizable(true);
      setIconifiable(true);
      setFrameIcon(ICON12);
      getContentPane().setLayout(new BorderLayout());
      attributeTab = new OneLayerAttributeTab(context.getWorkbenchContext(),
          ((TaskFrameProxy) context.getActiveInternalFrame()).getTaskFrame(),
          this).setLayer(layer);

      getContentPane().add(attributeTab, BorderLayout.CENTER);
      updateTitle(attributeTab.getLayer());
      final LayerListener layerListener = new LayerListener() {
        public void layerChanged(LayerEvent e) {
          if (attributeTab.getLayer() != null) {
            updateTitle(attributeTab.getLayer());
          }
          // Layer REMOVE [mmichaud 2012-01-05]
          if (e.getType() == LayerEventType.REMOVED) {
            if (e.getLayerable() == attributeTab.getLayer()) {
              attributeTab.getModel().dispose();
              context.getWorkbenchFrame().removeInternalFrame(
                      ViewAttributesFrame.this);
              dispose();
            }
          }
        }

        public void categoryChanged(CategoryEvent e) {
        }

        public void featuresChanged(FeatureEvent e) {
        }
      };
      context.getLayerManager().addLayerListener(layerListener);

      addInternalFrameListener(new InternalFrameAdapter() {
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
          attributeTab.getToolBar().updateEnabledState();
        }
        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
          context.getLayerManager().removeLayerListener(layerListener);
          context.getLayerManager().removeLayerListener(attributeTab.attributeTabLayerListener);
          context.getLayerManager().removeLayerListener(attributeTab.oneAttributeTableLayerListener);
        }
      });

      Assert
          .isTrue(
              !(this instanceof CloneableInternalFrame),
              I18N.get("ui.plugin.ViewAttributesPlugIn.there-can-be-no-other-views-on-the-InfoModels"));
    }

    public OneLayerAttributeTab getOneLayerAttributeTab() {
        return attributeTab;
    }

    public LayerViewPanel getLayerViewPanel() {
      return getTaskFrame().getLayerViewPanel();
    }

    public LayerManager getLayerManager() {
      return layerManager;
    }

    private void updateTitle(Layer layer) {
      String editView;
      if (layer.isEditable()) {
        editView = I18N.get("ui.plugin.ViewAttributesPlugIn.edit");
      } else {
        editView = I18N.get("ui.plugin.ViewAttributesPlugIn.view");
      }

      setTitle(" " + I18N.get("ui.plugin.ViewAttributesPlugIn.attributes")
          + ": " + getTaskFrame().getTask().getName() + ":" + layer.getName());
    }

    public TaskFrame getTaskFrame() {
      return attributeTab.getTaskFrame();
    }

    public SelectionManager getSelectionManager() {
      return attributeTab.getPanel().getSelectionManager();
    }

    public LayerNamePanel getLayerNamePanel() {
      return attributeTab;
    }

    @Override
    public JFrame getFrame() {
      // our frame has to be all proxies InfoFrame is
      JFrame f = new InfoFrame.DetachableInternalFrameWithProxies(this);
      f.setIconImage(ICON16.getImage());
      return f;
    }
  }
}