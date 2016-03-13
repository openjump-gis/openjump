package jumptest.junit;
import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.warp.AffineTransform;

public class AffineTransformTestCase extends TestCase {

  public AffineTransformTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {AffineTransformTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  public void test1() {
    Coordinate a1 = new Coordinate(10, 10);
    Coordinate a2 = new Coordinate(20, 0);
    Coordinate b1 = new Coordinate(10, 20);
    Coordinate b2 = new Coordinate(20, 30);
    Coordinate c1 = new Coordinate(20, 20);
    Coordinate c2 = new Coordinate(30, 20);
    AffineTransform f = new AffineTransform(a1, a2, b1, b2, c1, c2);
    assertEquals(a2, f.transform(a1));
    assertEquals(b2, f.transform(b1));
    assertEquals(c2, f.transform(c1));
  }

  public void test2() {
    Coordinate a1 = new Coordinate(0, 10);
    Coordinate a2 = new Coordinate(0, 20);
    Coordinate b1 = new Coordinate(10, 10);
    Coordinate b2 = new Coordinate(20, 20);
    Coordinate c1 = new Coordinate(10, 0);
    Coordinate c2 = new Coordinate(20, 0);
    AffineTransform f = new AffineTransform(a1, a2, b1, b2, c1, c2);
    assertEquals(new Coordinate(10, 10), f.transform(new Coordinate(5, 5)));
  }

  public void test3() {
    Coordinate a1 = new Coordinate(1, 0);
    Coordinate a2 = new Coordinate(1, 0);
    Coordinate b1 = new Coordinate(2, 0);
    Coordinate b2 = new Coordinate(3, 0);
    Coordinate c1 = new Coordinate(3, 0);
    Coordinate c2 = new Coordinate(2, 0);
    try {
      new AffineTransform(a1, a2, b1, b2, c1, c2);
      assertTrue(false);
    }
    catch (RuntimeException e) {
      assertTrue(e.toString().indexOf("singular") > -1);
    }
  }

  public void testRotate90() {
    assertEquals(new Coordinate(10, 0),
        AffineTransform.rotate90(new Coordinate(0, 0), new Coordinate(0, 10)));
    assertEquals(new Coordinate(10, 0),
        AffineTransform.rotate90(new Coordinate(10, 10), new Coordinate(20, 10)));
  }

}
