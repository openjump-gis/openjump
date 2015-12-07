/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.oracle;

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
public class OracleResultSetConverter extends SpatialDatabasesResultSetConverter {

    public OracleResultSetConverter(Connection conn, ResultSet rs) {
        super(conn, rs);
        this.odm = new OracleValueConverterFactory(conn);
    }
}
