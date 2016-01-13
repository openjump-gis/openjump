package com.vividsolutions.jump.datastore.oracle;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class OracleDataStoreDriver
    extends AbstractSpatialDatabasesDSDriver {
    // TODO: uniformize
    public final static String JDBC_CLASS = "oracle.jdbc.driver.OracleDriver";
    public static final String GT_SDO_CLASS_NAME = "org.geotools.data.oracle.sdo.SDO";


    /**
     * Constructor
     * TODO: static variables for fields
     */
    public OracleDataStoreDriver() {
        this.driverName = "Oracle Spatial";
        this.jdbcClass = OracleDataStoreDriver.JDBC_CLASS;
        this.urlPrefix = "jdbc:oracle:thin:@//";
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
        return new OracleDSConnection(conn);
    }
}
