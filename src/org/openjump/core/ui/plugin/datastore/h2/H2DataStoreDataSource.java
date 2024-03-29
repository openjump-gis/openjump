package org.openjump.core.ui.plugin.datastore.h2;

import org.locationtech.jts.geom.Coordinate;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.h2.H2DSConnection;
import com.vividsolutions.jump.datastore.h2.H2DataStoreDriver;
import com.vividsolutions.jump.datastore.h2.H2ServerDataStoreDriver;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * A {@link WritableDataStoreDataSource} for H2.
 */
public class H2DataStoreDataSource extends WritableDataStoreDataSource {

    public H2DataStoreDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }

    public H2DataStoreDataSource(WorkbenchContext context) {
        this.context = context;
    }

    public H2DataStoreDataSource(
            ConnectionDescriptor connectionDescriptor,
            String datasetName,
            String geometryAttributeName,
            String identifierAttributeName,
            String txManagerClass,
            WorkbenchContext context) {
        super(connectionDescriptor,
                datasetName,
                geometryAttributeName,
                identifierAttributeName,
                txManagerClass,
                context);
    }

    /**
     * After a new postgis table has been create, it is useful to execute a VACUUM ANALYZE
     * in order to update indexes and get a precise estimated_extent.
     */
    public void finalizeUpdate(SpatialDatabasesDSConnection conn) throws Exception {
        //conn.getJdbcConnection().createStatement().execute("VACUUM ANALYZE " +
        //        SQLUtil.compose(schemaName, tableName));
        //Logger.debug("VACUUM ANALYZE " + SQLUtil.compose(schemaName, tableName));
    }

    protected FeatureCollection createFeatureCollection() throws Exception {

        Logger.debug("Create new FeatureCollection from " + getProperties().get(DATASET_NAME_KEY));
        ConnectionDescriptor connectionDescriptor =
                (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);


        H2DSConnection h2Connection;
        if (connectionDescriptor.getDataStoreDriverClassName()
                .equals(com.vividsolutions.jump.datastore.h2.H2DataStoreDriver.class.getName())) {
            h2Connection = (H2DSConnection)new H2DataStoreDriver()
                    .createConnection(connectionDescriptor.getParameterList());
        } else if (connectionDescriptor.getDataStoreDriverClassName()
                .equals(com.vividsolutions.jump.datastore.h2.H2ServerDataStoreDriver.class.getName())) {
            h2Connection = (H2DSConnection)new H2ServerDataStoreDriver()
                    .createConnection(connectionDescriptor.getParameterList());
        } else {
            throw new IllegalArgumentException("The connection does not use a H2 Driver");
        }


        boolean hasPK = getProperties().get(EXTERNAL_PK_KEY) != null;
        String PK = (String)getProperties().get(EXTERNAL_PK_KEY);
        String query = buildQueryString(h2Connection);
        Logger.debug(query);

        // Create the adhoc query corresponding to this datasource
        AdhocQuery adhocQuery = new AdhocQuery(query);
        if (hasPK) {
            adhocQuery.setPrimaryKey(PK);
        }

        // Get features
        FeatureInputStream featureInputStream = null;
        FeatureDataset featureDataset = null;
        try {
            featureInputStream = h2Connection.execute(adhocQuery);
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

    private String buildQueryString(SpatialDatabasesDSConnection conn) throws SQLException {

        String geometryColumn = (String)getProperties().get(GEOMETRY_ATTRIBUTE_NAME_KEY);

        //@TODO This method should be available in DataStoreMetadata to avoid the cast
        String[] columns = conn.getMetadata()
                .getColumnNames(SQLUtil.unquote((String) getProperties().get(DATASET_NAME_KEY)));

        int table_srid = getTableSRID(conn.getJdbcConnection(), geometryColumn);

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
                .append(schemaName == null ? "" : SQLUtil.unquote(schemaName)+"\".\"")
                .append(SQLUtil.unquote(tableName))
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
    protected void deleteTableQuery(SpatialDatabasesDSConnection conn) throws SQLException {
        conn.getJdbcConnection().createStatement().execute("DROP TABLE " + SQLUtil.compose(schemaName, tableName) + ";");
    }

    /**
     * Create and populate a table with features from a dataset.
     * @param conn connection to the database
     * @param fc featureCollection to upload to the database
     * @param srid srid of the geometry
     * @param geometryType geometry type
     * @param dim geometry dimension
     * @param normalizedColumnNames whether columns names have to be normalized or not
     * @throws SQLException if an Exception occurs while accessing database
     */
    protected void createAndPopulateTable(
            SpatialDatabasesDSConnection conn,
            FeatureCollection fc,
            int srid, String geometryType, boolean multi, int dim,
            boolean normalizedColumnNames) throws SQLException {
        FeatureSchema schema = fc.getFeatureSchema();
        String geometryColumn = normalizedColumnNames ?
                SQLUtil.normalize(schema.getAttributeName(schema.getGeometryIndex()))
                :schema.getAttributeName(schema.getGeometryIndex());
        try {
            conn.getJdbcConnection().createStatement().execute(conn.getMetadata()
                    .getCreateTableStatement(fc.getFeatureSchema(), schemaName, tableName, normalizedColumnNames));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + conn.getMetadata()
                    .getCreateTableStatement(fc.getFeatureSchema(), schemaName, tableName, normalizedColumnNames), sqle);
        }
        try {
            conn.getJdbcConnection().createStatement().execute(conn.getMetadata()
                    .getAddGeometryColumnStatement(schemaName, tableName, geometryColumn, srid, geometryType, dim));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + conn.getMetadata()
                    .getAddGeometryColumnStatement(schemaName, tableName, geometryColumn, srid, geometryType, dim), sqle);
        }
        populateTable(conn, fc, null, srid, multi, dim, normalizedColumnNames);
        try {
            conn.getJdbcConnection().createStatement().execute(conn.getMetadata()
                    .getAddSpatialIndexStatement(schemaName, tableName, geometryColumn));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + conn.getMetadata()
                    .getAddSpatialIndexStatement(schemaName, tableName, geometryColumn), sqle);
        }
    }

    private void populateTable(SpatialDatabasesDSConnection conn, FeatureCollection fc, String primaryKey,
                               int srid, boolean multi, int dim, boolean normalizedColumnNames) throws SQLException {
        PreparedStatement statement = insertStatement(conn, fc.getFeatureSchema(), multi, normalizedColumnNames);
        int count = 0;
        // There is an option to convert NaN values to any double value while uploading
        // z is changed without duplicating the geometry
        // in normal case, uploaded dataset will be downloaded just after
        // if the upload breaks, a part of geometries may have their z changed
        double replacementZ = getProperties().get(NAN_Z_TO_VALUE_KEY) == null ?
                Double.NaN :
                (Double)getProperties().get(NAN_Z_TO_VALUE_KEY);
        for (Iterator it = fc.iterator(); it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            if (dim==3 && getProperties().get(GEOM_DIM_KEY) != null) {
                for (Coordinate c : f.getGeometry().getCoordinates()) {
                    if (Double.isNaN(c.z)) c.z = replacementZ;
                }
            }
            statement = setAttributeValues(statement, f, srid, multi, dim);
            statement.addBatch();
            if (count++ % 10000 == 0) {
                statement.executeBatch();
                statement.clearBatch();
            }
        }
        statement.executeBatch();
        statement.clearBatch();
    }


    protected void addDBPrimaryKey(SpatialDatabasesDSConnection conn, String primaryKey) throws SQLException {
        String sql_create_dbid = "ALTER TABLE " + SQLUtil.compose(schemaName, tableName) + " ADD COLUMN \"" +
                primaryKey + "\" serial NOT NULL;";
        sql_create_dbid += "\nCREATE PRIMARY KEY ON " + SQLUtil.compose(schemaName, tableName) + "(\"" + primaryKey + "\")";
        try {
            conn.getJdbcConnection().createStatement().execute(sql_create_dbid);
        } catch (SQLException sqle) {
            throw new SQLException(sql_create_dbid, sqle);
        }
    }

    // Find_SRID replaced ST_Find_SRID in PostGIS 1.4
    // @TODO for backward compatibility it could be useful to implement a 1.3 compatibility mode
    protected int getTableSRID(java.sql.Connection conn, String column) throws SQLException {
        String sql = schemaName == null ?
                "SELECT srid FROM geometry_columns WHERE f_table_schema = 'public' AND f_table_name = '" + tableName + "'" :
                "SELECT srid FROM geometry_columns WHERE f_table_schema = '" + schemaName + "' AND f_table_name = '" + tableName + "'";
        Logger.debug(sql);
        ResultSet rs = conn.prepareStatement(sql).executeQuery();
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }
}
