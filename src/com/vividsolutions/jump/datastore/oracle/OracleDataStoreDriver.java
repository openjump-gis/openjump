/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class OracleDataStoreDriver
    extends SpatialDatabasesDataStoreDriver {
    // TODO: uniformize
    public final static String JDBC_CLASS = "oracle.jdbc.driver.OracleDriver";
    public static final String GT_SDO_CLASS_NAME = "org.geotools.data.oracle.sdo.SDO";


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
        DataStoreConnection ret = super.createConnection(params);
        return new OracleDSConnection(ret.getConnection());
    }
}
