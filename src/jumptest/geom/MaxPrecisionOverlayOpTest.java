package jumptest.geom;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.geom.MaxPrecisionOverlayOp;

public class MaxPrecisionOverlayOpTest {

  public static void main(String[] args) throws Exception
  {
    MaxPrecisionOverlayOpTest test = new MaxPrecisionOverlayOpTest();
    test.run();
  }

  static GeometryFactory fact = new GeometryFactory();
  static WKTReader wktRdr = new WKTReader(fact);

  public MaxPrecisionOverlayOpTest() {
  }

  public void run()
  {
    doubleManip();
    try {
      String gs0 = "POLYGON ((477598.2961 5367266.452, 477574.7625 5367238.688, 477569.6831 5367232.69, 477565.9399 5367203.046, 477562.2049 5367173.512, 477558.874 5367147.208, 477555.9223 5367123.873, 477553.8198 5367107.226, 477551.9398 5367103.227, 477542.4819 5367083.112, 477532.1088 5367061.056, 477521.7275 5367039.001, 477511.3543 5367016.946, 477500.9812 5366994.89, 477490.608 5366972.835, 477482.4942 5366955.589, 477451.7445 5366983.693, 477031.1214 5367184.259, 477307.7571 5367512.43, 477598.2961 5367266.452))";
      Geometry g0 = wktRdr.read(gs0);

      String gs1 = "POLYGON ((477481.626654971 5367413.24717737, 477498.354637662 5367399.06852002, 477474.616159523 5367371.16270802, 477457.875877778 5367385.33547321, 477481.626654971 5367413.24717737))";
      Geometry g1 = wktRdr.read(gs1);

      MaxPrecisionOverlayOp op = new MaxPrecisionOverlayOp();
      Geometry result = op.intersection(g0, g1);
      System.out.println(result);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  public void doubleManip()
  {
    //long xl = 100000100011101001001100111100100101111001101001101011010100001;
    //long xl = 0x411d26792f34d6a1
    long xl   = 0x411d000000000000L;
    double x = Double.longBitsToDouble(xl);
    System.out.println(x);

    //long xl = 0x4154792ae5b22d0e
    long yl   = 0x4154790000000000L;
    double y = Double.longBitsToDouble(yl);
    System.out.println(y);

  }
}
