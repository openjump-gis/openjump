package jumptest.junit;

import java.io.FileReader;
import java.util.List;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.util.io.SimpleGMLReader;

public class SimpleGMLReaderTestCase extends TestCase {

  public SimpleGMLReaderTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {SimpleGMLReaderTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private WKTReader wktReader = new WKTReader(new GeometryFactory());

  public void test() throws Exception {
    List geometries;
    FileReader fileReader = new FileReader(TestUtil.toFile("3points.xml"));
    try {
      geometries = new SimpleGMLReader().toGeometries(fileReader,
          "dataFeatures", "Feature", "gml:pointProperty");
    }
    finally {
      fileReader.close();
    }
    assertEquals(3, geometries.size());
    assertEquals(new Coordinate(1195523.78545869, 382130.432621668),
                 ((Geometry)geometries.get(0)).getCoordinate());
  }
}
