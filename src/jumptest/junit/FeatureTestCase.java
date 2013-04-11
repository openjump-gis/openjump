package jumptest.junit;
import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.*;

public class FeatureTestCase extends TestCase {

//<<TODO>> Rename to FeatureTestCase, as it tests the various classes in
//the feature package [Jon Aquino]

//<<TODO>> Move the JUnit test cases to a subpackage called junit [Jon Aquino]

  public FeatureTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {FeatureTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private GeometryFactory factory = new GeometryFactory();

  public void testClone() {
    FeatureSchema fs = new FeatureSchema();
    fs.addAttribute("integer", AttributeType.INTEGER);
    fs.addAttribute("double", AttributeType.DOUBLE);
    fs.addAttribute("geometry", AttributeType.GEOMETRY);
    fs.addAttribute("string", AttributeType.STRING);
    Feature f = new BasicFeature(fs);
    f.setAttribute(0, new Integer(3));
    f.setAttribute(1, new Double(4.5));
    f.setAttribute(2, factory.createPoint(new Coordinate(6, 7)));
    f.setAttribute(3, "abc");
    Feature c = (Feature) f.clone(false);
    assertEquals(3, c.getInteger(0));
    assertEquals(4.5, c.getDouble(1), 0.00001);
    assertTrue(factory.createPoint(new Coordinate(6, 7)).equals(c.getGeometry()));
    assertEquals("abc", c.getString(3));
  }

  public void testAdd() {
    FeatureSchema m = new FeatureSchema();
    m.addAttribute("featureType", AttributeType.STRING);
    m.addAttribute("igds_class", AttributeType.STRING);
    m.addAttribute("igds_color", AttributeType.STRING);
    assertEquals(3, m.getAttributeCount());
    assertEquals(2, m.getAttributeIndex("igds_color"));
    try {
      assertEquals(-1, m.getAttributeIndex("IGDS_CLASS"));
      assertTrue(false);
    }
    catch (IllegalArgumentException e) {
    }
    try {
      assertEquals(-1, m.getAttributeIndex("ABC123"));
      assertTrue(false);
    }
    catch (IllegalArgumentException e) {
    }
  }

  public void testIsEmpty() {
    FeatureSchema m = new FeatureSchema();
    FeatureCollection c = new FeatureDataset(m);
    assertTrue(c.isEmpty());
  }

}
