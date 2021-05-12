package com.vividsolutions.jump.datastore;

import java.sql.Driver;

import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.*;

/**
 * A driver for a given type of datastore
 */
public interface DataStoreDriver {

    String REGISTRY_CLASSIFICATION = DataStoreDriver.class.getName();

    String getName();

    String getVersion();

    Driver getJdbcDriver();

    String getJdbcDriverVersion();

    ParameterListSchema getParameterListSchema();

    DataStoreConnection createConnection(ParameterList params) throws Exception;

    /**
     * Default icon for this driver after a connection to the datastore has been set.
     * Subclass should overload this method to offer a database specific icon.
     */
    default Icon getConnectedIcon() {
        return IconLoader.icon("connect.png");
    }

    /**
     * Default icon for this driver when there is no active connection.
     * Subclass should overload this method to offer a database specific icon.
     */
    default Icon getDisconnectedIcon() {
        return IconLoader.icon("disconnect.png");
    }

    /**
     * @return a description of the driver
     */
    String toString();
    
    boolean isAdHocQuerySupported();    
}