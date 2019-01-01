package com.vividsolutions.jump.datastore.postgis;

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
        // Adds custom PG JDBC driver property: ApplicationName: useful to monitor applications connected to a PG instance
        // cf: https://jdbc.postgresql.org/documentation/head/connect.html
        Properties connectionProps = new Properties();
        connectionProps.put(
            "ApplicationName", 
            I18N.get("JUMPWorkbench.jump") + " " + JUMPVersion.CURRENT_VERSION);
        Connection conn = super.createJdbcConnection(params, connectionProps);
        return new PostgisDSConnection(conn);
    }
}
