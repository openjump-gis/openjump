package jumptest.junit;

import jumptest.io.DbfFileTestCase;
import junit.framework.*;

/**
 *@version    $Revision$
 */
public class AllTestCases extends TestCase {

  public AllTestCases(String name) {
    super(name);
  }

  public static Test suite() {
    TestSuite result = new TestSuite();
    result.addTest(new TestSuite(AbstractPlugInTestCase.class));
    result.addTest(new TestSuite(AffineTransformTestCase.class));
    result.addTest(new TestSuite(AngleTestCase.class));
    result.addTest(new TestSuite(AttributeMappingTestCase.class));
    result.addTest(new TestSuite(AttributeTypeTestCase.class));
    result.addTest(new TestSuite(CollectionUtilTestCase.class));
    result.addTest(new TestSuite(DbfFileTestCase.class));
    result.addTest(new TestSuite(EnvelopeIntersectorTestCase.class));
    result.addTest(new TestSuite(FeatureCollectionMetaDataTestCase.class));
    result.addTest(new TestSuite(FeatureTestCase.class));
    result.addTest(new TestSuite(FlexibleDateParserTestCase.class));    
    result.addTest(new TestSuite(GMLWriterTestCase.class));
    result.addTest(new TestSuite(GUIUtilTestCase.class));
    result.addTest(new TestSuite(InteriorPointFinderTestCase.class));
    result.addTest(new TestSuite(OverlayEngineTestCase.class));
    result.addTest(new TestSuite(PanelTestCase.class));
    result.addTest(new TestSuite(RangeTestCase.class));
    result.addTest(new TestSuite(ShapeCollectionPathIteratorTestCase.class));
    result.addTest(new TestSuite(SimpleGMLReaderTestCase.class));
    result.addTest(new TestSuite(StringUtilTestCase.class));
    result.addTest(new TestSuite(TriangulatorTestCase.class));
    result.addTest(new TestSuite(ValidatorTestCase.class));
    result.addTest(new TestSuite(VerticesInFencePlugInTestCase.class));
    return result;
  }

  public static void main(String[] args) {
      System.setProperty("jump-test-data-directory", "jumptest/data/");
    junit.textui.TestRunner.run(suite());
    //Must explicitly exit because any LayerViewPanels created have their own
    //rendering timers that keep going. [Jon Aquino]
    System.exit(0);
    //Comment out the above line when using the swingui TestRunner.
    //[Jon Aquino]
//    junit.swingui.TestRunner.run(AllTestCases.class);
  }

}
