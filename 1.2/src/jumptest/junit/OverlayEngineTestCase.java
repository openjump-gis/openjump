package jumptest.junit;
import junit.framework.TestCase;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.tools.OverlayEngine;

public class OverlayEngineTestCase extends TestCase {

  public OverlayEngineTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {OverlayEngineTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private static class TestEngine extends OverlayEngine {
    public TestEngine() {
      super();
    }
  }

  public void testAddFeature() throws Exception {
    String a = "MULTIPOLYGON("
          + "((5 0, 15 0, 15 10, 5 10, 5 0), (11 1, 12 1, 12 2, 11 2, 11 1)),"
          + "((20 0, 30 0, 30 10, 20 10, 20 0), (27 1, 28 1, 28 2, 27 2, 27 1)) )";
    String b = "MULTIPOLYGON("
          + "((0 5, 10 5, 10 15, 0 15, 0 5), (1 10, 2 10, 2 11, 1 11, 1 10)),"
          + "((25 5, 35 5, 35 15, 25 15, 25 5), (32 10, 33 10, 33 11, 32 11, 32 10)),"
          + "((30 0, 35 0, 35 -5, 30 -5, 30 0), (32 -1, 33 -1, 33 -2, 32 -2, 32 -1)) )";
    assertEquals(2, doTest(a, b, true, true));
    assertEquals(3, doTest(a, b, true, false));
    assertEquals(0, doTest(a, b, false, true));
    assertEquals(1, doTest(a, b, false, false));
  }

  private WKTReader reader = new WKTReader(new GeometryFactory());

  private int doTest(String wktA, String wktB,
        boolean splittingGeometryCollections, boolean allowingPolygonsOnly)
        throws ParseException {
    TestEngine te = new TestEngine();
    te.setSplittingGeometryCollections(splittingGeometryCollections);
    te.setAllowingPolygonsOnly(allowingPolygonsOnly);
    FeatureSchema schema = new FeatureSchema();
    schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    Feature aFeature = new BasicFeature(schema);
    Feature bFeature = new BasicFeature(schema);
    aFeature.setAttribute(0, reader.read(wktA));
    bFeature.setAttribute(0, reader.read(wktB));
    FeatureCollection a = new FeatureDataset(schema);
    FeatureCollection b = new FeatureDataset(schema);
    a.add(aFeature);
    b.add(bFeature);
    return te.overlay(a, b, new DummyTaskMonitor()).size();
  }

}
