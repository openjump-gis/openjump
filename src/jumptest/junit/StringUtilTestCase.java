package jumptest.junit;

import java.util.ArrayList;

import junit.framework.TestCase;

import com.vividsolutions.jump.util.StringUtil;

public class StringUtilTestCase extends TestCase {

  public StringUtilTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {StringUtilTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  public void testToCommaDelimitedString() {
    ArrayList a = new ArrayList();
    a.add("A");
    a.add("B");
    a.add("C");
    assertEquals("A, B, C", StringUtil.toCommaDelimitedString(a));
  }
  
  public void testToFriendlyName() {
      assertEquals("String", StringUtil.toFriendlyName(String.class.getName()));
  }
  
  public void testCapitalize() {
      assertEquals("", StringUtil.capitalize(""));
      assertEquals("1a2b x", StringUtil.capitalize("1a2b x"));
      assertEquals("A2b3 x", StringUtil.capitalize("a2b3 x"));
      assertEquals("A2B3 x", StringUtil.capitalize("A2B3 x"));      
  }
  
  public void testUncapitalize() {
      assertEquals("", StringUtil.uncapitalize(""));
      assertEquals("1a2b x", StringUtil.uncapitalize("1a2b x"));
      assertEquals("a2b3 x", StringUtil.uncapitalize("a2b3 x"));
      assertEquals("a2B3 x", StringUtil.uncapitalize("A2B3 x"));      
  }  
}
