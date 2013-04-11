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
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;

/**
 *  Implements an Attribute Panel.
 */

public class AttributePanel
    extends JPanel
    implements InfoModelListener, AttributeTablePanelListener {
    private SelectionManager selectionManager;
    private BorderLayout borderLayout1 = new BorderLayout();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private HashMap layerToTablePanelMap = new HashMap();
    private InfoModel model;
    private WorkbenchContext workbenchContext;
    private ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn =
        new ZoomToSelectedItemsPlugIn();
    private Row nullRow = new Row() {
        public boolean isFirstRow() {
            return rowCount() == 0;
        }
        public boolean isLastRow() {
            return rowCount() == 0;
        }
        public AttributeTablePanel getPanel() {
            throw new UnsupportedOperationException();
        }
        public int getIndex() {
            throw new UnsupportedOperationException();
        }
        public Row nextRow() {
            return firstRow();
        }
        public Row previousRow() {
            return firstRow();
        }
        private Row firstRow() {
            return new BasicRow(getTablePanel((Layer) getModel().getLayers().get(0)), 0);
        }
        public Feature getFeature() {
            throw new UnsupportedOperationException();
        }
    };
    private TaskFrame taskFrame;
    private LayerManagerProxy layerManagerProxy;
    private boolean addScrollPanesToChildren;
    /**
     * @param layerManagerProxy Can't simply get LayerManager from TaskFrame
     * because when that frame closes, it sets its LayerManager to null.
     */
    protected AttributePanel(
        InfoModel model,
        WorkbenchContext workbenchContext,
        TaskFrame taskFrame,
        LayerManagerProxy layerManagerProxy,
        boolean addScrollPanesToChildren) {
        this.addScrollPanesToChildren = addScrollPanesToChildren;
        selectionManager = new SelectionManager(null, layerManagerProxy);
        selectionManager.setPanelUpdatesEnabled(false);
        this.taskFrame = taskFrame;
        this.workbenchContext = workbenchContext;
        this.layerManagerProxy = layerManagerProxy;
        setModel(model);
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public AttributeTablePanel getTablePanel(Layer layer) {
        return (AttributeTablePanel) layerToTablePanelMap.get(layer);
    }
    public InfoModel getModel() {
        return model;
    }
    public void setModel(InfoModel model) {
        this.model = model;
        model.addListener(this);
    }
    public void layerAdded(LayerTableModel layerTableModel) {
        addTablePanel(layerTableModel);
    }
    public void layerRemoved(LayerTableModel layerTableModel) {
        removeTablePanel(layerTableModel);
    }
    void jbInit() throws Exception {
        setLayout(gridBagLayout1);
    }
    private void removeTablePanel(LayerTableModel layerTableModel) {
        Assert.isTrue(layerToTablePanelMap.containsKey(layerTableModel.getLayer()));
        remove(getTablePanel(layerTableModel.getLayer()));
        layerToTablePanelMap.remove(layerTableModel.getLayer());
        revalidate();
        repaint();
        updateSelectionManager();
    }
    private void addTablePanel(final LayerTableModel layerTableModel) {
        Assert.isTrue(!layerToTablePanelMap.containsKey(layerTableModel.getLayer()));
        final AttributeTablePanel tablePanel =
            new AttributeTablePanel(layerTableModel, addScrollPanesToChildren, workbenchContext);
        tablePanel.addListener(this);
        layerToTablePanelMap.put(layerTableModel.getLayer(), tablePanel);
        add(
            tablePanel,
            new GridBagConstraints(
                0,
                getComponentCount(),
                1,
                1,
                1.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0),
                0,
                0));
        revalidate();
        repaint();
        tablePanel.getTable().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    int row = tablePanel.getTable().rowAtPoint(e.getPoint());
                    if (row == -1) {
                        return;
                    }
                    ArrayList features = new ArrayList();
                    features.add(layerTableModel.getFeature(row));
                    if (taskFrame.isVisible()) {
                        zoomToSelectedItemsPlugIn.flash(
                            FeatureUtil.toGeometries(features),
                            taskFrame.getLayerViewPanel());
                    }
                } catch (Throwable t) {
                    workbenchContext.getErrorHandler().handleThrowable(t);
                }
            }
        });
        tablePanel
            .getTable()
            .getSelectionModel()
            .addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateSelectionManager();
            }
        });
        updateSelectionManager();
    }
    private void updateSelectionManager() {
        selectionManager.clear();
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            selectionManager.getFeatureSelection().selectItems(
                tablePanel.getModel().getLayer(),
                tablePanel.getSelectedFeatures());
        }
    }
    public int rowCount() {
        int rowCount = 0;
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            rowCount += tablePanel.getTable().getRowCount();
        }
        return rowCount;
    }
    public void flashSelectedFeatures() throws NoninvertibleTransformException {
        zoomToSelectedItemsPlugIn.flash(
            FeatureUtil.toGeometries(selectedFeatures()),
            taskFrame.getLayerViewPanel());
    }
    public void zoom(Collection features) throws NoninvertibleTransformException {
        zoomToSelectedItemsPlugIn.zoom(
            FeatureUtil.toGeometries(features),
            taskFrame.getLayerViewPanel());
    }
    public Collection selectedFeatures() {
        ArrayList selectedFeatures = new ArrayList();
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            if (tablePanel.getModel().getRowCount() == 0) {
            	return selectedFeatures;
            }
            int[] selectedRows = tablePanel.getTable().getSelectedRows();
            for (int j = 0; j < selectedRows.length; j++) {
                selectedFeatures.add(tablePanel.getModel().getFeature(selectedRows[j]));
            }
        }
        return selectedFeatures;
    }
    public void selectInLayerViewPanel() {
        taskFrame.getLayerViewPanel().getSelectionManager().clear();
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            int[] selectedRows = tablePanel.getTable().getSelectedRows();
            ArrayList selectedFeatures = new ArrayList();
            for (int j = 0; j < selectedRows.length; j++) {
                selectedFeatures.add(tablePanel.getModel().getFeature(selectedRows[j]));
            }
            taskFrame
                .getLayerViewPanel()
                .getSelectionManager()
                .getFeatureSelection()
                .selectItems(
                tablePanel.getModel().getLayer(),
                selectedFeatures);
        }
    }
    public Row topSelectedRow() {
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel panel = (AttributeTablePanel) i.next();
            int selectedRow = panel.getTable().getSelectedRow();
            if (selectedRow == -1) {
                continue;
            }
            return new BasicRow(panel, selectedRow);
        }
        return nullRow;
    }
    public void selectionReplaced(AttributeTablePanel panel) {
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            if (tablePanel == panel) {
                continue;
            }
            tablePanel.getTable().clearSelection();
        }
    }
    public void clearSelection() {
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            tablePanel.getTable().clearSelection();
        }
    }
    public static interface Row {
        public boolean isFirstRow();
        public boolean isLastRow();
        public AttributeTablePanel getPanel();
        public int getIndex();
        public Row nextRow();
        public Row previousRow();
        public Feature getFeature();
    }
    private class BasicRow implements Row {
        private AttributeTablePanel panel = null;
        private int index;
        public BasicRow(AttributeTablePanel panel, int index) {
            this.panel = panel;
            this.index = index;
        }
        public boolean isFirstRow() {
            return (panel.getModel().getLayer() == getModel().getLayers().get(0))
                && (index == 0);
        }
        public boolean isLastRow() {
            return (
                panel.getModel().getLayer()
                    == getModel().getLayers().get(getModel().getLayers().size() - 1))
                && (index == (panel.getTable().getRowCount() - 1));
        }
        public AttributeTablePanel getPanel() {
            return panel;
        }
        public int getIndex() {
            return index;
        }
        public Row previousRow() {
            if (isFirstRow()) {
                return this;
            }
            if (index > 0) {
                return new BasicRow(panel, index - 1);
            }
            return new BasicRow(
                previousPanel(),
                previousPanel().getTable().getRowCount() - 1);
        }
        public Row nextRow() {
            if (isLastRow()) {
                return this;
            }
            if (index < (panel.getTable().getRowCount() - 1)) {
                return new BasicRow(panel, index + 1);
            }
            return new BasicRow(nextPanel(), 0);
        }
        private AttributeTablePanel previousPanel() {
            return getTablePanel(previousLayer());
        }
        private AttributeTablePanel nextPanel() {
            return getTablePanel(nextLayer());
        }
        private Layer previousLayer() {
            return (Layer) getModel().getLayers().get(
                getModel().getLayers().indexOf(panel.getModel().getLayer()) - 1);
        }
        private Layer nextLayer() {
            return (Layer) getModel().getLayers().get(
                getModel().getLayers().indexOf(panel.getModel().getLayer()) + 1);
        }
        public Feature getFeature() {
            return panel.getModel().getFeature(index);
        }
    }
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
}
