package com.vividsolutions.jump.datastore.mariadb;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class MariadbDataStoreDriver
    extends AbstractSpatialDatabasesDataStoreDriver {

      // TODO: uniformize
    public final static String JDBC_CLASS = "com.mysql.jdbc.Driver";

    public MariadbDataStoreDriver() {
        this.driverName = "MariaDB/MySQL";
        this.jdbcClass = "com.mysql.jdbc.Driver";
        this.urlPrefix = "jdbc:mysql://";
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
        return new MariadbDSConnection(conn);
    }
}
