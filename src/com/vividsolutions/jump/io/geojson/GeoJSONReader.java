package com.vividsolutions.jump.io.geojson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openjump.core.ui.util.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.geojson.GeoJsonConstants;
import com.vividsolutions.jts.io.geojson.GeoJsonReader;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.AbstractJUMPReader;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.IllegalParametersException;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorUtil;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.Timer;
import com.vividsolutions.jump.workbench.Logger;

public class GeoJSONReader extends AbstractJUMPReader {

  @Override
  public FeatureCollection read(DriverProperties dp) throws Exception {

    String inputUri = dp.getProperty(DataSource.URI_KEY);
    if (inputUri == null) {
      throw new IllegalParametersException(
          "call to GeoJSONReader.read() has DataProperties w/o an InputUri specified");
    }

    InputStream in = null;
    InputStreamReader inReader = null;
    Reader bufReader = null;
    // create a new geojson capable feature collection
    GeoJSONFeatureCollectionWrapper fcwrap = new GeoJSONFeatureCollectionWrapper();
    try {
      URI uri = new URI(inputUri);

      in = CompressedFile.openFile(uri);
      inReader = new InputStreamReader(in, GeoJSONConstants.CHARSET);
      bufReader = new BufferedReader(inReader);

      // parse and create features via content handler
      Transformer t = new Transformer(fcwrap);
      new JSONParser().parse(bufReader, t);
    } catch (Exception e) {
      // collect exception for later
      addException(e);
    } finally {
      FileUtil.close(bufReader);
      FileUtil.close(inReader);
      FileUtil.close(in);
    }

    // we return nothing if the read was cancelled
    return TaskMonitorUtil.isCancelRequested(getTaskMonitor()) ? null : fcwrap
        .getFeatureCollection();
  }

  /**
   * a sax like content handler to create features already during parsing the
   * json file (saves memory and processing loops)
   */
  class Transformer implements ContentHandler {
    private Stack valueStack;
    private Object featsId = null;
    private GeoJSONFeatureCollectionWrapper fcwrap = null;
    private long milliSeconds = 0;

    public Transformer(GeoJSONFeatureCollectionWrapper fcwrap) {
      this.fcwrap = fcwrap;
    }

    public Stack getResult() {
      if (valueStack == null || valueStack.size() == 0)
        return null;
      return valueStack;
    }

    private boolean notCancelled() {
      return !getTaskMonitor().isCancelRequested();
    }

    /**
     * create JTS features from a json-simple map
     * 
     * @param featureMapList
     */
    private void addFeatures(List<Map> featureMapList) {
      TaskMonitor monitor = getTaskMonitor();

      for (Map featureMap : featureMapList) {
        try {
          fcwrap.add(featureMap);
          long now = Timer.milliSecondsSince(0);
          // show status every .5s
          if (now - 500 >= milliSeconds) {
            milliSeconds = now;
            TaskMonitorUtil.report(
                monitor,
                I18N.getMessage("Reader.parsed-{0}-features",
                    String.format("%,10d", fcwrap.size())));
          }
        } catch (Exception e) {
          addException(new IOException(JSONObject.toJSONString(featureMap), e));
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
      return notCancelled();
    }

    public boolean startArray() throws ParseException, IOException {
      List array = new LinkedList();
      consumeValue(array);
      valueStack.push(array);
      return notCancelled();
    }

    public boolean endArray() throws ParseException, IOException {
      trackBack();
      return notCancelled();
    }

    public void startJSON() throws ParseException, IOException {
      milliSeconds = Timer.milliSecondsSince(0);
      valueStack = new Stack();
    }

    public void endJSON() throws ParseException, IOException {
      // add rest of features
      for (Object object : valueStack) {
        List<Map> featureList = (List) ((Map) object)
            .get(GeoJSONConstants.FEATURES);
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

      return notCancelled();
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
      return notCancelled();
    }

    public boolean startObjectEntry(String key) throws ParseException,
        IOException {
      valueStack.push(key);
      return notCancelled();
    }

    public boolean endObjectEntry() throws ParseException, IOException {
      Object value = valueStack.pop();
      Object key = valueStack.pop();
      Map parent = (Map) valueStack.peek();
      parent.put(key, value);
      return notCancelled();
    }

  }
}

/**
 * utility class to allow creating geometries directly from simple-json maps for
 * performance reasons. TODO: this should probably be implemented directly in
 * JTS-io's GeoJsonReader
 */
class MapGeoJsonGeometryReader extends
    com.vividsolutions.jts.io.geojson.GeoJsonReader {
  GeometryFactory geometryFactory = null;
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
   * @throws com.vividsolutions.jts.io.ParseException
   * @throws ClassNotFoundException 
   */
  public Geometry read(Map geometryMap) throws IllegalAccessException,
      IllegalArgumentException, InvocationTargetException,
      com.vividsolutions.jts.io.ParseException, ClassNotFoundException {
    if (this.geometryFactory == null) {
      geometryFactory = (GeometryFactory) m2.invoke(this, geometryMap);
    }

    Object coords = geometryMap.get(GeoJsonConstants.NAME_COORDINATES);
    // are we a list of objects?
    if (!(coords instanceof List))
      throw new com.vividsolutions.jts.io.ParseException(
          GeoJsonConstants.NAME_COORDINATES + " is not a list: "
              + JSONObject.toJSONString(geometryMap));
    // are we an empty list? OJ allows empty geometries, so do we
    if (((List)coords).isEmpty()){
      String type = (String) geometryMap.get(GeoJsonConstants.NAME_TYPE);
        return GeometryUtils.createEmptyGeometry(type, geometryFactory);
    }
    
    return (Geometry) m.invoke(this, geometryMap, geometryFactory);
  }
}

/**
 * a FeatureSchema implementation that allows changing attrib types without
 * cloning the whole schema first
 */
class FlexibleFeatureSchema extends FeatureSchema {
  Class geometryClass = null;
  GeometryFactory geometryFactory = new GeometryFactory();

  public FlexibleFeatureSchema() {
  }

  public FlexibleFeatureSchema(FeatureSchema featureSchema) {
    super(featureSchema);
  }

  public void setAttributeType(int attributeIndex, AttributeType type) {
    attributeTypes.set(attributeIndex, type);
  }

  public void setAttributeType(String name, AttributeType type) {
    setAttributeType(super.getAttributeIndex(name), type);
  }

  public void setGeometryType(Class clazz) {
    geometryClass = clazz;
  }

  public Class getGeometryType() {
    return geometryClass;
  }

  /**
   * creates an empty geometry matching the geom type set already or an empty
   * geom collection if that fails
   * 
   * @return geometry
   */
  public Geometry createEmptyGeometry() {
    if (geometryClass != null) {
      try {
        return GeometryUtils
            .createEmptyGeometry(geometryClass, geometryFactory);
      } catch (Exception e) {
        Logger.debug(e);
      }
    }

    return geometryFactory.createGeometryCollection(null);
  }

}
