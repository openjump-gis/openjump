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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInfoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import org.openjump.core.ui.plugin.view.ViewOptionsPlugIn;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;
import java.util.List;

/**
 * Implements an Attribute Tab.
 */

public class AttributeTab extends JPanel implements LayerableNamePanel {
    private BorderLayout borderLayout1 = new BorderLayout();
    private ErrorHandler errorHandler;
    private TaskFrame taskFrame;
    private LayerManagerProxy layerManagerProxy;
    public LayerListener attributeTabLayerListener;
    private static final String sNoModifiedWritableLayerSelected = I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.No-modified-writable-layer-selected");
    //The String values returned by these EnableChecks are not used.
    //The only thing checked is whether they are null or not. [Jon Aquino]
    private EnableCheck taskFrameEnableCheck = new EnableCheck() {
        public String check(JComponent component) {
            return (!taskFrame.isVisible()) ? I18N.get("ui.AttributeTab.task-frame-must-be-open") : null;
        }
    };

    private EnableCheck layersEnableCheck = new EnableCheck() {
        public String check(JComponent component) {
            return panel.getModel().getLayers().isEmpty()
                ? I18N.get("ui.AttributeTab.one-or-more-layers-must-be-present")
                : null;
        }
    };

    private AttributePanel panel;
    private JScrollPane scrollPane = new JScrollPane();
    private EnableCheck rowsSelectedEnableCheck = new EnableCheck() {
        public String check(JComponent component) {
            return panel.selectedFeatures().isEmpty()
                ? I18N.get("ui.AttributeTab.one-or-more-rows-must-be-selected")
                : null;
        }
    };

    private EnableableToolBar toolBar = new EnableableToolBar();
    private InfoModel model;
    private Layer[] selectedLayers = new Layer[] {};
    private Layer[] lastSelectedLayers = new Layer[] {};

    public InfoModel getModel() {
        return model;
    }

    public AttributeTab(
            final InfoModel model,
            final WorkbenchContext workbenchContext,
            final TaskFrame taskFrame,
            final LayerManagerProxy layerManagerProxy, boolean addScrollPanesToChildren) {
        this.layerManagerProxy = layerManagerProxy;
        this.model = model;
        this.taskFrame = taskFrame;
        taskFrame
            .addInternalFrameListener(
                GUIUtil
                .toInternalFrameListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toolBar.updateEnabledState();
            }
        }));
        panel = new AttributePanel(model, workbenchContext,
                taskFrame, layerManagerProxy, addScrollPanesToChildren) {

            public void layerAdded(LayerTableModel layerTableModel) {
                super.layerAdded(layerTableModel);

                final AttributeTablePanel tablePanel =
                    getTablePanel(layerTableModel.getLayer());
                
                MouseListener mouseListener = new MouseAdapter() {
                  public void mousePressed(MouseEvent e) {
                    // popup triggers are pressed on Linux/OSX, released on Windows
                    _mouseReleased(e);
                  }
        
                  public void mouseReleased(MouseEvent e) {
                    _mouseReleased(e);
                  }
        
                  private void _mouseReleased(MouseEvent e) {
                    // [ede 04.2014] use isPopupTrigger which is
                    // supposed to be _really_ crossplatform
                    if (!e.isPopupTrigger()) {
                      return;
                    }

                    popupMenu(workbenchContext).setTitle(
                        tablePanel.getModel().getLayer().getName());
                    lastSelectedLayers = new Layer[] { tablePanel.getModel().getLayer() };

                    // Call #setEnableLastSelectedLayers here for EnableChecks that
                    // call #getSelectedLayers. [Jon Aquino]
                    setEnableLastSelectedLayers(true, AttributeTab.this);

                    try {
                      // place the popup 10px to the right as to circumvent accidental interaction with it
                      popupMenu(workbenchContext).show(
                          tablePanel, e.getX()+10, e.getY());
                    }
                    finally {
                      setEnableLastSelectedLayers(false, AttributeTab.this);
                    }
                  }
                };

                tablePanel.addMouseListener(mouseListener);
                tablePanel.getTable().addMouseListener(mouseListener);
                tablePanel.getTable().getTableHeader().addMouseListener(mouseListener);
                tablePanel.getLayerNameRenderer().addMouseListener(mouseListener);
            }
        };

        attributeTabLayerListener = new LayerListener() {
            public void featuresChanged(FeatureEvent e) {}

            public void layerChanged(LayerEvent e) {
                if (e.getType() == LayerEventType.METADATA_CHANGED) {
                    //Editability may have changed. [Jon Aquino]
                    toolBar.updateEnabledState();
                }
            }

            public void categoryChanged(CategoryEvent e) {}
        };

        layerManagerProxy.getLayerManager().addLayerListener(attributeTabLayerListener);

        model.addListener(new InfoModelListener() {
            public void layerAdded(LayerTableModel layerTableModel) {
                panel
                    .getTablePanel(layerTableModel.getLayer())
                    .getTable()
                    .getSelectionModel()
                    .addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        toolBar.updateEnabledState();
                    }
                });
                toolBar.updateEnabledState();
            }

            public void layerRemoved(LayerTableModel layerTableModel) {
                toolBar.updateEnabledState();
            }
        });
        this.errorHandler = workbenchContext.getErrorHandler();

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        initScrollPane();
        if (addScrollPanesToChildren) {
            remove(scrollPane);
            add(panel, BorderLayout.CENTER);
            
        }        
        installToolBarButtons(workbenchContext, taskFrame);                  
        toolBar.updateEnabledState();
    }

    private void installToolBarButtons(final WorkbenchContext workbenchContext, final TaskFrame taskFrame) {
    	
    	
    	/*
    	EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    	
    	  SaveDatasetsPlugIn saveDatasetsPlugIn = new SaveDatasetsPlugIn();
          toolBar.add(
              new JButton(),
              saveDatasetsPlugIn.getName(),
              GUIUtil.toSmallIcon(SaveDatasetsPlugIn.ICON),
              SaveDatasetsPlugIn.toActionListener(saveDatasetsPlugIn, workbenchContext, null),
              new MultiEnableCheck()
              //.add(taskFrameEnableCheck).add(layersEnableCheck)
                 .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
                  .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1))  
                  .add(new EnableCheck() {
              public String check(JComponent component) {
                  Layer[] lyrs = workbenchContext.getLayerNamePanel().getSelectedLayers();
                  boolean changesToSave = false;
                  for (Layer lyr : lyrs) {
                      if (!lyr.isReadonly() &&
                          lyr.hasReadableDataSource() &&
                          lyr.isFeatureCollectionModified()) return null;
                  }
                  return sNoModifiedWritableLayerSelected;
              }
          })
               );
          toolBar.addSeparator();
          
          UndoPlugIn undoPlugIn = new UndoPlugIn();
          toolBar.add(
              new JButton(),
              undoPlugIn.getName(),
              GUIUtil.toSmallIcon(undoPlugIn.getIcon()),
              UndoPlugIn.toActionListener(undoPlugIn, workbenchContext, null),
              undoPlugIn.createEnableCheck(workbenchContext));
     
      RedoPlugIn redoPlugIn = new RedoPlugIn();
      toolBar.add(
          new JButton(),
          redoPlugIn.getName(),
          GUIUtil.toSmallIcon(redoPlugIn.getIcon()),
          RedoPlugIn.toActionListener(redoPlugIn, workbenchContext, null),
          redoPlugIn.createEnableCheck(workbenchContext));    
         
                
          
          toolBar.addSeparator();
          
          ViewSchemaPlugIn viewSchemaPlugIn = new ViewSchemaPlugIn(null);
          toolBar.add(
              new JButton(),
              viewSchemaPlugIn.getName(),
              GUIUtil.toSmallIcon(ViewSchemaPlugIn.ICON),
              ViewSchemaPlugIn.toActionListener(viewSchemaPlugIn, workbenchContext, null),
              new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
      	
          toolBar.addSeparator();   	
    	*/
    	toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.pan-to-previous-row"),
                IconLoader.icon("SmallUp.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    pan(panel.topSelectedRow().previousRow());
                    //panel.selectInLayerViewPanel();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        }, new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.zoom-to-previous-row"),
                IconLoader.icon("SmallMagnifyUp.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    zoom(panel.topSelectedRow().previousRow());
                    //panel.selectInLayerViewPanel();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        }, new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.zoom-to-next-row"),
                IconLoader.icon("SmallMagnifyDown.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    zoom(panel.topSelectedRow().nextRow());
                    //panel.selectInLayerViewPanel();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        }, new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.pan-to-next-row"),
                IconLoader.icon("SmallDown.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    pan(panel.topSelectedRow().nextRow());
                    //panel.selectInLayerViewPanel();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        }, new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.zoom-to-selected-rows"),
                IconLoader.icon("SmallMagnify.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.zoom(panel.selectedFeatures());
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        }, new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck)
                    .add(rowsSelectedEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.pan-to-selected-rows"),
                IconLoader.icon("MoveTo.gif"),
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            panel.pan(panel.selectedFeatures());
                        } catch (Throwable t) {
                            errorHandler.handleThrowable(t);
                        }
                    }
                }, new MultiEnableCheck().add(taskFrameEnableCheck)
                                         .add(layersEnableCheck)
                                         .add(rowsSelectedEnableCheck)
            );
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.zoom-to-full-extent"),
                IconLoader.icon("globe3_16.png"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    taskFrame.getLayerViewPanel().getViewport().zoomToFullExtent();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        }, new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.select-in-task-window"),
                IconLoader.icon("SmallSelect.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.selectInLayerViewPanel();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        },
            new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck).add(
                rowsSelectedEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.flash-selected-rows"),
                IconLoader.icon("Flashlight.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.flashSelectedFeatures();
                } catch (Throwable t) {
                    errorHandler.handleThrowable(t);
                }
            }
        },
            new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck).add(
                rowsSelectedEnableCheck));

        
        FeatureInfoPlugIn featureInfoPlugIn = new FeatureInfoPlugIn();
        toolBar.add(
            new JButton(),
            featureInfoPlugIn.getName(),
            featureInfoPlugIn.ICON,
            FeatureInfoPlugIn.toActionListener(featureInfoPlugIn, workbenchContext, null),
            //FeatureInfoPlugIn.createEnableCheck(workbenchContext));
            // Fix BUG ID: 3441486
            new MultiEnableCheck().add(taskFrameEnableCheck)
                                  .add(layersEnableCheck)
                                  .add(rowsSelectedEnableCheck));
        
        //-- [sstein 4 nov 2006] added replace value
        /* but is not yet activated since problems appear:
         * either with enableCheck => nullpointer
         * or with actionPerformed => update of window?
         */
        /**
        ReplaceValuePlugIn myReplacePlugIn = new ReplaceValuePlugIn();
        toolBar.add(
                new JButton(),
                myReplacePlugIn.getName(),
                GUIUtil.toSmallIcon(ReplaceValuePlugIn.ICON),
                ReplaceValuePlugIn.toActionListener(myReplacePlugIn, workbenchContext, null),
                ReplaceValuePlugIn.createEnableCheck(workbenchContext));
         **/
        //-- [Giuseppe Aruta 1 gen. 2012] added Attribute Statistics PlugIn
        /* Deactivated as it appears also in Info Panel
        */
        /**
            toolBar.addSeparator();
        
        StatisticOverViewPlugIn statisticOverViewPlugIn = new StatisticOverViewPlugIn();
        toolBar.add(
            new JButton(),
            statisticOverViewPlugIn.getName(),
            GUIUtil.toSmallIcon(IconLoader.icon("statistics.png")),
            StatisticOverViewPlugIn.toActionListener(statisticOverViewPlugIn, workbenchContext, null),
            new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck));
        
        
       **/
        
        
        
    }

    public TaskFrame getTaskFrame() {
        return taskFrame;
    }

    public Layer chooseEditableLayer() {
        return TreeLayerNamePanel.chooseEditableLayer(this);
    }

    public LayerManager getLayerManager() {
        return layerManagerProxy.getLayerManager();
    }

    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        toolBar.setOrientation(JToolBar.HORIZONTAL);
        scrollPane.getViewport().add(panel, null);
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(toolBar, BorderLayout.NORTH);
    }

    private void initScrollPane() {
        scrollPane.getVerticalScrollBar().setUnitIncrement(new JTable().getRowHeight());
    }

    private void zoom(AttributePanel.Row row) throws NoninvertibleTransformException {
        //panel.clearSelection();
        //fixed : if the layer don't have any feature, do nothing.
        if (row.getPanel().getTable().getModel().getRowCount() == 0) {
        	return;
        }
        row.getPanel().getTable().getSelectionModel().setSelectionInterval(
            row.getIndex(),
            row.getIndex());

        Rectangle r = row.getPanel().getTable().getCellRect(row.getIndex(), 0, true);
        row.getPanel().getTable().scrollRectToVisible(r);

        if (row.isFirstRow()) {
            //Make header visible [Jon Aquino]
            row.getPanel().scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        }

        ArrayList features = new ArrayList();
        features.add(row.getFeature());
        panel.zoom(features);
    }
    
    private void pan(AttributePanel.Row row) throws NoninvertibleTransformException {
        //panel.clearSelection();
        //fixed : if the layer don't have any feature, do nothing.
        if (row.getPanel().getTable().getModel().getRowCount() == 0) {
        	return;
        }
        row.getPanel().getTable().getSelectionModel().setSelectionInterval(
            row.getIndex(),
            row.getIndex());

        Rectangle r = row.getPanel().getTable().getCellRect(row.getIndex(), 0, true);
        row.getPanel().getTable().scrollRectToVisible(r);

        if (row.isFirstRow()) {
            //Make header visible [Jon Aquino]
            row.getPanel().scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        }

        ArrayList features = new ArrayList();
        features.add(row.getFeature());
        panel.pan(features);
    }

    public static TitledPopupMenu popupMenu(WorkbenchContext context) {
        return (TitledPopupMenu) context.getWorkbench().getBlackboard().get(
            AttributeTab.class.getName() + " - LAYER POPUP MENU",
            new TitledPopupMenu());
    }

    /*
    public static void addPopupMenuItem(
        WorkbenchContext workbenchContext,
        PlugIn plugIn,
        String menuItemName,
        boolean checkBox,
        Icon icon,
        EnableCheck enableCheck) {
        FeatureInstaller.getInstance().addPopupMenuItem(
            popupMenu(workbenchContext),
            wrap(plugIn),
            menuItemName,
            checkBox,
            icon,
            enableCheck);
    }

    private static PlugIn wrap(final PlugIn plugIn) {
      // Can't simply add an ActionListener to the menu item to determine when the
      // plug-in finishes because ActionListeners are notified last to first (see
      // AbstractButton#fireActionPerformed). [Jon Aquino]
      return new PlugIn() {
        public void initialize(PlugInContext context) throws Exception {
          plugIn.initialize(context);
        }
  
        public boolean execute(PlugInContext context) throws Exception {
          // Save attributeTab before executing plug-in, as it may change active
          // window. [Jon Aquino]
          LayerNamePanelProxy lnpp;
          // check if we are detached
          Window w = KeyboardFocusManager.getCurrentKeyboardFocusManager()
              .getFocusedWindow();
          if (w instanceof InfoFrame.DetachableInternalFrameWithProxies)
            lnpp = (LayerNamePanelProxy) w;
          else {
            lnpp = ((LayerNamePanelProxy) context.getWorkbenchFrame()
                .getActiveInternalFrame());
          }
  
          LayerNamePanel lnp = lnpp.getLayerNamePanel();
          if (lnp instanceof AttributeTab) {
            AttributeTab attributeTab = (AttributeTab) lnp;
            setEnableLastSelectedLayers(true, attributeTab);
  
            try {
              return plugIn.execute(context);
            } finally {
              setEnableLastSelectedLayers(false, attributeTab);
            }
  
          }
          return false;
        }
  
        public String getName() {
          return plugIn.getName();
        }
      };
    }
    */

    private static void setEnableLastSelectedLayers(
        boolean enabled,
        AttributeTab attributeTab) {
        attributeTab.selectedLayers =
            enabled ? attributeTab.lastSelectedLayers : new Layer[] {};
    }

    public Collection getSelectedCategories() {
        return new ArrayList();
    }

    public Layer[] getSelectedLayers() {
        if (model.getLayers().size() == 1) {
            return new Layer[] {(Layer) model.getLayers().get(0)};
        }

        return selectedLayers;
    }

    public Collection<Layerable> getSelectedLayerables() {
      List<Layerable> ls = (List)Arrays.asList(getSelectedLayers());
      return ls;
    }

    public Collection selectedNodes(Class c) {
        if (!Layerable.class.isAssignableFrom(c)) {
            return new ArrayList();
        }

        return Arrays.asList(getSelectedLayers());
    }

    public AttributePanel getPanel() {
        return panel;
    }
    public EnableableToolBar getToolBar() {
        return toolBar;
    }

    public void addListener(LayerNamePanelListener listener) {}
    public void removeListener(LayerNamePanelListener listener) {}

    public void dispose() {}

}
