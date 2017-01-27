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
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.feature.*;

import java.io.BufferedWriter;
import java.io.FileReader;

import java.lang.reflect.Array;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;


/**
 * GMLWriter is a {@link JUMPWriter} specialized to output GML.
 *
 * <p>
 * DataProperties for the JCSWriter write(featureSchema,DataProperties) interface:<br>
 * </p>
 * <p>
 * <table border='1' cellspacing='0' cellpadding='4'>
 * <tr>
 *    <th>Parameter</th><th>Meaning</th>
 * </tr>
 * <tr>
 *   <td>OutputFile or DefaultValue</td>
 *   <td>File name for output .xml file</td>
 * </tr>
 * <tr>
 *   <td>OutputTemplateFile</td>
 *   <td>File name for GMLOutputTemplate file</td>
 * </tr>
 * </table><br>
 * </p>
 * NOTE: If OutputTemplateFile is unspecified, one will be auto-created (the JCS format).
 * <br>
 * <br>
 * Programmer details: <br>
 * <br>
 * This is usually called as follows:<br>
 * <pre>
 *    gmlWriter= new GMLWriter(); 
 *    gmlWriter.write( DriverProperites);
 * </pre>
 * or: 
 * <pre>
 *    gmlWriter.setOutputTemplate( GMLOutputTemplate);
 *    gmlWriter.write( <writer>, <stream name>);
 * </pre>
 * <br>

 * Also, the function "makeOutputTemplate()" is useful for
 * autogenerating the JCS outputtemplate.
 * <br>
 * <br>
 *
 * Output will be formed from the OutputTeplate like:<Br>
 * <br>
 * <pre>
 * headerText
 * ==== This section repeated for each feature 
 * featureText[0] &lt;evaluate codeText[0]&gt;
 * featureText[1] &lt;evaluate codeText[1]&gt;
 * ... 
 * featureTextFooter
 * ====
 * footerText 
 * </pre>
 * 
 */
public class GMLWriter implements JUMPWriter {
    // Standard tags for the auto-generated outputTemplate.
    public static String standard_geom = "geometry";
    public static String standard_feature = "feature";
    public static String standard_featureCollection = "featureCollection";
    private GMLOutputTemplate outputTemplate = null;
    private GMLGeometryWriter geometryWriter = new GMLGeometryWriter();

    /** constructor**/
    public GMLWriter() {
        geometryWriter.setLinePrefix("                ");
    }

    /**
     * Main entry function - write the GML file.
     * @param featureCollection features to write
     * @param dp specify the 'OuputFile' and 'OuputTemplateFile'
     */
    public void write(FeatureCollection featureCollection, DriverProperties dp)
        throws IllegalParametersException, Exception {
        GMLOutputTemplate gmlTemplate;
        String outputFname;

        outputFname = dp.getProperty("File");

        if (outputFname == null) {
            outputFname = dp.getProperty("DefaultValue");
        }

        if (outputFname == null) {
            throw new IllegalParametersException(
                "call to GMLWRite.write() has DataProperties w/o a OutputFile specified");
        }

        if (dp.getProperty("TemplateFile") == null) {
            //we're going create the output template
            gmlTemplate = GMLWriter.makeOutputTemplate(featureCollection.getFeatureSchema());
        } else {
            // load the template
            java.io.Reader r;

            r = new FileReader(dp.getProperty("TemplateFile"));
            gmlTemplate = new GMLOutputTemplate();
            gmlTemplate.load(r);
            r.close();
        }

        //have a template and FC.  Write it!
        setOutputTemplate(gmlTemplate);

        java.io.Writer w;

        w = new java.io.BufferedWriter(new java.io.FileWriter(outputFname));
        this.write(featureCollection, w);
        w.close();
    }

    /**
     * Actual evaluator/writer - you should have already called setOutputTemplate
     *@param featureCollection features to write
     *@param writer - where to send the output to
     */
    public void write(FeatureCollection featureCollection, java.io.Writer writer)
        throws Exception {
        BufferedWriter buffWriter;
        Feature f;
        String pre;
        String token;

        if (outputTemplate == null) {
            throw new Exception(
                "attempt to write GML w/o specifying the output template");
        }

        buffWriter = new BufferedWriter(writer);

        buffWriter.write(outputTemplate.headerText);

        for (Iterator t = featureCollection.iterator(); t.hasNext();) {
            f = (Feature) t.next();

            for (int u = 0; u < outputTemplate.featureText.size(); u++) {
                String evaled;
                pre = (String) outputTemplate.featureText.get(u);
                token = (String) outputTemplate.codingText.get(u);
                buffWriter.write(pre);
                evaled = evaluateToken(f, token);

                if (evaled == null) {
                    evaled = "";
                }

                buffWriter.write(evaled);
            }

            buffWriter.write(outputTemplate.featureTextfooter);
            buffWriter.write("\n");
        }

        buffWriter.write(outputTemplate.footerText);
        buffWriter.flush();
    }

    /**
     *Convert an arbitary string into something that will not cause XML to gack.
     * Ie. convert "<" to "&lt;"
     *@param s string to safe-ify
     */
    public static String safeXML(String s) {
        StringBuffer sb = new StringBuffer(s);
        char c;

        for (int t = 0; t < sb.length(); t++) {
            c = sb.charAt(t);

            if (c == '<') {
                sb.replace(t, t + 1, "&lt;");
            }

            if (c == '>') {
                sb.replace(t, t + 1, "&gt;");
            }

            if (c == '&') {
                sb.replace(t, t + 1, "&amp;");
            }

            if (c == '\'') {
                sb.replace(t, t + 1, "&apos;");
            }

            if (c == '"') {
                sb.replace(t, t + 1, "&quot;");
            }
        }

        return sb.toString();
    }

    /**
     * Attaches a GMLOuputTemplate
     */
    public void setOutputTemplate(GMLOutputTemplate ot) {
        outputTemplate = ot;
    }

    /**
     *takes a token and replaces it with its value (ie. geometry or column)
     * @param f feature to take geometry or column value from
     *@token token to evaluate - "column","geometry" or "geometrytype"
     */
    private String evaluateToken(Feature f, String token)
        throws Exception, ParseException {
        String column;
        String cmd;
        String result;
        int index;

        //token = token.toLowerCase();
        token = token.trim();

        if (!(token.startsWith("=")) || (token.length() < 7)) {
            throw new ParseException("couldn't understand token '" + token +
                "' in the output template");
        }

        token = token.substring(1);
        token = token.trim();
        index = token.indexOf(" ");

        if (index == -1) {
            cmd = token;
        } else {
            cmd = token.substring(0, token.indexOf(" "));
        }

        if (cmd.equalsIgnoreCase("column")) {
            column = token.substring(6);
            column = column.trim();

            //  System.out.println("column = " + column);
            result = toString(f, column);

            //need to ensure that the output is XML okay
            result = safeXML(result);

            return result;
        } else if (cmd.equalsIgnoreCase("geometry")) {
            // MD - testing new GMLGeometryWriter
            geometryWriter.setMaximumCoordinatesPerLine(1);

            return geometryWriter.write(f.getGeometry());

            //return Geometry2GML(f.getGeometry());
        } else if (cmd.equalsIgnoreCase("geometrytype")) {
            return f.getGeometry().getGeometryType();
        } else {
            throw new ParseException("couldn't understand token '" + token +
                "' in the output template");
        }
    }

    protected String toString(Feature f, String column) {
        Assert.isTrue(f.getSchema().getAttributeType(column) != AttributeType.GEOMETRY);
        Object attribute = f.getAttribute(column);
        if (attribute == null) { 
            return "";
        }
        if (attribute instanceof Date) {
            return format((Date)attribute);
        }
        return attribute.toString();
    }

    protected String format(Date date) {
        return dateFormatter.format(date);
    }
    
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    /** given a FEatureSchema, make an output template
     *  in the JCS  format
     * @param fcmd input featureSchema
     */
    public static GMLOutputTemplate makeOutputTemplate(FeatureSchema fcmd) {
        GMLOutputTemplate result;
        String inputTemplate;
        int t;
        String colName;
        String colText = "";
        String colCode = "";
        String colHeader = "";

        result = new GMLOutputTemplate();

        inputTemplate = makeInputTemplate(fcmd);

        result.setHeaderText(
            "<?xml version='1.0' encoding='UTF-8'?>\n<JCSDataFile xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2000/10/XMLSchema-instance\" >\n" +
            inputTemplate + "<" + standard_featureCollection + ">\n");

        colText = "";
        colHeader = "     <" + standard_feature + "> \n";

        for (t = 0; t < fcmd.getAttributeCount(); t++) {
            colName = fcmd.getAttributeName(t);
            colText = "";

            if (t != fcmd.getGeometryIndex()) {
                //not geometry
                colText = colHeader + "          <property name=\"" + colName +
                    "\">";
                colCode = "=column " + colName;
                colHeader = "</property>\n";
            } else {
                //geometry
                colText = colHeader + "          <" + standard_geom + ">\n";
                colCode = "=geometry";
                colHeader = "          </" + standard_geom + ">\n";
            }

            result.addItem(colText, colCode);
        }

        result.setFeatureFooter(colHeader + "     </" + standard_feature +
            ">\n");
        result.setFooterText("     </" + standard_featureCollection +
            ">\n</JCSDataFile>\n");

        return result;
    }

    /**given a FeatureSchema, make a chunk of XML that represents a valid
    * GMLInputTemplate for the JCS format.  Used by makeOutputTemplate since the
     * output template includes an inputtemplate.
     *
     *@param fcmd the featureSchema to describe
     */
    public static String makeInputTemplate(FeatureSchema fcmd) {
        String result;
        int t;

        result = "<JCSGMLInputTemplate>\n<CollectionElement>" +
            standard_featureCollection +
            "</CollectionElement> \n<FeatureElement>" + standard_feature +
            "</FeatureElement>\n<GeometryElement>" + standard_geom +
            "</GeometryElement>\n<ColumnDefinitions>\n";

        //fill in each of the column defs
        for (t = 0; t < fcmd.getAttributeCount(); t++) {
            String colDef;
            String colName;
            AttributeType attributeType;

            colName = fcmd.getAttributeName(t);

            if (t != fcmd.getGeometryIndex()) {
                colDef = "     <column>\n";

                colDef = colDef + "          <name>" + colName + "</name>\n";
                attributeType = fcmd.getAttributeType(t);
                colDef = colDef + "          <type>" + attributeType +
                    "</type>\n";

                colDef = colDef +
                    "          <valueElement elementName=\"property\" attributeName=\"name\" attributeValue=\"" +
                    colName + "\"/>\n";
                colDef = colDef +
                    "          <valueLocation position=\"body\"/>\n";

                colDef = colDef + "     </column>\n";

                result = result + colDef;
            }
        }

        result = result + "</ColumnDefinitions>\n</JCSGMLInputTemplate>\n\n";

        return result;
    }
}
