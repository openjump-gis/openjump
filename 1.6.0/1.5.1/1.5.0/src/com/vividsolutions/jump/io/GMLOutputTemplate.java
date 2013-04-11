
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
import java.util.ArrayList;


/**
 * This class provides a parser for reading GMLOuputTemplates.  This does NOT contain an evaluator for <br>
 * taking the parsed GMLOutputTemplate and a featureCollection and producing properly <bR>
 * formatted output GML. Thats the {@link GMLWriter} class.<br>
 *<br>
 *<br>
 * Most people will just use the load() function, but there is support for directly <br>
 *constructing/modifying one.  Your best bet for hand creating one is to make a String that <br>
 *contains a GMLOutputTemplate then make a java.io.StringReader and passing it to the load()<br>
 *method. <br>
 *The valuators (things that look like "&lt;%") are:<br>
 * &lt;%=GEOMETRY&gt; - replace this with a GML represention of the geometry<br>
 * &lt;%=COLUMN <column name>&gt; - replace this with the corresponding JCS column value <br>
 *  &lt;%FEATURE&gt;  - this marks the start of a feature in the OutputTemplate <Br>
 * &lt;%ENDFEATURE&gt;  - This marks the end of a feature in the OutputTempalte <br>
 *<br>
 *<br>
 *Here's an example of how a simple GMLOutputTemplate is parsed by this class:
*for input like:
*<br>
*&lt;?xml version='1.0' encoding='UTF-8'?&gt; <br>
*&lt;dataset...&gt; <br>
*&lt;schemaFeatures&gt; <br>
*&lt;gml:featureMember&gt; <br>
*&lt;% FEATURE %&gt; <br>
*&lt;Feature&gt; <br>
*&lt;featureType&gt; &lt;%=COLUMN featureType %&gt; &lt;/featureType&gt; <br>
*&lt;property fme:name="ID"&gt; &lt;%=COLUMN ID %&gt; &lt;/property&gt; <br>
*&lt;property fme:name="SITE"&gt; &lt;%=COLUMN SITE %&gt; &lt;/property&gt; <br>
*&lt;property fme:name="gml2_coordsys"&gt;&lt;/property&gt; <br>
*&lt;gml:pointProperty&gt; <br>
* &lt;%=GEOMETRY %&gt;  <br>
*&lt;/gml:pointProperty&gt; <br>
*&lt;/Feature&gt; <br>
*&lt;% ENDFEATURE %&gt; <br>
*&lt;/gml:featureMember&gt; <br>
*&lt;/dataFeatures&gt; <br>
*&lt;/dataset&gt; <br>
* <br>
* <br>
* headeText =  <br>
* <br>
*&lt;?xml version='1.0' encoding='UTF-8'?&gt; <br>
*&lt;dataset...&gt; <br>
*&lt;schemaFeatures&gt; <br>
*&lt;gml:featureMember&gt; <br>
*  <br>
* footerText = <br>
*&lt;/gml:featureMember&gt; <br>
*&lt;/dataFeatures&gt; <br>
*&lt;/dataset&gt; <br>
* <br>
* <br>
*featureText[0] =  <br>
*&lt;Feature&gt; <br>
*&lt;featureType&gt; <br>
* <br>
* codingText[0] = "=COLUMN featureTtype" <br>
* <br>
*featureText[1] =  <br>
*&lt;/property&gt; <br>
*&lt;property fme:name="SITE"&gt;  <br>
* <br>
*codingText[1] = "=COLUMN ID" <br>
 **/
public class GMLOutputTemplate {
    String headerText;
    String footerText;
    String AllFeatureText;
    ArrayList featureText = new ArrayList();
    ArrayList codingText = new ArrayList();
    String featureTextfooter;
    int lineNumber = 0;
    String streamName;

    /** constructor**/
    public GMLOutputTemplate() {
    }

    /** sets the HeaderText for the Outputtemplate */
    public void setHeaderText(String text) {
        headerText = text;
    }

    /** sets the FooterText for the Outputtemplate */
    public void setFooterText(String text) {
        footerText = text;
    }

    /** sets the Footer text for the bottom of the feature*/
    public void setFeatureFooter(String text) {
        featureTextfooter = text;
    }

    /**
      *for input like : <br>
     * &lt;feature&gt; &lt;PROPERTY type=name&gt; &lt;%=NAME&gt;&lt;/property&gt; &lt;PROPERTY type=address&gt; &lt;%=ADDRESS&gt; &lt;/property&gt;<br>
     *<br>
     * use addItem("&lt;feature&gt; &lt;PROPERTY type=name&gt;","=NAME")<br>
     *     addItem("&lt;/property&gt; &lt;PROPERTY type=address&gt;", "%=ADDRESS")<br>
     *
     **/
    public void addItem(String header, String coding) {
        featureText.add(header);
        codingText.add(coding);
    }

    /**
     * Calls the main load() method with the stream name as "Unknown Stream"
     */
    public void load(java.io.Reader r) throws Exception {
        load(r, "Unknown Stream");
    }

    /**
     * Gets a line from the input Stream
     */
    private String getLine(BufferedReader br) throws Exception {
        lineNumber++;

        return br.readLine();
    }

    /**
     *Main function - parse a GMLOuputTemplate.
     *@param r actual reader to read from
     *@param readerName name of the stream (for error reporting)
     */
    public void load(java.io.Reader r, String readerName)
        throws Exception {
        int index;
        int index2;
        String line;
        boolean keepgoing;
        String token;
        String textAccum;
        boolean justFoundTag = false;
        BufferedReader buffRead = new BufferedReader(r);

        streamName = readerName;

        // find the header 
        headerText = "";
        keepgoing = true;
        line = "";
        index = 0;

        while (keepgoing && ((line = getLine(buffRead)) != null)) {
            if ((index = line.indexOf("<%")) != -1) {
                //found a "<%" tag, look for the "%>" tag
                index2 = line.indexOf("%>", index);

                if (index2 == -1) {
                    throw new ParseException("While trying to find the GML output header, found a <%, but no %>",
                        streamName, lineNumber, index);
                }

                token = line.substring(index + 2, index2);
                token = token.trim();

                if (!(token.equalsIgnoreCase("FEATURE"))) {
                    throw new ParseException("While trying to find the GML output header, found a <%..%> that isnt a <%FEATURE%>",
                        streamName, lineNumber, index);
                }

                keepgoing = false;
                headerText = headerText + line.substring(0, index);
                line = line.substring(index2 + 2);
            } else {
                headerText = headerText + line + "\n";
            }
        }

        if (line == null) {
            throw new ParseException("Unexpected EOF while looking for header",
                streamName, lineNumber, index);
        }

        //find the feature text
        AllFeatureText = "";
        keepgoing = true;
        textAccum = "";

        while (keepgoing) {
            if ((index = line.indexOf("<%")) != -1) {
                //found a "<%" tag, look for the "%>" tag
                index2 = line.indexOf("%>", index);

                if (index2 == -1) {
                    throw new ParseException("While looking at the GML feature text, found a <%, but no %>",
                        streamName, lineNumber, index);
                }

                token = line.substring(index + 2, index2).trim();

                if (token.equalsIgnoreCase("ENDFEATURE")) {
                    keepgoing = false;
                    AllFeatureText = AllFeatureText + line.substring(0, index);
                    featureTextfooter = textAccum + line.substring(0, index);
                    line = line.substring(index2 + 2);
                } else {
                    //handle a part of the feature spec\
                    if (!(validop(token))) {
                        throw new ParseException("invalid token in <%..%> :" +
                            token, streamName, lineNumber, index);
                    }

                    justFoundTag = true;

                    String pre = textAccum + line.substring(0, index);
                    textAccum = line.substring(index2 + 2);
                    featureText.add(pre);
                    codingText.add(token);
                }
            }

            if (keepgoing) {
                AllFeatureText = AllFeatureText + line + "\n";

                if (!(justFoundTag)) {
                    textAccum = textAccum + line + "\n";
                } else {
                    justFoundTag = false; //textAccum already has a portion of line in it.
                    textAccum = textAccum + "\n";
                }

                line = getLine(buffRead);

                if (line == null) {
                    throw new ParseException("Unexpected EOF while looking for feature",
                        streamName, lineNumber, index);
                }
            }
        }

        // grab the footer
        footerText = line;

        while ((line = getLine(buffRead)) != null) {
            footerText = footerText + line + "\n";
        }

        //System.out.println(this.asString());
    }

    /**
     * For debugging - return a human readable version of the parsed outputtemplate
     */
    public String asString() {
        String result;
        int t;

        result = headerText + "\n--------------------------------------\n";

        for (t = 0; t < featureText.size(); t++) {
            result = result + featureText.get(t) + "<%" + codingText.get(t) +
                "%>";
        }

        result = result + featureTextfooter;
        result = result + "\n--------------------------------------\n";
        result = result + footerText;

        return result;
    }

    /**
     * returns true if the intput string is a valid op (for error checking)
     *@param op test string that should contain an op
     */
    private boolean validop(String op) {
        String op2;

        op2 = new String(op);
        op2 = op2.trim();
        op2 = op2.toLowerCase();

        if (!(op2.startsWith("=")) || (op2.length() < 2)) {
            return false;
        }

        op2 = op2.substring(1);
        op2 = op2.trim();

        return (op2.startsWith("column") || op2.startsWith("geometry"));
    }
}
