package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import org.openjump.core.ui.plugin.datastore.h2.H2DataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.postgis2.PostGISDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager;

/**
 * Factory to create WritableDataStoreDataSource fitting
 * a specific database connection.
 */
public class DataStoreDataSourceFactory {

    private static final String KEY = DataStoreDataSourceFactory.class.getName();

    static public WritableDataStoreDataSource createWritableDataStoreDataSource (
            ConnectionDescriptor connectionDescriptor,
            String datasetName,
            String geometryAttributeName,
            String externalPKName,
            boolean tableAlreadyCreated,
            String txManagerClass,
            WorkbenchContext context)  throws Exception {
        WritableDataStoreDataSource source;
        String driverName = connectionDescriptor.getDataStoreDriverClassName();
        if (driverName.equals(com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver.class.getName())) {
            source = new PostGISDataStoreDataSource(
                    connectionDescriptor, datasetName, geometryAttributeName, externalPKName,
                    txManagerClass,
                    context);
            source.setTableAlreadyCreated(tableAlreadyCreated);
        } else if (driverName.equals(com.vividsolutions.jump.datastore.h2.H2DataStoreDriver.class.getName())) {
            source = new H2DataStoreDataSource(
                    connectionDescriptor, datasetName, geometryAttributeName, externalPKName,
                    txManagerClass,
                    context);
            source.setTableAlreadyCreated(tableAlreadyCreated);
        } else {
            throw new Exception(I18N.getMessage(KEY + ".no-writable-datastore-datasource", driverName));
        }
        return source;
    }

}
