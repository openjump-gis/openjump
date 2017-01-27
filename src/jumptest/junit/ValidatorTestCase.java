package jumptest.junit;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.geom.Angle;
import com.vividsolutions.jump.qa.ValidationError;
import com.vividsolutions.jump.qa.ValidationErrorType;
import com.vividsolutions.jump.qa.Validator;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ValidatorTestCase extends TestCase {

  public ValidatorTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {ValidatorTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private class TestValidator extends Validator {
    protected void addIfNotNull(Object item, Collection<Object> collection) {
      super.addIfNotNull(item, collection);
    }
    protected ValidationError validateNoRepeatedConsecutivePoints(Feature feature) {
      return super.validateNoRepeatedConsecutivePoints(feature);
    }
    protected ValidationError validateGeometryClass(Feature feature) {
      return super.validateGeometryClass(feature);
    }
    protected ValidationError validateBasicTopology(Feature feature) {
      return super.validateBasicTopology(feature);
    }
    protected ValidationError validatePolygonOrientation(Feature feature) {
      return super.validatePolygonOrientation(feature);
    }
    protected ValidationError validateGeometriesSimple(Feature feature) {
      return super.validateGeometriesSimple(feature);
    }
    protected ValidationError validateMinSegmentLength(Feature feature) {
      return super.validateMinSegmentLength(feature);
    }
    protected ValidationError validateMinAngle(Feature feature) {
      return super.validateMinAngle(feature);
    }
    protected ValidationError validateMinPolygonArea(Feature feature) {
      return super.validateMinPolygonArea(feature);
    }
    protected ValidationError validateNoHoles(Feature feature) {
      return super.validateNoHoles(feature);
    }
  }

  private TestValidator testValidator;

  public void testAddIfNotNull() {
    List<Object> list = new ArrayList<>();
    assertTrue(list.isEmpty());
    testValidator.addIfNotNull(null, list);
    assertTrue(list.isEmpty());
    testValidator.addIfNotNull(new Object(), list);
    assertEquals(1, list.size());
  }

  private GeometryFactory geometryFactory = new GeometryFactory();

  public void testValidateGeometryClass1() {
    testValidator.setDisallowedGeometryClasses(Arrays.asList(new Class[]{
        MultiPolygon.class, LineString.class}));
    assertTypeEquals(null, testValidator.validateGeometryClass(toFeature("GEOMETRYCOLLECTION EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometryClass(toFeature("MULTIPOINT EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometryClass(toFeature("MULTILINESTRING EMPTY")));
    assertTypeEquals(ValidationErrorType.GEOMETRY_CLASS_DISALLOWED,
                     testValidator.validateGeometryClass(toFeature("MULTIPOLYGON EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometryClass(toFeature("POINT EMPTY")));
    assertTypeEquals(ValidationErrorType.GEOMETRY_CLASS_DISALLOWED,
                     testValidator.validateGeometryClass(toFeature("LINESTRING EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometryClass(toFeature("LINEARRING EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometryClass(toFeature("POLYGON EMPTY")));
  }

  public void testValidateBasicTopology() {
    testValidator.setCheckingBasicTopology(true);
    assertTypeEquals(null, testValidator.validateBasicTopology(toFeature(
        "POLYGON((10 10, 20 20, 30 20, 10 10))")));
    assertTypeEquals(ValidationErrorType.BASIC_TOPOLOGY_INVALID,
                     testValidator.validateBasicTopology(toFeature(
                     "POLYGON((0 0, 10 10, 10 0, 0 10, 0 0))")));
  }

  public void testValidateNoRepeatedConsecutivePoints() {
    testValidator.setCheckingNoRepeatedConsecutivePoints(true);
    assertTypeEquals(null, testValidator.validateBasicTopology(toFeature(
        "POLYGON((10 10, 20 20, 30 20, 10 10))")));
    assertTypeEquals(ValidationErrorType.REPEATED_CONSECUTIVE_POINTS,
                     testValidator.validateNoRepeatedConsecutivePoints(toFeature(
                     "GEOMETRYCOLLECTION("
                     + "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0)),"
                     + "POLYGON((20 20, 20 20, 30 20, 30 30, 20 30, 20 20))   )")),
                     new Coordinate(20, 20));
    assertTypeEquals(null, testValidator.validateBasicTopology(toFeature(
        "POLYGON EMPTY")));
  }

  public void testValidateNoHoles() {
    testValidator.setCheckingNoHoles(true);
    assertTypeEquals(null, testValidator.validateNoHoles(toFeature(
        "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")));
    assertTypeEquals(ValidationErrorType.POLYGON_HAS_HOLES, testValidator.validateNoHoles(toFeature(
        "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0), (3 3, 4 3, 4 4, 3 3))")));
    assertTypeEquals(null, testValidator.validateNoHoles(toFeature(
        "GEOMETRYCOLLECTION("
        + "POINT(5 5),"
        + "POLYGON((50 50, 60 50, 60 60, 50 50)),"
        + "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0)),"
        + "POLYGON((50 50, 60 50, 60 60, 50 50))"
        + ")")));
    assertTypeEquals(ValidationErrorType.POLYGON_HAS_HOLES, testValidator.validateNoHoles(toFeature(
        "GEOMETRYCOLLECTION("
        + "POINT(5 5),"
        + "POLYGON((50 50, 60 50, 60 60, 50 50)),"
        + "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0), (3 3, 4 3, 4 4, 3 3)),"
        + "POLYGON((50 50, 60 50, 60 60, 50 50))"
        + ")")));

    assertTypeEquals(ValidationErrorType.POLYGON_HAS_HOLES, testValidator.validateNoHoles(toFeature(
        "GEOMETRYCOLLECTION("
        + "POINT(5 5),"
        + "POLYGON((50 50, 60 50, 60 60, 50 50)),"
        + "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0), (1 1, 9 1, 9 9, 1 9, 1 1)),"
        + "POLYGON((50 50, 60 50, 60 60, 50 50))"
        + ")")),
        new Coordinate(1, 1));
  }

  public void testMinSegmentLength2() {
    testValidator.setCheckingMinSegmentLength(true);
    testValidator.setMinSegmentLength(5);
    assertTypeEquals(null, testValidator.validateMinSegmentLength(toFeature(
        "POLYGON((10 10, 20 20, 25 20, 10 10))")));
    assertTypeEquals(ValidationErrorType.SMALL_SEGMENT, testValidator.validateMinSegmentLength(toFeature(
        "POLYGON((10 10, 20 20, 24 20, 10 10))")));
  }

  public void testMinAngle() {
    testValidator.setCheckingMinAngle(true);
    testValidator.setMinAngle(20);
    assertTypeEquals(null, validateMinAnglePolygon(21));
    assertTypeEquals(ValidationErrorType.SMALL_ANGLE, validateMinAnglePolygon(19));
    assertTypeEquals(null, validateMinAngleLineString(21));
    assertTypeEquals(ValidationErrorType.SMALL_ANGLE, validateMinAngleLineString(19));
    assertTypeEquals(null, validateMinAngleLineString2(21));
    assertTypeEquals(null, validateMinAngleLineString2(19));
  }

  private ValidationError validateMinAnglePolygon(double degrees) {
    double h = 100 * Math.tan(Angle.toRadians(degrees));
    return testValidator.validateMinAngle(toFeature(
        "POLYGON((0 0, 100 0, 100 " + h + ", 0 0))"));
  }

  private ValidationError validateMinAngleLineString(double degrees) {
    double h = 100 * Math.tan(Angle.toRadians(degrees));
    return testValidator.validateMinAngle(toFeature(
        "LINESTRING(100 0, 0 0, 100 " + h + ")"));
  }

  private ValidationError validateMinAngleLineString2(double degrees) {
    double h = 100 * Math.tan(Angle.toRadians(degrees));
    return testValidator.validateMinAngle(toFeature(
        "LINESTRING(0 0, 100 0, 100 " + h + ")"));
  }

  public void testMinPolygonArea_polygon1() {
    doTestMinPolygonArea(false, "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))", null);
  }

  public void testMinPolygonArea_polygon2() {
    doTestMinPolygonArea(false, "POLYGON((0 0, 10 0, 10 11, 0 10, 0 0))", null);
  }

  public void testMinPolygonArea_polygon3() {
    doTestMinPolygonArea(true, "POLYGON((0 0, 10 0, 10 9, 0 10, 0 0))", null);
  }

  public void testMinPolygonArea_multiPolygon1() {
    doTestMinPolygonArea(false, "MULTIPOLYGON(((0 0, 10 0, 10 10, 0 10, 0 0)),"
                         + "((20 20, 20 30, 30 30, 30 20, 20 20)))", null);
  }

  public void testMinPolygonArea_multiPolygon2() {
    doTestMinPolygonArea(false, "MULTIPOLYGON(((0 0, 10 0, 10 11, 0 10, 0 0)),"
                         + "((20 20, 20 30, 30 30, 30 20, 20 20)))", null);
  }

  public void testMinPolygonArea_multiPolygon3() {
    doTestMinPolygonArea(true,
                         "MULTIPOLYGON(((0 0, 10 0, 10 9, 0 10, 0 0)),"
                         + "((20 20, 20 30, 30 30, 30 20, 20 20)))", null);
  }

  public void testMinPolygonArea_multiPolygon_errorLocation() {
    doTestMinPolygonArea(true,
                         "MULTIPOLYGON(((0 0, 20 0, 20 20, 0 20, 0 0)),"
                         + "((0 0, 5 0, 5 5, 0 5, 0 0)))", new Coordinate(2.5d, 2.5d));
  }

  public void testMinPolygonArea_geometryCollection1() {
    doTestMinPolygonArea(false, "GEOMETRYCOLLECTION(POLYGON((0 0, 10 0, 10 10, 0 10, 0 0)),"
                         + "LINESTRING(0 0, 10 0, 10 9, 0 10, 0 0))", null);
  }

  public void testMinPolygonArea_geometryCollection2() {
    doTestMinPolygonArea(false, "GEOMETRYCOLLECTION(POLYGON((0 0, 10 0, 10 11, 0 10, 0 0)),"
                         + "LINESTRING(0 0, 10 0, 10 9, 0 10, 0 0))", null);
  }

  public void testMinPolygonArea_geometryCollection3() {
    doTestMinPolygonArea(true,
                         "GEOMETRYCOLLECTION(POLYGON((0 0, 10 0, 10 9, 0 10, 0 0)),"
                         + "LINESTRING(0 0, 10 0, 10 9, 0 10, 0 0))", null);
  }

  public void testMinPolygonArea1_lineString() {
    doTestMinPolygonArea(false, "LINESTRING(0 0, 10 0, 10 9, 0 10, 0 0)", null);
  }

  private void doTestMinPolygonArea(boolean smallArea, String wkt, Coordinate errorLocation) {
    testValidator.setCheckingMinPolygonArea(true);
    testValidator.setMinPolygonArea(100);
    ValidationError e = testValidator.validateMinPolygonArea(toFeature(wkt));
    assertTypeEquals(smallArea ? ValidationErrorType.SMALL_AREA : null, e, errorLocation);
  }

  public void testMinSegmentLength1() {
    testValidator.setCheckingMinSegmentLength(true);
    testValidator.setMinSegmentLength(5);
    assertTypeEquals(null, testValidator.validateMinSegmentLength(toFeature(
        "LINESTRING(20 20, 25 20)")));
    assertTypeEquals(ValidationErrorType.SMALL_SEGMENT, testValidator.validateMinSegmentLength(toFeature(
        "LINESTRING(20 20, 24 20)")));
    assertTypeEquals(null, testValidator.validateMinSegmentLength(toFeature(
        "GEOMETRYCOLLECTION("
        + "POINT(10 10),"
        + "LINESTRING(20 20, 100 100),"
        + "LINESTRING(20 20, 25 20),"
        + "LINESTRING(20 20, 200 200)"
        + ")")));
    assertTypeEquals(ValidationErrorType.SMALL_SEGMENT, testValidator.validateMinSegmentLength(toFeature(
        "GEOMETRYCOLLECTION("
        + "POINT(10 10),"
        + "LINESTRING(20 20, 100 100),"
        + "LINESTRING(20 20, 24 20),"
        + "LINESTRING(20 20, 200 200)"
        + ")")));
  }

  public void testValidateGeometriesSimple() {
    testValidator.setCheckingGeometriesSimple(true);
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("GEOMETRYCOLLECTION EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("MULTIPOINT EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("MULTILINESTRING EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("MULTIPOLYGON EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("POINT EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("LINESTRING EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("LINEARRING EMPTY")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature("POLYGON EMPTY")));

    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature(
        "LINESTRING(0 0, 10 10, 10 0)")));
    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature(
        "MULTILINESTRING((100 100, 200 200), (0 0, 10 10, 10 0))")));

    assertTypeEquals(ValidationErrorType.NONSIMPLE, testValidator.validateGeometriesSimple(toFeature(
        "LINESTRING(0 0, 10 10, 10 0, 0 10)")));
    assertTypeEquals(ValidationErrorType.NONSIMPLE, testValidator.validateGeometriesSimple(toFeature(
        "MULTILINESTRING((100 100, 200 200), (0 0, 10 10, 10 0, 0 10))")),
        new Coordinate(5, 5));

    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature(
        "LINESTRING(0 0, 10 10, 10 0, 0 0)")));

    assertTypeEquals(null, testValidator.validateGeometriesSimple(toFeature(
        "LINEARRING(0 0, 10 10, 10 0, 0 0)")));
  }

  public void testValidatePolygonOrientation() {
    testValidator.setCheckingPolygonOrientation(true);
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("GEOMETRYCOLLECTION EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("MULTIPOINT EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("MULTILINESTRING EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("MULTIPOLYGON EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("POINT EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("LINESTRING EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("LINEARRING EMPTY")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature("POLYGON EMPTY")));

    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature(
        "POLYGON((0 0, 0 100, 100 100, 100 0, 0 0), (30 30, 70 30, 70 70, 30 70, 30 30))")));
    assertTypeEquals(null, testValidator.validatePolygonOrientation(toFeature(
        "MULTIPOLYGON( "
        + "((10 10, 20 20, 30 20, 10 10)),"
        + "((0 0, 0 100, 100 100, 100 0, 0 0), (30 30, 70 30, 70 70, 30 70, 30 30))   )")));

    assertTypeEquals(ValidationErrorType.EXTERIOR_RING_CCW, testValidator.validatePolygonOrientation(toFeature(
        "POLYGON((0 0, 100 0, 100 100, 0 100, 0 0), (30 30, 70 30, 70 70, 30 70, 30 30))")));
    assertTypeEquals(ValidationErrorType.EXTERIOR_RING_CCW, testValidator.validatePolygonOrientation(toFeature(
        "MULTIPOLYGON( "
        + "((10 10, 20 20, 30 20, 10 10)),"
        + "((0 0, 100 0, 100 100, 0 100, 0 0), (30 30, 70 30, 70 70, 30 70, 30 30))   )")));

    assertTypeEquals(ValidationErrorType.INTERIOR_RING_CW, testValidator.validatePolygonOrientation(toFeature(
        "POLYGON((0 0, 0 100, 100 100, 100 0, 0 0), (30 30, 30 70, 70 70, 70 30, 30 30))")));
    assertTypeEquals(ValidationErrorType.INTERIOR_RING_CW, testValidator.validatePolygonOrientation(toFeature(
        "MULTIPOLYGON( "
        + "((10 10, 20 20, 30 20, 10 10)),"
        + "((0 0, 0 100, 100 100, 100 0, 0 0), (30 30, 30 70, 70 70, 70 30, 30 30))   )")));

    assertTypeEquals(ValidationErrorType.EXTERIOR_RING_CCW, testValidator.validatePolygonOrientation(toFeature(
        "MULTIPOLYGON( "
        + "((10 10, 20 20, 30 20, 10 10)),"
        + "((0 0, 100 0, 100 100, 0 100, 0 0))   )")),
        new Coordinate(50, 50));
  }

  private WKTReader reader = new WKTReader(geometryFactory);

  private void assertTypeEquals(ValidationErrorType type, ValidationError error) {
    assertTypeEquals(type, error, null);
  }

  private void assertTypeEquals(ValidationErrorType type, ValidationError error,
                                Coordinate errorLocation) {
    if (type == null) {
      String message = error != null ? error.getMessage() : "";
      assertNull(message, error);
    }
    else {
      assertNotNull(error);
      assertEquals(type, error.getType());
      if (errorLocation != null) {
        assertEquals(errorLocation, error.getLocation());
      }
    }
  }

  private Feature toFeature(String wkt) {
    Geometry g = null;
    try {
      g = reader.read(wkt);
    }
    catch (ParseException ex) {
      assertTrue(ex.toString(), false);
    }
    return toFeature(g);
  }

  private Feature toFeature(Geometry g) {
    FeatureSchema schema = new FeatureSchema();
    schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    Feature feature = new BasicFeature(schema);
    feature.setGeometry(g);
    return feature;
  }
  protected void setUp() throws java.lang.Exception {
    super.setUp();
    testValidator = new TestValidator();
  }

}
