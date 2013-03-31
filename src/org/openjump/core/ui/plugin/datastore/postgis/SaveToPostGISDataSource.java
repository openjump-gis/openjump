package org.openjump.core.ui.plugin.datastore.postgis;

import java.lang.StringBuffer;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;                
import java.util.Map;

import javax.swing.JOptionPane;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver;
import com.vividsolutions.jump.datastore.postgis.PostgisDSConnection;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.cache.CachingFeatureCollection;
import com.vividsolutions.jump.workbench.model.cache.DynamicFeatureCollection;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.PasswordPrompter;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.*;

/**
 * Adds Write capabilities to DataStoreQueryDataSource for PostGIS table.
 */
public class SaveToPostGISDataSource extends DataStoreQueryDataSource {
    
    // Do not translate, these keys are used to persist information in a map
    //public static final String CONNECTION_DESCRIPTOR_KEY = "Connection Descriptor";
    
    // QUERY
    //public static final String DATASET_NAME_KEY   = "Dataset Name";
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
    public static final String LOCAL_ID_KEY        = "LOCAL_ID";
    public static final String NO_LOCAL_ID         = "NO_LOCAL_ID";
    public static final String USE_DB_ID_KEY       = "USE_DB_ID";
    
    public static final String SRID_KEY            = "SRID";


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
                String[] dbSchemaTable = PostGISQueryUtil.divideTableName(table);
                String quotedSchemaName = quote(unquote(dbSchemaTable[0]));
                String quotedTableName = quote(unquote(dbSchemaTable[1]));
                String local_id_key = (String)getProperties().get(LOCAL_ID_KEY);
                boolean use_db_id_key = (Boolean)getProperties().get(USE_DB_ID_KEY);
                // In PostGIS 2.x, default SRID has changed to 0, but don't mind,
                // AddGeometryColumn automatically change -1 to 0.
                int srid = getProperties().get(SRID_KEY)==null ? -1 : (Integer)getProperties().get(SRID_KEY);
                // finally, always create a new table with the Geometry Type
                //String geometryType = getGeometryType(featureCollection, "GEOMETRY");
                int dim = getGeometryDimension(featureCollection, 3);

                PostgisDSConnection pgConnection = 
                        (PostgisDSConnection)new PostgisDataStoreDriver()
                        .createConnection(connectionDescriptor.getParameterList());
                java.sql.Connection conn = pgConnection.getConnection();
                PostGISConnectionUtil connUtil = new PostGISConnectionUtil(conn);
                // For update operations, use the dimension defined in 
                // geometry_column if any
                if (!method.equals(SAVE_METHOD_CREATE)) {
                    dim = connUtil.getGeometryDimension(unquote(quotedSchemaName), unquote(quotedTableName), dim);
                }

                if (method.equals(SAVE_METHOD_CREATE)) {
                    boolean exists = tableExists(conn, unquote(quotedSchemaName), unquote(quotedTableName));
                    if (exists && !confirmOverwrite()) return;
                    try {
                        conn.setAutoCommit(false);
                        if (exists) {
                            deleteTableQuery(conn, quotedSchemaName, quotedTableName);
                        }
                        createAndPopulateTable(conn, featureCollection, 
                            quotedSchemaName, quotedTableName, srid, "GEOMETRY", dim);
                        if (local_id_key != null && !local_id_key.equals(NO_LOCAL_ID)) {
                            addPrimaryKey(conn, quotedSchemaName, quotedTableName, local_id_key);
                        }
                        conn.commit();
                        conn.setAutoCommit(true);
                        // Adding vacuum analyze seems to be necessary to be able to use 
                        // ST_Estimated_Extent on the newly created table
                        conn.createStatement().execute("VACUUM ANALYZE " + 
                            PostGISQueryUtil.compose(quotedSchemaName, quotedTableName));
                    } catch(SQLException e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_REPLACE)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (connUtil.compatibleSchemaSubset(quotedSchemaName, quotedTableName, featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        truncateTable(conn, quotedSchemaName, quotedTableName);
                        insertInTable(conn, featureCollection, 
                            quotedSchemaName, quotedTableName, srid>0, dim);
                        conn.commit();
                    } catch(Exception e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_INSERT)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (connUtil.compatibleSchemaSubset(quotedSchemaName, quotedTableName, featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertInTable(conn, featureCollection, 
                            quotedSchemaName, quotedTableName, srid>0, dim);
                        conn.commit();
                    } catch(Exception e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_UPDATE)) {
                    try {
                        // Makes delete previous table and create new table atomic
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (connUtil.compatibleSchemaSubset(quotedSchemaName, quotedTableName, featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertUpdateTable(conn, featureCollection, quotedSchemaName, quotedTableName, local_id_key, srid>0, dim);
                        conn.commit();
                    } catch(SQLException e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_DELETE)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (connUtil.compatibleSchemaSubset(quotedSchemaName, quotedTableName, featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertUpdateTable(conn, featureCollection, quotedSchemaName, quotedTableName, local_id_key, srid>0, dim);
                        deleteNotExistingFeaturesFromTable(conn, featureCollection, quotedSchemaName, quotedTableName, local_id_key);
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
     */
    private void deleteTableQuery(java.sql.Connection connection, 
                          String dbSchema, String dbTable) throws SQLException {
        try {
            // Try to delete table AND corresponding rows in geometry_columns table
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
    
    private void truncateTable(java.sql.Connection conn, String quotedSchemaName, String quotedTableName) throws SQLException {
        String name = (quotedSchemaName != null && quotedSchemaName.length() > 0) ? 
                quotedSchemaName + "." + quotedTableName : quotedTableName;
        try {
            conn.createStatement().execute("TRUNCATE TABLE " + name);
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: TRUNCATE TABLE " + name, sqle);
        }
    }
    
    
    private void createAndPopulateTable(
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
        populateTable(conn, fc, dbSchema, dbTable, (srid>0), dim);
        try {
            conn.createStatement().execute(PostGISQueryUtil.getAddSpatialIndexStatement(dbSchema, dbTable, geometryColumn));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISQueryUtil.getAddSpatialIndexStatement(dbSchema, dbTable, geometryColumn), sqle);
        }
    }
    
    /**
     * Add an automatically named primary key constraint to the table.
     */
    private void addPrimaryKey(java.sql.Connection conn, String dbSchema, 
                               String dbTable, String key) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        try {
            conn.createStatement().execute("ALTER TABLE " + tableName + " ADD PRIMARY KEY (\"" + key + "\");");
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + "ALTER TABLE " + tableName + " ADD PRIMARY KEY (\"" + key + "\");", sqle);
        }
    }
    
    private void populateTable(java.sql.Connection conn, FeatureCollection fc, 
        String dbSchema, String dbTable, boolean hasSrid, int dim) throws SQLException {
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            insertStatement(conn, f, dbSchema, dbTable, hasSrid, dim).executeUpdate();
        }
    }
    
    private void insertInTable(java.sql.Connection conn, FeatureCollection fc, 
            String dbSchema, String dbTable, boolean hasSrid, int dim) throws SQLException {
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            insertStatement(conn, f, dbSchema, dbTable, hasSrid, dim).executeUpdate();
        }
    }
    
    private void insertUpdateTable(java.sql.Connection conn, FeatureCollection fc, 
            String dbSchema, String dbTable, String key, boolean hasSrid, int dim) throws Exception {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            Object fid = f.getAttribute(key);
            if (fid == null) throw new Exception("Some features have null attribute " + key);
            String qfid = (fid instanceof String)? "'"+fid+"'" : fid.toString();
            ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) AS count FROM " + tableName + " WHERE \"" + key + "\" = " + qfid);
            int count = (rs.next()) ? rs.getInt("count") : 0;
            if (count==0) insertStatement(conn, f, dbSchema, dbTable, hasSrid, dim).executeUpdate();
            else updateStatement(conn, f, key, qfid, dbSchema, dbTable, hasSrid, dim).executeUpdate();
        }
    }
    
    /**
     * Delete table rows not found in fc FeatureCollection.
     */
    private void deleteNotExistingFeaturesFromTable(java.sql.Connection conn, 
            FeatureCollection fc, String dbSchema, String dbTable, String key) throws SQLException {
        StringBuffer sb = new StringBuffer();
        boolean numeric = 
            fc.getFeatureSchema().getAttributeType(key) == AttributeType.INTEGER ? true : false;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            if (numeric) sb.append(((Feature)it.next()).getString(key));
            else sb.append("'" + ((Feature)it.next()).getString(key) + "'");
            if (it.hasNext()) sb.append(",");
        }
        try {
            conn.createStatement().execute("DELETE FROM " + compose(dbSchema, dbTable) + " WHERE \"" + key + "\" not in (" + sb.toString() + ")");
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + 
                "DELETE FROM " + compose(dbSchema, dbTable) + " WHERE \"" + key + "\" not in (" + sb.toString() + ")", sqle);
        }
    }
    
    private PreparedStatement insertStatement(java.sql.Connection conn, 
                Feature feature, String dbSchema, String dbTable, boolean hasSrid, int dim) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        FeatureSchema schema = feature.getSchema();
        StringBuffer sb = new StringBuffer("INSERT INTO " + tableName + "(");
        sb.append(PostGISQueryUtil.createColumnList(schema, false, true)).append(") VALUES(");
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            sb.append(i==0?"?":",?");
        }
        sb.append(")");
        PreparedStatement pstmt = conn.prepareStatement(sb.toString());
        return setAttributeValues(pstmt, feature, hasSrid, dim);
    }
    
    private PreparedStatement updateStatement(java.sql.Connection conn, 
                Feature feature, String key, String fid, 
                String dbSchema, String dbTable, boolean hasSrid, int dim) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        FeatureSchema schema = feature.getSchema();
        StringBuffer sb = new StringBuffer("UPDATE " + tableName + " SET (");
        sb.append(PostGISQueryUtil.createColumnList(schema, false, true)).append(") = (");
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            sb.append(i==0?"?":",?");
        }
        sb.append(") WHERE \"" + key + "\" = " + fid + ";");
        PreparedStatement pstmt = conn.prepareStatement(sb.toString());
        return setAttributeValues(pstmt, feature, hasSrid, dim);
    }
    
    private PreparedStatement setAttributeValues(PreparedStatement pstmt, 
                Feature feature, boolean hasSrid, int dim) throws SQLException {
        FeatureSchema schema = feature.getSchema();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            AttributeType type = schema.getAttributeType(i);
            if (feature.getAttribute(i) == null)     pstmt.setObject(i+1, null);
            else if (type == AttributeType.STRING)   pstmt.setString(i+1, feature.getString(i));
            else if (type == AttributeType.GEOMETRY) pstmt.setBytes(i+1, PostGISQueryUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(i), hasSrid, dim));
            else if (type == AttributeType.INTEGER)  pstmt.setInt(i+1, feature.getInteger(i));
            else if (type == AttributeType.DOUBLE)   pstmt.setDouble(i+1, feature.getDouble(i));
            else if (type == AttributeType.DATE)     pstmt.setTimestamp(i+1, new Timestamp(((Date)feature.getAttribute(i)).getTime()));
            else if (type == AttributeType.OBJECT)   pstmt.setObject(i+1, feature.getAttribute(i));
            else throw new IllegalArgumentException("" + type + " is an unknown AttributeType !");
        }
        return pstmt;
    }
    
    

    
    //public WorkbenchContext getWorkbenchContext() {
    //    return context;
    //}
    
    //private FeatureCollection createFeatureCollection() {
    //    FilterQuery query = new FilterQuery();
    //    query.setDatasetName((String)getProperties().get(DATASET_NAME_KEY));
    //    query.setGeometryAttributeName((String)getProperties().get(
    //            GEOMETRY_ATTRIBUTE_NAME_KEY));
    //    if (((String)getProperties().get(WHERE_CLAUSE_KEY)).length() > 0) {
    //        query.setCondition((String) getProperties().get(WHERE_CLAUSE_KEY));
    //    }
    //    if (getProperties().get(MAX_FEATURES_KEY) != null) {
    //        query.setLimit((Integer)getProperties().get(MAX_FEATURES_KEY));
    //    }
    //    return new CachingFeatureCollection(new DynamicFeatureCollection(
    //            (ConnectionDescriptor) getProperties().get(
    //                    CONNECTION_DESCRIPTOR_KEY), ConnectionManager
    //                    .instance(context), query))
    //            .setCachingByEnvelope(((Boolean) LangUtil.ifNull(
    //                    getProperties().get(CACHING_KEY), Boolean.TRUE))
    //                    .booleanValue());
    //}
    //
    //public void setWorkbenchContext(WorkbenchContext context) {
    //    this.context = context;
    //    try {
    //        // This method is called by OpenProjectPlugIn in the
    //        // GUI thread, so now is a good time to prompt for
    //        // a password if necessary. [Jon Aquino 2005-03-16]
    //        if (ConnectionManager.instance(context) != null &&
    //            getProperties() != null &&
    //            getProperties().get(CONNECTION_DESCRIPTOR_KEY) != null) {
    //            new PasswordPrompter().getOpenConnection(
    //                ConnectionManager.instance(context),
    //                (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY), 
    //                context.getWorkbench().getFrame()
    //            );
    //        }
    //    } catch (Exception e) {
    //        throw new RuntimeException(e);
    //    }
    //}

}