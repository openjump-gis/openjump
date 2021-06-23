package com.vividsolutions.jump.datastore.mariadb;

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
 * @author nicolas
 */
public class MariadbDSConnection extends SpatialDatabasesDSConnection {

    public MariadbDSConnection(Connection con) {
        super(con);
        this.dbMetadata = new MariadbDSMetadata(this);
    }
    
    @Override
    public SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid, String[] colNames) {
      return new MariadbSQLBuilder(this.dbMetadata, srid, colNames);
    }

    /**
     * Executes a filter query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException if an Exception occurs during query execution
     */
    @Override
    public FeatureInputStream executeFilterQuery(FilterQuery query) throws SQLException {
        SpatialReferenceSystemID srid = dbMetadata.getSRID(query.getDatasetName(), query.getGeometryAttributeName());
        String[] colNames = dbMetadata.getColumnNames(query.getDatasetName());

        MariadbSQLBuilder builder = (MariadbSQLBuilder)this.getSqlBuilder(srid, colNames);
        String queryString = builder.getSQL(query);

        // [mmichaud 2013-08-07] add a parameter for database primary key name
        return new MariadbFeatureInputStream(connection, queryString, query.getPrimaryKey());
    }
    
    /**
     * Executes an adhoc query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException if an Exception occurs during query execution
     */
    @Override
    public FeatureInputStream executeAdhocQuery(AdhocQuery query) throws Exception {
        String queryString = query.getQuery();
        MariadbFeatureInputStream ifs = new MariadbFeatureInputStream(connection, queryString, query.getPrimaryKey());
        
        // Nicolas Ribot: getting FeatureSchema here actually runs the query: if an error occurs, must trap it here
        FeatureSchema fs;
        try {
          fs = ifs.getFeatureSchema();
        } catch (Exception e) {
          throw new Exception(
              I18N.getInstance().get(com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection.class.getName()
                  +".SQL-error") + e.getMessage());
        }
        
        if (fs.getGeometryIndex() < 0) {
            throw new Exception(I18N.getInstance().get(
                com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection.class.getName()
                +".resultset-must-have-a-geometry-column"));
        }
        return ifs;
        
    }

    public MariadbValueConverterFactory getValueConverterFactory() {
        return new MariadbValueConverterFactory(connection);
    }
    
}
