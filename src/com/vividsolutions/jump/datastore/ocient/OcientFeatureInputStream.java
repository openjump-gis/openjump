package com.vividsolutions.jump.datastore.ocient;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import java.sql.Connection;
import java.sql.ResultSet;

public class OcientFeatureInputStream extends SpatialDatabasesFeatureInputStream {

    public OcientFeatureInputStream(Connection conn, String queryString) {
        this(conn, queryString, null);
    }

    public OcientFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    }
    
    /**
     * Returns a OcientResultSetConverter
     * @param rs
     * @return 
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
        return new OcientResultSetConverter(conn, rs);
    }

}
