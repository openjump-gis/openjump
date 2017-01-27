package com.vividsolutions.jump.datastore.postgis;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class PostgisDataStoreDriver
    extends AbstractSpatialDatabasesDSDriver {

    public final static String JDBC_CLASS = "org.postgresql.Driver";

    public PostgisDataStoreDriver() {
        this.driverName = "PostGIS";
        this.jdbcClass = JDBC_CLASS;
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
