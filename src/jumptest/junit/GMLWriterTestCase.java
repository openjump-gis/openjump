package jumptest.junit;

import java.text.SimpleDateFormat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.AssertionFailedException;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.GMLWriter;

import junit.framework.TestCase;


public class GMLWriterTestCase extends TestCase {
    private TestWriter writer = new TestWriter();
    private GeometryFactory factory = new GeometryFactory();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    public GMLWriterTestCase(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GMLWriterTestCase.class);
    }

    public void testToString() throws Exception {
        doAssert("ABC 123", "ABC 123", AttributeType.STRING);
        doAssert("", null, AttributeType.STRING);
        doAssert("", "", AttributeType.STRING);
        doAssert(" ", " ", AttributeType.STRING);
        doAssert("1.23", new Double(1.23), AttributeType.DOUBLE);
        doAssert("", null, AttributeType.DOUBLE);
        doAssert("4", new Integer(4), AttributeType.INTEGER);
        doAssert("", null, AttributeType.INTEGER);
        doAssert("1921-04-18", dateFormatter.parse("1921-04-18"), AttributeType.DATE);
        doAssert("", null, AttributeType.DATE);

        try {
            doAssert("", factory.createPoint(new Coordinate()),
                AttributeType.GEOMETRY);
            assertTrue(false);
        } catch (AssertionFailedException e) {
            assertTrue(true);
        }
    }

    private void doAssert(String expectedString, Object attributeValue,
        AttributeType type) {
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("x", type);

        Feature f = new BasicFeature(schema);
        f.setAttribute("x", attributeValue);
        assertEquals(expectedString, writer.toString(f, "x"));
    }

    private static class TestWriter extends GMLWriter {
        protected String toString(Feature f, String column) {
            return super.toString(f, column);
        }
    }
}
