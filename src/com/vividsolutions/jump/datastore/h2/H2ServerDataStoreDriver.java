package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class H2ServerDataStoreDriver extends SpatialDatabasesDataStoreDriver {

    public final static String JDBC_CLASS = "org.h2.Driver";

    public H2ServerDataStoreDriver() {
        this.driverName = "H2GIS Server";
        this.jdbcClass = "org.h2.Driver";
        this.urlPrefix = "jdbc:h2:tcp://";
    }
}