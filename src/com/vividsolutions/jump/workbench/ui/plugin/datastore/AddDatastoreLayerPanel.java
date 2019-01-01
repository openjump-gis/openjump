package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Component;
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
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSMetadata;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

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
import org.openjump.core.ui.plugin.datastore.AddDataStoreLayerWizardPanel;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.AbstractAction;
import javax.swing.Action;

// TODO String s1 = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Prevents-unnecessary-queries-to-the-datastore");
//      String s2 = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.The-recommended-setting-is-to-leave-this-checked");
//      cachingCheckBox.setToolTipText("<html>" + s1 + "<br>" + s2 + "</html>");
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
    getConnectionComboBox().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
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
      final Component panel = this;
      datasetOutline.setRootVisible(false);
      //Assign the model to the Outline object:
      datasetOutline.setModel(datasetOutlineModel);
      datasetOutline.setRenderDataProvider(new DataStoreLayerRenderData());
      // sets some columns sizes to maximize geom col
      setTableColWidth();
      datasetOutline.setDefaultEditor(String.class, new DataStoreLayerWhereEditor());

      // Checks where clause for layer in a custom Action triggered by the
      // custom DataStoreTableCellListener
      Action action = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          DataStoreTableCellListener tcl = (DataStoreTableCellListener) e.getSource();
          //gets corresponding layer and checks it:
          Object o = datasetOutline.getValueAt(tcl.getRow(), 0);
          // stores only correct DataStoreLayer
          if (o instanceof DataStoreLayer) {
            String s = checkSelectedLayer((DataStoreLayer) o);
            if (!s.isEmpty()) {
              ErrorDialog.show(panel, 
                  I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.SQL-error"),
                  I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Invalid-layer-where-clause") 
                      + " " + ((DataStoreLayer) o).getFullName(), s);
            }
          }
        }
      };

      DataStoreTableCellListener tcl = new DataStoreTableCellListener(datasetOutline, action);

      // adds a custom selectionListener to the table, that builds the list of selected
      // layers.
      datasetOutline.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          // refreshes the list of selected DataStoreLayers if user is not manipulating 
          // the selection
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
                  // stores only correct DataStoreLayer
                  if (o instanceof DataStoreLayer) {
                    selectedLayers.add((DataStoreLayer) o);
                  }
                }
              }
            }
          }
          // notify the wizard panel to dis/enable buttons
          Component c = panel;
          while (c != null && (c = c.getParent()) != null) {
            if (c instanceof WizardPanel) {
              ((AddDataStoreLayerWizardPanel) c).selectionChanged();
              break;
            }
          }
        }
      }
      );
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

    // reset the list first, in case the request doesn't return any (eg. in case of error)
    LinkedHashMap<String, ArrayList<DataStoreLayer>> root = new LinkedHashMap<>();
    datasetTreeModel = new DataStoreLayerTreeModel(root);

    Throwable t = null;
    try {
      //loads list of layers from the given connection
      // and builds the datasetTreeModel object for these layers
      loadDatasetList(getConnectionDescriptor());
    } catch (ThreadDeath td) {
      t = td;
    } catch (Exception e) {
      t = e;
    } finally {
      if (t != null) {
        getContext().getErrorHandler().handleThrowable(t);
      }
      datasetOutlineModel = DefaultOutlineModel.createOutlineModel(
          datasetTreeModel, new DataStoreLayerRowModel(), true,
          I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Dataset"));
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
   * @param datasetNames array of dataset names
   * @param message message displayed if the node (database schema) has no child (no geo layer)
   * @return a HashMap mapping database schemas to a list of tables in this schema
   * @throws Exception
   */
  private LinkedHashMap<String, ArrayList<DataStoreLayer>> getTreeModelData(
      final String[] datasetNames,
      final String message) throws Exception {

    final ConnectionDescriptor connectionDescriptor = getConnectionDescriptor();

    LinkedHashMap<String, ArrayList<DataStoreLayer>> ret = new LinkedHashMap<>();
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

      // Nico Ribot, 2018-08-07: new mechanism in SpatialDatabasesDSMetadata;
      // DataStoreLayer list is retrieved when getDatasetNames is called
      // TODO: propagate to DataStoreMetadata interface
      if (md instanceof SpatialDatabasesDSMetadata && ((SpatialDatabasesDSMetadata)md).getDataStoreLayers() != null) {
        System.out.println("adding datastorelayer directly !");
        for (DataStoreLayer layer : ((SpatialDatabasesDSMetadata)md).getDataStoreLayers()) {
          ArrayList<DataStoreLayer> newEntry = new ArrayList<>();
          newEntry.add(layer);
          ArrayList<DataStoreLayer> list = ret.get(layer.getSchema());
          if (list == null) {
            ret.put(layer.getSchema(), newEntry);
          } else {
            // this schema exists: add newEntry into existing list
            list.addAll(newEntry);
          }
        }
      } else {
        // normal mechanims
        for (String dsName : datasetNames) {
          for (GeometryColumn geo : md.getGeometryAttributes(dsName)) {
            DataStoreLayer layer = new DataStoreLayer(dsName, geo);
            ArrayList<DataStoreLayer> newEntry = new ArrayList<>();
            newEntry.add(layer);
            // ON Java 8:
  //                    ArrayList<DataStoreLayer> list = ret.putIfAbsent(layer.getSchema(), newEntry);
            // On Java 6, 7
            ArrayList<DataStoreLayer> list = ret.get(layer.getSchema());
            if (list == null) {
              ret.put(layer.getSchema(), newEntry);
            } else {
              // this schema exists: add newEntry into existing list
              list.addAll(newEntry);
            }
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
            String[] dsNames = new PasswordPrompter()
                    .getOpenConnection(connectionManager(), connectionDescriptor, AddDatastoreLayerPanel.this)
                    .getMetadata()
                    .getDatasetNames();

            AddDatastoreLayerPanel.this.datasetTreeModel = new DataStoreLayerTreeModel(
                getTreeModelData(dsNames, msgGeoT));
            return dsNames;
          }
        });
    // Don't cache the dataset array if it is empty, as a problem
    // likely occurred. [Jon Aquino 2005-03-14]
    // if task is cancelled, datasetNames can be null [Nicolas Ribot 2015-10-13]
    if (datasetNames != null && datasetNames.length != 0) {
      connectionDescriptorToDatasetNamesMap.put(connectionDescriptor,
          datasetNames);
    }
  }

  private void initialize() {
    JScrollPane sp = new JScrollPane(getDatasetOutline());
    sp.setPreferredSize(new Dimension(MAIN_COLUMN_WIDTH, 400));

    addRow("Dataset", sp, null, false);
  }

  public interface Block {
    Object yield() throws Exception;
  }

  /**
   * Tests if current selected layers are valid by running the where clause with
   * a 0 limit. Returns errors for each layer in a String. Returns null if no
   * error is found.
   */
  private String checkSelectedLayer(DataStoreLayer layer) {
    final StringBuilder ret = new StringBuilder();
    final ConnectionDescriptor connectionDescriptor = getConnectionDescriptor();
    DataStoreConnection conn = null;

    try {
      conn = new PasswordPrompter().getOpenConnection(
          connectionManager(), connectionDescriptor, AddDatastoreLayerPanel.this);
      DataStoreMetadata md = conn.getMetadata();
      // Gets connection for this layers and check where clause
      SpatialReferenceSystemID srid = conn.getMetadata().getSRID(layer.getFullName(), layer.getGeoCol().getName());
      String[] colNames = conn.getMetadata().getColumnNames(layer.getFullName());
      SpatialDatabasesSQLBuilder builder = conn.getSqlBuilder(srid, colNames);
      String sql = builder.getCheckSQL(layer);
      try {
        JDBCUtil.execute(conn.getJdbcConnection(), sql, new ResultSetBlock() {
          public void yield(ResultSet resultSet) throws SQLException {
            // if query succeeds, nothing to do. 
            // Exception thrown in case of error: we manage it.
          }
        });
      } catch (Exception e) {
        ret.append(layer.getFullName()).append(": ").append(e.getMessage())
            .append("\nCause: ").append(e.getCause().getMessage());
      }
    } catch (Exception e) {
      ret.append(e.getMessage());
    }

    return ret.toString();
  }

}
