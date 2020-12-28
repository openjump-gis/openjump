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
   * @return an array of dataset names for this DataStore
   */
  String[] getDatasetNames();

  List<GeometryColumn> getGeometryAttributes(String datasetName);

  /**
   * Returns columns of this dataset involved in the Primary Key. (added on
   * 2013-08-07)
   * 
   * @param datasetName
   *          the table name (optionally prefixed by a schema name)
   * @return the list of columns to be used as a PrimaryKey
   * @throws SQLException if the server throws an Exception during Primary Key retrieval
   */
  List<PrimaryKeyColumn> getPrimaryKeyColumns(String datasetName)
      throws SQLException;

  /**
   * @param datasetName
   *          table name (optionally prefixed by a schema name)
   * @param attributeName
   *          column containing the Geometry
   * @return May be null if the extents cannot be determined
   */
  Envelope getExtents(String datasetName, String attributeName);

  /**
   * Get the SRID of a table's (geometry) column
   *
   * @param datasetName the dataset name
   * @param colName the column name
   * @return the SpatialReferenceSystemID for this column
   * @throws SQLException if the server throws an Exception during SRID retrieval
   */
  SpatialReferenceSystemID getSRID(String datasetName, String colName)
      throws SQLException;

  /**
   * list columns of a table
   * @param datasetName name of the table or dataset
   * @return the names of this dataset columns
   */
  String[] getColumnNames(String datasetName);
  
  /**
   * DataSoreConnection used by these metadata
   * @return a DataStoreConnection
   */
  DataStoreConnection getDataStoreConnection();
}