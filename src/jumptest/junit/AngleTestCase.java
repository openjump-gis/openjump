package jumptest.junit;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.geom.Angle;

public class AngleTestCase extends TestCase {

  public AngleTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {AngleTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  public void testAngle() {
    assertEquals(Angle.toRadians(0),
          Angle.angle(new Coordinate(0, 0), new Coordinate(5, 0)), 1E-10);
    assertEquals(Angle.toRadians(45),
          Angle.angle(new Coordinate(0, 0), new Coordinate(5, 5)), 1E-10);
    assertEquals(Angle.toRadians(90),
          Angle.angle(new Coordinate(0, 0), new Coordinate(0, 5)), 1E-10);
    assertEquals(Angle.toRadians(135),
          Angle.angle(new Coordinate(0, 0), new Coordinate(-5, 5)), 1E-10);
    assertEquals(Angle.toRadians(180),
          Angle.angle(new Coordinate(0, 0), new Coordinate(-5, 0)), 1E-10);
    assertEquals(Angle.toRadians(-135),
          Angle.angle(new Coordinate(0, 0), new Coordinate(-5, -5)), 1E-10);
    assertEquals(Angle.toRadians(-90),
          Angle.angle(new Coordinate(0, 0), new Coordinate(0, -5)), 1E-10);
    assertEquals(Angle.toRadians(-45),
          Angle.angle(new Coordinate(0, 0), new Coordinate(5, -5)), 1E-10);
  }
}
