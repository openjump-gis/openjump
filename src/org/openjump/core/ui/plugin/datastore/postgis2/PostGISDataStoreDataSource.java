package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.postgis.PostgisDSConnection;
import com.vividsolutions.jump.datastore.postgis.PostgisDSMetadata;
import com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import org.apache.log4j.Logger;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.compose;
import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.unquote;

/**
 * Writable DataStoreDataSource for PostGIS.
 */
public class PostGISDataStoreDataSource extends WritableDataStoreDataSource {

    Logger LOG = Logger.getLogger(WritableDataStoreDataSource.class);

    public PostGISDataStoreDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }

    public PostGISDataStoreDataSource(ConnectionDescriptor connectionDescriptor,
            String datasetName, String geometryAttributeName, String identifierAttributeName) {
        super(connectionDescriptor, datasetName, geometryAttributeName, identifierAttributeName);
    }

    public void finalizeUpdate(java.sql.Connection conn, String quotedSchemaName, String quotedTableName) throws Exception {
        conn.createStatement().execute("VACUUM ANALYZE " +
                                   PostGISQueryUtil.compose(quotedSchemaName, quotedTableName));
        LOG.debug("VACUUM ANALYZE " + PostGISQueryUtil.compose(quotedSchemaName, quotedTableName));
    }

    //@TODO This method needs to be decomposed and cleaned
    protected FeatureCollection createFeatureCollection() throws Exception {

        ConnectionDescriptor connectionDescriptor =
                (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);

        // Schema name, table name and geometry column name are needed
        // to get the database srid associated to this FeatureCollection
        String[] datasetName = PostGISQueryUtil.splitTableName((String)getProperties().get(DATASET_NAME_KEY));
        String schemaName = datasetName[0];
        String tableName = datasetName[1];
        String geometryColumn = (String)getProperties().get(GEOMETRY_ATTRIBUTE_NAME_KEY);

        // get the srid to use for this dataset and geometry column
        PostgisDSConnection pgConnection =
                (PostgisDSConnection)new PostgisDataStoreDriver()
                        .createConnection(connectionDescriptor.getParameterList());
        java.sql.Connection conn = pgConnection.getConnection();
        int table_srid = getTableSRID(conn, schemaName, tableName, geometryColumn);

        // Set the query string, including semantic and geometric conditions as needd
        boolean limited_to_view = (Boolean)getProperties().get(LIMITED_TO_VIEW);
        String extent = limited_to_view ? " AND (\"" + geometryColumn + "\" && ST_GeomFromText('" +
                getViewEnvelope().toText() + "'," + table_srid + "))" : "";
        String whereClause = (String)getProperties().get(WHERE_CLAUSE_KEY);
        whereClause = (whereClause == null || whereClause.length() == 0) ? "true" : "(" + whereClause + ")";
        String PK = (String)getProperties().get(EXTERNAL_PK_KEY);
        int max_features = (Integer)getProperties().get(MAX_FEATURES_KEY);

        StringBuffer sb = new StringBuffer("SELECT \"").append(geometryColumn).append("\"");
        //@TODO This method should be available in DataStoreMetadata to avoid the cast
        String[] columns = ((PostgisDSMetadata)pgConnection.getMetadata()).getColumnNames(PostGISQueryUtil.unquote(tableName));
        for (String col : columns) {
            if (col.equals(geometryColumn)) continue;
            //if (col.equals(PK)) continue;
            sb.append(", \"").append(col).append("\"");
        }
        sb.append(" FROM \"")
          .append(unquote((String)getProperties().get(DATASET_NAME_KEY)))
          .append("\" WHERE ")
          .append(whereClause)
          .append(extent)
          .append(" LIMIT " + max_features)
          .append(";");
        //String query = "SELECT * FROM \"" + unquote((String)getProperties().get(DATASET_NAME_KEY)) + "\" WHERE " + whereClause + extent + ";";
        String query = sb.toString();
        LOG.debug(query);

        // Create the adhoc query corresponding to this datasource
        AdhocQuery adhocQuery = new AdhocQuery(query);

        if (getProperties().get(EXTERNAL_PK_KEY) != null) {
            adhocQuery.setPrimaryKey(PK);
        }

        // Get features
        FeatureInputStream featureInputStream = null;
        FeatureDataset featureDataset = null;
        try {
            LOG.debug("Connection opened: " + !conn.isClosed());
            featureInputStream = ConnectionManager.instance(context)
                    .getOpenConnection(connectionDescriptor).execute(adhocQuery);
            featureDataset = new FeatureDataset(featureInputStream.getFeatureSchema());
            featureDataset.getFeatureSchema().setExternalPrimaryKeyIndex(
                    featureDataset.getFeatureSchema().getAttributeIndex(PK)
            );
            while (featureInputStream.hasNext()) {
                featureDataset.add( featureInputStream.next() );
            }
            return featureDataset;
        }
        catch(Exception e) {
            if (context != null) {
                context.getWorkbench().getFrame().handleThrowable(e);
            }
            featureDataset = new FeatureDataset(new FeatureSchema());
        }
        finally {
            //if (featureInputStream != null) {
            //    try {featureInputStream.close();}
            //    catch(Exception e){
            //        LOG.error("Error closing FeatureInputStream : " +
            //                getProperties().get(DATASET_NAME_KEY) + " (" +
            //                getProperties().get(CONNECTION_DESCRIPTOR_KEY) + ")", e);
            //    }
            //}
        }
        return featureDataset;

    }


    /**
     * Execute a query against this connection to delete the reference to this
     * table in the PostGIS's geometry_columns table.
     */
    protected void deleteTableQuery(java.sql.Connection connection,
                                  String dbSchema, String dbTable) throws SQLException {
        try {
            // Try to delete dbTable AND the corresponding rows in geometry_columns table
            if (dbSchema == null) {
                connection.createStatement().execute("SELECT DropGeometryTable( '" +
                        unquote(dbTable) + "' );");
            } else {
                connection.createStatement().execute("SELECT DropGeometryTable( '" +
                        unquote(dbSchema) + "' , '" + unquote(dbTable) + "' );");
            }
        } catch(SQLException e) {
            // If DropGeometryTable failed, try a simple DROP TABLE statement
            connection.createStatement().execute("DROP TABLE " + compose(dbSchema, dbTable) + ";");
        }
    }

    protected void createAndPopulateTable(
            java.sql.Connection conn,
            FeatureCollection fc,
            String dbSchema, String dbTable,
            int srid, String geometryType, int dim) throws SQLException {
        FeatureSchema schema = fc.getFeatureSchema();
        String geometryColumn = schema.getAttributeName(schema.getGeometryIndex());
        try {
            conn.createStatement().execute(PostGISQueryUtil.getCreateTableStatement(fc.getFeatureSchema(), dbSchema, dbTable));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil.getCreateTableStatement(fc.getFeatureSchema(), dbSchema, dbTable), sqle);
        }
        try {
            conn.createStatement().execute(PostGISQueryUtil.getAddGeometryColumnStatement(dbSchema, dbTable, geometryColumn, srid, geometryType, dim));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil.getAddGeometryColumnStatement(dbSchema, dbTable, geometryColumn, srid, geometryType, dim), sqle);
        }
        populateTable(conn, fc, dbSchema, dbTable, null, srid, dim);
        try {
            conn.createStatement().execute(PostGISQueryUtil.getAddSpatialIndexStatement(dbSchema, dbTable, geometryColumn));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil.getAddSpatialIndexStatement(dbSchema, dbTable, geometryColumn), sqle);
        }
    }

    private void populateTable(java.sql.Connection conn, FeatureCollection fc,
                               String dbSchema, String dbTable, String primaryKey, int srid, int dim) throws SQLException {
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            insertStatement(conn, f, dbSchema, dbTable, primaryKey, srid, dim).executeUpdate();
        }
    }


    protected void addDBPrimaryKey(java.sql.Connection conn, String dbSchema,
                                 String dbTable, String primaryKey) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        String sql_test_seq = "SELECT * FROM information_schema.sequences\n" +
                "    WHERE sequence_schema = 'public' AND sequence_name = 'openjump_dbid_sequence';";
        String sql_create_seq = "CREATE SEQUENCE openjump_dbid_sequence;";
        String sql_create_dbid = "ALTER TABLE " + tableName + " ADD COLUMN \"" +
                primaryKey + "\" BIGINT DEFAULT nextval('openjump_dbid_sequence') PRIMARY KEY;";
        boolean sequence_already_exists;
        // check if openjump_dbid_sequence already exists
        try {
            sequence_already_exists = conn.createStatement().executeQuery(sql_test_seq).next();
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + sql_test_seq, sqle);
        }
        if (!sequence_already_exists) {
            // create the sequence
            try {
                conn.createStatement().execute(sql_create_seq);
            } catch (SQLException sqle) {
                throw new SQLException(sql_create_seq, sqle);
            }
        }
        // add the column based on openjump_dbid_sequence
        try {
            conn.createStatement().execute(sql_create_dbid);
        } catch (SQLException sqle) {
            throw new SQLException(sql_create_dbid, sqle);
        }
    }

    protected int getTableSRID(java.sql.Connection conn, String dbSchema, String dbTable, String column) throws SQLException {
        String sql = dbSchema == null ?
                "SELECT Find_SRID('public', '" + unquote(dbTable) + "', '" + unquote(column) + "');" :
                "SELECT Find_SRID('" + unquote(dbSchema) + "', '" + unquote(dbTable) + "', '" + unquote(column) + "');";
        LOG.debug(sql);
        ResultSet rs = conn.prepareStatement(sql).executeQuery();
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }



}
