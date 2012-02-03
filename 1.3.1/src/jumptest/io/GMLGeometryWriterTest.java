package jumptest.io;

import java.io.*;
import java.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.io.*;

public class GMLGeometryWriterTest {

  public static void main(String[] args) throws Exception
  {
    GMLGeometryWriterTest test = new GMLGeometryWriterTest();
    try {
      test.run();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public GMLGeometryWriterTest() {
  }

  public void run()
      throws Exception
  {
    String filename = "x:\\jcs\\testOutput\\testGML.jml";
    FeatureCollection fc = example1();
    JMLWriter writer = new JMLWriter();
    DriverProperties dp = new DriverProperties();
    dp.set("File", filename);
    writer.write(fc, dp);
  }

  public FeatureCollection example1()
      throws Exception
  {
    //GeometryFactory factor = new GeometryFactory();
    com.vividsolutions.jts.io.WKTReader rdr = new com.vividsolutions.jts.io.WKTReader();
    List geomList = new ArrayList();
    geomList.add(rdr.read(
                          "POINT (100 100)"
    ));
    geomList.add(rdr.read(
"MULTIPOINT (100 100, 200 200)"
    ));
    geomList.add(rdr.read(
"LINESTRING (100 100, 200 200)"
    ));
    geomList.add(rdr.read(
"MULTILINESTRING ((4586.8 2604.7, 5393.3 2315.6, 5302 1509.1, 4647.7 2011.3, 5591.1 2011.3), (4525.9 3426.4, 5317.2 3167.7, 4632.4 2924.3, 5332.4 2756.9))"
    ));
    geomList.add(rdr.read(
"POLYGON ((1000 1000, 1000 2000, 2000 2000, 2000 1000, 1000 1000), (1168 1227.8, 1499.9 1227.8, 1499.9 1469.7, 1168 1469.7, 1168 1227.8), (1643.6 1606.3, 1848.4 1606.3, 1848.4 1852.1, 1643.6 1852.1, 1643.6 1606.3))"
    ));
    geomList.add(rdr.read(
    "POLYGON ((-145 5802, -248 6664, 409 6130, 759 6931, 1149 6151, 2710 6089, 1293 5699, 1272 5103, 327 4652, 697 5145, -761 4631, 451 5206, -1295 5124, -207 5350, -1295 5720, -515 5761, -145 5802))"
    ));
    geomList.add(rdr.read(
"GEOMETRYCOLLECTION (POINT (5626 6089), POINT (4866 5555), LINESTRING (4558 5165, 6140 5678, 4332 5987, 5441 6336), POLYGON ((4250 6418, 4250 6746, 4805 6746, 4805 6418, 4250 6418)))"
));
    return FeatureDatasetFactory.createFromGeometry(geomList);
  }
}
