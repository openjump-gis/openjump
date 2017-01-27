package com.vividsolutions.jump.datastore.mariadb;

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
public class MariadbResultSetConverter extends SpatialDatabasesResultSetConverter {

    public MariadbResultSetConverter(Connection conn, ResultSet rs) {
        super(conn, rs);
        this.odm = new MariadbValueConverterFactory(conn);
    }
}
