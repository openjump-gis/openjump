package jumptest.junit;

import java.awt.Color;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.workbench.ui.cursortool.SplitLineStringTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SplitLineStringsOp;

import junit.framework.TestCase;

public class SplitLineStringToolTestCase extends TestCase {

	public SplitLineStringToolTestCase(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(SplitLineStringToolTestCase.class);
	}

	class TestSplitLineStringsOp extends SplitLineStringsOp {
		public TestSplitLineStringsOp() {
			super(Color.black);
		}

		public LineString[] split(LineString lineString, Coordinate coordinate, boolean moveSplitToTarget) {
			return super.split(lineString, coordinate, moveSplitToTarget);
		}
	}

	public void testSplit() throws Exception {
		assertSplitEquals("LINESTRING (0 0, 1 1, 2 1, 3 2)", 1.5, 1.1,
				"LINESTRING (0 0, 1 1, 1.5 1)", "LINESTRING (1.5 1, 2 1, 3 2)", false);
		assertSplitEquals("LINESTRING (0 0, 1 1, 2 1, 3 2)", 1.5, 1.1,
				"LINESTRING (0 0, 1 1, 1.5 1.1)", "LINESTRING (1.5 1.1, 2 1, 3 2)", true);		
		assertSplitEquals("LINESTRING (0 0 50, 1 1 51, 2 1 52, 3 2 53)", 1.5,
				1.1, "LINESTRING (0 0 50, 1 1 51, 1.5 1 51.5)",
				"LINESTRING (1.5 1 51.5, 2 1 52, 3 2 53)", false);
		assertSplitEquals("LINESTRING (0 0 50, 1 1 51, 2 1 52, 3 2 53)", 1.75,
				1.1, "LINESTRING (0 0 50, 1 1 51, 1.75 1 51.75)",
				"LINESTRING (1.75 1 51.75, 2 1 52, 3 2 53)", false);
		assertSplitEquals("LINESTRING (0 0 50, 1 1, 2 1 52, 3 2 53)", 1.5, 1.1,
				"LINESTRING (0 0 50, 1 1, 1.5 1)",
				"LINESTRING (1.5 1, 2 1 52, 3 2 53)", false);
		assertSplitEquals("LINESTRING (0 0 50, 1 1 51, 2 1, 3 2 53)", 1.5, 1.1,
				"LINESTRING (0 0 50, 1 1 51, 1.5 1)",
				"LINESTRING (1.5 1, 2 1, 3 2 53)", false);
		assertSplitEquals("LINESTRING (0 0 50, 1 1 99, 2 0 48)", 1, 1.1,
				"LINESTRING (0 0 50, 1 1 99)", "LINESTRING (1 1 99, 2 0 48)", false);
		assertSplitEquals("LINESTRING (0 0 50, 1 1, 2 0 48)", 1, 1.1,
				"LINESTRING (0 0 50, 1 1 49)", "LINESTRING (1 1 49, 2 0 48)", false);
	}

	private void assertSplitEquals(String input, double x, double y,
			String output1, String output2, boolean moveSplitToTarget) throws ParseException {
		LineString[] lineStrings = new TestSplitLineStringsOp().split(
				(LineString) new WKTReader().read(input), new Coordinate(x, y), moveSplitToTarget);
		assertCoordinatesEqual(new WKTReader().read(output1), lineStrings[0]);
		assertCoordinatesEqual(new WKTReader().read(output2), lineStrings[1]);
	}

	private void assertCoordinatesEqual(Geometry expected, Geometry actual) {
		assertEquals(expected.getNumPoints(), actual.getNumPoints());
		for (int i = 0; i < expected.getCoordinates().length; i++) {
			assertCoordinatesEqual(expected.getCoordinates()[i], actual
					.getCoordinates()[i]);
		}
	}

	private void assertCoordinatesEqual(Coordinate expected, Coordinate actual) {
		assertOrdinatesEqual(expected.x, actual.x);
		assertOrdinatesEqual(expected.y, actual.y);
		assertOrdinatesEqual(expected.z, actual.z);
	}

	private void assertOrdinatesEqual(double expected, double actual) {
		assertTrue("Expected: " + expected + " but was: " + actual, (Double
				.isNaN(expected) && Double.isNaN(actual))
				|| Math.abs(expected - actual) < 1E-7);
	}

}