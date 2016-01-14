/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jump.datastore.jdbc.BoundQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.*;
import com.vividsolutions.jump.datastore.GeometryColumn;
import java.sql.SQLException;
import java.util.List;

public class OracleDataStoreMetadata extends SpatialDataStoreMetadata {

    public OracleDataStoreMetadata(DataStoreConnection con) {
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
            "  WHERE owner = ? and table_name = ? AND COLUMN_NAME=?\n" +
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
        spatialExtentQuery2 = "select sdo_util.to_wktgeometry(sdo_aggr_mbr(\"%s\")) as geom from \"%s\".\"%s\"";
        
        geoColumnsQuery = "select t.column_name, t.srid, 'SDO_GEOMETRY' as type from ALL_SDO_GEOM_METADATA t "
            + "where t.owner = ? and t.table_name = ?";
        sridQuery = "select t.srid from ALL_SDO_GEOM_METADATA t "
            + "where t.owner = ? and t.table_name = ? and t.COLUMN_NAME = ?";
    }
}
