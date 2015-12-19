package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class H2DataStoreDriver extends SpatialDatabasesDataStoreDriver {

        public final static String JDBC_CLASS = "org.h2.Driver";

        public H2DataStoreDriver() {
        this.driverName = "H2GIS";
        this.jdbcClass = "org.h2.Driver";
        this.urlPrefix = "jdbc:h2:";
    }
}
