package jumptest.junit;

import java.io.File;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.*;

public class TestUtil {

  public TestUtil() {
  }

  /**
   * Converts a Geometry to a Feature.
   * @param geometry the geometry to wrap in a Feature
   * @return a Feature based on a simple FeatureSchema
   */
  public static Feature toFeature(Geometry geometry) {
    FeatureSchema geometrySchema = new FeatureSchema();
    geometrySchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    Feature f = new BasicFeature(geometrySchema);
    f.setGeometry(geometry);
    return f;
  }

  public static File toFile(String filename) {
    String parent = System.getProperty("jump-test-data-directory");
    Assert.isTrue(parent != null);
    return new File(parent, filename);
  }

}
