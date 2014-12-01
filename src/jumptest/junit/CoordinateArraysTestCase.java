package jumptest.junit;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jump.util.CoordinateArrays;

import junit.framework.TestCase;


public class CoordinateArraysTestCase extends TestCase {
    public CoordinateArraysTestCase(String Name_) {
        super(Name_);
    }

    public static void main(String[] args) {
        String[] testCaseName = { CoordinateArraysTestCase.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testReverseOddNumberOfCoordinates() {
        Coordinate[] c = new Coordinate[] {
                new Coordinate(0, 1), new Coordinate(2, 3), new Coordinate(4, 5),
            };
        CoordinateArrays.reverse(c);
        assertEquals(new Coordinate(4, 5), c[0]);
        assertEquals(new Coordinate(2, 3), c[1]);
        assertEquals(new Coordinate(0, 1), c[2]);
    }

    public void testReverseEvenNumberOfCoordinates() {
        Coordinate[] c = new Coordinate[] {
                new Coordinate(0, 1), new Coordinate(2, 3), new Coordinate(4, 5),
                new Coordinate(6, 7),
            };
        CoordinateArrays.reverse(c);
        assertEquals(new Coordinate(6, 7), c[0]);
        assertEquals(new Coordinate(4, 5), c[1]);
        assertEquals(new Coordinate(2, 3), c[2]);
        assertEquals(new Coordinate(0, 1), c[3]);
    }
}
