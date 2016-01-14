package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.DataStoreConnection;

public class PostgisDataStoreMetadata extends SpatialDataStoreMetadata {

    public PostgisDataStoreMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: use bind parameters to avoid SQL injection
        datasetNameQuery = "SELECT DISTINCT f_table_schema, f_table_name FROM geometry_columns";
        defaultSchemaName = "public";
        spatialDbName = "PostGIS";
        spatialExtentQuery1 = "SELECT ST_AsBinary(ST_Estimated_Extent( ?, ?, ? ))";
        // Nicolas Ribot: add double quotes for identifiers
        spatialExtentQuery2 = "SELECT ST_AsBinary(ST_Envelope(ST_Extent(\"%s\"))) FROM \"%s\".\"%s\"";
        geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_schema=? and f_table_name = ?";
        sridQuery = "SELECT srid FROM geometry_columns where f_table_schema = ? and f_table_name = ? and f_geometry_column = ?";
    }
}
