package com.vividsolutions.jump.datastore;

import com.vividsolutions.jts.geom.*;

/**
 * A spatial filter {@link Query} on a {@link DataStoreConnection}.
 */
public class FilterQuery
    implements Query
{

  private String datasetName;
  private String[] propertyNames;
  private Geometry geom;
  private String condition;
  /**
   * Not all query processors need this.
   */
  private String geomAttrName = null;
  /**
   * For those query processors which need a CRS
   */
  private SpatialReferenceSystemID srid = new SpatialReferenceSystemID();

  public FilterQuery() {
  }

  public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
  public String getDatasetName() { return datasetName; }
  public void setPropertyNames(String[] propertyNames) { this.propertyNames = propertyNames; }
  public String[] getPropertyNames() { return propertyNames; }
  public void setFilterGeometry(Geometry geom) { this.geom = geom; }
  public Geometry getFilterGeometry() { return geom; }
  public void setCondition(String condition) { this.condition = condition; }
  public String getCondition() { return condition; }

  public void setGeometryAttributeName(String geomAttrName) { this.geomAttrName = geomAttrName; }
  public String getGeometryAttributeName() { return geomAttrName; }

  /**
   * Sets the SpatialReferenceSystem for a query.
   * This is optional; whether it is required depends on the datastore implemention.
   * Datastore drivers may set this themselves
   * and override any user settings.
   *
   * @param srid the SpatialReferenceSystem ID
   */
  public void setSRSName(SpatialReferenceSystemID srid) { this.srid = srid; }
  public SpatialReferenceSystemID getSRSName() { return srid; }
}