package jumptest.junit;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.geom.InteriorPointFinder;

public class InteriorPointFinderTestCase extends TestCase {

  public InteriorPointFinderTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {InteriorPointFinderTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private class TestFinder extends InteriorPointFinder {
    protected Geometry widestGeometry(Geometry geometry) {
      return super.widestGeometry(geometry);
    }
    protected Geometry envelopeMiddle(Geometry geometry) {
      return super.envelopeMiddle(geometry);
    }
  }

  private TestFinder finder;

  protected void setUp() {
    finder = new TestFinder();
  }

  private void assertEquals(Geometry expected, Geometry actual) {
    //JTS has a bug comparing empty geometries -- two empty points are not equal.
    if (expected.isEmpty() && actual.isEmpty() && expected.getClass() == actual.getClass()) {
      return;
    }
    assertTrue("Expected " + expected + " but found " + actual, expected.equals(actual));
  }

  public void testWidestGeometry1() throws ParseException {
    assertEquals(reader.read("POINT EMPTY"),
        finder.widestGeometry(reader.read("POINT EMPTY")));
  }

  public void testWidestGeometry2() throws ParseException {
    //Can't use assertEquals because GeometryCollection doesn't support #equals [Jon Aquino]
    Geometry g = finder.widestGeometry(reader.read("GEOMETRYCOLLECTION EMPTY"));
    assertTrue(g.getClass().getName().equals(GeometryCollection.class.getName()));
    assertTrue(g.isEmpty());
  }

  public void testWidestGeometry3() throws ParseException {
    assertEquals(reader.read("POINT(5 5)"),
        finder.widestGeometry(reader.read("POINT(5 5)")));
  }

  public void testWidestGeometry4() throws ParseException {
    assertEquals(reader.read("LINESTRING(10 0, 20 0)"),
        finder.widestGeometry(reader.read("MULTILINESTRING("
        + "(0 0, 5 0),"
        + "(10 0, 20 0),"
        + "(30 0, 31 0)"
        + ")")));
  }

  private WKTReader reader = new WKTReader(new GeometryFactory());

  public void testPolygon1() throws ParseException {
    assertEquals(new Coordinate(5, 5), finder.findPoint(reader.read(
        "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")));
  }

  public void testPolygon2() throws ParseException {
    assertEquals(new Coordinate(1, 5), finder.findPoint(reader.read(
        "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0),"
        + "(2 2, 8 2, 8 8, 2 8, 2 2))")));
  }

  public void testPoint() throws ParseException {
    assertEquals(new Coordinate(3, 5), finder.findPoint(reader.read(
        "POINT(3 5)")));
  }

  public void testMultiPoint() throws ParseException {
    assertEquals(new Coordinate(1, 0), finder.findPoint(reader.read(
        "MULTIPOINT(1 0, 5 0, 0 5)")));
  }

  public void testLineString() throws ParseException {
    assertEquals(new Coordinate(0, 5), finder.findPoint(reader.read(
        "LINESTRING(0 10, 0 0, 10 0)")));
  }

  public void testEmptyPoint() throws ParseException {
    assertEquals(new Coordinate(0, 0), finder.findPoint(reader.read(
        "POINT EMPTY")));
  }

  public void testEmptyPolygon() throws ParseException {
    assertEquals(new Coordinate(0, 0), finder.findPoint(reader.read(
        "POLYGON EMPTY")));
  }

  public void testEnvelopeMiddle() throws ParseException {
    assertEquals(reader.read("LINESTRING(0 5, 10 5)")
        , finder.envelopeMiddle(reader.read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")));
  }

  public void testMultiPolygon() throws ParseException {
    //Date is from northsaanich_ici.xml [Jon Aquino]
    Geometry g = reader.read("MULTIPOLYGON (((465580.832271147 5391464.52023311, 465580.111202618 5391336.89734026, 465579.752372068 5391273.38743641, 465479.252315499 5391273.99424146, 465479.920331542 5391384.63216379, 465480.40616139 5391465.09610732, 465480.466867918 5391475.15042287, 465580.889078094 5391474.57457145, 465580.832271147 5391464.52023311)), ((470158.222263892 5391788.98525343, 470183.197563252 5391788.47146625, 470182.581834725 5391762.27738734, 470112.540056973 5391759.49053165, 470110.317662615 5391768.46067921, 470110.01382665 5391769.80577349, 470108.795865109 5391777.57013429, 470108.610418023 5391778.93659114, 470108.073799825 5391786.77755855, 470107.919263704 5391790.02007733, 470158.222263892 5391788.98525343)))");
    System.out.println(finder.envelopeMiddle(g));
    Coordinate p = finder.findPoint(g);
    assertTrue(p + "", g.getEnvelopeInternal().contains(p));
  }

}
