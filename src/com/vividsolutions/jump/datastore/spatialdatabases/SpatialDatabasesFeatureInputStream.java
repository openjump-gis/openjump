package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.BaseFeatureInputStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Reads features from a Spatial database.
 * 
 */
public class SpatialDatabasesFeatureInputStream extends BaseFeatureInputStream {
    
    protected FeatureSchema featureSchema;
    protected Connection conn;
    protected String queryString;
    private boolean initialized = false;
    private Exception savedException;

    private Statement stmt = null;
    private ResultSet rs = null;
    private SpatialDatabasesResultSetConverter mapper;

    String externalIdentifier = null;  // added on 2013-08-07

    public SpatialDatabasesFeatureInputStream(Connection conn, String queryString) {
        this(conn, queryString, null);
    }

    public SpatialDatabasesFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        this.conn = conn;
        this.queryString = queryString;
        this.externalIdentifier = externalIdentifier;
    }

    /**
     * @return The underlaying {@link Connection}.
     */
    public Connection getConnection(){return conn;}
    
    /**
     * 
     * @return 
     */
    public String getQueryString(){return queryString;}
    
    /**
     * To overload
     * @param rs
     * @return 
     */
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
      return new SpatialDatabasesResultSetConverter(conn, rs);
    }
  
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
          // adds SQL query to SQLError
          e.setNextException(new SQLException("Invalid query: " + queryString));
          throw e;
        }
//        mapper = new SpatialDatabasesResultSetConverter(conn, rs);
        mapper = getResultSetConverter(rs);
        featureSchema = mapper.getFeatureSchema();
        //System.out.println("init: getting featureSchema wuth query: " + queryString + " init ? " + initialized);
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
            throw new Error(message, ex);
        }
        if (featureSchema == null) {
            featureSchema = new FeatureSchema();
        }
        return featureSchema;
    }
}