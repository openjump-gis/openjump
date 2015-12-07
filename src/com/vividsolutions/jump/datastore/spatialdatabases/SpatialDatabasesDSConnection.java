/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.Query;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for all spatial databases DataStore connections.
 * No need to subclass for PostGIS, Oracle Spatial, 
 * @author nicolas Ribot
 */
public class SpatialDatabasesDSConnection implements DataStoreConnection {

    protected SpatialDatabasesDSMetadata dbMetadata;
    protected Connection connection;

    public SpatialDatabasesDSConnection(Connection conn) {
        JUMPWorkbench.getInstance().getFrame().log("creating a SpatialDatabasesDSConnection id" + this.hashCode(), this.getClass());
        connection = conn;
        dbMetadata = new SpatialDatabasesDSMetadata(this);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public DataStoreMetadata getMetadata() {
        return dbMetadata;
    }
    
    public SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid, String[] colNames) {
      return new SpatialDatabasesSQLBuilder(this.dbMetadata, srid, colNames);
    }

    @Override
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

        SpatialDatabasesSQLBuilder builder = this.getSqlBuilder(srid, colNames);
        String queryString = builder.getSQL(query);

        // [mmichaud 2013-08-07] add a parameter for database primary key name
        return new SpatialDatabasesFeatureInputStream(connection, queryString, query.getPrimaryKey());
    }

    public FeatureInputStream executeAdhocQuery(AdhocQuery query) throws Exception {
        String queryString = query.getQuery();
        SpatialDatabasesFeatureInputStream ifs = new SpatialDatabasesFeatureInputStream(connection, queryString, query.getPrimaryKey());
        if (ifs.getFeatureSchema().getGeometryIndex() < 0) {
            throw new Exception(I18N.get(this.getClass().getName()+".resultset-must-have-a-geometry-column"));
        }
        return ifs;
    }


    @Override
    public void close() throws DataStoreException {
        try {
            connection.close();
        }
        catch (Exception ex) { throw new DataStoreException(ex); }
    }

    @Override
    public boolean isClosed() throws DataStoreException {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            throw new DataStoreException(e);
        }
    }
}