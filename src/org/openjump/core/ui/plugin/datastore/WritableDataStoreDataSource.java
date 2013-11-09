package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.postgis.PostgisDSConnection;
import com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;
import org.apache.log4j.Logger;
import org.openjump.core.ui.plugin.datastore.postgis.PostGISConnectionUtil;
import org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil;import org.openjump.core.ui.plugin.datastore.transaction.Evolution;
import org.openjump.core.ui.plugin.datastore.transaction.EvolutionOperationException;

import javax.swing.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil.*;

/**
 * Extension of DataBaseDataSource adding write capabilities.
 */
public abstract class WritableDataStoreDataSource extends DataStoreDataSource {

    private static final String KEY = WritableDataStoreDataSource.class.getName();
    Logger LOG = Logger.getLogger(WritableDataStoreDataSource.class);

    // Inherited from DataStoreDataSource (query)
    // DATASET_NAME_KEY
    // GEOMETRY_ATTRIBUTE_NAME_KEY
    // MAX_FEATURES_KEY
    // WHERE_CLAUSE_KEY
    // CACHING_KEY
    // CONNECTION_DESCRIPTOR_KEY

    // More query options (specific to WritableDataStoreSataSource) : don't translate, these are map keys
    public static final String LIMITED_TO_VIEW   = "Limited To View";
    public static final String MANAGE_CONFLICTS  = "Manage conflicts";

    // Update options (write to database) : don't translate, these are map keys
    public static final String EXTERNAL_PK_KEY   = "External PK";
    public static final String SRID_KEY          = "SRID";
    public static final String CREATE_TABLE      = "Create table";
    public static final String CREATE_PK         = "Create PK";

    final static public String DEFAULT_PK_NAME   = "dbid";

    // Ordered Map of evolutions
    // Map is indexed by FID in order to merge successive evolutions of a feature efficiently
    final private LinkedHashMap<Integer,Evolution> evolutions = new LinkedHashMap<Integer,Evolution>();


    public WritableDataStoreDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }

    /**
     * Constructor with mandatory parameters for a WritableDataStoreDataSource.
     * @param connectionDescriptor descriptor of the connection this datasource is connected to
     * @param datasetName dataset name
     * @param geometryAttributeName geometry attribute name
     * @param externalPKName database primary key used to manage feature updates
     */
    public WritableDataStoreDataSource(ConnectionDescriptor connectionDescriptor,
                                       String datasetName,
                                       String geometryAttributeName,
                                       String externalPKName) {
        setProperties(CollectionUtil.createMap(new Object[]{
                CONNECTION_DESCRIPTOR_KEY, connectionDescriptor,
                DATASET_NAME_KEY, datasetName,
                GEOMETRY_ATTRIBUTE_NAME_KEY, geometryAttributeName,
                EXTERNAL_PK_KEY, externalPKName,

        }));
        // default options
        getProperties().put(WHERE_CLAUSE_KEY, null);
        getProperties().put(MAX_FEATURES_KEY, Integer.MAX_VALUE);
        getProperties().put(LIMITED_TO_VIEW, false);
        getProperties().put(MANAGE_CONFLICTS, false);
        // This parameter must be set to true to create a new table from a layer
        getProperties().put(CREATE_TABLE, false);
        getProperties().put(CREATE_PK, false);
        getProperties().put(SRID_KEY, 0);
    }

    public void setLimitedToView(boolean limitedToView) {
        getProperties().put(LIMITED_TO_VIEW, limitedToView);
    }

    public void setManageConflicts(boolean manageConflicts) {
        getProperties().put(MANAGE_CONFLICTS, manageConflicts);
    }

    public void setSRID(int srid) {
        getProperties().put(SRID_KEY, srid);
    }

    public boolean isWritable() {
        return true;
    }

    public Connection getConnection() {

        return new Connection() {
            public FeatureCollection executeQuery(String query,
                                                  Collection exceptions, TaskMonitor monitor) {
                try {
                    // Must be implemented by subclasses
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

            public void executeUpdate(String query,
                        FeatureCollection featureCollection, TaskMonitor monitor) throws Exception {

                ConnectionDescriptor connectionDescriptor =
                        (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);
                String[] datasetName = PostGISQueryUtil.splitTableName((String)getProperties().get(DATASET_NAME_KEY));
                String quotedSchemaName = quote(datasetName[0]);
                String quotedTableName = quote(datasetName[1]);
                boolean createPrimaryKey = (Boolean)getProperties().get(WritableDataStoreDataSource.CREATE_PK);
                int srid = getProperties().get(SRID_KEY)==null ? 0 : (Integer)getProperties().get(SRID_KEY);
                int dim = getGeometryDimension(featureCollection, 3);

                PostgisDSConnection pgConnection =
                        (PostgisDSConnection)new PostgisDataStoreDriver()
                                .createConnection(connectionDescriptor.getParameterList());
                java.sql.Connection conn = pgConnection.getConnection();
                try {
                    conn.setAutoCommit(false);
                    if ((Boolean)getProperties().get(WritableDataStoreDataSource.CREATE_TABLE)) {
                        boolean exists = tableExists(conn, unquote(quotedSchemaName), unquote(quotedTableName));
                        if (exists && !confirmOverwrite()) return;
                        if (exists) {
                            deleteTableQuery(conn, quotedSchemaName, quotedTableName);
                        }
                        if (createPrimaryKey) {
                            featureCollection.getFeatureSchema().removeExternalPrimaryKey();
                        }
                        createAndPopulateTable(conn, featureCollection,
                                quotedSchemaName, quotedTableName, srid, "GEOMETRY", dim);
                        if (createPrimaryKey) {
                            addDBPrimaryKey(conn, quotedSchemaName, quotedTableName, DEFAULT_PK_NAME);
                            // @TODO reload part is kept out of the transaction because it uses
                            // PostGISFeatureInputStream which init() function contains
                            // rs = stmt.executeQuery(parsedQuery);
                            // This instruction is not compatible with the transaction mode
                            // ==> If PostGISFeatureInputStream#init() is made transactionnal
                            // we must check that all calling methods do commit it.
                            conn.commit();
                            reloadDataFromDataStore(this, monitor);
                        }
                    }
                    else {
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        PostGISConnectionUtil connUtil = new PostGISConnectionUtil(conn);
                        if (connUtil.compatibleSchemaSubset(quotedSchemaName, quotedTableName, featureSchema)
                                .length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        commit(conn, quotedSchemaName, quotedTableName,
                                (String)getProperties().get(EXTERNAL_PK_KEY), srid, dim);
                        evolutions.clear();
                    }
                    conn.commit();
                }
                finally {
                    if (conn != null) conn.setAutoCommit(true);
                }
                // Adding vacuum analyze seems to be necessary to be able to use
                // ST_Estimated_Extent on the newly created table
                finalizeUpdate(conn, quotedSchemaName, quotedTableName);
            }

            public void close() {
                // Do nothing, because DataStore connections are always
                // open (managed by the ConnectionManager). [Jon Aquino
                // 2005-03-16]
            }
        };
    }

    /**
     * With some databases, it may be useful to do some cleaning after a big update.
     * Example : perform a vacuum analyze in PostgreSQL to compact database and to
     * update statistics (needed by ST_Estimated_Extent function)
     */
    public abstract void finalizeUpdate(java.sql.Connection conn,
            String quotedSchemaName, String quotedTableName) throws Exception;

    /*
    protected FeatureCollection createFeatureCollection() throws Exception {

        ConnectionDescriptor connectionDescriptor =
                (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);

        // Schema name, table name and geometry column name are needed
        // to get the database srid associated to this FeatureCollection
        String[] fullName = PostGISQueryUtil.splitTableName((String)getProperties().get(DATASET_NAME_KEY));
        String schemaName = fullName[0];
        String tableName = fullName[1];
        String geometryColumn = (String)getProperties().get(GEOMETRY_ATTRIBUTE_NAME_KEY);

        PostgisDSConnection pgConnection =
                (PostgisDSConnection)new PostgisDataStoreDriver()
                        .createConnection(connectionDescriptor.getParameterList());
        java.sql.Connection conn = pgConnection.getConnection();
        int table_srid = getTableSRID(conn, schemaName, tableName, geometryColumn);

        // Set the query string, including extension restriction if needed
        boolean limited_to_view = (Boolean)getProperties().get(LIMITED_TO_VIEW);
        String extent = limited_to_view ? " AND (\"" + geometryColumn + "\" && ST_GeomFromText('" +
                getViewEnvelope().toText() + "'," + table_srid + "))" : "";
        String whereClause = (String)getProperties().get(WHERE_CLAUSE_KEY);
        whereClause = (whereClause == null || whereClause.length() == 0) ? "true" : "(" + whereClause + ")";
        String query = "SELECT * FROM \"" + unquote((String)getProperties().get(DATASET_NAME_KEY)) + "\" WHERE " + whereClause + extent + ";";
        LOG.debug(query);

        AdhocQuery adhocQuery = new AdhocQuery(query);
        String PK = (String)getProperties().get(EXTERNAL_ID_KEY);
        if (getProperties().get(EXTERNAL_ID_KEY) != null) {
            adhocQuery.setPrimaryKey(PK);
        }

        FeatureInputStream featureInputStream = null;
        FeatureDataset featureDataset = null;
        try {
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
            context.getWorkbench().getFrame().handleThrowable(e);
            featureDataset = new FeatureDataset(new FeatureSchema());
        }
        finally {
            if (featureInputStream != null) {
                try {featureInputStream.close();}
                catch(Exception e){}
            }
        }
        return featureDataset;

    }
    */

    private void commit(java.sql.Connection conn, String dbSchema, String dbTable,
                        String keyName, int srid, int dim) throws Exception {

        LOG.info("Evolutions to commit to " + dbSchema + "." + dbTable + " (PK=" + keyName +")");
        for (Evolution evolution : evolutions.values()) {
            if (evolution.getType() == Evolution.Type.CREATION) {
                insertStatement(conn, evolution.getNewFeature(), dbSchema, dbTable, keyName, srid, dim).executeUpdate();
                LOG.info("  create new feature " + evolution.getNewFeature().getID()+"/");
            } else if (evolution.getType() == Evolution.Type.SUPPRESSION) {
                deleteStatement(conn, evolution.getOldFeature(), keyName, dbSchema, dbTable, srid, dim).executeUpdate();
                LOG.info("  delete " + evolution.getOldFeature().getID() + "/" + evolution.getOldFeature().getAttribute(keyName));
            } else if (evolution.getType() == Evolution.Type.MODIFICATION) {
                Feature oldFeature = evolution.getOldFeature();
                Feature newFeature = evolution.getNewFeature();
                FeatureSchema schema = oldFeature.getSchema();
                // Attribute changes are updated individually, avoiding to replace
                // values changed concurrently by another client if it is not needed
                for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                    if (oldFeature.getAttribute(i) == null && newFeature.getAttribute(i) != null ||
                        oldFeature.getAttribute(i) != null && newFeature.getAttribute(i) == null ||
                        oldFeature.getAttribute(i) != null && !oldFeature.getAttribute(i).equals(newFeature.getAttribute(i))) {
                        updateOneAttributeStatement(conn, newFeature, i, keyName, dbSchema, dbTable, srid, dim).executeUpdate();
                    }
                }
                LOG.info("  modify " + evolution.getNewFeature().getID() + "/" + evolution.getNewFeature().getAttribute(keyName));
            }
        }

        evolutions.clear();
    }

    protected PreparedStatement insertStatement(java.sql.Connection conn, Feature feature,
                                                String dbSchema, String dbTable, String keyName,
                                                int srid, int dim) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        FeatureSchema schema = feature.getSchema();
        StringBuilder sb = new StringBuilder("INSERT INTO " + tableName + "(");

        sb.append(PostGISQueryUtil.createColumnList(schema, false, true, false)).append(") VALUES(");
        int nbValues = schema.getAttributeCount();
        if (keyName != null && schema.hasAttribute(keyName)) nbValues --;
        for (int i = 0 ; i < nbValues ; i++) {
            sb.append(i==0?"?":",?");
        }
        sb.append(");");
        PreparedStatement pstmt = conn.prepareStatement(sb.toString());
        pstmt = setAttributeValues(pstmt, feature, srid, dim, keyName);
        LOG.debug(pstmt);
        return pstmt;
    }

    /*
    private PreparedStatement updateStatement(java.sql.Connection conn, Feature feature, String keyName,
                                              String dbSchema, String dbTable, boolean hasSrid, int dim) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        FeatureSchema schema = feature.getSchema();
        boolean quoted = schema.getAttributeType(schema.getExternalPrimaryKeyIndex()) == AttributeType.STRING;
        String quote = quoted ? "'" : "";
        StringBuilder sb = new StringBuilder("UPDATE " + tableName + " SET (");
        //sb.append(PostGISQueryUtil.createColumnList(schema, keyName)).append(") = (");
        sb.append(PostGISQueryUtil.createColumnList(schema, false, true, false)).append(") = (");
        for (int i = 0 ; i < schema.getAttributeCount()-1 ; i++) {
            sb.append(i==0?"?":",?");
        }
        sb.append(") WHERE \"").append(keyName).append("\" = ").append(quote).append(feature.getAttribute(keyName)).append(quote).append(";");
        String query = sb.toString();
        //LOG.debug(query);
        PreparedStatement pstmt = conn.prepareStatement(query);
        return setAttributeValues(pstmt, feature, hasSrid, dim, keyName);
    }
    */

    private PreparedStatement updateOneAttributeStatement(java.sql.Connection conn, Feature feature, int attribute,
                String keyName, String dbSchema, String dbTable, int srid, int dim) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        FeatureSchema schema = feature.getSchema();
        boolean quoted = schema.getAttributeType(schema.getExternalPrimaryKeyIndex()) == AttributeType.STRING;
        String quoteKey = quoted ? "'" : "";

        StringBuilder sb = new StringBuilder("UPDATE ").append(tableName)
                .append(" SET \"").append(schema.getAttributeName(attribute)).append("\" = ?")
                .append(" WHERE \"").append(keyName).append("\" = ").append(quoteKey).append(feature.getAttribute(keyName)).append(quoteKey).append(";");
        PreparedStatement pstmt = conn.prepareStatement(sb.toString());
        AttributeType type = schema.getAttributeType(attribute);
        if (feature.getAttribute(attribute) == null) pstmt.setObject(1, null);
        else if (type == AttributeType.STRING)   pstmt.setString(1, feature.getString(attribute));
        else if (type == AttributeType.GEOMETRY) {
            pstmt.setBytes(1, PostGISQueryUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(attribute), srid, dim));
        }
        else if (type == AttributeType.INTEGER)  pstmt.setInt(1, feature.getInteger(attribute));
        else if (type == AttributeType.DOUBLE)   pstmt.setDouble(1, feature.getDouble(attribute));
        else if (type == AttributeType.DATE)     pstmt.setTimestamp(1, new Timestamp(((Date)feature.getAttribute(attribute)).getTime()));
        else if (type == AttributeType.OBJECT)   pstmt.setObject(1, feature.getAttribute(attribute));
        else throw new IllegalArgumentException(type + " is an unknown AttributeType !");
        LOG.debug(pstmt);
        return pstmt;
    }

    // @TODO Remove unused parameters srid and dim ?
    private PreparedStatement deleteStatement(java.sql.Connection conn, Feature feature, String keyName,
                                              String dbSchema, String dbTable, int srid, int dim) throws SQLException {
        String tableName = dbSchema == null ? dbTable : dbSchema + "." + dbTable;
        FeatureSchema schema = feature.getSchema();
        PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + tableName + " WHERE \"" + keyName + "\" = ?");
        pstmt.setObject(1,feature.getAttribute(keyName));
        LOG.debug(pstmt);
        return pstmt;
    }

    protected PreparedStatement setAttributeValues(PreparedStatement pstmt,
                                                 Feature feature, int srid, int dim, String...exclude) throws SQLException {
        FeatureSchema schema = feature.getSchema();
        List<String> excludeList = Arrays.asList(exclude);
        int index = 1;
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            AttributeType type = schema.getAttributeType(i);
            if (excludeList.contains(schema.getAttributeName(i))) continue;
            if (schema.getExternalPrimaryKeyIndex() == i) pstmt.setObject(index++, null);
            if (feature.getAttribute(i) == null)     pstmt.setObject(index++, null);
            else if (type == AttributeType.STRING)   pstmt.setString(index++, feature.getString(i));
            else if (type == AttributeType.GEOMETRY) {
                pstmt.setBytes(index++, PostGISQueryUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(i), srid, dim));
            }
            else if (type == AttributeType.INTEGER)  pstmt.setInt(index++, feature.getInteger(i));
            else if (type == AttributeType.DOUBLE)   pstmt.setDouble(index++, feature.getDouble(i));
            else if (type == AttributeType.DATE)     pstmt.setTimestamp(index++, new Timestamp(((Date)feature.getAttribute(i)).getTime()));
            else if (type == AttributeType.OBJECT)   {
                if (feature.getAttribute(i) instanceof Geometry) {
                    // In our use case, other geometry attributes use the same srid as the main geometry
                    // but always have dimension = 2. This use case does not fit all !
                    pstmt.setBytes(index++, PostGISQueryUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(i), false, 2));
                }
                else pstmt.setObject(index++, feature.getAttribute(i));
            }
            else throw new IllegalArgumentException(type + " is an unknown AttributeType !");
        }
        return pstmt;
    }

    protected abstract int getTableSRID(java.sql.Connection conn,
                                        String dbSchema, String dbTable, String column) throws SQLException;

    protected Geometry getViewEnvelope() throws Exception {
        return new GeometryFactory().toGeometry(
                JUMPWorkbench.getInstance().getFrame().getActiveTaskFrame()
                        .getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates()
        );
    }

    private boolean confirmWriteDespiteDifferentSchemas() {
        JOptionPane.showMessageDialog(
                getWorkbenchContext().getWorkbench().getFrame(),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.schema-mismatch-dialog-message"),
                I18N.get("org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource.schema-mismatch-dialog-title"),
                JOptionPane.ERROR_MESSAGE);
        return false;
    }

    public void addCreation(Feature feature) throws EvolutionOperationException {
        Evolution oldEvo = evolutions.remove(feature.getID());
        Evolution newEvo = Evolution.createCreation(feature.clone(true, false)).mergeToPrevious(oldEvo);
        if (newEvo != null) evolutions.put(feature.getID(), newEvo);
    }

    public void addModification(Feature feature, Feature oldFeature) throws EvolutionOperationException  {
        Evolution oldEvo = evolutions.remove(feature.getID());
        Evolution newEvo = Evolution.createModification(feature.clone(true, true), oldFeature.clone(true, true)).mergeToPrevious(oldEvo);
        if (newEvo != null) evolutions.put(feature.getID(), newEvo);
    }

    public void addSuppression(Feature feature) throws EvolutionOperationException  {
        Evolution oldEvo = evolutions.remove(feature.getID());
        Evolution newEvo = Evolution.createSuppression(feature.clone(true, true)).mergeToPrevious(oldEvo);
        if (newEvo != null) evolutions.put(feature.getID(), newEvo);
    }

    /**
     * Remove the evolution currently recorded for feature fid.
     * To be used cautiously : this method is used by DataStoreTransactionManager to remove
     * an evolution when the newFeature of this evolution happens to be the same as the last
     * version updated from the server (false conflict).
     * @param fid
     */
    public void removeEvolution(int fid) {
        evolutions.remove(fid);
    }

    public Collection<Evolution> getUncommittedEvolutions() {
        return evolutions.values();
    }


    /**
     * Return a map with modified features indexed by their database id.
     * WARNING : New features are excluded from this map.
     * @return
     */
    public Map<Object,Evolution> getIndexedEvolutions() {
        Map<Object,Evolution> index = new TreeMap<Object,Evolution>();
        for (Evolution evolution : evolutions.values()) {
            Evolution.Type type = evolution.getType();
            if (type == Evolution.Type.MODIFICATION || type == Evolution.Type.SUPPRESSION) {
                Object dbid = evolution.getOldFeature().getAttribute(getProperties().get(EXTERNAL_PK_KEY).toString());
                if (dbid != null) {
                    index.put(dbid, evolution);
                }
            }
        }
        return index;
    }

    private boolean confirmOverwrite() {
        // This is a strange place to set WorkbenchContext, but it has not yet been set...
        setWorkbenchContext(JUMPWorkbench.getInstance().getContext());
        int opt = JOptionPane.showConfirmDialog(
                getWorkbenchContext().getWorkbench().getFrame(),
                I18N.get(KEY + ".overwrite-dialog-message"),
                I18N.get(KEY + ".overwrite-dialog-title"),
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
    abstract protected void deleteTableQuery(java.sql.Connection connection,
                                    String dbSchema, String dbTable) throws SQLException;

    abstract protected void createAndPopulateTable(
            java.sql.Connection conn,
            FeatureCollection fc,
            String dbSchema, String dbTable,
            int srid, String geometryType, int dim) throws SQLException;


    /**
     * Add an automatically named primary key constraint to the table.
     */
    protected abstract void addDBPrimaryKey(java.sql.Connection conn, String dbSchema,
                                 String dbTable, String primaryKey) throws SQLException;

    private void reloadDataFromDataStore(Connection conn, TaskMonitor monitor) throws Exception {
        Layer[] selectedLayers = JUMPWorkbench.getInstance().getContext().getLayerNamePanel().getSelectedLayers();
        if (selectedLayers != null && selectedLayers.length == 1) {
            selectedLayers[0].setFeatureCollection(conn.executeQuery(null, monitor));
        }
    }

}
