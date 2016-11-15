package com.vividsolutions.jump.io.geojson;

public class GeoJSONConstants {
  public static final String CHARSET = "UTF-8";

  public static final String PROPERTIES = "properties";
  public static final String GEOMETRY = "geometry";
  public static final String FEATURES = "features";
  public static final String TYPE = "type";

  public static final String TYPE_FEATURE = "Feature";
  public static final String TYPE_FEATURECOLLECTION = "FeatureCollection";

  // for performance reasons, prevent concatenating the same string
  public static final String EMPTY_GEOMETRY = "\"" + GEOMETRY
      + "\": {\"type\":\"GeometryCollection\",\"geometries\":[]}";
  public static final String EMPTY_PROPERTIES = "\"" + PROPERTIES + "\" : null";
}
