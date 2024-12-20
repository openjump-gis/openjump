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

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.swing.JPanel;
import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.ui.zoom.PanToSelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;

/**
 *  Implements an Attribute Panel.
 */

public class AttributePanel extends JPanel implements InfoModelListener {

    //private SelectionManager selectionManager;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private HashMap<Layerable,AttributeTablePanel> layerToTablePanelMap = new HashMap<>();
    private InfoModel model;
    private WorkbenchContext workbenchContext;
    private ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn =
        new ZoomToSelectedItemsPlugIn();
    private PanToSelectedItemsPlugIn panToSelectedItemsPlugIn =
        new PanToSelectedItemsPlugIn();
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
        //selectionManager = new SelectionManager(null, layerManagerProxy);
        //selectionManager.setPanelUpdatesEnabled(false);
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
        return layerToTablePanelMap.get(layer);
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
		// add fillpanel for nice Layout but only if we havn't a scrollpane, because on a scrollpane there are no needs for that
		if (!addScrollPanesToChildren) {
			JPanel fillPanel = new JPanel();
			GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 999; // quick'n dirty because this fillpanel is the first and i don't know here how many comes
			gridBagConstraints.weightx = 0.0;
			gridBagConstraints.weighty = 0.5;
			add(fillPanel, gridBagConstraints);
		}
    }
    private void removeTablePanel(LayerTableModel layerTableModel) {
        Assert.isTrue(layerToTablePanelMap.containsKey(layerTableModel.getLayer()));
        AttributeTablePanel attributeTablePanel = getTablePanel(layerTableModel.getLayer());
        layerManagerProxy.getLayerManager().removeLayerListener(attributeTablePanel.layerListener);
        remove(getTablePanel(layerTableModel.getLayer()));
        layerToTablePanelMap.remove(layerTableModel.getLayer());
        revalidate();
        repaint();
        //updateSelectionManager(); [mmichaud 2015-06-13] refactoring of selection management
    }
    private void addTablePanel(final LayerTableModel layerTableModel) {
        Assert.isTrue(!layerToTablePanelMap.containsKey(layerTableModel.getLayer()));
        final AttributeTablePanel tablePanel =
            new AttributeTablePanel(layerTableModel, addScrollPanesToChildren, workbenchContext);
        //tablePanel.addListener(this); [mmichaud 2015-06-13] listener has been moved to AttributeTablePanel
        layerToTablePanelMap.put(layerTableModel.getLayer(), tablePanel);
		int topInset = layerToTablePanelMap.size() > 1 ? 10 : 0; // a small space on top for 2. and following panel
        add(
            tablePanel,
            new GridBagConstraints(
                0,
                getComponentCount(),
                1,
                1,
                1.0,
                addScrollPanesToChildren ? 1.0 : 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(topInset, 0, 0, 0),
                0,
                0));
        revalidate();
        repaint();
        tablePanel.getTable().addMouseListener(new MouseAdapter() {
			@Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int row = tablePanel.getTable().rowAtPoint(e.getPoint());
                    if (row == -1) {
                        return;
                    }
                    List<Feature> features = new ArrayList<>();
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
        //tablePanel
        //    .getTable()
        //    .getSelectionModel()
        //    .addListSelectionListener(new ListSelectionListener() {
        //    public void valueChanged(ListSelectionEvent e) {
        //        updateSelectionManager();
        //    }
        //});
        //updateSelectionManager(); [mmichaud 2015-06-13] refactoring of selection management
    }

    // [mmichaud 2015-06-13] refactoring of selection management
    /*
    private void updateSelectionManager() {
      for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
        AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
        Layer layer = tablePanel.getModel().getLayer();
        Collection<Feature> selected_tab = tablePanel.getSelectedFeatures();
        Collection<Feature> selected_old = selectionManager.getFeatureSelection()
            .getFeaturesWithSelectedItems(layer);
        Collection<Feature> selected = new ArrayList<Feature>();
        Collection<Feature> unselected = new ArrayList<Feature>();
        // keep selection order
        for (Feature feature : selected_old) {
          if (!selected_tab.contains(feature))
            unselected.add(feature);
        }
        selectionManager.getFeatureSelection().unselectItems(layer, unselected);
        // add newly selected to list end
        for (Feature feature : selected_tab) {
          if (!selected_old.contains(feature))
            selected.add(feature);
        }
        selectionManager.getFeatureSelection().selectItems(layer, selected);
      }
    }
    */

    public int rowCount() {
        int rowCount = 0;
        for (AttributeTablePanel tablePanel : layerToTablePanelMap.values()) {
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

    public void pan(Collection features) throws NoninvertibleTransformException {
        panToSelectedItemsPlugIn.pan(
            FeatureUtil.toGeometries(features),
            taskFrame.getLayerViewPanel());
    }

    public Collection<Feature> selectedFeatures() {
        List<Feature> selectedFeatures = new ArrayList<>();
        for (AttributeTablePanel tablePanel : layerToTablePanelMap.values()) {
            if (tablePanel.getModel().getRowCount() == 0) {
                continue;
            }
            int[] selectedRows = tablePanel.getTable().getSelectedRows();
            for (int j : selectedRows) {
                selectedFeatures.add(tablePanel.getModel().getFeature(j));
            }
        }
        return selectedFeatures;
    }

    // [mmichaud 2015-06-13] moved to AttributeTablePanel
    //
    public void selectInLayerViewPanel() {
        taskFrame.getLayerViewPanel().getSelectionManager().clear();
        for (AttributeTablePanel tablePanel : layerToTablePanelMap.values()) {
            int[] selectedRows = tablePanel.getTable().getSelectedRows();
            List<Feature> selectedFeatures = new ArrayList<>();
            for (int j : selectedRows) {
                selectedFeatures.add(tablePanel.getModel().getFeature(j));
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
        for (AttributeTablePanel panel : layerToTablePanelMap.values()) {
            int selectedRow = panel.getTable().getSelectedRow();
            if (selectedRow == -1) {
                continue;
            }
            return new BasicRow(panel, selectedRow);
        }
        return nullRow;
    }

    // [mmichaud 2015-06-13] moved to AttributeTablePanel
    /*
    public void selectionReplaced(AttributeTablePanel panel) {
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            if (tablePanel == panel) {
                // this one liner prevents the feature being edited to be deleted (BUG#3178207)
                if (tablePanel.getTable().isEditing()) tablePanel.getTable().clearSelection();
                else continue;
            }
            tablePanel.getTable().clearSelection();
        }
        selectInLayerViewPanel();
    }
    public void clearSelection() {
        for (Iterator i = layerToTablePanelMap.values().iterator(); i.hasNext();) {
            AttributeTablePanel tablePanel = (AttributeTablePanel) i.next();
            tablePanel.getTable().clearSelection();
        }
    }
    */

    public interface Row {
        boolean isFirstRow();
        boolean isLastRow();
        AttributeTablePanel getPanel();
        int getIndex();
        Row nextRow();
        Row previousRow();
        Feature getFeature();
    }

    private class BasicRow implements Row {
        private AttributeTablePanel panel;
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
            return getModel().getLayers().get(
                getModel().getLayers().indexOf(panel.getModel().getLayer()) - 1);
        }
        private Layer nextLayer() {
            return getModel().getLayers().get(
                getModel().getLayers().indexOf(panel.getModel().getLayer()) + 1);
        }
        public Feature getFeature() {
            return panel.getModel().getFeature(index);
        }
    }
    public SelectionManager getSelectionManager() {
        return workbenchContext.getLayerViewPanel().getSelectionManager();
    }
}
