/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 *
 * @author nicolas
 */
public class PostgisFeatureInputStream extends SpatialDatabasesFeatureInputStream {
    public PostgisFeatureInputStream(Connection conn, String queryString) {
        super(conn, queryString);
    }

    public PostgisFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    }
    
    /**
     * Returns a PostgisResultSetConverter
     * @param rs
     * @return 
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
      return new PostgisResultSetConverter(conn, rs);
    }
}
