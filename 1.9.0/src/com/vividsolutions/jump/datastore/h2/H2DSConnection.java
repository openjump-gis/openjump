package com.vividsolutions.jump.datastore.h2;

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
 * Connection to a H2GIS database.
 */
public class H2DSConnection extends SpatialDatabasesDSConnection {

    public H2DSConnection(Connection con) {
        super(con); // ?
        connection = con;
        this.dbMetadata = new H2DSMetadata(this);
    }

    @Override
    public SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid, String[] colNames) {
        return new H2SQLBuilder(this.dbMetadata, srid, colNames);
    }

    /**
     * Executes a filter query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     * 13 dec 2015: query is now tested before execution, to prevent adding an empty
     * layer built from invalid WHERE clause, for instance.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException
     */
    @Override
    public FeatureInputStream executeFilterQuery(FilterQuery query) throws SQLException {
        SpatialReferenceSystemID srid = dbMetadata.getSRID(query.getDatasetName(), query.getGeometryAttributeName());
        String[] colNames = dbMetadata.getColumnNames(query.getDatasetName());

        H2SQLBuilder builder = (H2SQLBuilder)this.getSqlBuilder(srid, colNames);
        String queryString = builder.getSQL(query);

        // [mmichaud 2013-08-07] add a parameter for database primary key name
        return new H2FeatureInputStream(connection, queryString, query.getPrimaryKey());
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
        H2FeatureInputStream ifs = new H2FeatureInputStream(connection, queryString, query.getPrimaryKey());

        // Nicolas Ribot: getting FeatureSchema here actually runs the query: if an error occurs, must trap it here
        FeatureSchema fs = null;
        try {
            fs = ifs.getFeatureSchema();
        } catch (Exception e) {
            throw new Exception(
                    I18N.get(SpatialDatabasesDSConnection.class.getName()
                            + ".SQL-error") + e.getMessage());
        }

        if (fs.getGeometryIndex() < 0) {
            throw new Exception(I18N.get(SpatialDatabasesDSConnection.class.getName()
                    +".resultset-must-have-a-geometry-column"));
        }
        return ifs;

    }
}
