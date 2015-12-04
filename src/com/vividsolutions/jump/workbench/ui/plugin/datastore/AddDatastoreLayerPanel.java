package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeModel;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;

// TODO             String s1 = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Prevents-unnecessary-queries-to-the-datastore");
//            String s2 = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.The-recommended-setting-is-to-leave-this-checked");
//            cachingCheckBox.setToolTipText("<html>" + s1 + "<br>" + s2 + "</html>");
public class AddDatastoreLayerPanel extends ConnectionPanel {

  private Map connectionDescriptorToDatasetNamesMap = new HashMap();

  // new components for the tree list of layers for a given dataset
  private Outline datasetOutline = null;
  private TreeModel datasetTreeModel = null;
  private OutlineModel datasetOutlineModel = null;
  // The list of currently selected layers
  private ArrayList<DataStoreLayer> selectedLayers = null;

  // dummy constructor for JBuilder - do not use!!!
  public AddDatastoreLayerPanel() {
    super(null);
  }

    public AddDatastoreLayerPanel(WorkbenchContext context) {
        super(context);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initialize();
            }
        });
        //initialize();
        getConnectionComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //getDatasetComboBox().setSelectedItem( null );
                getDatasetOutline();
            }
        });
    }

  public static Object runInKillableThread(final String description,
      WorkbenchContext context, final Block block) {
    final Object[] result = new Object[]{null};
    // ThreadedBasePlugIn displays a dialog that the user can
    // use to kill the thread by pressing the close button
    // [Jon Aquino 2005-03-14]
    AbstractPlugIn.toActionListener(
        new ThreadedBasePlugIn() {
          public String getName() {
            return description;
          }

          public boolean execute(PlugInContext context) throws Exception {
            return true;
          }

          public void run(TaskMonitor monitor, PlugInContext context)
          throws Exception {
            monitor.report(description);
            result[0] = block.yield();
          }
        }, context, new TaskMonitorManager()).actionPerformed(null);
    return result[0];
  }

  public List<DataStoreLayer> getDatasetLayers() {
    return this.selectedLayers;
  }

  public String validateInput() {
    if (super.validateInput() != null) {
      return super.validateInput();
    }
    if (this.selectedLayers == null || this.selectedLayers.size() == 0) {
      return I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Required-field-missing-Dataset");
    }
    return null;
  }

  private Outline getDatasetOutline() {
    if (datasetOutline == null) {

      LinkedHashMap<String, ArrayList<DataStoreLayer>> root = new LinkedHashMap<String, ArrayList<DataStoreLayer>>();
      datasetTreeModel = new DataStoreLayerTreeModel(root);
      datasetOutlineModel = DefaultOutlineModel.createOutlineModel(
          datasetTreeModel, new DataStoreLayerRowModel(), true,
          I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Schemas"));

      datasetOutline = new Outline();
      //datasetOutline.setBackground(Color.lightGray);

      // Table selection: build the 
      datasetOutline.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          // refreshes the list of selected DataStoreLayers if user is not manipulating 
          // the selection
//                    System.out.println("event:" + e);

          ListSelectionModel lsm = (ListSelectionModel) e.getSource();
          if (!e.getValueIsAdjusting()) {
            if (!lsm.isSelectionEmpty()) {
              selectedLayers = new ArrayList<DataStoreLayer>();
              // Find out which indexes are selected.
              int minIndex = lsm.getMinSelectionIndex();
              int maxIndex = lsm.getMaxSelectionIndex();
              for (int i = minIndex; i <= maxIndex; i++) {
                if (lsm.isSelectedIndex(i)) {
                  Object o = datasetOutline.getValueAt(i, 0);
                  // stores only DataStoreLayer
                  if (o instanceof DataStoreLayer) {
                    selectedLayers.add((DataStoreLayer) o);
                    //System.out.println("adding a layer in selection for idx: " + i + " ds: " + ((DataStoreLayer) o).toString());
                  }
                }
              }
            }
          }
        }
      }
      );

      datasetOutline.setRootVisible(false);
      //Assign the model to the Outline object:
      datasetOutline.setModel(datasetOutlineModel);
      datasetOutline.setRenderDataProvider(new DataStoreLayerRenderData());
      // sets some columns sizes to maximize geom col
      setTableColWidth();
      datasetOutline.setDefaultEditor(String.class, new DataStoreLayerWhereEditor());

    }
    populateDatasetTree();
    return datasetOutline;
  }

  private void setTableColWidth() {
    if (datasetOutline != null) {
      datasetOutline.getColumnModel().getColumn(0).setPreferredWidth(195);
      datasetOutline.getColumnModel().getColumn(1).setPreferredWidth(227);
      datasetOutline.getColumnModel().getColumn(2).setPreferredWidth(72);
      datasetOutline.getColumnModel().getColumn(3).setPreferredWidth(40);
      datasetOutline.getColumnModel().getColumn(4).setPreferredWidth(40);
    }
  }

  public Object[] sortGeometryColumns(List<GeometryColumn> list) {
    Collections.sort(list, new Comparator<GeometryColumn>() {
      public int compare(GeometryColumn o1, GeometryColumn o2) {
        return o1.getName().compareTo(o2.getName());
      }

      public boolean equals(Object obj) {
        return this == obj;
      }
    });
    return list.toArray(new GeometryColumn[list.size()]);
  }

  private void populateDatasetTree() {
    if (getConnectionDescriptor() == null) {
      return;
    }
    try {
      //loads list of layers from the given connection and builds the datasetTreeModel object for these layers
      loadDatasetList(getConnectionDescriptor());
    } catch (ThreadDeath td) {
      //TODO:
      throw td;
    } catch (Exception e) {
      getContext().getErrorHandler().handleThrowable(e);
    } finally {
      //datasetTreeModel = new DataStoreLayerTreeModel(root);
      datasetOutlineModel = DefaultOutlineModel.createOutlineModel(
          datasetTreeModel, new DataStoreLayerRowModel(), true,
          I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Layer"));
      datasetOutline.setModel(datasetOutlineModel);
      setTableColWidth();
      datasetOutline.setDefaultEditor(String.class, new DataStoreLayerWhereEditor());
    }
  }

  /**
   * Returns List of nodes (schemas), each one containing the list of dataStore
   * Layers (geo layers) If given datasetNames is null, returns an empty list
   * build with one node displaying given message and no children
   *
   * @param datasetNames
   * @param connectionDescriptor
   * @return
   * @throws Exception
   */
  private LinkedHashMap<String, ArrayList<DataStoreLayer>> getTreeModelData(
      final String[] datasetNames,
      final ConnectionDescriptor connectionDescriptor,
      final String message) throws Exception {

    LinkedHashMap<String, ArrayList<DataStoreLayer>> ret = new LinkedHashMap<String, ArrayList<DataStoreLayer>>();
    if (datasetNames == null || datasetNames.length == 0) {
      // builds an empty list with message
      ret.put(message, new ArrayList<DataStoreLayer>());
      return ret;
    } else {
      // creates a DataStoreLayer for each given layer and organize them
      // by schema in the linkedList
      DataStoreMetadata md = new PasswordPrompter().getOpenConnection(
          connectionManager(), connectionDescriptor,
          AddDatastoreLayerPanel.this).getMetadata();
      // empty current list model
      for (String dsName : datasetNames) {
        for (GeometryColumn geo : md.getGeometryAttributes(dsName)) {
          DataStoreLayer layer = new DataStoreLayer(dsName, geo);
          ArrayList<DataStoreLayer> newEntry = new ArrayList<DataStoreLayer>();
          newEntry.add(layer);
          // ON Java 8:
//                    ArrayList<DataStoreLayer> list = ret.putIfAbsent(layer.getSchema(), newEntry);
          // On Java 6, 7
          ArrayList<DataStoreLayer> list = ret.get(layer.getSchema());
          if (list == null) {
            list = ret.put(layer.getSchema(), newEntry);
          } else {
            // this schema exists: add newEntry into existing list
            list.addAll(newEntry);
          }
        }
      }
      return ret;
    }
  }

  private void loadDatasetList(
      final ConnectionDescriptor connectionDescriptor) throws Exception {
    // [mmichaud 2013-03-23] remove this test to be sure that the list of
    // tables is always uptodate (hopefully, the query to the metadata
    // table is always fast)
    //if (!connectionDescriptorToDatasetNamesMap.containsKey(connectionDescriptor)) {
    // Prompt for a password outside the ThreadedBasePlugIn thread,
    // which is not the GUI thread. [Jon Aquino 2005-03-11]
    new PasswordPrompter().getOpenConnection(connectionManager(),
        connectionDescriptor, this);
            // Retrieve the dataset names using a ThreadedBasePlugIn, so
    // that the user can kill the thread if desired
    // [Jon Aquino 2005-03-11]
    final String msgDs = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Retrieving-list-of-datasets");
    final String msgGeoT = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.No-geo-table-found");
    String[] datasetNames = (String[]) runInKillableThread(
        msgDs, getContext(),
        new Block() {
          public Object yield() throws Exception {
            //Gets the list of datasets 
            String[] dsNames = new PasswordPrompter().getOpenConnection(connectionManager(), connectionDescriptor,AddDatastoreLayerPanel.this)
                .getMetadata().getDatasetNames();
            
            AddDatastoreLayerPanel.this.datasetTreeModel = new DataStoreLayerTreeModel(
                getTreeModelData(dsNames, getConnectionDescriptor(), msgGeoT));
            return dsNames;
          }
        });
    // Don't cache the dataset array if it is empty, as a problem
    // likely occurred. [Jon Aquino 2005-03-14]
    // // if task is cancelled, datasetNames can be null [Nicolas Ribot 2015-10-13]
    if (datasetNames != null && datasetNames.length != 0) {
      connectionDescriptorToDatasetNamesMap.put(connectionDescriptor,
          datasetNames);
    }
    //}
    //return (String[]) connectionDescriptorToDatasetNamesMap.get(connectionDescriptor);
    return;
  }

  private void initialize() {
    JScrollPane sp = new JScrollPane(getDatasetOutline());
    sp.setPreferredSize(new Dimension(MAIN_COLUMN_WIDTH, 400));

    addRow(I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Dataset"), sp, null, false);
  }

  public static interface Block {

    public Object yield() throws Exception;
  }

}
