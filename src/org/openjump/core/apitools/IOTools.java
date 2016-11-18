/*
 * Project Name: OpenJUMP 
 * Original Organization Name: The JUMP Pilot Project
 * Original Programmer Name: Martin Davis
 * Current Maintainer Name: The JUMP Pilot Project
 * Current Maintainer Contact Information
 *    E-Mail Address: sunburned.surveyor@gmail.com
 * Copyright Holder: Martin Davis
 * Date Last Modified: Dec 12, 2007
 * IDE Name: Eclipse
 * IDE Version: Europa
 * Type: Java Class
 */

package org.openjump.core.apitools;

import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.*;
import com.vividsolutions.jump.io.datasource.DataSource;

public class IOTools
{

 private static String getExtension(String filename)
 {
   int len = filename.length();
   return filename.substring(len - 3, len);
 }

 public static FeatureCollection load(String filename)
     throws Exception
 {
   String extension = getExtension(filename);
   if (extension.equalsIgnoreCase("SHP"))
     return loadShapefile(filename);
   if (extension.equalsIgnoreCase("JML"))
     return loadJMLFile(filename);
   if (extension.equalsIgnoreCase("WKT"))
     return loadWKT(filename);
   throw new Exception("Unknown file type: " + extension);
 }

 public static FeatureCollection load(String filename, String zipFileName)
     throws Exception
 {
   String extension = getExtension(filename);
   if (extension.equalsIgnoreCase("SHP"))
     return loadShapefile(filename, zipFileName);
   throw new Exception("Unknown file type: " + extension);
 }

 public static FeatureCollection loadJMLFile(String filename)
     throws Exception
 {
   JMLReader rdr = new JMLReader();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   return rdr.read(dp);
 }

 public static FeatureCollection loadShapefile(String filename)
     throws Exception
 {
   ShapefileReader rdr = new ShapefileReader();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   return rdr.read(dp);
 }

 public static FeatureCollection loadShapefile(String filename, String zipFileName)
     throws Exception
 {
   ShapefileReader rdr = new ShapefileReader();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   if (zipFileName != null)
     dp.set(DataSource.COMPRESSED_KEY, zipFileName);
   return rdr.read(dp);
 }

 public static FeatureCollection loadFMEGML(String filename)
     throws Exception
 {
   FMEGMLReader rdr = new FMEGMLReader();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   return rdr.read(dp);
 }

 public static FeatureCollection loadWKT(String filename)
     throws Exception
 {
   WKTReader rdr = new WKTReader();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   FeatureCollection fc = rdr.read(dp);
   return fc;
 }

 public static void save(FeatureCollection fc, String filename)
     throws Exception
 {
   String extension = getExtension(filename);
   if (extension.equalsIgnoreCase("SHP")) {
     saveShapefile(fc, filename);
     return;
   }
   else if (extension.equalsIgnoreCase("JML")) {
     saveJMLFile(fc, filename);
     return;
   }
   throw new Exception("Unknown file type: " + extension);
 }

 public static void saveShapefile(FeatureCollection fc, String filename)
     throws Exception
 {
   ShapefileWriter writer = new ShapefileWriter();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   writer.write(fc, dp);
 }
 public static void saveJMLFile(FeatureCollection fc, String filename)
     throws Exception
 {
   JMLWriter writer = new JMLWriter();
   DriverProperties dp = new DriverProperties();
   dp.set(DataSource.FILE_KEY, filename);
   writer.write(fc, dp);
 }
 public static void print(FeatureCollection fc)
 {
   List featList = fc.getFeatures();
   for (Iterator i = featList.iterator(); i.hasNext(); ) {
     Feature f = (Feature) i.next();
     System.out.println(f.getGeometry());
   }
 }

}
