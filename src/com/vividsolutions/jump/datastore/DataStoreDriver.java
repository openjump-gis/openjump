package com.vividsolutions.jump.datastore;

import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;

/**
 * A driver for a given type of datastore
 */
public interface DataStoreDriver {
    public static final Object REGISTRY_CLASSIFICATION = DataStoreDriver.class
            .getName();

    String getName();

    ParameterListSchema getParameterListSchema();

    DataStoreConnection createConnection(ParameterList params) throws Exception;

    /**
     * @return a description of the driver
     */
    public String toString();
    
    boolean isAdHocQuerySupported();    
}