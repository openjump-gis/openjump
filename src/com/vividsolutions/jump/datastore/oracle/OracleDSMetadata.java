package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.PrimaryKeyColumn;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;
import java.sql.DatabaseMetaData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class OracleDSMetadata implements DataStoreMetadata {

    private final WKBReader reader = new WKBReader();

    private OracleDSConnection conn;
    // connection username, used a default schema
    private String userSchema;

    private Map sridMap = new HashMap();
    
    // map storing the geo column names and the name of their index 
    private HashMap<String, String> geoIndexes = new HashMap<String, String>();

    public OracleDSMetadata(OracleDSConnection conn) {
        this.conn = conn;

        try {
            this.userSchema = conn.getConnection().getMetaData().getUserName();
        } catch (SQLException ex) {
            System.out.println(ex.toString());
        }
    }

    public HashMap<String, String> getGeoIndexes() {
        return geoIndexes;
    }

    public String[] getDatasetNames() {
        final List datasetNames = new ArrayList();
        // Spatial tables only.
        JDBCUtil.execute(
            conn.getConnection(),
            "select distinct owner, table_name from ALL_SDO_GEOM_METADATA",
            new ResultSetBlock() {
                public void yield(ResultSet resultSet) throws SQLException {
                    while (resultSet.next()) {
                        String schema = resultSet.getString(1);
                        String table = resultSet.getString(2);
                        // on Oracle, user's schema is the default schema
                        if (!schema.equalsIgnoreCase(userSchema)) {
                            table = schema + "." + table;
                        }
                        datasetNames.add(table);
                    }
                }
            });
        return (String[]) datasetNames.toArray(new String[datasetNames.size()]);
    }

    // TODO: check
    public Envelope getExtents(String datasetName, String attributeName) {

        final Envelope[] e = new Envelope[]{null};
        // extent is taken from layer's metadata
        String sql1 = "SELECT dim.* FROM ALL_SDO_GEOM_METADATA usgm, TABLE(usgm.diminfo) "
            + "dim WHERE table_name = '" + getTableName(datasetName) + "' and owner='" + getSchemaName(datasetName) 
            + "' and column_name = '" + attributeName + "'";
        
        System.out.println("getting extent: " + sql1);

        final ResultSetBlock resultSetBlock = new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws Exception {
                // looks only for X-Y dimension, though oracle metadata can store more
                double xmin = 0, ymin = 0, xmax = 0, ymax = 0;
                while (resultSet.next()) {
                    if ("X".equalsIgnoreCase(resultSet.getString(1))) {
                        xmin = resultSet.getDouble(2);
                        xmax = resultSet.getDouble(3);
                    }
                    if ("Y".equalsIgnoreCase(resultSet.getString(1))) {
                        ymin = resultSet.getDouble(2);
                        ymax = resultSet.getDouble(3);
                    }
                }
                e[0] = new Envelope(xmin, ymin, xmax, ymax);
            }
        };
        try {
            JDBCUtil.execute(conn.getConnection(), (sql1), resultSetBlock);
            if (e[0] == null || e[0].isNull()) {
                JDBCUtil.execute(conn.getConnection(), (sql1), resultSetBlock);
            }
        } catch (Exception ex1) {
            // TODO: 
            ex1.printStackTrace();
        }
        return e[0];
    }

    /**
     * Returns the list of geometryColumns for the given name in Oracle,
     * metadata does not store this info: queries the table to get distinct list
     * of geometries: if several found: geometry type. Also retrieves geom srid
     * from metadata and stores it in the map
     *
     * @param datasetName
     * @return
     */
    public List<GeometryColumn> getGeometryAttributes(final String datasetName) {
        final List<GeometryColumn> geometryAttributes = new ArrayList<GeometryColumn>();
        String sql = "select column_name, srid from ALL_SDO_GEOM_METADATA "
            + geomColumnMetadataWhereClause("owner", "table_name", datasetName);
        JDBCUtil.execute(
            conn.getConnection(), sql,
            new ResultSetBlock() {
                public void yield(ResultSet resultSet) throws SQLException {
                    while (resultSet.next()) {
                        String colName = resultSet.getString(1);
                        int srid = resultSet.getInt(2);
                        sridMap.put(datasetName + "#" + colName, new SpatialReferenceSystemID(srid));
                        geometryAttributes.add(new GeometryColumn(
                                colName,
                                srid
                            //              ,resultSet.getString(3)
                            ));
                    }
                }
            });
        
        // TODO: move MD retrieving in a JDBCUtil static method and use JDBC metadata
        // as deep as we can ?
        geoIndexes = new HashMap<String, String>();
        try {
            // gets info in cnx metadata:
            DatabaseMetaData md = conn.getConnection().getMetaData();
            ResultSet rs = md.getIndexInfo(null, getSchemaName(datasetName), getTableName(datasetName), false, true);
            StringBuilder b = new StringBuilder();
            while (rs.next()) {
                if (rs.getString(6) != null && rs.getString(9) != null) {
                    geoIndexes.put(rs.getString(9), rs.getString(6));
                }
            }
        } catch (SQLException ex) {
            // TODO
        }

        // gets the geometry column type for each geo col
        for (final GeometryColumn gc : geometryAttributes) {
            sql = "select distinct t." + gc.getName() + ".sdo_gtype from " + datasetName + " t";
            JDBCUtil.execute(
                conn.getConnection(), sql,
                new ResultSetBlock() {
                    public void yield(ResultSet resultSet) throws SQLException {
                        int gtype = 0;
                        int i = 0;
                        String type = "";
                        while (resultSet.next()) {
                            gtype = resultSet.getInt(1) % 10;
                            if (i == 0) {
                                switch (gtype) {
                                    case 1:
                                        type = "POINT";
                                        break;
                                    case 2:
                                        type = "LINESTRING";
                                        break;
                                    case 3:
                                        type = "POLYGON";
                                        break;
                                    case 4:
                                        type = "GEOMETRYCOLLECTION";
                                        break;
                                    case 5:
                                        type = "MULTIPOINT";
                                        break;
                                    case 6:
                                        type = "MULTILINESTRING";
                                        break;
                                    case 7:
                                        type = "MULTIPOLYGON";
                                        break;
                                    default:
                                        type = "GEOMETRY";
                                }
                            } else if (i > 0) {
                                // more than one geo type:
                                type = "GEOMETRY";
                                break;
                            }
                            i++;
                        }
                        gc.setType(type);
                    }
                });
            // also gets info about index is column indexed: useful to avoid calling sdo_filter on
            // a non-indexed column
            gc.setIndexed(geoIndexes.containsKey(gc.getName()));
        }
        return geometryAttributes;
    }

    /**
     * Returns PRIMARY KEY columns of dataset names. // TODO: check STATUS PK:
     * enabled/disabled
     *
     * @param datasetName name of the table (optionally prefixed by the schema
     * name)
     * @return the list of columns involved in the Primary Key (generally, a
     * single column)
     */
    public List<PrimaryKeyColumn> getPrimaryKeyColumns(String datasetName) {
        final List<PrimaryKeyColumn> identifierColumns = new ArrayList<PrimaryKeyColumn>();
        // query taken from http://www.alberton.info/postgresql_meta_info.html#.UewsFG29b0Q
        String sql
            = "SELECT cols.table_name, cols.column_name \n"
            + "FROM all_constraints cons, all_cons_columns cols \n"
            + "WHERE cols.table_name = '" + getTableName(datasetName) + "' and cols.OWNER = '"
            + getSchemaName(datasetName) + "' \n"
            + "AND cons.constraint_type = 'P' \n"
            + "AND cons.constraint_name = cols.constraint_name \n"
            + "AND cons.owner = cols.owner \n"
            + "ORDER BY cols.table_name, cols.position";
        JDBCUtil.execute(
            conn.getConnection(), sql,
            new ResultSetBlock() {
                public void yield(ResultSet resultSet) throws SQLException {
                    while (resultSet.next()) {
                        identifierColumns.add(new PrimaryKeyColumn(
                                resultSet.getString(1),
                                resultSet.getString(2)));
                    }
                }
            });
        return identifierColumns;
    }

    /**
     * Fetch only not hidden columns
     *
     * @param datasetName
     * @return
     */
    public String[] getColumnNames(String datasetName) {
        String sql = "SELECT cols.COLUMN_NAME, cols.DATA_TYPE \n"
            + "FROM ALL_TAB_COLS cols \n"
            + "WHERE hidden_column = 'NO' and cols.table_name = '" + getTableName(datasetName) + "' and cols.OWNER = '"
            + getSchemaName(datasetName) + "' \n"
            + "ORDER BY cols.table_name, cols.COLUMN_ID";
        ColumnNameBlock block = new ColumnNameBlock();
        JDBCUtil.execute(conn.getConnection(), sql, block);
        return block.colName;
    }

    /**
     * Returns the schema name based on the given tableName: string before . if
     * exists, else returns userName
     *
     * @param tableName
     * @return
     */
    private String getSchemaName(String tableName) {
        int dotPos = tableName.indexOf(".");
        String schema = this.userSchema;
        if (dotPos != -1) {
            schema = tableName.substring(0, dotPos);
        }
        return schema;
    }

    /**
     * Returns the table name based on the given tableName: string after "." if
     * exists, else returns userName
     *
     * @param tableName
     * @return
     */
    private String getTableName(String tableName) {
        int dotPos = tableName.indexOf(".");
        String ret = tableName;
        if (dotPos != -1) {
            ret = tableName.substring(0, dotPos);
        }
        return ret;
    }

    @Deprecated
    public SpatialReferenceSystemID getSRID(String tableName, String colName)
        throws SQLException {
        String key = tableName + "#" + colName;
        if (!sridMap.containsKey(key)) {
            // not in cache, so query it
            String srid = querySRID(tableName, colName);
            sridMap.put(key, new SpatialReferenceSystemID(srid));
        }
        return (SpatialReferenceSystemID) sridMap.get(key);
    }

    // TODO: should never be called: sridMap should be filled when getting geo columns
    @Deprecated
    private String querySRID(String tableName, String colName) {
        final StringBuffer srid = new StringBuffer();
    // Changed by Michael Michaud 2010-05-26 (throwed exception for empty tableName)
        // String sql = "SELECT getsrid(" + colName + ") FROM " + tableName + " LIMIT 1";
        String[] tokens = tableName.split("\\.", 2);
        String schema = tokens.length == 2 ? tokens[0] : "public";
        String table = tokens.length == 2 ? tokens[1] : tableName;
        String sql = "SELECT srid FROM geometry_columns where (f_table_schema = '" + schema + "' and f_table_name = '" + table + "')";
        // End of the fix
        JDBCUtil.execute(conn.getConnection(), sql, new ResultSetBlock() {
            public void yield(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    srid.append(resultSet.getString(1));
                }
            }
        });

        return srid.toString();
    }

    private String geomColumnMetadataWhereClause(String schemaCol, String tableCol, String tableName) {
        // [mmichaud 2011-07-24] Fixed a bug related to tables having common
        // names in public schema and another schema
        int dotPos = tableName.indexOf(".");
        String schema = this.userSchema;
        String table = tableName;
        if (dotPos != -1) {
            schema = tableName.substring(0, dotPos);
            table = tableName.substring(dotPos + 1);
        }
        return "WHERE " + schemaCol + " = '" + schema + "'"
            + " AND " + tableCol + " = '" + table + "'";
    }

    private static class ColumnNameBlock implements ResultSetBlock {

        List colList = new ArrayList();
        String[] colName;

        public void yield(ResultSet resultSet) throws SQLException {
            while (resultSet.next()) {
                colList.add(resultSet.getString(1));
            }
            colName = (String[]) colList.toArray(new String[colList.size()]);
        }
    }

}
