package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 *
 * @author nicolas Ribot
 */
public class PostgisFeatureInputStream extends SpatialDatabasesFeatureInputStream {

    public PostgisFeatureInputStream(Connection conn, String queryString) {
        this(conn, queryString, null);
    }

    public PostgisFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    }
    
    /**
     * Returns a PostgisResultSetConverter
     * @param rs a ResultSet
     * @return a SpatialDatabasesResultSetConverter containing converters
     *      to convert data get from a PostgisDatabase
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
        return new PostgisResultSetConverter(conn, rs);
    }

}
