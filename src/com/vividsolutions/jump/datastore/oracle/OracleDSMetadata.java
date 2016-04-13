/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.GeometryColumn;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OracleDSMetadata extends SpatialDatabasesDSMetadata {

    public OracleDSMetadata(DataStoreConnection con) {
        conn = con;
        // TODO: use bind parameters to avoid SQL injection
        try {
            this.defaultSchemaName = conn.getJdbcConnection().getMetaData().getUserName();
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            defaultSchemaName = "";
        }
        datasetNameQuery = "SELECT distinct asgm.OWNER, asgm.TABLE_NAME FROM ALL_SDO_GEOM_METADATA asgm";
        spatialDbName = "Oracle Spatial";
        spatialExtentQuery1 = "with tmp as (\n" +
            "  SELECT dim.*\n" +
            "  FROM ALL_SDO_GEOM_METADATA asgm, TABLE (asgm.diminfo) dim\n" +
            "  WHERE owner = '%s' and table_name = '%s' AND COLUMN_NAME='%s'\n" +
            ") select sdo_util.to_wktgeometry(SDO_GEOMETRY(\n" +
            "    2003,\n" +
            "    NULL,\n" +
            "    NULL,\n" +
            "    SDO_ELEM_INFO_ARRAY(1,1003,1),\n" +
            "    SDO_ORDINATE_ARRAY((select sdo_lb from tmp where sdo_dimname = 'X'),\n" +
            "                       (select sdo_lb from tmp where sdo_dimname = 'Y'), \n" +
            "                       (select sdo_ub from tmp where sdo_dimname = 'X'),\n" +
            "                       (select sdo_lb from tmp where sdo_dimname = 'Y'),\n" +
            "                       (select sdo_ub from tmp where sdo_dimname = 'X'),\n" +
            "                       (select sdo_ub from tmp where sdo_dimname = 'Y'),\n" +
            "                       (select sdo_ub from tmp where sdo_dimname = 'X'),\n" +
            "                       (select sdo_lb from tmp where sdo_dimname = 'Y'),\n" +
            "                       (select sdo_lb from tmp where sdo_dimname = 'X'),\n" +
            "                       (select sdo_lb from tmp where sdo_dimname = 'Y'))\n" +
            "  )) as geom \n" +
            "from dual";
        // double quotes identifiers
        spatialExtentQuery2 = "select sdo_util.to_wktgeometry(sdo_aggr_mbr(%s)) as geom from \"%s\".\"%s\"";
        
        geoColumnsQuery = "select t.column_name, t.diminfo, t.srid, 'SDO_GEOMETRY' as type from ALL_SDO_GEOM_METADATA t "
            + "where t.owner = '%s' and t.table_name = '%s'";

        sridQuery = "select t.srid from ALL_SDO_GEOM_METADATA t "
            + "where t.owner = '%s' and t.table_name = '%s' and t.COLUMN_NAME = '%s'";

        coordDimQuery = "select t.diminfo from ALL_SDO_GEOM_METADATA t "
                + "where t.owner = '%s' and t.table_name = '%s'";
    }

    @Override
    public String getSpatialExtentQuery1(String schema, String table, String attributeName) {
        // escape single quote for table name:
        // TODO: do it for schema/user name ?
        return String.format(this.spatialExtentQuery1, schema,
                SQLUtil.escapeSingleQuote(table), attributeName);
    }

    @Override
    public String getSpatialExtentQuery2(String schema, String table, String attributeName) {
        return String.format(this.spatialExtentQuery2, attributeName, schema, table);
    }

    @Override
    public String getGeoColumnsQuery(String datasetName) {
        // escape single quote for table name:
        // TODO: do it for schema/user name ?
        return String.format(this.geoColumnsQuery, getSchemaName(datasetName),
                SQLUtil.escapeSingleQuote(getTableName(datasetName)));
    }

    @Override
    public String getSridQuery(String schemaName, String tableName, String colName) {
        // escape single quote for table name:
        // TODO: do it for schema/user name ?
        return String.format(this.sridQuery, schemaName,
                SQLUtil.escapeSingleQuote(tableName), colName);
    }
    
    @Override
    public List<GeometryColumn> getGeometryAttributes(String datasetName) {
        String sql = this.getGeoColumnsQuery(datasetName);
        return getGeometryAttributes(sql, datasetName);
    }

    @Override
    protected List<GeometryColumn> getGeometryAttributes(String sql, String datasetName) {
        final List<GeometryColumn> geometryAttributes = new ArrayList<GeometryColumn>();
        //System.out.println("getting geom Attribute for dataset: " + datasetName + " with query: " + sql);

        JDBCUtil.execute(
                conn.getJdbcConnection(), sql,
                new ResultSetBlock() {
                    public void yield(ResultSet resultSet) throws SQLException {
                        while (resultSet.next()) {
                            // TODO: escape single quotes in geo column name ?
                            geometryAttributes.add(new GeometryColumn(
                                    resultSet.getString(1),
                                    ((Object[])resultSet.getArray(2).getArray()).length,
                                    resultSet.getInt(3),
                                    resultSet.getString(4)));
                        }
                    }
                });
        return geometryAttributes;
    }

    @Override
    public int getCoordinateDimension(String datasetName, String colName) {
        final StringBuffer coordDim = new StringBuffer();
        String sql = this.getCoordinateDimensionQuery(this.getSchemaName(datasetName),
                this.getTableName(datasetName), colName);
        JDBCUtil.execute(conn.getJdbcConnection(), sql, new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    // Nicolas Ribot: test if a null is returned
                    // Michael Michaud: choose 2 rather than 0 as the default coordDim in case of failure
                    Array array = resultSet.getArray(1);
                    if (array == null) {
                        coordDim.append("2");
                    } else {
                        coordDim.append(((Object[])array.getArray()).length);
                    }
                }
            }
        });

        return Integer.parseInt(coordDim.toString());
    }

}
