package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSMetadata;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.SQLException;

/**
 * Creates SQL query strings for a Spatial database. To be overloaded by classes
 * implementing a spatial database support.
 */
public class OracleSQLBuilder extends SpatialDatabasesSQLBuilder {

  public OracleSQLBuilder(SpatialDatabasesDSMetadata dbMetadata,
      SpatialReferenceSystemID defaultSRID, String[] colNames) {

    super(dbMetadata, defaultSRID, colNames);

  }

  /**
   * Builds a valid SQL spatial query with the given spatial filter.
   *
   * @param query
   * @return a SQL query to get column names
   */
  @Override
  public String getSQL(FilterQuery query) {
    StringBuilder qs = new StringBuilder();
        //HACK
    // surrond query by a rownum clause, used for limit
    qs.append("SELECT ROWNUM, ").append(getColumnListSpecifier(colNames, query.getGeometryAttributeName()));
    qs.append(" FROM ( ");
    qs.append("SELECT ");
    qs.append(getColumnListSpecifier(colNames, query.getGeometryAttributeName()));
    qs.append(" FROM ");
    // fixed by mmichaud on 2010-05-27 for mixed case dataset names
    qs.append("\"").append(query.getDatasetName().replaceAll("\\.", "\".\"")).append("\"");
    qs.append(" t WHERE ");

    qs.append(buildBoxFilter(query));

    String whereCond = query.getCondition();
    if (whereCond != null) {
      qs.append(" AND ");
      qs.append(whereCond);
    }
    qs.append(")");

    int limit = query.getLimit();
    if (limit != 0 && limit != Integer.MAX_VALUE) {
      qs.append(" where ROWNUM <= ").append(limit);
    }

//        JUMPWorkbench.getInstance().getFrame().log(
//            "SQL query to get Spatial table features:\n\t" 
//                + qs.toString(), this.getClass());
    return qs.toString();
  }

  /**
   * Returns the query allowing to test a DataStoreLayer: builds a query with
   * where clause and limit 0 to check where clause.
   *
   * @return
   */
  @Override
  public String getCheckSQL(DataStoreLayer dsLayer) {
    String s = "select ROWNUM FROM (select * FROM %s %s) where ROWNUM <=0";
    String wc = dsLayer.getWhereClause();
    if (wc != null && !wc.isEmpty()) {
      wc = " WHERE " + wc;
    } else {
      wc = "";
    }
    //System.out.println(qs);
    return String.format(s, dsLayer.getFullName(), wc);
  }

  /**
   * Returns the string representing a SQL column definition. Implementors
   * should take care of column names (case, quotes)
   *
   * @param colNames
   * @param geomColName
   * @return column list
   */
  @Override
  protected String getColumnListSpecifier(String[] colNames, String geomColName) {
    StringBuilder buf = new StringBuilder();
    buf.append(geomColName).append(" as ").append("\"").append(geomColName).append("\"");
    for (String colName : colNames) {
      if (!geomColName.equalsIgnoreCase(colName)) {
        buf.append(", \"").append(colName).append("\"");
      }
    }
    return buf.toString();
  }

  @Override
  protected String buildBoxFilter(FilterQuery query) {
    StringBuilder buf = new StringBuilder("1=1");

        // spatial query in Oracle is only available if geom column is indexed.
    // This information is found during datastoreDSMetadata init.
    // todo names can contain dots ?
    // todo: oracle always have a schema name ?
    try {
      if (super.getDbMetadata().isIndexed(query.getDatasetName(), query.getGeometryAttributeName())) {
        buf = new StringBuilder();
        Envelope env = query.getFilterGeometry().getEnvelopeInternal();

        // Example of Postgis SQL: where sdo_filter (geom, SDO_geometry(2003,2154,NULL,SDO_elem_info_array(1,1003,3),SDO_ordinate_array(100225,1002375,6549646,6810524))) = 'TRUE'
        String srid = getSRID(query.getSRSName()) == null ? "null" : getSRID(query.getSRSName());

        // fixed by mmichaud on 2010-05-27 for mixed case geometryColName names
        buf.append("sdo_filter(\"").append(query.getGeometryAttributeName()).append("\" , SDO_geometry(");
        buf.append("2003,").append(srid).append(",NULL,SDO_elem_info_array(1,1003,3),SDO_ordinate_array(");
        // force min/max values to avoid a ORA-01426: numeric overflow with some extents OJ can generate
        buf.append(env.getMinX()).append(", ").append(env.getMinY()).append(", ").append(env.getMaxX()).append(", ").append(env.getMaxY());
        buf.append(srid).append(")))='TRUE'");

        JUMPWorkbench.getInstance().getFrame().log(
            "SQL query fragment to get spatial table BBOX filter:\n\t"
            + buf.toString(), this.getClass());
      }
    } catch (SQLException ex) {
      JUMPWorkbench.getInstance().getFrame().log(
          "cannot guess if geo column is indexed, error: " + ex.getMessage(), this.getClass());
      //TODO: remove ?
      ex.printStackTrace();
    }

    return buf.toString();
  }
}
