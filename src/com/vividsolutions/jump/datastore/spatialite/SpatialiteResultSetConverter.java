package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Implements the mapping between a result set and a {@link FeatureSchema} and
 * {@link Feature} set.
 *
 * This is a transient worker class, whose lifetime should be no longer than the
 * lifetime of the provided ResultSet
 */
public class SpatialiteResultSetConverter extends SpatialDatabasesResultSetConverter {
    /**
     * propagate the metadata object through Spatialite classes to get access to
     * specific information
     */
    private SpatialiteDSMetadata metadata;

    public SpatialiteResultSetConverter(Connection conn, ResultSet rs) {
        super(conn, rs);
        this.odm = new SpatialiteValueConverterFactory(conn);
    }

    public void setMetadata(SpatialiteDSMetadata metadata) {
        this.metadata = metadata;
        //hack: todo: clean inheritance ?
        if (this.odm != null) {
            ((SpatialiteValueConverterFactory)this.odm).setMetadata(this.metadata);
        }
    }
    
    
}
