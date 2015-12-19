package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;

/**
 * Created by UMichael on 18/12/2015.
 */
public class H2ServerDataStoreDriver extends SpatialDatabasesDataStoreDriver {

    public final static String JDBC_CLASS = "org.h2.Driver";

    public H2ServerDataStoreDriver() {
        this.driverName = "H2GIS Server";
        this.jdbcClass = "org.h2.Driver";
        this.urlPrefix = "jdbc:h2:tcp://";
    }
}