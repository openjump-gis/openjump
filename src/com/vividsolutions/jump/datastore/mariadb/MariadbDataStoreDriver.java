package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class MariadbDataStoreDriver
    extends SpatialDatabasesDataStoreDriver {

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
        DataStoreConnection ret = super.createConnection(params);
        return new MariadbDSConnection(ret.getConnection());
    }
}
