package com.vividsolutions.jump.datastore.mariadb;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class MariadbDataStoreDriver
    extends AbstractSpatialDataStoreDriver {

      // TODO: uniformize
    public final static String JDBC_CLASS = "org.mariadb.jdbc.Driver";

    public MariadbDataStoreDriver() {
        this.driverName = "MariaDB";
        this.jdbcClass = JDBC_CLASS;
        this.urlPrefix = "jdbc:mariadb://";
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
        return new MariadbDataStoreConnection(conn);
    }
}
