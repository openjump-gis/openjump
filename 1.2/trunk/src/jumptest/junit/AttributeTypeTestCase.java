package jumptest.junit;

import junit.framework.TestCase;

import com.vividsolutions.jump.feature.AttributeType;

public class AttributeTypeTestCase extends TestCase {

  public AttributeTypeTestCase(String Name_) {
    super(Name_);
  }
  public static void main(String[] args) {
    String[] testCaseName = {AttributeTypeTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }
    public void testToString() {
    assertEquals("STRING", AttributeType.STRING.toString());
    assertEquals("DOUBLE", AttributeType.DOUBLE.toString());
    assertEquals("INTEGER", AttributeType.INTEGER.toString());
    assertEquals("DATE", AttributeType.DATE.toString());
    assertEquals("GEOMETRY", AttributeType.GEOMETRY.toString());
  }
    public void testToAttributeType() {
    assertSame(AttributeType.STRING, AttributeType.toAttributeType("STRING"));
    assertSame(AttributeType.DOUBLE, AttributeType.toAttributeType("DOUBLE"));
    assertSame(AttributeType.INTEGER, AttributeType.toAttributeType("INTEGER"));
    assertSame(AttributeType.DATE, AttributeType.toAttributeType("DATE"));
    assertSame(AttributeType.GEOMETRY, AttributeType.toAttributeType("GEOMETRY"));
    try {
      AttributeType.toAttributeType("X");
      assertTrue(false);
    }
    catch (IllegalArgumentException e) {
    }
  }
}
