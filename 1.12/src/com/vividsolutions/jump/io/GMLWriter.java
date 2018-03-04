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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorSupport;
import com.vividsolutions.jump.task.TaskMonitorUtil;
import com.vividsolutions.jump.util.Timer;


/**
 * GMLWriter is a {@link JUMPWriter} specialized to output GML.
 * TODO: use a proper XML framework, switch to XML1.1 which supports much broader range of unicode characters
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
public class GMLWriter implements JUMPWriter, TaskMonitorSupport {

    // Standard tags for the auto-generated outputTemplate.
    private static final String standard_geom = "geometry";
    private static final String standard_feature = "feature";
    private static final String standard_featureCollection = "featureCollection";
    private GMLOutputTemplate outputTemplate = null;
    private GMLGeometryWriter geometryWriter = new GMLGeometryWriter();

    /** constructor**/
    public GMLWriter() {
        geometryWriter.setLinePrefix("      ");
    }

    /**
     * Main entry function - write the GML file.
     * @param featureCollection features to write
     * @param dp specify the 'OuputFile' and 'OuputTemplateFile'
     */
    public void write(FeatureCollection featureCollection, DriverProperties dp)
            throws Exception {
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
            gmlTemplate = makeOutputTemplate(featureCollection);
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

        Writer w;

        // we always write UTF-8
        // TODO: we should probably add a parser here to find out the
        //       charset hardcoded in the template's header section
        FileOutputStream fileStream = new FileOutputStream(new File(outputFname));
        w = new OutputStreamWriter(fileStream, "UTF-8");
        this.write(featureCollection, w);
        w.close();
    }

    /**
     * Actual evaluator/writer - you should have already called setOutputTemplate
     *@param featureCollection features to write
     *@param writer - where to send the output to
     */
    public void write(FeatureCollection featureCollection, Writer writer)
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

        // write a bounded by box definition setting an srid for the whole dataset
        // Workaround or convenience as OGC writes in the WFS 2.0 standard:
        //   11.3.6 Inheritance rules for srsName values
        String srsAttrib="";
        if (getSrid(featureCollection) > 0){
          srsAttrib=" srsName=\"http://www.opengis.net/gml/srs/epsg.xml#"+getSrid(featureCollection)+"\"";
        }
        Envelope env = featureCollection.getEnvelope();
        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        df.setGroupingUsed(false);
        String envString = df.format(env.getMinX()) + "," + df.format(env.getMinY()) + " " + df.format(env.getMaxX())
            + "," + df.format(env.getMaxY());
        buffWriter.write(
          "  <gml:boundedBy>\n" +
          "    <gml:Box"+srsAttrib+">\n" +
          "      <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">" + envString + "</gml:coordinates>\n" +
          "    </gml:Box>\n" +
          "  </gml:boundedBy>\n");

        long milliSeconds = 0;
        int count = 0;
        int size = featureCollection.size();
        TaskMonitorUtil.report(getTaskMonitor(),
            I18N.getMessage("Writer.writing-features"));
        for (Iterator t = featureCollection.iterator(); t.hasNext() && !getTaskMonitor().isCancelRequested();) {
            f = (Feature) t.next();

            for (int u = 0; u < outputTemplate.featureText.size(); u++) {
                pre = outputTemplate.featureText.get(u);
                token = outputTemplate.codingText.get(u);

                //[mmichaud 2012-04-27] write directly into the writer instead
                // of getting string which are hard to handle for multi-million
                // coordinates geometries
                evaluateToken(f, pre, token, buffWriter);
            }

            buffWriter.write(outputTemplate.featureTextfooter);
            buffWriter.write("\n");
            
            long now = Timer.milliSecondsSince(0);
            count++;
            // show status every .5s
            if (now - 500 >= milliSeconds) {
              milliSeconds = now;
              TaskMonitorUtil.report(getTaskMonitor(), count, size, "");
            }
        }

        buffWriter.write(outputTemplate.footerText);
        buffWriter.flush();
    }

    /**
     *Convert an arbitary string into something that will not cause XML to gack.
     * Ie. convert "<" to "&lt;"
     *@param s string to safe-ify
     */
    private static String escapeXML(String s) {
        if (s == null) return null;
        
        // this should take care of really _all_ XML1.0 invalid chars
        // see https://commons.apache.org/proper/commons-lang/javadocs/api-3.5/org/apache/commons/lang3/StringEscapeUtils.html#escapeXml10-java.lang.String-
        return StringEscapeUtils.escapeXml10(s);
    }

    /**
     * utility function to retrieve srid from a feature collection or -1 if none
     * 
     * @param featureCollection
     * @return srid
     */
    private static int getSrid( FeatureCollection featureCollection ){
      Integer srid = -1;
      CoordinateSystem cs = featureCollection.getFeatureSchema().getCoordinateSystem();
      if (!cs.equals(CoordinateSystem.UNSPECIFIED)) {
        srid = cs.getEPSGCode();
      }

      return srid.intValue();
    }

    /**
     * Attaches a GMLOuputTemplate
     */
    void setOutputTemplate(GMLOutputTemplate ot) {
        outputTemplate = ot;
    }

    /**
     * Takes a token and replaces it with its value (ie. geometry or column)
     * @param f feature to take geometry or column value from
     * @param token to evaluate - "column","geometry" or "geometrytype"
     */
    // [mmichaud 2012-04-27] no more used, replaced by
    // evaluateToken(Feature f, String token, Writer writer)
    private String evaluateToken(Feature f, String token)
        throws Exception {
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
            result = escapeXML(result);
            
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
    
    // precompile pattern for performance reasons
    static Pattern closingTagPattern = Pattern.compile(">$");
    
    private void evaluateToken(Feature f, String pre, String token, Writer writer)
        throws Exception {
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

            result = toString(f, column);

            //need to ensure that the output is XML okay
            result = escapeXML(result);
            if (result == null)
              writer.append(closingTagPattern.matcher(pre).replaceAll(" xsi:nil=\"true\">"));
            else
              writer.append(pre).append(result);
            //writer.append(result);
            //return result;
        } else if (cmd.equalsIgnoreCase("geometry")) {
            // MD - testing new GMLGeometryWriter
            geometryWriter.setMaximumCoordinatesPerLine(1);

            //return geometryWriter.write(f.getGeometry(), writer);
            writer.append(pre);
            geometryWriter.write(f.getGeometry(), writer);

            //return Geometry2GML(f.getGeometry());
        } else if (cmd.equalsIgnoreCase("geometrytype")) {
            writer.append(pre);
            writer.append(f.getGeometry().getGeometryType());
            //return f.getGeometry().getGeometryType();
        } else {
            throw new ParseException("couldn't understand token '" + token +
                "' in the output template");
        }
    }

    protected String toString(Feature f, String column) {
        Assert.isTrue(f.getSchema().getAttributeType(column) != AttributeType.GEOMETRY);
        Object attribute = f.getAttribute(column);
        if (attribute == null) { 
            return null;
        }
        if (attribute instanceof Date) {
            return format((Date)attribute);
        }
        return attribute.toString();
    }

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    protected String format(Date date) {
        return dateFormatter.format(date);
    }

    /**
     * Given a FeatureSchema, make an output template
     * in the JCS  format
     * @param fcmd input featureSchema
     */
    private static GMLOutputTemplate makeOutputTemplate(FeatureCollection fc) {
        GMLOutputTemplate result;
        String inputTemplate;
        int t;
        String colName;
        String colText;
        String colCode;
        String colHeader;
        FeatureSchema fcmd = fc.getFeatureSchema();

        result = new GMLOutputTemplate();

        inputTemplate = makeInputTemplate(fc);

        result.setHeaderText(
            "<?xml version='1.0' encoding='UTF-8'?>\n<JCSDataFile xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2000/10/XMLSchema-instance\" >\n" +
            inputTemplate + "<" + standard_featureCollection + ">\n");

        colHeader = "  <" + standard_feature + ">\n";

        for (t = 0; t < fcmd.getAttributeCount(); t++) {
            colName = fcmd.getAttributeName(t);

            if (t != fcmd.getGeometryIndex()) {
                //not geometry
                colText = colHeader + "    <property name=\"" + escapeXML(colName) +
                    "\">";
                colCode = "=column " + colName;
                colHeader = "</property>\n";
            } else {
                //geometry
                colText = colHeader + "    <" + standard_geom + ">\n";
                colCode = "=geometry";
                colHeader = "    </" + standard_geom + ">\n";
            }

            result.addItem(colText, colCode);
        }

        result.setFeatureFooter(colHeader + "  </" + standard_feature + ">");
        result.setFooterText("</" + standard_featureCollection +
            ">\n</JCSDataFile>\n");

        return result;
    }

    /**
     * Given a FeatureSchema, make a chunk of XML that represents a valid
     * GMLInputTemplate for the JCS format.  Used by makeOutputTemplate since the
     * output template includes an inputtemplate.
     *
     * @param fcmd the featureSchema to describe
     */
    private static String makeInputTemplate(FeatureCollection fc) {
        String result = "";
        int t;
        FeatureSchema fcmd = fc.getFeatureSchema(); 

        result += "<JCSGMLInputTemplate>\n";
        result += "<CollectionElement>" + standard_featureCollection + "</CollectionElement>\n";
        result += "<FeatureElement>" + standard_feature + "</FeatureElement>\n";
        result += "<GeometryElement>" + standard_geom + "</GeometryElement>\n";
        // write a bounded by box definition setting an srid for the whole dataset
        // Workaround or convenience as OGC writes in the WFS 2.0 standard:
        //   11.3.6 Inheritance rules for srsName values
        result += "<CRSElement>boundedBy</CRSElement>\n";
        result += "<ColumnDefinitions>\n";

        //fill in each of the column defs
        for (t = 0; t < fcmd.getAttributeCount(); t++) {
            String colDef;
            String colName;
            AttributeType attributeType;

            colName = fcmd.getAttributeName(t);

            if (t != fcmd.getGeometryIndex()) {
                colDef = "     <column>\n";

                colDef = colDef + "          <name>" + escapeXML(colName) + "</name>\n";
                attributeType = fcmd.getAttributeType(t);
                colDef = colDef + "          <type>" + escapeXML(attributeType.toString()) +
                    "</type>\n";

                colDef = colDef +
                    "          <valueElement elementName=\"property\" attributeName=\"name\" attributeValue=\"" +
                    escapeXML(colName) + "\"/>\n";
                colDef = colDef +
                    "          <valueLocation position=\"body\"/>\n";

                colDef = colDef + "     </column>\n";

                result = result + colDef;
            }
        }

        result = result + "</ColumnDefinitions>\n</JCSGMLInputTemplate>\n\n";

        return result;
    }

    private TaskMonitor taskMonitor = new DummyTaskMonitor();

    public void setTaskMonitor(TaskMonitor taskMonitor) {
      this.taskMonitor = taskMonitor;
    }

    public TaskMonitor getTaskMonitor() {
      return taskMonitor;
    }
}
