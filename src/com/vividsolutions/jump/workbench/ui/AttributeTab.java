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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openjump.core.ui.plugin.tools.ReplaceValuePlugIn;

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
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.FeatureInfoTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInfoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Implements an Attribute Tab.
 */

public class AttributeTab extends JPanel implements LayerNamePanel {
    private BorderLayout borderLayout1 = new BorderLayout();
    private ErrorHandler errorHandler;
    private TaskFrame taskFrame;
    private LayerManagerProxy layerManagerProxy;

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
        LayerManagerProxy layerManagerProxy, boolean addScrollPanesToChildren) {
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
        panel =
            new AttributePanel(model, workbenchContext, taskFrame, layerManagerProxy, addScrollPanesToChildren) {
            public void layerAdded(LayerTableModel layerTableModel) {
                super.layerAdded(layerTableModel);

                final AttributeTablePanel tablePanel =
                    getTablePanel(layerTableModel.getLayer());
                MouseListener mouseListener = new MouseAdapter() {
                    public void mouseReleased(MouseEvent e) {
                        if (!SwingUtilities.isRightMouseButton(e)) {
                            return;
                        }

                        popupMenu(workbenchContext).setTitle(
                            tablePanel.getModel().getLayer().getName());
                        lastSelectedLayers =
                            new Layer[] { tablePanel.getModel().getLayer()};

                        //Call #setEnableLastSelectedLayers here for EnableChecks that
                        //call #getSelectedLayers. [Jon Aquino]                                        
                        setEnableLastSelectedLayers(true, AttributeTab.this);

                        try {
                            popupMenu(workbenchContext).show(
                                tablePanel.getLayerNameRenderer(),
                                e.getX(),
                                e.getY());
                        } finally {
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
        layerManagerProxy.getLayerManager().addLayerListener(new LayerListener() {
            public void featuresChanged(FeatureEvent e) {}

            public void layerChanged(LayerEvent e) {
                if (e.getType() == LayerEventType.METADATA_CHANGED) {
                    //Editability may have changed. [Jon Aquino]
                    toolBar.updateEnabledState();
                }
            }

            public void categoryChanged(CategoryEvent e) {}
        });
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
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.pan-to-previous-row"),
                IconLoader.icon("SmallUp.gif"),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    pan(panel.topSelectedRow().previousRow());
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
        },
            new MultiEnableCheck().add(taskFrameEnableCheck).add(layersEnableCheck).add(
                rowsSelectedEnableCheck));
        toolBar
            .add(
                new JButton(),
                I18N.get("ui.AttributeTab.zoom-to-full-extent"),
                IconLoader.icon("SmallWorld.gif"),
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
            GUIUtil.toSmallIcon(FeatureInfoTool.ICON),
            FeatureInfoPlugIn.toActionListener(featureInfoPlugIn, workbenchContext, null),
            FeatureInfoPlugIn.createEnableCheck(workbenchContext));
        
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
        toolBar.setOrientation(JToolBar.VERTICAL);
        scrollPane.getViewport().add(panel, null);
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(toolBar, BorderLayout.WEST);
    }

    private void initScrollPane() {
        scrollPane.getVerticalScrollBar().setUnitIncrement(new JTable().getRowHeight());
    }

    private void zoom(AttributePanel.Row row) throws NoninvertibleTransformException {
        panel.clearSelection();
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
        panel.clearSelection();
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

    public static void addPopupMenuItem(
        WorkbenchContext workbenchContext,
        PlugIn plugIn,
        String menuItemName,
        boolean checkBox,
        Icon icon,
        EnableCheck enableCheck) {
        new FeatureInstaller(workbenchContext).addPopupMenuItem(
            popupMenu(workbenchContext),
            wrap(plugIn),
            menuItemName,
            checkBox,
            icon,
            enableCheck);
    }

    private static PlugIn wrap(final PlugIn plugIn) {
        //Can't simply add an ActionListener to the menu item to determine when the
        //plug-in finishes because ActionListeners are notified last to first (see
        //AbstractButton#fireActionPerformed). [Jon Aquino]
        return new PlugIn() {
            public void initialize(PlugInContext context) throws Exception {
                plugIn.initialize(context);
            }

            public boolean execute(PlugInContext context) throws Exception {
                //Save attributeTab before executing plug-in, as it may change active window. [Jon Aquino]
                AttributeTab attributeTab = (AttributeTab) context.getLayerNamePanel();
                setEnableLastSelectedLayers(true, attributeTab);

                try {
                    return plugIn.execute(context);
                } finally {
                    setEnableLastSelectedLayers(false, attributeTab);
                }
            }

            public String getName() {
                return plugIn.getName();
            }
        };
    }

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
