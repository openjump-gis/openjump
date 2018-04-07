package org.openjump.core.ui.plugin.datastore;

import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.swing.JOptionPane;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager;
import org.openjump.core.ui.plugin.datastore.transaction.Evolution;
import org.openjump.core.ui.plugin.datastore.transaction.EvolutionOperationException;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;

/**
 * Extension of DataBaseDataSource adding write capabilities.
 */
public abstract class WritableDataStoreDataSource extends DataStoreDataSource {

    private static final String KEY = WritableDataStoreDataSource.class.getName();


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
    public static final String EXTERNAL_PK_KEY              = "External PK";
    public static final String SRID_KEY                     = "SRID";
    public static final String GEOM_DIM_KEY                 = "Dimension";
    public static final String NAN_Z_TO_VALUE_KEY           = "NaN Z to value";
    public static final String NARROW_GEOMETRY_TYPE_KEY     = "Narrow geometry type";
    public static final String CONVERT_TO_MULTIGEOMETRY_KEY = "Convert to multigeometry";
    public static final String CREATE_PK                    = "Create PK";
    public static final String NORMALIZED_COLUMN_NAMES      = "Normalized Column Names";

    public static final String DEFAULT_PK_NAME   = "gid";

    // Ordered Map of evolutions
    // Map is indexed by FID in order to merge successive evolutions of a feature efficiently
    final private LinkedHashMap<Integer,Evolution> evolutions = new LinkedHashMap<>();

    private DataStoreTransactionManager txManager;

    // See setTableAlreadyCreated()
    private boolean tableAlreadyCreated = true;

    // unquoted schema name or null for default schema
    protected String schemaName;
    // unquoted table name
    protected String tableName;
    // primary key name
    protected String primaryKeyName = DEFAULT_PK_NAME;

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
                                       String externalPKName,
                                       DataStoreTransactionManager txManager,
                                       WorkbenchContext context) {
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

        //getProperties().put(CREATE_TABLE, false);
        getProperties().put(CREATE_PK, false);
        getProperties().put(SRID_KEY, 0);
        this.txManager = txManager;
        this.context = context;
    }

    public void setLimitedToView(boolean limitedToView) {
        getProperties().put(LIMITED_TO_VIEW, limitedToView);
    }

    public void setManageConflicts(boolean manageConflicts) {
        getProperties().put(MANAGE_CONFLICTS, manageConflicts);
    }

    public void setMultiGeometry(boolean multi) {
        getProperties().put(CONVERT_TO_MULTIGEOMETRY_KEY, multi);
    }

    public void setCoordDimension(int dbCoordDim) {
        getProperties().put(GEOM_DIM_KEY, dbCoordDim);
    }

    public void setSRID(int srid) {
        getProperties().put(SRID_KEY, srid);
    }

    /**
     * Add this attribute to decide if executeUpdate must write a new table
     * or commit to an existing table.
     * Note : I tried first to set this property in the DataSourceQuery properties,
     * but properties are set through the "load" or "save as" dialog box and are not
     * supposed to change (I tried to change the value at the end of an executeUpdate,
     * but initial properties set in the dialog box are re-applied and overwrite
     * changed value after that).
     */
    public void setTableAlreadyCreated(boolean tableAlreadyCreated) {
        this.tableAlreadyCreated = tableAlreadyCreated;
    }

    public boolean isWritable() {
        return true;
    }

    public Connection getConnection() {

        return new Connection() {

            @Override
            public FeatureCollection executeQuery(String query,
                        Collection<Throwable> exceptions, TaskMonitor monitor) {
                try {
                    String[] datasetName = SQLUtil.splitTableName((String) getProperties().get(DATASET_NAME_KEY));
                    schemaName = datasetName[0];
                    tableName = datasetName[1];
                    primaryKeyName = (String)getProperties().get(EXTERNAL_PK_KEY);
                    // Must be implemented by subclasses
                    return createFeatureCollection();
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                }
            }

            @Override
            public FeatureCollection executeQuery(String query,
                                                  TaskMonitor monitor) throws Exception {
                Collection<Throwable> exceptions = new ArrayList<>();
                FeatureCollection featureCollection = executeQuery(query,
                        exceptions, monitor);
                if (!exceptions.isEmpty()) {
                    throw (Exception) exceptions.iterator().next();
                }
                return featureCollection;
            }

            @Override
            public void executeUpdate(String query,
                        FeatureCollection featureCollection, TaskMonitor monitor) throws Exception {

                ConnectionDescriptor connectionDescriptor =
                        (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);

                boolean normalizedColumnNames = getProperties().containsKey(NORMALIZED_COLUMN_NAMES) ?
                        (Boolean)getProperties().get(NORMALIZED_COLUMN_NAMES) : false;

                String[] datasetName = SQLUtil.splitTableName((String) getProperties().get(DATASET_NAME_KEY));
                schemaName = datasetName[0];
                tableName = datasetName[1];
                //String geometryColumn = (String)getProperties().get(WritableDataStoreDataSource.GEOMETRY_ATTRIBUTE_NAME_KEY);
                boolean createPrimaryKey = (Boolean)getProperties().get(WritableDataStoreDataSource.CREATE_PK);
                int srid = getProperties().get(SRID_KEY)==null ? 0 : (Integer)getProperties().get(SRID_KEY);
                boolean narrow = getProperties().get(NARROW_GEOMETRY_TYPE_KEY) != null &&
                        (boolean)getProperties().get(NARROW_GEOMETRY_TYPE_KEY);
                boolean multi = getProperties().get(CONVERT_TO_MULTIGEOMETRY_KEY) != null &&
                        (boolean)getProperties().get(CONVERT_TO_MULTIGEOMETRY_KEY);
                Class geometryType = getGeometryType(featureCollection, narrow, multi);
                int dim = getProperties().get(GEOM_DIM_KEY)==null?
                        getGeometryDimension(featureCollection, 3) :
                        (Integer)getProperties().get(GEOM_DIM_KEY);

                DataStoreDriver driver = ConnectionManager.instance(context)
                        .getDriver(connectionDescriptor.getDataStoreDriverClassName());
                SpatialDatabasesDSConnection conn =
                        (SpatialDatabasesDSConnection)connectionDescriptor.createConnection(driver);
                java.sql.Connection jdbcConn = conn.getJdbcConnection();
                try {
                    jdbcConn.setAutoCommit(false);
                    if (!tableAlreadyCreated) {
                        Logger.debug("Update mode: create table");
                        boolean exists = tableExists(jdbcConn);
                        if (exists && !confirmOverwrite()) return;
                        if (exists) {
                            deleteTableQuery(conn);
                        }
                        // if a external PK already exists, unmark it
                        // if createPrimaryKey=true, it will be re-created
                        // if createPrimaryKey=false, old gid will be considered as a normal attribute
                        featureCollection.getFeatureSchema().removeExternalPrimaryKey();
                        createAndPopulateTable(
                                conn,
                                featureCollection,
                                srid,
                                geometryType.getSimpleName(),
                                multi,
                                dim,
                                normalizedColumnNames);
                        if (createPrimaryKey) {
                            addDBPrimaryKey(conn, DEFAULT_PK_NAME);
                            // @TODO reload part is kept out of the transaction because it uses
                            // PostGISFeatureInputStream which init() function contains
                            // rs = stmt.executeQuery(parsedQuery);
                            // This instruction is not compatible with the transaction mode
                            // ==> If PostGISFeatureInputStream#init() is made transactionnal
                            // we must check that all calling methods do commit it.
                            jdbcConn.commit();
                            reloadDataFromDataStore(this, monitor);
                        }
                        tableAlreadyCreated = true;
                    }
                    else {
                        Logger.debug("Update mode: update table");
                        primaryKeyName = (String)getProperties().get(EXTERNAL_PK_KEY);
                        FeatureSchema featureSchema = featureCollection.getFeatureSchema();
                        if (conn.getCompatibleSchemaSubset(schemaName, tableName, featureSchema, normalizedColumnNames)
                                .length < featureSchema.getAttributeCount()) {
                            if (!confirmWriteDespiteDifferentSchemas()) return;
                        }
                        commit(conn, srid, multi, dim, normalizedColumnNames);
                        evolutions.clear();
                    }
                    jdbcConn.commit();
                }
                finally {
                    if (jdbcConn != null) jdbcConn.setAutoCommit(true);
                }
                // Adding vacuum analyze seems to be necessary to be able to use
                // ST_Estimated_Extent on the newly created table
                //finalizeUpdate(conn);
            }

            @Override
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
    @Deprecated // maybe much time consuming, to be driven by the server, not the client
    public abstract void finalizeUpdate(SpatialDatabasesDSConnection conn) throws Exception;


    private void commit(SpatialDatabasesDSConnection conn,
                int srid, boolean multi, int dim, boolean normalizedColumnNames) throws Exception {

        Logger.info("Evolutions to commit to " + schemaName + "." + tableName + " (PK=" + primaryKeyName +")");
        for (Evolution evolution : evolutions.values()) {
            if (evolution.getType() == Evolution.Type.CREATION) {
                PreparedStatement pstmt = insertStatement(conn,
                        evolution.getNewFeature().getSchema(), multi, normalizedColumnNames);
                pstmt = setAttributeValues(pstmt, evolution.getNewFeature(), srid, multi, dim);
                pstmt.execute();
                Logger.info("  create new feature " + evolution.getNewFeature().getID()+"/");
            } else if (evolution.getType() == Evolution.Type.SUPPRESSION) {
                deleteStatement(conn, evolution.getOldFeature()).executeUpdate();
                Logger.info("  delete " + evolution.getOldFeature().getID() + "/" +
                        evolution.getOldFeature().getAttribute(primaryKeyName));
            } else if (evolution.getType() == Evolution.Type.MODIFICATION) {
                Feature oldFeature = evolution.getOldFeature();
                Feature newFeature = evolution.getNewFeature();
                FeatureSchema schema = oldFeature.getSchema();
                // Attribute changes are updated individually, avoiding to replace
                // values changed concurrently by another client if it is not needed
                for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                    if (schema.isAttributeReadOnly(i)) continue;
                    if (oldFeature.getAttribute(i) == null && newFeature.getAttribute(i) != null ||
                        oldFeature.getAttribute(i) != null && newFeature.getAttribute(i) == null ||
                        oldFeature.getAttribute(i) != null && !oldFeature.getAttribute(i).equals(newFeature.getAttribute(i))) {
                        updateOneAttributeStatement(conn, newFeature, i, srid, multi, dim).executeUpdate();
                    }
                }
                Logger.info("  modify " + evolution.getNewFeature().getID() + "/" +
                        evolution.getNewFeature().getAttribute(primaryKeyName));
            }
        }

        evolutions.clear();
    }

    /**
     * Create a statement to insert a feature into the database
     * @param conn connection to the database to update.
     * @param fSchema feature schema
     * @param normalizedColumnNames whether database column names should be normalized (lowercase) or not
     * @return a PreparedStatement
     * @throws SQLException  if an exception occurs during insert
     */
    protected PreparedStatement insertStatement(SpatialDatabasesDSConnection conn,
                FeatureSchema fSchema, boolean multi, boolean normalizedColumnNames) throws SQLException {

        StringBuilder sb = new StringBuilder("INSERT INTO " + SQLUtil.compose(schemaName, tableName) + "(");
        // create a column name list without datatypes, including geometry and excluding primary key
        sb.append(conn.getMetadata().createColumnList(fSchema, false, true, false, false, normalizedColumnNames))
          .append(") VALUES(");
        //int nbValues = fSchema.getAttributeCount();
        //if (primaryKeyName != null && fSchema.hasAttribute(primaryKeyName)) nbValues --;
        boolean first = true;
        for (int i = 0 ; i < fSchema.getAttributeCount() ; i++) {
            if (fSchema.getExternalPrimaryKeyIndex() == i) continue;
            if (fSchema.isAttributeReadOnly(i)) continue;
            if (multi && fSchema.getAttributeType(i) == AttributeType.GEOMETRY) {
                sb.append(first ? "ST_Multi(?)" : ",ST_Multi(?)");
            } else {
                sb.append(first ? "?" : ",?");
            }
            first = false;
        }
        sb.append(");");
        Logger.trace(sb.toString());
        return conn.getJdbcConnection().prepareStatement(sb.toString());
    }


    private PreparedStatement updateOneAttributeStatement(SpatialDatabasesDSConnection conn,
                Feature feature, int attribute, int srid, boolean multi, int dim) throws SQLException {

        FeatureSchema schema = feature.getSchema();
        boolean quoted = schema.getAttributeType(schema.getExternalPrimaryKeyIndex()) == AttributeType.STRING;
        String quoteKey = quoted ? "'" : "";

        String query = "UPDATE " + SQLUtil.compose(schemaName, tableName) +
                       " SET \"" + schema.getAttributeName(attribute) + "\" = ?" +
                       " WHERE \"" + primaryKeyName + "\" = " + quoteKey +
                       feature.getAttribute(primaryKeyName) + quoteKey + ";";

        PreparedStatement pstmt = conn.getJdbcConnection().prepareStatement(query);
        AttributeType type = schema.getAttributeType(attribute);
        if (feature.getAttribute(attribute) == null) pstmt.setObject(1, null);
        else if (type == AttributeType.STRING)   pstmt.setString(1, feature.getString(attribute));
        else if (type == AttributeType.GEOMETRY) {
            Geometry g = (Geometry) feature.getAttribute(attribute);
            if (multi) {
                if (g instanceof Point) g = g.getFactory().createMultiPoint(new Point[]{(Point)g});
                else if (g instanceof LineString) g = g.getFactory().createMultiLineString(new LineString[]{(LineString)g});
                else if (g instanceof Polygon) g = g.getFactory().createMultiPolygon(new Polygon[]{(Polygon)g});
            }
            pstmt.setBytes(1, SQLUtil.getByteArrayFromGeometry(g, srid, dim));
        }
        else if (type == AttributeType.INTEGER)  pstmt.setInt(1, feature.getInteger(attribute));
        else if (type == AttributeType.LONG)     pstmt.setLong(1, (Long) feature.getAttribute(attribute));
        else if (type == AttributeType.DOUBLE)   pstmt.setDouble(1, feature.getDouble(attribute));
        else if (type == AttributeType.BOOLEAN)  pstmt.setBoolean(1, (Boolean) feature.getAttribute(attribute));
        else if (type == AttributeType.DATE)     pstmt.setTimestamp(1, new Timestamp(((Date) feature.getAttribute(attribute)).getTime()));
        else if (type == AttributeType.OBJECT)   pstmt.setObject(1, feature.getAttribute(attribute));
        else throw new IllegalArgumentException(type + " is an unknown AttributeType !");
        Logger.debug(pstmt.toString());
        return pstmt;
    }

    private PreparedStatement deleteStatement(SpatialDatabasesDSConnection conn, Feature feature) throws SQLException {
        PreparedStatement pstmt = conn.getJdbcConnection()
                .prepareStatement("DELETE FROM " + SQLUtil.compose(schemaName, tableName) + " WHERE \"" + primaryKeyName + "\" = ?");
        pstmt.setObject(1,feature.getAttribute(primaryKeyName));
        Logger.debug(pstmt.toString());
        return pstmt;
    }

    protected PreparedStatement setAttributeValues(PreparedStatement pstmt,
                Feature feature, int srid, boolean multi, int dim) throws SQLException {
        FeatureSchema schema = feature.getSchema();
        Set<String> excludedAttributes = new HashSet<>();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            if (schema.isAttributeReadOnly(i)) {
                excludedAttributes.add(schema.getAttributeName(i));
            }
        }
        int index = 1;
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            AttributeType type = schema.getAttributeType(i);
            if (excludedAttributes.contains(schema.getAttributeName(i))) continue;
            if (schema.getExternalPrimaryKeyIndex() == i) pstmt.setObject(index++, null);
            if (feature.getAttribute(i) == null)     pstmt.setObject(index++, null);
            else if (type == AttributeType.STRING)   pstmt.setString(index++, feature.getString(i));
            else if (type == AttributeType.GEOMETRY) {
                Geometry g = (Geometry)feature.getAttribute(i);
                if (multi) {
                    if (g instanceof Point) g = g.getFactory().createMultiPoint(new Point[]{(Point)g});
                    else if (g instanceof LineString) g = g.getFactory().createMultiLineString(new LineString[]{(LineString)g});
                    else if (g instanceof Polygon) g = g.getFactory().createMultiPolygon(new Polygon[]{(Polygon)g});
                }
                pstmt.setBytes(index++, SQLUtil.getByteArrayFromGeometry(g, srid, dim));
            }
            else if (type == AttributeType.INTEGER)  pstmt.setInt(index++, feature.getInteger(i));
            else if (type == AttributeType.LONG)     pstmt.setLong(index++, (Long) feature.getAttribute(i));
            else if (type == AttributeType.DOUBLE)   pstmt.setDouble(index++, feature.getDouble(i));
            else if (type == AttributeType.BOOLEAN)  pstmt.setBoolean(index++, (Boolean) feature.getAttribute(i));
            else if (type == AttributeType.DATE)     pstmt.setTimestamp(index++, new Timestamp(((Date)feature.getAttribute(i)).getTime()));
            else if (type == AttributeType.OBJECT)   {
                if (feature.getAttribute(i) instanceof Geometry) {
                    // In our use case, other geometry attributes use the same srid as the main geometry
                    // but always have dimension = 2. This use case does not fit all !
                    int object_srid = ((Geometry)feature.getAttribute(i)).getSRID();
                    pstmt.setBytes(index++, SQLUtil.getByteArrayFromGeometry((Geometry)feature.getAttribute(i), object_srid, 2));
                } else if (feature.getAttribute(i) instanceof String) {
                    pstmt.setBytes(index++, feature.getAttribute(i).toString().getBytes());
                }
                else pstmt.setObject(index++, feature.getAttribute(i));
            }
            else throw new IllegalArgumentException(type + " is an unknown AttributeType !");
        }
        return pstmt;
    }

    protected abstract int getTableSRID(java.sql.Connection conn, String column) throws SQLException;

    protected Geometry getViewEnvelope() {
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
        //Evolution newEvo = Evolution.createCreation(feature.clone(true, false)).mergeToPrevious(oldEvo);
        // copy the pk if exists (may happen when a new feature is build from an old one as in split command)
        // the pk of a creation will not be committed to the database, but keeping it in the local feature
        // may help in case of undo/redo
        Evolution newEvo = Evolution.createCreation(feature.clone(true, true)).mergeToPrevious(oldEvo);
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
     * @param fid id of the feature to be removed in the evolution stack
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
     * @return a Map containing evolutions indexed by id
     */
    public Map<Object,Evolution> getIndexedEvolutions() {
        Map<Object,Evolution> index = new TreeMap<>();
        for (Evolution evolution : evolutions.values()) {
            Evolution.Type type = evolution.getType();
            if (type == Evolution.Type.MODIFICATION || type == Evolution.Type.SUPPRESSION) {
                Object dbid = evolution.getOldFeature().getAttribute(primaryKeyName);
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
    private boolean tableExists(java.sql.Connection conn) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        return metadata.getTables(null, schemaName, tableName, new String[]{"TABLE"}).next();
    }

    /**
     * Execute a query against this connection to delete the reference to this
     * table in the PostGIS's geometry_columns table.
     */
    abstract protected void deleteTableQuery(SpatialDatabasesDSConnection conn) throws SQLException;

    /**
     * Create and populate a table with features from a dataset.
     * @param conn connection to the database
     * @param fc featureCollection to upload to the database
     * @param srid srid of the geometry
     * @param geometryType geometry type
     * @param dim geometry dimension
     * @param normalizedColumnNames whether columns names have to be normalized or not
     * @throws SQLException if an exception occurs during table creation or inserts
     */
    abstract protected void createAndPopulateTable(
            SpatialDatabasesDSConnection conn,
            FeatureCollection fc,
            int srid, String geometryType, boolean multi, int dim,
            boolean normalizedColumnNames) throws SQLException;


    /**
     * Add an automatically named primary key constraint to the table.
     */
    protected abstract void addDBPrimaryKey(SpatialDatabasesDSConnection conn, String primaryKey) throws SQLException;


    // @TODO Bad design : it should be possible to do this kind of post-processing
    // in the loader (where layer name is known rather than in the datasource)
    private void reloadDataFromDataStore(Connection conn, TaskMonitor monitor) throws Exception {
        Layer[] selectedLayers = JUMPWorkbench.getInstance().getContext().getLayerableNamePanel().getSelectedLayers();
        if (selectedLayers != null && selectedLayers.length == 1) {
            boolean oldFiringEvents = JUMPWorkbench.getInstance().getContext().getLayerManager().isFiringEvents();
            JUMPWorkbench.getInstance().getContext().getLayerManager().setFiringEvents(false);
            try {
                selectedLayers[0].setFeatureCollection(conn.executeQuery(null, monitor));
                // We connect to a new table : the transaction manager must listen to it
                if (!tableAlreadyCreated) {
                    //DataStoreTransactionManager.getTransactionManager().registerLayer(selectedLayers[0],
                    txManager.registerLayer(selectedLayers[0],
                            JUMPWorkbench.getInstance().getContext().getTask());
                    tableAlreadyCreated = true;
                }
            } finally {
                JUMPWorkbench.getInstance().getContext().getLayerManager().setFiringEvents(oldFiringEvents);
            }
        }
    }

    /**
     * Return 3 if coll contains at least one 3d geometry, 2 if coll contains
     * only 2d geometries and defaultDim if coll is empty.
     */
    private static int getGeometryDimension(FeatureCollection coll, int defaultDim) {
        if (coll.size() > 0) {
            // will explore up to 1000 features regularly distributed in the dataset
            // if none of these feature has dim = 3, return 2, else return 3
            int step = 1 + coll.size()/1000;
            int count = 0;
            for (Iterator it = coll.iterator() ; it.hasNext() ; ) {
                if (count%step == 0 &&
                        getGeometryDimension(((Feature)it.next()).getGeometry()) == 3) {
                    return 3;
                }
                count++;
            }
            return 2;
        } else return defaultDim;
    }

    private static int getGeometryDimension(Geometry g) {
        Coordinate[] cc = g.getCoordinates();
        for (Coordinate c : cc) {
            if (!Double.isNaN(c.z)) return 3;
        }
        return 2;
    }

    /**
     * Determine database geometry type according to
     * <ul>
     *   <li>values present in the feature collection</li>
     *   <li>narrow attribute : true means that we want to use the most specific
     *   attribute type able to represent all geometries of the collection</li>
     *   <li>multi parameter : true means that we previously transform single
     *   geometry types into multigeometry types to be able to use the same
     *   type (multi) for geometries of same dimension (single or multi)</li>
     * </ul>
     */
    private static Class getGeometryType(FeatureCollection coll, boolean narrow, boolean multi) {
        if (!narrow && !multi) return Geometry.class;
        Class[] classes = new Class[]{
                Point.class,
                LineString.class,
                Polygon.class,
                MultiPoint.class,
                MultiLineString.class,
                MultiPolygon.class
        };
        int[] types = new int[]{0,0,0,0,0,0};
        for (Iterator it = coll.iterator() ; it.hasNext() ; ) {
            Geometry geom = ((Feature)it.next()).getGeometry();
            Class currentClazz = geom.getClass();
            if (currentClazz == GeometryCollection.class) return Geometry.class;
            int index = geom.getDimension() + ((geom instanceof GeometryCollection)?3:0);
            types[index]++;
        }
        if (multi) types = new int[]{0,0,0,types[0]+types[3],types[1]+types[4],types[2]+types[5]};
        Class firstClass = null, lastClass = null;
        for (int i = 0 ; i < 6 ; i++) {
            if (firstClass == null && types[i]>0) firstClass = classes[i];
            if (types[i]>0) lastClass = classes[i];
        }
        if (firstClass == lastClass) return firstClass;
        else return Geometry.class;
    }

}
