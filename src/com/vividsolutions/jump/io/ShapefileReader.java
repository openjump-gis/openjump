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

import com.vividsolutions.jts.geom.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;

import org.geotools.dbffile.DbfFile;

import org.geotools.shapefile.Shapefile;

import java.io.*;

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
 *      <td>File name (.zip NOT a .gz) with a .shp and .dbf file inside</td>
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
public class ShapefileReader implements JUMPReader {
    private File delete_this_tmp_dbf = null;

	public static final String FILE_PROPERTY_KEY = "File";
	public static final String DEFAULT_VALUE_PROPERTY_KEY = "DefaultValue";
	public static final String COMPRESSED_FILE_PROPERTY_KEY = "CompressedFile";

    /** Creates new ShapeReader */
    public ShapefileReader() {
    }

    /**
     * Main method to read a shapefile.  Most of the work is done in the org.geotools.* package.
     *
     *@param dp 'InputFile' or 'DefaultValue' to specify output .shp file.
     *
     */
    public FeatureCollection read(DriverProperties dp)
        throws IllegalParametersException, Exception {

        String shpfileName = dp.getProperty(FILE_PROPERTY_KEY);

        if (shpfileName == null) {
            shpfileName = dp.getProperty(DEFAULT_VALUE_PROPERTY_KEY);
        }

        if (shpfileName == null) {
            throw new IllegalParametersException(I18N.get("io.ShapefileReader.no-file-property-specified"));
        }

        int loc = shpfileName.lastIndexOf(File.separatorChar);
        String path = shpfileName.substring(0, loc + 1); // ie. "/data1/hills.shp" -> "/data1/"
        String fname = shpfileName.substring(loc + 1); // ie. "/data1/hills.shp" -> "hills.shp"

        loc = fname.lastIndexOf(".");

        if (loc == -1) {
            throw new IllegalParametersException(I18N.get("io.ShapefileReader.filename-must-end-in-shp"));
        }

        String fnameWithoutExtention = fname.substring(0, loc); // ie. "hills.shp" -> "hills"
        String dbfFileName = path + fnameWithoutExtention + ".dbf";

        //okay, have .shp and .dbf file paths, lets start
        // install Shapefile and DbfFile
        Shapefile myshape = getShapefile(shpfileName, dp.getProperty(COMPRESSED_FILE_PROPERTY_KEY));
        DbfFile mydbf = getDbfFile(dbfFileName, dp.getProperty(COMPRESSED_FILE_PROPERTY_KEY));
        GeometryFactory factory = new GeometryFactory();
        GeometryCollection collection = null;
        try {
        	collection = myshape.read(factory);
        } finally {
        	myshape.close(); //ensure we can delete input shape files before task is closed
        }
        FeatureSchema fs = new FeatureSchema();

        // fill in schema
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        FeatureCollection featureCollection = null;

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
            // There is a DBF file so we have to associate the attributes in
            // the DBF file with the features.
            int numfields = mydbf.getNumFields();

            for (int j = 0; j < numfields; j++) {
                AttributeType type = AttributeType.toAttributeType(mydbf.getFieldType(j));
                fs.addAttribute( mydbf.getFieldName(j), type );
            }

            featureCollection = new FeatureDataset(fs);

            for (int x = 0; x < mydbf.getLastRec(); x++) {
                Feature feature = new BasicFeature(fs);
                Geometry geo = collection.getGeometryN(x);
                StringBuffer s = mydbf.GetDbfRec(x);

                for (int y = 0; y < numfields; y++) {
                    feature.setAttribute(y + 1, mydbf.ParseRecordColumn(s, y));
                }

                feature.setGeometry(geo);
                featureCollection.add(feature);
            }

            mydbf.close();
            deleteTmpDbf(); // delete dbf file if it was decompressed
        }
        //System.gc();

        return featureCollection;
    }


    protected Shapefile getShapefile(String shpfileName, String compressedFname)
        throws Exception {
        InputStream in = CompressedFile.openFile(shpfileName,compressedFname);
        Shapefile myshape = new Shapefile(in);
        return myshape;
    }


    protected DbfFile getDbfFile(String dbfFileName, String compressedFname)
        throws Exception {

        DbfFile mydbf = null;

        if ((compressedFname != null) && (compressedFname.length() > 0)) {
            byte[] b = new byte[16000];
            int len;
            boolean keepGoing = true;

            // copy the file then use that copy
            File file = File.createTempFile("dbf", ".dbf");
            FileOutputStream out = new FileOutputStream(file);

            InputStream in = CompressedFile.openFile(dbfFileName,compressedFname);

            while (keepGoing) {
                len = in.read(b);

                if (len > 0) {
                    out.write(b, 0, len);
                }

                keepGoing = (len != -1);
            }

            in.close();
            out.close();

            mydbf = new DbfFile(file.toString());
            delete_this_tmp_dbf = file; // to be deleted later on
        } else {
            File dbfFile = new File( dbfFileName );

            if ( dbfFile.exists() ) {
                mydbf = new DbfFile(dbfFileName);
            }
        }

        return mydbf;
    }


    private void deleteTmpDbf() {
        if (delete_this_tmp_dbf != null) {
            delete_this_tmp_dbf.delete();
            delete_this_tmp_dbf = null;
        }
    }
}
