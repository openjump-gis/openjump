package com.vividsolutions.jump.datastore;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Metadata about the datasets in the database
 */
public interface DataStoreMetadata
{
  String[] getDatasetNames();
  String[] getGeometryAttributeNames(String datasetName);

  /**
   * @param datasetName
   * @param attributeName
   * @return May be null if the extents cannot be determined
   */
  Envelope getExtents(String datasetName, String attributeName);
}