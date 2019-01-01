package jumptest.junit;

import junit.framework.TestCase;

import com.vividsolutions.jump.feature.*;

public class FeatureCollectionMetaDataTestCase extends TestCase {

//<<TODO>> Rename to FeatureTestCase, as it tests the various classes in
//the feature package [Jon Aquino]

//<<TODO>> Move the JUnit test cases to a subpackage called junit [Jon Aquino]

  public FeatureCollectionMetaDataTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {FeatureCollectionMetaDataTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
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
    FeatureDataset c = new FeatureDataset(m);
    assertTrue(c.isEmpty());
  }

}
