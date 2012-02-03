package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.cache.CachingFeatureCollection;
import com.vividsolutions.jump.workbench.model.cache.DynamicFeatureCollection;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;

/**
 * Adapts the DataStore API to the DataSource API.
 */
public class DataStoreDataSource extends DataSource implements
        WorkbenchContextReference {

    public static final String DATASET_NAME_KEY = "Dataset Name";

    public static final String GEOMETRY_ATTRIBUTE_NAME_KEY = "Geometry Attribute Name";
    public static final String MAX_FEATURES_KEY = "Max Features";
    public static final String WHERE_CLAUSE_KEY = "Where Clause";

    public static final String CACHING_KEY = "Caching";

    public static final String CONNECTION_DESCRIPTOR_KEY = "Connection Descriptor";

    private WorkbenchContext context;

    public DataStoreDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }
    
    public DataStoreDataSource(String datasetName,
                               String geometryAttributeName, 
                               String whereClause,
                               ConnectionDescriptor connectionDescriptor, 
                               boolean caching,
                               WorkbenchContext context) {
        setProperties(CollectionUtil.createMap(new Object[] {
            DATASET_NAME_KEY, datasetName,
            GEOMETRY_ATTRIBUTE_NAME_KEY, geometryAttributeName,
            WHERE_CLAUSE_KEY, whereClause,
            CONNECTION_DESCRIPTOR_KEY, connectionDescriptor, 
            CACHING_KEY, Boolean.valueOf(caching) }));
        setWorkbenchContext(context);
    }

    public DataStoreDataSource(String datasetName,
                               String geometryAttributeName, 
                               String whereClause,
                               int maxFeatures,
                               ConnectionDescriptor connectionDescriptor, 
                               boolean caching,
                               WorkbenchContext context) {
        setProperties(CollectionUtil.createMap(new Object[] {
            DATASET_NAME_KEY, datasetName,
            GEOMETRY_ATTRIBUTE_NAME_KEY, geometryAttributeName,
            WHERE_CLAUSE_KEY, whereClause,
            MAX_FEATURES_KEY, maxFeatures,
            CONNECTION_DESCRIPTOR_KEY, connectionDescriptor, 
            CACHING_KEY, Boolean.valueOf(caching) }));
        setWorkbenchContext(context);
    }

    public boolean isWritable() {
        return false;
    }

    public Connection getConnection() {
        return new Connection() {
            public FeatureCollection executeQuery(String query,
                    Collection exceptions, TaskMonitor monitor) {
                try {
                    return createFeatureCollection();
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                }
            }

            public FeatureCollection executeQuery(String query,
                    TaskMonitor monitor) throws Exception {
                Collection exceptions = new ArrayList();
                FeatureCollection featureCollection = executeQuery(query,
                        exceptions, monitor);
                if (!exceptions.isEmpty()) {
                    throw (Exception) exceptions.iterator().next();
                }
                return featureCollection;
            }

            public void executeUpdate(String query,
                    FeatureCollection featureCollection, TaskMonitor monitor)
                    throws Exception {
                throw new UnsupportedOperationException();
            }

            public void close() {
                // Do nothing, because DataStore connections are always
                // open (managed by the ConnectionManager). [Jon Aquino
                // 2005-03-16]
            }
        };
    }

    private FeatureCollection createFeatureCollection() {
        FilterQuery query = new FilterQuery();
        query.setDatasetName((String)getProperties().get(DATASET_NAME_KEY));
        query.setGeometryAttributeName((String)getProperties().get(
                GEOMETRY_ATTRIBUTE_NAME_KEY));
        if (((String)getProperties().get(WHERE_CLAUSE_KEY)).length() > 0) {
            query.setCondition((String) getProperties().get(WHERE_CLAUSE_KEY));
        }
        if (getProperties().get(MAX_FEATURES_KEY) != null) {
            query.setLimit((Integer)getProperties().get(MAX_FEATURES_KEY));
        }
        return new CachingFeatureCollection(new DynamicFeatureCollection(
                (ConnectionDescriptor) getProperties().get(
                        CONNECTION_DESCRIPTOR_KEY), ConnectionManager
                        .instance(context), query))
                .setCachingByEnvelope(((Boolean) LangUtil.ifNull(
                        getProperties().get(CACHING_KEY), Boolean.TRUE))
                        .booleanValue());
    }

    public void setWorkbenchContext(WorkbenchContext context) {
        this.context = context;
        try {
            // This method is called by OpenProjectPlugIn in the
            // GUI thread, so now is a good time to prompt for
            // a password if necessary. [Jon Aquino 2005-03-16]
            new PasswordPrompter().getOpenConnection(ConnectionManager
                    .instance(context),
                    (ConnectionDescriptor) getProperties().get(
                            CONNECTION_DESCRIPTOR_KEY), context.getWorkbench()
                            .getFrame());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}