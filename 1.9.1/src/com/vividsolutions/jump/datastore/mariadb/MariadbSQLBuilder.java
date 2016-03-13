package com.vividsolutions.jump.datastore.mariadb;

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
public class MariadbSQLBuilder extends SpatialDatabasesSQLBuilder {

  public MariadbSQLBuilder(SpatialDatabasesDSMetadata dsMetadata, SpatialReferenceSystemID defaultSRID, String[] colNames) {
    super(dsMetadata, defaultSRID, colNames);
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
    qs.append(" FROM ").append(query.getDatasetName()).append("");
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
    //buf.append("ST_AsBinary(").append(geomColName).append(") as ").append(geomColName);
    // Nicolas Ribot: 12 dec: Native reader supported now
    buf.append(geomColName);
    for (String colName : colNames) {
      if (! geomColName.equalsIgnoreCase(colName)) {
        buf.append(",").append(colName).append("");
      }
    }
    return buf.toString();
  }

  @Override
  protected String buildBoxFilter(FilterQuery query) {
    Envelope env = query.getFilterGeometry().getEnvelopeInternal();

    // Example of MariaDB SQL: where st_Intersects(b.geom, st_polygonFromText('POLYGON((4 4, 5 4, 5 5, 4 5, 4 4))'))
    // Nicolas Ribot: 23 dec: MySQL 5.7.10 checks geom SRID and reject the query if bbox has not correct srid
    String s = this.defaultSRID == null ? "0" : this.defaultSRID.getString();
    StringBuilder buf = new StringBuilder();
    buf.append("st_intersects(").append(query.getGeometryAttributeName()).append(", st_polygonFromText('POLYGON((");
    buf.append(env.getMinX()).append(" ").append(env.getMinY()).append(",")
        .append(env.getMaxX()).append(" ").append(env.getMinY()).append(",")
        .append(env.getMaxX()).append(" ").append(env.getMaxY()).append(",")
        .append(env.getMinX()).append(" ").append(env.getMaxY()).append(",")
        .append(env.getMinX()).append(" ").append(env.getMinY());
    buf.append("))', ").append(s).append("))");
    return buf.toString();
  }
}
