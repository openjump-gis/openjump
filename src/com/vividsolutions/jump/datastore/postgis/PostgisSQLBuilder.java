package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.BoundQuery;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDataStoreMetadata;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;


/**
 * Creates SQL query strings for a Spatial database.
 * To be overloaded by classes implementing a spatial database support.
 */
public class PostgisSQLBuilder extends SpatialDatabasesSQLBuilder {

  public PostgisSQLBuilder(SpatialDataStoreMetadata dbMetadata, 
      SpatialReferenceSystemID defaultSRID, String[] colNames) {
    super(dbMetadata, defaultSRID, colNames);
  }

  /**
   * Builds a valid SQL spatial query with the given spatial filter.
   * @param query
   * @return a SQL query to get column names
   * //TODO: refactor like Oracle code: queries as variable placeholders: put it in base class.
   */
  @Override
  public BoundQuery getSQL(FilterQuery query) {
    // TODO: how to deal with PreparedStatements and the 2 possible queries ?
    String q = "SELECT %s FROM \"%s\" WHERE \"%s\" && %s %s %s";
//    String q2 = "SELECT %s FROM \"%s\" WHERE \"%s\" && st_setSRID(?, st_srid(\"%s\")) %s %s";
    
    String whereCond = query.getCondition() == null ? "" : " AND " + query.getCondition();
    String limit = (query.getLimit() != 0 && query.getLimit() != Integer.MAX_VALUE) 
        ? " LIMIT " + query.getLimit() : " ";
    
    return new BoundQuery(String.format(q, 
        getColumnListSpecifier(colNames, query.getGeometryAttributeName()),
        query.getDatasetName().replaceAll("\\.","\".\""),
        query.getGeometryAttributeName(),
        buildBoxFilter(query),
        whereCond,
        limit));
    
  };
  
  /**
   * Returns the query allowing to test a DataStoreLayer: builds a query with where
   * clause and limit 0 to check where clause.
   * @return 
   */
  @Override
  public BoundQuery getCheckSQL(DataStoreLayer dsLayer) {
    String s = "select * FROM \"%s\" %s LIMIT 0";
    String wc = dsLayer.getWhereClause();
    if (wc != null && ! wc.isEmpty()) {
      wc = " WHERE " + wc ;
    } else {
      wc = "";
    }
    //System.out.println(qs);
    return new BoundQuery(String.format(s, dsLayer.getFullName(), wc));
  }

  /**
   * Returns the string representing a SQL column definition.
   * Implementors should take care of column names (case, quotes)
   * @param colNames
   * @param geomColName
   * @return column list
   */
  @Override
  protected String getColumnListSpecifier(String[] colNames, String geomColName) {
    // Added double quotes around each column name in order to read mixed case table names
    // correctly [mmichaud 2007-05-13]
    StringBuilder buf = new StringBuilder();
    // fixed by mmichaud using a patch from jaakko [2008-05-21]
    // query geomColName as geomColName instead of geomColName as geomColName + "_wkb"
    buf.append("ST_AsEWKB(\"").append(geomColName).append("\") as ").append("\"").append(geomColName).append("\"");
    for (String colName : colNames) {
      if (! geomColName.equalsIgnoreCase(colName)) {
        buf.append(",\"").append(colName).append("\"");
      }
    }
    return buf.toString();
  }

  /**
   * Example of Postgis SQL: GEOM && SetSRID('BOX3D(191232 243117,191232 243119)'::box3d,-1);
   * @param query
   * @return 
   */
  @Override
  protected String buildBoxFilter(FilterQuery query) {
    Envelope env = query.getFilterGeometry().getEnvelopeInternal();
    StringBuilder buf = new StringBuilder();
    String s = "ST_SetSRID('BOX3D(%s %s, %s %s)'::box3d, %s)";
    // [mmichaud 2012-03-14] make windows srid homogeneous with geometry srid
    // in case it is not defined
    String srid = getSRID(query.getSRSName());
    srid = srid==null? "ST_SRID(\"" + query.getGeometryAttributeName() + "\")" : srid;
    
    return String.format(s, env.getMinX(),env.getMinY(), env.getMaxX(), env.getMaxY(),
        srid);
  }
}
