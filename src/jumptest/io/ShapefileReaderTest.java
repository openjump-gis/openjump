package jumptest.io;

import java.util.*;
import com.vividsolutions.jump.io.*;
import com.vividsolutions.jump.feature.*;

public class ShapefileReaderTest {

  public static FeatureCollection loadShapefile(String filename)
      throws Exception
  {
    ShapefileReader rdr = new ShapefileReader();
    DriverProperties dp = new DriverProperties();
    dp.set("File", filename);
    return rdr.read(dp);
  }

  public ShapefileReaderTest() {
  }

  public static void main(String[] args) throws Exception
  {
    String filename = "X:\\jcs\\iciTest\\quesnel1.shp";
    try {
      FeatureCollection fc = loadShapefile(filename);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

  }
}