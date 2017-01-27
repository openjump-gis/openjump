package jumptest.io;

import java.io.*;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class GMLReaderTester {

  public GMLReaderTester() {
  }

  public static void main(String[] args) throws Exception
  {

    FeatureCollection fc;
    GMLInputTemplate gml_IT;
    GMLReader gmlReader;
    FileReader  r;
    GMLWriter gmlWriter;
    String  temp;
    DriverProperties dp;
    FMEGMLWriter fmegmlwriter;
    FMEGMLReader fmegmlReader;
    ShapefileReader  shpReader;



    try
    {
      //  LEDataInputStream leidstream;
      //  LEDataOutputStream leodstream;
        EndianDataInputStream eistream;
         EndianDataOutputStream eostream;
        java.io.FileInputStream  fis;
        java.io.FileOutputStream  fos;

        fos = new FileOutputStream ("l:\\test.endian");

        eostream = new EndianDataOutputStream(fos);


        // leodstream.writeShort(-555);
        eostream.writeDoubleLE(Double.POSITIVE_INFINITY);
        eostream.writeDoubleLE(Double.NEGATIVE_INFINITY);
        eostream.writeDoubleLE(Double.NaN);
        eostream.writeDoubleLE(Double.MIN_VALUE );
        eostream.writeDoubleLE(Double.MAX_VALUE );
        eostream.close();

               //   fis = new FileInputStream ("l:\\test.endian");
               //   leidstream = new LEDataInputStream(fis);
               //    leidstream.setLittleEndianMode(true);
               //   double ss= leidstream.readDouble();



          fis = new FileInputStream ("l:\\test.endian");
          eistream = new EndianDataInputStream(fis);

          double s= eistream.readDoubleLE();
           s= eistream.readDoubleLE();
           s= eistream.readDoubleLE();
           s= eistream.readDoubleLE();
           s= eistream.readDoubleLE();

         return;
    }
    catch (Exception e)
    {
        e.printStackTrace();
    }

    try{
        /*
            FMEGMLReader fmeReader = new FMEGMLReader();
                dp = new DataProperties();
                dp.set("InputFMEGMLFile","l:/Refractions/JCS/data3/3points.xml");

             fc = fmeReader.read(dp);

             fmegmlwriter = new FMEGMLWriter();
             fmegmlwriter.createOutputTemplate(fc.getMetaData(),fc);

         */

        dp = new DriverProperties();
        dp.set("File","l:\\Refractions\\JCS\\data4\\3points2.jml");
        //dp.set("TemplateFile","l:/Refractions/JCS/data2/99dra01.jml");
        //dp.set("File","l:/test_fme.xml");
        gmlReader = new GMLReader();
        fc = gmlReader.read(dp);

        gmlWriter = new GMLWriter();
        dp = new DriverProperties();
        dp.set("OutputXMLFile","c:\\out.jml");
        gmlWriter.write(fc,dp);


        //dp = new DataProperties();
        //dp.set("OutputXMLFile","l:/Refractions/JCS/out.jml");
        //gmlWriter = new GMLWriter();
        //gmlWriter.write(fc,dp);
        int i;
        i=1;
        if (i==1)
            return;

      }
      catch (Exception e)
      {
            e.printStackTrace() ;
            return;
      }


    try {
        dp = new DriverProperties();
        dp.set("File","l:/Refractions/JCS/data/victoria_ici.xml");
        dp.set("TemplateFile","l:/Refractions/JCS/data/victoria_ici_input.xml");
        gmlReader = new GMLReader();
        fc = gmlReader.read(dp);
        //gml_IT = new GMLInputTemplate();
        //r= new  FileReader("c:/tmp.xml");
        //gml_IT.load(r);
        //r.close();
        //gmlReader = new GMLReader();
        //gmlReader.setInputTemplate(gml_IT);
      }
      catch (Exception e)
      {
            e.printStackTrace() ;
            return;
      }

      try {
        //r=new  FileReader("c:/tmp.xml");
        //fc = gmlReader.read(r);
       // r.close();
       // gmlWriter = new GMLWriter();
        //gmlWriter.setOutputTemplate(gmlWriter.makeOutputTemplate(fc.getMetaData()));
        //gmlWriter.write(fc, new java.io.FileWriter("C:/tmp2.xml") );

        dp = new DriverProperties();
        dp.set("OutputXMLFile","l:/Refractions/JCS/data2/victoria_ici.jml");
        gmlWriter = new GMLWriter();
        gmlWriter.write(fc,dp);
      }
      catch (Exception e)
      {
            e.printStackTrace() ;
      }

    //TEST TEST1 = new TEST();




  }
}
