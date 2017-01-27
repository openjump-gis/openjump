package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;

/**
 * Creates SQL query strings for a Spatial database.
 * To be overloaded by classes implementing a spatial database support.
 */
public class SpatialDatabasesSQLBuilder {

  protected  SpatialReferenceSystemID defaultSRID = null;
  protected String[] colNames = null;
  protected SpatialDatabasesDSMetadata dbMetadata;

  public SpatialDatabasesSQLBuilder(SpatialDatabasesDSMetadata dbMetadata, 
      SpatialReferenceSystemID defaultSRID, String[] colNames) {
    
    this.defaultSRID = defaultSRID;
    this.colNames = colNames;
    this.dbMetadata = dbMetadata;
  }

  /**
   * Builds a valid SQL spatial query with the given spatial filter.
   * @param query the filter query
   * @return a SQL query to get column names
   */
  public String getSQL(FilterQuery query) {
    return null;
  }

  /**
   * Builds a check SQL query for the given DataStoreLayer.
   * @param dsLayer the @link DataStoreLayer to test
   * @return a SQL query forced to limit 0 to test the layer
   */
  public String getCheckSQL(DataStoreLayer dsLayer) {
    return null;
  }

  /**
   * Returns the string representing a SQL column definition.
   * Implementors should take care of column names (case, quotes)
   * @param colNames list of column names
   * @param geomColName name of the geometry column
   * @return column list
   */
  protected String getColumnListSpecifier(String[] colNames, String geomColName) {
    return null;
  }

  protected SpatialDatabasesDSMetadata getDbMetadata() {
    return dbMetadata;
  }

  protected String buildBoxFilter(FilterQuery query) {
    return null;
  }

  protected String getSRID(SpatialReferenceSystemID querySRID) {
    SpatialReferenceSystemID srid = defaultSRID;
    if (! querySRID.isNull())
      srid = querySRID;

    if (srid.isNull() || srid.getString().trim().length()==0)
      return null;
    else
      return srid.getString();
  }


}
