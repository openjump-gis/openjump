package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.GeometryColumn;
import java.util.List;

public class MariadbDSMetadata extends SpatialDatabasesDSMetadata {

    public MariadbDSMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: defaults to database name ?
        defaultSchemaName = "";
        // TODO: use bind parameters to avoid SQL injection
        datasetNameQuery = "select distinct t.TABLE_SCHEMA, t.TABLE_NAME \n" +
            "from information_schema.TABLES t join information_schema.COLUMNS C \n" +
            "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n" +
            "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n" +
            "and c.COLUMN_TYPE = 'geometry';";
        spatialDbName = "MariaDB/MySQL";
        spatialExtentQuery1 = "select st_asBinary(st_envelope(Geomfromtext(concat(concat(\"geometrycollection(\",group_concat(astext(%s))),\")\")))) from %s.%s;";
        // NO metadata => same query is defined.
        spatialExtentQuery2 = spatialExtentQuery1;
        geoColumnsQuery = "select c.COLUMN_NAME, 0, 'geometry' \n" +
            "from information_schema.TABLES t join information_schema.COLUMNS C \n" +
            "  on t.TABLE_NAME = c.TABLE_NAME and t.TABLE_SCHEMA = c.TABLE_SCHEMA\n" +
            "where t.TABLE_TYPE not in ('SYSTEM VIEW')\n" +
            "and t.TABLE_SCHEMA = '%s' and t.TABLE_NAME = '%s'\n" +
            "and c.COLUMN_TYPE = 'geometry'";
        sridQuery = "select case when min(st_srid(%s)) <> max(st_srid(%s)) then 0 else min(st_srid(%s)) end as srid\n" +
                "from %s.%s";
    }

    @Override
    public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery1, attributeName, schema, table);
    }

    @Override
    public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery2, attributeName, schema, table);
    }

    @Override
    public String getGeoColumnsQuery(String datasetName) {
        return String.format(this.geoColumnsQuery, getSchemaName(datasetName), getTableName(datasetName));
    }

    @Override
    public String getSridQuery(String schemaName, String tableName, String colName) {
        // TODO
        return String.format(this.sridQuery, colName, colName, colName, schemaName, tableName);
    }
    
    @Override
    public List<GeometryColumn> getGeometryAttributes(String datasetName) {
        String sql = this.getGeoColumnsQuery(datasetName);
        // TODO: manage srid by executing 2 SQL queries: one for geo cols, one for
        // srids.
        return getGeometryAttributes(sql, datasetName);
    }

}
