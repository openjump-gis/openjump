package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.postgis.PostgisValueConverterFactory;
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
public class H2ResultSetConverter extends SpatialDatabasesResultSetConverter {

    public H2ResultSetConverter(Connection conn, ResultSet rs) {
        this.rs = rs;
        this.odm = new H2ValueConverterFactory(conn);
    }
}