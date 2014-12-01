package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import org.openjump.core.ui.plugin.datastore.postgis2.PostGISDataStoreDataSource;

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
            boolean tableAlreadyCreated)  throws Exception {
        WritableDataStoreDataSource source;
        String driverName = connectionDescriptor.getDataStoreDriverClassName();
        if (driverName.equals("com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver")) {
            source = new PostGISDataStoreDataSource(
                    connectionDescriptor, datasetName, geometryAttributeName, externalPKName);
            source.setTableAlreadyCreated(tableAlreadyCreated);
        } else {
            throw new Exception(I18N.getMessage(KEY + ".no-writable-datastore-datasource", driverName));
        }
        /*
        source.setProperties(CollectionUtil.createMap(new Object[]{
                WritableDataStoreDataSource.DATASET_NAME_KEY, datasetName,
                WritableDataStoreDataSource.GEOMETRY_ATTRIBUTE_NAME_KEY, geometryAttributeName,
                WritableDataStoreDataSource.EXTERNAL_PK_KEY, externalPKName,
                WritableDataStoreDataSource.WHERE_CLAUSE_KEY, whereClause,
                WritableDataStoreDataSource.MAX_FEATURES_KEY, maxFeatures,
                WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, connectionDescriptor,
                WritableDataStoreDataSource.LIMITED_TO_VIEW, limitedToView,     // boolean
                WritableDataStoreDataSource.MANAGE_CONFLICTS, manageConflicts   // boolean
        }));
        source.getProperties().put(WritableDataStoreDataSource.CREATE_PK, false);
        source.getProperties().put(WritableDataStoreDataSource.CREATE_TABLE, false);
        source.setWorkbenchContext(context);
        */
        return source;
    }

}
