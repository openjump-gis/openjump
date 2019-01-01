package com.vividsolutions.jump.datastore.postgis;

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
public class PostgisSQLBuilder extends SpatialDatabasesSQLBuilder {

  public PostgisSQLBuilder(SpatialDatabasesDSMetadata dbMetadata, 
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

    // Example of Postgis SQL: GEOM && SetSRID('BOX3D(191232 243117,191232 243119)'::box3d,-1);
    StringBuilder buf = new StringBuilder();
    // fixed by mmichaud on 2010-05-27 for mixed case geometryColName names
    buf.append("\"").append(query.getGeometryAttributeName()).append("\" && ST_SetSRID('BOX3D(");
    buf.append(env.getMinX()
               + " " + env.getMinY()
               + "," + env.getMaxX()
               + " " + env.getMaxY()
               );
    buf.append(")'::box3d,");
    // [mmichaud 2012-03-14] make windows srid homogeneous with geometry srid
    // in case it is not defined
    String srid = getSRID(query.getSRSName());
    srid = srid==null? "ST_SRID(\"" + query.getGeometryAttributeName() + "\")" : srid;
    buf.append(srid).append(")");
    return buf.toString();
  }
}
