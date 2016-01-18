package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSMetadata;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;

import java.util.List;

/**
 * Metadata for a H2 Database
 */
public class H2DSMetadata extends SpatialDatabasesDSMetadata {

    public H2DSMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: use bind parameters to avoid SQL injection
        datasetNameQuery = "SELECT DISTINCT f_table_schema, f_table_name FROM geometry_columns";
        defaultSchemaName = "PUBLIC";
        spatialDbName = "H2";
        //spatialExtentQuery1 = "SELECT ST_AsBinary(ST_Estimated_Extent( '%s', '%s', '%s' ))";
        spatialExtentQuery1 = "SELECT ST_AsBinary(ST_Envelope(ST_Extent(%s))) FROM \"%s\".\"%s\"";
        geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s'";
        sridQuery = "SELECT srid FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
    }

    @Override
    public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery1, schema, table, attributeName);
    }

    @Override
    public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery1, attributeName, schema, table);
    }

    @Override
    public String getGeoColumnsQuery(String datasetName) {
        // escape single quotes
        return String.format(this.geoColumnsQuery, 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(getSchemaName(datasetName)), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(getTableName(datasetName)));
    }

    @Override
    public String getSridQuery(String schemaName, String tableName, String colName) {
        // escape single quotes
        return String.format(this.sridQuery, 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(schemaName), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(tableName), colName);
    }

    @Override
    public List<GeometryColumn> getGeometryAttributes(String datasetName) {
        String sql = this.getGeoColumnsQuery(datasetName);
        return getGeometryAttributes(sql, datasetName);
    }
}
