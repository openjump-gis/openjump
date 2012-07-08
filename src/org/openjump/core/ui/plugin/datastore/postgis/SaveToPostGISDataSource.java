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
import com.vividsolutions.jump.coordsys.CoordinateSystem;
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
import com.vividsolutions.jump.workbench.ui.plugin.datastore.PasswordPrompter;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISUtil.*;

/**
 * Implementation of DataSource interface to write/update a FeatureCollection
 * into a PostGIS table.
 */
public class SaveToPostGISDataSource extends com.vividsolutions.jump.io.datasource.DataSource implements
        WorkbenchContextReference {
    
    //public static final String DATASET_NAME_KEY = "Dataset Name";
    //public static final String GEOMETRY_COLUMN_KEY = "Geometry Column Name";
    
    // Do not translate, these are keys
    public static final String CONNECTION_DESCRIPTOR_KEY = "Connection Descriptor";
    public static final String TABLE_KEY = "Table";
    
    public static final String SAVE_METHOD_KEY = "Save method";
    public static final String SAVE_METHOD_CREATE = "Create";
    public static final String SAVE_METHOD_INSERT = "Insert";
    public static final String SAVE_METHOD_UPDATE = "Update";
    public static final String SAVE_METHOD_DELETE = "Delete";
    public static final String ID_COLUMN_KEY = "Unique column";

    private WorkbenchContext context;

    public SaveToPostGISDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }
    
    public SaveToPostGISDataSource(String tableName,
                                   String geometryColumnName, 
                                   ConnectionDescriptor connectionDescriptor, 
                                   WorkbenchContext context) {
        setProperties(CollectionUtil.createMap(new Object[] {
            TABLE_KEY, tableName,
            //GEOMETRY_COLUMN_KEY, geometryColumnName,
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
                throw new UnsupportedOperationException();
            }

            public FeatureCollection executeQuery(String query,
                    TaskMonitor monitor) throws Exception {
                throw new UnsupportedOperationException();
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
                String[] dbSchemaTable = PostGISUtil.divideTableName(table);
                String key = (String)getProperties().get(ID_COLUMN_KEY);
                String geometryType = "GEOMETRY";
                int srid = -1;
                int dim = 3;
                setFeatureCollectionProperties(featureCollection, srid, geometryType, dim);

                PostgisDSConnection pgConnection = 
                        (PostgisDSConnection)new PostgisDataStoreDriver()
                        .createConnection(connectionDescriptor.getParameterList());
                java.sql.Connection conn = pgConnection.getConnection();

                if (method.equals(SAVE_METHOD_CREATE)) {
                    boolean exists = tableExists(conn, dbSchemaTable[0], dbSchemaTable[1]);
                    if (exists && !confirmOverwrite()) return;
                    try {
                        conn.setAutoCommit(false);
                        if (exists) {
                            deleteTableQuery(conn, dbSchemaTable[0], dbSchemaTable[1]);
                        }
                        createAndPopulateTable(conn, featureCollection, 
                            dbSchemaTable[0], dbSchemaTable[1], srid, geometryType, dim);
                        if (key != null) addPrimaryKey(conn, dbSchemaTable[0], dbSchemaTable[1], key);
                        conn.commit();
                    } catch(SQLException e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_INSERT)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (PostGISUtil.compatibleSchemaSubset(conn, dbSchemaTable[0], dbSchemaTable[1], featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertInTable(conn, featureCollection, 
                            dbSchemaTable[0], dbSchemaTable[1], srid!=-1, dim);
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
                        if (PostGISUtil.compatibleSchemaSubset(conn, dbSchemaTable[0], dbSchemaTable[1], featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertUpdateTable(conn, featureCollection, dbSchemaTable[0], dbSchemaTable[1], key, srid!=-1, dim);
                        conn.commit();
                    } catch(SQLException e) {
                        throw e;
                    }
                }
                if (method.equals(SAVE_METHOD_DELETE)) {
                    try {
                        conn.setAutoCommit(false);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (PostGISUtil.compatibleSchemaSubset(conn, dbSchemaTable[0], dbSchemaTable[1], featureSchema).length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        insertUpdateTable(conn, featureCollection, dbSchemaTable[0], dbSchemaTable[1], key, srid!=-1, dim);
                        deleteNotExistingFeaturesFromTable(conn, featureCollection, dbSchemaTable[0], dbSchemaTable[1], key);
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
                context.getWorkbench().getFrame(),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.overwrite-dialog-message"),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.overwrite-dialog-title"),
                JOptionPane.YES_NO_OPTION);
        return (opt != JOptionPane.NO_OPTION);
    }
    
    private boolean confirmWriteDespiteDifferentSchemas() {
        int opt = JOptionPane.showConfirmDialog(
                context.getWorkbench().getFrame(),
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
        return metadata.getTables(null, unquote(dbSchema), unquote(dbTable), new String[]{"TABLE"}).next();
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
    
    
    private void createAndPopulateTable(
                java.sql.Connection conn, 
                FeatureCollection fc, 
                String dbSchema, String dbTable,
                int srid, String geometryType, int dim) throws SQLException {
        FeatureSchema schema = fc.getFeatureSchema();
        String geometryColumn = schema.getAttributeName(schema.getGeometryIndex());
        try {
            conn.createStatement().execute(PostGISUtil.getCreateTableStatement(fc.getFeatureSchema(), dbSchema, dbTable));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISUtil.getCreateTableStatement(fc.getFeatureSchema(), dbSchema, dbTable), sqle);
        }
        try {
            conn.createStatement().execute(PostGISUtil.getAddGeometryColumnStatement(dbSchema, dbTable, geometryColumn, srid, geometryType, dim));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISUtil.getAddGeometryColumnStatement(dbSchema, dbTable, geometryColumn, srid, geometryType, dim), sqle);
        }
        populateTable(conn, fc, dbSchema, dbTable, srid!=-1, dim);
        try {
            conn.createStatement().execute(PostGISUtil.getAddSpatialIndexStatement(dbSchema, dbTable, geometryColumn));
        } catch (SQLException sqle) {
            throw new SQLException("Error executing query: " + PostGISUtil.getAddSpatialIndexStatement(dbSchema, dbTable, geometryColumn), sqle);
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
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            sb.append(((Feature)it.next()).getString(key));
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
        sb.append(PostGISUtil.createColumnList(schema, false, true)).append(") VALUES(");
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
        sb.append(PostGISUtil.createColumnList(schema, false, true)).append(") = (");
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            sb.append(i==0?"?":",?");
        }
        sb.append(") WHERE \"" + key + "\" = " + fid + ";");
        //System.out.println(sb.toString());
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
            else if (type == AttributeType.INTEGER)  pstmt.setInt(i+1, feature.getInteger(i));
            else if (type == AttributeType.DOUBLE)   pstmt.setDouble(i+1, feature.getDouble(i));
            else if (type == AttributeType.DATE)     pstmt.setTimestamp(i+1, new Timestamp(((Date)feature.getAttribute(i)).getTime()));
            else if (type == AttributeType.GEOMETRY) pstmt.setBytes(i+1, PostGISUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(i), hasSrid, dim));
            else if (type == AttributeType.OBJECT)   pstmt.setObject(i+1, feature.getAttribute(i));
            else throw new IllegalArgumentException("" + type + " is an unknown AttributeType !");
        }
        return pstmt;
    }
    
    
    private void setFeatureCollectionProperties(FeatureCollection featureCollection, int srid, String geometryType, int dim) {
        CoordinateSystem cs = featureCollection.getFeatureSchema().getCoordinateSystem();
        if (cs != null) {
            try {srid = cs.getEPSGCode();}
            // if no srid defined, default posgis -1 code is used
            catch(UnsupportedOperationException e) {}
        }
        if (featureCollection.size() > 0) {
            Feature f = (Feature)featureCollection.iterator().next();
            String firstGeometryType = f.getGeometry().getGeometryType();
            boolean homogeneous = true;
            int d = 2;
            for (Iterator it = featureCollection.iterator() ; it.hasNext() ; ) {
                f = (Feature)it.next();
                if (homogeneous && !f.getGeometry().getGeometryType().equals(firstGeometryType)) {
                    homogeneous = false;
                }
                if (PostGISUtil.getGeometryDimension(f.getGeometry()) == 3) d = 3;
                if (d==3 && !homogeneous) return;
            }
            if (homogeneous) geometryType = firstGeometryType;
            if (d==2) dim = 2;
        }
    }
    
    //private int getDimension(Geometry g) {
    //    Coordinate[] cc = g.getCoordinates();
    //    int d = 2;
    //    for (Coordinate c : cc) {
    //        if (!Double.isNaN(c.z)) return 3;
    //    }
    //    return 2;
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
    
    public void setWorkbenchContext(WorkbenchContext context) {
        this.context = context;
        try {
            // This method is called by OpenProjectPlugIn in the
            // GUI thread, so now is a good time to prompt for
            // a password if necessary. [Jon Aquino 2005-03-16]
            if (ConnectionManager.instance(context) != null &&
                getProperties() != null &&
                getProperties().get(CONNECTION_DESCRIPTOR_KEY) != null) {
                new PasswordPrompter().getOpenConnection(
                    ConnectionManager.instance(context),
                    (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY), 
                    context.getWorkbench().getFrame()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public WorkbenchContext getWorkbenchContext() {
        return context;
    }

}