package com.vividsolutions.jump.workbench.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.Query;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

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
                              final Collection connectionDescriptors) {
      this.context = context;
        for (Iterator i = connectionDescriptors.iterator(); i.hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            connectionDescriptorToConnectionMap.put(connectionDescriptor,
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
    }

    private Map connectionDescriptorToConnectionMap = new HashMap();

    public DataStoreConnection getOpenConnection(
            ConnectionDescriptor connectionDescriptor) throws Exception {
        if (getConnection(connectionDescriptor).isClosed()) {
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

        public boolean isClosed() throws DataStoreException {
            return true;
        }
    };

    /**
     * @return a connection, possibly closed, never null
     */
    public DataStoreConnection getConnection(
            ConnectionDescriptor connectionDescriptor) {
        if (!connectionDescriptorToConnectionMap
                .containsKey(connectionDescriptor)) {
            connectionDescriptorToConnectionMap.put(connectionDescriptor,
                    DUMMY_CONNECTION);
            fireConnectionDescriptorAdded(connectionDescriptor);
        }
        return (DataStoreConnection) connectionDescriptorToConnectionMap
                .get(connectionDescriptor);
    }

    public Collection getConnectionDescriptors() {
        return Collections
                .unmodifiableCollection(connectionDescriptorToConnectionMap
                        .keySet());
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
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener listener = (Listener) i.next();
            listener.connectionDescriptorAdded(connectionDescriptor);
        }
    }

    private void fireConnectionDescriptorRemoved(
            ConnectionDescriptor connectionDescriptor) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener listener = (Listener) i.next();
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
                    (Collection) PersistentBlackboardPlugIn.get(blackboard)
                            .get(
                                    ConnectionManager.class.getName()
                                            + " - CONNECTION DESCRIPTORS",
                                    new ArrayList())));
        }
        return (ConnectionManager) blackboard.get(INSTANCE_KEY);
    }

    private List listeners = new ArrayList();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void closeConnections() throws DataStoreException {
        for (Iterator i = getConnectionDescriptors().iterator(); i.hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            if (!getConnection(connectionDescriptor).isClosed()) {
                getConnection(connectionDescriptor).close();
            }
        }
    }

}