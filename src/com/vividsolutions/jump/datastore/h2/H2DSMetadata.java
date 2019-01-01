package com.vividsolutions.jump.datastore.h2;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSMetadata;

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
        geoColumnsQuery = "SELECT f_geometry_column, coord_dimension, srid, type FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s'";
        sridQuery = "SELECT srid FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
        coordDimQuery = "SELECT coord_dimension FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
        datasetInfoQuery = "SELECT f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type FROM geometry_columns";
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
                SQLUtil.escapeSingleQuote(getSchemaName(datasetName)),
                SQLUtil.escapeSingleQuote(getTableName(datasetName)));
    }

    @Override
    public String getSridQuery(String schemaName, String tableName, String colName) {
        // escape single quotes
        return String.format(this.sridQuery,
                SQLUtil.escapeSingleQuote(schemaName),
                SQLUtil.escapeSingleQuote(tableName), colName);
    }

    @Override
    public List<GeometryColumn> getGeometryAttributes(String datasetName) {
        String sql = this.getGeoColumnsQuery(datasetName);
        return getGeometryAttributes(sql, datasetName);
    }

    @Override
    public String getAddGeometryColumnStatement(String schemaName, String tableName,
                                                String geometryColumn, int srid, String geometryType, int dim) {
        String schematable = SQLUtil.compose(schemaName, tableName);
        return "ALTER TABLE " + schematable + " ADD COLUMN \"" + geometryColumn + "\" GEOMETRY;";
    }

    @Override
    public String getAddSpatialIndexStatement(String schemaName, String tableName, String geometryColumn) {
        // Geometry index creation is different on different spatial databases
        // Do not add if it is not defined
        String schematable = SQLUtil.compose(schemaName, tableName);
        String indexname = tableName + "_" + geometryColumn + "_idx";
        return "CREATE SPATIAL INDEX \"" + indexname + "\" ON " + schematable + "(\"" + geometryColumn + "\");";
    }
}
