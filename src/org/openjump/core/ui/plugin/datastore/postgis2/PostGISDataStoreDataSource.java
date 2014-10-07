package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jts.geom.Coordinate;
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
import java.util.Iterator;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.compose;
import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.normalize;
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
     * @throws Exception
     */
    public void finalizeUpdate(Connection conn) throws Exception {
        conn.createStatement().execute("VACUUM ANALYZE " +
                                   PostGISQueryUtil.compose(schemaName, tableName));
        LOG.debug("VACUUM ANALYZE " + PostGISQueryUtil.compose(schemaName, tableName));
    }

    protected FeatureCollection createFeatureCollection() throws Exception {

        LOG.debug("Create new FeatureCollection from " + getProperties().get(DATASET_NAME_KEY));
        //String[] datasetName = PostGISQueryUtil.splitTableName((String)getProperties().get(DATASET_NAME_KEY));
        //schemaName = datasetName[0];
        //tableName = datasetName[1];
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

        //@TODO This method should be available in DataStoreMetadata to avoid the cast
        String[] columns = ((PostgisDSMetadata)pgConnection.getMetadata())
                .getColumnNames(PostGISQueryUtil.unquote((String)getProperties().get(DATASET_NAME_KEY)));

        Connection conn = pgConnection.getConnection();

        int table_srid = getTableSRID(conn, geometryColumn);

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
                .append(schemaName == null ? "" : unquote(schemaName)+"\".\"")
                .append(unquote(tableName))
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
    protected void deleteTableQuery(java.sql.Connection connection) throws SQLException {
        try {
            // Try to delete dbTable AND the corresponding rows in geometry_columns table
            if (schemaName == null) {
                connection.createStatement().execute("SELECT DropGeometryTable( '" +
                        tableName + "' );");
            } else {
                connection.createStatement().execute("SELECT DropGeometryTable( '" +
                        schemaName + "' , '" + tableName + "' );");
            }
        } catch(SQLException e) {
            // If DropGeometryTable failed, try a simple DROP TABLE statement
            connection.createStatement().execute("DROP TABLE " + compose(schemaName, tableName) + ";");
        }
    }

    /**
     * Create and populate a table with features from a dataset.
     * @param conn connection to the database
     * @param fc featureCollection to upload to the database
     * @param srid srid of the geometry
     * @param geometryType geometry type
     * @param dim geometry dimension
     * @param normalizedColumnNames whether columns names have to be normalized or not
     * @throws SQLException
     */
    protected void createAndPopulateTable(
            java.sql.Connection conn,
            FeatureCollection fc,
            int srid, String geometryType, int dim,
            boolean normalizedColumnNames) throws SQLException {
        FeatureSchema schema = fc.getFeatureSchema();
        String geometryColumn = normalizedColumnNames ?
                normalize(schema.getAttributeName(schema.getGeometryIndex()))
                :schema.getAttributeName(schema.getGeometryIndex());
        try {
            conn.createStatement().execute(PostGISQueryUtil
                    .getCreateTableStatement(fc.getFeatureSchema(), schemaName, tableName, normalizedColumnNames));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil
                    .getCreateTableStatement(fc.getFeatureSchema(), schemaName, tableName, normalizedColumnNames), sqle);
        }
        try {
            conn.createStatement().execute(PostGISQueryUtil
                    .getAddGeometryColumnStatement(schemaName, tableName, geometryColumn, srid, geometryType, dim));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil
                    .getAddGeometryColumnStatement(schemaName, tableName, geometryColumn, srid, geometryType, dim), sqle);
        }
        populateTable(conn, fc, null, srid, dim, normalizedColumnNames);
        try {
            conn.createStatement().execute(PostGISQueryUtil
                    .getAddSpatialIndexStatement(schemaName, tableName, geometryColumn));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil
                    .getAddSpatialIndexStatement(schemaName, tableName, geometryColumn), sqle);
        }
    }

    private void populateTable(java.sql.Connection conn, FeatureCollection fc, String primaryKey,
                               int srid, int dim, boolean normalizedColumnNames) throws SQLException {
        PreparedStatement statement = insertStatement(conn, fc.getFeatureSchema(), normalizedColumnNames);
        int count = 0;
        // There is an option to convert NaN values to any double value while uploading
        // z is changed without duplicating the geometry
        // in normal case, uploaded dataset will be downloaded just after
        // if the upload breaks, a part of geometries may have their z changed
        double replacementZ = getProperties().get(NAN_Z_TO_VALUE_KEY) == null ?
                Double.NaN :
                (Double)getProperties().get(NAN_Z_TO_VALUE_KEY);
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            if (dim==3 && getProperties().get(GEOM_DIM_KEY) != null) {
                for (Coordinate c : f.getGeometry().getCoordinates()) {
                    if (Double.isNaN(c.z)) c.z = replacementZ;
                }
            }
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


    protected void addDBPrimaryKey(java.sql.Connection conn, String primaryKey) throws SQLException {
        String sql_create_dbid = "ALTER TABLE " + compose(schemaName,tableName) + " ADD COLUMN \"" +
                primaryKey + "\" serial NOT NULL PRIMARY KEY;";
        try {
            conn.createStatement().execute(sql_create_dbid);
        } catch (SQLException sqle) {
            throw new SQLException(sql_create_dbid, sqle);
        }
    }

    // Find_SRID replaced ST_Find_SRID in PostGIS 1.4
    // @TODO for backward compatibility it could be useful to implement a 1.3 compatibility mode
    protected int getTableSRID(java.sql.Connection conn, String column) throws SQLException {
        String sql = schemaName == null ?
                "SELECT Find_SRID('public', '" + tableName + "', '" + unquote(column) + "');" :
                "SELECT Find_SRID('" + schemaName + "', '" + tableName + "', '" + unquote(column) + "');";
        LOG.debug(sql);
        ResultSet rs = conn.prepareStatement(sql).executeQuery();
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }

}
