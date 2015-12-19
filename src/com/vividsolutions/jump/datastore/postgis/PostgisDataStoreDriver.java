package com.vividsolutions.jump.datastore.postgis;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class PostgisDataStoreDriver
    extends AbstractSpatialDatabasesDataStoreDriver {

    public final static String JDBC_CLASS = "org.postgresql.Driver";

    public PostgisDataStoreDriver() {
        this.driverName = "PostGIS";
        this.jdbcClass = "org.postgresql.Driver";
        this.urlPrefix = "jdbc:postgresql://";
    }
    
    /**
     * returns the right type of DataStoreConnection
     * @param params
     * @return
     * @throws Exception 
     */
    @Override
    public DataStoreConnection createConnection(ParameterList params)
        throws Exception {
        Connection conn = super.createJdbcConnection(params);
        return new PostgisDSConnection(conn);
    }
}
