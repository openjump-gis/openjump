package com.vividsolutions.jump.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.geojson.GeoJsonConstants;
import com.vividsolutions.jts.io.geojson.GeoJsonReader;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.FileUtil;

public class GeoJSONReader extends AbstractJUMPReader {

  @Override
  public FeatureCollection read(DriverProperties dp) throws Exception {

    String inputUri = dp.getProperty("Uri");
    if (inputUri == null) {
      throw new IllegalParametersException(
          "call to GeoJSONReader.read() has DataProperties w/o an InputUri specified");
    }

    InputStream in = null;
    InputStreamReader inReader = null;
    Reader bufReader = null;
    GeoJSONFeatureCollectionWrapper fcwrap = new GeoJSONFeatureCollectionWrapper();
    try {
      in = CompressedFile.openFile(new URI(inputUri));
      inReader = new InputStreamReader(in, "UTF-8");
      bufReader = new BufferedReader(inReader);

      // parse and create features via content handler
      Transformer t = new Transformer(fcwrap);
      new JSONParser().parse(bufReader, t);

    } finally {
      FileUtil.close(bufReader);
      FileUtil.close(inReader);
      FileUtil.close(in);
    }

    return fcwrap.getFeatureCollection();
  }
}

/**
 * utility class to allow creating geometries directly from simple-json maps
 */
class MapGeoJsonGeometryReader extends GeoJsonReader {
  GeometryFactory gf = null;
  Method m, m2;

  /**
   * this is a hack and should be communicated to JTS. we need an exposed method
   * to feed the simple-json map to
   * 
   * @throws NoSuchMethodException
   * @throws SecurityException
   */
  public MapGeoJsonGeometryReader() throws NoSuchMethodException,
      SecurityException {
    m = GeoJsonReader.class.getDeclaredMethod("create", Map.class,
        GeometryFactory.class);
    m.setAccessible(true);
    m2 = GeoJsonReader.class.getDeclaredMethod("getGeometryFactory", Map.class);
    m2.setAccessible(true);
  }

  /**
   * create a JTS geometry from a simple-json map
   * 
   * @param geometryMap
   * @return
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   */
  public Geometry read(Map geometryMap) throws IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    GeometryFactory geometryFactory = null;
    if (this.gf == null) {
      geometryFactory = (GeometryFactory) m2.invoke(this, geometryMap);
    } else {
      geometryFactory = this.gf;
    }

    return (Geometry) m.invoke(this, geometryMap, geometryFactory);
  }
}

/**
 * a wrapper for a feature collection to do funky geojson stuff to/with
 *
 */
class GeoJSONFeatureCollectionWrapper {
  MapGeoJsonGeometryReader geomReader = null;
  FeatureSchema featureSchema = null;
  List<Feature> featureList = new LinkedList<Feature>();
  FeatureCollection featureCollection = null;

  public GeoJSONFeatureCollectionWrapper() {
    featureSchema = new FeatureSchema();
  }

  public GeoJSONFeatureCollectionWrapper(FeatureCollection fc) {
    this.featureSchema = fc.getFeatureSchema();
    this.featureCollection = fc;
  }

  public void add(Map featureMap) throws Exception {

    // parse geometry
    Geometry geom = null;
    if (featureMap.containsKey(GeoJSONConstants.GEOMETRY)
        && featureMap.get(GeoJSONConstants.GEOMETRY) instanceof Map) {
      Map geometryMap = (Map) featureMap.get(GeoJSONConstants.GEOMETRY);

      if (geomReader == null)
        geomReader = new MapGeoJsonGeometryReader();

      if (featureSchema.getGeometryIndex() < 0) {
        featureSchema.addAttribute("Geometry", AttributeType.GEOMETRY);
      }

      geom = geomReader.read(geometryMap);
    }

    // parse attributes
    Map attribsMap = null;
    if (featureMap.containsKey(GeoJSONConstants.PROPERTIES)
        && featureMap.get(GeoJSONConstants.PROPERTIES) instanceof Map) {
      attribsMap = (Map) featureMap.get(GeoJSONConstants.PROPERTIES);
      for (Object key : attribsMap.keySet()) {
        String attribName = key.toString();
        // extend schema if attrib is unknown
        if (!featureSchema.hasAttribute(attribName)) {
          featureSchema.addAttribute(attribName, AttributeType.STRING);
        }
      }
    }

    // this type of feature "autoextends" by returning null for unknown attribs
    Feature feature = new BasicFeature(featureSchema) {
      public Object getAttribute(int i) {
        Object[] attributes = getAttributes();
        if (i >= attributes.length)
          return null;

        return attributes[i];
      }
    };

    // set geo, if any
    if (geom != null)
      feature.setGeometry(geom);

    // set attribs, if any
    if (attribsMap != null) {
      for (Object key : attribsMap.keySet()) {
        String attribName = key.toString();
        feature.setAttribute(attribName, attribsMap.get(attribName));
      }
    }

    featureList.add(feature);
  }

  public FeatureCollection getFeatureCollection() {
    return new FeatureDataset(featureList, featureSchema);
  }
}

class Transformer implements ContentHandler {
  private Stack valueStack;
  private Object featsId = null;
  private GeoJSONFeatureCollectionWrapper fcwrap = null;

  public Transformer(GeoJSONFeatureCollectionWrapper fcwrap) {
    super();
    this.fcwrap = fcwrap;
  }

  public Stack getResult() {
    if (valueStack == null || valueStack.size() == 0)
      return null;
    return valueStack;
  }

  private void addFeatures(List<Map> featureMapList) throws IOException {
    for (Map featureMap : featureMapList) {
      try {
        fcwrap.add(featureMap);
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  private void trackBack() {
    if (valueStack.size() > 1) {
      Object value = valueStack.pop();
      Object prev = valueStack.peek();
      if (prev instanceof String) {
        valueStack.push(value);
      }
    }
  }

  private void consumeValue(Object value) {
    if (valueStack.size() == 0)
      valueStack.push(value);
    else {
      Object prev = valueStack.peek();
      if (prev instanceof List) {
        List array = (List) prev;
        array.add(value);
      } else {
        valueStack.push(value);
      }
    }
  }

  public boolean primitive(Object value) throws ParseException, IOException {
    consumeValue(value);
    return true;
  }

  public boolean startArray() throws ParseException, IOException {
    List array = new LinkedList();
    consumeValue(array);
    valueStack.push(array);
    return true;
  }

  public boolean endArray() throws ParseException, IOException {
    trackBack();
    return true;
  }

  public void startJSON() throws ParseException, IOException {
    valueStack = new Stack();
  }

  public void endJSON() throws ParseException, IOException {
    // add rest of features
    for (Object object : valueStack) {
      List<Map> featureList = (List) ((Map) object).get("features");
      addFeatures(featureList);
      featureList.clear();
    }

  }

  public boolean startObject() throws ParseException, IOException {
    // memorize feature object for processing in endObject
    if (featsId == null
        && valueStack.size() > 2
        && valueStack.elementAt(valueStack.size() - 3).equals(
            GeoJSONConstants.FEATURES)) {
      featsId = valueStack.elementAt(valueStack.size() - 3);
    }

    Map object = new LinkedHashMap();
    consumeValue(object);
    valueStack.push(object);

    return true;
  }

  public boolean endObject() throws ParseException, IOException {
    trackBack();

    // at this point we just finished parsing a whole feature object
    if (featsId != null && valueStack.size() > 2
        && valueStack.elementAt(valueStack.size() - 3) == featsId) {
      List<Map> featureList = (List) valueStack
          .elementAt(valueStack.size() - 2);

      // process by batches of ten and the rest in endJSON()
      if (featureList.size() >= 10) {
        addFeatures(featureList);
        featureList.clear();
      }
    }
    return true;
  }

  public boolean startObjectEntry(String key) throws ParseException,
      IOException {
    valueStack.push(key);
    return true;
  }

  public boolean endObjectEntry() throws ParseException, IOException {
    Object value = valueStack.pop();
    Object key = valueStack.pop();
    Map parent = (Map) valueStack.peek();
    parent.put(key, value);
    return true;
  }

}
