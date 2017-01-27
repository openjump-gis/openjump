package jumptest.junit;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.workbench.ui.plugin.VerticesInFencePlugIn;

public class VerticesInFencePlugInTestCase extends TestCase {

  public VerticesInFencePlugInTestCase(String Name_) {
    super(Name_);
  }
  public static void main(String[] args) {
    String[] testCaseName = {VerticesInFencePlugInTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private WKTReader reader = new WKTReader(new GeometryFactory());

  public void test() throws Exception {
    Geometry g = reader.read(
        "MULTILINESTRING(" +
        "(0 0, 100 0, 100 100, 0 0)," +
        "(0 0, 50 10, 10 50, 0 0))");
    VerticesInFencePlugIn.VerticesInFence coordinates =
        VerticesInFencePlugIn.verticesInFence(g, reader.read(
        "POLYGON((0 0, 100 0, 100 90, 0 90, 0 0))"), true);
    assertEquals(5, coordinates.getCoordinates().size());
    assertTrue(coordinates.getCoordinates().get(0) instanceof Coordinate);
    assertEquals(0, 0, 0, (Coordinate)coordinates.getCoordinates().get(0), coordinates.getIndex(0));
    assertEquals(100, 0, 1, (Coordinate)coordinates.getCoordinates().get(1), coordinates.getIndex(1));
    assertEquals(0, 0, 4, (Coordinate)coordinates.getCoordinates().get(2), coordinates.getIndex(2));
    assertEquals(50, 10, 5, (Coordinate)coordinates.getCoordinates().get(3), coordinates.getIndex(3));
    assertEquals(10, 50, 6, (Coordinate)coordinates.getCoordinates().get(4), coordinates.getIndex(4));
  }

  private void assertEquals(double x, double y, int index, Coordinate actualCoordinate, int actualIndex) {
    assertEquals(x, actualCoordinate.x, 1E-15);
    assertEquals(y, actualCoordinate.y, 1E-15);
    assertEquals(index, actualIndex);
  }
}
