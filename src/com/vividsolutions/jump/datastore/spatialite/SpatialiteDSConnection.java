package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.FeatureInputStream;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author nicolas ribot
 * TODO: Manage converter to handle all geometry type in the same database.
 */
public class SpatialiteDSConnection extends SpatialDatabasesDSConnection {

    public SpatialiteDSConnection(Connection con) {
        super(con);
        this.dbMetadata = new SpatialiteDSMetadata(this);
    }
    
    /**
     * Keeps a reference on SpatialiteDSMetadata into the SQL builder, to access
     * Spatialite preferences necessary in order to build proper queries according to 
     * geometric type
     * @param srid srid of the dataset
     * @param colNames list of column names
     */
    @Override
    public SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid, String[] colNames) {
        return new SpatialiteSQLBuilder((SpatialiteDSMetadata)this.dbMetadata, srid, colNames);
    }

    /**
     * Executes a filter query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException
     */
    @Override
    public FeatureInputStream executeFilterQuery(FilterQuery query) throws SQLException {
        SpatialReferenceSystemID srid = dbMetadata.getSRID(query.getDatasetName(), query.getGeometryAttributeName());
        String[] colNames = dbMetadata.getColumnNames(query.getDatasetName());

        SpatialiteSQLBuilder builder = (SpatialiteSQLBuilder)this.getSqlBuilder(srid, colNames);
        String queryString = builder.getSQL(query);

        // [mmichaud 2013-08-07] add a parameter for database primary key name
        SpatialiteFeatureInputStream fis = new SpatialiteFeatureInputStream(connection, queryString, query.getPrimaryKey());
        // Needed to choose converter according to real geometry type
        fis.setMetadata((SpatialiteDSMetadata)dbMetadata);
        return fis;
    }
    
    /**
     * Executes an adhoc query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException
     */
    @Override
    public FeatureInputStream executeAdhocQuery(AdhocQuery query) throws Exception {
        String queryString = query.getQuery();
        SpatialiteFeatureInputStream ifs = new SpatialiteFeatureInputStream(connection, queryString, query.getPrimaryKey());
        // Needed to choose converter according to real geometry type
        ifs.setMetadata((SpatialiteDSMetadata)dbMetadata);
        
        // Nicolas Ribot: getting FeatureSchema here actually runs the query: if an error occurs, must trap it here
        FeatureSchema fs;
        try {
          fs = ifs.getFeatureSchema();
        } catch (Exception e) {
          throw new Exception(
            I18N.get(com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection.class.getName()
                +".SQL-error") + e.getMessage());
        }
        
        if (fs.getGeometryIndex() < 0) {
            throw new Exception(I18N.get(
                com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection.class.getName()
                +".resultset-must-have-a-geometry-column"));
        }
        return ifs;
        
    }

    public SpatialiteValueConverterFactory getValueConverterFactory() {
        return new SpatialiteValueConverterFactory(connection);
    }
    
}
