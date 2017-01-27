package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.GeometryColumn;
import java.util.List;

public class PostgisDSMetadata extends SpatialDatabasesDSMetadata {

    public PostgisDSMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: use bind parameters to avoid SQL injection
        datasetNameQuery = "SELECT DISTINCT f_table_schema, f_table_name FROM geometry_columns";
        defaultSchemaName = "public";
        spatialDbName = "PostGIS";
        spatialExtentQuery1 = "SELECT ST_AsBinary(ST_Estimated_Extent( '%s', '%s', '%s' ))";
        // Nicolas Ribot: add double quotes for identifiers
        spatialExtentQuery2 = "SELECT ST_AsBinary(ST_Envelope(ST_Extent(\"%s\"))) FROM \"%s\".\"%s\"";
        geoColumnsQuery = "SELECT f_geometry_column, srid, type FROM geometry_columns where f_table_schema='%s' and f_table_name = '%s'";
        sridQuery = "SELECT srid FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
    }

    @Override
    public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.spatialExtentQuery1, 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(schema), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(table), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(attributeName));
    }

    @Override
    public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery2, attributeName, schema, table);
    }

    @Override
    public String getGeoColumnsQuery(String datasetName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.geoColumnsQuery, 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(getSchemaName(datasetName)), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(getTableName(datasetName)));
    }

    @Override
    public String getSridQuery(String schemaName, String tableName, String colName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.sridQuery, 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(schemaName), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(tableName), 
            SpatialDatabasesSQLBuilder.escapeSingleQuote(colName));
    }
    
    @Override
    public List<GeometryColumn> getGeometryAttributes(String datasetName) {
        String sql = this.getGeoColumnsQuery(datasetName);
        return getGeometryAttributes(sql, datasetName);
    }

}
