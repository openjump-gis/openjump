package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 *
 * @author nicolas
 */
public class OracleFeatureInputStream extends SpatialDatabasesFeatureInputStream {
    public OracleFeatureInputStream(Connection conn, String queryString) {
        super(conn, queryString);
    }

    public OracleFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    }
    
    /**
     * Returns a PostgisResultSetConverter
     * @param rs
     * @return 
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
      return new OracleResultSetConverter(conn, rs);
    }
}
