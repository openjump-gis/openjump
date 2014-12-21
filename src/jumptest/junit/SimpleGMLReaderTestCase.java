package jumptest.junit;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.util.io.SimpleGMLReader;
import junit.framework.TestCase;

import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;

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
    FileInputStream fileInputStream = new FileInputStream(SimpleGMLReaderTestCase.class.getClassLoader().getResource("jumptest/data/3points.xml").toURI().getPath());
    try {
      geometries = new SimpleGMLReader().toGeometries(fileInputStream,
          "dataFeatures", "Feature", "gml:pointProperty");
    }
    finally {
      fileInputStream.close();
    }
    assertEquals(3, geometries.size());
    assertEquals(new Coordinate(1195523.78545869, 382130.432621668),
                 ((Geometry)geometries.get(0)).getCoordinate());
  }
}
