package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.*;
import com.vividsolutions.jump.io.FeatureInputStream;

import java.sql.Connection;
import java.sql.SQLException;

/**
 */
public class OracleDSConnection implements DataStoreConnection {

    private OracleDSMetadata dbMetadata;
    private Connection connection;

    public OracleDSConnection(Connection conn) {
        connection = conn;
        dbMetadata = new OracleDSMetadata(this);
    }

    public Connection getConnection() {
        return connection;
    }

    public DataStoreMetadata getMetadata() {
        return dbMetadata;
    }

    public FeatureInputStream execute(Query query) throws Exception {
        if (query instanceof FilterQuery) {
            try {
                return executeFilterQuery((FilterQuery) query);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        if (query instanceof AdhocQuery) {
            return executeAdhocQuery((AdhocQuery) query);
        }
        throw new IllegalArgumentException(I18N.get(this.getClass().getName()+".unsupported-query-type"));
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
    public FeatureInputStream executeFilterQuery(FilterQuery query) throws SQLException {

        SpatialReferenceSystemID srid = dbMetadata.getSRID(query.getDatasetName(), query.getGeometryAttributeName());
        String[] colNames = dbMetadata.getColumnNames(query.getDatasetName());

        OracleSQLBuilder builder = new OracleSQLBuilder(srid, colNames, dbMetadata.getGeoIndexes());
        String queryString = builder.getSQL(query);

        return new OracleFeatureInputStream(connection, queryString, query.getPrimaryKey());
    }

    public FeatureInputStream executeAdhocQuery(AdhocQuery query) throws Exception {
        String queryString = query.getQuery();
        OracleFeatureInputStream ifs = new OracleFeatureInputStream(connection, queryString, query.getPrimaryKey());
        if (ifs.getFeatureSchema().getGeometryIndex() < 0) {
            throw new Exception(I18N.get(this.getClass().getName()+".resultset-must-have-a-geometry-column"));
        }
        return ifs;
    }


    public void close() throws DataStoreException {
        try {
            connection.close();
        }
        catch (Exception ex) { throw new DataStoreException(ex); }
    }

    public boolean isClosed() throws DataStoreException {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            throw new DataStoreException(e);
        }
    }

}