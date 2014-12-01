package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.BaseFeatureInputStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Reads features from a PostgreSQL/PostGIS database.
 */
public class PostgisFeatureInputStream extends BaseFeatureInputStream {
    
    private FeatureSchema featureSchema;
    private Connection conn;
    private String queryString;
    private boolean initialized = false;
    private Exception savedException;

    private Statement stmt = null;
    private ResultSet rs = null;
    private PostgisResultSetConverter mapper;

    int geometryColIndex = -1;
    String externalIdentifier = null;  // added on 2013-08-07

    public PostgisFeatureInputStream(Connection conn, String queryString) {
        this.conn = conn;
        this.queryString = queryString;
    }

    public PostgisFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        this.conn = conn;
        this.queryString = queryString;
        this.externalIdentifier = externalIdentifier;
    }

    /**
     * @return The underlaying {@link Connection}.
     */
    public Connection getConnection(){return conn;}
  
    /**
     * @return The underlaying {@link Statement}.
     * Useful to cancel the query on the server if the PlugIn is interrupted 
     */
    public Statement getStatement(){return stmt;}

    private void init() throws SQLException {
        if (initialized) {
            return;
        }
        initialized = true;
    
        stmt = conn.createStatement();
        String parsedQuery = queryString;
        try {
            rs = stmt.executeQuery(parsedQuery);
        } catch (SQLException e) {
            SQLException sqle = new SQLException("Error : " + parsedQuery);
            sqle.setNextException(e);
            throw sqle;
        }
        mapper = new PostgisResultSetConverter(conn, rs);
        featureSchema = mapper.getFeatureSchema();
        if (externalIdentifier != null) {
            featureSchema.setExternalPrimaryKeyIndex(featureSchema.getAttributeIndex(externalIdentifier));
        }
    }
    
    protected Feature readNext() throws Exception {
        if (savedException != null) throw savedException;
        if (! initialized) init();
        if (rs == null) return null;
        if (! rs.next()) return null;
        return getFeature();
    }
    
    private Feature getFeature() throws Exception {
        return mapper.getFeature();
    }
    
    public void close() throws SQLException {
        if (rs != null) {
            rs.close();
        }
        if (stmt != null) {
            stmt.close();
        }
    }
    
    public FeatureSchema getFeatureSchema() {      
        if (featureSchema != null) {
            return featureSchema;
        }
        try {
            init();
        }
        catch (SQLException ex) {
            String message = ex.getLocalizedMessage();
            Throwable nextT = ex.getNextException();
            if (nextT != null) message = message + "\n" + nextT.getLocalizedMessage();
            throw new Error(message);
        }
        if (featureSchema == null) {
            featureSchema = new FeatureSchema();
        }
        return featureSchema;
    }
}