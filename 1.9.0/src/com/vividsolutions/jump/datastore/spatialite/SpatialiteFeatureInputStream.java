package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 *
 * @author nicolas
 */
public class SpatialiteFeatureInputStream extends SpatialDatabasesFeatureInputStream {

    /**
     * propagate the metadata object through Spatialite classes to get access to
     * specific information
     */
    private SpatialiteDSMetadata metadata;

    public SpatialiteFeatureInputStream(Connection conn, String queryString) {
        super(conn, queryString);
    }

    public SpatialiteFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    }

    public void setMetadata(SpatialiteDSMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns a SpatialiteResultSetConverter
     *
     * @param rs
     * @return
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
        SpatialiteResultSetConverter ret = new SpatialiteResultSetConverter(conn, rs);
        ret.setMetadata(this.metadata);
        
        return ret;
    }
}
