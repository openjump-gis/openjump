package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.feature.AttributeType;

import java.text.Normalizer;
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
        geoColumnsQuery = "SELECT f_geometry_column, coord_dimension, srid, type FROM geometry_columns where f_table_schema='%s' and f_table_name = '%s'";
        sridQuery = "SELECT srid FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
        coordDimQuery = "SELECT coord_dimension FROM geometry_columns where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
        datasetInfoQuery = "SELECT f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type FROM geometry_columns order by 1, 2";

    }

    @Override
    public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.spatialExtentQuery1, 
                SQLUtil.escapeSingleQuote(schema),
                SQLUtil.escapeSingleQuote(table),
                SQLUtil.escapeSingleQuote(attributeName));
    }

    @Override
    public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery2, attributeName, schema, table);
    }

    @Override
    public String getGeoColumnsQuery(String datasetName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.geoColumnsQuery,
                SQLUtil.escapeSingleQuote(getSchemaName(datasetName)),
                SQLUtil.escapeSingleQuote(getTableName(datasetName)));
    }

    @Override
    public String getSridQuery(String schemaName, String tableName, String colName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.sridQuery,
                SQLUtil.escapeSingleQuote(schemaName),
                SQLUtil.escapeSingleQuote(tableName),
                SQLUtil.escapeSingleQuote(colName));
    }
    
    @Override
    public List<GeometryColumn> getGeometryAttributes(String datasetName) {
        String sql = this.getGeoColumnsQuery(datasetName);
        return getGeometryAttributes(sql, datasetName);
    }

    @Override
    public String getCoordinateDimensionQuery(String schemaName, String tableName, String colName) {
        //must escape single quote in idenfifiers before formatting query
        return String.format(this.coordDimQuery,
                SQLUtil.escapeSingleQuote(schemaName),
                SQLUtil.escapeSingleQuote(tableName),
                SQLUtil.escapeSingleQuote(colName));
    }

    @Override
    // Return postgresql/postgis data types
    protected String getDbTypeName(AttributeType type) {
        if (type == AttributeType.GEOMETRY)     return "geometry";
        else if (type == AttributeType.STRING)  return "varchar";
        else if (type == AttributeType.INTEGER) return "int4";
        else if (type == AttributeType.LONG)    return "int8";
        else if (type == AttributeType.DOUBLE)  return "float8";
        else if (type == AttributeType.DATE)    return "timestamp";
        else if (type == AttributeType.BOOLEAN) return "bool";
        else if (type == AttributeType.OBJECT)  return "bytea";
        else return "bytea";
    }

    /**
     * Create statement to add a spatial index on the specified geometry column.
     * The geometry column name must have its final form. Attribute name normalization
     * is the responsability of the calling method.
     * @param schemaName unquoted schema name or null if default schema is used
     * @param tableName unquoted table name
     * @param geometryColumn unquoted geometry column name
     * @return a sql string to add a spatial index
     */
    @Override
    public String getAddSpatialIndexStatement(String schemaName, String tableName, String geometryColumn) {
        String name = schemaName == null ? "" : schemaName + "_";
        name = name + tableName + "_" + geometryColumn + "_idx";
        name = Normalizer.normalize(name, Normalizer.Form.NFD); // separe base character from accent
        name = name.replaceAll("\\p{M}", ""); // remove accents
        name = name.toLowerCase();
        name = name.replaceAll("[^\\x5F\\x30-\\x39\\x41-\\x5A\\x61-\\x7A]", "_");
        name = name.replaceAll("_+", "_");
        return "CREATE INDEX " + name + " ON " +
                SQLUtil.compose(schemaName, tableName) + " USING GIST ( \"" + geometryColumn + "\" );";
    }

}
