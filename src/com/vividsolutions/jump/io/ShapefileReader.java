/*
 * ShapeReader.java
 *
 * Created on June 27, 2002, 2:49 PM
 */
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vividsolutions.jump.io.datasource.DataSource;
import org.geotools.dbffile.DbfFile;
import org.geotools.shapefile.Shapefile;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.Logger;

/**
 * ShapefileReader is a {@link JUMPReader} specialized to read Shapefiles.
 *
 * <p>
 *   DataProperties for the JUMPReader load(DataProperties) interface:<br><br>
 * </p>
 *
 * <p>
 *  <table border='1' cellspacing='0' cellpadding='4'>
 *    <tr>
 *      <th>Parameter</th><th>Meaning</th>
 *    </tr>
 *    <tr>
 *      <td>InputFile or DefaultValue</td>
 *      <td>File name for the input .shp file</td>
 *    </tr>
 *    <tr>
 *      <td colspan='2'>
 *         NOTE: The input .dbf is assumed to be 'beside' (in the same
 *         directory) as the .shp file.
 *      </td>
 *    </tr>

 *    <tr>
 *      <td>CompressedFile</td>
 *      <td>File name (.zip or .tgz NOT a .gz) with .shp and .dbf file inside</td>
 *    </tr>
 *
 *    <tr>
 *      <td colspan='2'>
 *         Uses a modified version of geotools to do the .dbf and .shp
 *         file reading.  If you are reading from a .zip file, the dbf
 *         file will be copied to your temp directory and deleted
 *         after being read.
 *      </td>
 *    </tr>
 *  </table>

 */
public class ShapefileReader extends AbstractJUMPReader {

    private File delete_this_tmp_dbf = null;
    private File delete_this_tmp_shx = null;
    private File delete_this_tmp_cpg = null;

    /** Creates new ShapeReader */
    public ShapefileReader() {
    }

    /**
     * Main method to read a shapefile.
     * Most of the work is done in the org.geotools.* package.
     *
     * @param dp 'InputFile' or 'DefaultValue' to specify output .shp file.
     * @return a FeatureCollection created from .shp and .dbf (dbf is optional)
     */
    public FeatureCollection read(DriverProperties dp) throws Exception {

        getExceptions().clear();

        // ATTENTION: this can contain a zip file path as well
        // shpFileName contains the .shp extension
        String shpFileName = dp.getProperty(DataSource.FILE_KEY);

        if (shpFileName == null) {
            shpFileName = dp.getProperty(DriverProperties.DEFAULT_VALUE_KEY);
        }

        if (shpFileName == null) {
            throw new IllegalParametersException(I18N.get("io.ShapefileReader.no-file-property-specified"));
        }

        //okay, we have .shp and .dbf file paths, lets create Shapefile and DbfFile
        Shapefile myshape = getShapefile(shpFileName, dp.getProperty(DataSource.COMPRESSED_KEY));

        // charset used to read dbf (one charset defined by cpg file,
        // charset defined in dp or default platform charset)
        String charsetName = getCharset(shpFileName, dp);

        DbfFile mydbf = getDbfFile(shpFileName, dp.getProperty(DataSource.COMPRESSED_KEY),
                Charset.forName(charsetName));

        try(InputStream shx = getShx(shpFileName, dp.getProperty(DataSource.COMPRESSED_KEY))) {

            GeometryFactory factory = new GeometryFactory();
            GeometryCollection collection;
            // Read the shapefile either from shx (if provided) or directly from shp
        	collection = shx == null ? myshape.read(factory) : myshape.readFromIndex(factory, shx);

            // Minimal schema for FeatureCollection (if no dbf is provided)
            FeatureSchema fs = new FeatureSchema();
            fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

            FeatureCollection featureCollection;

            if ( mydbf == null ) {
                // handle shapefiles without dbf files.
                featureCollection = new FeatureDataset(fs);

                int numGeometries = collection.getNumGeometries();

                for (int x = 0; x < numGeometries; x++) {
                    Feature feature = new BasicFeature(fs);
                    Geometry geo = collection.getGeometryN(x);

                    feature.setGeometry(geo);
                    featureCollection.add(feature);
                }
            } else {
                // There is a DBF file so we have to set the Charset to use and
                // to associate the attributes in the DBF file with the features.

                int numfields = mydbf.getNumFields();

                for (int j = 0; j < numfields; j++) {
                    AttributeType type = AttributeType.toAttributeType(mydbf.getFieldType(j));
                    fs.addAttribute( mydbf.getFieldName(j), type );
                }

                featureCollection = new FeatureDataset(fs);

                for (int x = 0; x < Math.min(mydbf.getLastRec(), collection.getNumGeometries()); x++) {

                    // [sstein 9.Sept.08] Get bytes rather than String to be able to read multibytes strings
                    byte[] s = mydbf.GetDbfRec(x);
                    // [mmichaud 2017-06-10] skip deleted records
                    if (s[0] == (byte)0x2A && System.getProperty("dbf.deleted.on")==null) {
                        continue;
                    }
                    Feature feature = new BasicFeature(fs);
                    Geometry geo = collection.getGeometryN(x);
                    for (int y = 0; y < numfields; y++) {
                        feature.setAttribute(y + 1, mydbf.ParseRecordColumn(s, y));
                    }

                    feature.setGeometry(geo);
                    featureCollection.add(feature);
                }

                // [mmichaud 2013-10-07] if the number of shapes is greater than the number of records
                // it is better to go on and create features with a geometry and null attributes
                if (collection.getNumGeometries() > mydbf.getLastRec()) {
                    String message = I18N.getMessage("com.vividsolutions.jump.io.ShapefileReader.shp-gt-dbf",
                            shpFileName, collection.getNumGeometries(), mydbf.getLastRec());
                    Logger.error(message);
                    getExceptions().add(new Exception(message));
                    for (int x = mydbf.getLastRec() ; x < collection.getNumGeometries() ; x++) {
                        Feature feature = new BasicFeature(fs);
                        Geometry geo = collection.getGeometryN(x);
                        feature.setGeometry(geo);
                        featureCollection.add(feature);
                    }
                }
                if (collection.getNumGeometries() < mydbf.getLastRec()) {
                    String message = I18N.getMessage("com.vividsolutions.jump.io.ShapefileReader.shp-lt-dbf",
                            shpFileName, collection.getNumGeometries(), mydbf.getLastRec());
                    Logger.error(message);
                    getExceptions().add(new Exception(message));
                    List emptyList = new ArrayList();
                    for (int x = collection.getNumGeometries() ; x < mydbf.getLastRec() ; x++) {
                        Feature feature = new BasicFeature(fs);
                        Geometry geo = factory.buildGeometry(emptyList);
                        byte[] s = mydbf.GetDbfRec(x); //[sstein 9.Sept.08]
                        // [mmichaud 2017-06-10] skip deleted records
                        if (s[0] == (byte)0x2A && System.getProperty("dbf.deleted.on")==null) {
                            continue;
                        }
                        for (int y = 0; y < numfields; y++) {
                            feature.setAttribute(y + 1, mydbf.ParseRecordColumn(s, y));
                        }
                        feature.setGeometry(geo);
                        featureCollection.add(feature);
                    }
                }
            }
            return featureCollection;
        } finally {
            deleteTmpDbf(); // delete dbf file if it was decompressed
            deleteTmpShx(); // delete shx file if it was decompressed
            deleteTmpCpg(); // delete cpg file if it was decompressed
            myshape.close(); //ensure we can delete input shape files before task is closed
            if (mydbf != null) mydbf.close();
        }
    }


    protected Shapefile getShapefile(String shpfileName, String compressedFname)
                throws Exception {
        InputStream in = CompressedFile.openFile(shpfileName,compressedFname);
        return new Shapefile(in);
    }

    protected InputStream getShx(String srcFileName, String compressedFname) throws Exception {
        FileInputStream shxInputStream;

        // default is a *.shx src file
        if (srcFileName.matches("(?i).*\\.shp$")) {
            // replace file name extension of compressedFname (probably .shp) with .shx
            srcFileName = srcFileName.replaceAll("\\.[^.]*$", ".shx");
            File shxFile = new File( srcFileName );
            if ( shxFile.exists() )
                return new FileInputStream(srcFileName);
        }
        // if we are in an archive that can hold multiple files compressedFname is defined and a String
        else if (compressedFname != null) {
            byte[] b = new byte[4096];
            int len;
            boolean keepGoing = true;

            // copy the file then use that copy
            File file = File.createTempFile("shx", ".shx");
            FileOutputStream out = new FileOutputStream(file);

            // replace file name extension of compressedFname (probably .shp) with .dbf
            compressedFname = compressedFname.replaceAll("\\.[^.]*$", ".shx");

            try {
                InputStream in = CompressedFile.openFile(srcFileName,compressedFname);

                while (keepGoing) {
                    len = in.read(b);

                    if (len > 0) {
                        out.write(b, 0, len);
                    }

                    keepGoing = (len != -1);
                }

                in.close();
                out.close();

                shxInputStream = new FileInputStream(file.toString());
                delete_this_tmp_shx = file; // to be deleted later on
                return shxInputStream;
            } catch (Exception e) {
              Logger.error(e);
            }
        }

        return null;
    }

    protected String getCharset(String shpFileName, DriverProperties dp) throws Exception {

        // default charset used to read dbf is platform default charset
        String charsetName = Charset.defaultCharset().name();

        // if a cpg file is found, charset used is the one defined in the cpg file
        //BufferedReader cpgCharsetReader = null;
        try (InputStream cpgCharsetInputStream =
                     getCpgInputStream(shpFileName, dp.getProperty(DataSource.COMPRESSED_KEY))) {
            if (cpgCharsetInputStream != null) {
                try (BufferedReader cpgCharsetReader =
                             new BufferedReader(new InputStreamReader(cpgCharsetInputStream))) {
                    String cpgCharset = cpgCharsetReader.readLine();
                    cpgCharset = esri_cp_2_java(cpgCharset);
                    try {
                        if (Charset.isSupported(cpgCharset)) {
                            charsetName = cpgCharset;
                        }
                    } catch (IllegalCharsetNameException ice) {
                        Logger.info("Could not interpret charset name " + cpgCharset + " : revert to default " + charsetName);
                    }
                }
            }
        }
        // if dp.getProperty("charset") contains a charset different from platform default,
        // this charset is preferred to the one defined by cpg file
        if (dp.getProperty(DataSource.CHARSET_KEY) != null && Charset.isSupported(dp.getProperty(DataSource.CHARSET_KEY)) &&
                !Charset.defaultCharset().name().equals(Charset.forName(dp.getProperty(DataSource.CHARSET_KEY)).name())) {
            charsetName = dp.getProperty(DataSource.CHARSET_KEY);
        }
        return charsetName;
    }

    protected InputStream getCpgInputStream(String srcFileName, String compressedFname) throws Exception {
        FileInputStream cpgInputStream;

        // default is a *.cpg src file
        if (srcFileName.matches("(?i).*\\.shp$")) {
            // replace file name extension of compressedFname (probably .shp) with .cpg
            srcFileName = srcFileName.replaceAll("\\.[^.]*$", ".cpg");
            File cpgFile = new File(srcFileName);
            if (cpgFile.exists()) {
                return new FileInputStream(srcFileName);
            }
        }
        // if we are in an archive that can hold multiple files compressedFname is defined and a String
        else if (compressedFname != null) {
            byte[] b = new byte[4096];
            int len;
            boolean keepGoing = true;

            // copy the file then use that copy
            File file = File.createTempFile("cpg", ".cpg");
            FileOutputStream out = new FileOutputStream(file);

            // replace file name extension of compressedFname (probably .shp) with .dbf
            compressedFname = compressedFname.replaceAll("\\.[^.]*$", ".cpg");

            try {
                InputStream in = CompressedFile.openFile(srcFileName,compressedFname);

                while (keepGoing) {
                    len = in.read(b);

                    if (len > 0) {
                        out.write(b, 0, len);
                    }

                    keepGoing = (len != -1);
                }

                in.close();
                out.close();

                cpgInputStream = new FileInputStream(file.toString());
                delete_this_tmp_cpg = file; // to be deleted later on
                return cpgInputStream;
            } catch (Exception e) {
                Logger.warn(e.getMessage());
            }
        }

        return null;
    }

	/**
	 * Get's a DbfFile.
	 * Kept, for compatibilty. Use the method with charset parameter.
	 *
	 * @param srcFileName either a pass to a dbf or an archive file (*.zip etc.) accompanied by the compressedFname it contains
	 * @param compressedFname the name of the compressed entry in the compressed file or null
	 * @return a DbfFile object for the dbf file named FileName
	 * @throws Exception
	 */
    protected DbfFile getDbfFile(String srcFileName, String compressedFname) throws Exception {
		return getDbfFile(srcFileName, compressedFname, Charset.defaultCharset());
	}

    protected DbfFile getDbfFile(String srcFileName, String compressedFname, Charset charset)
        throws Exception {

        DbfFile mydbf;

        // default is a *.shp src file
        if (srcFileName.matches("(?i).*\\.shp$")) {
          // replace file name extension of compressedFname (probably .shp) with .dbf
          srcFileName = srcFileName.replaceAll("\\.[^.]*$", ".dbf");
          File dbfFile = new File( srcFileName );
          if ( dbfFile.exists() )
              return new DbfFile(srcFileName, charset);
        }
        // if we are in an archive that can hold multiple files compressedFname is defined and a String
        else if (compressedFname != null) {
            byte[] b = new byte[4096];
            int len;
            boolean keepGoing = true;

            // copy the file then use that copy
            File file = File.createTempFile("dbf", ".dbf");
            FileOutputStream out = new FileOutputStream(file);
            
            // replace file name extension of compressedFname (probably .shp) with .dbf
            compressedFname = compressedFname.replaceAll("\\.[^.]*$", ".dbf");
            
            try {
                InputStream in = CompressedFile.openFile(srcFileName,compressedFname);

                while (keepGoing) {
                    len = in.read(b);
                
                    if (len > 0) {
                        out.write(b, 0, len);
                    }
                
                    keepGoing = (len != -1);
                }
                
                in.close();
                out.close();
                
                mydbf = new DbfFile(file.toString(), charset);
                delete_this_tmp_dbf = file; // to be deleted later on
                return mydbf;
            } catch (Exception e) {
                Logger.error(e);
            }
        } 

        return null;
    }


    private void deleteTmpDbf() {
        if (delete_this_tmp_dbf != null) {
            delete_this_tmp_dbf.delete();
            delete_this_tmp_dbf = null;
        }
    }

    private void deleteTmpShx() {
        if (delete_this_tmp_shx != null) {
            delete_this_tmp_shx.delete();
            delete_this_tmp_shx = null;
        }
    }

    private void deleteTmpCpg() {
        if (delete_this_tmp_cpg != null) {
            delete_this_tmp_cpg.delete();
            delete_this_tmp_cpg = null;
        }
    }

    private static final Pattern CODE_PAGE = Pattern.compile(".*?(\\d\\d\\d++)");

    private String esri_cp_2_java(String esri_cp) {
        Matcher matcher = CODE_PAGE.matcher(esri_cp);
        if (matcher.matches() && matcher.groupCount() == 1) {
            String code = matcher.group(1);
            if (code.length() == 3) {
                if (code.equals("708")) return "ISO-8859-6";
                else if (code.equals("932")) return "Shift_JIS";
                else if (code.equals("936")) return "GBK";
                else return "IBM"+code;
            } else if (code.length() == 4) {
                return "windows-"+code;
            } else if (code.startsWith("8859")) {
                return "ISO-8859-"+code.substring(4);
            } else return esri_cp.replaceAll(" ","-");
        } else return esri_cp.replaceAll(" ","-");
    }

}
