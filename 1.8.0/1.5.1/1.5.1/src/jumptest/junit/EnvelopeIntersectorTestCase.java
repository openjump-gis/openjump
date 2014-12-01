package jumptest.junit;
import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.geom.EnvelopeIntersector;

public class EnvelopeIntersectorTestCase extends TestCase {

  public EnvelopeIntersectorTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {EnvelopeIntersectorTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private WKTReader reader = new WKTReader(new GeometryFactory());

  public void testRobustIntersects() throws Exception {
    assertTrue(EnvelopeIntersector.intersects(reader.read("POINT(5 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read("POINT(5 10)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read("POINT(5 15)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "MULTIPOINT(100 100, 5 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "MULTIPOINT(100 100, 5 10)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "MULTIPOINT(100 100, 5 15)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read("LINESTRING(3 3, 5 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read("LINESTRING(5 15, 5 10)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read("LINESTRING(5 15, 5 11)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "POLYGON((0 0, 100 0, 100 100, 0 100, 0 0), (10 10, 90 10, 90 90, 10 90, 10 10))"),
        new Envelope(-15, 105, -15, 105)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "POLYGON((0 0, 100 0, 100 100, 0 100, 0 0), (10 10, 90 10, 90 90, 10 90, 10 10))"),
        new Envelope(5, 95, 5, 95)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "POLYGON((0 0, 100 0, 100 100, 0 100, 0 0), (10 10, 90 10, 90 90, 10 90, 10 10))"),
        new Envelope(15, 85, 15, 85)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "GEOMETRYCOLLECTION(POINT(100 100), POINT(5 5))"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "GEOMETRYCOLLECTION(POINT(100 100), POINT(5 10))"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "GEOMETRYCOLLECTION(POINT(100 100), POINT(5 15))"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "LINESTRING(-15 5, -5 5, 5 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "LINESTRING(5 5, 15 5, 25 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(EnvelopeIntersector.intersects(reader.read(
        "LINESTRING(-5 5, 15 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "MULTIPOINT(-5 5, 15 5)"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "POINT EMPTY"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "LINESTRING EMPTY"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "POLYGON EMPTY"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "MULTILINESTRING EMPTY"),
        new Envelope(0, 10, 0, 10)));
    assertTrue(!EnvelopeIntersector.intersects(reader.read(
        "GEOMETRYCOLLECTION EMPTY"),
        new Envelope(0, 10, 0, 10)));
  }
}
