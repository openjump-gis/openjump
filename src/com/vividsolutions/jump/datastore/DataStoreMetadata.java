package com.vividsolutions.jump.datastore;

import com.vividsolutions.jts.geom.Envelope;

import java.sql.SQLException;
import java.util.List;

/**
 * Metadata about the datasets in the database
 */
public interface DataStoreMetadata {

  /**
   * list all tables
   */
  String[] getDatasetNames();

  List<GeometryColumn> getGeometryAttributes(String datasetName);

  /**
   * Returns columns of this dataset involved in the Primary Key. (added on
   * 2013-08-07)
   * 
   * @param datasetName
   *          the table name (optionally prefixed by a schema name)
   */
  List<PrimaryKeyColumn> getPrimaryKeyColumns(String datasetName);

  /**
   * @param datasetName
   *          table name (optionally prefixed by a schema name)
   * @param attributeName
   *          column containing the Geometry
   * @return May be null if the extents cannot be determined
   */
  Envelope getExtents(String datasetName, String attributeName);

  /**
   * get the SRID of a table's (geometry) column
   */
  SpatialReferenceSystemID getSRID(String datasetName, String colName)
      throws SQLException;

  /**
   * list columns of a table
   */
  String[] getColumnNames(String datasetName);
  
  /**
   * DataSoreConnection used by these metadata
   */
  DataStoreConnection getDataStoreConnection();
}