package com.vividsolutions.jump.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.geojson.GeoJsonReader;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorUtil;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.Timer;

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
      URI uri = new URI(inputUri);

      in = CompressedFile.openFile(uri);
      inReader = new InputStreamReader(in, "UTF-8");
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
    private long milliSeconds = 0;;

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

    private void addFeatures(List<Map> featureMapList) {
      TaskMonitor monitor = getTaskMonitor();

      for (Map featureMap : featureMapList) {
        try {
          fcwrap.add(featureMap);
          long now = Timer.milliSecondsSince(0);
          // show status every 1s
          if (now - 1000 >= milliSeconds) {
            milliSeconds = now;
            TaskMonitorUtil.report(monitor,
                I18N.getMessage("com.vividsolutions.jump.io.GeoJSONReader.parsed-{0}-features",
                    String.format("%,10d", fcwrap.size())));
          }
        } catch (Exception e) {
          addException(e);
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
 * a FeatureSchema implementation that allows changing attrib types without
 * cloning the whole schema first
 */
class FlexibleFeatureSchema extends FeatureSchema {
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
}

/**
 * a wrapper for a feature collection to do funky geojson stuff to/with
 *
 */
class GeoJSONFeatureCollectionWrapper {
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
        Object[] attributes = getAttributes();
        if (i >= attributes.length)
          return null;

        return attributes[i];
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
    };

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
        // System.out.println(value+"/"+(value!=null?value.getClass():""));
        String attribName = key.toString();
        // extend schema if attrib is unknown
        if (!featureSchema.hasAttribute(attribName)) {
          Class clazz = value != null ? value.getClass() : Object.class;
          featureSchema.addAttribute(attribName,
              AttributeType.toAttributeType(clazz));
          ;
        }
        // detect mixedType columns to fixup Schema later
        else if (value != null
            && !columnsWithMixedValues.contains(key)
            && featureSchema.getAttributeType(key).toJavaClass() != value
                .getClass()) {

          // Object class columns are still undecided, because it was null until
          // now. set the first concrete class provided as type
          if (featureSchema.getAttributeType(key).toJavaClass() == Object.class)
            featureSchema.setAttributeType(
                featureSchema.getAttributeIndex(key),
                AttributeType.toAttributeType(value.getClass()));
          // all other obviously are mixed value columns
          else
            columnsWithMixedValues.add(key);
        }

        feature.setAttribute(attribName, value);
      }
    }

    featureCollection.add(feature);
  }

  public int size() {
    return featureCollection.size();
  }

  public FeatureCollection getFeatureCollection() {
    // set type to String for mixed columns
    for (String key : columnsWithMixedValues) {
      featureSchema.setAttributeType(featureSchema.getAttributeIndex(key),
          AttributeType.STRING);
      columnsWithMixedValues.remove(key);
    }
    return featureCollection;
  }
}
