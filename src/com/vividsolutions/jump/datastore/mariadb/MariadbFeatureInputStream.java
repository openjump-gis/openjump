package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 *
 * @author nicolas Ribot
 */
public class MariadbFeatureInputStream extends SpatialDatabasesFeatureInputStream {
    public MariadbFeatureInputStream(Connection conn, String queryString) {
        super(conn, queryString);
    }

    public MariadbFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    }
    
    /**
     * Returns a MariadbResultSetConverter
     * @param rs
     * @return 
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
      return new MariadbResultSetConverter(conn, rs);
    }
}
