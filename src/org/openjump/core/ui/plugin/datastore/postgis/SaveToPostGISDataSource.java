package org.openjump.core.ui.plugin.datastore.postgis;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.postgis.PostgisDSConnection;
import com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;

import javax.swing.*;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.*;

/**
 * Add Write capabilities to DataStoreQueryDataSource for PostGIS table.
 * <p>There is now a more poweful way to connect to a postgis table with
 * write access. See {@link org.openjump.core.ui.plugin.datastore.postgis2.PostGISDataStoreDataSource}
 * and {@link org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource}</p>
 */
public class SaveToPostGISDataSource extends DataStoreQueryDataSource {
    
    // Do not translate, these keys are used to persist information in a map
    //public static final String GEOMETRY_ATTRIBUTE_NAME_KEY = "Geometry Attribute Name";
    //public static final String MAX_FEATURES_KEY   = "Max Features";
    //public static final String WHERE_CLAUSE_KEY   = "Where Clause";
    //public static final String CACHING_KEY        = "Caching";

    // UPDATE
    public static final String TABLE_KEY           = "Table";
    
    public static final String SAVE_METHOD_KEY     = "Save method";
    public static final String SAVE_METHOD_CREATE  = "Create";
    public static final String SAVE_METHOD_REPLACE = "Replace";
    public static final String SAVE_METHOD_INSERT  = "Insert";
    public static final String SAVE_METHOD_UPDATE  = "Update";
    public static final String SAVE_METHOD_DELETE  = "Delete";
    public static final String PRIMARY_KEY         = "Primary Key";
    public static final String USE_DB_PRIMARY_KEY  = "Use DB Primary Key";
    
    public static final String SRID_KEY            = "SRID";

    private static final String DEFAULT_PK_NAME    = "dbid";


    public SaveToPostGISDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }
    
    /** Constructor used by the SaveToPostGISPlugIn.*/
    public SaveToPostGISDataSource(WorkbenchContext context) {
        super(context);
    }
    
    public SaveToPostGISDataSource(String tableName,
                                   String geometryColumnName,
                                   ConnectionDescriptor connectionDescriptor,
                                   WorkbenchContext context) {
        setProperties(CollectionUtil.createMap(new Object[] {
            TABLE_KEY, tableName,
            CONNECTION_DESCRIPTOR_KEY, connectionDescriptor, 
        }));
        setWorkbenchContext(context);
    }


    public boolean isWritable() {
        return true;
    }

    
    public Connection getConnection() {
        
        return new Connection() {
            
            public FeatureCollection executeQuery(String query,
                    Collection exceptions, TaskMonitor monitor) {
                try {
                    return createFeatureCollection();
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                }
            }

            public FeatureCollection executeQuery(String query,
                    TaskMonitor monitor) throws Exception {
                Collection exceptions = new ArrayList();
                FeatureCollection featureCollection = executeQuery(query,
                        exceptions, monitor);
                if (!exceptions.isEmpty()) {
                    throw (Exception) exceptions.iterator().next();
                }
                return featureCollection;
            }
            

            // Main method doing the job of updating a PostGIS table
            public void executeUpdate(String query,
                    FeatureCollection featureCollection, TaskMonitor monitor)
                    throws Exception {
                
                // Get the write method (currently update or create table)
                String method = (String)getProperties().get(SAVE_METHOD_KEY);
                // Get the connection descriptor
                ConnectionDescriptor connectionDescriptor = 
                    (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);
                
                // Check that Connection descriptor connects to a PostGIS database
                // @TODO the connection panel should show only PostGIS connection
                // Error message move to PostGISSaveDataSourceQueryChooser.isInputValid
                // if (!connectionDescriptor.getDataStoreDriverClassName().equals("com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver")) {
                //     JOptionPane.showMessageDialog(null,
                //         "The selected Connection is not a PostGIS connection!",
                //         "Error!", JOptionPane.ERROR_MESSAGE );
                //     return;
                // }

                // Get schema and table names
                String table = (String)getProperties().get(TABLE_KEY);
                String[] dbSchemaTable = PostGISQueryUtil.splitTableName(table);
                String schemaName = unquote(dbSchemaTable[0]);
                String tableName = unquote(dbSchemaTable[1]);
                boolean normalizedColumnNames = false;

                String primary_key = (String)getProperties().get(PRIMARY_KEY);
                boolean createPrimaryKey = (Boolean)getProperties().get(USE_DB_PRIMARY_KEY);
                //boolean use_db_id_key = (Boolean)getProperties().get(USE_DB_ID_KEY);
                // In PostGIS 2.x, default SRID has changed to 0, but we don't mind,
                // AddGeometryColumn automatically change -1 to 0.
                int srid = getProperties().get(SRID_KEY)==null ? -1 : (Integer)getProperties().get(SRID_KEY);
                // default dim value (may be overloaded by a value read from the database)
                int dim = getGeometryDimension(featureCollection, 3);

                PostgisDSConnection pgConnection = 
                        (PostgisDSConnection)new PostgisDataStoreDriver()
                        .createConnection(connectionDescriptor.getParameterList());
                java.sql.Connection conn = pgConnection.getConnection();
                PostGISConnectionUtil connUtil = new PostGISConnectionUtil(conn);

                // For update operations, use the dimension defined in geometry_column if any
                if (!method.equals(SAVE_METHOD_CREATE)) {
                    dim = connUtil.getGeometryDimension(schemaName, tableName, dim);
                }

                if (method.equals(SAVE_METHOD_CREATE)) {
                    boolean exists = tableExists(conn, schemaName, tableName);
                    if (exists && !confirmOverwrite()) return;
                    try {
                        conn.setAutoCommit(false);
                        if (exists) {
                            deleteTableQuery(conn, schemaName, tableName);
                        }
                        createAndPopulateTable(conn, featureCollection, schemaName,
                                tableName, srid, "geometry", dim, normalizedColumnNames);
                        if (createPrimaryKey) {
                            addDBPrimaryKey(conn, schemaName, tableName, "dbid");
                        }
                        conn.commit();
                        conn.setAutoCommit(true);
                        if (createPrimaryKey) {
                            reloadDataFromDataStore(this, connectionDescriptor, schemaName, tableName, DEFAULT_PK_NAME, monitor);
                        }
                        // Adding vacuum analyze seems to be necessary to be able to use 
                        // ST_Estimated_Extent on the newly created table
                        conn.createStatement().execute("VACUUM ANALYZE " + 
                            PostGISQueryUtil.compose(schemaName, tableName));
                    } catch(SQLException e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_REPLACE)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (connUtil.compatibleSchemaSubset(schemaName, tableName, featureSchema, normalizedColumnNames).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        truncateTable(conn, schemaName, tableName);
                        insertInTable(conn, featureCollection, schemaName, tableName, primary_key,
                                srid>0, dim, normalizedColumnNames);
                        conn.commit();
                        conn.setAutoCommit(true);
                        if (featureSchema.getExternalPrimaryKeyIndex() > -1) {
                            reloadDataFromDataStore(this, connectionDescriptor, schemaName, tableName, DEFAULT_PK_NAME, monitor);
                        }
                    } catch(Exception e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_INSERT)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (primary_key != null) {
                            featureSchema.setExternalPrimaryKeyIndex(featureSchema.getAttributeIndex(primary_key));
                        }
                        if (connUtil.compatibleSchemaSubset(schemaName, tableName, featureSchema, normalizedColumnNames).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertInTable(conn, featureCollection, schemaName, tableName, primary_key,
                                srid>0, dim, normalizedColumnNames);
                        conn.commit();
                        conn.setAutoCommit(true);
                        if (featureSchema.getExternalPrimaryKeyIndex() > -1) {
                            reloadDataFromDataStore(this, connectionDescriptor, schemaName, tableName, DEFAULT_PK_NAME, monitor);
                        }
                    } catch(Exception e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_UPDATE)) {
                    try {
                        // Makes delete previous table and create new table atomic
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (primary_key != null) {
                            featureSchema.setExternalPrimaryKeyIndex(featureSchema.getAttributeIndex(primary_key));
                        }
                        if (connUtil.compatibleSchemaSubset(schemaName, tableName, featureSchema, normalizedColumnNames).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertUpdateTable(conn, featureCollection, schemaName, tableName, primary_key,
                                srid>0, dim, normalizedColumnNames);
                        conn.commit();
                        conn.setAutoCommit(true);
                        if (featureSchema.getExternalPrimaryKeyIndex() > -1) {
                            reloadDataFromDataStore(this, connectionDescriptor, schemaName, tableName, DEFAULT_PK_NAME, monitor);
                        }
                    } catch(SQLException e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_DELETE)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (connUtil.compatibleSchemaSubset(schemaName, tableName, featureSchema, normalizedColumnNames).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        deleteNotExistingFeaturesFromTable(conn, featureCollection, schemaName, tableName, primary_key);
                        insertUpdateTable(conn, featureCollection, schemaName, tableName, primary_key,
                                srid > 0, dim, normalizedColumnNames);
                        conn.commit();
                    } catch(SQLException e) {
                        throw e;
                    }
                }

            }

            public void close() {
                // Do nothing, because DataStore connections are always
                // open (managed by the ConnectionManager). [Jon Aquino
                // 2005-03-16]
            }
        };
    }
    
    private boolean confirmOverwrite() {
        int opt = JOptionPane.showConfirmDialog(
                getWorkbenchContext().getWorkbench().getFrame(),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.overwrite-dialog-message"),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.overwrite-dialog-title"),
                JOptionPane.YES_NO_OPTION);
        return (opt != JOptionPane.NO_OPTION);
    }
    
    private boolean confirmWriteDespiteDifferentSchemas() {
        int opt = JOptionPane.showConfirmDialog(
                getWorkbenchContext().getWorkbench().getFrame(),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.schema-mismatch-dialog-message"),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.schema-mismatch-dialog-title"),
                JOptionPane.YES_NO_OPTION);                        
        return (opt != JOptionPane.NO_OPTION);
    }

    /**
     * Check if this [schema.]table exists in this database.
     */
    private boolean tableExists(java.sql.Connection connection, String dbSchema, String dbTable) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata.getTables(null, dbSchema, dbTable, new String[]{"TABLE"}).next();
    }
    
    
    /** 
     * Execute a query against this connection to delete the reference to this
     * table in the PostGIS's geometry_columns table.
     * @schemaName unquoted schema name or null to use default schema
     * @tableName unquoted table name
     */
    private void deleteTableQuery(java.sql.Connection connection, 
                          String schemaName, String tableName) throws SQLException {
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


    private void truncateTable(java.sql.Connection conn, String schemaName, String tableName) throws SQLException {
        String tableQName = compose(schemaName, tableName);
        try {
            conn.createStatement().execute("TRUNCATE TABLE " + tableQName);
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: TRUNCATE TABLE " + tableQName, sqle);
        }
    }
    
    
    private void createAndPopulateTable(
                java.sql.Connection conn, 
                FeatureCollection fc, 
                String schemaName, String tableName,
                int srid, String geometryType, int dim,
                boolean normalizeColumnNames) throws SQLException {
        FeatureSchema schema = fc.getFeatureSchema();
        String geometryColumn = schema.getAttributeName(schema.getGeometryIndex());
        try {
            conn.createStatement()
                .execute(PostGISQueryUtil
                .getCreateTableStatement(fc.getFeatureSchema(), schemaName, tableName, false));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " +
                    PostGISQueryUtil.getCreateTableStatement(fc.getFeatureSchema(), schemaName, tableName, false), sqle);
        }
        try {
            conn.createStatement().execute(PostGISQueryUtil.getAddGeometryColumnStatement(schemaName, tableName, geometryColumn, srid, geometryType, dim));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil.getAddGeometryColumnStatement(schemaName, tableName, geometryColumn, srid, geometryType, dim), sqle);
        }
        populateTable(conn, fc, schemaName, tableName, null, (srid>0), dim, normalizeColumnNames);
        try {
            conn.createStatement().execute(PostGISQueryUtil.getAddSpatialIndexStatement(schemaName, tableName, geometryColumn));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil.getAddSpatialIndexStatement(schemaName, tableName, geometryColumn), sqle);
        }
    }


    /**
     * Add an automatically named primary key constraint to the table.
     */
    private void addDBPrimaryKey(java.sql.Connection conn, String dbSchema,
                               String dbTable, String primaryKey) throws SQLException {
        String tableFullName = compose(dbSchema,dbTable);
        String sql_test_seq = "SELECT * FROM information_schema.sequences\n" +
                "    WHERE sequence_schema = 'public' AND sequence_name = 'openjump_dbid_sequence';";
        String sql_create_seq = "CREATE SEQUENCE openjump_dbid_sequence;";
        String sql_create_dbid = "ALTER TABLE " + tableFullName + " ADD COLUMN \"" +
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
    
    private void populateTable(java.sql.Connection conn, FeatureCollection fc, 
        String dbSchema, String dbTable, String primaryKey, boolean hasSrid, int dim, boolean normalizedColumnNames) throws SQLException {
        PreparedStatement statement = insertStatement(conn, fc.getFeatureSchema(),
                dbSchema, dbTable, primaryKey, hasSrid, dim, normalizedColumnNames);
        int count = 0;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            //insertStatement(conn, f, dbSchema, dbTable, primaryKey, hasSrid, dim).executeUpdate();
            statement = setAttributeValues(statement, f, primaryKey, hasSrid, dim);
            statement.addBatch();
            if (count++ % 10000 == 0) {
                statement.executeBatch();
                statement.clearBatch();
            }
        }
        statement.executeBatch();
        statement.clearBatch();
    }
    
    private void insertInTable(java.sql.Connection conn, FeatureCollection fc, 
            String schemaName, String tableName, String primaryKey,
            boolean hasSrid, int dim, boolean normalizeColumnNames) throws SQLException {
        PreparedStatement statement = insertStatement(conn, fc.getFeatureSchema(),
                schemaName, tableName, primaryKey, hasSrid, dim, normalizeColumnNames);
        int count = 0;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            statement = setAttributeValues(statement, f, primaryKey, hasSrid, dim);
            statement.addBatch();
            if (count++ % 10000 == 0) {
                statement.executeBatch();
                statement.clearBatch();
            }
        }
        statement.executeBatch();
        statement.clearBatch();
    }
    
    private void insertUpdateTable(java.sql.Connection conn, FeatureCollection fc, 
            String dbSchema, String dbTable, String primaryKey, boolean hasSrid, int dim,
            boolean normalizedColumnNames) throws Exception {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        PreparedStatement statementI = insertStatement(conn, fc.getFeatureSchema(),
                dbSchema, dbTable, primaryKey, hasSrid, dim, normalizedColumnNames);
        PreparedStatement statementU = updateStatement(conn, fc.getFeatureSchema(),
                dbSchema, dbTable, primaryKey, hasSrid, dim, normalizedColumnNames);
        int countInsert = 0;
        int countUpdate = 0;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            Object fid = f.getAttribute(primaryKey);
            String sFid = fid instanceof String ? "'" + fid + "'" : ""+fid;
            //if (fid == null) throw new Exception("Some features have null attribute " + key);
            //String qfid = (fid instanceof String)? "'"+fid+"'" : fid.toString();
            ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) AS count FROM " + tableName + " WHERE \"" + primaryKey + "\" = " + sFid);
            int count = (rs.next()) ? rs.getInt("count") : 0;
            if (count==0) {
                setAttributeValues(statementI, f, primaryKey, hasSrid, dim);
                statementI.addBatch();
                if (countInsert++ % 10000 == 0) {
                    statementI.executeBatch();
                    statementI.clearBatch();
                }
            }
            else {
                setAttributeValues(statementU, f, primaryKey, hasSrid, dim);
                setPrimaryKeyValue(statementU, f, primaryKey);
                statementU.addBatch();
                if (countUpdate++ % 10000 == 0) {
                    statementU.executeBatch();
                    statementU.clearBatch();
                }
            }
        }
        statementI.executeBatch();
        statementI.clearBatch();
        statementU.executeBatch();
        statementU.clearBatch();
    }
    
    /**
     * Delete table rows not found in fc FeatureCollection.
     * @schemaName unquoted schema name or null to use default schema
     * @tableName unquoted table name
     */
    private void deleteNotExistingFeaturesFromTable(java.sql.Connection conn, 
            FeatureCollection fc, String schemaName, String tableName, String primaryKey) throws SQLException {
        StringBuilder sb = new StringBuilder();
        boolean numeric = false;
        if (fc.getFeatureSchema().getAttributeType(primaryKey) == AttributeType.INTEGER) numeric = true;
        // WARNING : OBJECT is not necessarilly of type Long
        else if (fc.getFeatureSchema().getAttributeType(primaryKey) == AttributeType.OBJECT) {
            if (fc.size()>0) {
                for (Object f : fc.getFeatures()) {
                    Object o = ((Feature)f).getAttribute(primaryKey);
                    if (o == null) continue;
                    if (o instanceof Integer || o instanceof Long || o instanceof BigInteger) {
                        numeric = true;
                        break;
                    }
                }
            }
        }
        boolean first = true;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Object pk = ((Feature)it.next()).getAttribute(primaryKey);
            if (pk != null) {
                if (!first) sb.append(",");
                if (numeric) sb.append(pk);
                else sb.append("'").append(pk).append("'");
                first = false;
            }
        }
        try {
            conn.createStatement().execute(
                    "DELETE FROM " + compose(schemaName, tableName) +
                    " WHERE \"" + primaryKey + "\" not in (" + sb.toString() + ")");
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + 
                "DELETE FROM " + compose(schemaName, tableName) +
                    " WHERE \"" + primaryKey + "\" not in (" + sb.toString() + ")", sqle);
        }
    }

    /** Prepare insert (without data) */
    private PreparedStatement insertStatement(java.sql.Connection conn,
            FeatureSchema schema, String schemaName, String tableName,
            String primaryKey, boolean hasSrid, int dim, boolean normalizedColumnNames) throws SQLException {
        String tableQName = compose(schemaName, tableName);
        StringBuilder sb = new StringBuilder("INSERT INTO " + tableQName + "(");
        sb.append(PostGISQueryUtil.createColumnList(schema, false, true, false, normalizedColumnNames))
          .append(") VALUES(");
        boolean first = true;
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            if (schema.getExternalPrimaryKeyIndex() == i) continue;
            sb.append(first?"?":",?");
            first = false;
        }
        sb.append(")");
        return conn.prepareStatement(sb.toString());
    }


    private PreparedStatement updateStatement(java.sql.Connection conn, FeatureSchema schema,
                String dbSchema, String dbTable, String primaryKey, boolean hasSrid, int dim,
                boolean normalizedColumnNames) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        //AttributeType type = schema.getAttributeType(primaryKey);
        //Object fid = feature.getAttribute(primaryKey);
        //String sFid = type == AttributeType.STRING ? "'" + fid + "'" : ""+fid;
        StringBuilder sb = new StringBuilder("UPDATE " + tableName + " SET (");
        sb.append(PostGISQueryUtil.createColumnList(schema, false, true, false, normalizedColumnNames))
          .append(") = (");
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            if (schema.getExternalPrimaryKeyIndex() == i) continue;
            sb.append(i==0?"?":",?");
        }
        sb.append(") WHERE \"").append(primaryKey).append("\" = ? ;");
        //PreparedStatement pstmt = conn.prepareStatement(sb.toString());
        return conn.prepareStatement(sb.toString());
        //return setAttributeValues(pstmt, feature, primaryKey, hasSrid, dim);
    }
    
    private PreparedStatement setAttributeValues(PreparedStatement pstmt, 
                Feature feature, String primaryKey, boolean hasSrid, int dim) throws SQLException {
        FeatureSchema schema = feature.getSchema();
        int shift = 1;
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            if (schema.getExternalPrimaryKeyIndex() == i) {
                shift--;
                continue;
            }
            AttributeType type = schema.getAttributeType(i);
            if (feature.getAttribute(i) == null)     pstmt.setObject(i+shift, null);
            else if (type == AttributeType.STRING)   pstmt.setString(i+shift, feature.getString(i));
            else if (type == AttributeType.GEOMETRY) pstmt.setBytes(i+shift, PostGISQueryUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(i), hasSrid, dim));
            else if (type == AttributeType.INTEGER)  pstmt.setInt(i+shift, feature.getInteger(i));
            else if (type == AttributeType.DOUBLE)   pstmt.setDouble(i+shift, feature.getDouble(i));
            else if (type == AttributeType.DATE)     pstmt.setTimestamp(i+shift, new Timestamp(((Date)feature.getAttribute(i)).getTime()));
            else if (type == AttributeType.OBJECT)   pstmt.setObject(i+shift, feature.getAttribute(i));
            else throw new IllegalArgumentException("" + type + " is an unknown AttributeType !");
        }
        return pstmt;
    }

    private PreparedStatement setPrimaryKeyValue(PreparedStatement pstmt, Feature feature, String primaryKey)
            throws SQLException {
        FeatureSchema schema = feature.getSchema();
        pstmt.setObject(schema.getAttributeCount(), feature.getAttribute(schema.getExternalPrimaryKeyIndex()));
        return pstmt;
    }
    
    private void reloadDataFromDataStore(Connection conn, ConnectionDescriptor connectionDescriptor,
                                         String schemaName, String tableName, String dbid,
                                         TaskMonitor monitor) throws Exception {
        String tableQName = compose(schemaName,tableName);
        setProperties(CollectionUtil.createMap(new Object[]{
                TABLE_KEY, tableQName,
                PRIMARY_KEY_KEY, dbid,
                SQL_QUERY_KEY, "SELECT * FROM " + tableQName,
                CONNECTION_DESCRIPTOR_KEY, connectionDescriptor,
        }));
        Layer[] selectedLayers = JUMPWorkbench.getInstance().getContext().getLayerNamePanel().getSelectedLayers();
        if (selectedLayers != null && selectedLayers.length == 1) {
            selectedLayers[0].setFeatureCollection(conn.executeQuery(null, monitor));
        }
    }

}