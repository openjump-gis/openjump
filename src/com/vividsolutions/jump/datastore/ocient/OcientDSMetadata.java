package com.vividsolutions.jump.datastore.ocient;

import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.feature.AttributeType;

import java.text.Normalizer;
import java.util.List;

public class OcientDSMetadata extends SpatialDatabasesDSMetadata {

    public OcientDSMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: use bind parameters to avoid SQL injection
	String geometryColumnsDef = "(SELECT \n" +
			"'' AS f_table_catalog, \n" +
			"CASE WHEN t.schema = 'sysgdc' THEN SUBSTRING(t.name, 0, LOCATE('_', t.name)) ELSE t.schema END AS f_table_schema, \n" +
			"CASE WHEN t.schema = 'sysgdc' THEN SUBSTRING(t.name, LOCATE('_', t.name) + 1) ELSE t.name END AS f_table_name, \n" +
			"c.name AS f_geometry_column, \n" + 
			"int(2) AS coord_dimension, \n" +
			"int(4326) AS srid, \n" +
			"CASE WHEN c.data_type = 'POINT(2)' THEN 'POINT' ELSE c.data_type END AS \"type\" \n" +
			"FROM sys.columns c \n" +
			"INNER JOIN sys.tables t \n" +
			"ON c.table_id = t.id \n" +
			"WHERE c.data_type IN ('POINT(2)', 'LINESTRING', 'POLYGON'))";

	datasetNameQuery = "SELECT DISTINCT f_table_schema, f_table_name FROM " + geometryColumnsDef;
        defaultSchemaName = "public";
        spatialDbName = "Ocient";
        // No safe way found to get layer's extent from database.
        // text aggregation trick (select st_envelope(Geomfromtext(concat(concat(\"geometrycollection(\",group_concat(astext(%s))),\")\")))) from %s.%s)
        // Use extent of first record only as an hint to layer location
	String spatialExtentQuery1 = "select st_polygon(concat('POLYGON((', concat(STRING_AGG(points, ', '), '))')))\n" + 
                                "FROM (\n" + 
                                "  SELECT UNNEST(char[](\n" + 
                                "    concat(minx, concat(' ', miny)),\n" + 
                                "    concat(maxx, concat(' ', miny)),\n" + 
                                "    concat(maxx, concat(' ', maxy)),\n" + 
                                "    concat(minx, concat(' ', maxy)),\n" +  
                                "    concat(minx, concat(' ', miny)))) as points\n" +  
                                "    FROM (\n" + 
                                "      SELECT\n" +
                                "        min(st_x(st_pointN(geom, 1))) AS minx,\n" +
                                "        min(st_y(st_pointN(geom, 1))) AS miny,\n" +
                                "        max(st_x(st_pointN(geom, 3))) AS maxx,\n" +
                                "        max(st_y(st_pointN(geom, 3))) AS maxy\n" +
                                "            FROM (\n" +
                                "              SELECT st_ExteriorRing(st_envelope(%s)) AS geom\n" +
                                "                  FROM %s.%s\n" +
                                "    )) AS t\n" +
                                ") as t2";		       
 
        // NO st_extent function defined yet => same query is defined.
        spatialExtentQuery2 = spatialExtentQuery1;
        geoColumnsQuery = "SELECT f_geometry_column, coord_dimension, srid, type FROM " + geometryColumnsDef + "  where f_table_schema='%s' and f_table_name = '%s'";
        sridQuery = "SELECT srid FROM " + geometryColumnsDef + "  where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
        coordDimQuery = "SELECT coord_dimension FROM " + geometryColumnsDef + "  where f_table_schema = '%s' and f_table_name = '%s' and f_geometry_column = '%s'";
        datasetInfoQuery = "SELECT f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type FROM " + geometryColumnsDef + "  order by 1, 2";

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
    
    protected String getDbTypeName(AttributeType type) {
        if (type == AttributeType.GEOMETRY)      return "char";
        else if (type == AttributeType.STRING)   return "char";
        else if (type == AttributeType.INTEGER)  return "integer";
        else if (type == AttributeType.LONG)     return "bigint";
        else if (type == AttributeType.DOUBLE)   return "double";
        else if (type == AttributeType.NUMERIC)  return "decimal";
        else if (type == AttributeType.DATE)     return "timestamp";
        else if (type == AttributeType.BOOLEAN)  return "boolean";
        else if (type == AttributeType.OBJECT)   return "binary";
        else return "char";
      }
}
