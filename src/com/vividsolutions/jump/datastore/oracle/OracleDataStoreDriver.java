package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class OracleDataStoreDriver
    extends SpatialDatabasesDataStoreDriver {
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
}
