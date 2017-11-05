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

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import org.geotools.dbffile.DbfFieldDef;
import org.geotools.dbffile.DbfFile;
import org.geotools.dbffile.DbfFileWriter;
import org.geotools.shapefile.Shapefile;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;


/**
 *
 * ShapefileWriter is a {@link JUMPWriter} specialized to write Shapefiles.
 *
 * <p>
 * DataProperties for the ShapefileWriter write(DataProperties) interface:<br><br>
 * </p>
 *
 * <p>
 * <table border='1' cellspacing='0' cellpadding='4'>
 *   <tr>
 *       <th>Parameter</th>
 *       <th>Meaning</th>
 *   </tr>
 *   <tr>
 *       <td>OutputFile or DefaultValue</td>
 *       <td>File name for the output .shp file</td>
 *   </tr>
 *   <tr>
 *       <td>ShapeType</td>
 *       <td>
 *          Dimensionality of the Shapefile - 'xy', 'xym' or 'xyz'.  'xymz' and
 *          'xyzm' are the same as 'xyz' 
 *       </td>
 *   </tr>
 * </table><br>
 *
 * <p>
 * NOTE: The input .dbf and .shx is assumed to be 'beside' (in the
 * same directory) as the .shp file.
 * </p>
 *
 *   The shapefile writer consists of two parts: writing attributes
 *   (.dbf) and writing geometries (.shp).
 *
 * <p>
 * JUMP columns are converted to DBF columns by:
 * </p>
 *
 * <table border='1' cellspacing='0' cellpadding='4'>
 *   <tr>
 *     <th>JUMP Column</th>
 *     <th>DBF column</th>
 *   </tr>
 *   <tr>
 *     <td>STRING</td>
 *     <td>Type 'C' length is size of longest string in the FeatureCollection </td>
 *   </tr>
 *   <tr>
 *     <td>DOUBLE</td>
 *     <td>Type 'N' length is 33, with 16 digits right of the decimal</td>
 *   </tr>
 *   <tr>
 *     <td>INTEGER</td>
 *     <td>Type 'N' length is 16, with 0 digits right of the decimal</td>
 *   </tr>
 * </table>
 *
 * 
 * <p>
 *   For more information on the DBF file format, see the
 *   <a
 *     target='_new'
 *     href='http://www.apptools.com/dbase/faq/qformt.htm'>DBF Specification FAQ</a>
 * </p>
 *
 * <p>
 *   Since shape files may contain only one type of geometry (POINT,
 *   MULTPOINT, POLYLINE, POLYGON, POINTM, MULTPOINTM, POLYLINEM,
 *   POLYGONM, POINTZ, MULTPOINTZ, POLYLINEZ, or POLYGONZ), the
 *   FeatureCollection must be first be normalized to one type:
 * </p>
 *
 * <table border='1' cellspacing='0' cellpadding='4'>
 *   <tr>
 *     <th>First non-NULL non-Point geometry in FeatureCollection</th>
 *      <th>Coordinate Dimensionality</th>
 *      <th>Shape Type</th>
 *   </tr>
 *   <tr>
 *     <td>
 *        MULTIPOINT
 *     </td>
 *     <td>
 *        xy xym xyzm     
 *     </td>
 *     <td>
 *	  MULTIPOINT MULTIPOINTM MULTIPOINTZ
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       LINESTRING/MULTILINESTRING    
 *     </td>
 *     <td>
 *       xy xym xyzm    
 *     </td>
 *     <td>
 *	 POLYLINE POLYLINEM POLYLINEZ
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       POLYGON/MULTIPOLYGON     
 *     </td>
 *     <td>
 *        xy xym xyzm    
 *     </td>
 *     <td>
 *	   POLYGON POLYGONM POLYGONZ
 *     </td>
 *   </tr>
 *   <tr>
 *     <th>All geometries in FeatureCollection are</th>
 *      <th>Coordinate Dimensionality</th>
 *      <th>Shape Type</th>
 *   </tr>
 *   <tr>
 *     <td>
 *        POINT
 *     </td>
 *     <td>
 *        xy xym xyzm     
 *     </td>
 *     <td>
 *	     POINT POINTM POINTZ
 *     </td>
 *   </tr>
 * </table>
 * 
 * <p>
 * During this normalization process any non-consistent geometry will
 * be replaced by a NULL geometry.
 * </p>
 * 
 * <p>
 * For example, if the shapetype is determined to be 'POLYLINE' any
 * POINT, MULTIPOINT, or POLYGON geometries in the FeatureCollection
 * will be replaced with a NULL geometry.
 * </p>
 *
 * <p>
 *  The coordinate dimensionality can be explicitly set with a
 *  DataProperties tag of 'ShapeType': 'xy', 'xym', or 'xyz' ('xymz'
 *  and 'xyzm' are pseudonyms for 'xyz').  If this DataProperties is
 *  unspecified, it will be auto set to 'xy' or 'xyz' based on the
 *  first non-NULL geometry having a Z coordinate.
 * </p>
 *
 * <p>
 *  Since JUMP and JTS do not currently support a M (measure)
 *  coordinate, it will always be set to -10E40 in the shape file
 *  (type 'xym' or 'xyzm').  This value represents the Measure "no
 *  data" value (page 2, ESRI Shapefile Technical Description).  Since
 *  the 'NaN' DOUBLE values for Z coordinates is invalid in a
 *  shapefile, it is converted to '0.0'.
 * </p>
 *
 * <p>
 *  For more information on the shapefile format, see the
 *   <a href='http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf'>ESRI 
 *    Shapefile Spec</a>
 * </p>
 *
 *  For more information on the DBF, see
 *   <a href='http://www.digitalpreservation.gov/formats/fdd/fdd000325.shtml'></a> and
 *   <a href='http://www.dbase.com/KnowledgeBase/int/db7_file_fmt.htm'></a> 
 *
 **/
public class ShapefileWriter implements JUMPWriter {

	private static final String SHAPE_TYPE_PROPERTY_KEY = "ShapeType";
	private static boolean truncate = false;
	private static long lastTimeTruncate = new Date(0).getTime();

    /** Creates new ShapefileWriter */
    public ShapefileWriter() {
    }

    /**
     * Main method - write the featurecollection to a shapefile (2d, 3d or 4d).
     *
     * @param featureCollection collection to write
     * @param dp 'OutputFile' or 'DefaultValue' to specify where to write, and 'ShapeType' to specify dimensionality.
     */
    public void write(FeatureCollection featureCollection, DriverProperties dp) throws Exception {

        String shpfileName;
        String dbffname;
        String shxfname;
        String cpgfname;
        String prjfname;

        String path;
        String fname;
        String fname_withoutextention;
        int shapeType;
        int loc;

        GeometryCollection gc;
        
        //sstein: check for mixed geometry types in the FC
        this.checkIfGeomsAreMixed(featureCollection);
        
        shpfileName = dp.getProperty(DataSource.FILE_KEY);

        if (shpfileName == null) {
            shpfileName = dp.getProperty(DriverProperties.DEFAULT_VALUE_KEY);
        }

        if (shpfileName == null) {
            throw new IllegalParametersException(I18N.get("io.ShapefileWriter.no-output-filename-specified"));
        }

        shpfileName = new File(shpfileName).getPath(); // write the path with the native file separator
        loc = shpfileName.lastIndexOf(File.separatorChar);

        if (loc == -1) {
            // probably using the wrong path separator character.
            throw new Exception(
                I18N.getMessage("io.ShapefileWriter.path-separator-not-found", 
                                File.separatorChar));
        } else {
            path = shpfileName.substring(0, loc + 1); // ie. "/data1/hills.shp" -> "/data1/"
            fname = shpfileName.substring(loc + 1); // ie. "/data1/hills.shp" -> "hills.shp"
        }

        loc = fname.lastIndexOf(".");

        if (loc == -1) {
            throw new IllegalParametersException(I18N.get("io.ShapefileWriter.filename-must-end-in-shp"));
        }

        fname_withoutextention = fname.substring(0, loc); // ie. "hills.shp" -> "hills."
        dbffname = path + fname_withoutextention + ".dbf";

		    String charsetName = dp.getProperty(DataSource.CHARSET_KEY);
		    if (charsetName == null) charsetName = Charset.defaultCharset().name();
        writeDbf(featureCollection, dbffname, Charset.forName(charsetName));

        PrintWriter pw = null;
        try {
            cpgfname = path + fname_withoutextention + ".cpg";
            pw = new PrintWriter(new FileOutputStream(cpgfname));
            pw.write(java_cp_2_esri(charsetName));
        } finally {
            if (pw != null) pw.close();
        }

        String registry = dp.getProperty(DataSource.COORDINATE_SYSTEM_REGISTRY, "EPSG");
        String code = dp.getProperty(DataSource.COORDINATE_SYSTEM_CODE, "0");
        String prjString = getPrjString(path + fname_withoutextention + ".prj", registry, code);
        if (prjString != null) {
            try {
                prjfname = path + fname_withoutextention + ".prj";
                pw = new PrintWriter(new FileOutputStream(prjfname));
                pw.write(prjString);
            } finally {
                if (pw != null) pw.close();
            }
        }

        // this gc will be a collection of either multi-points, multi-polygons, or multi-linestrings
        // polygons will have the rings in the correct order
        gc = makeSHAPEGeometryCollection(featureCollection);

        shapeType = 2; //x,y

        if (dp.getProperty(SHAPE_TYPE_PROPERTY_KEY) != null) {
            String st = dp.getProperty(SHAPE_TYPE_PROPERTY_KEY);

            if (st.equalsIgnoreCase("xy")) {
                shapeType = 2;
            } else if (st.equalsIgnoreCase("xym")) {
                shapeType = 3;
            } else if (st.equalsIgnoreCase("xymz")) {
                shapeType = 4;
            } else if (st.equalsIgnoreCase("xyzm")) {
                shapeType = 4;
            } else if (st.equalsIgnoreCase("xyz")) {
                shapeType = 4;
            } else {
                throw new IllegalParametersException(
                    I18N.get("io.ShapefileWriter.unknown-type"));
            }
        } else {
            if (gc.getNumGeometries() > 0) {
                shapeType = guessCoordinateDims(gc.getGeometryN(0));
            }
        }

        URL url = new URL("file", "localhost", shpfileName);
        // Write shp file
        Shapefile myshape = new Shapefile(url);
        myshape.write(gc, shapeType);

        // Write shx file
        shxfname = path + fname_withoutextention + ".shx";
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(shxfname));
                EndianDataOutputStream sfile = new EndianDataOutputStream(outputStream)) {
            myshape.writeIndex(gc, sfile, shapeType);
        }
        
        // Delete sbn, sbx and qix index files
        deleteIndex(path, fname_withoutextention, "sbn");
        deleteIndex(path, fname_withoutextention, "sbx");
        deleteIndex(path, fname_withoutextention, "qix");
        
        //If long fields have been truncated, remember the end process timestamp
        if (truncate) {
            lastTimeTruncate = new Date().getTime();
        }
    }

    /**
     * Returns: <br>
     * 2 for 2d (default) <br>
     * 4 for 3d  - one of the coordinates has a non-NaN z value <br>
     * (3 is for x,y,m but thats not supported yet) <br>
     * @param g geometry to test - looks at 1st coordinate
     */
    private int guessCoordinateDims(Geometry g) {
        Coordinate[] cs = g.getCoordinates();

        for (Coordinate coord : cs) {
            if (!(Double.isNaN(coord.z))) {
                return 4;
            }
        }

        return 2;
    }

	/**
	 * Write a dbf file with the information from the featureCollection.
	 * For compatibilty reasons, this method is
	 * is now a wrapper for the changed/new one with Charset functions.
	 *
	 * @see #writeDbf(com.vividsolutions.jump.feature.FeatureCollection, String, java.nio.charset.Charset)
	 *
	 * @param featureCollection featureCollection to write
	 * @param fname name of the shapefile to write to
	 */
	void writeDbf(FeatureCollection featureCollection, String fname) throws Exception {
		writeDbf(featureCollection, fname, Charset.defaultCharset());
	}

    /**
     * Write a dbf file with the information from the featureCollection.
     * @param featureCollection featureCollection to write
     * @param fname name of the dbf file to write to
     * July 2, 2010 - modified by beckerl to read existing dbf file header
     * and use the existing numeric field definitions.
     */
    private void writeDbf(FeatureCollection featureCollection, String fname, Charset charset) throws Exception {

        int t;
        int f;
        int u;
        int num;

        HashMap<String,DbfFieldDef> fieldMap = null;
        if (new File(fname).exists()){
            DbfFile dbfFile = null;
            try {
                dbfFile = new DbfFile(fname);
                int numFields = dbfFile.getNumFields();
                fieldMap = new HashMap<>(numFields);
                for (int i = 0; i < numFields; i++) {
                    String fieldName = dbfFile.getFieldName(i);
                    fieldMap.put(fieldName, dbfFile.fielddef[i]);
                }
            } finally {
                if (dbfFile != null) dbfFile.close();
            }
        }

        FeatureSchema fs = featureCollection.getFeatureSchema();

        // -1 because one of the columns is geometry
        DbfFieldDef[] fields = new DbfFieldDef[fs.getAttributeCount() - 1];

        // dbf column type and size
        f = 0;
        
        Set<String> uniqueNames = new HashSet<>();

        for (t = 0; t < fs.getAttributeCount(); t++) {
            AttributeType columnType = fs.getAttributeType(t);
            String columnName = fs.getAttributeName(t);
            
            //[mmichaud 2012-03-24] increment identical truncated field names
            //[mmichaud 2012-10-07] change from 11 to 10 char (to conform to dbf
            // specification)
            //[mmichaud 2015-06-17] fix bug described in
            // http://sourceforge.net/p/jump-pilot/mailman/message/34210973/
            columnName = uniqueName(uniqueNames, columnName, 10);

            if (columnType == AttributeType.INTEGER ||
                    columnType == AttributeType.SMALLINT ||
                    columnType == AttributeType.TINYINT) {
                int maxlength = findMaxStringLength(featureCollection, t, charset);
                if (maxlength <= 3) fields[f] = new DbfFieldDef(columnName, 'N', 3, 0);
                else if (maxlength <= 6) fields[f] = new DbfFieldDef(columnName, 'N', 6, 0);
                else if (maxlength <= 9) fields[f] = new DbfFieldDef(columnName, 'N', 9, 0);
                else fields[f] = new DbfFieldDef(columnName, 'N', maxlength, 0);
                DbfFieldDef fromFile = overrideWithExistingCompatibleDbfFieldDef(fields[f], fieldMap);
                if (fromFile.fieldnumdec == 0)
                    fields[f] = fromFile;
                f++;
            }

            else if (columnType == AttributeType.LONG ||
                    columnType == AttributeType.BIGINT) {
                int maxlength = findMaxStringLength(featureCollection, t, charset);
                if (maxlength <= 12) fields[f] = new DbfFieldDef(columnName, 'N', 12, 0);
                else if (maxlength <= 15) fields[f] = new DbfFieldDef(columnName, 'N', 15, 0);
                else if (maxlength <= 18) fields[f] = new DbfFieldDef(columnName, 'N', 18, 0);
                else fields[f] = new DbfFieldDef(columnName, 'N', maxlength, 0);
                DbfFieldDef fromFile = overrideWithExistingCompatibleDbfFieldDef(fields[f], fieldMap);
                if (fromFile.fieldnumdec == 0)
                    fields[f] = fromFile;
                f++;
            }

            else if (columnType == AttributeType.DOUBLE ||
                        columnType == AttributeType.REAL ||
                        columnType == AttributeType.FLOAT ||
                        columnType == AttributeType.NUMERIC ||
                        columnType == AttributeType.DECIMAL ||
                        columnType == AttributeType.BIGDECIMAL) {
                fields[f] = new DbfFieldDef(columnName, 'N', 33, 16);
                DbfFieldDef fromFile = overrideWithExistingCompatibleDbfFieldDef(fields[f], fieldMap);
                if (fromFile.fieldnumdec > 0)
                    fields[f] = fromFile;
                f++;
            }

            else if (columnType == AttributeType.STRING ||
                        columnType == AttributeType.OBJECT ||
                        columnType == AttributeType.VARCHAR ||
                        columnType == AttributeType.LONGVARCHAR ||
                        columnType == AttributeType.CHAR ||
                        columnType == AttributeType.TEXT) {
                int maxlength = findMaxStringLength(featureCollection, t, charset);

                if (maxlength > 255) {
                    // If truncate option has been applied for less than 30 s
                    // automatically switch to truncate option
                    if ((new Date().getTime() - lastTimeTruncate) < 30000) {
                        maxlength = 255;
                    }
                    else {
                        OKCancelDialog okCancelDialog = getLongFieldManagementDialogBox();
                        okCancelDialog.setLocationRelativeTo(null);
                        okCancelDialog.setVisible(true);
                        if (okCancelDialog.wasOKPressed()) {
                            maxlength = 255;
                            truncate = true;
                        }
                        else {
                            truncate = false;
                            throw new Exception(
                                I18N.get("io.ShapefileWriter.export-cancelled") + " " +
                                I18N.get("io.ShapefileWriter.more-than-255-characters-field-found"));
                        }
                    }
                }

                fields[f] = new DbfFieldDef(columnName, 'C', maxlength, 0);
                //fields[f] = overrideWithExistingCompatibleDbfFieldDef(fields[f], fieldMap);
               f++;
            } else if (columnType == AttributeType.DATE ||
                    columnType == AttributeType.TIMESTAMP ||
                    columnType == AttributeType.TIME) {
                fields[f] = new DbfFieldDef(columnName, 'D', 8, 0);
                f++;                
            } else if (columnType == AttributeType.BOOLEAN || columnType == AttributeType.BIT) {
                fields[f] = new DbfFieldDef(columnName, 'L', 1, 0);
                f++;
            } else if (columnType == AttributeType.GEOMETRY) {
                //do nothing - the .shp file handles this
            } else if (columnType == null) {
            	//[sstein 9.Nov.2012] added this, as Sextante delivered an AttributeType set to null
            	if(!columnName.isEmpty()){
	            	// treat as string
	                int maxlength = findMaxStringLength(featureCollection, t, charset);
	
	                if (maxlength > 255) {
	                    // If truncate option has been applied for less than 30 s
	                    // automatically switch to truncate option
	                    if ((new Date().getTime() - lastTimeTruncate) < 30000) {
	                        maxlength = 255;
	                    }
	                    else {
	                        OKCancelDialog okCancelDialog = getLongFieldManagementDialogBox();
	                        okCancelDialog.setLocationRelativeTo(null);
	                        okCancelDialog.setVisible(true);
	                        if (okCancelDialog.wasOKPressed()) {
	                            maxlength = 255;
	                            truncate = true;
	                        }
	                        else {
	                            truncate = false;
	                            throw new Exception(
	                                I18N.get("io.ShapefileWriter.export-cancelled") + " " +
	                                I18N.get("io.ShapefileWriter.more-than-255-characters-field-found"));
	                        }
	                    }
	                }
	
	                fields[f] = new DbfFieldDef(columnName, 'C', maxlength, 0);
	                //fields[f] = overrideWithExistingCompatibleDbfFieldDef(fields[f], fieldMap);
	               f++;
            	}
            } else {
                throw new Exception(I18N.get("io.ShapefileWriter.unsupported-attribute-type") + " : " + columnType.toString() );
            }
        }

        // write header
        DbfFileWriter dbf = new DbfFileWriter(fname);
		dbf.setCharset(charset);
        dbf.writeHeader(fields, featureCollection.size());

        //write rows
        num = featureCollection.size();

        List features = featureCollection.getFeatures();

        for (t = 0; t < num; t++) {
            //System.out.println("dbf: record "+t);
            Feature feature = (Feature) features.get(t);
            Vector<Object> DBFrow = new Vector<>();

            //make data for each column in this feature (row)
            for (u = 0; u < fs.getAttributeCount(); u++) {
                AttributeType columnType = fs.getAttributeType(u);

                if (columnType == AttributeType.INTEGER ||
                        columnType == AttributeType.SMALLINT ||
                        columnType == AttributeType.TINYINT) {
                    Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(0);
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.LONG || columnType == AttributeType.BIGINT) {
                    Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(0L);
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.DOUBLE ||
                        columnType == AttributeType.REAL ||
                        columnType == AttributeType.FLOAT ||
                        columnType == AttributeType.DECIMAL ||
                        columnType == AttributeType.NUMERIC ||
                        columnType == AttributeType.BIGDECIMAL) {
                    Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(0.0);
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.DATE ||
                        columnType == AttributeType.TIMESTAMP ||
                        columnType == AttributeType.TIME) {
                    Object a = feature.getAttribute(u);
                    if (a == null) {
                        DBFrow.add("");
                    } else {
                        DBFrow.add(DbfFile.DATE_PARSER.format((Date)a));
                    }                    
                } else if (columnType == AttributeType.STRING ||
                        columnType == AttributeType.OBJECT ||
                        columnType == AttributeType.VARCHAR ||
                        columnType == AttributeType.LONGVARCHAR ||
                        columnType == AttributeType.CHAR ||
                        columnType == AttributeType.TEXT) {
                    Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add("");
                    } else {
                        // MD 16 jan 03 - added some defensive programming
                        if (a instanceof String) {
                            DBFrow.add(a);
                        } else {
                            DBFrow.add(a.toString());
                        }
                    }
                } else if (columnType == AttributeType.BOOLEAN || columnType == AttributeType.BIT) {
                    Object a = feature.getAttribute(u);
                    if (a == null) {
                        DBFrow.add(null);
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == null) {
                	// [sstein 9 Nov. 2012] added:
                	// in case there is no attribute type but an attribute name
                	// which was for instance returned by Sextante Buffer algorithm
                	// than we treat it like a String
                	String columnName = fs.getAttributeName(u);
                	if(!columnName.isEmpty()){
                        Object a = feature.getAttribute(u);

                        if (a == null) {
                            DBFrow.add("");
                        } else {
                            // MD 16 jan 03 - added some defensive programming
                            if (a instanceof String) {
                                DBFrow.add(a);
                            } else {
                                DBFrow.add(a.toString());
                            }
                        }
                	}
                }
            }

            dbf.writeRecord(DBFrow);
        }

        dbf.close();
    }

    // Prepare prj writing for 1.12 version
    private String getPrjString(String fname, String registry, String code)
            throws Exception {
        if (code.equals("0")) return null;
        if (!code.matches("\\d+")) return null;
        if (Class.forName("org.cts.CRSFactory") != null) {
            org.cts.CRSFactory crsFactory = new org.cts.CRSFactory();
            org.cts.registry.RegistryManager registryManager = crsFactory.getRegistryManager();
            if (registry.equals("EPSG")) registryManager.addRegistry(new org.cts.registry.EPSGRegistry());
            if (registry.equals("ESRI")) registryManager.addRegistry(new org.cts.registry.ESRIRegistry());
            org.cts.crs.CoordinateReferenceSystem crs = crsFactory.getCRS(registry + ":" + code);
            //System.out.println(crs.toWKT());
            return crs.toWKT();
        } else {
            throw new Exception("Class org.cts.CRSFactory has not been found");
        }
    }

    private String removeCount(String s, int count) {
        return s.substring(0, s.length()-Integer.toString(count).length());
    }

    private String uniqueName(Set<String> set, String name, int maxLength) {

        name = name.substring(0, Math.min(maxLength, name.length()));

        int count = 1;
        while (set.contains(name)) {
            // case name + digit = 10 maxLength
            if (name.length()==maxLength) {
                name = removeCount(name, count);
            }
            // name + digits < maxLength
            else if (count > 1) {
                name = removeCount(name, count-1);
            }
            name = name + Integer.toString(count);
            count++;
        }
        set.add(name);
        return name;
    }

    private DbfFieldDef overrideWithExistingCompatibleDbfFieldDef(DbfFieldDef field, Map columnMap) {
    	String fieldname = field.fieldname.toString().trim();
    	if ((columnMap != null) && (columnMap.containsKey(fieldname))) {
    		DbfFieldDef dbfFieldDef = (DbfFieldDef) columnMap.get(fieldname);
    		dbfFieldDef.fieldname = field.fieldname;    //must have null padded version to work
    		switch(dbfFieldDef.fieldtype){
    		case 'C': case 'c':  //character case not working yet
    			if (field.fieldtype == 'C')
    				if (field.fieldlen > dbfFieldDef.fieldlen)  //allow string expansion if needed
    					return field;
    				else {
    					dbfFieldDef.fieldtype = field.fieldtype;
    					return dbfFieldDef; 
    				}
    			break;
    		// if previous dbf field with the same name was a numeric
    		// and new field type is N, set type to N, but keep old 
    		// field length
    		case 'N': case 'n': case 'F': case 'f':
    			if (field.fieldtype == 'N') {
    				dbfFieldDef.fieldtype = field.fieldtype;
    				return dbfFieldDef;   
    			}
    			break;
    		}   		
    	}
    	return field;
    }

    /**
     * Look at all the data in the column of the featurecollection, and find the largest string!
     * @param fc features to look at
     * @param attributeIndex which of the column to test.
     */
    private int findMaxStringLength(FeatureCollection fc, int attributeIndex, Charset charset) {
        int l;
        int maxlen = 0;
        Feature f;

        for (Iterator i = fc.iterator(); i.hasNext();) {
            f = (Feature) i.next();
            //patch from Hisaji Ono for Double byte characters
            if (f.getAttribute(attributeIndex) == null) {
                continue;
            }
            l = f.getString(attributeIndex).getBytes(charset).length;

            if (l > maxlen) {
                maxlen = l;
            }
        }

        return Math.max(1, maxlen); //LDB: don't allow zero length strings
    }


    /**
     * Find the generic geometry type of the feature collection.
     * Simple method - find the 1st non null geometry and its type
     * is the generic type.
     * returns  0 : only empty geometry collection <br>
     *          1 : only single points<br>
     *          3 : at least one line or multiline<br>
     *          5 : at least one polygon or multipolygon <br>
     *          8 : at least one multipoint<br>
     *         31 : only non empty geometry collection<br>
     * @param fc feature collection containing tet geometries.
     */
    private int findBestGeometryType(FeatureCollection fc) {
        Geometry geom;
        boolean onlyPoints = true;
        boolean onlyEmptyGeometryCollection = true;
        // [mmichaud 2007-06-12] : add the type variable to test if
        // all geometries are single Point
        // maybe it would be clearer using shapefile types integer for type
        
        for (Iterator i = fc.iterator(); i.hasNext();) {
            geom = ((Feature) i.next()).getGeometry();

            // If geometry is empty, we should be able
            // to write it in any kind of shapefile
            if (geom.isEmpty()) continue;

            if (onlyPoints && !(geom instanceof Point)) {
                onlyPoints = false;
            }
            
            if (onlyEmptyGeometryCollection && !(geom.isEmpty())) {
                onlyEmptyGeometryCollection = false;
            }

            if (geom instanceof MultiPoint) {
                return 8;
            }

            if (geom instanceof Polygon) {
                return 5;
            }

            if (geom instanceof MultiPolygon) {
                return 5;
            }

            if (geom instanceof LineString) {
                return 3;
            }

            if (geom instanceof MultiLineString) {
                return 3;
            }
        }
        
        if (onlyPoints) return 1;
        else if (onlyEmptyGeometryCollection) return 0;
        else return 31;
        
    }

    private void checkIfGeomsAreMixed(FeatureCollection featureCollection) throws Exception {
	    //-- sstein: check first if features are of different geometry type.
	    int i= 0;
	    Class firstClass = null;
	    Geometry firstGeom = null;
	    for (Iterator iter = featureCollection.iterator(); iter.hasNext();) {
			Feature myf = (Feature) iter.next();
            // mmichaud 2014-03-15 consider that empty geometries are un-typed
            if (myf.getGeometry().isEmpty()) continue;
			if (i==0){
				firstClass = myf.getGeometry().getClass();
				firstGeom = myf.getGeometry();
			}
			else{
				if (firstClass != myf.getGeometry().getClass()){
			       // System.out.println("first test failed");
					if((firstGeom instanceof Polygon) && (myf.getGeometry() instanceof MultiPolygon)){
                        //everything is ok
					}
					else if((firstGeom instanceof MultiPolygon) && (myf.getGeometry() instanceof Polygon)){
						//everything is ok
					}
					else if((firstGeom instanceof Point) && (myf.getGeometry() instanceof MultiPoint)){
						//everything is ok
					}
					else if((firstGeom instanceof MultiPoint) && (myf.getGeometry() instanceof Point)){
						//everything is ok
					}
					else if((firstGeom instanceof LineString) && (myf.getGeometry() instanceof MultiLineString)){
						//everything is ok
					}
					else if((firstGeom instanceof MultiLineString) && (myf.getGeometry() instanceof LineString)){
						//everything is ok
					}
					else{
			           throw new IllegalParametersException(
			               I18N.get("io.ShapefileWriter.unsupported-mixed-geometry-type"));
			        }
				}
			}
			i++;
	    }
	}
    
    /**
     * Reverses the order of points in lr (is CW -> CCW or CCW->CW)
     */
    private LinearRing reverseRing(LinearRing lr) {
        int numPoints = lr.getNumPoints();
        Coordinate[] newCoords = new Coordinate[numPoints];

        for (int t = 0; t < numPoints; t++) {
            newCoords[t] = lr.getCoordinateN(numPoints - t - 1);
        }

        return lr.getFactory().createLinearRing(newCoords);
    }

    /**
     * Make sure outer ring is CCW and holes are CW
     * @param p polygon to check
     */
    private Polygon makeGoodSHAPEPolygon(Polygon p) {
        
        if (p.isEmpty()) return p;
        
        LinearRing outer;
        LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
        Coordinate[] coords;

        coords = p.getExteriorRing().getCoordinates();

        if (CGAlgorithms.isCCW(coords)) {
            outer = reverseRing((LinearRing) p.getExteriorRing());
        } else {
            outer = (LinearRing) p.getExteriorRing();
        }

        for (int t = 0; t < p.getNumInteriorRing(); t++) {
            coords = p.getInteriorRingN(t).getCoordinates();
            if (!(CGAlgorithms.isCCW(coords))) {
                holes[t] = reverseRing((LinearRing) p.getInteriorRingN(t));
            } else {
                holes[t] = (LinearRing) p.getInteriorRingN(t);
            }
        }

        return p.getFactory().createPolygon(outer, holes);
    }

    /**
     * Make sure outer ring is CCW and holes are CW for all the polygons in the Geometry
     * @param mp set of polygons to check
     */
    private MultiPolygon makeGoodSHAPEMultiPolygon(MultiPolygon mp) {
        MultiPolygon result;
        Polygon[] ps = new Polygon[mp.getNumGeometries()];

        //check each sub-polygon
        for (int t = 0; t < mp.getNumGeometries(); t++) {
            ps[t] = makeGoodSHAPEPolygon((Polygon) mp.getGeometryN(t));
        }

        result = mp.getFactory().createMultiPolygon(ps);

        return result;
    }

    /**
     * Return a single geometry collection <br>
     * result.GeometryN(i) = the i-th feature in the FeatureCollection<br>
     * All the geometry types will be the same type (ie. all polygons) - or they will be set to<br>
     * NULL geometries<br>
     * <br>
     * GeometryN(i) = {Multipoint,Multilinestring, or Multipolygon)<br>
     *
     * @param fc feature collection to make homogeneous
     */
    private GeometryCollection makeSHAPEGeometryCollection(FeatureCollection fc)
        throws Exception {
        GeometryCollection result;
        Geometry[] allGeoms = new Geometry[fc.size()];
        GeometryFactory gf = new GeometryFactory();
        
        int geomtype = findBestGeometryType(fc);

        if (geomtype == 31) {
            throw new Exception(
                I18N.get("io.ShapefileWriter.unsupported-geometry-collection"));
        }

        List features = fc.getFeatures();

        for (int t = 0; t < features.size(); t++) {
            
            Geometry geom = ((Feature) features.get(t)).getGeometry();
            gf = geom.getFactory();

            switch (geomtype) {
            
            case 0: //empty geometry collection
                // empty geometry collections are arbitrarily written in a Point shapefile
                allGeoms[t] = gf.createGeometryCollection(new Geometry[0]);
                break;
                
            case 1: //single point

                if ((geom instanceof Point)) {
                    allGeoms[t] = geom;
                } else {
                    allGeoms[t] = gf.createPoint((Coordinate)null);
                }

                break;
                
            case 8: //point

                if ((geom instanceof Point)) {
                    //good!
                    Point[] p = new Point[1];
                    p[0] = (Point) geom;

                    allGeoms[t] = gf.createMultiPoint(p);
                } else if (geom instanceof MultiPoint) {
                    allGeoms[t] = geom;
                } else {
                    allGeoms[t] = gf.createMultiPoint(new Point[0]);
                }

                break;

            case 3: //line

                if ((geom instanceof LineString)) {
                    LineString[] l = new LineString[1];
                    l[0] = (LineString) geom;

                    allGeoms[t] = gf.createMultiLineString(l);
                } else if (geom instanceof MultiLineString) {
                    allGeoms[t] = geom;
                } else {
                    allGeoms[t] = gf.createMultiLineString(new LineString[0]);
                }

                break;

            case 5: //polygon

                if (geom instanceof Polygon) {
                    //good!
                    Polygon[] p = new Polygon[1];
                    p[0] = (Polygon) geom;

                    allGeoms[t] = makeGoodSHAPEMultiPolygon(gf.createMultiPolygon(p));
                } else if (geom instanceof MultiPolygon) {
                    allGeoms[t] = makeGoodSHAPEMultiPolygon((MultiPolygon) geom);
                } else {
                    allGeoms[t] = gf.createMultiPolygon(new Polygon[0]);
                }

                break;
            }
        }

        result = gf.createGeometryCollection(allGeoms);

        return result;
    }
    
    private boolean deleteIndex(String path, String nameWithoutExtension, String extension) {
        File file = new File(path + nameWithoutExtension + "." + extension.toLowerCase());
        if (file.exists()) return file.delete();
        else {
            file = new File(path + nameWithoutExtension + "." + extension.toUpperCase());
            return file.exists() && file.delete();
        }
    }
    
    private OKCancelDialog getLongFieldManagementDialogBox() {
        return new OKCancelDialog((JFrame)null, I18N.get("io.ShapefileWriter.fields-too-long"), true, 
            new JLabel(
                "<html><br/>" +
                I18N.get("io.ShapefileWriter.more-than-255-characters-field-found") +
                "<br/><br/>" +
                I18N.get("io.ShapefileWriter.truncate-option") +
                "<br/></html>"), null);
    }

    private String java_cp_2_esri(String java_cp) {
        if (java_cp.startsWith("IBM")) return "OEM " + java_cp.substring(3);
        else if (java_cp.startsWith("ISO-8859-6")) return "OEM 708";
        else if (java_cp.startsWith("ISO-8859-")) return "ISO 8859" + java_cp.substring(9);
        else if (java_cp.startsWith("windows-")) return "OEM " + java_cp.substring(8);
        else if (java_cp.equalsIgnoreCase("x-windows-")) return "OEM " + java_cp.substring(10);
        else if (java_cp.equalsIgnoreCase("UTF-8")) return "UTF-8";
        else if (java_cp.equalsIgnoreCase("Shift_JIS")) return "SJIS";
        else if (java_cp.equalsIgnoreCase("Big5")) return "Big5";
        else if (java_cp.equalsIgnoreCase("GBK")) return "OEM 936";
        else return java_cp;
    }
}
