package com.vividsolutions.jump.datastore;

/**
 * Represents an ID used by DataStores to refer to a
 * particular Spatial Reference System.
 * Often an integer value.
 */
public class SpatialReferenceSystemID {

  private String sridString = null;

  public SpatialReferenceSystemID() {
  }

  public SpatialReferenceSystemID(String sridString) {
    this.sridString = sridString;
  }

  public SpatialReferenceSystemID(int srid) {
    this.sridString = Integer.toString(srid);
  }

  public int getInt() {
    if (sridString == null)
      return -1;
    // could do something cleverer here, like try and extract an integer
    return Integer.parseInt(sridString);
  }

  public String getString()
  {
    return sridString;
  }

  public boolean isNull() { return sridString == null; }
}