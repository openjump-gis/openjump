package com.vividsolutions.jump.datastore.ocient;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSMetadata;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;


/**
 * Creates SQL query strings for a Spatial database.
 * To be overloaded by classes implementing a spatial database support.
 */
public class OcientSQLBuilder extends SpatialDatabasesSQLBuilder {

  public OcientSQLBuilder(SpatialDatabasesDSMetadata dbMetadata, 
      SpatialReferenceSystemID defaultSRID, String[] colNames) {
    super(dbMetadata, defaultSRID, colNames);
  }

  /**
   * Builds a valid SQL spatial query with the given spatial filter.
   * @param query
   * @return a SQL query to get column names
   */
  @Override
  public String getSQL(FilterQuery query) {
    StringBuilder qs = new StringBuilder();
    //HACK
    qs.append("SELECT ");
    qs.append(getColumnListSpecifier(colNames, query.getGeometryAttributeName()));
    qs.append(" FROM ");
    // fixed by mmichaud on 2010-05-27 for mixed case dataset names
    qs.append("\"").append(query.getDatasetName().replaceAll("\\.","\".\"")).append("\"");
    qs.append(" t WHERE ");
    qs.append(buildBoxFilter(query));

    String whereCond = query.getCondition();
    if (whereCond != null) {
      qs.append(" AND ");
      qs.append(whereCond);
    }
    int limit = query.getLimit();
    if (limit != 0 && limit != Integer.MAX_VALUE) {
      qs.append(" LIMIT ").append(limit);
    }
    //System.out.println(qs);
    return qs.toString();
  };
  
  /**
   * Returns the query allowing to test a DataStoreLayer: builds a query with where
   * clause and limit 0 to check where clause.
   * @return 
   */
  @Override
  public String getCheckSQL(DataStoreLayer dsLayer) {
    String s = "select * FROM %s %s LIMIT 0";
    String wc = dsLayer.getWhereClause();
    if (wc != null && ! wc.isEmpty()) {
      wc = " WHERE " + wc ;
    } else {
      wc = "";
    }
    //System.out.println(qs);
    return String.format(s, dsLayer.getFullName(), wc);
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

    // [mmichaud 2016-10-01] ST_AsEWKB is no more util and is not compatible with r5032
    buf.append("\"").append(geomColName).append("\"");
    for (String colName : colNames) {
      if (! geomColName.equalsIgnoreCase(colName)) {
        buf.append(",\"").append(colName).append("\"");
      }
    }
    return buf.toString();
  }

  @Override
  protected String buildBoxFilter(FilterQuery query) {
    Envelope env = query.getFilterGeometry().getEnvelopeInternal();

    // Example of Ocient SQL: where st_Intersects(b.geom, st_polygonFromText('POLYGON((4 4, 5 4, 5 5, 4 5, 4 4))'))
    StringBuilder buf = new StringBuilder();
    buf.append("st_intersects(").append(query.getGeometryAttributeName()).append(", st_polygonFromText('POLYGON((");
    buf.append(env.getMinX()).append(" ").append(env.getMinY()).append(",")
        .append(env.getMaxX()).append(" ").append(env.getMinY()).append(",")
        .append(env.getMaxX()).append(" ").append(env.getMaxY()).append(",")
        .append(env.getMinX()).append(" ").append(env.getMaxY()).append(",")
        .append(env.getMinX()).append(" ").append(env.getMinY());
    buf.append("))'))");
    return buf.toString();
  }
}
