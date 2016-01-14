package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.jdbc.BoundQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDataStoreMetadata;

/**
 * Metadata for a H2 Database
 */
public class H2DataStoreMetadata extends SpatialDataStoreMetadata {

    public H2DataStoreMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: use bind parameters to avoid SQL injection
        datasetNameQuery = "SELECT DISTINCT f_table_schema, f_table_name FROM geometry_columns";
        defaultSchemaName = "PUBLIC";
        spatialDbName = "H2";
        //spatialExtentQuery1 = "SELECT ST_AsBinary(ST_Estimated_Extent( '%s', '%s', '%s' ))";
        spatialExtentQuery1 = "SELECT ST_AsBinary(ST_Envelope(ST_Extent(\"%s\"))) FROM \"%s\".\"%s\"";
        geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_schema = ? and f_table_name = ?";
        sridQuery = "SELECT srid FROM geometry_columns where f_table_schema = ? and f_table_name = ? and f_geometry_column = ?";
    }
}
