package com.vividsolutions.jump.workbench.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.Query;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDSLayer;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.ApplicationExitHandler;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;

/**
 * Reuses existing connections where possible.
 */
public class ConnectionManager {

    public interface Listener {
        void connectionDescriptorAdded(ConnectionDescriptor connectionDescriptor);

        void connectionDescriptorRemoved(
                ConnectionDescriptor connectionDescriptor);
    }


    private WorkbenchContext context;

    /**
     * @param connectionDescriptors
     *            a collection that is kept up to date by the ConnectionManager
     */
    private ConnectionManager(WorkbenchContext context,
                              final Collection<ConnectionDescriptor> connectionDescriptors) {
      this.context = context;
        for (Object connectionDescriptor : connectionDescriptors) {
            if (connectionDescriptor == null) continue;
            connectionDescriptorToConnectionMap.put((ConnectionDescriptor) connectionDescriptor,
                    DUMMY_CONNECTION);
        }
        addListener(new Listener() {
            public void connectionDescriptorAdded(
                    ConnectionDescriptor connectionDescriptor) {
                updateConnectionDescriptors();
            }

            public void connectionDescriptorRemoved(
                    ConnectionDescriptor connectionDescriptor) {
                updateConnectionDescriptors();
            }

            private void updateConnectionDescriptors() {
                connectionDescriptors.clear();
                connectionDescriptors
                        .addAll(connectionDescriptorToConnectionMap.keySet());
            }
        });
        
        // close all connections on exit
        context.getWorkbench().getFrame()
            .addApplicationExitHandler(new ApplicationExitHandler() {
              public void exitApplication(JFrame mainFrame) {
                  closeConnections();
              }
            });
    }

    private Map<ConnectionDescriptor,DataStoreConnection> connectionDescriptorToConnectionMap =
            new HashMap<>();

    public DataStoreConnection getOpenConnection(ConnectionDescriptor connectionDescriptor) throws Exception {
      if (getConnection(connectionDescriptor).isClosed()) {
        // create a connection and put into cache
        connectionDescriptorToConnectionMap.put(connectionDescriptor,
                connectionDescriptor.createConnection(
                getDriver(connectionDescriptor.getDataStoreDriverClassName())));
      }
      return getConnection(connectionDescriptor);
    }

    public DataStoreDriver getDriver(String driverClassName) {
      DataStoreDriver driver = findDriverRegistryEntry(driverClassName);
      if (driver == null)
        throw new RuntimeException("Can't find DataStoreDriver: " + driverClassName);
      return driver;
    }

    private DataStoreDriver findDriverRegistryEntry(String driverClassName) {
      List drivers = context.getRegistry().getEntries(DataStoreDriver.REGISTRY_CLASSIFICATION);
      for (Iterator i = drivers.iterator(); i.hasNext(); ) {
        DataStoreDriver driver = (DataStoreDriver) i.next();
        if (driver.getClass().getName().equals(driverClassName))
          return driver;
      }
      return null;
    }


    private static final DataStoreConnection DUMMY_CONNECTION = new DataStoreConnection() {
        public DataStoreMetadata getMetadata() {
            throw new UnsupportedOperationException();
        }

        public FeatureInputStream execute(Query query) {
            throw new UnsupportedOperationException();
        }

        public void close() throws DataStoreException {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the native JDBC connection used by the manager.
         * (needed by DataStoreDataSource classes (Oracle, Postgis, ...)
         * @return the jdbc Connection
         */
        public java.sql.Connection getJdbcConnection() {
            throw new UnsupportedOperationException();
        }

        public boolean isClosed() throws DataStoreException {
            return true;
        }

        @Override
        public SpatialDatabasesSQLBuilder getSqlBuilder(
            SpatialReferenceSystemID srid, String[] colNames) {
          throw new UnsupportedOperationException();
        }
    };

    /**
     * @return a connection, possibly closed, never null
     */
    public DataStoreConnection getConnection(
            ConnectionDescriptor connectionDescriptor) {
        if (connectionDescriptor == null) return DUMMY_CONNECTION;
        if (!connectionDescriptorToConnectionMap
                .containsKey(connectionDescriptor)) {
            connectionDescriptorToConnectionMap.put(connectionDescriptor,
                    DUMMY_CONNECTION);
            fireConnectionDescriptorAdded(connectionDescriptor);
        }
        return connectionDescriptorToConnectionMap
                .get(connectionDescriptor);
    }

    public Collection<ConnectionDescriptor> getConnectionDescriptors() {
        return Collections.unmodifiableCollection(
                connectionDescriptorToConnectionMap.keySet()
        );
    }

    /**
     * Removes the ConnectionDescriptor and closes its associated
     * DataStoreConnection.
     */
    public void deleteConnectionDescriptor(
            ConnectionDescriptor connectionDescriptor)
            throws DataStoreException {
        if (!getConnection(connectionDescriptor).isClosed()) {
            getConnection(connectionDescriptor).close();
        }
        connectionDescriptorToConnectionMap.remove(connectionDescriptor);
        fireConnectionDescriptorRemoved(connectionDescriptor);
    }

    private void fireConnectionDescriptorAdded(
            ConnectionDescriptor connectionDescriptor) {
        for (Listener listener : listeners) {
            listener.connectionDescriptorAdded(connectionDescriptor);
        }
    }

    private void fireConnectionDescriptorRemoved(
            ConnectionDescriptor connectionDescriptor) {
        for (Listener listener : listeners) {
            listener.connectionDescriptorRemoved(connectionDescriptor);
        }
    }

    public static ConnectionManager instance(WorkbenchContext context) {
      Blackboard blackboard = context.getBlackboard();
        String INSTANCE_KEY = ConnectionManager.class.getName() + " - INSTANCE";
        if (blackboard.get(INSTANCE_KEY) == null) {
            // If the blackboard has an associated PersistentBlackboard,
            // that will be used to persist the DataStoreDrivers.
            // [Jon Aquino 2005-03-11]
            blackboard.put(INSTANCE_KEY, new ConnectionManager(
                context,
                    (Collection<ConnectionDescriptor>) PersistentBlackboardPlugIn.get(blackboard)
                            .get(
                                    ConnectionManager.class.getName()
                                            + " - CONNECTION DESCRIPTORS",
                                    new ArrayList<ConnectionDescriptor>())));
        }
        return (ConnectionManager) blackboard.get(INSTANCE_KEY);
    }

    private List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * run by Exit handler, close all 
     * @throws DataStoreException if a DataStoreException occurs during connections closing
     */
    public void closeConnections() {
      for (ConnectionDescriptor connectionDescriptor : getConnectionDescriptors()) {
        closeConnection(connectionDescriptor);
      }
    }

    /**
     * close all connections currently not used by any layer in any open task
     */
    public void closeConnectionsUnused() {
      for (ConnectionDescriptor connectionDescriptor : getConnectionDescriptors()) {
        if (getLayersUsing(connectionDescriptor).isEmpty()) {
          closeConnection(connectionDescriptor);
        }
      }
    }

    /**
     * used by {@link SpatialDSLayer#dispose()}
     * close the connection utilized by the layer given if no other layer
     * is currently needing it
     */
    public void closeConnection(SpatialDSLayer layer2remove) {
      ConnectionDescriptor cd2remove = (ConnectionDescriptor) layer2remove.getDataSourceQuery()
          .getDataSource().getProperties().get(DataStoreQueryDataSource.CONNECTION_DESCRIPTOR_KEY);
      
      // find out if same connection is used in another layer
      List<SpatialDSLayer> layers = getLayersUsing(cd2remove);
      for (SpatialDSLayer layer : layers) {
        ConnectionDescriptor layerCd = (ConnectionDescriptor) layer.getDataSourceQuery()
            .getDataSource().getProperties().get(DataStoreQueryDataSource.CONNECTION_DESCRIPTOR_KEY);
        if (layer != layer2remove) {
          // still needed? nothing to do and return
          return;
        }
      }
      
      closeConnection(cd2remove);
    }

    /**
     * try to close the connection for the given connection descriptor
     */
    public void closeConnection(ConnectionDescriptor cd2remove) {
      try {
        if (!getConnection(cd2remove).isClosed())
          getConnection(cd2remove).close();
      } catch (DataStoreException e) {
        // ignore but log it
        Logger.error(e);
      }
    }

    /**
     * gather a list of all spatial db datasource layers using the given
     * connection descriptor
     */
    private List<SpatialDSLayer> getLayersUsing(ConnectionDescriptor cd) {
      // iterate over al tasks collecting datastore layers
      List<SpatialDSLayer> layers = new ArrayList<SpatialDSLayer>();
      for (TaskFrame frame : context.getWorkbench().getFrame().getTaskFrames()) {
        layers.addAll(frame.getLayerManager().getLayerables(SpatialDSLayer.class));
      }

      // check if anyone is using the connection descriptor given
      List<SpatialDSLayer> layersUsingThisCD = new ArrayList<SpatialDSLayer>();
      for (SpatialDSLayer layer : layers) {
        try {
          ConnectionDescriptor layerCd = (ConnectionDescriptor) layer.getDataSourceQuery()
            .getDataSource().getProperties().get(DataStoreQueryDataSource.CONNECTION_DESCRIPTOR_KEY);
          if (cd == layerCd) {
            layersUsingThisCD.add(layer);
          }
        } catch (NullPointerException e) {
          // no datasource or whatever, no usage :), log it though just in case
          Logger.error(e);
        }
      }
      return layersUsingThisCD;
    }
}