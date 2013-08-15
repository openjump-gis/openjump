package com.vividsolutions.jump.datastore;

import com.vividsolutions.jts.geom.Envelope;
import java.util.List;

/**
 * Metadata about the datasets in the database
 */
public interface DataStoreMetadata {

    String[] getDatasetNames();

    //@Deprecated
    //String[] getGeometryAttributeNames(String datasetName);

    List<GeometryColumn> getGeometryAttributes(String datasetName);

    /**
     * Returns columns of this dataset involved in the Primary Key.
     * (added on 2013-08-07)
     * @param datasetName the table name (optionally prefixed by a schema name)
     * @return
     */
    List<PrimaryKeyColumn> getPrimaryKeyColumns(String datasetName);

    /**
     * @param datasetName table name (optionally prefixed by a schema name)
     * @param attributeName column containing the Geometry
     * @return May be null if the extents cannot be determined
     */
    Envelope getExtents(String datasetName, String attributeName);
}