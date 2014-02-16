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
import org.apache.log4j.Logger;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.compose;
import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.unquote;

/**
 * A {@link WritableDataStoreDataSource} for PostGIS.
 */
public class PostGISDataStoreDataSource extends WritableDataStoreDataSource {

    Logger LOG = Logger.getLogger(WritableDataStoreDataSource.class);

    public PostGISDataStoreDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }

    public PostGISDataStoreDataSource(
            ConnectionDescriptor connectionDescriptor,
            String datasetName,
            String geometryAttributeName,
            String identifierAttributeName) {
        super(connectionDescriptor, datasetName, geometryAttributeName, identifierAttributeName);
    }

    /**
     * After a new postgis table has been create, it is useful to execute a VACUUM ANALYZE
     * in order to update indexes and get a precise estimated_extent.
     * @param conn
     * @param quotedSchemaName
     * @param quotedTableName
     * @throws Exception
     */
    public void finalizeUpdate(Connection conn, String quotedSchemaName, String quotedTableName) throws Exception {
        conn.createStatement().execute("VACUUM ANALYZE " +
                                   PostGISQueryUtil.compose(quotedSchemaName, quotedTableName));
        LOG.debug("VACUUM ANALYZE " + PostGISQueryUtil.compose(quotedSchemaName, quotedTableName));
    }

    protected FeatureCollection createFeatureCollection() throws Exception {

        LOG.debug("Create new FeatureCollection from " + getProperties().get(DATASET_NAME_KEY));
        ConnectionDescriptor connectionDescriptor =
                (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);

        PostgisDSConnection pgConnection =
                (PostgisDSConnection)new PostgisDataStoreDriver()
                        .createConnection(connectionDescriptor.getParameterList());

        boolean hasPK = getProperties().get(EXTERNAL_PK_KEY) != null;
        String PK = (String)getProperties().get(EXTERNAL_PK_KEY);
        String query = buildQueryString(pgConnection);
        LOG.debug(query);

        // Create the adhoc query corresponding to this datasource
        AdhocQuery adhocQuery = new AdhocQuery(query);
        if (hasPK) {
            adhocQuery.setPrimaryKey(PK);
        }

        // Get features
        FeatureInputStream featureInputStream = null;
        FeatureDataset featureDataset = null;
        try {
            featureInputStream = pgConnection.execute(adhocQuery);
            featureDataset = new FeatureDataset(featureInputStream.getFeatureSchema());
            if (hasPK) {
                featureDataset.getFeatureSchema().setExternalPrimaryKeyIndex(
                        featureDataset.getFeatureSchema().getAttributeIndex(PK)
                );
            }
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
        return featureDataset;

    }

    private String buildQueryString(PostgisDSConnection pgConnection) throws SQLException {

        String geometryColumn = (String)getProperties().get(GEOMETRY_ATTRIBUTE_NAME_KEY);
        String[] datasetName = PostGISQueryUtil.splitTableName((String)getProperties().get(DATASET_NAME_KEY));
        String schemaName = datasetName[0];
        String tableName = datasetName[1];

        //@TODO This method should be available in DataStoreMetadata to avoid the cast
        String[] columns = ((PostgisDSMetadata)pgConnection.getMetadata()).getColumnNames(PostGISQueryUtil.unquote(tableName));

        Connection conn = pgConnection.getConnection();

        int table_srid = getTableSRID(conn, schemaName, tableName, geometryColumn);

        boolean limited_to_view = (Boolean)getProperties().get(LIMITED_TO_VIEW);
        String extent = limited_to_view ? " AND (\"" + geometryColumn + "\" && ST_GeomFromText('" +
                getViewEnvelope().toText() + "'," + table_srid + "))" : "";

        String whereClause = (String)getProperties().get(WHERE_CLAUSE_KEY);
        whereClause = (whereClause == null || whereClause.length() == 0) ? "true" : "(" + whereClause + ")";

        int max_features = (Integer)getProperties().get(MAX_FEATURES_KEY);

        StringBuffer sb = new StringBuffer("SELECT \"").append(geometryColumn).append("\"");
        for (String col : columns) {
            if (col.equals(geometryColumn)) continue;
            sb.append(", \"").append(col).append("\"");
        }
        sb.append(" FROM \"")
                .append(unquote((String)getProperties().get(DATASET_NAME_KEY)))
                .append("\" WHERE ")
                .append(whereClause)
                .append(extent)
                .append(" LIMIT " + max_features)
                .append(";");
        return sb.toString();
    }


    /**
     * Execute a query on this connection to DROP this table as well as
     * its reference in the PostGIS's geometry_columns table (PostGIS < 2).
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
        PreparedStatement statement = insertStatement(conn, fc.getFeatureSchema(), dbSchema, dbTable, primaryKey, srid, dim);
        int count = 0;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            //insertStatement(conn, f, dbSchema, dbTable, primaryKey, srid, dim).executeUpdate();
            statement = setAttributeValues(statement, f, srid, dim, primaryKey);
            statement.addBatch();
            if (count++ % 10000 == 0) {
                statement.executeBatch();
                statement.clearBatch();
            }
        }
        statement.executeBatch();
        statement.clearBatch();
    }


    protected void addDBPrimaryKey(java.sql.Connection conn, String dbSchema,
                                 String dbTable, String primaryKey) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        String sql_create_dbid = "ALTER TABLE " + tableName + " ADD COLUMN \"" +
                primaryKey + "\" serial NOT NULL PRIMARY KEY;";
        try {
            conn.createStatement().execute(sql_create_dbid);
        } catch (SQLException sqle) {
            throw new SQLException(sql_create_dbid, sqle);
        }
    }

    // Find_SRID replaced ST_Find_SRID in PostGIS 1.4
    // @TODO for backward compatibility it could be useful to implement a 1.3 compatibility mode
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
