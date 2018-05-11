/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FlexibleFeature;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorSupport;
import com.vividsolutions.jump.task.TaskMonitorUtil;
import com.vividsolutions.jump.util.Timer;

/**
 * GMLReader is a {@link JUMPReader} specialized to read GML files.
 *
 * <p>
 * DataProperties for the JCSReader load(DataProperties) interface:
 * </p>
 * <p>
 * <table border='1' cellspacing='0' cellpadding='4'>
 * <tr>
 * <th>Parameter</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>
 * File or DefaultValue</td>
 *
 * <td>
 * File name for the input .xml file</td>
 * </tr>
 *
 * <tr>
 * <td>
 * InputTemplateFile</td>
 * <td>
 * Filename for the GMLInputTemplate .xml file</td>
 * </tr>
 *
 * <tr>
 * <td>CompressedFile</td>
 * <td>
 * File name (a .zip or .gz) with a .jml/.xml/.gml inside (specified by File)</td>
 * </tr>
 *
 * <tr>
 * <td>
 * CompressedFileTemplate</td>
 * <td>File name (.zip or .gz) with the input template in (specified by
 * InputTemplateFile)</td>
 * </tr>
 * </table>
 * </p>
 *
 * <br>
 * NOTE: If InputTemplateFile is unspecified, GMLReader will try to read one off
 * the top of the input .xml file (the JCS format). This is the same as
 * specifying the File and TemplateFile to be the same. <br>
 * <br>
 *
 * Typically, you would write:<br>
 *
 * <pre>
 * gmlReader = new GMLReader();
 * gmlReader.load(DriverProperties); // has InputFile and InputTemplate
 * </pre>
 * 
 * or:
 * 
 * <pre>
 *     gmlReader.setInputTemplate( GMLInputTemplate);
 *     gmlReader.load( <Reader> , <stream name> );
 * </pre>
 * 
 * <br>
 * <br>
 * Internal Details - This is based on a small finite state machine with these
 * states: <br>
 * <br>
 * STATE MEANING <br>
 * 0 Init <br>
 * 1 Waiting for Collection tag <br>
 * 2 Waiting for Feature tag <br>
 * 3 Getting jcs columns <br>
 * 4 Parsing geometry (goes back to state 3) <br>
 * 1000 Parsing Multi-geometry, recursion level =1 <br>
 * 1001 Parsing Multi-geometry, recursion level =2 <br>
 * ... <br>
 * <br>
 * <br>
 * State Diagram <br>
 * <br>
 * init <br>
 * 
 * <PRE>
 *        0  ----->  1
 *                   |
 *                   | Collection start Tag
 *                   |
 *                -->2---------------->     FINISH
 *                \  |   End Collection tag
 * End Feature tag \ |
 *                  \|
 *        4<-------->3
 *           Geometry start/end
 * </PRE>
 * 
 * <br>
 * For multi-geometries <br>
 * On start Multi-geometry, increment state by 1 (or jump to 1000 if at state 4) <br>
 * make sure recursivegeometry[state-1000] is null <br>
 * <put any object into the recursivegeometry[state-1000] collection>
 *
 * <br>
 * <br>
 * on end multi-geometry, <br>
 * build geometry in recursivegeometry[state-1000], add it to
 * recursivegeometry[state-1000-1] <br>
 * state= state-1 <br>
 * <br>
 * For single geometries - they will be stuck into recursivegeometry[0], which
 * is the same <br>
 * as geometry <br>
 * <br>
 * For multi geometries - they will also be stuck into recursivegeometry[0],
 * which is the same <br>
 * as geometry. But, for the first nested geometry, it will be stuck into
 * recursivegeometry[1], <br>
 * which will then be geometry <br>
 * 
 * <pre>
 *  example of double GCs:
 *  START geometry     ->move to state 4
 *  START TAG: multi*  -> move to state 1000, geometry = recursivegeometry[0]
 *  <POINT>
 * 
 *  -> added to geometry <POINT>
 * 
 *  -> added to geometry START TAG: multi* -> move to state 1001, geometry =
 *  recursivegeometry[1] <POINT>
 * 
 *  -> added to geometry <POINT>
 * 
 *  -> added to geometry END TAG: multi -> move to state 1000, build geometry in
 *  recursivegeometry[1], add to recursivegeometry[0], geometry =
 *  recursivegeometry[0] <POINT>
 * 
 *  -> added to geometry END TAG: multi -> <finished> move to state 4, build
 *  geometry in recursivegeometry[0] (thats the result) and put it in
 *  finalGeometry END geometry -> add to feature collection example of simple
 *  geometry: START geometry ->move to state 4 BEGIN polygon ->clear out inner
 *  ring accumulator BEGIN outerboundary BEGIN linearring END linearring -> put
 *  points in linearRing END outerboundary -> put linearRing in outerBoundary
 *  BEGIN innerboundary BEGIN linearring END linearring -> put points in
 *  linearRing END innerboundary -> add linearRing to innerBoundary list END
 *  polygon -> build polygon (put in geometry, which is recursivegeometry[0] END
 *  geometry => add to feature collection
 * </pre>
 *
 * Most of the work is done in the endTag method! <br>
 * <br>
 * New additions: Jan 2005 by Dave Blasby allow srid to be parsed from the GML
 * file For example: <gml:LineString srsName="EPSG:42102"> ....
 * </gml:LineString> The SRID of the created geometry will be 42102. It accepts
 * srsNames of the form "<letters>:<number>". ie. "EPSG:111" or "DAVE:222" or
 * "BCGOV:333" etc... The Geometry's SRID will be the number. If you have a
 * GEOMETRYCOLLECTION with more than one SRID specified the SRID of the result
 * will be indeterminate (this isnt correct GML).
 *
 * Geometries without a srsName will get SRID 0.
 *
 * This functionality defaults to off for compatibility. To turn it on or off,
 * call the acceptSRID(true|false) function.
 *
 * New Addition: Jan, 2005by Dave Blasby Added slightly better support for
 * type=OBJECT. It sticks a String in. Before it would probably throw an error.
 * Added support for multi-objects for example: <a> <b>...1...</b>
 * <b>...2...</b> <b>...3...</b> </a> Old behavior would be to for column 'b' to
 * have value "...3...". New behavior (only if you set b's type to 'OBJECT' and
 * set the GMLReader to processMultiItems as lists) <a><b>...1...</b></a> --> b
 * get the string "...1..." (as before)
 * <a><b>...1...</b><b>...2...</b><b>...3...</b></a> --> 'b' is a list of String
 * ['...1...','...2...','...3...']
 *
 */
public class GMLReader extends DefaultHandler implements JUMPReader, TaskMonitorSupport {

  private static int STATE_GET_COLUMNS = 3;
  private Collection<Exception> exceptions = new ArrayList<Exception>();
  private Locator locator;

  /**
   * STATE MEANING <br>
   * 0 Init <br>
   * 1 Waiting for Collection tag <br>
   * 2 Waiting for Feature tag <br>
   * 3 Getting jcs columns <br>
   * 4 Parsing geometry (goes back to state 3) <br>
   * 1000 Parsing Multi-geometry, recursion level =1 <br>
   * 1001 Parsing Multi-geometry, recursion level =2 <br>
   */
  private static int STATE_INIT = 0;
  private static int STATE_PARSE_GEOM_NESTED = 1000;
  private static int STATE_PARSE_GEOM_SIMPLE = 4;
  private static int STATE_WAIT_COLLECTION_TAG = 1;
  private static int STATE_WAIT_FEATURE_TAG = 2;
  private static int STATE_WAIT_GMLBOX_TAG = 8;

  private final static List<String> simpleGeoms = new ArrayList<>();
  private final static List<String> multiGeoms = new ArrayList<>();

  static {
    multiGeoms.add("multipoint");
    multiGeoms.add("multilinestring");
    multiGeoms.add("multipolygon");
    multiGeoms.add("multigeometry");
    multiGeoms.add("geometrycollection");
    simpleGeoms.add("polygon");
    simpleGeoms.add("linestring");
    simpleGeoms.add("point");
    simpleGeoms.add("linearring");
  }

  private GMLInputTemplate GMLinput = null;
  private int STATE = STATE_INIT; // list of points
  private Point apoint;
  private Feature currentFeature;
//  private int currentGeometryNumb = 1;
  private FeatureCollection fc;
  private FeatureSchema fcmd; // list of geometries
  private Geometry finalGeometry; // list of geometrycollections - list of list of
                          // geometry
  private String current_geom_qname = "";
  private ArrayList<Geometry> geometry;
  private GeometryFactory geometryFactory = new GeometryFactory(); // this might get
                                                           // replaced if
                                                           // there's an SRID
                                                           // change
  private ArrayList<LinearRing> innerBoundaries = new ArrayList<>();
  private Attributes lastStartTag_atts;
  private String lastStartTag_name;
  private String lastStartTag_qName; // accumulate values inside a tag

  // info about the last start tag encountered
  private String lastStartTag_uri;
  private LineString lineString;
  private LinearRing linearRing; // a LR
  private LinearRing outerBoundary; // list of LinearRing
  private ArrayList<Coordinate> pointList = new ArrayList<>(); // list of accumulated points
                                         // (Coordinate)
  private Polygon polygon; // polygon

  // higher level geometry object
  private ArrayList<ArrayList> recursivegeometry = new ArrayList<>();

  // low-level geometry objects
  private Coordinate singleCoordinate = new Coordinate();
  private String streamName; // result geometry -
  private StringBuffer tagBody;
  private XMLReader xr; // see above

  private int SRID = 0; // srid to give the created geometries
  private boolean parseSRID = false; // true = put SRID for srsName="EPSG:42102"

  /**
   * true => for 'OBJECT' types, if you find more than 1 item, make a list and
   * store all the results
   */
  public boolean multiItemsAsLists = false;

  // precompiled patterns for performance reasons
  private static Pattern regex_geomMultiPoint, regex_geomMultiLineString, regex_geomMultiPolygon, regex_geomLinearRing;
  static {
    regex_geomMultiPoint = Pattern.compile("^(?i)(gml:)?multipoint$");
    regex_geomMultiLineString = Pattern.compile("^(?i)(gml:)?multilinestring$");
    regex_geomMultiPolygon = Pattern.compile("^(?i)(gml:)?multipolygon$");
    regex_geomLinearRing = Pattern.compile("^(?i)(gml:)?linearring$");
  }

  /**
   * Constructor - make a SAXParser and have this GMLReader be its
   * ContentHandler and ErrorHandler.
   */
  public GMLReader() {
    super();
    xr = new org.apache.xerces.parsers.SAXParser();
    xr.setContentHandler(this);
    xr.setErrorHandler(this);
  }

  // get current location for throw errors
  public void setDocumentLocator(Locator locator) {
      this.locator = locator;
  }

  /**
   * parse SRID information in geometry tags
   * 
   * @param parseTheSRID true = parse
   */
  public void acceptSRID(boolean parseTheSRID) {
    parseSRID = parseTheSRID;
  }

  /**
   * Added slightly better support for type=OBJECT. It sticks a String in.
   * Before it would probably throw an error. Added support for multi-objects
   * for example: <a> <b>...1...</b> <b>...2...</b> <b>...3...</b> </a> Old
   * behavior would be to for column 'b' to have value "...3...". New behavior
   * (only if you set b's type to 'OBJECT' and set the GMLReader to
   * processMultiItems as lists) <a><b>...1...</b></a> --> b get the string "b"
   * (as before) <a><b>...1...</b><b>...2...</b><b>...3...</b></a> --> 'b' is a
   * list of String ['...1...','...2...','...3...']
   */
  public void processMultiItems(boolean accept) {
    multiItemsAsLists = accept;
  }

  /**
   * Attach a GMLInputTemplate information class.
   *
   * @param template
   *          The new inputTemplate value
   */
  public void setInputTemplate(GMLInputTemplate template) {
    GMLinput = template;
  }

  /**
   * SAX handler - store and accumulate tag bodies
   *
   * @param ch      Description of the Parameter
   * @param start   Description of the Parameter
   * @param length  Description of the Parameter
   * @exception SAXException Description of the Exception
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      tagBody.append(ch, start, length);
    } catch (Exception e) {
      throw new SAXException(e.getMessage());
    }
  }

  private boolean compareToIgnoreCaseWithOptionalGmlColonPrefix( String value1, String value2 ){
    if (value1 == null || value2 == null)
      throw new InvalidParameterException("no null values allowed. value1/"+value1+", value2/"+value2);

    if (value1.equalsIgnoreCase(value2))
      return true;

    // retry making sure both are prefixed
    String prefix = "gml:";
    if (!value1.toLowerCase().startsWith(prefix))
        value1 = prefix + value1;
    if (!value2.toLowerCase().startsWith(prefix))
        value2 = prefix + value2;
    return value1.equalsIgnoreCase(value2);
  }

  /**
   * Main Entry - load in a GML file
   *
   * @param dp
   *          Description of the Parameter
   * @return Description of the Return Value
   * @exception IllegalParametersException
   *              Description of the Exception
   * @exception Exception
   *              Description of the Exception
   */
  public FeatureCollection read(DriverProperties dp) throws Exception {
    FeatureCollection fc;
    GMLInputTemplate gmlTemplate;
    String inputFname;
    boolean isCompressed;
    boolean isCompressed_template;

    isCompressed_template = (dp.getProperty("CompressedFileTemplate") != null);

    isCompressed = (dp.getProperty("CompressedFile") != null);

    inputFname = dp.getProperty("File");

    if (inputFname == null) {
      inputFname = dp.getProperty("DefaultValue");
    }

    if (inputFname == null) {
      throw new IllegalParametersException(
          "call to GMLReader.read() has DataProperties w/o a InputFile specified");
    }

    if (dp.getProperty("TemplateFile") == null) {
      // load from .gml file
      if (isCompressed) {
        InputStream in = CompressedFile.openFile(inputFname,
            dp.getProperty("CompressedFile"));
        gmlTemplate = inputTemplateFromFile(in);
        in.close();
      } else {
        gmlTemplate = inputTemplateFromFile(inputFname);
      }
    } else {
      // template file specified
      if (isCompressed_template) {
        InputStream in = CompressedFile.openFile(
            dp.getProperty("TemplateFile"),
            dp.getProperty("CompressedFileTemplate"));
        gmlTemplate = inputTemplateFromFile(in);
        in.close();
      } else {
        if (isCompressed) // special case if the .gml file is compressed, and a
                          // template file is specified
        {
          if (dp.getProperty("CompressedFile").equals(
              dp.getProperty("TemplateFile"))) // the template file is the
                                               // compressed file
          {
            InputStream in = CompressedFile.openFile(inputFname,
                dp.getProperty("CompressedFile"));
            gmlTemplate = inputTemplateFromFile(in);
            in.close();
          } else {
            gmlTemplate = inputTemplateFromFile(dp.getProperty("TemplateFile"));
          }
        } else {
          // normal load
          gmlTemplate = inputTemplateFromFile(dp.getProperty("TemplateFile"));
        }
      }
    }

    //java.io.Reader r;

    this.setInputTemplate(gmlTemplate);

    // if (isCompressed) {
    // r = new BufferedReader(new InputStreamReader(
    // CompressedFile.openFile(inputFname,
    // dp.getProperty("CompressedFile"))));
    // } else {
    // r = new BufferedReader(new FileReader(inputFname));
    // }

    // forward stream instead of reader to allow sax to detect encoding
    fc = read(
        CompressedFile.openFile(inputFname, dp.getProperty("CompressedFile")),
        inputFname);
    // r.close();

    return fc;
  }

  /**
   * Helper function - calls read(java.io.Reader r,String readerName) with the
   * readerName "Unknown Stream". You should have already called
   * setInputTempalate().
   *
   * @param is input stream to read the GML File from
   * @return Description of the Return Value
   * @exception Exception
   *              Description of the Exception
   */
  public FeatureCollection read(InputStream is) throws Exception {
    return read(is, "Unknown Stream");
  }

  /**
   * Main function to read a GML file. You should have already called
   * setInputTempalate().
   * 
   * NOTE: if proper xml charset support is needed this method should be called
   * with an InputStream as source
   *
   * @param o
   *          reader/inputstream object to read the GML from
   * @param readerName
   *          what to call the reader for error reporting
   * @return Description of the Return Value
   * @exception Exception
   *              Description of the Exception
   */
  public FeatureCollection read(Object o, String readerName) throws Exception {

    // determine reader or stream
    InputSource is;
    if (o instanceof Reader) {
      is = new InputSource((Reader) o);
    } else if (o instanceof InputStream) {
      is = new InputSource((InputStream) o);
    } else {
      throw new ParseException("need InputStream or Reader object!");
    }

    if (GMLinput == null) {
      throw new ParseException("you must set the GMLinput template first!");
    }

    streamName = readerName;

    fcmd = GMLinput.toFeatureSchema();
    fc = new FeatureDataset(fcmd);

    try {
      xr.parse(is);
    } catch (SAXCancelledException e){
      // we were cancelled
      fc = null;
    } catch (SAXParseException e) {
      throw new ParseException(e.getMessage() + "  Last Opened Tag: "
          + lastStartTag_qName + ".  Reader reports last line read as "
          + e.getLineNumber(), streamName + " - " + e.getPublicId() + " ("
          + e.getSystemId() + ") ", e.getLineNumber(), e.getColumnNumber());
    } catch (SAXException e) {
      throw new ParseException(e.getMessage() + "  Last Opened Tag: "
          + lastStartTag_qName, streamName, 0, 0);
    }

    return fc;
  }

  // //////////////////////////////////////////////////////////////////
  // Event handlers.
  // //////////////////////////////////////////////////////////////////

  /**
   * SAX handler - move to state 1
   */
  public void startDocument() {
    // System.out.println("Start document");
    tagBody = new StringBuffer();
    STATE = STATE_WAIT_COLLECTION_TAG;
  }

  /**
   * SAX handler. Handle state and state transitions based on an element
   * starting
   *
   * @param uri
   *          Description of the Parameter
   * @param name
   *          Description of the Parameter
   * @param qName
   *          Description of the Parameter
   * @param atts
   *          Description of the Parameter
   * @exception SAXException
   *              Description of the Exception
   */
  public void startElement(String uri, String name, String qName,
      Attributes atts) throws SAXException {
    
    // allow cancellation
    if (getTaskMonitor().isCancelRequested()) throw new SAXCancelledException();
    
    try {
      //System.out.println("Start element: " + qName);
      tagBody = new StringBuffer();
      lastStartTag_uri = uri;
      lastStartTag_name = name;
      lastStartTag_qName = qName;
      lastStartTag_atts = atts;

//      if (STATE == STATE_WAIT_COLLECTION_TAG)
//        System.out.println("(STATE == STATE_WAIT_COLLECTION_TAG)");
      
      if (STATE == STATE_INIT) {
        return; // something wrong
      }

      if ((STATE == STATE_WAIT_COLLECTION_TAG)
          && (qName.compareToIgnoreCase(GMLinput.collectionTag) == 0)) {
        // found the collection tag
        // System.out.println("found collection");
        STATE = STATE_WAIT_FEATURE_TAG;

        return;
      }
      if ((STATE == STATE_WAIT_FEATURE_TAG)
          && (qName.compareToIgnoreCase(GMLinput.featureTag) == 0)) {
        // found the feature tag
        // System.out.println("found feature");
        currentFeature = new FlexibleFeature(fcmd);
        STATE = STATE_GET_COLUMNS;
//        SRID = 0;// default SRID (reset for each feature, but should be constant
//                 // for a featurecollection)
//        if (geometryFactory.getSRID() != SRID)
//          geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

        return;
      }

      if ((STATE == STATE_GET_COLUMNS) && GMLinput.isGeometryElement(qName)) {
        // found the geom tag
        // System.out.println("found geom #"+currentGeometryNumb );
        recursivegeometry = new ArrayList<>();
        geometry = new ArrayList<>();
        recursivegeometry.add(geometry);

        // recursivegeometry[0] = geometry
        finalGeometry = null;
        STATE = STATE_PARSE_GEOM_SIMPLE;

        return;
      }

      if (parseSRID && (STATE >= STATE_PARSE_GEOM_SIMPLE)
          && isGeometryTag(qName)) {
        // System.out.println("src="+atts.getValue("srsName"));
        // System.out.println("srid="+ parseSRID(atts.getValue("srsName")));

        int newSRID = parseSRID(atts.getValue("srsName"));
        // NOTE: if parseSRID it usually means that there was an error parsing
        // but, it could actually be specified as 'EPGS:0'. Thats not
        // a problem because we've already defaulted to srid 0.
        if (newSRID != 0) {
          SRID = newSRID;
          if (geometryFactory.getSRID() != SRID)
            geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
        }
      }

      if ((STATE >= STATE_PARSE_GEOM_SIMPLE)
          && ((qName.compareToIgnoreCase("coord") == 0) || (qName
              .compareToIgnoreCase("gml:coord") == 0))) {
        singleCoordinate.x = Double.NaN;
        singleCoordinate.y = Double.NaN;
        singleCoordinate.z = Double.NaN;
      }

      if ((STATE >= STATE_PARSE_GEOM_SIMPLE)
          && (!((qName.compareToIgnoreCase("X") == 0)
              || (qName.compareToIgnoreCase("gml:x") == 0)
              || (qName.compareToIgnoreCase("y") == 0)
              || (qName.compareToIgnoreCase("gml:y") == 0)
              || (qName.compareToIgnoreCase("z") == 0)
              || (qName.compareToIgnoreCase("gml:z") == 0)
              || (qName.compareToIgnoreCase("coord") == 0) || (qName
              .compareToIgnoreCase("gml:coord") == 0)))) {
        pointList.clear(); // clear out any accumulated points
      }

      if ((STATE >= STATE_PARSE_GEOM_SIMPLE)
          && ((qName.compareToIgnoreCase("polygon") == 0) || (qName
              .compareToIgnoreCase("gml:polygon") == 0))) {
        innerBoundaries.clear(); // polygon just started - clear out the last
                                 // one
      }

      if ((STATE > STATE_GET_COLUMNS) && (isMultiGeometryTag(qName))) {
        // in state 4 or a 1000 state and found a start GC (or Multi-geom) event
        if (STATE == STATE_PARSE_GEOM_SIMPLE) {
          // geometry already = recursivegeometry[0]
          STATE = STATE_PARSE_GEOM_NESTED;
          current_geom_qname = qName;
        } else {
          STATE++;
          geometry = new ArrayList<>();
          recursivegeometry.add(geometry);
        }
      }
      
      if ((STATE > STATE_GET_COLUMNS) && isSimpleGeometryTag(qName) && current_geom_qname.isEmpty()) {
        current_geom_qname = qName;
      }

      if ((STATE == STATE_WAIT_FEATURE_TAG)
          && GMLinput.crsTag != null && compareToIgnoreCaseWithOptionalGmlColonPrefix(qName, GMLinput.crsTag) ) {
        // found the crs1 tag
        STATE = STATE_WAIT_GMLBOX_TAG;

        return;
      }
      if ((STATE == STATE_WAIT_GMLBOX_TAG)
          && compareToIgnoreCaseWithOptionalGmlColonPrefix(qName, "Box") ) {
        // found the crs2 box tag

        // fetch srid id
        for (int i = 0; i < atts.getLength(); i++) {
          String attName = atts.getQName(i);
          if (attName.equalsIgnoreCase("srsName")){
            String attValue = atts.getValue(i);
            String sridString = attValue.substring(attValue.lastIndexOf("#") + 1);

            try {
              SRID = Integer.valueOf(sridString);
            } catch (NumberFormatException e) {
              addParseException("srid '"+sridString+"'is not a number.", e);
            }
          }
        }
        STATE = STATE_WAIT_FEATURE_TAG;
        return;
      }

    } catch (Exception e) {
      e.printStackTrace(System.err);
      throw new SAXException(e.getMessage());
    }
  }

  /**
   * SAX handler - handle state information and transitions based on ending
   * elements Most of the work of the parser is done here.
   *
   * @param uri
   *          Description of the Parameter
   * @param name
   *          Description of the Parameter
   * @param qName
   *          Description of the Parameter
   * @exception SAXException
   *              Description of the Exception
   */
  public void endElement(String uri, String name, String qName)
      throws SAXException {
    
    // allow cancellation
    if (getTaskMonitor().isCancelRequested()) throw new SAXCancelledException();
    
    try {
      int index;

      // System.out.println("End element: " + qName);
      if (STATE == STATE_INIT) {
        tagBody = new StringBuffer();

        return; // something wrong
      }

      if (STATE > STATE_GET_COLUMNS) {
        if (isMultiGeometryTag(qName)) {
          if (STATE == STATE_PARSE_GEOM_NESTED) {
            STATE = STATE_PARSE_GEOM_SIMPLE; // finished - no action. geometry
                                             // is correct
          } else {
            // build the geometry that was in that collection
            Geometry g;

            g = geometryFactory.buildGeometry(geometry);
            geometry = (ArrayList) recursivegeometry.get(STATE
                - STATE_PARSE_GEOM_NESTED - 1);
            geometry.add(g);
            recursivegeometry.remove(STATE - STATE_PARSE_GEOM_NESTED);
            g = null;

            STATE--;
          }
        }

        //System.out.println("wrap-element: " + qName);
        // finalize geometry and add to feature
        if (GMLinput.isGeometryElement(qName)) {
          tagBody = new StringBuffer();
          STATE = STATE_GET_COLUMNS;

          // -- [sstein] 14.March.2009
          // read LinearRings even if we don't have polygons
          if ( linearRing != null ) {
            geometry.add(linearRing);
            linearRing = null;
          }

          if (regex_geomMultiPoint.matcher(current_geom_qname).matches())
            finalGeometry = geometryFactory
                .createMultiPoint(geometry.toArray(new Point[0]));
          else if (regex_geomMultiLineString.matcher(current_geom_qname).matches())
            finalGeometry = geometryFactory
                .createMultiLineString((geometry.toArray(new LineString[0])));
          else if (regex_geomMultiPolygon.matcher(current_geom_qname).matches())
            finalGeometry = geometryFactory
                .createMultiPolygon((geometry.toArray(new Polygon[0])));
//          else if (current_geom_qname.matches("^(?i)(gml:)?linearring$"))
//            finalGeometry = (Geometry) geometry.get(0);
          else
            finalGeometry = geometryFactory.buildGeometry(geometry);

          // System.out.println("end geom: "+finalGeometry.toString() );
          currentFeature.setGeometry(finalGeometry);
//          currentGeometryNumb++;

          // reset multi qname
          current_geom_qname = "";
          return;
        }
        
        
        //System.out.println("geom-element: " + qName);
        // these correspond to <coord><X>0.0</X><Y>0.0</Y></coord>
        if ((qName.compareToIgnoreCase("X") == 0)
            || (qName.compareToIgnoreCase("gml:X") == 0)) {
          singleCoordinate.x = Double.parseDouble(tagBody.toString());
        } else if ((qName.compareToIgnoreCase("Y") == 0)
            || (qName.compareToIgnoreCase("gml:y") == 0)) {
          singleCoordinate.y = Double.parseDouble(tagBody.toString());
        } else if ((qName.compareToIgnoreCase("Z") == 0)
            || (qName.compareToIgnoreCase("gml:z") == 0)) {
          singleCoordinate.z = Double.parseDouble(tagBody.toString());
        } else if ((qName.compareToIgnoreCase("COORD") == 0)
            || (qName.compareToIgnoreCase("gml:coord") == 0)) {
          pointList.add(new Coordinate(singleCoordinate)); // remember it
        }
        // this corresponds to
        // <gml:coordinates>1195156.78946687,382069.533723461</gml:coordinates>
        else if ((qName.compareToIgnoreCase("COORDINATES") == 0)
            || (qName.compareToIgnoreCase("gml:coordinates") == 0)) {
          // tagBody has a wack-load of points in it - we need
          // to parse them into the pointList list.
          // assume that the x,y,z coordinate are "," separated, and the points
          // are " " separated
          parsePoints(tagBody.toString(), geometryFactory);
        } else if ((qName.compareToIgnoreCase("linearring") == 0)
            || (qName.compareToIgnoreCase("gml:linearring") == 0)) {
          Coordinate[] c = new Coordinate[0];

          c = pointList.toArray(c);

          linearRing = geometryFactory.createLinearRing(c);
        } else if ((qName.compareToIgnoreCase("outerBoundaryIs") == 0)
            || (qName.compareToIgnoreCase("gml:outerBoundaryIs") == 0)) {
          outerBoundary = linearRing;
          linearRing = null;
        } else if ((qName.compareToIgnoreCase("innerBoundaryIs") == 0)
            || (qName.compareToIgnoreCase("gml:innerBoundaryIs") == 0)) {
          innerBoundaries.add(linearRing);
          linearRing = null;
        } else if ((qName.compareToIgnoreCase("polygon") == 0)
            || (qName.compareToIgnoreCase("gml:polygon") == 0)) {
          // LinearRing[] lrs = new LinearRing[1];
          LinearRing[] lrs = new LinearRing[0];
          lrs = innerBoundaries.toArray(lrs);
          polygon = geometryFactory.createPolygon(outerBoundary, lrs);
          geometry.add(polygon);
        } else if ((qName.compareToIgnoreCase("linestring") == 0)
            || (qName.compareToIgnoreCase("gml:linestring") == 0)) {
          Coordinate[] c = new Coordinate[0];

          c = pointList.toArray(c);

          lineString = geometryFactory.createLineString(c);
          geometry.add(lineString);
        } else if ((qName.compareToIgnoreCase("point") == 0)
            || (qName.compareToIgnoreCase("gml:point") == 0)) {
          apoint = geometryFactory
              .createPoint(pointList.size() > 0 ? pointList.get(0) : null);
          geometry.add(apoint);
        }
      } else if (STATE == STATE_GET_COLUMNS) {
        if (qName.compareToIgnoreCase(GMLinput.featureTag) == 0) {
          tagBody = new StringBuffer();
          STATE = STATE_WAIT_FEATURE_TAG;

          // System.out.println("end feature");
          // create a feature and put it inside the featurecollection
          if (currentFeature.getGeometry() == null) {
            Geometry g = currentFeature.getGeometry();

            if (g != null) {
              //System.out.println(g.toString());
            }

            throw new ParseException("no geometry specified in feature");
          }

          fc.add(currentFeature);
          report(fc.size());

          currentFeature = null;

          return;
        } else {
          // check to see if this was a tag we want to store as a column
          // DB: added 2nd check for GML like <a><b></b></a>
          // the "b" tag is the "lastStartTag_qName" for "</b>" and "</a>" we
          // only need to
          // process it once.
          try {
            if (((index = GMLinput.match(lastStartTag_qName, lastStartTag_atts)) > -1)
                && (lastStartTag_qName.equalsIgnoreCase(qName))) {
              // System.out.println("value of " +
              // GMLinput.columnName(index)+" : " +
              // GMLinput.getColumnValue(index,tagBody, lastStartTag_atts) );

              // if the column already has a value and multiItems support is
              // turned on ..and its type ==object
              if ((multiItemsAsLists)
                  && (currentFeature.getAttribute(GMLinput.columnName(index)) != null)
                  && (((ColumnDescription) (GMLinput.columnDefinitions
                      .get(index))).type == AttributeType.OBJECT)) {
                Object oldValue = currentFeature.getAttribute(GMLinput
                    .columnName(index));
                if (oldValue instanceof List) {
                  // already a list there - just stuff another thing in!
                  ((List) oldValue).add(GMLinput.getColumnValue(index,
                      tagBody.toString(), lastStartTag_atts));
                } else {
                  // no list currently there - make a list and replace
                  List l = new ArrayList();
                  l.add(oldValue);
                  l.add(GMLinput.getColumnValue(index, tagBody.toString(),
                      lastStartTag_atts)); // new value
                  currentFeature.setAttribute(GMLinput.columnName(index), l);
                }
              } else // handle normally
              {
                currentFeature.setAttribute(GMLinput.columnName(index),
                    GMLinput.getColumnValue(index, tagBody.toString(),
                        lastStartTag_atts));
              }
            }
          } catch (Exception e) {
            // dont actually do anything with the parse problem - just ignore
            // it,
            // we cannot send it back because the function its overiding doesnt
            // allow
            e.printStackTrace();
          }

          tagBody = new StringBuffer();
        }
      } else if (STATE == STATE_WAIT_FEATURE_TAG) {
        if (qName.compareToIgnoreCase(GMLinput.collectionTag) == 0) {
          STATE = STATE_INIT; // finish

          // System.out.println("DONE!");
          tagBody = new StringBuffer();

          return;
        }
      } else if (STATE == STATE_WAIT_COLLECTION_TAG) {
        tagBody = new StringBuffer();

        return; // still look for start collection tag
      }
      else if (STATE == STATE_WAIT_GMLBOX_TAG && 
          compareToIgnoreCaseWithOptionalGmlColonPrefix(qName, "boundedBy")) {
        // found </gml:boundedBy>, return to feature parsing
        STATE = STATE_WAIT_FEATURE_TAG;
        return;
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new SAXException(e.getMessage());
    }
  }

  /**
   * SAX HANDLER - move to state 0
   */
  public void endDocument() {
    // System.out.println("End document");
    
    // apply srid if not unset
    if (SRID > 0)
      fc.getFeatureSchema().setCoordinateSystem(new CoordinateSystem("", SRID, null));
    STATE = STATE_INIT;
  }

  // //////////////////////////////////////////////////////////////////
  // Error handlers.
  // //////////////////////////////////////////////////////////////////
  public void warning(SAXParseException exception) throws SAXException {
    exceptions.add(exception);
  }

  public void error(SAXParseException exception) throws SAXException {
    exceptions.add(exception);
  }

  public void fatalError(SAXParseException exception) throws SAXException {
    throw exception;
  }

  /**
   * returns true if the the string represents a geometry type ie.
   * "gml:linestring" or "linestring"
   *
   * @param s
   *          Description of the Parameter
   * @return true if this is a geometry tag
   */
  private boolean isGeometryTag(String s) {
    // remove the "gml:" if its there
    s = stripGmlColon(s).toLowerCase();

    return simpleGeoms.contains(s) || multiGeoms.contains(s);
  }
  
  private boolean isSimpleGeometryTag(String s) {
    // remove the "gml:" if its there
    s = stripGmlColon(s).toLowerCase();

    return simpleGeoms.contains(s);
  }

  /**
   * returns true if the the string represents a multi* geometry type
   *
   * @param s
   *          Description of the Parameter
   * @return The multiGeometryTag value
   */
  private boolean isMultiGeometryTag(String s) {
    // remove the "gml:" if its there
    s = stripGmlColon(s).toLowerCase();

    return multiGeoms.contains(s);
  }

  private String stripGmlColon(String s) {
    return s.replaceFirst("^(?i)gml:", "");
  }

  private GMLInputTemplate inputTemplateFromFile(InputStream in)
      throws ParseException, IOException {
    GMLInputTemplate result;
    result = inputTemplate(in);
    return result;
  }

  private GMLInputTemplate inputTemplateFromFile(String filename)
      throws ParseException, IOException {
    GMLInputTemplate result;
    InputStream is = new BufferedInputStream(new FileInputStream(filename));
    result = inputTemplate(is);
    is.close();

    return result;
  }

  /**
   * Parse a bunch of points - stick them in pointList. Handles 2d and 3d.
   *
   * @param ptString
   *          string containing a bunch of coordinates
   * @param geometryFactory
   *          JTS point/coordinate factory
   */
  private void parsePoints(String ptString, GeometryFactory geometryFactory) {
    String aPoint;
    StringTokenizer stokenizerPoint;
    Coordinate coord = new Coordinate();
    int dim;
    String numb;
    StringBuffer sb;
    int t;
    char ch;

    // remove \n and \r and replace with spaces
    sb = new StringBuffer(ptString);

    for (t = 0; t < sb.length(); t++) {
      ch = sb.charAt(t);

      if ((ch == '\n') || (ch == '\r')) {
        sb.setCharAt(t, ' ');
      }
    }

    StringTokenizer stokenizer = new StringTokenizer(new String(sb), " ", false);

    while (stokenizer.hasMoreElements()) {
      // have a point in memory - handle the single point
      aPoint = stokenizer.nextToken();
      stokenizerPoint = new StringTokenizer(aPoint, ",", false);
      coord.x = coord.y = coord.z = Double.NaN;
      dim = 0;

      while (stokenizerPoint.hasMoreElements()) {
        numb = stokenizerPoint.nextToken();

        if (dim == 0) {
          coord.x = Double.parseDouble(numb);
        } else if (dim == 1) {
          coord.y = Double.parseDouble(numb);
        } else if (dim == 2) {
          coord.z = Double.parseDouble(numb);
        }

        dim++;
      }
      if ((coord.x != coord.x) || (coord.y != coord.y)) // one (x,y) is NaN
      {
        throw new IllegalArgumentException(
            "GML error - coordinate list isnt valid GML. Watch your spaces and commas!");
      }
      pointList.add(coord); // remember it
      coord = new Coordinate();
      stokenizerPoint = null;
    }
  }

  private GMLInputTemplate inputTemplate(InputStream is) throws IOException,
      ParseException {
    GMLInputTemplate gmlTemplate = new GMLInputTemplate();
    gmlTemplate.load(is);
    is.close();

    if (!(gmlTemplate.loaded)) {
      throw new ParseException("Failed to load GML input template");
    }

    return gmlTemplate;
  }

  /**
   * parses the given srs text and returns the SRID
   * 
   * @param srsName
   *          srsName of the type "EPSG:<number>"
   * @return srid or 0 if there is a problem
   */
  private int parseSRID(String srsName) {
    try {
      int semicolonLoc = srsName.lastIndexOf(':');
      if (semicolonLoc == -1)
        return 0;
      return Integer.parseInt(srsName.substring(semicolonLoc + 1).trim());
    } catch (NumberFormatException e) {
      addParseException( "srid '"+srsName+"'is not a number.", e);
      return 0;
    }
  }

  protected void addException(Exception e){
    exceptions.add(e);
  }

  protected void addParseException(String message, Exception cause) {
    exceptions.add(
        new ParseException(message, streamName, locator.getLineNumber(), locator.getColumnNumber(), cause));
  }

  /**
   * @return exceptions collected during the reading process.
   */
  public Collection<Exception> getExceptions() {
    return exceptions;
  }

  private TaskMonitor taskMonitor = new DummyTaskMonitor();

  public void setTaskMonitor(TaskMonitor taskMonitor) {
    this.taskMonitor = taskMonitor;
  }

  public TaskMonitor getTaskMonitor() {
    return taskMonitor;
  }
  
  private long milliSeconds = 0;

  private void report( int num ){
    long now = Timer.milliSecondsSince(0);
    // show status every .5s
    if (now - 500 >= milliSeconds) {
      milliSeconds = now;
      TaskMonitorUtil.report(
          getTaskMonitor(),
          I18N.getMessage("Reader.parsed-{0}-features",
              String.format("%,10d", num)));
    }
  }
}

class SAXCancelledException extends SAXException{
  // dummy to allow proper cancellation
}