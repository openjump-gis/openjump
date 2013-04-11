package jumptest.junit;
import java.util.*;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.tools.AttributeMapping;

public class AttributeMappingTestCase extends TestCase {

  public AttributeMappingTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {AttributeMappingTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  public void testTransferAttributes() {
    FeatureSchema aSchema = new FeatureSchema();
    aSchema.addAttribute("WIDTH", AttributeType.STRING);
    aSchema.addAttribute("HEIGHT", AttributeType.STRING);
    aSchema.addAttribute("SHAPE", AttributeType.GEOMETRY);
    FeatureSchema bSchema = new FeatureSchema();
    bSchema.addAttribute("BREADTH", AttributeType.STRING);
    bSchema.addAttribute("LENGTH", AttributeType.STRING);
    bSchema.addAttribute("REGION", AttributeType.GEOMETRY);
    FeatureSchema cSchema = new FeatureSchema();
    cSchema.addAttribute("A_WIDTH", AttributeType.STRING);
    cSchema.addAttribute("B_WIDTH", AttributeType.STRING);
    cSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

    Feature aFeature = new BasicFeature(aSchema);
    aFeature.setAttribute("WIDTH", "5");
    aFeature.setAttribute("HEIGHT", "6");
    Feature bFeature = new BasicFeature(bSchema);
    bFeature.setAttribute("BREADTH", "7");
    bFeature.setAttribute("LENGTH", "8");
    Feature cFeature = new BasicFeature(cSchema);

    new AttributeMapping(aSchema,
                         Arrays.asList(new String[]{"WIDTH"}),
                         Arrays.asList(new String[]{"A_WIDTH"}),
                         bSchema,
                         Arrays.asList(new String[]{"BREADTH"}),
                         Arrays.asList(new String[]{"B_WIDTH"}))
        .transferAttributes(aFeature, bFeature, cFeature);
    assertEquals("5", cFeature.getAttribute("A_WIDTH"));
    assertEquals("7", cFeature.getAttribute("B_WIDTH"));
  }

  private static class TestAttributeMapping extends AttributeMapping {
    protected boolean isDisjoint(Collection a, Collection b) {
      return super.isDisjoint(a, b);
    }
  }

  public void testIsDisjoint1() {
    ArrayList a = new ArrayList();
    a.add("1");
    a.add("2");
    ArrayList b = new ArrayList();
    b.add("3");
    b.add("4");
    assertTrue(new TestAttributeMapping().isDisjoint(a, b));
  }

  public void testIsDisjoint2() {
    ArrayList a = new ArrayList();
    a.add("1");
    a.add("2");
    ArrayList b = new ArrayList();
    b.add("3");
    b.add("2");
    assertTrue(!new TestAttributeMapping().isDisjoint(a, b));
  }

  public void testFeatureSchemaConstructor() {
    FeatureSchema aSchema = new FeatureSchema();
    aSchema.addAttribute("WIDTH", AttributeType.STRING);
    aSchema.addAttribute("HEIGHT", AttributeType.STRING);
    aSchema.addAttribute("SHAPE", AttributeType.GEOMETRY);
    FeatureSchema bSchema = new FeatureSchema();
    bSchema.addAttribute("WIDTH", AttributeType.STRING);
    bSchema.addAttribute("LENGTH", AttributeType.STRING);
    bSchema.addAttribute("REGION", AttributeType.GEOMETRY);

    AttributeMapping mapping = new AttributeMapping(aSchema, bSchema);
    FeatureSchema cSchema = mapping.createSchema("GEOMETRY");
    assertEquals(5, cSchema.getAttributeCount());
    assertEquals("WIDTH_1", cSchema.getAttributeName(0));
    assertEquals("HEIGHT", cSchema.getAttributeName(1));
    assertEquals("WIDTH_2", cSchema.getAttributeName(2));
    assertEquals("LENGTH", cSchema.getAttributeName(3));
    assertEquals("GEOMETRY", cSchema.getAttributeName(4));

    Feature aFeature = new BasicFeature(aSchema);
    aFeature.setAttribute("WIDTH", "10");
    aFeature.setAttribute("HEIGHT", "11");
    aFeature.setAttribute("SHAPE", factory.createPoint(new Coordinate(1,2)));

    Feature bFeature = new BasicFeature(bSchema);
    bFeature.setAttribute("WIDTH", "20");
    bFeature.setAttribute("LENGTH", "21");
    bFeature.setAttribute("REGION", factory.createPoint(new Coordinate(3,4)));

    Feature cFeature = new BasicFeature(cSchema);
    cFeature.setGeometry(factory.createPoint(new Coordinate(5,6)));
    mapping.transferAttributes(aFeature, bFeature, cFeature);
    assertEquals("10", cFeature.getAttribute("WIDTH_1"));
    assertEquals("11", cFeature.getAttribute("HEIGHT"));
    assertEquals("20", cFeature.getAttribute("WIDTH_2"));
    assertEquals("21", cFeature.getAttribute("LENGTH"));
    assertTrue(factory.createPoint(new Coordinate(5,6))
               .equalsExact((Geometry)cFeature.getAttribute("GEOMETRY")));
  }

  private GeometryFactory factory = new GeometryFactory();

}
