/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class PostgisDataStoreDriver
    extends SpatialDatabasesDataStoreDriver {

    public PostgisDataStoreDriver() {
        this.driverName = "PostGIS";
        this.jdbcClass = "org.postgresql.Driver";
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
        DataStoreConnection ret = super.createConnection(params);
        return new PostgisDSConnection(ret.getConnection());
    }
}
