package com.vividsolutions.jump.datastore.ocient;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import java.util.Properties;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class OcientDataStoreDriver
    extends AbstractSpatialDatabasesDSDriver {

    public final static String JDBC_CLASS = "com.ocient.jdbc.JDBCDriver";

    public OcientDataStoreDriver() {
        this.driverName = "Ocient";
        this.jdbcClass = JDBC_CLASS;
        this.urlPrefix = "jdbc:ocient://";
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
        Properties connectionProps = new Properties();
        Connection conn = super.createJdbcConnection(params, connectionProps);
        return new OcientDSConnection(conn);
    }
}
