package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class PostgisDataStoreDriver
    extends SpatialDatabasesDataStoreDriver {

    public final static String JDBC_CLASS = "org.postgresql.Driver";

    public PostgisDataStoreDriver() {
        this.driverName = "PostGIS";
        this.jdbcClass = "org.postgresql.Driver";
        this.urlPrefix = "jdbc:postgresql://";
    }
}
