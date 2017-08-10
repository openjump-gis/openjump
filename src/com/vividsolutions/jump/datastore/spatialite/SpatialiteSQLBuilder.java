package com.vividsolutions.jump.datastore.spatialite;

import java.util.Locale;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.workbench.JUMPWorkbench;

/**
 * Creates SQL query strings for a Spatial database. To be overloaded by classes
 * implementing a spatial database support.
 */
public class SpatialiteSQLBuilder extends SpatialDatabasesSQLBuilder {

  private String datasetName;

  public SpatialiteSQLBuilder(SpatialiteDSMetadata dsMetadata, SpatialReferenceSystemID defaultSRID, String[] colNames) {
    super(dsMetadata, defaultSRID, colNames);
  }

  /**
   * Builds a valid SQL spatial query with the given spatial filter.
   *
   * @param query
   * @return a SQL query to get column names
   */
  @Override
  public String getSQL(FilterQuery query) {
    this.datasetName = query.getDatasetName();
    StringBuilder qs = new StringBuilder();
    //HACK
    String ret = "SELECT %s FROM %s WHERE %s AND (%s) %s";
    String cols = getColumnListSpecifier(colNames, query.getGeometryAttributeName());
    String bbox = buildBoxFilter(query);
    String and = query.getCondition() == null ? "1" : query.getCondition();
    String lim = (query.getLimit() != 0 && query.getLimit() != Integer.MAX_VALUE) ? " LIMIT " + query.getLimit() : "";

    //System.out.println(qs);
    String s = String.format(ret, cols, this.datasetName, bbox, and, lim);
    JUMPWorkbench.getInstance().getFrame().log(
        "SQL query to get Spatial table features:\n\t"
        + s, this.getClass());

    return s;
  }

  /**
   * Returns the query allowing to test a DataStoreLayer: builds a query with
   * where clause and limit 0 to check where clause.
   *
   * @return
   */
  @Override
  public String getCheckSQL(DataStoreLayer dsLayer) {
    // select * crashes Java with Spatialite extension loaded ??
    String s = "select * FROM %s %s LIMIT 0";
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
    // Added double quotes around each column name in order to read mixed case table names
    // correctly [mmichaud 2007-05-13]
    StringBuilder buf = new StringBuilder();
    SpatialiteDSMetadata dsm = (SpatialiteDSMetadata) getDbMetadata();
    GeometricColumnType gcType = dsm.getGeoColTypesdMap().get(this.datasetName.toLowerCase() + "." + geomColName.toLowerCase());
    String s = null;
    switch (gcType) {
      case SPATIALITE:
        s = geomColName;
        break;
      case WKB:
        s = String.format("st_geomFromWkb(%s) as %s", geomColName, geomColName);
        break;
      case WKT:
        s = String.format("st_geomFromText(%s) as %s", geomColName, geomColName);
        break;
    }
    // TODO: use previous code or remove it after check:
    // geo col should be returned in its default type and handled by the converter
    // according to the geo column type (avoid conversion overhead)

    buf.append("").append(geomColName);
    for (String colName : colNames) {
      if (!geomColName.equalsIgnoreCase(colName)) {
        buf.append(",").append(colName);
      }
    }
    return buf.toString();
  }

  @Override
  protected String buildBoxFilter(FilterQuery query) {
    Envelope env = query.getFilterGeometry().getEnvelopeInternal();
    String ret = "1";
    String indexQuery = buildSpatialIndexFilter(query);
    // Example of Spatialite SQL: 
    SpatialiteDSMetadata dsm = (SpatialiteDSMetadata) getDbMetadata();
    
    if (dsm.isSpatialiteLoaded()) {
      GeometricColumnType gcType = dsm.getGeoColTypesdMap().get(
          query.getDatasetName().toLowerCase() + "." + query.getGeometryAttributeName().toLowerCase());
      // use Locale.US to enforce floating point number with a dot separator
      if (gcType == GeometricColumnType.SPATIALITE) {
        ret = String.format(Locale.US, "st_envIntersects(CastAutomagic(%s), %f,%f,%f,%f)", query.getGeometryAttributeName(), env.getMinX(),
            env.getMinY(), env.getMaxX(), env.getMaxY());
      } else if (gcType == GeometricColumnType.WKB) {
        ret = String.format(Locale.US, "st_envIntersects(st_geomFromWkb(%s), %f,%f,%f,%f)", query.getGeometryAttributeName(), env.getMinX(),
            env.getMinY(), env.getMaxX(), env.getMaxY());
      } else if (gcType == GeometricColumnType.WKT) {
        ret = String.format(Locale.US, "st_envIntersects(st_geomFromText(%s), %f,%f,%f,%f)", query.getGeometryAttributeName(), env.getMinX(),
            env.getMinY(), env.getMaxX(), env.getMaxY());
      } else {
        JUMPWorkbench.getInstance().getFrame().log(
          "Spatialite SQL builder: invalid geometric column type: " + gcType ,
            this.getClass());
      }
    }
    ret += indexQuery;
    return ret;
  }

  /**
   * Builds a SQL filter to use spatial index, if concerned geometry column supports
   * it.
   * @param query the query to build the filter for
   * @return the spatial index filter query, or an empty string if geometry 
   * column is not spatially indexed
   */
  protected String buildSpatialIndexFilter(FilterQuery query) {
    String ret = "";
    
    Envelope env = query.getFilterGeometry().getEnvelopeInternal();
    SpatialiteDSMetadata dsm = (SpatialiteDSMetadata) getDbMetadata();
    // test if geometry column is indexed, if so, builds special query according to type:
    GeometryColumn gc = dsm.getGeometryColumn(query.getDatasetName(), query.getGeometryAttributeName());
    if (gc.isIndexed()) {
      if (dsm.getGeometryColumnsLayout() == GeometryColumnsLayout.OGC_GEOPACKAGE_LAYOUT) {
        ret = String.format(Locale.US,
          " AND ROWID IN (SELECT id FROM rtree_%s_%s WHERE minx > %f and maxx < %f and miny > %f and maxy < %f) ",
          query.getDatasetName(), query.getGeometryAttributeName(), 
          env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY());
      } else if (dsm.isSpatialiteLoaded()) {
        // always use spatialIndex table if spatialite
        ret = String.format(Locale.US,
          " AND ROWID IN (SELECT ROWID FROM SpatialIndex WHERE f_table_name = '%s' AND search_frame = BuildMbr(%f,%f,%f,%f))",
          query.getDatasetName(), env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
      } else {
        // TODO other cases: no idx
        ret = "";
      }
    }
    
    return ret;
  }
}
