package com.vividsolutions.jump.datastore.mariadb;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.*;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class MariadbDataStoreDriver
    extends AbstractSpatialDatabasesDSDriver {

      // TODO: uniformize
    public final static String JDBC_CLASS = "org.mariadb.jdbc.Driver";

    public MariadbDataStoreDriver() {
        this.driverName = "MariaDB";
        this.jdbcClass = JDBC_CLASS;
        this.urlPrefix = "jdbc:mariadb://";
    }
    
    /**
     * returns the right type of DataStoreConnection
     * @param params list of parameters to connect to a MariadbDataStore
     * @return a Connection to a MariadbDataStore
     * @throws Exception if an exception occurs while building the connection
     */
    @Override
    public DataStoreConnection createConnection(ParameterList params)
        throws Exception {
        Connection conn = super.createJdbcConnection(params);
        return new MariadbDSConnection(conn);
    }

    /** {@inheritDoc} */
    @Override public Icon getConnectedIcon() {
        return IconLoader.icon("ok_mariadb.png");
    }

    /** {@inheritDoc} */
    @Override public Icon getDisconnectedIcon() {
        return IconLoader.icon("ko_mariadb.png");
    }
}
