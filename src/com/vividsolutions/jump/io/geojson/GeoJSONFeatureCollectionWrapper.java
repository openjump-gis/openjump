package com.vividsolutions.jump.io.geojson;

import java.io.IOException;
import java.io.Writer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.geojson.GeoJsonWriter;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorUtil;
import com.vividsolutions.jump.util.Timer;

/**
 * a wrapper for a feature collection to do funky geojson stuff to/with
 *
 */
public class GeoJSONFeatureCollectionWrapper implements JSONStreamAware {
  MapGeoJsonGeometryReader geomReader = null;
  FlexibleFeatureSchema featureSchema = null;
  FeatureCollection featureCollection = null;
  List<String> columnsWithMixedValues = new LinkedList<String>();

  /**
   * create a new empty FeatureCollection wrapper
   */
  public GeoJSONFeatureCollectionWrapper() {
    this.featureSchema = new FlexibleFeatureSchema();
    this.featureCollection = new FeatureDataset(featureSchema);
  }

  /**
   * create a wrapper for an existing FeatureCollection
   */
  public GeoJSONFeatureCollectionWrapper(FeatureCollection fc) {
    this.featureSchema = new FlexibleFeatureSchema(fc.getFeatureSchema());
    this.featureCollection = fc;
  }

  /**
   * add a Feature defined by given JSON-simple map the to the collection
   */
  public void add(Map featureMap) throws Exception {

    // this type of feature "autoextends" by returning null for undefined
    // attribs
    Feature feature = new BasicFeature(featureSchema) {

      @Override
      public Object getAttribute(int i) {
        if (i < 0)
          throw new InvalidParameterException(
              "index must be greater or equal zero");

        Object attrib = null;

        Object[] attributes = getAttributes();
        // only grab attrib if stack holds it already
        if (i < attributes.length)
          attrib = attributes[i];

        // OJ does not allow null geoms!
        if (i == featureSchema.getGeometryIndex()) {
          // create and set an empty geom
          if (attrib == null) {
            attrib = featureSchema.createEmptyGeometry();
            setGeometry((Geometry) attrib);
          }
        }
        // enforce String if schema says so
        else if (featureSchema.getAttributeType(i).equals(AttributeType.STRING)
            && attrib != null && !(attrib instanceof String)) {
          attrib = String.valueOf(attrib);
        }

        return attrib;
      }

      /**
       * setting an attribute, fixing the underlying array in case the schema
       * changed inbetween
       */
      public void setAttribute(int attributeIndex, Object newAttribute) {
        FeatureSchema schema = getSchema();
        Object[] oldAttribs = getAttributes();
        // add fields if schema changed in between
        int diffCount = schema.getAttributeCount() - oldAttribs.length;
        if (diffCount > 0) {
          List attributes = new ArrayList(Arrays.asList(oldAttribs));
          attributes.addAll(Arrays.asList(new Object[diffCount]));
          super.setAttributes(attributes.toArray());
        }
        super.setAttribute(attributeIndex, newAttribute);
      }

      /**
       * setting the geometry by explicitly using the flexible setAttribute()
       * method above
       */
      public void setGeometry(Geometry geometry) {
        setAttribute(getSchema().getGeometryIndex(), geometry);
      }

      /**
       * getting the geometry by explicitly using the flexible getAttribute()
       * method above
       */
      public Geometry getGeometry() {
        return (Geometry) getAttribute(getSchema().getGeometryIndex());
      }
    };

    // parse geometry
    if (featureMap.containsKey(GeoJSONConstants.GEOMETRY)
        && (featureMap.get(GeoJSONConstants.GEOMETRY) instanceof Map)) {
      // add geom attribute to schema if none so far
      if (featureSchema.getGeometryIndex() < 0) {
        featureSchema.addAttribute("Geometry", AttributeType.GEOMETRY);
      }

      Map geometryMap = (Map) featureMap.get(GeoJSONConstants.GEOMETRY);
      // initialize geom reader
      if (geomReader == null)
        geomReader = new MapGeoJsonGeometryReader();

      Geometry geom = geomReader.read(geometryMap);
      // memorize a geomtype from the dataset
      if (featureSchema.getGeometryType() == null)
        featureSchema.setGeometryType(geom.getClass());

      feature.setGeometry(geom);
    }

    // parse attributes
    Map<String, Object> attribsMap = null;
    if (featureMap.containsKey(GeoJSONConstants.PROPERTIES)
        && featureMap.get(GeoJSONConstants.PROPERTIES) instanceof Map) {
      attribsMap = (Map) featureMap.get(GeoJSONConstants.PROPERTIES);
      // iterate over this feature's attribs
      for (String key : attribsMap.keySet()) {
        Object value = attribsMap.get(key);
        AttributeType type = toAttributeType(value);

        // extend schema if attrib is unknown
        if (!featureSchema.hasAttribute(key)) {
          featureSchema.addAttribute(key, type);
        }
        // detect mixedType columns to fixup Schema later
        else if (!columnsWithMixedValues.contains(key)
            && featureSchema.getAttributeType(key) != type) {
          // this column had null until now
          if (featureSchema.getAttributeType(key) == ATTRIBUTETYPE_NULL) {
            featureSchema.setAttributeType(key, type);
          }
          // this column hosts mixed attrib types eg. String/Long, NULL values are allowed though
          else if (type != ATTRIBUTETYPE_NULL){
            columnsWithMixedValues.add(key);
          }
        }

        // add the attribute value to the feature
        feature.setAttribute(key, value);
      }
    }

    featureCollection.add(feature);
  }

  static class Null extends Object {
  };

  static class NullAttributeType extends AttributeType {
    public NullAttributeType() {
      super("NULL", Null.class);
    }
  };

  public static final AttributeType ATTRIBUTETYPE_NULL = new NullAttributeType();

  public static AttributeType toAttributeType(Object value) {
    // for null values we use temporarily a custom attrib type which get's fixed
    // in getFeatCol()
    if (value == null)
      return ATTRIBUTETYPE_NULL;
    AttributeType type = AttributeType.toAttributeType(value.getClass());
    // unknown mappings return null, we assume Object then
    if (type == null)
      type = AttributeType.OBJECT;
    return type;
  }

  public int size() {
    return featureCollection.size();
  }

  /**
   * we need to fixup the feature schema before the collection is ready to be
   * used
   * 
   * @return
   */
  public FeatureCollection getFeatureCollection() {
    // set type to String for mixed columns
    for (String key : new LinkedList<String>(columnsWithMixedValues) ) {
      featureSchema.setAttributeType(featureSchema.getAttributeIndex(key),
          AttributeType.STRING);
      columnsWithMixedValues.remove(key);
    }
    // set type to String for the temporary internal ATTRIBUTETYPE_NULL columns
    for (int i = 0; i < featureSchema.getAttributeCount(); i++) {
      AttributeType type = featureSchema.getAttributeType(i);
      if (type == ATTRIBUTETYPE_NULL)
        featureSchema.setAttributeType(i, AttributeType.STRING);
    }
    return featureCollection;
  }

  @Override
  public void writeJSONString(Writer out) throws IOException {
    writeJSONString(out, null);
  }

  public void writeJSONString(Writer out, TaskMonitor monitor)
      throws IOException {
    out.write("{\n");
    out.write("\"type\": \"" + GeoJSONConstants.TYPE_FEATURECOLLECTION
        + "\",\n\n");
    out.write("\"" + GeoJSONConstants.FEATURES + "\": [\n");

    long milliSeconds = 0;
    int count = 0;
    boolean first = true;
    String[] featureFields = new String[] { GeoJSONConstants.TYPE,
        GeoJSONConstants.PROPERTIES, GeoJSONConstants.GEOMETRY };
    TaskMonitorUtil.report(monitor,
        I18N.getMessage("GeoJSONWriter.writing-features"));
    for (Feature feature : featureCollection.getFeatures()) {

      if (TaskMonitorUtil.isCancelRequested(monitor))
        break;

      // write separator after first dataset
      if (!first)
        out.write(",\n");

      // only first dataset writes NULL values to keep attribute order
      String featureJson = toJSONString(feature, first);
      out.write(featureJson);

      long now = Timer.milliSecondsSince(0);
      count++;
      // show status every .5s
      if (now - 500 >= milliSeconds) {
        milliSeconds = now;
        TaskMonitorUtil.report(monitor, count, size(), "");
      }

      // unset first marker
      if (first)
        first = false;
    }
    out.write("\n]");

    out.write("\n\n}");
  }

  private static String toJSONString(Feature feature) {
    return toJSONString(feature, false);
  }

  private static String toJSONString(Feature feature, boolean saveNullValues) {
    String propertiesJson = null, geometryJson = null;
    FeatureSchema schema = feature.getSchema();

    for (int i = 0; i < schema.getAttributeCount(); i++) {
      String name = schema.getAttributeName(i);
      AttributeType type = schema.getAttributeType(i);
      Object value = feature.getAttribute(i);

      // geometry to json
      if (i == schema.getGeometryIndex()) {
        Geometry geometry = (Geometry) value;
        if (geometry != null)
          geometryJson = new GeoJsonWriter().write(geometry);
      } 
      // attrib to json
      else {
        // we do NOT save null values to minimize the file size
        if (!saveNullValues && value == null)
          continue;

        // Date objects should be saved quoted in String representation
        if (type.equals(AttributeType.DATE))
          value = String.valueOf(value);

        String json = JSONObject.toString(name, value);
        propertiesJson = propertiesJson != null ? propertiesJson + ", " + json
            : json;
      }
    }

    // the GeoJSON specs expect a geometry to be written, it might be empty though
    if (geometryJson != null)
      geometryJson = "\"" + GeoJSONConstants.GEOMETRY + "\": " + geometryJson;
    else
      geometryJson = GeoJSONConstants.EMPTY_GEOMETRY;

    if (propertiesJson != null)
      propertiesJson = "\"" + GeoJSONConstants.PROPERTIES + "\": { "
          + propertiesJson + " }";

    return "{ \"" + GeoJSONConstants.TYPE + "\": \""
        + GeoJSONConstants.TYPE_FEATURE + "\""
        + (propertiesJson != null ? ", " + propertiesJson : "")
        + (geometryJson != null ? ", " + geometryJson : "") + " }";
  }
}
