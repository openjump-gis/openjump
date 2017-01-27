/*
 * FMEGMLReader.java
 *
 * Created on June 17, 2002, 1:46 PM
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

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.DataSource;

import java.io.*;
import java.nio.charset.Charset;


/**
 * A driver that reads GML files following the FME default schema.
 * <p>
 * <h3>DataProperties for the driver</h3>
 *
 * <table border=1 width=100%>
 * <tr><th>Parameter</th><th>Meaning</th></tr>
 * <tr><td>File <i>or</i> DefaultValue</td><td>File name for the input FME .xml file</td></tr>
 * <tr><td>CompressedFile</td><td>File name (a .zip or .gz) with a .jml/.xml/.gml inside (specified by File)</td></tr>
 * </table>
 * <p>
 * This is implemented using the more general {@link GMLReader}.
 * It makes a {@link GMLInputTemplate} to pass to the GMLReader by examining
 * the schema specified in the input file.
 */
// TODO : I18N
public class FMEGMLReader extends AbstractJUMPReader {

    /** Creates new FMEGMLReader */
    public FMEGMLReader() {
    }

    /**
     * Main method - reads in FMEGML file specified in the DriverProperties ('InputFile' or 'DefaultValue')
     *
     * @param dp {@link DriverProperties} to specify the file to read ('InputFile' or 'DefaultValue')
     */
    public FeatureCollection read(DriverProperties dp) throws Exception {

        FeatureCollection result;
        InputStream inputStream;
        GMLInputTemplate inputTemplate;
        GMLReader gmlReader = new GMLReader();
        String inputfname;
        boolean isCompressed;

        isCompressed = (dp.getProperty(DataSource.COMPRESSED_KEY) != null);

        inputfname = dp.getProperty(DriverProperties.DEFAULT_VALUE_KEY);

        if (inputfname == null) {
            inputfname = dp.getProperty(DataSource.FILE_KEY);
        }

        if (inputfname == null) {
            throw new IllegalParametersException(
                "call to FMEReader.read() has DataProperties w/o a InputFile specified");
        }

        if (isCompressed) {
            inputStream = CompressedFile.openFile(inputfname,
                            dp.getProperty(DataSource.COMPRESSED_KEY));
        } else {
            inputStream = new BufferedInputStream(new FileInputStream(inputfname));
        }

        try {
            try {
                inputTemplate = getGMLInputTemplate(inputStream, inputfname);
            } finally {
                inputStream.close();
            }
        } finally {
            inputStream.close();
        }

        if (isCompressed) {
            inputStream = CompressedFile.openFile(inputfname,
                            dp.getProperty(DataSource.COMPRESSED_KEY));
        } else {
            inputStream = new BufferedInputStream(new FileInputStream(inputfname));
        }

        try {
            gmlReader.setInputTemplate(inputTemplate);

            try {
                result = gmlReader.read(inputStream, inputfname);
            } finally {
                inputStream.close();
            }
        } finally {
            inputStream.close();
        }

        return result;
    }

    /**
     * Parse the input file and make a GMLInputTemplate out of it
     *
     * @param fname just used in error message
     * @param inputStream a UTF-8 encoded input stream
     */
    public GMLInputTemplate getGMLInputTemplate(InputStream inputStream, String fname)
        throws java.io.IOException, ParseException {

        GMLInputTemplate result;
        LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        int lineNo = 0;
        boolean foundStartTag = false;
        boolean foundEndTag = false;
        String s;
        String columns;
        String columnName;
        String columnType;
        String columnDef;
        String templateText;
        int start;
        int end;
        String propertyNamePrefix;

        while (!foundStartTag && lineNo < 10) {
            s = reader.readLine();

            if (s == null) {
                throw new ParseException(
                    "Couldn't find a <schemaFeatures> tag in the input FME GML file.  This isn't a valid FME GML file.");
            }

            lineNo++;

            if (s.contains("<schemaFeatures>")) {
                foundStartTag = true;
            }
        }

        if (!(foundStartTag)) {
            throw new ParseException("Read first 10 lines of " + fname +
                " and couldn't find a <schemaFeatures> tag.  This isn't a valid FME GML file.");
        }

        columns = "";

        while (!foundEndTag) {
            s = reader.readLine();

            if (s.contains("</schemaFeatures>")) {
                foundEndTag = true;
            }

            if ((s.contains("<property fme:name")) ||
                    (s.contains("<property name"))) {
                //column definition
                //handle 2 fme variants - <property fme:name="name">...</property> and <property name="name">..</property>
                propertyNamePrefix = "";

                if (s.contains("<property fme:name")) {
                    propertyNamePrefix = "fme:";
                }

                //get column name
                start = s.indexOf("\"");
                end = s.indexOf("\"", start + 1);

                if ((start == -1) || (end == -1)) {
                    throw new ParseException("Parsing file " + fname +
                        " couldn't get column name on line # " +
                        reader.getLineNumber() + " - " + s);
                }

                columnName = s.substring(start + 1, end);

                start = s.indexOf(">");
                end = s.indexOf("<", start + 1);

                if ((start == -1) || (end == -1)) {
                    throw new ParseException("Parsing file " + fname +
                        " couldn't get column type on line # " +
                        reader.getLineNumber() + " - " + s);
                }

                columnType = FMEtypeToJCSType(s.substring(start + 1, end));
                columnDef = "     <column>\n     <name>" + columnName +
                    "</name>\n          <type>" + columnType + "</type>\n";
                columnDef = columnDef +
                    "         <valueElement elementName=\"property\" attributeName=\"" +
                    propertyNamePrefix + "name\" attributeValue=\"" +
                    columnName + "\" />\n";
                columnDef = columnDef +
                    "         <valueLocation position=\"body\" />\n";
                columnDef = columnDef + "     </column>\n";
                columns = columns + columnDef;
            }
        }

        //add <featuretype>...</featuretype> 'column'
        columnDef = "     <column>\n     <name>featuretype</name>\n";
        columnDef = columnDef + "           <type>STRING</type>\n";
        columnDef = columnDef +
            "           <valueElement elementName=\"featureType\"/>\n";
        columnDef = columnDef +
            "           <valueLocation position=\"body\"/>\n";
        columnDef = columnDef + "     </column>\n";
        columns = columns + columnDef;

        templateText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
        templateText = templateText + "<JCSGMLInputTemplate>\n";
        templateText = templateText +
            "     <CollectionElement>dataFeatures</CollectionElement>\n";
        templateText = templateText +
            "     <FeatureElement>Feature</FeatureElement>\n";
        templateText = templateText +
            "     <GeometryElement>gml:PointProperty</GeometryElement>\n";
        templateText = templateText +
            "     <GeometryElement>gml:PolygonProperty</GeometryElement>\n";
        templateText = templateText +
            "     <GeometryElement>gml:LineStringProperty</GeometryElement>\n";

        templateText = templateText +
            "     <GeometryElement>gml:MultiPointProperty</GeometryElement>\n";
        templateText = templateText +
            "     <GeometryElement>gml:MultiPolygonProperty</GeometryElement>\n";
        templateText = templateText +
            "     <GeometryElement>gml:MultiLineStringProperty</GeometryElement>\n";

        templateText = templateText +
            "     <GeometryElement>gml:MultiGeometryProperty</GeometryElement>\n";

        templateText = templateText + "     <ColumnDefinitions>\n";
        templateText = templateText + columns;
        templateText = templateText + "     </ColumnDefinitions>\n";
        templateText = templateText + "</JCSGMLInputTemplate>\n";

        // System.out.println(templateText);
        //java.io.StringReader sr = new java.io.StringReader(templateText);
        InputStream is = new ByteArrayInputStream(templateText.getBytes("UTF-8"));
        result = new GMLInputTemplate();
        result.load(is, "Auto created FME GML input template");
        is.close();

        return result;
    }

    /**
     * converts the name of an FME type to a JCS type (STRING,DOUBLE, INTEGER)
     *
     * @param fmeType type that fme reports (ie. fme_char, fme_decimal, long)
     */
    private String FMEtypeToJCSType(String fmeType) {
        if (fmeType.contains("fme_char")) {
            return "STRING";
        }

        if (fmeType.contains("fme_decimal")) {
            int loc;

            loc = fmeType.indexOf(",");

            if ((loc == -1) || (loc == (fmeType.length() - 1))) {
                return "STRING"; //bad
            }

            if (fmeType.substring(loc + 1, loc + 2).equalsIgnoreCase("0")) {
                return "INTEGER";
            }

            return "DOUBLE";
        }

        if (fmeType.contains("long")) {
            return "DOUBLE"; // strange but true!
        }

        return "STRING";
        
        //Note that there is no FME "date" type. FME's Shapefile=>GML conversion converts
        //dates to strings. [Jon Aquino]
    }
}
